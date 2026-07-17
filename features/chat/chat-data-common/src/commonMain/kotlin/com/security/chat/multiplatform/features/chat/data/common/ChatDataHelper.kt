package com.security.chat.multiplatform.features.chat.data.common

import com.security.chat.multiplatform.features.chat.data.common.entity.EncryptedMessage
import com.security.chat.multiplatform.features.chat.data.common.mapper.toSM
import com.security.chat.multiplatform.features.chat.data.network.ChatNetworkManager
import com.security.chat.multiplatform.features.chat.data.storage.ChatStorage
import com.security.chat.multiplatform.features.chat.data.storage.entity.MessageSM
import com.security.chat.multiplatform.features.user.data.storage.UserStorage
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.AES
import dev.whyoleg.cryptography.algorithms.RSA
import dev.whyoleg.cryptography.algorithms.SHA512
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

    public suspend fun encryptText(
        text: String,
        publicKeyString: String,
        key: String,
    ): EncryptedMessage

    public suspend fun getOneTimeEncryptionKey(): String
}

internal class ChatDataHelperImpl(
    private val userStorage: UserStorage,
    private val chatNetworkManager: ChatNetworkManager,
    private val chatStorage: ChatStorage,
) : ChatDataHelper {

    override suspend fun fetchAndSaveMessages(chatId: String) {
        val messages = chatNetworkManager.getMessages(chatId = chatId)
        if (messages.isEmpty()) return
        val messageIds = messages.map { it.id }

        confirmReceivingMessages(
            chatId = chatId,
            messageIds = messageIds,
        )

        val privateKey = checkNotNull(userStorage.getKeys()?.privateKey)

        val messagesToStore = messages.map {
            it.toSM(
                chatId = chatId,
                decryptMessage = { encryptedText, key ->
                    decryptText(
                        text = encryptedText,
                        privateKeyString = privateKey,
                        key = key,
                    )
                },
                recipients = listOf(checkNotNull(userStorage.getUserId())),
            )
        }
        chatStorage.saveMessages(messages = messagesToStore)
    }

    override suspend fun processNewMessages(
        serializedMessages: String,
        chatId: String,
    ): List<String> {
        val messages = chatNetworkManager.processNewMessages(serializedMessages)
        val privateKey = checkNotNull(userStorage.getKeys()?.privateKey)

        val messagesToStore = messages.map {
            it.toSM(
                decryptMessage = { encryptedText, key ->
                    decryptText(
                        text = encryptedText,
                        privateKeyString = privateKey,
                        key = key,
                    )
                },
                chatId = chatId,
                recipients = listOf(checkNotNull(userStorage.getUserId())),
            )
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
            }
        }
    }

    override suspend fun decryptText(
        text: String,
        privateKeyString: String,
        key: String,
    ): String {
        val provider = CryptographyProvider.Default

        val rsa = provider.get(RSA.OAEP)
        val privateKeyBytes = Base64.decode(privateKeyString)
        val privateKey = rsa.privateKeyDecoder(digest = SHA512).decodeFromByteArray(
            format = RSA.PrivateKey.Format.DER,
            bytes = privateKeyBytes,
        )

        val encryptedAesKeyBytes = Base64.decode(key)
        val rawAesKey = privateKey.decryptor().decrypt(encryptedAesKeyBytes)

        val aesGcm = provider.get(AES.GCM)
        val aesKey = aesGcm.keyDecoder().decodeFromByteArray(
            format = AES.Key.Format.RAW,
            bytes = rawAesKey,
        )

        val ciphertextBytes = Base64.decode(text)
        return aesKey.cipher().decrypt(ciphertext = ciphertextBytes).decodeToString()
    }

    override suspend fun encryptText(
        text: String,
        publicKeyString: String,
        key: String,
    ): EncryptedMessage {
        val provider = CryptographyProvider.Default
        val rawAesKey = Base64.decode(key)
        val aesGcm = provider.get(AES.GCM)

        val aesKey = aesGcm.keyDecoder().decodeFromByteArray(
            format = AES.Key.Format.RAW,
            bytes = rawAesKey,
        )

        val encryptedTextBytes = aesKey.cipher().encrypt(plaintext = text.encodeToByteArray())
        val rsa = provider.get(RSA.OAEP)
        val publicKeyBytes = Base64.decode(publicKeyString)

        val publicKey = rsa.publicKeyDecoder(digest = SHA512).decodeFromByteArray(
            format = RSA.PublicKey.Format.DER,
            bytes = publicKeyBytes,
        )

        val encryptedKeyBytes = publicKey.encryptor().encrypt(rawAesKey)

        return EncryptedMessage(
            encryptedText = Base64.encode(encryptedTextBytes),
            encryptedKey = Base64.encode(encryptedKeyBytes),
        )
    }

    override suspend fun getOneTimeEncryptionKey(): String {
        val aesGcm = CryptographyProvider.Default.get(AES.GCM)
        val aesKey = aesGcm.keyGenerator(AES.Key.Size.B256).generateKey()
        val rawAesKey = aesKey.encodeToByteArray(AES.Key.Format.RAW)
        return Base64.encode(rawAesKey)
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
}