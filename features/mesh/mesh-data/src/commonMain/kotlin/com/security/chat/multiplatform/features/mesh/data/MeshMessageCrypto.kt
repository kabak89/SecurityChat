package com.security.chat.multiplatform.features.mesh.data

import com.security.chat.multiplatform.common.encryption.signRsa
import com.security.chat.multiplatform.common.encryption.verifyRsa
import com.security.chat.multiplatform.features.chat.data.common.ChatDataHelper
import com.security.chat.multiplatform.features.chat.data.network.entity.network.RecipientCiphertext
import com.security.chat.multiplatform.features.chat.data.network.entity.network.TextMessageRequest
import com.security.chat.multiplatform.features.chat.data.storage.entity.MessageSM
import com.security.chat.multiplatform.features.mesh.domain.LogManager
import com.security.chat.multiplatform.features.user.data.storage.UserStorage
import com.security.chat.multiplatform.features.users.data.storage.UsersStorage
import kotlinx.serialization.json.Json

internal class MeshMessageCrypto(
    private val chatDataHelper: ChatDataHelper,
    private val userStorage: UserStorage,
    private val usersStorage: UsersStorage,
    private val json: Json,
    private val log: LogManager,
) {

    suspend fun pack(message: MessageSM.Text, now: Long): SignedMeshData? {
        val keys = userStorage.getKeys() ?: run {
            log.d { "Cannot prepare mesh message=${message.id}, local account keys are missing" }
            return null
        }
        val deviceId = userStorage.getDeviceId() ?: run {
            log.d { "Cannot prepare mesh message=${message.id}, registered device ID is missing" }
            return null
        }
        val key = chatDataHelper.getOneTimeEncryptionKey()
        val recipients = (message.recipients.map { it.userId } + message.authorId)
            .distinct().map { userId ->
                val publicKey = if (userId == message.authorId) {
                    keys.publicKey
                } else {
                    usersStorage.getUser(userId)?.publicKey ?: run {
                        log.d { "Cannot prepare mesh message=${message.id}, a recipient key is missing from cache" }
                        return null
                    }
                }
                RecipientCiphertext(
                    recipientId = userId,
                    key = chatDataHelper.encryptKey(key = key, publicKeyString = publicKey),
                )
            }
        val payload = MeshMessagePayload(
            authorId = message.authorId,
            authorDeviceId = deviceId,
            createdAt = now,
            expiresAt = now + PACKET_DURATION,
            message = TextMessageRequest(
                id = message.id,
                chatId = message.chatId,
                timestamp = message.timestamp,
                recipients = recipients,
                message = chatDataHelper.encryptText(text = message.text, key = key),
                replyToMessageId = message.replyToMessageId,
            ),
        )
        val encoded = json.encodeToString(payload)
        return SignedMeshData(
            payload = encoded,
            signature = signRsa(
                data = MESSAGE_SIGNATURE_PREFIX + encoded,
                privateKey = keys.privateKey,
            ),
        )
    }

    suspend fun isAuthentic(packet: SignedMeshData, publicKey: String): Boolean {
        return verifyRsa(
            data = MESSAGE_SIGNATURE_PREFIX + packet.payload,
            signature = packet.signature,
            publicKey = publicKey,
        )
    }

    suspend fun decrypt(payload: MeshMessagePayload, userId: String): String {
        val privateKey = checkNotNull(userStorage.getKeys()).privateKey
        return chatDataHelper.decryptText(
            text = payload.message.message,
            privateKeyString = privateKey,
            key = payload.message.recipients.first { it.recipientId == userId }.key,
        )
    }
}
