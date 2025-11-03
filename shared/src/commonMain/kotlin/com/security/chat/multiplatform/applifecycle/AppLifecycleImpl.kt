package com.security.chat.multiplatform.applifecycle

import com.security.chat.multiplatform.common.app.lifecycle.AppLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

internal class AppLifecycleImpl : AppLifecycleChanger, AppLifecycle {

    private val stateFlow = MutableStateFlow(State())

    override fun onAppStarted() {
        stateFlow.update { it.copy(isAppStarted = true) }
    }

    override fun getIsAppStartedFlow(): Flow<Boolean> {
        return stateFlow
            .map { it.isAppStarted }
            .distinctUntilChanged()
    }

    private data class State(
        val isAppStarted: Boolean = false,
    )

}