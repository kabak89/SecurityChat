package com.security.chat.multiplatform.common.core.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Desktop JVM does not ship with a built-in network availability API, so we
 * assume the connection is always on. Reconnection logic in [LiveEventsManager]
 * will still recover based on WebSocket errors; a more accurate implementation
 * (e.g. periodic reachability probes) can replace this later without touching
 * call sites.
 */
internal class ConnectivityObserverJvm : ConnectivityObserver {

    override val isOnline: StateFlow<Boolean> = MutableStateFlow(true).asStateFlow()

}
