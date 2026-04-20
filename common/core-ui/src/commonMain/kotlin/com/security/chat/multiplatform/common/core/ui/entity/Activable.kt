package com.security.chat.multiplatform.common.core.ui.entity

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Inspired by https://github.com/aartikov/Sesame/tree/master/sesame-activable
 */
public interface Activable {

    public val activeFlow: StateFlow<Boolean>

    public fun onActive()

    public fun onInactive()
}

internal val Activable.active get() = activeFlow.value

public fun Activable(): Activable = ActivableImpl()

internal class ActivableImpl : Activable {

    override val activeFlow = MutableStateFlow(false)

    override fun onActive() {
        activeFlow.value = true
    }

    override fun onInactive() {
        activeFlow.value = false
    }
}