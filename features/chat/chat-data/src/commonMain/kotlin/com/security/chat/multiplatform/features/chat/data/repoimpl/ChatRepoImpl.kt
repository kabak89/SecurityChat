package com.security.chat.multiplatform.features.chat.data.repoimpl

import com.security.chat.multiplatform.common.core.network.NetworkManager
import com.security.chat.multiplatform.common.core.network.NetworkManagerFactory
import com.security.chat.multiplatform.features.chat.data.entity.GetMessagesResponse
import com.security.chat.multiplatform.features.chat.data.entity.GetPublicKeyResponse
import com.security.chat.multiplatform.features.chat.data.entity.SendMessageRequest
import com.security.chat.multiplatform.features.chat.data.mapper.toDomain
import com.security.chat.multiplatform.features.chat.domain.entity.Message
import com.security.chat.multiplatform.features.chat.domain.repo.ChatRepo
import com.security.chat.multiplatform.features.chats.data.storage.ChatsStorage
import com.security.chat.multiplatform.features.user.data.storage.UserStorage
import com.security.chat.multiplatform.features.users.data.storage.UsersStorage
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.RSA
import dev.whyoleg.cryptography.algorithms.SHA512
import kotlin.io.encoding.Base64
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal class ChatRepoImpl(
    private val networkManagerFactory: NetworkManagerFactory,
    private val userStorage: UserStorage,
    private val usersStorage: UsersStorage,
    private val chatsStorage: ChatsStorage,
) : ChatRepo {

    private val networkManager: NetworkManager by lazy {
        networkManagerFactory.build(baseUrl = "http://192.168.1.5:80")
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun sendMessage(
        message: String,
        chatId: String,
    ) {
        val authorId = checkNotNull(userStorage.getUserId())
        val chat = checkNotNull(chatsStorage.getChat(chatId))
        val receiverId = if (chat.secondUserId == authorId) {
            chat.firstUserId
        } else {
            chat.secondUserId
        }

        val publicKey = usersStorage.getPublicKey(receiverId) ?: run {
            networkManager.runGet<GetPublicKeyResponse>(
                relativePath = "/users/public-key",
                request = mapOf("id" to receiverId),
            )
                .publicKey
        }

        val encryptedMessage = encryptMessage(message = message, publicKeyString = publicKey)

        networkManager.runPost<SendMessageRequest, Unit>(
            relativePath = "/messages",
            request = SendMessageRequest(
                id = Uuid.random().toString(),
                authorId = authorId,
                chatId = chatId,
                message = encryptedMessage,
            ),
        )
    }

    override suspend fun fetchMessages(
        chatId: String,
    ): List<Message> {
        val privateKey = checkNotNull(userStorage.getKeys()?.privateKey)

        return networkManager.runGet<GetMessagesResponse>(
            relativePath = "/messages",
            request = mapOf("chat-id" to chatId),
        )
            .messages.map { response ->
                response.toDomain(
                    decryptMessage = { encryptedText ->
                        decryptMessage(
                            message = encryptedText,
                            privateKeyString = privateKey,
                        )
                    },
                )
            }
    }

    private suspend fun encryptMessage(message: String, publicKeyString: String): String {
        val provider = CryptographyProvider.Default
        val rsa = provider.get(RSA.OAEP)
        val publicKeyBytes = Base64.decode(publicKeyString)
        val publicKey = rsa.publicKeyDecoder(digest = SHA512).decodeFromByteArray(
            format = RSA.PublicKey.Format.DER,
            bytes = publicKeyBytes,
        )
        val plaintextBytes = message.encodeToByteArray()
        val encryptedMessage = publicKey.encryptor().encrypt(plaintextBytes)
        return Base64.encode(encryptedMessage)
    }

    private suspend fun decryptMessage(message: String, privateKeyString: String): String {
        val provider = CryptographyProvider.Default
        val rsa = provider.get(RSA.OAEP)
        val privateKeyBytes = Base64.decode(privateKeyString)
        val privateKey = rsa.privateKeyDecoder(digest = SHA512).decodeFromByteArray(
            format = RSA.PrivateKey.Format.DER,
            bytes = privateKeyBytes,
        )
        val messageBytes = Base64.decode(message)
        return privateKey.decryptor().decrypt(messageBytes).decodeToString()
    }

}