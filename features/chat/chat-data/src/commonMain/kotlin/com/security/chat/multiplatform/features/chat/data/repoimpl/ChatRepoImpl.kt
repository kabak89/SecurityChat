package com.security.chat.multiplatform.features.chat.data.repoimpl

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.security.chat.multiplatform.common.core.network.LiveEventsManager
import com.security.chat.multiplatform.common.core.network.NetworkManager
import com.security.chat.multiplatform.common.core.network.NetworkManagerFactory
import com.security.chat.multiplatform.common.core.network.entity.NetworkConfig
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.common.core.time.TimeProvider
import com.security.chat.multiplatform.features.chat.data.common.ChatDataHelper
import com.security.chat.multiplatform.features.chat.data.entity.FindUserResponse
import com.security.chat.multiplatform.features.chat.data.entity.MessagesReceivedRequest
import com.security.chat.multiplatform.features.chat.data.entity.OnlineInfoMessage
import com.security.chat.multiplatform.features.chat.data.entity.OnlineStatusPublisherMessage
import com.security.chat.multiplatform.features.chat.data.entity.OnlineStatusSubscribeMessage
import com.security.chat.multiplatform.features.chat.data.entity.RecipientCiphertext
import com.security.chat.multiplatform.features.chat.data.entity.SendMessageRequest
import com.security.chat.multiplatform.features.chat.data.mapper.toDomain
import com.security.chat.multiplatform.features.chat.data.mapper.toSM
import com.security.chat.multiplatform.features.chat.data.network.ChatNetworkManager
import com.security.chat.multiplatform.features.chat.data.paging.MessagesPagingSource
import com.security.chat.multiplatform.features.chat.data.storage.ChatStorage
import com.security.chat.multiplatform.features.chat.data.storage.entity.MessageSM
import com.security.chat.multiplatform.features.chat.domain.entity.Interlocutor
import com.security.chat.multiplatform.features.chat.domain.entity.Message
import com.security.chat.multiplatform.features.chat.domain.entity.MessageAuthor
import com.security.chat.multiplatform.features.chat.domain.repo.ChatRepo
import com.security.chat.multiplatform.features.chats.data.storage.ChatsStorage
import com.security.chat.multiplatform.features.chats.data.storage.entity.ChatSM
import com.security.chat.multiplatform.features.user.data.storage.UserStorage
import com.security.chat.multiplatform.features.users.data.network.UsersNetworkManager
import com.security.chat.multiplatform.features.users.data.storage.UsersStorage
import com.security.chat.multiplatform.features.users.data.storage.entity.UserSM
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
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
    private val usersNetworkManager: UsersNetworkManager,
    private val dispatcherProvider: DispatcherProviderInterface,
    private val chatNetworkManager: ChatNetworkManager,
    private val chatDataHelper: ChatDataHelper,
) : ChatRepo {

    private val networkManager: NetworkManager by lazy {
        networkManagerFactory.build(
            baseUrl = "${networkConfig.host}:${networkConfig.port}",
            needAuthorization = true,
        )
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun saveMessage(
        message: String,
        chatId: String,
    ) {
        val chat = chatsStorage.getPersonalChat(chatId) ?: chatsStorage.getGroupChat(chatId)
        checkNotNull(chat)

        val userId = checkNotNull(userStorage.getUserId())

        val recipients = when (chat) {
            is ChatSM.GroupChat -> chat.members + chat.authorId - userId
            is ChatSM.PersonalChat -> listOf(chat.interlocutorId)
        }

        val timestamp = timeProvider.now().toEpochMilliseconds()
        val messageId = Uuid.random().toString()

        val messageSm = MessageSM(
            id = messageId,
            chatId = chatId,
            text = message,
            authorId = userId,
            status = MessageSM.Status.Created,
            timestamp = timestamp,
            recipients = recipients,
        )

        chatStorage.saveMessage(messageSm)
    }

    override suspend fun uploadMessages(chatId: String) {
        val messagesToUpload = chatStorage.getMessages(
            chatId = chatId,
            limit = Long.MAX_VALUE,
            offset = 0,
        )
            .filter { it.status == MessageSM.Status.Created }

        messagesToUpload.forEach { message ->
            val cipherTexts = message.recipients
                .map { recipient ->
                    val publicKey = usersStorage.getUser(recipient)?.publicKey ?: run {
                        val userInfo = networkManager.runGet<FindUserResponse>(
                            relativePath = "/users/info",
                            request = mapOf("id" to recipient),
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

                    val key = chatDataHelper.getOneTimeEncryptionKey()

                    val encryptedMessage = chatDataHelper.encryptText(
                        text = message.text,
                        publicKeyString = publicKey,
                        key = key,
                    )

                    RecipientCiphertext(
                        recipientId = recipient,
                        message = encryptedMessage.encryptedText,
                        key = encryptedMessage.encryptedKey,
                    )
                }

            val request = SendMessageRequest(
                id = message.id,
                chatId = chatId,
                recipients = cipherTexts,
                timestamp = message.timestamp,
            )

            networkManager.runPost<SendMessageRequest, Unit>(
                relativePath = "/messages",
                request = request,
            )
        }

        val messagesToUpdate = messagesToUpload.map { it.copy(status = MessageSM.Status.Sent) }
        messagesToUpdate.forEach { message -> chatStorage.updateMessage(message) }
    }

    override suspend fun fetchMessages(
        chatId: String,
    ) {
        chatDataHelper.fetchAndSaveMessages(chatId = chatId)
    }

    override fun getMessagesPager(chatId: String): Flow<PagingData<Message>> {
        return Pager(
            config = PagingConfig(
                pageSize = MESSAGES_PAGE_SIZE,
                initialLoadSize = MESSAGES_INITIAL_LOAD_SIZE,
                prefetchDistance = MESSAGES_PREFETCH_DISTANCE,
                enablePlaceholders = false,
            ),
            pagingSourceFactory = {
                MessagesPagingSource(
                    chatId = chatId,
                    chatStorage = chatStorage,
                    dispatcherProvider = dispatcherProvider,
                )
            },
        ).flow
            .map { pagingData ->
                val userId = checkNotNull(userStorage.getUserId())
                pagingData.map { message ->
                    val author = usersStorage.getUser(message.authorId)?.let {
                        MessageAuthor(
                            id = it.id,
                            name = it.name,
                        )
                    } ?: run {
                        val userInfo = networkManager.runGet<FindUserResponse>(
                            relativePath = "/users/info",
                            request = mapOf("id" to message.authorId),
                        )

                        usersStorage.saveUser(
                            user = UserSM(
                                id = userInfo.userId,
                                publicKey = userInfo.publicKey,
                                name = userInfo.login,
                            ),
                        )
                        MessageAuthor(
                            id = userInfo.userId,
                            name = userInfo.login,
                        )
                    }

                    message.toDomain(
                        appOwnerId = userId,
                        author = author,
                    )
                }
            }
    }

    override suspend fun subscribeToNewMessages(chatId: String) {
        val authorId = requireNotNull(userStorage.getUserId())
        val privateKey = checkNotNull(userStorage.getKeys()?.privateKey)

        chatNetworkManager.getNewMessagesFlow(
            chatId = chatId,
            authorId = authorId,
        )
            .collect { chatMessage ->
                val author = usersStorage.getUser(chatMessage.authorId)?.let {
                    MessageAuthor(
                        id = it.id,
                        name = it.name,
                    )
                } ?: run {
                    val userInfo = networkManager.runGet<FindUserResponse>(
                        relativePath = "/users/info",
                        request = mapOf("id" to chatMessage.authorId),
                    )

                    usersStorage.saveUser(
                        user = UserSM(
                            id = userInfo.userId,
                            publicKey = userInfo.publicKey,
                            name = userInfo.login,
                        ),
                    )
                    MessageAuthor(
                        id = userInfo.userId,
                        name = userInfo.login,
                    )
                }

                val newMessage = chatMessage.toDomain(
                    decryptMessage = { encryptedText ->
                        chatDataHelper.decryptText(
                            text = encryptedText,
                            privateKeyString = privateKey,
                            key = chatMessage.key,
                        )
                    },
                    appOwnerId = authorId,
                    author = author,
                )

                val storageModel = newMessage.toSM(
                    chatId = chatId,
                    recipients = listOf(authorId),
                )
                chatStorage.saveMessage(storageModel)

                networkManager.runPost<MessagesReceivedRequest, Unit>(
                    relativePath = "/messages/received",
                    request = MessagesReceivedRequest(
                        chatId = chatId,
                        messageIds = listOf(newMessage.id),
                    ),
                )
            }
    }

    override suspend fun fetchCompanionInfo(chatId: String) {
        val chat = requireNotNull(chatsStorage.getPersonalChat(chatId))
        val companionId = chat.interlocutorId
        val userNM = usersNetworkManager.getUser(companionId)
        usersStorage.saveUser(user = userNM.toSM())
    }

    override fun getInterlocutorInfoFlow(chatId: String): Flow<Interlocutor?> {
        return chatsStorage.getPersonalChatFlow(chatId)
            .flatMapLatest { chat ->
                chat ?: return@flatMapLatest flowOf(null)

                val subscribeMessage = OnlineStatusSubscribeMessage(
                    targetUserId = chat.interlocutorId,
                )

                val onlineStatusFlow = liveEventsManager
                    .subscribe<OnlineInfoMessage, OnlineStatusSubscribeMessage>(
                        subscribeMessage = subscribeMessage,
                        type = "online_status_receive",
                    )
                    .map { it.isOnline }
                    .onStart { emit(false) }

                combine(
                    usersStorage.getUserFlow(chat.interlocutorId),
                    onlineStatusFlow,
                ) { userSM, isOnline ->
                    userSM ?: return@combine null

                    Interlocutor(
                        id = userSM.id,
                        name = userSM.name,
                        isOnline = isOnline,
                    )
                }
                    .distinctUntilChanged()
            }
    }

    override suspend fun setUserOnline() {
        val userId = checkNotNull(userStorage.getUserId())
        val subscribeMessage = OnlineStatusPublisherMessage(
            userId = userId,
        )

        liveEventsManager
            .subscribe<String, OnlineStatusPublisherMessage>(
                subscribeMessage = subscribeMessage,
                type = "online_status_publish",
            )
            .collect {
                //no messages expected
            }
    }
}

private const val MESSAGES_PAGE_SIZE = 60
private const val MESSAGES_INITIAL_LOAD_SIZE = 60
private const val MESSAGES_PREFETCH_DISTANCE = 20