package com.security.chat.multiplatform.features.chat.data.common

import com.github.michaelbull.result.coroutines.runSuspendCatching
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.getOrThrow
import com.github.michaelbull.result.onErr
import com.security.chat.multiplatform.common.core.files.FileManager
import com.security.chat.multiplatform.common.core.localization.StringRes
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.common.log.Log
import com.security.chat.multiplatform.features.chat.data.common.mapper.toSM
import com.security.chat.multiplatform.features.chat.data.network.ChatNetworkManager
import com.security.chat.multiplatform.features.chat.data.network.entity.ChatMessageNM
import com.security.chat.multiplatform.features.chat.data.storage.ChatStorage
import com.security.chat.multiplatform.features.chat.data.storage.entity.MessageSM
import com.security.chat.multiplatform.features.user.data.storage.UserStorage
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.AES
import dev.whyoleg.cryptography.algorithms.RSA
import dev.whyoleg.cryptography.algorithms.SHA512
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import org.jetbrains.compose.resources.getString
import securitychat.common.localization.generated.resources.push_stub_image
import kotlin.io.encoding.Base64

public interface ChatDataHelper {
    public suspend fun fetchAndSaveMessages(chatId: String)
    public suspend fun processPushPayload(
        serializedMessages: String,
        chatId: String,
    ): List<String>

    public suspend fun processNewMessage(
        message: ChatMessageNM,
        chatId: String,
    )

    public suspend fun decryptText(
        text: String,
        privateKeyString: String,
        key: String,
    ): String

    public suspend fun decryptKey(
        key: String,
        privateKeyString: String,
    ): String

    public suspend fun encryptText(
        text: String,
        key: String,
    ): String

    public suspend fun encryptFile(
        sourcePath: String,
        destinationPath: String,
        key: String,
    )

    public suspend fun decryptFile(
        sourcePath: String,
        destinationPath: String,
        key: String,
    )

    public suspend fun encryptKey(
        key: String,
        publicKeyString: String,
    ): String

    public suspend fun getOneTimeEncryptionKey(): String
}

