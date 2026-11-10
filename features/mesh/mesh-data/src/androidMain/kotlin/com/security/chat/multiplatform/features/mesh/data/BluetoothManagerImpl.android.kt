package com.security.chat.multiplatform.features.mesh.data

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.os.SystemClock
import android.util.LruCache
import com.security.chat.multiplatform.features.mesh.domain.LogManager
import com.security.chat.multiplatform.features.mesh.domain.entity.MeshPeer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.nio.ByteBuffer
import java.util.UUID
import kotlin.random.Random
import android.bluetooth.BluetoothManager as AndroidBluetoothManager

@SuppressLint("MissingPermission")
internal actual class BluetoothManagerImpl actual constructor(
    private val logManager: LogManager,
) : BluetoothManagerBase(), KoinComponent {

    private val context: Context by inject()
    private val handler = Handler(Looper.getMainLooper())
    private val discoveryKey = Random.nextLong()
    private val clients = mutableMapOf<Long, MeshGattClient>()
    private var host: GattHost? = null
    private var scan: MeshScan? = null
    private var isObservingAdapter = false
    private val adapterReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) start()
        }
    }

    override fun start() {
        handler.post {
            logManager.d { "Android mesh start requested (API ${Build.VERSION.SDK_INT})" }
            if (!hasBluetoothPermissions()) {
                logManager.d {
                    "Android mesh start blocked by permissions " +
                            "(connect=${hasPermission(Manifest.permission.BLUETOOTH_CONNECT)}, " +
                            "advertise=${hasPermission(Manifest.permission.BLUETOOTH_ADVERTISE)})"
                }
                return@post
            }

            if (!isObservingAdapter) {
                val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.registerReceiver(adapterReceiver, filter, Context.RECEIVER_EXPORTED)
                } else {
                    context.registerReceiver(adapterReceiver, filter)
                }
                isObservingAdapter = true
            }

            val manager = context.getSystemService(AndroidBluetoothManager::class.java)
            if (manager == null) {
                logManager.d { "Android mesh Bluetooth manager unavailable" }
                return@post
            }
            val adapter = manager.adapter
            if (adapter?.isEnabled != true) {
                logManager.d { "Android mesh Bluetooth unavailable (state=${adapter?.state})" }
                stopBluetooth()
                return@post
            }
            if (host == null) {
                if (adapter.bluetoothLeAdvertiser == null) {
                    logManager.d { "Android mesh BLE advertiser unavailable" }
                } else {
                    val newHost = GattHost(adapter)
                    host = newHost
                    newHost.open(manager)
                }
            }
            val backgroundEnabled = canScanInBackground()
            if (scan?.usesPendingIntent == backgroundEnabled) return@post
            if (!hasScanPermission()) {
                logManager.d { "Android mesh BLE scan blocked by missing scan permission" }
                return@post
            }
            val scanner = adapter.bluetoothLeScanner
            if (scanner == null) {
                logManager.d { "Android mesh BLE scanner unavailable" }
                return@post
            }
            scan?.stop()
            val newScan = MeshScan(scanner, usesPendingIntent = backgroundEnabled)
            scan = newScan
            newScan.start()
        }
    }

    override suspend fun sendFrame(peer: MeshPeer, payload: ByteArray) {
        withContext(Dispatchers.Main.immediate) {
            val client = clients.values.firstOrNull { it.readyPeer == peer }
            if (client != null) {
                client.send(payload)
            } else {
                val currentHost = checkNotNull(host) { "Bluetooth GATT server is unavailable" }
                currentHost.send(peer, payload)
            }
        }
    }

    fun onBackgroundScanResults(results: List<ScanResult>) {
        handler.post {
            val currentScan = scan?.takeIf { it.usesPendingIntent } ?: return@post
            results.forEach(currentScan::handleResult)
        }
    }

    fun onBackgroundScanFailed(errorCode: Int) {
        handler.post {
            scan?.takeIf { it.usesPendingIntent }?.handleFailure(errorCode)
        }
    }

    private fun stopBluetooth() {
        scan = null
        val outgoingConnections = clients.values.toList()
        clients.clear()
        outgoingConnections.forEach { it.close() }
        host?.close()
        host = null
        publishReadyPeers()
    }

    private fun hasBluetoothPermissions(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                hasPermission(Manifest.permission.BLUETOOTH_CONNECT) &&
                hasPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    }

    private fun hasScanPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Manifest.permission.BLUETOOTH_SCAN
        } else {
            Manifest.permission.ACCESS_FINE_LOCATION
        }
        return hasPermission(permission)
    }

    private fun canScanInBackground(): Boolean {
        return Build.VERSION.SDK_INT !in Build.VERSION_CODES.Q..Build.VERSION_CODES.R ||
                hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    }

    private fun hasPermission(permission: String): Boolean {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun publishReadyPeers() {
        updateReadyPeers(host?.readyPeers.orEmpty() + clients.values.mapNotNull { it.readyPeer })
    }

    private fun connectToPeer(result: ScanResult) {
        if (!result.isConnectable || host?.isAdvertising == false) return
        val data = result.scanRecord?.getServiceData(SERVICE_PARCEL_UUID) ?: return
        if (data.size != Long.SIZE_BYTES) return
        val remoteKey = ByteBuffer.wrap(data).long
        /** The lower key initiates the connection when both devices are advertising. */
        if (remoteKey == discoveryKey || host?.isAdvertising == true && discoveryKey > remoteKey) {
            return
        }
        if (remoteKey in clients) return

        val client = MeshGattClient(
            context = context,
            device = result.device,
            handler = handler,
            logManager = logManager,
            onPacket = ::handleIncomingPacket,
            onStateChanged = { changedClient ->
                if (clients[remoteKey] === changedClient) {
                    publishReadyPeers()
                    if (changedClient.readyPeer == null) {
                        handler.postDelayed(
                            { clients.remove(remoteKey, changedClient) },
                            RECONNECT_DELAY_MS,
                        )
                    }
                }
            },
        )
        clients[remoteKey] = client
        logManager.d { "Android mesh connecting to BLE peer (RSSI: ${result.rssi} dBm)" }
        client.connect()
    }

    private inner class MeshScan(
        private val scanner: BluetoothLeScanner,
        val usesPendingIntent: Boolean,
    ) : ScanCallback() {

        private val lastSeen = LruCache<BluetoothDevice, Long>(128)
        private val pendingIntent = if (usesPendingIntent) meshScanPendingIntent(context) else null

        fun start() {
            val filter = ScanFilter.Builder().setServiceUuid(SERVICE_PARCEL_UUID).build()
            val settings = ScanSettings.Builder().build()
            if (pendingIntent == null) {
                logManager.d { "Android mesh using BLE scan callback without background location" }
                scanner.startScan(listOf(filter), settings, this)
            } else {
                logManager.d { "Android mesh requesting background BLE scan (service=$SERVICE_UUID)" }
                val status = scanner.startScan(listOf(filter), settings, pendingIntent)
                if (status != 0 && status != SCAN_FAILED_ALREADY_STARTED) {
                    handleFailure(status)
                    return
                }
            }
            handler.postDelayed(
                {
                    if (scan === this && lastSeen.size() == 0) {
                        logManager.d {
                            "Android mesh BLE scan has no matching results after " +
                                    "${FIRST_SCAN_RESULT_CHECK_MS / 1_000} seconds"
                        }
                    }
                },
                FIRST_SCAN_RESULT_CHECK_MS,
            )
        }

        fun stop() {
            if (!hasScanPermission()) return
            if (pendingIntent == null) {
                scanner.stopScan(this)
            } else {
                scanner.stopScan(pendingIntent)
            }
        }

        fun handleResult(result: ScanResult) {
            val now = SystemClock.elapsedRealtime()
            val previous = lastSeen.put(result.device, now)
            if (previous == null || now - previous >= REDISCOVERY_GAP_MS) {
                logManager.d {
                    val serviceData = result.scanRecord?.getServiceData(SERVICE_PARCEL_UUID)
                    "Mesh BLE peer discovered (RSSI: ${result.rssi} dBm, " +
                            "connectable=${result.isConnectable}, serviceDataBytes=${serviceData?.size ?: 0})"
                }
            }
            connectToPeer(result)
        }

        fun handleFailure(errorCode: Int) {
            if (scan !== this || usesPendingIntent && errorCode == SCAN_FAILED_ALREADY_STARTED) {
                return
            }
            scan = null
            logManager.e("Android mesh BLE scan failed with status $errorCode")
        }

        override fun onScanResult(callbackType: Int, result: ScanResult) {
            handler.post {
                if (scan === this) handleResult(result)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            handler.post {
                handleFailure(errorCode)
            }
        }
    }

    private inner class GattHost(private val adapter: BluetoothAdapter) :
        BluetoothGattServerCallback() {

        private val sessions = mutableMapOf<BluetoothDevice, MeshPeer>()
        val readyPeers: Collection<MeshPeer> get() = sessions.values
        var isAdvertising = false
            private set
        private val service =
            BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        private val incoming = BluetoothGattCharacteristic(
            WRITE_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_WRITE,
        )
        private val outgoing = BluetoothGattCharacteristic(
            NOTIFY_UUID,
            BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            0,
        )
        private val config = BluetoothGattDescriptor(
            CLIENT_CONFIG_UUID,
            BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE,
        )
        private var server: BluetoothGattServer? = null
        private var pendingNotification: Pair<BluetoothDevice, CompletableDeferred<Int>>? = null
        private val advertiseCallback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                handler.post {
                    if (host === this@GattHost) {
                        isAdvertising = true
                        logManager.d { "Android mesh BLE advertising started" }
                    }
                }
            }

            override fun onStartFailure(errorCode: Int) {
                handler.post { fail("BLE advertising failed with status $errorCode") }
            }
        }

        fun open(manager: AndroidBluetoothManager) {
            logManager.d { "Android mesh opening GATT server (service=$SERVICE_UUID)" }
            outgoing.addDescriptor(config)
            service.addCharacteristic(incoming)
            service.addCharacteristic(outgoing)
            server = manager.openGattServer(context, this)
            if (server?.addService(service) != true) fail("Could not open the mesh GATT service")
        }

        fun close() {
            adapter.bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)
            server?.close()
            server = null
            pendingNotification?.second?.complete(BluetoothGatt.GATT_FAILURE)
            pendingNotification = null
            sessions.clear()
            publishReadyPeers()
        }

        @Suppress("DEPRECATION")
        suspend fun send(peer: MeshPeer, payload: ByteArray) {
            require(payload.size <= MAX_MESH_PAYLOAD_BYTES) {
                "Mesh payload exceeds $MAX_MESH_PAYLOAD_BYTES bytes"
            }
            val gatt = checkNotNull(server) { "Mesh GATT server is closed" }
            val device = checkNotNull(sessions.entries.firstOrNull { it.value == peer }?.key) {
                "Mesh peer disconnected"
            }
            val result = CompletableDeferred<Int>()
            pendingNotification = device to result
            try {
                val accepted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    gatt.notifyCharacteristicChanged(device, outgoing, false, payload) ==
                            BluetoothStatusCodes.SUCCESS
                } else {
                    outgoing.value = payload
                    gatt.notifyCharacteristicChanged(device, outgoing, false)
                }
                check(accepted) { "Bluetooth rejected the mesh notification" }
                val status = withTimeoutOrNull(NOTIFICATION_TIMEOUT_MS) { result.await() }
                if (status == null) {
                    sessions.remove(device)
                    publishReadyPeers()
                    gatt.cancelConnection(device)
                }
                check(status == BluetoothGatt.GATT_SUCCESS) { "Mesh notification failed: $status" }
            } finally {
                pendingNotification = null
            }
        }

        override fun onServiceAdded(status: Int, service: BluetoothGattService) {
            handler.post {
                if (host !== this) return@post
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    fail("Could not publish the mesh GATT service: $status")
                    return@post
                }
                val settings = AdvertiseSettings.Builder().setConnectable(true).build()
                val data = AdvertiseData.Builder().addServiceUuid(SERVICE_PARCEL_UUID).build()
                val key = ByteBuffer.allocate(Long.SIZE_BYTES).putLong(discoveryKey).array()
                val response =
                    AdvertiseData.Builder().addServiceData(SERVICE_PARCEL_UUID, key).build()
                logManager.d { "Android mesh GATT service published, requesting BLE advertising" }
                adapter.bluetoothLeAdvertiser?.startAdvertising(
                    settings,
                    data,
                    response,
                    advertiseCallback,
                )
            }
        }

        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            handler.post {
                if (host !== this || newState != BluetoothProfile.STATE_DISCONNECTED) return@post
                sessions.remove(device)
                publishReadyPeers()
                if (pendingNotification?.first == device) {
                    pendingNotification?.second?.complete(BluetoothGatt.GATT_FAILURE)
                }
            }
        }

        override fun onDescriptorReadRequest(
            device: BluetoothDevice,
            requestId: Int,
            offset: Int,
            descriptor: BluetoothGattDescriptor,
        ) {
            handler.post {
                val value = if (device in sessions) {
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                } else {
                    BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                }
                val status = when {
                    descriptor.uuid != CLIENT_CONFIG_UUID -> BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED
                    offset !in 0..value.size -> BluetoothGatt.GATT_INVALID_OFFSET
                    else -> BluetoothGatt.GATT_SUCCESS
                }
                val response = if (status == BluetoothGatt.GATT_SUCCESS) {
                    value.copyOfRange(offset, value.size)
                } else {
                    null
                }
                server?.sendResponse(device, requestId, status, offset, response)
            }
        }

        override fun onDescriptorWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            descriptor: BluetoothGattDescriptor,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray,
        ) {
            handler.post {
                val gatt = server ?: return@post
                val enabled = value.contentEquals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                val disabled =
                    value.contentEquals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)
                val status = when {
                    descriptor.uuid != CLIENT_CONFIG_UUID || preparedWrite ->
                        BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED

                    offset != 0 -> BluetoothGatt.GATT_INVALID_OFFSET
                    enabled || disabled -> BluetoothGatt.GATT_SUCCESS
                    else -> BluetoothGatt.GATT_FAILURE
                }
                if (responseNeeded && !gatt.sendResponse(device, requestId, status, offset, null)) {
                    return@post
                }
                if (status != BluetoothGatt.GATT_SUCCESS) return@post

                if (enabled) {
                    sessions.getOrPut(device) { MeshPeer(sessionId = UUID.randomUUID().toString()) }
                    logManager.d { "Android mesh peer subscribed to GATT notifications" }
                } else {
                    sessions.remove(device)
                }
                publishReadyPeers()
            }
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray,
        ) {
            handler.post {
                val peer = sessions[device]
                val status = when {
                    characteristic.uuid != WRITE_UUID || preparedWrite ->
                        BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED

                    offset != 0 -> BluetoothGatt.GATT_INVALID_OFFSET
                    value.size > MAX_MESH_PAYLOAD_BYTES -> BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH
                    peer != null && handleIncomingPacket(peer, value) -> BluetoothGatt.GATT_SUCCESS
                    else -> BluetoothGatt.GATT_FAILURE
                }
                if (responseNeeded) server?.sendResponse(device, requestId, status, offset, null)
            }
        }

        override fun onNotificationSent(device: BluetoothDevice, status: Int) {
            handler.post {
                if (pendingNotification?.first == device) pendingNotification?.second?.complete(
                    status,
                )
            }
        }

        private fun fail(message: String) {
            if (host !== this) return
            logManager.e(message)
            close()
            host = null
        }
    }
}

internal val SERVICE_UUID: UUID = UUID.fromString(MESH_SERVICE_UUID)
internal val WRITE_UUID: UUID = UUID.fromString(MESH_WRITE_UUID)
internal val NOTIFY_UUID: UUID = UUID.fromString(MESH_NOTIFY_UUID)
internal val CLIENT_CONFIG_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
private val SERVICE_PARCEL_UUID = ParcelUuid(SERVICE_UUID)
private const val NOTIFICATION_TIMEOUT_MS = 5_000L
private const val RECONNECT_DELAY_MS = 5_000L
private const val REDISCOVERY_GAP_MS = 30_000L
private const val FIRST_SCAN_RESULT_CHECK_MS = 15_000L
