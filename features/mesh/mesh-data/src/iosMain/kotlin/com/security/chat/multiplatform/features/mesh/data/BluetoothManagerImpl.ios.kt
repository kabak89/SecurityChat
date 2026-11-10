package com.security.chat.multiplatform.features.mesh.data

import com.security.chat.multiplatform.features.mesh.domain.LogManager
import com.security.chat.multiplatform.features.mesh.domain.entity.MeshPeer
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import platform.CoreBluetooth.CBCentralManager
import platform.CoreBluetooth.CBCentralManagerDelegateProtocol
import platform.CoreBluetooth.CBCentralManagerOptionRestoreIdentifierKey
import platform.CoreBluetooth.CBCentralManagerRestoredStatePeripheralsKey
import platform.CoreBluetooth.CBCharacteristic
import platform.CoreBluetooth.CBCharacteristicPropertyNotify
import platform.CoreBluetooth.CBCharacteristicPropertyWrite
import platform.CoreBluetooth.CBCharacteristicWriteWithResponse
import platform.CoreBluetooth.CBManager
import platform.CoreBluetooth.CBManagerAuthorizationAllowedAlways
import platform.CoreBluetooth.CBManagerStatePoweredOn
import platform.CoreBluetooth.CBPeripheral
import platform.CoreBluetooth.CBPeripheralDelegateProtocol
import platform.CoreBluetooth.CBPeripheralStateConnected
import platform.CoreBluetooth.CBService
import platform.CoreBluetooth.CBUUID
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSNumber
import platform.Foundation.NSUUID
import platform.Foundation.create
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@OptIn(ExperimentalForeignApi::class)
internal actual class BluetoothManagerImpl actual constructor(
    private val logManager: LogManager,
) : BluetoothManagerBase() {

    private val serviceUuid = CBUUID.UUIDWithString(MESH_SERVICE_UUID)
    private val writeUuid = CBUUID.UUIDWithString(MESH_WRITE_UUID)
    private val notifyUuid = CBUUID.UUIDWithString(MESH_NOTIFY_UUID)
    private val sessions = mutableMapOf<String, Session>()
    private val delegate = CentralDelegate()
    private val gattPeripheral = MeshGattPeripheral(
        logManager = logManager,
        onPacket = ::handleIncomingPacket,
        onReadyPeersChanged = ::publishReadyPeers,
    )
    private var manager: CBCentralManager? = null

    override fun start() {
        dispatch_async(dispatch_get_main_queue()) {
            val authorization = CBManager.authorization
            logManager.d { "iOS mesh start requested (Bluetooth authorization=$authorization)" }
            if (authorization != CBManagerAuthorizationAllowedAlways) {
                logManager.d { "iOS mesh start blocked by Bluetooth permission" }
                return@dispatch_async
            }
            if (manager == null) {
                manager = CBCentralManager(
                    delegate = delegate,
                    queue = null,
                    options = mapOf(CBCentralManagerOptionRestoreIdentifierKey to "securitychat.mesh.central"),
                )
            }
            gattPeripheral.start()
        }
    }

    override suspend fun sendFrame(peer: MeshPeer, payload: ByteArray) {
        withContext(Dispatchers.Main.immediate) {
            val session = sessions.values.firstOrNull { it.peer == peer && it.ready }
            if (session == null) {
                gattPeripheral.send(peer, payload)
                return@withContext
            }
            val characteristic = checkNotNull(session.outgoing)
            val peripheral = session.peripheral
            val maxLength = minOf(
                MAX_MESH_PAYLOAD_BYTES.toULong(),
                peripheral.maximumWriteValueLengthForType(CBCharacteristicWriteWithResponse),
            )
            require(payload.size.toULong() <= maxLength) {
                "Mesh payload exceeds $maxLength bytes"
            }

            val result = CompletableDeferred<Boolean>()
            session.pendingWrite = result
            try {
                peripheral.writeValue(
                    data = payload.toNSData(),
                    forCharacteristic = characteristic,
                    type = CBCharacteristicWriteWithResponse,
                )
                val accepted = withTimeoutOrNull(WRITE_TIMEOUT_MS) { result.await() }
                if (accepted == null) {
                    disconnect(session)
                }
                check(accepted == true) { "Mesh GATT write failed" }
            } finally {
                session.pendingWrite = null
            }
        }
    }

    private fun publishReadyPeers() {
        val connectedPeripherals = sessions.values.filter { it.ready }.map { it.peer }
        updateReadyPeers(connectedPeripherals + gattPeripheral.readyPeers)
    }

    private fun getSession(peripheral: CBPeripheral): Session? {
        return sessions[peripheral.identifier.UUIDString]
    }

    private fun discoverServices(session: Session) {
        session.peripheral.delegate = delegate
        session.peripheral.discoverServices(listOf(serviceUuid))
    }

    private fun disconnect(session: Session) {
        session.ready = false
        session.pendingWrite?.complete(false)
        publishReadyPeers()
        manager?.cancelPeripheralConnection(session.peripheral)
    }

    private inner class CentralDelegate : NSObject(), CBCentralManagerDelegateProtocol,
        CBPeripheralDelegateProtocol {

        override fun centralManagerDidUpdateState(central: CBCentralManager) {
            logManager.d { "iOS mesh central state=${central.state}" }
            if (central.state != CBManagerStatePoweredOn) {
                sessions.values.forEach { it.pendingWrite?.complete(false) }
                sessions.clear()
                publishReadyPeers()
                return
            }
            if (!central.isScanning()) {
                logManager.d { "iOS mesh requesting BLE scan (service=$MESH_SERVICE_UUID)" }
                central.scanForPeripheralsWithServices(listOf(serviceUuid), options = null)
            }
            sessions.values.filter { it.peripheral.state == CBPeripheralStateConnected }
                .forEach(::discoverServices)
        }

        override fun centralManager(central: CBCentralManager, willRestoreState: Map<Any?, *>) {
            val restored = willRestoreState[CBCentralManagerRestoredStatePeripheralsKey]
                    as? List<*> ?: return
            restored.filterIsInstance<CBPeripheral>().forEach { peripheral ->
                sessions[peripheral.identifier.UUIDString] = Session(peripheral)
                peripheral.delegate = this
            }
        }

        override fun centralManager(
            central: CBCentralManager,
            didDiscoverPeripheral: CBPeripheral,
            advertisementData: Map<Any?, *>,
            RSSI: NSNumber,
        ) {
            logManager.d { "Mesh BLE peer discovered (RSSI: ${RSSI.intValue} dBm)" }
            if (didDiscoverPeripheral.identifier.UUIDString !in sessions) {
                connect(central, didDiscoverPeripheral)
            }
        }

        override fun centralManager(central: CBCentralManager, didConnectPeripheral: CBPeripheral) {
            logManager.d { "iOS mesh connected to BLE peer, discovering GATT services" }
            getSession(didConnectPeripheral)?.let(::discoverServices)
        }

        @ObjCSignatureOverride
        override fun centralManager(
            central: CBCentralManager,
            didFailToConnectPeripheral: CBPeripheral,
            error: NSError?,
        ) {
            logManager.d { "iOS mesh BLE connection failed: ${error?.localizedDescription}" }
            removeSession(didFailToConnectPeripheral)
        }

        @ObjCSignatureOverride
        override fun centralManager(
            central: CBCentralManager,
            didDisconnectPeripheral: CBPeripheral,
            error: NSError?,
        ) {
            val reconnect = getSession(didDisconnectPeripheral)?.ready == true
            removeSession(didDisconnectPeripheral)
            if (reconnect && central.state == CBManagerStatePoweredOn) {
                connect(central, didDisconnectPeripheral)
            }
        }

        override fun peripheral(peripheral: CBPeripheral, didDiscoverServices: NSError?) {
            val session = getSession(peripheral) ?: return
            val service = peripheral.services?.filterIsInstance<CBService>()
                ?.firstOrNull { it.UUID == serviceUuid }
            if (didDiscoverServices != null || service == null) {
                logManager.d {
                    "iOS mesh GATT service unavailable: ${didDiscoverServices?.localizedDescription}"
                }
                disconnect(session)
                return
            }
            peripheral.discoverCharacteristics(listOf(writeUuid, notifyUuid), forService = service)
        }

        override fun peripheral(
            peripheral: CBPeripheral,
            didDiscoverCharacteristicsForService: CBService,
            error: NSError?,
        ) {
            val session = getSession(peripheral) ?: return
            val characteristics = didDiscoverCharacteristicsForService.characteristics
                ?.filterIsInstance<CBCharacteristic>().orEmpty()
            val write = characteristics.firstOrNull {
                it.UUID == writeUuid && it.properties and CBCharacteristicPropertyWrite != 0uL
            }
            val notify = characteristics.firstOrNull {
                it.UUID == notifyUuid && it.properties and CBCharacteristicPropertyNotify != 0uL
            }
            if (error != null || write == null || notify == null) {
                logManager.d {
                    "iOS mesh GATT characteristics unavailable: ${error?.localizedDescription}"
                }
                disconnect(session)
                return
            }
            session.outgoing = write
            if (notify.isNotifying()) {
                session.ready = true
                logManager.d { "iOS mesh GATT subscription already active" }
                publishReadyPeers()
            } else {
                peripheral.setNotifyValue(true, forCharacteristic = notify)
            }
        }

        @ObjCSignatureOverride
        override fun peripheral(
            peripheral: CBPeripheral,
            didUpdateNotificationStateForCharacteristic: CBCharacteristic,
            error: NSError?,
        ) {
            val session = getSession(peripheral) ?: return
            if (didUpdateNotificationStateForCharacteristic.UUID != notifyUuid) return
            if (error != null || !didUpdateNotificationStateForCharacteristic.isNotifying()) {
                logManager.d {
                    "iOS mesh GATT subscription failed: ${error?.localizedDescription}"
                }
                disconnect(session)
                return
            }
            session.ready = true
            logManager.d { "iOS mesh GATT subscription confirmed" }
            publishReadyPeers()
        }

        @ObjCSignatureOverride
        override fun peripheral(
            peripheral: CBPeripheral,
            didUpdateValueForCharacteristic: CBCharacteristic,
            error: NSError?,
        ) {
            val session = getSession(peripheral) ?: return
            if (!session.ready || error != null || didUpdateValueForCharacteristic.UUID != notifyUuid) {
                return
            }
            val bytes = didUpdateValueForCharacteristic.value?.toByteArray() ?: return
            if (bytes.size > MAX_MESH_PAYLOAD_BYTES || !handleIncomingPacket(session.peer, bytes)) {
                logManager.e("Could not queue a mesh Bluetooth packet")
            }
        }

        @ObjCSignatureOverride
        override fun peripheral(
            peripheral: CBPeripheral,
            didWriteValueForCharacteristic: CBCharacteristic,
            error: NSError?,
        ) {
            if (didWriteValueForCharacteristic.UUID == writeUuid) {
                getSession(peripheral)?.pendingWrite?.complete(error == null)
            }
        }

        private fun connect(central: CBCentralManager, peripheral: CBPeripheral) {
            sessions[peripheral.identifier.UUIDString] = Session(peripheral)
            logManager.d { "iOS mesh connecting to BLE peer" }
            central.connectPeripheral(peripheral, options = null)
        }

        private fun removeSession(peripheral: CBPeripheral) {
            sessions.remove(peripheral.identifier.UUIDString)?.pendingWrite?.complete(false)
            publishReadyPeers()
        }
    }

    private class Session(val peripheral: CBPeripheral) {

        val peer = MeshPeer(sessionId = NSUUID().UUIDString)
        var outgoing: CBCharacteristic? = null
        var ready = false
        var pendingWrite: CompletableDeferred<Boolean>? = null
    }
}

private const val WRITE_TIMEOUT_MS = 5_000L

@OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
internal fun ByteArray.toNSData(): NSData {
    if (isEmpty()) return NSData()
    return usePinned { NSData.create(bytes = it.addressOf(0), length = size.toULong()) }
}

@OptIn(ExperimentalForeignApi::class)
internal fun NSData.toByteArray(): ByteArray {
    return bytes?.readBytes(length.toInt()) ?: ByteArray(0)
}
