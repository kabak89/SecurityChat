package com.security.chat.multiplatform.features.chat.data.common

import com.security.chat.multiplatform.common.core.files.FileManager
import com.security.chat.multiplatform.common.core.localization.StringRes
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.features.chat.data.common.mapper.toSM
import com.security.chat.multiplatform.features.chat.data.network.ChatNetworkManager
import com.security.chat.multiplatform.features.chat.data.storage.ChatStorage
import com.security.chat.multiplatform.features.chat.data.storage.entity.MessageSM
import com.security.chat.multiplatform.features.user.data.storage.UserStorage
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.AES
import dev.whyoleg.cryptography.algorithms.RSA
import dev.whyoleg.cryptography.algorithms.SHA512
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import org.jetbrains.compose.resources.getString
import securitychat.common.localization.generated.resources.push_stub_image
import kotlin.io.encoding.Base64

public interface ChatDataHelper {
    public suspend fun fetchAndSaveMessages(chatId: String)
    public suspend fun processNewMessages(
        serializedMessages: String,
        chatId: String,
    ): List<String>

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

    public suspend fun downloadImage(message: MessageSM.Image): String
}

internal class ChatDataHelperImpl(
    private val userStorage: UserStorage,
    private val chatNetworkManager: ChatNetworkManager,
    private val chatStorage: ChatStorage,
    private val fileManager: FileManager,
    private val dispatcherProvider: DispatcherProviderInterface,
) : ChatDataHelper {

    override suspend fun fetchAndSaveMessages(chatId: String) {
        val messages = chatNetworkManager.getMessages(chatId = chatId)
        if (messages.isEmpty()) return
        val messageIds = messages.map { it.id }

        val privateKey = checkNotNull(userStorage.getKeys()?.privateKey)

        val messagesToStore = messages
            .map {
                it.toSM(
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
                    recipients = listOf(checkNotNull(userStorage.getUserId())),
                )
            }
            .map { message ->
                when (message) {
                    is MessageSM.Image -> message.copy(localPath = downloadImage(message))
                    is MessageSM.Text -> message
                }
            }

        chatStorage.saveMessages(messages = messagesToStore)

        confirmReceivingMessages(
            chatId = chatId,
            messageIds = messageIds,
        )
    }

    override suspend fun processNewMessages(
        serializedMessages: String,
        chatId: String,
    ): List<String> {
        val messages = chatNetworkManager.processNewMessages(serializedMessages)
        val privateKey = checkNotNull(userStorage.getKeys()?.privateKey)

        val messagesToStore = messages
            .map {
                it.toSM(
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
                    chatId = chatId,
                    recipients = listOf(checkNotNull(userStorage.getUserId())),
                )
            }
            .map { message ->
                when (message) {
                    is MessageSM.Image -> message.copy(localPath = downloadImage(message))
                    is MessageSM.Text -> message
                }
            }
        chatStorage.saveMessages(messages = messagesToStore)

        val messageIds = messages.map { it.id }
        confirmReceivingMessages(
            chatId = chatId,
            messageIds = messageIds,
        )

        return messagesToStore.map { message ->
            when (message) {
                is MessageSM.Text -> message.text
                is MessageSM.Image -> getString(StringRes.push_stub_image)
            }
        }
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
            aesKey.cipher().decrypt(ciphertext = ciphertextBytes).decodeToString()
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

    private suspend fun confirmReceivingMessages(
        chatId: String,
        messageIds: List<String>,
    ) {
        chatNetworkManager.confirmReceivingMessages(
            chatId = chatId,
            messageIds = messageIds,
        )
    }

    override suspend fun downloadImage(message: MessageSM.Image): String {
        val encryptedDirectory =
            fileManager.getDataDirectoryPath(FileManager.ENCRYPTED_IMAGES_FOLDER)
        val encryptedFilePath = "$encryptedDirectory/${message.fileId}"

        chatNetworkManager.downloadFile(
            fileId = message.fileId,
            destinationPath = encryptedFilePath,
        )

        val imagesDirectory = fileManager.getDataDirectoryPath(FileManager.IMAGES_FOLDER)
        val localPath = "$imagesDirectory/${message.fileId}"

        decryptFile(
            sourcePath = encryptedFilePath,
            destinationPath = localPath,
            key = message.key,
        )

        fileManager.deleteFile(path = encryptedFilePath)
        return localPath
    }
}