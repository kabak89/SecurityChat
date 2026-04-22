package com.security.chat.multiplatform.features.chat.data.repoimpl

import com.security.chat.multiplatform.common.core.network.LiveEventsManager
import com.security.chat.multiplatform.common.core.network.NetworkManager
import com.security.chat.multiplatform.common.core.network.NetworkManagerFactory
import com.security.chat.multiplatform.common.core.network.entity.NetworkConfig
import com.security.chat.multiplatform.common.core.time.TimeProvider
import com.security.chat.multiplatform.features.chat.data.entity.ChatMessage
import com.security.chat.multiplatform.features.chat.data.entity.ChatSubscribeMessage
import com.security.chat.multiplatform.features.chat.data.entity.FindUserResponse
import com.security.chat.multiplatform.features.chat.data.entity.GetMessagesResponse
import com.security.chat.multiplatform.features.chat.data.entity.MessagesReceivedRequest
import com.security.chat.multiplatform.features.chat.data.entity.SendMessageRequest
import com.security.chat.multiplatform.features.chat.data.mapper.toDomain
import com.security.chat.multiplatform.features.chat.data.mapper.toSM
import com.security.chat.multiplatform.features.chat.data.storage.ChatStorage
import com.security.chat.multiplatform.features.chat.data.storage.entity.MessageSM
import com.security.chat.multiplatform.features.chat.domain.entity.Message
import com.security.chat.multiplatform.features.chat.domain.repo.ChatRepo
import com.security.chat.multiplatform.features.chats.data.storage.ChatsStorage
import com.security.chat.multiplatform.features.user.data.storage.UserStorage
import com.security.chat.multiplatform.features.users.data.storage.UsersStorage
import com.security.chat.multiplatform.features.users.data.storage.entity.UserSM
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.RSA
import dev.whyoleg.cryptography.algorithms.SHA512
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.io.encoding.Base64
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal class ChatRepoImpl(
    private val networkManagerFactory: NetworkManagerFactory,
    private val userStorage: UserStorage,
    private val usersStorage: UsersStorage,
    private val chatsStorage: ChatsStorage,
    private val chatStorage: ChatStorage,
    private val timeProvider: TimeProvider,
    private val liveEventsManager: LiveEventsManager,
    private val networkConfig: NetworkConfig,
) : ChatRepo {

    private val networkManager: NetworkManager by lazy {
        networkManagerFactory.build(baseUrl = "${networkConfig.host}:${networkConfig.port}")
    }

    @OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
    override suspend fun saveMessage(
        message: String,
        chatId: String,
    ) {
        val messageSM = MessageSM(
            id = Uuid.random().toString(),
            chatId = chatId,
            text = message,
            authorId = checkNotNull(userStorage.getUserId()),
            status = MessageSM.Status.Created,
            timestamp = timeProvider.now().toEpochMilliseconds(),
        )
        chatStorage.saveMessage(messageSM)
    }

    override suspend fun uploadMessages(chatId: String) {
        val authorId = checkNotNull(userStorage.getUserId())
        val chat = checkNotNull(chatsStorage.getChat(chatId))
        val companionId = chat.companionId

        val publicKey = usersStorage.getUser(companionId)?.publicKey ?: run {
            val userInfo = networkManager.runGet<FindUserResponse>(
                relativePath = "/users/info",
                request = mapOf("id" to companionId),
            )

            usersStorage.saveUser(
                user = UserSM(
                    id = userInfo.userId,
                    publicKey = userInfo.publicKey,
                    name = userInfo.login,
                ),
            )
            userInfo.publicKey
        }

        val messagesToUpload = chatStorage.getMessages(
            chatId = chatId,
            limit = Long.MAX_VALUE,
            offset = 0,
        )
            .filter { it.status == MessageSM.Status.Created }

        messagesToUpload
            .forEach { message ->
                val encryptedText = encryptText(
                    text = message.text,
                    publicKeyString = publicKey,
                )

                networkManager.runPost<SendMessageRequest, Unit>(
                    relativePath = "/messages",
                    request = SendMessageRequest(
                        id = message.id,
                        authorId = authorId,
                        chatId = chatId,
                        message = encryptedText,
                    ),
                )
            }

        val messagesToUpdate = messagesToUpload.map { it.copy(status = MessageSM.Status.Sent) }
        messagesToUpdate.forEach { message -> chatStorage.updateMessage(message) }
    }

    override suspend fun fetchMessages(
        chatId: String,
    ) {
        val privateKey = checkNotNull(userStorage.getKeys()?.privateKey)
        val userId = checkNotNull(userStorage.getUserId())

        val messages = networkManager.runGet<GetMessagesResponse>(
            relativePath = "/messages",
            request = mapOf("chat-id" to chatId),
        )
            .messages
            .filter { it.authorId != userId }
            .map { response ->
                response.toDomain(
                    decryptMessage = { encryptedText ->
                        decryptText(
                            text = encryptedText,
                            privateKeyString = privateKey,
                        )
                    },
                    appOwnerId = userId,
                )
            }
            .map {
                it.toSM(
                    chatId = chatId,
                )
            }

        val messageIds = messages.map { it.id }

        if (messageIds.isNotEmpty()) {
            val companionId = messages.first().authorId

            networkManager.runPost<MessagesReceivedRequest, Unit>(
                relativePath = "/messages/received",
                request = MessagesReceivedRequest(
                    authorId = companionId,
                    chatId = chatId,
                    messageIds = messageIds,
                ),
            )
        }

        chatStorage.saveMessages(messages)
    }

    override fun getMessagesFlow(chatId: String): Flow<List<Message>> {
        //TODO add paging
        return chatStorage.getMessagesFlow(
            chatId = chatId,
            limit = Long.MAX_VALUE,
        )
            .map { messages ->
                val userId = checkNotNull(userStorage.getUserId())

                messages
                    .map { message ->
                        message.toDomain(
                            appOwnerId = userId,
                        )
                    }
            }
            .distinctUntilChanged()
    }

    override suspend fun subscribeToNewMessages(chatId: String) {
        val authorId = requireNotNull(userStorage.getUserId())
        val subscribeMessage = ChatSubscribeMessage(
            chatId = chatId,
            authorId = authorId,
        )

        val privateKey = checkNotNull(userStorage.getKeys()?.privateKey)
        val userId = checkNotNull(userStorage.getUserId())

        liveEventsManager
            .subscribe<ChatMessage, ChatSubscribeMessage>(
                subscribeMessage = subscribeMessage,
                type = "chat_message",
            )
            .collect { chatMessage ->
                val newMessage = chatMessage.toDomain(
                    decryptMessage = { encryptedText ->
                        decryptText(
                            text = encryptedText,
                            privateKeyString = privateKey,
                        )
                    },
                    appOwnerId = userId,
                )

                val storageModel = newMessage.toSM(chatId)
                chatStorage.saveMessage(storageModel)

                networkManager.runPost<MessagesReceivedRequest, Unit>(
                    relativePath = "/messages/received",
                    request = MessagesReceivedRequest(
                        authorId = newMessage.authorId,
                        chatId = chatId,
                        messageIds = listOf(newMessage.id),
                    ),
                )
            }
    }

    private suspend fun encryptText(text: String, publicKeyString: String): String {
        val provider = CryptographyProvider.Default
        val rsa = provider.get(RSA.OAEP)
        val publicKeyBytes = Base64.decode(publicKeyString)
        val publicKey = rsa.publicKeyDecoder(digest = SHA512).decodeFromByteArray(
            format = RSA.PublicKey.Format.DER,
            bytes = publicKeyBytes,
        )
        val plaintextBytes = text.encodeToByteArray()
        val encryptedMessage = publicKey.encryptor().encrypt(plaintextBytes)
        return Base64.encode(encryptedMessage)
    }

    private suspend fun decryptText(text: String, privateKeyString: String): String {
        val provider = CryptographyProvider.Default
        val rsa = provider.get(RSA.OAEP)
        val privateKeyBytes = Base64.decode(privateKeyString)
        val privateKey = rsa.privateKeyDecoder(digest = SHA512).decodeFromByteArray(
            format = RSA.PrivateKey.Format.DER,
            bytes = privateKeyBytes,
        )
        val messageBytes = Base64.decode(text)
        return privateKey.decryptor().decrypt(messageBytes).decodeToString()
    }

}