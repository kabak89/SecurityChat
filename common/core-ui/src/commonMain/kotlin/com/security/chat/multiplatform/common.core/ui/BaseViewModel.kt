package com.security.chat.multiplatform.common.core.ui

import androidx.annotation.CallSuper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

public abstract class BaseViewModel<S : Any, E : Any> : ViewModel(),
    ViewModelInterface<S, E> {

    private var loadingTriggered = false

    private val _viewState by lazy { MutableStateFlow(createInitialState()) }

    /**
     * Do not use inside without subscription because value inside may be outdated. To get current
     * value use [_viewState]
     */
    override val viewState: StateFlow<S> by lazy {
        _viewState
            .onEach {
                if (!loadingTriggered) {
                    loadingTriggered = true
                    onPostStart()
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(),
                initialValue = _viewState.value,
            )
    }

    private val _viewEvent = Channel<E>(capacity = Channel.UNLIMITED)
    override val viewEvent: Flow<E> = _viewEvent.receiveAsFlow()

//    protected val viewActivable: Activable = Activable()

    protected val currentViewState: S get() = _viewState.value

    init {
        println("${this::class.simpleName} init")
    }

    protected abstract fun createInitialState(): S

    protected fun updateState(update: (S) -> S) {
        _viewState.update(update)
    }

    protected fun sendEvent(event: E) {
        _viewEvent.trySend(event)
    }

//    protected fun <T> Flow<T>.collectWhenViewActive(): Flow<T> =
//        activableFlow(originalFlow = this, activable = viewActivable, scope = viewModelScope)

    /**
     * Method calls after subscription on [_viewState] is happened. Suitable for start loading data
     */
    protected open fun onPostStart(): Unit = Unit

    @CallSuper
    override fun onViewActive() {
//        viewActivable.onActive()
    }

    @CallSuper
    override fun onViewInactive() {
//        viewActivable.onInactive()
    }

    override fun onCleared() {
        super.onCleared()
        println("${this::class.simpleName} onCleared")
    }
}