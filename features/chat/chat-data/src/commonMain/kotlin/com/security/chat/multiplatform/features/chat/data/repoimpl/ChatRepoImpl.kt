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
import com.security.chat.multiplatform.features.chat.data.entity.SendMessageRequest
import com.security.chat.multiplatform.features.chat.data.mapper.toDomain
import com.security.chat.multiplatform.features.chat.data.mapper.toSM
import com.security.chat.multiplatform.features.chat.data.network.ChatNetworkManager
import com.security.chat.multiplatform.features.chat.data.paging.MessagesPagingSource
import com.security.chat.multiplatform.features.chat.data.storage.ChatStorage
import com.security.chat.multiplatform.features.chat.data.storage.entity.MessageSM
import com.security.chat.multiplatform.features.chat.domain.entity.Interlocutor
import com.security.chat.multiplatform.features.chat.domain.entity.Message
import com.security.chat.multiplatform.features.chat.domain.repo.ChatRepo
import com.security.chat.multiplatform.features.chats.data.storage.ChatsStorage
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

    @OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
    override suspend fun saveMessage(
        message: String,
        chatId: String,
    ) {
        val timestamp = timeProvider.now().toEpochMilliseconds()

        val messageSM = MessageSM(
            id = Uuid.random().toString(),
            chatId = chatId,
            text = message,
            authorId = checkNotNull(userStorage.getUserId()),
            status = MessageSM.Status.Created,
            timestamp = timestamp,
        )
        chatStorage.saveMessage(messageSM)
    }

    override suspend fun uploadMessages(chatId: String) {
        val authorId = checkNotNull(userStorage.getUserId())
        val chat = checkNotNull(chatsStorage.getChat(chatId))
        val companionId = chat.interlocutorId

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
                val encryptedText = chatDataHelper.encryptText(
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
                    message.toDomain(appOwnerId = userId)
                }
            }
    }

    override suspend fun subscribeToNewMessages(chatId: String) {
        val authorId = requireNotNull(userStorage.getUserId())
        val privateKey = checkNotNull(userStorage.getKeys()?.privateKey)
        val userId = checkNotNull(userStorage.getUserId())

        chatNetworkManager.getNewMessagesFlow(
            chatId = chatId,
            authorId = authorId,
        )
            .collect { chatMessage ->
                val newMessage = chatMessage.toDomain(
                    decryptMessage = { encryptedText ->
                        chatDataHelper.decryptText(
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

    override suspend fun fetchCompanionInfo(chatId: String) {
        val chat = requireNotNull(chatsStorage.getChat(chatId))
        val companionId = chat.interlocutorId
        val userNM = usersNetworkManager.getUser(companionId)
        usersStorage.saveUser(user = userNM.toSM())
    }

    override fun getInterlocutorInfoFlow(chatId: String): Flow<Interlocutor?> {
        return chatsStorage.getChatFlow(chatId)
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