internal class ChatDataHelperImpl(
    private val userStorage: UserStorage,
    private val chatNetworkManager: ChatNetworkManager,
    private val chatStorage: ChatStorage,
    private val fileManager: FileManager,
    private val dispatcherProvider: DispatcherProviderInterface,
) : ChatDataHelper {

    private val downloadLocks: MutableMap<String, DownloadLock> = mutableMapOf()
    private val downloadLocksGuard: Mutex = Mutex()

    override suspend fun fetchAndSaveMessages(chatId: String) {
        val messages = chatNetworkManager.getMessages(chatId = chatId)
        saveAndConfirmReceivingOfMessages(messages = messages, chatId = chatId)
    }

    override suspend fun processPushPayload(
        serializedMessages: String,
        chatId: String,
    ): List<String> {
        val messages = chatNetworkManager.processNewMessages(serializedMessages)
        val storedMessages = saveAndConfirmReceivingOfMessages(messages = messages, chatId = chatId)

        return storedMessages.map { message ->
            when (message) {
                is MessageSM.Text -> message.text
                is MessageSM.Image -> getString(StringRes.push_stub_image)
            }
        }
    }

    override suspend fun processNewMessage(
        message: ChatMessageNM,
        chatId: String,
    ) {
        saveAndConfirmReceivingOfMessages(messages = listOf(message), chatId = chatId)
    }

    override suspend fun decryptText(
        text: String,
        privateKeyString: String,
        key: String,
    ): String {
        return withContext(dispatcherProvider.Default) {
            val rawAesKey = Base64.decode(
                decryptKey(key = key, privateKeyString = privateKeyString),
            )

            val aesGcm = CryptographyProvider.Default.get(AES.GCM)
            val aesKey = aesGcm.keyDecoder().decodeFromByteArray(
                format = AES.Key.Format.RAW,
                bytes = rawAesKey,
            )

            val ciphertextBytes = Base64.decode(text)

            return@withContext runSuspendCatching {
                aesKey.cipher().decrypt(ciphertext = ciphertextBytes).decodeToString()
            }
                .onErr {
                    Log.e(
                        "AES-GCM decrypt failed: aesKeyBytes=${rawAesKey.size}, " +
                                "ciphertextBytes=${ciphertextBytes.size}, " +
                                "wrappedKeyBytes=${Base64.decode(key).size}",
                    )
                }
                .getOrThrow()
        }
    }

    override suspend fun decryptKey(
        key: String,
        privateKeyString: String,
    ): String {
        return withContext(dispatcherProvider.Default) {
            val rsa = CryptographyProvider.Default.get(RSA.OAEP)
            val privateKeyBytes = Base64.decode(privateKeyString)
            val privateKey = rsa.privateKeyDecoder(digest = SHA512).decodeFromByteArray(
                format = RSA.PrivateKey.Format.DER,
                bytes = privateKeyBytes,
            )

            val encryptedAesKeyBytes = Base64.decode(key)
            val rawAesKey = privateKey.decryptor().decrypt(encryptedAesKeyBytes)

            Base64.encode(rawAesKey)
        }
    }

    override suspend fun encryptText(
        text: String,
        key: String,
    ): String {
        return withContext(dispatcherProvider.Default) {
            val rawAesKey = Base64.decode(key)
            val aesGcm = CryptographyProvider.Default.get(AES.GCM)

            val aesKey = aesGcm.keyDecoder().decodeFromByteArray(
                format = AES.Key.Format.RAW,
                bytes = rawAesKey,
            )

            val encryptedTextBytes = aesKey.cipher().encrypt(plaintext = text.encodeToByteArray())
            Base64.encode(encryptedTextBytes)
        }
    }

    override suspend fun encryptFile(
        sourcePath: String,
        destinationPath: String,
        key: String,
    ) {
        withContext(dispatcherProvider.IO) {
            val provider = CryptographyProvider.Default
            val rawAesKey = Base64.decode(key)
            val aesGcm = provider.get(AES.GCM)
            val aesKey = aesGcm.keyDecoder().decodeFromByteArray(
                format = AES.Key.Format.RAW,
                bytes = rawAesKey,
            )
            val cipher = aesKey.cipher()

            SystemFileSystem.source(Path(sourcePath)).use { fileSource ->
                val encryptingSource = cipher.encryptingSource(fileSource)
                SystemFileSystem.sink(Path(destinationPath)).buffered().use { out ->
                    encryptingSource.buffered().transferTo(out)
                }
            }
        }
    }

    override suspend fun decryptFile(
        sourcePath: String,
        destinationPath: String,
        key: String,
    ) {
        withContext(dispatcherProvider.IO) {
            val provider = CryptographyProvider.Default
            val rawAesKey = Base64.decode(key)
            val aesGcm = provider.get(AES.GCM)
            val aesKey = aesGcm.keyDecoder().decodeFromByteArray(
                format = AES.Key.Format.RAW,
                bytes = rawAesKey,
            )
            val cipher = aesKey.cipher()

            SystemFileSystem.source(Path(sourcePath)).use { fileSource ->
                val decryptingSource = cipher.decryptingSource(fileSource)
                SystemFileSystem.sink(Path(destinationPath)).buffered().use { out ->
                    decryptingSource.buffered().transferTo(out)
                }
            }
        }
    }

    override suspend fun encryptKey(
        key: String,
        publicKeyString: String,
    ): String {
        return withContext(dispatcherProvider.Default) {
            val provider = CryptographyProvider.Default
            val rsa = provider.get(RSA.OAEP)
            val publicKey = rsa.publicKeyDecoder(digest = SHA512).decodeFromByteArray(
                format = RSA.PublicKey.Format.DER,
                bytes = Base64.decode(publicKeyString),
            )
            val encryptedKeyBytes = publicKey.encryptor().encrypt(Base64.decode(key))
            Base64.encode(encryptedKeyBytes)
        }
    }

    override suspend fun getOneTimeEncryptionKey(): String {
        return withContext(dispatcherProvider.Default) {
            val aesGcm = CryptographyProvider.Default.get(AES.GCM)
            val aesKey = aesGcm.keyGenerator(AES.Key.Size.B256).generateKey()
            val rawAesKey = aesKey.encodeToByteArray(AES.Key.Format.RAW)
            Base64.encode(rawAesKey)
        }
    }

    private suspend fun saveAndConfirmReceivingOfMessages(
        messages: List<ChatMessageNM>,
        chatId: String,
    ): List<MessageSM> {
        if (messages.isEmpty()) return emptyList()

        val privateKey = checkNotNull(userStorage.getKeys()?.privateKey)
        val recipients = listOf(checkNotNull(userStorage.getUserId()))

        val messagesToStore = messages.mapNotNull {
            it.toSMOrNull(
                chatId = chatId,
                privateKey = privateKey,
                recipients = recipients,
            )
        }

        chatStorage.saveMessages(messages = messagesToStore)
        val messageIds = messagesToStore.map { it.id }

        confirmReceivingMessages(
            chatId = chatId,
            messageIds = messageIds,
        )

        return messagesToStore
    }

    private suspend fun confirmReceivingMessages(
        chatId: String,
        messageIds: List<String>,
    ) {
        if (messageIds.isEmpty()) return

        chatNetworkManager.confirmReceivingMessages(
            chatId = chatId,
            messageIds = messageIds,
        )
    }

    private suspend fun downloadImage(message: MessageSM.Image) {
        withDownloadLock(message.fileId) {
            val imagesDirectory = fileManager.getImagesDirectoryPath()
            val destinationPath = "$imagesDirectory/${message.fileId}"
            if (fileManager.fileExists(destinationPath)) return@withDownloadLock

            val stagingDirectory = fileManager.getCacheDirectoryPath(FileManager.DOWNLOADS_FOLDER)
            val encryptedPath = "$stagingDirectory/${message.fileId}$ENCRYPTED_SUFFIX"
            val decryptedPath = "$stagingDirectory/${message.fileId}"

            try {
                chatNetworkManager.downloadFile(
                    fileId = message.fileId,
                    destinationPath = encryptedPath,
                )

                decryptFile(
                    sourcePath = encryptedPath,
                    destinationPath = decryptedPath,
                    key = message.key,
                )

                fileManager.moveFile(
                    sourcePath = decryptedPath,
                    destinationPath = destinationPath,
                )
            } finally {
                fileManager.deleteFile(path = encryptedPath)
                fileManager.deleteFile(path = decryptedPath)
            }
        }
    }

    private suspend fun withDownloadLock(fileId: String, action: suspend () -> Unit) {
        val lock = downloadLocksGuard.withLock {
            downloadLocks.getOrPut(fileId) { DownloadLock() }.also { it.holderCount++ }
        }

        try {
            lock.mutex.withLock { action() }
        } finally {
            /** Releasing suspends, so cancellation would otherwise keep the entry forever. */
            withContext(NonCancellable) {
                downloadLocksGuard.withLock {
                    lock.holderCount--
                    if (lock.holderCount == 0) downloadLocks.remove(fileId)
                }
            }
        }
    }

    private suspend fun ChatMessageNM.toSMOrNull(
        chatId: String,
        privateKey: String,
        recipients: List<String>,
    ): MessageSM? {
        return runSuspendCatching {
            val message = toSM(
                chatId = chatId,
                decryptMessage = { encryptedText, key ->
                    decryptText(
                        text = encryptedText,
                        privateKeyString = privateKey,
                        key = key,
                    )
                },
                decryptKey = { key ->
                    decryptKey(key = key, privateKeyString = privateKey)
                },
                recipients = recipients,
            )
            when (message) {
                is MessageSM.Image -> {
                    downloadImage(message)
                    message.copy(isDownloaded = true)
                }

                is MessageSM.Text -> message
            }
        }
            .getOrElse { error ->
                val type = when (this) {
                    is ChatMessageNM.Text -> "text"
                    is ChatMessageNM.Image -> "image"
                }
                val message =
                    "Skipped $type message: id=$id, authorId=$authorId, timestamp=$timestamp"

                Log.e(error, message)
                null
            }
    }

    private class DownloadLock {
        val mutex: Mutex = Mutex()
        var holderCount: Int = 0
    }
}

/** Keeps the still encrypted download apart from its decrypted result in the same directory. */
private const val ENCRYPTED_SUFFIX = ".encrypted"