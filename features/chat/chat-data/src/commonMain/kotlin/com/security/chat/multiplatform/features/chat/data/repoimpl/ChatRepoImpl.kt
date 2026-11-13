package com.security.chat.multiplatform.features.chat.data.repoimpl

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.github.michaelbull.result.coroutines.runSuspendCatching
import com.github.michaelbull.result.onErr
import com.security.chat.multiplatform.common.core.files.FileManager
import com.security.chat.multiplatform.common.core.files.error.TranscodeException
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.common.core.time.TimeProvider
import com.security.chat.multiplatform.common.log.Log
import com.security.chat.multiplatform.features.chat.data.common.ChatDataHelper
import com.security.chat.multiplatform.features.chat.data.common.ChatEventsPublisher
import com.security.chat.multiplatform.features.chat.data.common.ChatPresenceHelper
import com.security.chat.multiplatform.features.chat.data.common.OutgoingMessageScheduler
import com.security.chat.multiplatform.features.chat.data.mapper.toDomain
import com.security.chat.multiplatform.features.chat.data.network.ChatNetworkManager
import com.security.chat.multiplatform.features.chat.data.network.entity.ChatEventNM
import com.security.chat.multiplatform.features.chat.data.network.entity.ChatMessageNM
import com.security.chat.multiplatform.features.chat.data.paging.MessagesPagingSource
import com.security.chat.multiplatform.features.chat.data.paging.toPosition
import com.security.chat.multiplatform.features.chat.data.storage.ChatStorage
import com.security.chat.multiplatform.features.chat.data.storage.entity.MessageRecipientSM
import com.security.chat.multiplatform.features.chat.data.storage.entity.MessageSM
import com.security.chat.multiplatform.features.chat.data.storage.entity.Status
import com.security.chat.multiplatform.features.chat.domain.entity.ChatInfo
import com.security.chat.multiplatform.features.chat.domain.entity.FileDescriptor
import com.security.chat.multiplatform.features.chat.domain.entity.ImageMessageDescriptor
import com.security.chat.multiplatform.features.chat.domain.entity.Message
import com.security.chat.multiplatform.features.chat.domain.entity.MessageAuthor
import com.security.chat.multiplatform.features.chat.domain.entity.PickedImage
import com.security.chat.multiplatform.features.chat.domain.entity.error.MessageDeletionError
import com.security.chat.multiplatform.features.chat.domain.entity.error.NotImageError
import com.security.chat.multiplatform.features.chat.domain.entity.toFileSource
import com.security.chat.multiplatform.features.chat.domain.repo.ChatRepo
import com.security.chat.multiplatform.features.chats.data.common.ChatsDataHelper
import com.security.chat.multiplatform.features.chats.data.storage.ChatsStorage
import com.security.chat.multiplatform.features.user.data.storage.UserStorage
import com.security.chat.multiplatform.features.users.data.network.UsersNetworkManager
import com.security.chat.multiplatform.features.users.data.storage.UsersStorage
import com.security.chat.multiplatform.features.users.data.storage.entity.UserSM
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.uuid.Uuid

