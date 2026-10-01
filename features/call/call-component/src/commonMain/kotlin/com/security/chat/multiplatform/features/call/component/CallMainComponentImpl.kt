package com.security.chat.multiplatform.features.call.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnCreate
import com.security.chat.multiplatform.common.core.component.BaseComponentImpl
import com.security.chat.multiplatform.features.call.component.api.CallMainComponent
import com.security.chat.multiplatform.features.call.domain.CallModel

internal class CallMainComponentImpl(
    override val chatId: String,
    private val onBack: () -> Unit,
    componentContext: ComponentContext,
) : CallMainComponent,
    BaseComponentImpl(
        componentContext = componentContext,
        scopeId = SCOPE_ID_CALL_MAIN,
    ) {

    init {
        doOnCreate {
            val callModel: CallModel = getKoin().get()
            callModel.start(parentScope = componentCoroutineScope)
        }
    }

    override fun onBackClicked() {
        onBack()
    }
}

private const val SCOPE_ID_CALL_MAIN: String = "SCOPE_ID_CALL_MAIN"
