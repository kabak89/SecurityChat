package com.security.chat.multiplatform.common.core.ui

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ViewModelInterface<out S : Any, out E : Any> {

    val viewState: StateFlow<S>
    val viewEvent: Flow<E>

    fun onViewActive()
    fun onViewInactive()

}