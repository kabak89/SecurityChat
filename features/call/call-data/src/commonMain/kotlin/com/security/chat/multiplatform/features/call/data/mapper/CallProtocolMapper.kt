package com.security.chat.multiplatform.features.call.data.mapper

import com.security.chat.multiplatform.common.log.Log
import com.security.chat.multiplatform.features.call.data.entity.CallCommand
import com.security.chat.multiplatform.features.call.data.entity.CallEnvelope
import com.security.chat.multiplatform.features.call.data.entity.CallErrorMessage
import com.security.chat.multiplatform.features.call.data.entity.CallRevokedMessage
import com.security.chat.multiplatform.features.call.data.entity.CallSignalMessage
import com.security.chat.multiplatform.features.call.data.entity.CallSnapshotMessage
import com.security.chat.multiplatform.features.call.domain.entity.CallIceServer
import com.security.chat.multiplatform.features.call.domain.entity.CallPeerConnection
import com.security.chat.multiplatform.features.call.domain.entity.CallRoomConnection
import com.security.chat.multiplatform.features.call.domain.entity.CallRoomEvent
import com.security.chat.multiplatform.features.call.domain.entity.CallRoomSnapshot
import com.security.chat.multiplatform.features.call.domain.entity.CallSignal
import kotlinx.serialization.json.Json

internal fun CallEnvelope.toDomain(chatId: String, json: Json): CallRoomEvent? =
    when (type) {
        "call.state", "call.joined" -> {
            val message = json.decodeFromString<CallSnapshotMessage>(payload)
            if (message.chatId != chatId) null else {
                val snapshot = message.toDomain()
                if (type == "call.joined") {
                    CallRoomEvent.Joined(requireNotNull(message.requestId), snapshot)
                } else {
                    CallRoomEvent.Snapshot(snapshot)
                }
            }
        }

        "call.signal" -> {
            val message = json.decodeFromString<CallSignalMessage>(payload)
            if (message.chatId != chatId) null else message.toDomain()?.let {
                CallRoomEvent.Signal(message.callId, it)
            }
        }

        "call.revoked" -> {
            val message = json.decodeFromString<CallRevokedMessage>(payload)
            if (message.chatId == chatId) CallRoomEvent.Revoked(message.reason) else null
        }

        "call.error" -> {
            val message = json.decodeFromString<CallErrorMessage>(payload)
            CallRoomEvent.Error(message.code, message.message)
        }

        "call.ack" -> null
        else -> {
            Log.e("Unknown call event type: $type")
            null
        }
    }

private fun CallSnapshotMessage.toDomain(): CallRoomSnapshot {
    require(connections.map { it.id }.distinct().size == connections.size)
    require(connections.map { it.participantSessionId }.distinct().size == connections.size)
    require(participantSessionId == null || !callId.isNullOrBlank())
    return CallRoomSnapshot(
        callId = callId,
        participantSessionId = participantSessionId,
        connections = connections.map {
            require(it.id.isNotBlank() && it.participantSessionId.isNotBlank())
            require(it.participantSessionId != participantSessionId)
            CallRoomConnection(CallPeerConnection(it.id, it.participantSessionId), it.isInitiator)
        },
        iceServers = iceServers.map { CallIceServer(it.urls, it.username, it.credential) },
        participantUserIds = participants.map { it.userId }.toSet(),
    )
}

private fun CallSignalMessage.toDomain(): CallSignal? {
    val connection = CallPeerConnection(connectionId, participantSessionId)
    return when (signalType) {
        "offer" -> CallSignal.Offer(connection, requireNotNull(sdp))
        "answer" -> CallSignal.Answer(connection, requireNotNull(sdp))
        "ice_candidate" -> CallSignal.IceCandidate(
            connection, requireNotNull(candidate), sdpMid, sdpMLineIndex,
        )

        "ice_gathering_complete" -> CallSignal.IceGatheringComplete(connection)
        else -> {
            Log.e("Unknown call signal type: $signalType")
            null
        }
    }
}

internal fun CallSignal.toCommand(chatId: String, callId: String, requestId: String): CallCommand {
    val command = CallCommand(
        requestId = requestId,
        chatId = chatId,
        callId = callId,
        connectionId = connection.id,
        participantSessionId = connection.participantSessionId,
    )
    return when (this) {
        is CallSignal.Offer -> command.copy(signalType = "offer", sdp = sdp)
        is CallSignal.Answer -> command.copy(signalType = "answer", sdp = sdp)
        is CallSignal.IceCandidate -> command.copy(
            signalType = "ice_candidate", candidate = candidate,
            sdpMid = sdpMid, sdpMLineIndex = sdpMLineIndex,
        )

        is CallSignal.IceGatheringComplete -> command.copy(signalType = "ice_gathering_complete")
    }
}