internal class ChatRepoImpl(
    private val usersNetworkManager: UsersNetworkManager,
    private val userStorage: UserStorage,
    private val usersStorage: UsersStorage,
    private val chatsStorage: ChatsStorage,
    private val chatStorage: ChatStorage,
    private val timeProvider: TimeProvider,
    private val dispatcherProvider: DispatcherProviderInterface,
    private val chatNetworkManager: ChatNetworkManager,
    private val chatDataHelper: ChatDataHelper,
    private val fileManager: FileManager,
    private val chatsDataHelper: ChatsDataHelper,
    private val chatPresenceHelper: ChatPresenceHelper,
    private val chatEventsPublisher: ChatEventsPublisher,
    private val outgoingMessageScheduler: OutgoingMessageScheduler,
) : ChatRepo {

    override suspend fun saveTextMessage(
        message: String,
        chatId: String,
        replyToMessageId: String?,
    ) {
        val chat = checkNotNull(chatsStorage.getChat(chatId))
        val userId = checkNotNull(userStorage.getUserId())
        val recipients = (chat.members + chat.authorId).distinct()

        val timestamp = timeProvider.now().toEpochMilliseconds()
        val messageId = Uuid.random().toString()

        val messageSm = MessageSM.Text(
            id = messageId,
            chatId = chatId,
            text = message,
            authorId = userId,
            timestamp = timestamp,
            replyToMessageId = replyToMessageId,
            recipients = recipients.map {
                MessageRecipientSM(userId = it, status = Status.Created)
            },
        )

        outgoingMessageScheduler.saveAndSchedule(messageSm)
    }

    override suspend fun saveImageMessage(
        chatId: String,
        message: ImageMessageDescriptor,
        replyToMessageId: String?,
    ) {
        val userId = checkNotNull(userStorage.getUserId())
        val timestamp = timeProvider.now().toEpochMilliseconds()
        val messageId = Uuid.random().toString()

        val messageSm = MessageSM.Image(
            id = messageId,
            chatId = chatId,
            recipients = message.recipients.map {
                MessageRecipientSM(userId = it, status = Status.Created)
            },
            authorId = userId,
            timestamp = timestamp,
            replyToMessageId = replyToMessageId,
            fileId = message.fileId,
            key = message.key,
            isDownloaded = true,
        )

        outgoingMessageScheduler.saveAndSchedule(messageSm)
    }

    override suspend fun markMessagesReadUpTo(chatId: String, messageId: String) {
        chatDataHelper.migrateMessageStatuses()
        val userId = checkNotNull(userStorage.getUserId())
        chatStorage.markMessagesReadUpTo(
            chatId = chatId,
            messageId = messageId,
            userId = userId,
        )
        chatEventsPublisher.start(chatId)
    }

    override suspend fun fetchMessages(
        chatId: String,
    ) {
        chatDataHelper.fetchAndSaveMessages(chatId = chatId)
    }

    override fun getMessagesPager(
        chatId: String,
        initialMessageId: String?,
    ): Flow<PagingData<Message>> {
        return flow {
            chatDataHelper.migrateMessageStatuses()
            val initialKey = initialMessageId?.let {
                chatStorage.getMessage(chatId = chatId, messageId = it)?.toPosition()
            }
            var activeSource: MessagesPagingSource? = null
            val pager = Pager(
                config = PagingConfig(
                    pageSize = MESSAGES_PAGE_SIZE,
                    initialLoadSize = MESSAGES_INITIAL_LOAD_SIZE,
                    prefetchDistance = MESSAGES_PREFETCH_DISTANCE,
                    enablePlaceholders = false,
                ),
                initialKey = initialKey,
                pagingSourceFactory = {
                    MessagesPagingSource(
                        chatId = chatId,
                        chatStorage = chatStorage,
                        dispatcherProvider = dispatcherProvider,
                        initialPosition = initialKey,
                    ).also { activeSource = it }
                },
            )
            try {
                emitAll(pager.flow)
            } finally {
                activeSource?.invalidate()
            }
        }.map { pagingData ->
            val userId = checkNotNull(userStorage.getUserId())
            val imagesDirectoryPath = fileManager.getImagesDirectoryPath()
            pagingData.map { (message, quotedMessage) ->
                val author = usersStorage.getUser(message.authorId)?.let {
                    MessageAuthor(id = it.id, name = it.name)
                } ?: fetchMessageAuthor(message.authorId)

                message.toDomain(
                    appOwnerId = userId,
                    author = author,
                    imagesDirectoryPath = imagesDirectoryPath,
                    quotedMessage = quotedMessage,
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

    override suspend fun setUserOnline() {
        chatPresenceHelper.setUserOnline()
    }

    override suspend fun subscribeToEvents(chatId: String) {
        val userId = requireNotNull(userStorage.getUserId())
        chatDataHelper.migrateMessageStatuses()
        chatDataHelper.restoreOutgoingMessageRecipients(chatId)
        chatEventsPublisher.start(chatId)
        chatNetworkManager.getEventsFlow(chatId).collect { event ->
            runSuspendCatching {
                if (event.chatId != chatId) {
                    Log.e(
                        "Skipped chat event from another chat: " +
                                "id=${event.id}, chatId=${event.chatId}",
                    )
                    return@collect
                }
                val processed = when (event) {
                    is ChatEventNM.MessageDeleted -> {
                        chatDataHelper.deleteMessage(
                            chatId = chatId,
                            messageId = event.messageId,
                            deletedByUserId = event.deletedByUserId,
                        )
                        true
                    }
                    is ChatEventNM.MessageReceived -> {
                        chatStorage.markMessageReceived(
                            chatId = chatId,
                            messageId = event.messageId,
                            authorId = userId,
                            receivedByUserId = event.receivedByUserId,
                        )
                    }

                    is ChatEventNM.MessageRead -> {
                        chatStorage.markMessageRead(
                            chatId = chatId,
                            messageId = event.messageId,
                            authorId = userId,
                            readByUserId = event.readByUserId,
                        )
                    }
                }
                if (processed || chatStorage.isMessageDeleted(
                        chatId = chatId,
                        messageId = event.messageId,
                        authorId = userId,
                    )
                ) {
                    chatNetworkManager.acknowledgeEvent(event)
                } else {
                    Log.e(
                        "Skipped chat event without a matching outgoing message recipient: " +
                                "id=${event.id}",
                    )
                }
            }
                .onErr { error ->
                    Log.e(error, "Failed to process chat event: id=${event.id}")
                }
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
            runSuspendCatching {
                fileManager.transcodeToJpeg(localPath)
            }
                .onErr { error ->
                    if (error is TranscodeException) {
                        Log.e(error)
                        fileManager.deleteFile(localPath)
                        error("Can not transcode image")
                    } else {
                        throw error
                    }
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

        val chat = checkNotNull(chatsStorage.getChat(chatId))
        val recipients = (chat.members + chat.authorId).distinct()

        return ImageMessageDescriptor(
            fileId = fileId,
            key = key,
            recipients = recipients,
        )
    }

    override fun getChatInfoFlow(chatId: String): Flow<ChatInfo?> {
        return combine(
            chatsDataHelper.getChatInfoFlow(chatId),
            chatPresenceHelper.getOnlineParticipantIdsFlow(chatId),
        ) { chatInfo, onlineParticipantIds ->
            chatInfo ?: return@combine null

            ChatInfo(
                totalMembersCount = chatInfo.participantIds.size + 1,
                onlineCount = onlineParticipantIds.size + 1,
            )
        }
    }

    override suspend fun deleteMessage(chatId: String, messageId: String) {
        outgoingMessageScheduler.sendDeleteMutex.withLock {
            val userId = requireNotNull(userStorage.getUserId())
            val message = chatStorage.getMessage(chatId = chatId, messageId = messageId) ?: return
            if (message.authorId != userId) throw MessageDeletionError()
            val enqueued = chatStorage.enqueueMessageDeletion(
                chatId = chatId,
                messageId = messageId,
                authorId = userId,
            )
            if (!enqueued) return
            chatNetworkManager.deleteMessage(chatId = chatId, messageId = messageId)
            withContext(NonCancellable) {
                chatDataHelper.deleteMessage(
                    chatId = chatId,
                    messageId = messageId,
                    deletedByUserId = userId,
                )
            }
        }
    }

    private suspend fun fetchMessageAuthor(authorId: String): MessageAuthor {
        val userInfo = usersNetworkManager.getUser(authorId)
        usersStorage.saveUser(
            user = UserSM(
                id = userInfo.userId,
                publicKey = userInfo.publicKey,
                name = userInfo.name,
            ),
        )
        return MessageAuthor(id = userInfo.userId, name = userInfo.name)
    }
}

private const val MESSAGES_PAGE_SIZE = 60
private const val MESSAGES_INITIAL_LOAD_SIZE = 60
private const val MESSAGES_PREFETCH_DISTANCE = 20
