package com.security.chat.multiplatform.common.core.ui

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

public interface ViewModelInterface<out S : Any, out E : Any> {

    public val viewState: StateFlow<S>
    public val viewEvent: Flow<E>

    public fun onViewActive()
    public fun onViewInactive()

}