package com.security.chat.multiplatform.features.mesh.data

import android.app.PendingIntent
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import org.koin.mp.KoinPlatform

public interface MeshScanWakeHandler {

    public fun onMeshScanWake()
}

public class MeshScanReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_MESH_SCAN_RESULT) return

        val bluetoothManager = KoinPlatform.getKoin().get<BluetoothManagerImpl>()
        val error = intent.getIntExtra(BluetoothLeScanner.EXTRA_ERROR_CODE, 0)
        if (error != 0) {
            bluetoothManager.onBackgroundScanFailed(error)
            return
        }

        val results = intent.getScanResults()
        if (results.isEmpty()) return
        bluetoothManager.onBackgroundScanResults(results)
        (context.applicationContext as? MeshScanWakeHandler)?.onMeshScanWake()
    }
}

internal fun meshScanPendingIntent(context: Context): PendingIntent {
    val intent = Intent(context, MeshScanReceiver::class.java).setAction(ACTION_MESH_SCAN_RESULT)
    val mutability = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        PendingIntent.FLAG_MUTABLE
    } else {
        0
    }
    return PendingIntent.getBroadcast(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or mutability,
    )
}

@Suppress("DEPRECATION")
private fun Intent.getScanResults(): List<ScanResult> {
    val key = BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableArrayListExtra(key, ScanResult::class.java).orEmpty()
    } else {
        getParcelableArrayListExtra<ScanResult>(key).orEmpty()
    }
}

private const val ACTION_MESH_SCAN_RESULT = "com.security.chat.multiplatform.mesh.SCAN_RESULT"
