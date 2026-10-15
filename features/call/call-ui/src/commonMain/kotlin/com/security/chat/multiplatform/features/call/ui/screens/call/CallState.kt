package com.security.chat.multiplatform.features.call.ui.screens.call

import androidx.compose.runtime.Immutable
import com.security.chat.multiplatform.common.ui.kit.components.alertdialog.AlertDialogDescriptor
import com.security.chat.multiplatform.features.call.ui.screens.call.entity.CallParticipantUM
import com.security.chat.multiplatform.features.call.ui.screens.call.entity.CallStatusUM
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class CallState(
    val onlineParticipants: ImmutableList<CallParticipantUM>,
    val callStatus: CallStatusUM,
    val alertDialogDescriptor: AlertDialogDescriptor?,
    val isCallInProgress: Boolean,
) {

    val isCallButtonEnabled: Boolean =
        when (callStatus) {
            CallStatusUM.Idle -> onlineParticipants.isNotEmpty() || isCallInProgress
            CallStatusUM.Connecting -> true
            CallStatusUM.Active -> true
        }
}
