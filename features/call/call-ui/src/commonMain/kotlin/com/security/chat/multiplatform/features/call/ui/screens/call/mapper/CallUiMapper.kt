package com.security.chat.multiplatform.features.call.ui.screens.call.mapper

import com.security.chat.multiplatform.features.call.domain.entity.CallParticipant
import com.security.chat.multiplatform.features.call.domain.entity.CallStatus
import com.security.chat.multiplatform.features.call.ui.screens.call.entity.CallParticipantUM
import com.security.chat.multiplatform.features.call.ui.screens.call.entity.CallStatusUM

internal fun CallParticipant.toUi(): CallParticipantUM = CallParticipantUM(id = id, name = name)

internal fun CallStatus.toUi(): CallStatusUM =
    when (this) {
        CallStatus.Idle -> CallStatusUM.Idle
        CallStatus.Connecting -> CallStatusUM.Connecting
        CallStatus.Active -> CallStatusUM.Active
    }
