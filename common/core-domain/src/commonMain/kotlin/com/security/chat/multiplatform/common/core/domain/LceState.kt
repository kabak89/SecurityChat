package com.security.chat.multiplatform.common.core.domain

public sealed interface LceState<out T : Any> {

    public data object Loading : LceState<Nothing>
    public data object None : LceState<Nothing>
    public data class Content<C : Any>(val value: C) : LceState<C>
    public data class Error(val value: Throwable) : LceState<Nothing>

    public val content: T?
        get() = (this as? Content<T>)?.value

    public val isContent: Boolean
        get() = this is Content

    public val error: Throwable?
        get() = (this as? Error)?.value

    public val isError: Boolean
        get() = this is Error

    public val isLoading: Boolean
        get() = this is Loading

}