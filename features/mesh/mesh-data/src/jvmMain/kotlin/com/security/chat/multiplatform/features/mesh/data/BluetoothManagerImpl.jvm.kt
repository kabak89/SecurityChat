package com.security.chat.multiplatform.features.mesh.data

import com.juul.kable.Advertisement
import com.juul.kable.Characteristic
import com.juul.kable.Identifier
import com.juul.kable.Peripheral
import com.juul.kable.Scanner
import com.juul.kable.State
import com.juul.kable.WriteType
import com.juul.kable.notify
import com.juul.kable.write
import com.security.chat.multiplatform.common.core.component.SCOPE_ID_APP
import com.security.chat.multiplatform.features.mesh.domain.LogManager
import com.security.chat.multiplatform.features.mesh.domain.entity.MeshPeer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.uuid.Uuid

internal actual class BluetoothManagerImpl actual constructor(
    private val logManager: LogManager,
) : BluetoothManagerBase(), KoinComponent {

    private val appScope: CoroutineScope by inject(named(SCOPE_ID_APP))
    private val dispatcher = Dispatchers.IO.limitedParallelism(1)
    private val scope by lazy { CoroutineScope(appScope.coroutineContext + dispatcher) }
    private val started = AtomicBoolean()
    private val connectingOrConnected = mutableSetOf<Identifier>()
    private val connections = mutableMapOf<MeshPeer, Connection>()

    override fun start() {
        if (!started.compareAndSet(false, true)) return
        if (System.getProperty("os.name").startsWith("Mac") && !hasMacBluetoothUsageDescription()) {
            logManager.e("Desktop mesh Bluetooth requires a macOS bundle with a Bluetooth usage description")
            return
        }

        val scanner = Scanner { filters { match { services = listOf(serviceUuid) } } }
        logManager.d { "Desktop mesh starting BLE scan" }
        scanner.advertisements
            .retryWhen { error, _ ->
                if (error !is Exception || error is CancellationException) return@retryWhen false
                logManager.e(message = "Desktop mesh BLE scan failed, retrying", error = error)
                delay(SCAN_RETRY_DELAY_MS)
                true
            }
            .onEach(::connectToPeer)
            .catch { error ->
                logManager.e(
                    message = "Desktop mesh BLE scan stopped",
                    error = error,
                )
            }
            .launchIn(scope)
    }

    override suspend fun sendFrame(peer: MeshPeer, payload: ByteArray) {
        withContext(dispatcher) {
            val connection = checkNotNull(connections[peer]) {
                "Desktop mesh peer is no longer connected"
            }
            try {
                val sent = withTimeoutOrNull(GATT_TIMEOUT_MS) {
                    connection.peripheral.write(
                        characteristic = connection.writeCharacteristic,
                        data = payload,
                        writeType = WriteType.WithResponse,
                    )
                    true
                }
                check(sent == true) { "Desktop mesh BLE write timed out" }
            } catch (error: Exception) {
                connection.job.cancel()
                throw error
            }
        }
    }

    private fun connectToPeer(advertisement: Advertisement) {
        if (advertisement.isConnectable == false || !connectingOrConnected.add(advertisement.identifier)) {
            return
        }

        logManager.d { "Desktop mesh discovered a BLE peer (RSSI=${advertisement.rssi})" }
        scope.launch {
            try {
                val peripheral = Peripheral(advertisement)
                try {
                    checkNotNull(withTimeoutOrNull(CONNECT_TIMEOUT_MS) { peripheral.connect() }) {
                        "Desktop mesh BLE connection timed out"
                    }
                    coroutineScope { observePeer(peripheral) }
                } finally {
                    closePeripheral(peripheral)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                logManager.e(message = "Desktop mesh BLE session failed", error = error)
            } finally {
                try {
                    delay(RECONNECT_DELAY_MS)
                } finally {
                    connectingOrConnected.remove(advertisement.identifier)
                }
            }
        }
    }

    private suspend fun CoroutineScope.observePeer(peripheral: Peripheral) {
        val service =
            checkNotNull(peripheral.services.value?.find { it.serviceUuid == serviceUuid }) {
                "Desktop mesh peer is missing the BLE mesh service"
            }
        val writeCharacteristic = checkNotNull(
            service.characteristics.find { it.characteristicUuid == writeUuid && it.properties.write },
        ) { "Desktop mesh peer is missing the write characteristic" }
        val notifyCharacteristic = checkNotNull(
            service.characteristics.find { it.characteristicUuid == notifyUuid && it.properties.notify },
        ) { "Desktop mesh peer is missing the notify characteristic" }

        val peer = MeshPeer(sessionId = UUID.randomUUID().toString())
        val subscribed = CompletableDeferred<Unit>()
        val ready = CompletableDeferred<Unit>()
        val observer = peripheral.observe(notifyCharacteristic) { subscribed.complete(Unit) }
            .onEach { frame ->
                ready.await()
                if (!handleIncomingPacket(peer = peer, payload = frame)) {
                    logManager.e("Desktop mesh rejected an incoming BLE frame")
                }
            }
            .launchIn(this)

        try {
            val subscription = withTimeoutOrNull(GATT_TIMEOUT_MS) { subscribed.await() }
            check(subscription != null && peripheral.state.value is State.Connected) {
                "Desktop mesh peer did not enable BLE notifications"
            }
            connections[peer] = Connection(
                peripheral = peripheral,
                writeCharacteristic = writeCharacteristic,
                job = coroutineContext.job,
            )
            updateReadyPeers(connections.keys.toList())
            ready.complete(Unit)
            logManager.d { "Desktop mesh peer ready (session=${peer.sessionId})" }

            peripheral.state.first { it is State.Disconnected || it is State.Disconnecting }
        } finally {
            observer.cancel()
            if (connections.remove(peer) != null) {
                updateReadyPeers(connections.keys.toList())
                logManager.d { "Desktop mesh peer disconnected (session=${peer.sessionId})" }
            }
        }
    }

    private suspend fun closePeripheral(peripheral: Peripheral) {
        withContext(NonCancellable) {
            try {
                withTimeoutOrNull(GATT_TIMEOUT_MS) { peripheral.disconnect() }
            } catch (error: Exception) {
                logManager.e(message = "Desktop mesh BLE disconnect failed", error = error)
            } finally {
                peripheral.close()
            }
        }
    }

    private class Connection(
        val peripheral: Peripheral,
        val writeCharacteristic: Characteristic,
        val job: Job,
    )
}

private val serviceUuid = Uuid.parse(MESH_SERVICE_UUID)
private val writeUuid = Uuid.parse(MESH_WRITE_UUID)
private val notifyUuid = Uuid.parse(MESH_NOTIFY_UUID)

private const val CONNECT_TIMEOUT_MS = 15_000L
private const val GATT_TIMEOUT_MS = 5_000L
private const val RECONNECT_DELAY_MS = 5_000L
private const val SCAN_RETRY_DELAY_MS = 10_000L

private fun hasMacBluetoothUsageDescription(): Boolean {
    /** CoreBluetooth can terminate the process if the host application has no usage description. */
    val command = ProcessHandle.current().info().command().orElse(null) ?: return false
    val executableDirectory = Path.of(command).parent ?: return false
    if (executableDirectory.fileName?.toString() != "MacOS") return false
    val infoPlist = executableDirectory.resolveSibling("Info.plist")
    return runCatching {
        Files.readString(infoPlist).contains("<key>NSBluetoothAlwaysUsageDescription</key>")
    }.getOrDefault(false)
}
