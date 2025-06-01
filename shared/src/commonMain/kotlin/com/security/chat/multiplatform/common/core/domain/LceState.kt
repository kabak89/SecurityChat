package com.security.chat.multiplatform.common.core.domain

sealed class LceState<out T : Any> {
    data object Loading : LceState<Nothing>()
    data object None : LceState<Nothing>()
    data class Content<C : Any>(val value: C) : LceState<C>()
    data class Error(val value: Throwable) : LceState<Nothing>()

    val content: T?
        get() = (this as? Content<T>)?.value

    val isContent: Boolean
        get() = this is Content

    val error: Throwable?
        get() = (this as? Error)?.value

    val isError: Boolean
        get() = this is Error

    val isLoading: Boolean
        get() = this is Loading
}