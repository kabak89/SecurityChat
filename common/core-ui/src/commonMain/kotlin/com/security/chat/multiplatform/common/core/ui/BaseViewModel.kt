package com.security.chat.multiplatform.common.core.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.security.chat.multiplatform.common.core.ui.entity.Activable
import com.security.chat.multiplatform.common.log.Log
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

public abstract class BaseViewModel<S : Any, E : Any> : ViewModel(), ViewModelInterface<S, E> {

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

    protected val currentViewState: S get() = _viewState.value

    protected val viewActivable: Activable = Activable()

    init {
        Log.d { "${this::class.qualifiedName} init" }
    }

    protected abstract fun createInitialState(): S

    protected fun <T> Flow<T>.collectWhenViewActive(): Flow<T> =
        viewActivable.activeFlow
            .flatMapLatest { isActive ->
                if (isActive) this else emptyFlow()
            }

    protected fun updateState(update: (S) -> S) {
        _viewState.update(update)
    }

    protected fun sendEvent(event: E) {
        _viewEvent.trySend(event)
    }

    /**
     * Method calls after subscription on [_viewState] is happened. Suitable for start loading data
     */
    protected open fun onPostStart(): Unit = Unit

    override fun onCleared() {
        super.onCleared()
        Log.d { "${this::class.qualifiedName} onCleared" }
    }

    override fun onViewActive() {
        viewActivable.onActive()
        Log.d { "onActive" }
    }

    override fun onViewInactive() {
        viewActivable.onInactive()
        Log.d { "onViewInactive" }
    }
}