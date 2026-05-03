package com.security.chat.multiplatform.features.chat.data.common

import com.security.chat.multiplatform.features.chat.data.common.mapper.toSM
import com.security.chat.multiplatform.features.chat.data.network.ChatNetworkManager
import com.security.chat.multiplatform.features.chat.data.network.entity.ChatMessageNM
import com.security.chat.multiplatform.features.chat.data.storage.ChatStorage
import com.security.chat.multiplatform.features.user.data.storage.UserStorage
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.RSA
import dev.whyoleg.cryptography.algorithms.SHA512
import kotlin.io.encoding.Base64

public interface ChatDataHelper {
    public suspend fun fetchAndSaveMessages(chatId: String)
    public suspend fun processNewMessages(
        serializedMessages: String,
        chatId: String,
    ): List<String>

    public suspend fun decryptText(text: String, privateKeyString: String): String
    public suspend fun encryptText(text: String, publicKeyString: String): String
}

internal class ChatDataHelperImpl(
    private val userStorage: UserStorage,
    private val chatNetworkManager: ChatNetworkManager,
    private val chatStorage: ChatStorage,
) : ChatDataHelper {

    override suspend fun fetchAndSaveMessages(chatId: String) {
        val messages = fetchMessages(chatId = chatId)
        val messageIds = messages.map { it.id }
        val companionId = messages.first().authorId

        confirmReceivingMessages(
            authorId = companionId,
            chatId = chatId,
            messageIds = messageIds,
        )

        val privateKey = checkNotNull(userStorage.getKeys()?.privateKey)

        val messagesToStore = messages.map {
            it.toSM(
                chatId = chatId,
                decryptMessage = { encryptedText ->
                    decryptText(
                        text = encryptedText,
                        privateKeyString = privateKey,
                    )
                },
            )
        }
        chatStorage.saveMessages(messages = messagesToStore)
    }

    override suspend fun processNewMessages(
        serializedMessages: String,
        chatId: String,
    ): List<String> {
        val messages = chatNetworkManager.processNewMessages(serializedMessages)
        val messageIds = messages.map { it.id }
        confirmReceivingMessages(
            authorId = messages.first().authorId,
            chatId = chatId,
            messageIds = messageIds,
        )
        val privateKey = checkNotNull(userStorage.getKeys()?.privateKey)

        val messagesToStore = messages.map {
            it.toSM(
                decryptMessage = { encryptedText ->
                    decryptText(
                        text = encryptedText,
                        privateKeyString = privateKey,
                    )
                },
                chatId = chatId,
            )
        }
        chatStorage.saveMessages(messages = messagesToStore)
        return messagesToStore.map { it.text }
    }

    override suspend fun decryptText(text: String, privateKeyString: String): String {
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

    override suspend fun encryptText(text: String, publicKeyString: String): String {
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

    private suspend fun fetchMessages(chatId: String): List<ChatMessageNM> {
        val userId = checkNotNull(userStorage.getUserId())

        return chatNetworkManager.getMessages(chatId = chatId)
            .filter { it.authorId != userId }
    }

    private suspend fun confirmReceivingMessages(
        authorId: String,
        chatId: String,
        messageIds: List<String>,
    ) {
        chatNetworkManager.confirmReceivingMessages(
            authorId = authorId,
            chatId = chatId,
            messageIds = messageIds,
        )
    }
}