package com.security.chat.multiplatform.features.mesh.data

import com.security.chat.multiplatform.common.encryption.sha256Hash
import com.security.chat.multiplatform.features.chat.data.network.entity.network.TextMessageRequest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.Uuid

@Serializable
internal sealed interface MeshFrame {
    @Serializable
    @SerialName("mesh.hello.v1")
    data class Hello(val deviceId: String, val supportsPacketAcknowledgements: Boolean = false) :
        MeshFrame

    @Serializable
    @SerialName("mesh.message.v1")
    data class Message(val packet: SignedMeshData) : MeshFrame

    @Serializable
    @SerialName("mesh.packet-ack.v1")
    data class PacketAcknowledgement(val packetHash: String) : MeshFrame

    @Serializable
    @SerialName("mesh.receipts.v1")
    data class Receipts(val receipts: List<SignedMeshData>) : MeshFrame
}

@Serializable
internal data class SignedMeshData(val payload: String, val signature: String) {
    fun packetHash(): String = sha256Hash("$signature\n$payload")
}

@Serializable
internal data class MeshMessagePayload(
    val authorId: String,
    val authorDeviceId: String,
    val createdAt: Long,
    val expiresAt: Long,
    val message: TextMessageRequest,
) {
    fun isValid(now: Long): Boolean {
        return expiresAt > now && createdAt in (now - PACKET_DURATION)..(now + CLOCK_TOLERANCE) &&
                expiresAt == createdAt + PACKET_DURATION && isUuid(message.id) &&
                isUuid(authorId) && isUuid(authorDeviceId) && isUuid(message.chatId) &&
                message.recipients.isNotEmpty() &&
                message.recipients.map { it.recipientId }
                    .distinct().size == message.recipients.size &&
                message.recipients.all { isUuid(it.recipientId) && it.key.isNotBlank() } &&
                message.message.isNotBlank() &&
                (message.replyToMessageId == null ||
                        (isUuid(checkNotNull(message.replyToMessageId)) && message.replyToMessageId != message.id))
    }
}

@Serializable
internal data class MeshReceiptPayload(
    val authorId: String,
    val chatId: String,
    val messageId: String,
    val packetHash: String,
    val expiresAt: Long,
    val acceptedRecipientIds: List<String>,
)

@Serializable
internal data class MeshReceiptKey(val publicKey: String)

internal fun isUuid(value: String): Boolean {
    return runCatching { Uuid.parse(value).toString() == value }.getOrDefault(false)
}

internal val PACKET_DURATION = 7.days.inWholeMilliseconds
internal val CLOCK_TOLERANCE = 5.minutes.inWholeMilliseconds
internal const val MESSAGE_SIGNATURE_PREFIX = "SecurityChat.mesh.message.v1\n"
internal const val RECEIPT_SIGNATURE_PREFIX = "SecurityChat.mesh.receipt.v1\n"
internal const val MAX_PACKET_BYTES = 16 * 1024
internal const val MAX_QUEUED_RELAY_PACKETS = 512
