package com.security.chat.multiplatform.features.chat.data.repoimpl

import com.security.chat.multiplatform.common.core.network.NetworkManager
import com.security.chat.multiplatform.common.core.network.NetworkManagerFactory
import com.security.chat.multiplatform.features.chat.data.entity.GetPublicKeyResponse
import com.security.chat.multiplatform.features.chat.data.entity.SendMessageRequest
import com.security.chat.multiplatform.features.chat.domain.repo.ChatRepo
import com.security.chat.multiplatform.features.chats.data.storage.ChatsStorage
import com.security.chat.multiplatform.features.user.data.storage.UserStorage
import com.security.chat.multiplatform.features.users.data.storage.UsersStorage
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.DelicateCryptographyApi
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

        val encryptedMessage = encryptMessage(message = message, publicKey = publicKey)

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

    @OptIn(DelicateCryptographyApi::class)
    private suspend fun encryptMessage(message: String, publicKey: String): String {
        val provider = CryptographyProvider.Default
        val rsa = provider.get(RSA.RAW)
        println("qweqw publicKey = $publicKey")
        val publicKeyBytes = Base64.decode(publicKey)
        val publicKey = rsa.publicKeyDecoder(digest = SHA512).decodeFromByteArray(
            format = RSA.PublicKey.Format.DER,
            bytes = publicKeyBytes,
        )
        val plaintextBytes = message.encodeToByteArray()
        val ciphertext = publicKey.encryptor().encrypt(plaintextBytes)
        return Base64.encode(ciphertext)
    }

}