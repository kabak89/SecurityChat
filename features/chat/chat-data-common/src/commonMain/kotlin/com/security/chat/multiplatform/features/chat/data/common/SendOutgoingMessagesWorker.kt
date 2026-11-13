package com.security.chat.multiplatform.features.chat.data.common

import com.github.michaelbull.result.coroutines.runSuspendCatching
import com.github.michaelbull.result.onErr
import com.security.chat.multiplatform.common.background.work.BackgroundWorker
import com.security.chat.multiplatform.common.core.error.NetworkError
import com.security.chat.multiplatform.common.core.files.FileManager
import com.security.chat.multiplatform.common.log.Log
import com.security.chat.multiplatform.features.chat.data.network.ChatNetworkManager
import com.security.chat.multiplatform.features.chat.data.network.entity.network.ImageMessageRequest
import com.security.chat.multiplatform.features.chat.data.network.entity.network.RecipientCiphertext
import com.security.chat.multiplatform.features.chat.data.network.entity.network.TextMessageRequest
import com.security.chat.multiplatform.features.chat.data.storage.ChatStorage
import com.security.chat.multiplatform.features.chat.data.storage.entity.MessageSM
import com.security.chat.multiplatform.features.chat.data.storage.entity.Status
import com.security.chat.multiplatform.features.user.data.storage.UserStorage
import com.security.chat.multiplatform.features.users.data.network.UsersNetworkManager
import com.security.chat.multiplatform.features.users.data.storage.UsersStorage
import com.security.chat.multiplatform.features.users.data.storage.entity.UserSM
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class SendOutgoingMessagesWorker(
    private val chatStorage: ChatStorage,
    private val userStorage: UserStorage,
    private val usersStorage: UsersStorage,
    private val usersNetworkManager: UsersNetworkManager,
    private val chatNetworkManager: ChatNetworkManager,
    private val chatDataHelper: ChatDataHelper,
    private val fileManager: FileManager,
    private val scheduler: OutgoingMessageScheduler,
) : BackgroundWorker {

    override suspend fun run(input: Map<String, String>) {
        val userId = input[OUTGOING_MESSAGES_USER_ID] ?: return
        if (userStorage.getUserId() != userId) return

        val messages = try {
            chatStorage.migrateMessageStatuses(userId)
            chatStorage.getUnsentMessages(userId)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.e(error, "Could not read outgoing messages")
            return
        }
        for (pending in messages) {
            runSuspendCatching {
                scheduler.sendDeleteMutex.withLock {
                    if (userStorage.getUserId() != userId) return
                    val deletionPending = chatStorage.isMessageDeletionPending(
                        chatId = pending.chatId,
                        messageId = pending.id,
                        authorId = userId,
                    )
                    if (deletionPending) return@withLock
                    val message = chatStorage.getMessage(
                        chatId = pending.chatId,
                        messageId = pending.id,
                    ) ?: return@withLock
                    if (message.authorId != userId ||
                        message.recipients.none { it.status == Status.Created }
                    ) {
                        return@withLock
                    }
                    send(message)
                }
            }.onErr { error ->
                if (error is Error) throw error
                Log.e(error, "Could not send outgoing message: id=${pending.id}")
            }
        }
    }

    private suspend fun send(message: MessageSM) {
        val isReply = message.replyToMessageId != null
        val key = when (message) {
            is MessageSM.Text -> chatDataHelper.getOneTimeEncryptionKey()
            is MessageSM.Image -> message.key
        }
        val storedRecipientIds = message.recipients.map { it.userId }
        val recipientIds = if (isReply) {
            (storedRecipientIds + message.authorId).distinct()
        } else {
            storedRecipientIds
        }
        val recipients = recipientIds.mapNotNull { recipientId ->
            val publicKey = try {
                resolveRecipientPublicKey(recipientId)
            } catch (error: NetworkError) {
                if (isReply && error.statusCode == HttpStatusCode.NotFound.value) {
                    return@mapNotNull null
                }
                throw error
            }
            RecipientCiphertext(
                recipientId = recipientId,
                key = chatDataHelper.encryptKey(key = key, publicKeyString = publicKey),
            )
        }

        val encryptedFilePath = (message as? MessageSM.Image)?.let {
            val directory = fileManager.getDataDirectoryPath(FileManager.ENCRYPTED_IMAGES_FOLDER)
            "$directory/${it.fileId}"
        }
        val request = when (message) {
            is MessageSM.Text -> TextMessageRequest(
                id = message.id,
                chatId = message.chatId,
                timestamp = message.timestamp,
                replyToMessageId = message.replyToMessageId,
                recipients = recipients,
                message = chatDataHelper.encryptText(text = message.text, key = key),
            )

            is MessageSM.Image -> {
                try {
                    chatNetworkManager.uploadFile(
                        fileId = message.fileId,
                        path = checkNotNull(encryptedFilePath),
                    )
                } catch (error: NetworkError) {
                    if (error.statusCode != HttpStatusCode.Conflict.value) throw error
                }
                ImageMessageRequest(
                    id = message.id,
                    chatId = message.chatId,
                    timestamp = message.timestamp,
                    replyToMessageId = message.replyToMessageId,
                    recipients = recipients,
                    fileId = message.fileId,
                )
            }
        }
        val acceptedRecipientIds = chatNetworkManager.sendMessage(request)
        withContext(NonCancellable) {
            chatStorage.markMessageSent(
                id = message.id,
                acceptedRecipientIds = acceptedRecipientIds,
            )
            if (encryptedFilePath != null) {
                fileManager.deleteFile(path = encryptedFilePath)
            }
        }
    }

    private suspend fun resolveRecipientPublicKey(recipientId: String): String {
        usersStorage.getUser(recipientId)?.let { return it.publicKey }
        val user = usersNetworkManager.getUser(recipientId)
        usersStorage.saveUser(
            user = UserSM(id = user.userId, publicKey = user.publicKey, name = user.name),
        )
        return user.publicKey
    }
}
