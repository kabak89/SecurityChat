package com.security.chat.multiplatform.features.mesh.data

import com.security.chat.multiplatform.features.mesh.domain.LogManager
import com.security.chat.multiplatform.features.mesh.domain.entity.MeshPeer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import platform.CoreBluetooth.CBATTErrorInvalidAttributeValueLength
import platform.CoreBluetooth.CBATTErrorInvalidOffset
import platform.CoreBluetooth.CBATTErrorRequestNotSupported
import platform.CoreBluetooth.CBATTErrorSuccess
import platform.CoreBluetooth.CBATTErrorUnlikelyError
import platform.CoreBluetooth.CBATTErrorWriteNotPermitted
import platform.CoreBluetooth.CBATTRequest
import platform.CoreBluetooth.CBAdvertisementDataServiceUUIDsKey
import platform.CoreBluetooth.CBAttributePermissionsWriteable
import platform.CoreBluetooth.CBCentral
import platform.CoreBluetooth.CBCharacteristic
import platform.CoreBluetooth.CBCharacteristicPropertyNotify
import platform.CoreBluetooth.CBCharacteristicPropertyWrite
import platform.CoreBluetooth.CBManagerStatePoweredOff
import platform.CoreBluetooth.CBManagerStatePoweredOn
import platform.CoreBluetooth.CBManagerStateUnknown
import platform.CoreBluetooth.CBMutableCharacteristic
import platform.CoreBluetooth.CBMutableService
import platform.CoreBluetooth.CBPeripheralManager
import platform.CoreBluetooth.CBPeripheralManagerDelegateProtocol
import platform.CoreBluetooth.CBPeripheralManagerOptionRestoreIdentifierKey
import platform.CoreBluetooth.CBPeripheralManagerRestoredStateServicesKey
import platform.CoreBluetooth.CBService
import platform.CoreBluetooth.CBUUID
import platform.Foundation.NSError
import platform.Foundation.NSUUID
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
internal class MeshGattPeripheral(
    private val logManager: LogManager,
    private val onPacket: (MeshPeer, ByteArray) -> Boolean,
    private val onReadyPeersChanged: () -> Unit,
) : NSObject(), CBPeripheralManagerDelegateProtocol {

    private val serviceUuid = CBUUID.UUIDWithString(MESH_SERVICE_UUID)
    private val writeUuid = CBUUID.UUIDWithString(MESH_WRITE_UUID)
    private val notifyUuid = CBUUID.UUIDWithString(MESH_NOTIFY_UUID)
    private val sessions = mutableMapOf<String, Session>()
    private var manager: CBPeripheralManager? = null
    private var pendingService: CBMutableService? = null
    private var notification: CBMutableCharacteristic? = null
    private var isStartingAdvertising = false
    private var pendingUpdate: CompletableDeferred<Unit>? = null

    val readyPeers: List<MeshPeer> get() = sessions.values.map { it.peer }

    fun start() {
        val peripheral = manager
        if (peripheral == null) {
            manager = CBPeripheralManager(
                delegate = this,
                queue = null,
                options = mapOf(CBPeripheralManagerOptionRestoreIdentifierKey to "securitychat.mesh.peripheral"),
            )
        } else if (peripheral.state == CBManagerStatePoweredOn) {
            publishOrAdvertise(peripheral)
        }
    }

    suspend fun send(peer: MeshPeer, payload: ByteArray) {
        val peripheral = checkNotNull(manager) { "Mesh GATT server is unavailable" }
        val characteristic =
            checkNotNull(notification) { "Mesh notify characteristic is unavailable" }
        val session = checkNotNull(sessions.values.firstOrNull { it.peer == peer }) {
            "Mesh peer disconnected"
        }
        val centralId = session.central.identifier.UUIDString
        val maxLength =
            minOf(MAX_MESH_PAYLOAD_BYTES.toULong(), session.central.maximumUpdateValueLength)
        require(payload.size.toULong() <= maxLength) { "Mesh payload exceeds $maxLength bytes" }

        val data = payload.toNSData()
        val accepted = withTimeoutOrNull(NOTIFICATION_TIMEOUT_MS) {
            while (sessions[centralId] === session) {
                val queued = peripheral.updateValue(
                    value = data,
                    forCharacteristic = characteristic,
                    onSubscribedCentrals = listOf(session.central),
                )
                if (queued) return@withTimeoutOrNull true
                val ready = CompletableDeferred<Unit>()
                pendingUpdate = ready
                try {
                    ready.await()
                } finally {
                    pendingUpdate = null
                }
            }
            false
        }
        check(accepted == true) { "Mesh GATT notification failed" }
    }

    override fun peripheralManagerDidUpdateState(peripheral: CBPeripheralManager) {
        logManager.d { "iOS mesh peripheral state=${peripheral.state}" }
        if (peripheral.state == CBManagerStatePoweredOn) {
            publishOrAdvertise(peripheral)
        } else if (peripheral.state != CBManagerStateUnknown) {
            pendingService = null
            isStartingAdvertising = false
            if (peripheral.state != CBManagerStatePoweredOff) notification = null
            sessions.clear()
            pendingUpdate?.complete(Unit)
            onReadyPeersChanged()
        }
    }

    override fun peripheralManager(
        peripheral: CBPeripheralManager,
        willRestoreState: Map<Any?, *>,
    ) {
        val services = willRestoreState[CBPeripheralManagerRestoredStateServicesKey] as? List<*>
        val service =
            services?.filterIsInstance<CBService>()?.firstOrNull { it.UUID == serviceUuid }
        val characteristic = service?.let(::findNotification) ?: return
        notification = characteristic
        characteristic.subscribedCentrals.orEmpty().filterIsInstance<CBCentral>()
            .forEach { central ->
                sessions.getOrPut(central.identifier.UUIDString) { Session(central) }
            }
        onReadyPeersChanged()
    }

    override fun peripheralManager(
        peripheral: CBPeripheralManager,
        didAddService: CBService,
        error: NSError?,
    ) {
        val service = pendingService ?: return
        if (didAddService.UUID != serviceUuid) return
        pendingService = null
        if (error != null) {
            logManager.e("Could not publish the mesh GATT service: ${error.localizedDescription}")
            return
        }
        notification = findNotification(service)
        if (peripheral.state == CBManagerStatePoweredOn) publishOrAdvertise(peripheral)
    }

    override fun peripheralManagerDidStartAdvertising(
        peripheral: CBPeripheralManager,
        error: NSError?,
    ) {
        isStartingAdvertising = false
        if (error != null) {
            logManager.e("Could not start mesh advertising: ${error.localizedDescription}")
        } else {
            logManager.d { "iOS mesh BLE advertising started (service=$MESH_SERVICE_UUID)" }
        }
    }

    @ObjCSignatureOverride
    override fun peripheralManager(
        peripheral: CBPeripheralManager,
        central: CBCentral,
        didSubscribeToCharacteristic: CBCharacteristic,
    ) {
        if (didSubscribeToCharacteristic.UUID != notifyUuid) return
        sessions.getOrPut(central.identifier.UUIDString) { Session(central) }
        logManager.d { "iOS mesh peer subscribed to GATT notifications" }
        onReadyPeersChanged()
    }

    @ObjCSignatureOverride
    override fun peripheralManager(
        peripheral: CBPeripheralManager,
        central: CBCentral,
        didUnsubscribeFromCharacteristic: CBCharacteristic,
    ) {
        if (didUnsubscribeFromCharacteristic.UUID != notifyUuid) return
        sessions.remove(central.identifier.UUIDString)
        pendingUpdate?.complete(Unit)
        onReadyPeersChanged()
    }

    override fun peripheralManager(
        peripheral: CBPeripheralManager,
        didReceiveWriteRequests: List<*>,
    ) {
        val request = didReceiveWriteRequests.firstOrNull() as? CBATTRequest ?: return
        val session = sessions[request.central.identifier.UUIDString]
        val value = request.value
        val result = when {
            didReceiveWriteRequests.size != 1 -> CBATTErrorRequestNotSupported
            request.characteristic.UUID != writeUuid || session == null -> CBATTErrorWriteNotPermitted
            request.offset != 0uL -> CBATTErrorInvalidOffset
            value == null || value.length > MAX_MESH_PAYLOAD_BYTES.toULong() -> CBATTErrorInvalidAttributeValueLength
            onPacket(session.peer, value.toByteArray()) -> CBATTErrorSuccess
            else -> CBATTErrorUnlikelyError
        }
        peripheral.respondToRequest(request, withResult = result)
    }

    override fun peripheralManagerIsReadyToUpdateSubscribers(peripheral: CBPeripheralManager) {
        pendingUpdate?.complete(Unit)
    }

    private fun publishOrAdvertise(peripheral: CBPeripheralManager) {
        if (notification != null) {
            if (!peripheral.isAdvertising() && !isStartingAdvertising) {
                isStartingAdvertising = true
                logManager.d { "iOS mesh GATT service published, requesting BLE advertising" }
                peripheral.startAdvertising(
                    mapOf(
                        CBAdvertisementDataServiceUUIDsKey to listOf(
                            serviceUuid,
                        ),
                    ),
                )
            }
        } else if (pendingService == null) {
            val incoming = CBMutableCharacteristic(
                type = writeUuid,
                properties = CBCharacteristicPropertyWrite,
                value = null,
                permissions = CBAttributePermissionsWriteable,
            )
            val outgoing = CBMutableCharacteristic(
                type = notifyUuid,
                properties = CBCharacteristicPropertyNotify,
                value = null,
                permissions = 0uL,
            )
            val service = CBMutableService(type = serviceUuid, primary = true)
            service.setCharacteristics(listOf(incoming, outgoing))
            pendingService = service
            logManager.d { "iOS mesh publishing GATT service $MESH_SERVICE_UUID" }
            peripheral.addService(service)
        }
    }

    private fun findNotification(service: CBService): CBMutableCharacteristic? {
        return service.characteristics?.filterIsInstance<CBMutableCharacteristic>()
            ?.firstOrNull { it.UUID == notifyUuid }
    }

    private class Session(val central: CBCentral) {

        val peer = MeshPeer(sessionId = NSUUID().UUIDString)
    }
}

private const val NOTIFICATION_TIMEOUT_MS = 5_000L
