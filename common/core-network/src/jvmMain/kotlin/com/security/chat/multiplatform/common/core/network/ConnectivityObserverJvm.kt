package com.security.chat.multiplatform.common.core.network

import com.security.chat.multiplatform.common.core.network.entity.NetworkConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI
import kotlin.time.Duration.Companion.seconds

internal class ConnectivityObserverJvm(
    networkConfig: NetworkConfig,
    coroutineScope: CoroutineScope,
    ioDispatcher: CoroutineDispatcher,
) : ConnectivityObserver {

    private val host = requireNotNull(URI(networkConfig.host).host)
    private val port = networkConfig.port

    override val isOnline: StateFlow<Boolean> = flow {
        while (true) {
            emit(canConnect())
            delay(5.seconds)
        }
    }.flowOn(ioDispatcher).stateIn(
        scope = coroutineScope,
        started = SharingStarted.Eagerly,
        initialValue = false,
    )

    private fun canConnect(): Boolean {
        return try {
            Socket().use { it.connect(InetSocketAddress(host, port), 2_000) }
            true
        } catch (_: IOException) {
            false
        }
    }
}
