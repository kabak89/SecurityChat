package com.security.chat.multiplatform.features.mesh.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import android.os.Build
import android.os.Handler
import com.security.chat.multiplatform.features.mesh.domain.LogManager
import com.security.chat.multiplatform.features.mesh.domain.entity.MeshPeer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

@SuppressLint("MissingPermission")
@Suppress("DEPRECATION")
internal class MeshGattClient(
    private val context: Context,
    private val device: BluetoothDevice,
    private val handler: Handler,
    private val logManager: LogManager,
    private val onPacket: (MeshPeer, ByteArray) -> Boolean,
    private val onStateChanged: (MeshGattClient) -> Unit,
) : BluetoothGattCallback() {

    private val peer = MeshPeer(sessionId = UUID.randomUUID().toString())
    private var gatt: BluetoothGatt? = null
    private var outgoing: BluetoothGattCharacteristic? = null
    private var pendingWrite: CompletableDeferred<Int>? = null
    private val setupTimeout = Runnable { fail("Mesh GATT setup timed out") }
    var readyPeer: MeshPeer? = null
        private set

    fun connect() {
        gatt = device.connectGatt(context, false, this, BluetoothDevice.TRANSPORT_LE)
        if (gatt == null) {
            fail("Could not open a mesh GATT connection")
            return
        }
        handler.postDelayed(setupTimeout, SETUP_TIMEOUT_MS)
    }

    fun close() {
        handler.removeCallbacks(setupTimeout)
        val connection = gatt
        gatt = null
        readyPeer = null
        outgoing = null
        pendingWrite?.complete(BluetoothGatt.GATT_FAILURE)
        connection?.close()
        onStateChanged(this)
    }

    suspend fun send(payload: ByteArray) {
        val connection = checkNotNull(gatt) { "Mesh GATT connection is closed" }
        val characteristic = checkNotNull(outgoing) { "Mesh write characteristic is unavailable" }
        val result = CompletableDeferred<Int>()
        pendingWrite = result
        try {
            val writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            val accepted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                connection.writeCharacteristic(characteristic, payload, writeType) ==
                        BluetoothStatusCodes.SUCCESS
            } else {
                characteristic.writeType = writeType
                characteristic.value = payload
                connection.writeCharacteristic(characteristic)
            }
            check(accepted) { "Bluetooth rejected the mesh write" }
            val status = withTimeoutOrNull(WRITE_TIMEOUT_MS) { result.await() }
            if (status == null) close()
            check(status == BluetoothGatt.GATT_SUCCESS) { "Mesh GATT write failed: $status" }
        } finally {
            pendingWrite = null
        }
    }

    override fun onConnectionStateChange(connection: BluetoothGatt, status: Int, newState: Int) {
        handler.post {
            if (gatt !== connection) return@post
            when {
                status != BluetoothGatt.GATT_SUCCESS -> fail("Mesh GATT connection failed: $status")
                newState == BluetoothProfile.STATE_DISCONNECTED -> close()
                newState == BluetoothProfile.STATE_CONNECTED && !connection.discoverServices() ->
                    fail("Could not discover the mesh GATT service")
            }
        }
    }

    override fun onServicesDiscovered(connection: BluetoothGatt, status: Int) {
        handler.post {
            if (gatt !== connection) return@post
            if (status != BluetoothGatt.GATT_SUCCESS) {
                fail("Mesh GATT service discovery failed: $status")
                return@post
            }
            logManager.d { "Android mesh GATT services discovered" }
            val service = connection.getService(SERVICE_UUID)
            val write = service?.getCharacteristic(WRITE_UUID)
            val config = service?.getCharacteristic(NOTIFY_UUID)?.getDescriptor(CLIENT_CONFIG_UUID)
            if (write == null || write.properties and BluetoothGattCharacteristic.PROPERTY_WRITE == 0 ||
                config == null || !enableNotifications(connection, config)
            ) {
                fail("Could not configure the mesh GATT characteristics")
                return@post
            }
            outgoing = write
        }
    }

    override fun onDescriptorWrite(
        connection: BluetoothGatt,
        descriptor: BluetoothGattDescriptor,
        status: Int,
    ) {
        handler.post {
            if (gatt !== connection || descriptor.uuid != CLIENT_CONFIG_UUID) return@post
            if (status != BluetoothGatt.GATT_SUCCESS) {
                fail("Mesh GATT subscription failed: $status")
                return@post
            }
            handler.removeCallbacks(setupTimeout)
            readyPeer = peer
            logManager.d { "Android mesh GATT subscription confirmed" }
            onStateChanged(this)
        }
    }

    override fun onCharacteristicWrite(
        connection: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        status: Int,
    ) {
        handler.post {
            if (gatt === connection && characteristic.uuid == WRITE_UUID) {
                pendingWrite?.complete(status)
            }
        }
    }

    override fun onCharacteristicChanged(
        connection: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
    ) {
        receive(connection = connection, characteristic = characteristic, value = value)
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onCharacteristicChanged(
        connection: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            val value = characteristic.value?.copyOf() ?: return
            receive(connection = connection, characteristic = characteristic, value = value)
        }
    }

    private fun enableNotifications(
        connection: BluetoothGatt,
        config: BluetoothGattDescriptor,
    ): Boolean {
        if (!connection.setCharacteristicNotification(config.characteristic, true)) return false
        val value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            connection.writeDescriptor(config, value) == BluetoothStatusCodes.SUCCESS
        } else {
            config.value = value
            connection.writeDescriptor(config)
        }
    }

    private fun receive(
        connection: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
    ) {
        handler.post {
            if (gatt !== connection || readyPeer == null || characteristic.uuid != NOTIFY_UUID) {
                return@post
            }
            if (!onPacket(peer, value)) logManager.e("Could not queue a mesh Bluetooth packet")
        }
    }

    private fun fail(message: String) {
        logManager.e(message)
        close()
    }
}

private const val SETUP_TIMEOUT_MS = 20_000L
private const val WRITE_TIMEOUT_MS = 5_000L
