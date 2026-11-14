package com.security.chat.multiplatform.common.core.network

import com.security.chat.multiplatform.common.core.network.entity.NetworkConfig
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import java.net.ServerSocket
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class ConnectivityObserverJvmTest {

    @Test
    fun `reports loss and recovery of the configured server`() = runTest {
        listenOnLoopback(0).use { server ->
            val port = server.localPort
            val observer = ConnectivityObserverJvm(
                networkConfig = NetworkConfig(host = "http://127.0.0.1", port = port),
                coroutineScope = backgroundScope,
                ioDispatcher = StandardTestDispatcher(testScheduler),
            )

            assertFalse(observer.isOnline.value)
            runCurrent()
            assertTrue(observer.isOnline.value)

            server.close()
            advanceTimeBy(5.seconds)
            runCurrent()
            assertFalse(observer.isOnline.value)

            listenOnLoopback(port).use {
                advanceTimeBy(5.seconds)
                runCurrent()
                assertTrue(observer.isOnline.value)
            }
        }
    }

    private fun listenOnLoopback(port: Int): ServerSocket {
        return ServerSocket().apply {
            reuseAddress = true
            bind(InetSocketAddress("127.0.0.1", port))
        }
    }
}
