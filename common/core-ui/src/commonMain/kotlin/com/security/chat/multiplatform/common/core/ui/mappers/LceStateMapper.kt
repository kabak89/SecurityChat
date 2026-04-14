package com.security.chat.multiplatform.common.core.ui.mappers

import com.security.chat.multiplatform.common.core.domain.LceState
import com.security.chat.multiplatform.common.core.ui.entity.UiError
import com.security.chat.multiplatform.common.core.ui.entity.UiLceState

public fun <T : Any> LceState<T>.toUiLceState(
    errorMapper: (Throwable) -> UiError? = DefaultErrorMapper::mapError,
): UiLceState {
    return when (this) {
        is LceState.Content -> UiLceState.Ready
        is LceState.Loading -> UiLceState.Loading
        is LceState.Error -> {
            UiLceState.Error(errorMapper(this.value) ?: DefaultErrorMapper.mapError(this.value))
        }

        is LceState.None -> UiLceState.NotStarted
    }
}
