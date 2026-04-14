package com.security.chat.multiplatform.common.core.ui.entity

import androidx.compose.runtime.Immutable

@Immutable
public sealed interface UiLceState {

    @Immutable
    public object NotStarted : UiLceState

    @Immutable
    public object Loading : UiLceState

    @Immutable
    public object Ready : UiLceState

    @Immutable
    public data class Error(val error: UiError) : UiLceState
}

public fun List<UiLceState>.mergedState(): UiLceState {
    return when {
        this.all { it is UiLceState.Ready } -> UiLceState.Ready
        this.all { it is UiLceState.NotStarted } -> UiLceState.NotStarted
        this.any { it is UiLceState.Loading } -> UiLceState.Loading
        this.any { it is UiLceState.Error } -> this.first { it is UiLceState.Error }
        else -> this.first()
    }
}

public fun Array<UiLceState>.mergedState(): UiLceState = this.toList().mergedState()

public val UiLceState.isLoading: Boolean
    get() = this is UiLceState.Loading
