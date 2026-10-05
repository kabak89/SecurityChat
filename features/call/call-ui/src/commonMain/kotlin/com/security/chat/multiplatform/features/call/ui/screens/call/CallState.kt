package com.security.chat.multiplatform.features.call.ui.screens.call

import androidx.compose.runtime.Immutable
import com.security.chat.multiplatform.features.call.ui.screens.call.entity.CallParticipantUM
import com.security.chat.multiplatform.features.call.ui.screens.call.entity.CallStatusUM
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class CallState(
    val onlineParticipants: ImmutableList<CallParticipantUM>,
    val callStatus: CallStatusUM,
) {

    val isCallButtonEnabled: Boolean =
        when (callStatus) {
            CallStatusUM.Idle -> onlineParticipants.isNotEmpty()
            CallStatusUM.Connecting -> false
            CallStatusUM.Active -> true
        }
}
