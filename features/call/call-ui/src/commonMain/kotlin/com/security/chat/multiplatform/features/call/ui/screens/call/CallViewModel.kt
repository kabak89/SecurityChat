package com.security.chat.multiplatform.features.call.ui.screens.call

import com.security.chat.multiplatform.common.core.ui.BaseViewModel

internal class CallViewModel : BaseViewModel<CallState, CallEvent>() {

    override fun createInitialState(): CallState = CallState
}
