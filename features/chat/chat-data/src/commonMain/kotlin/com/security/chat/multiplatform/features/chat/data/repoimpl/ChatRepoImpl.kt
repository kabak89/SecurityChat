package com.security.chat.multiplatform.features.chat.data.repoimpl

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.github.michaelbull.result.coroutines.runSuspendCatching
import com.github.michaelbull.result.onErr
import com.security.chat.multiplatform.common.core.error.NetworkError
import com.security.chat.multiplatform.common.core.files.FileManager
import com.security.chat.multiplatform.common.core.files.error.TranscodeException
import com.security.chat.multiplatform.common.core.network.LiveEventsManager
import com.security.chat.multiplatform.common.core.network.NetworkManager
import com.security.chat.multiplatform.common.core.network.NetworkManagerFactory
import com.security.chat.multiplatform.common.core.network.entity.NetworkConfig
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.common.core.time.TimeProvider
import com.security.chat.multiplatform.common.log.Log
import com.security.chat.multiplatform.features.chat.data.common.ChatDataHelper
import com.security.chat.multiplatform.features.chat.data.entity.FindUserResponse
import com.security.chat.multiplatform.features.chat.data.entity.ImageMessageRequest
import com.security.chat.multiplatform.features.chat.data.entity.OnlineInfoMessage
import com.security.chat.multiplatform.features.chat.data.entity.OnlineStatusPublisherMessage
import com.security.chat.multiplatform.features.chat.data.entity.OnlineStatusSubscribeMessage
import com.security.chat.multiplatform.features.chat.data.entity.RecipientCiphertext
import com.security.chat.multiplatform.features.chat.data.entity.SendMessageRequest
import com.security.chat.multiplatform.features.chat.data.entity.TextMessageRequest
import com.security.chat.multiplatform.features.chat.data.mapper.toDomain
import com.security.chat.multiplatform.features.chat.data.mapper.toSM
import com.security.chat.multiplatform.features.chat.data.network.ChatNetworkManager
import com.security.chat.multiplatform.features.chat.data.network.entity.ChatMessageNM
import com.security.chat.multiplatform.features.chat.data.paging.MessagesPagingSource
import com.security.chat.multiplatform.features.chat.data.storage.ChatStorage
import com.security.chat.multiplatform.features.chat.data.storage.entity.MessageSM
import com.security.chat.multiplatform.features.chat.data.storage.entity.Status
import com.security.chat.multiplatform.features.chat.domain.entity.FileDescriptor
import com.security.chat.multiplatform.features.chat.domain.entity.ImageMessageDescriptor
import com.security.chat.multiplatform.features.chat.domain.entity.Interlocutor
import com.security.chat.multiplatform.features.chat.domain.entity.Message
import com.security.chat.multiplatform.features.chat.domain.entity.MessageAuthor
import com.security.chat.multiplatform.features.chat.domain.entity.PickedImage
import com.security.chat.multiplatform.features.chat.domain.entity.error.NotImageError
import com.security.chat.multiplatform.features.chat.domain.entity.toFileSource
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
import kotlinx.serialization.json.Json
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
    private val fileManager: FileManager,
    private val json: Json,
) : ChatRepo {

    private val networkManager: NetworkManager by lazy {
        networkManagerFactory.build(
            baseUrl = "${networkConfig.host}:${networkConfig.port}",
            needAuthorization = true,
        )
    }

    override suspend fun saveTextMessage(
        message: String,
        chatId: String,
    ) {
        val chat = chatsStorage.getPersonalChat(chatId) ?: chatsStorage.getGroupChat(chatId)
        checkNotNull(chat)

        val userId = checkNotNull(userStorage.getUserId())

        val recipients = when (chat) {
            is ChatSM.GroupChat -> (chat.members + chat.authorId).distinct()
            is ChatSM.PersonalChat -> listOf(chat.interlocutorId, userId).distinct()
        }

        val timestamp = timeProvider.now().toEpochMilliseconds()
        val messageId = Uuid.random().toString()

        val messageSm = MessageSM.Text(
            id = messageId,
            chatId = chatId,
            text = message,
            authorId = userId,
            status = Status.Created,
            timestamp = timestamp,
            recipients = recipients,
        )

        chatStorage.saveMessage(messageSm)
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun saveImageMessage(
        chatId: String,
        message: ImageMessageDescriptor,
    ) {
        val userId = checkNotNull(userStorage.getUserId())
        val timestamp = timeProvider.now().toEpochMilliseconds()
        val messageId = Uuid.random().toString()

        val messageSm = MessageSM.Image(
            id = messageId,
            chatId = chatId,
            recipients = message.recipients,
            authorId = userId,
            status = Status.Created,
            timestamp = timestamp,
            fileId = message.fileId,
            key = message.key,
            isDownloaded = true,
        )

        chatStorage.saveMessage(messageSm)
    }

    override suspend fun uploadMessages(chatId: String) {
        val messagesToUpload = chatStorage.getMessages(
            chatId = chatId,
            limit = Long.MAX_VALUE,
            offset = 0,
        )
            .filter { it.status == Status.Created }

        messagesToUpload.forEach { message ->
            val sendMessageRequest = when (message) {
                is MessageSM.Text -> {
                    val key = chatDataHelper.getOneTimeEncryptionKey()
                    val encryptedText = chatDataHelper.encryptText(
                        text = message.text,
                        key = key,
                    )
                    val recipients = buildRecipientCipherTexts(
                        recipients = message.recipients,
                        key = key,
                    )
                    val textMessageRequest = TextMessageRequest(
                        id = message.id,
                        chatId = chatId,
                        timestamp = message.timestamp,
                        recipients = recipients,
                        message = encryptedText,
                    )
                    SendMessageRequest(
                        type = "text",
                        message = json.encodeToString(textMessageRequest),
                    )
                }

                is MessageSM.Image -> {
                    uploadImageFile(fileId = message.fileId)
                    val recipients = buildRecipientCipherTexts(
                        recipients = message.recipients,
                        key = message.key,
                    )
                    val imageMessageRequest = ImageMessageRequest(
                        id = message.id,
                        chatId = chatId,
                        timestamp = message.timestamp,
                        recipients = recipients,
                        fileId = message.fileId,
                    )
                    SendMessageRequest(
                        type = "image",
                        message = json.encodeToString(imageMessageRequest),
                    )
                }
            }

            networkManager.runPost<SendMessageRequest, Unit>(
                relativePath = "/messages",
                request = sendMessageRequest,
            )

            when (message) {
                is MessageSM.Text -> chatStorage.updateMessage(message.copy(status = Status.Sent))
                is MessageSM.Image -> {
                    chatStorage.updateMessage(message.copy(status = Status.Sent))
                    /** The encrypted copy is uploaded already and no longer needed locally. */
                    deleteEncryptedImageFile(fileId = message.fileId)
                }
            }
        }
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
                val imagesDirectoryPath = fileManager.getImagesDirectoryPath()
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
                        imagesDirectoryPath = imagesDirectoryPath,
                    )
                }
            }
    }

    override suspend fun subscribeToNewMessages(chatId: String) {
        val authorId = requireNotNull(userStorage.getUserId())
        val deviceId = requireNotNull(userStorage.getDeviceId())

        chatNetworkManager.getNewMessagesFlow(
            chatId = chatId,
            authorId = authorId,
            deviceId = deviceId,
        )
            .collect { chatMessage ->
                runSuspendCatching {
                    chatDataHelper.processNewMessage(message = chatMessage, chatId = chatId)
                }
                    .onErr { error ->
                        val type = when (chatMessage) {
                            is ChatMessageNM.Text -> "text"
                            is ChatMessageNM.Image -> "image"
                        }
                        val message =
                            "Skipped $type message: id=${chatMessage.id}, " +
                                    "authorId=${chatMessage.authorId}, " +
                                    "timestamp=${chatMessage.timestamp}"

                        Log.e(error, message)
                    }
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

    override suspend fun copyImageToCache(image: PickedImage): FileDescriptor {
        val fileSource = image.toFileSource()

        if (!fileManager.isImage(fileSource)) {
            throw NotImageError()
        }

        val localPath = fileManager.copyToCache(
            fileSource = fileSource,
            directoryName = FileManager.IMAGES_FOLDER,
        )

        if (!fileManager.isRenderable(localPath)) {
            try {
                fileManager.transcodeToJpeg(localPath)
            } catch (error: TranscodeException) {
                Log.e(error)
                fileManager.deleteFile(localPath)
                error("Can not transcode image")
            }
        }

        return FileDescriptor(
            localPath = localPath,
            filename = localPath.substringAfterLast('/'),
        )
    }

    override suspend fun createEncryptedFile(
        file: FileDescriptor,
        chatId: String,
    ): ImageMessageDescriptor {
        val key = chatDataHelper.getOneTimeEncryptionKey()
        val fileId = file.filename
        val encryptedDirectory =
            fileManager.getDataDirectoryPath(FileManager.ENCRYPTED_IMAGES_FOLDER)
        val encryptedFilePath = "$encryptedDirectory/$fileId"

        chatDataHelper.encryptFile(
            sourcePath = file.localPath,
            destinationPath = encryptedFilePath,
            key = key,
        )

        val imagesDirectory = fileManager.getImagesDirectoryPath()
        fileManager.moveFile(
            sourcePath = file.localPath,
            destinationPath = "$imagesDirectory/$fileId",
        )

        val userId = checkNotNull(userStorage.getUserId())
        val chat = chatsStorage.getPersonalChat(chatId) ?: chatsStorage.getGroupChat(chatId)
        checkNotNull(chat)

        val recipients = when (chat) {
            is ChatSM.GroupChat -> (chat.members + chat.authorId).distinct()
            is ChatSM.PersonalChat -> listOf(chat.interlocutorId, userId).distinct()
        }

        return ImageMessageDescriptor(
            fileId = fileId,
            key = key,
            recipients = recipients,
        )
    }

    private suspend fun resolveRecipientPublicKey(recipientId: String): String {
        return usersStorage.getUser(recipientId)?.publicKey ?: run {
            val userInfo = networkManager.runGet<FindUserResponse>(
                relativePath = "/users/info",
                request = mapOf("id" to recipientId),
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
    }

    private suspend fun buildRecipientCipherTexts(
        recipients: List<String>,
        key: String,
    ): List<RecipientCiphertext> {
        return recipients.map { recipient ->
            RecipientCiphertext(
                recipientId = recipient,
                key = chatDataHelper.encryptKey(
                    key = key,
                    publicKeyString = resolveRecipientPublicKey(recipient),
                ),
            )
        }
    }

    private suspend fun uploadImageFile(fileId: String) {
        val encryptedDirectory =
            fileManager.getDataDirectoryPath(FileManager.ENCRYPTED_IMAGES_FOLDER)
        val filePath = "$encryptedDirectory/$fileId"
        try {
            networkManager.runPostFile(
                relativePath = "/files/$fileId",
                filePath = filePath,
            )
        } catch (error: NetworkError) {
            /** 409 means the file already exists on the server, which is fine for retries. */
            if (error.statusCode != 409) {
                throw error
            }
        }
    }

    private suspend fun deleteEncryptedImageFile(fileId: String) {
        val encryptedDirectory =
            fileManager.getDataDirectoryPath(FileManager.ENCRYPTED_IMAGES_FOLDER)
        fileManager.deleteFile(path = "$encryptedDirectory/$fileId")
    }
}

private const val MESSAGES_PAGE_SIZE = 60
private const val MESSAGES_INITIAL_LOAD_SIZE = 60
private const val MESSAGES_PREFETCH_DISTANCE = 20