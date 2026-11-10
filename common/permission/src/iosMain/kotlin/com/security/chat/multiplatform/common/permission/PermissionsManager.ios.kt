package com.security.chat.multiplatform.common.permission

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import com.security.chat.multiplatform.common.log.Log
import com.security.chat.multiplatform.common.permission.entity.AllowanceResult
import com.security.chat.multiplatform.common.permission.entity.Permission
import com.security.chat.multiplatform.common.permission.entity.RequestPermissionLauncher
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreBluetooth.CBCentralManager
import platform.CoreBluetooth.CBCentralManagerDelegateProtocol
import platform.CoreBluetooth.CBCentralManagerOptionShowPowerAlertKey
import platform.CoreBluetooth.CBManager
import platform.CoreBluetooth.CBManagerAuthorizationAllowedAlways
import platform.CoreBluetooth.CBManagerAuthorizationNotDetermined
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNUserNotificationCenter
import platform.darwin.NSObject
import kotlin.coroutines.resume

@Composable
public actual fun createLauncher(
    permission: Permission,
    onResult: (allowanceResult: AllowanceResult) -> Unit,
): RequestPermissionLauncher {
    val scope = rememberCoroutineScope()
    val currentOnResult by rememberUpdatedState(onResult)
    return remember(permission, scope) {
        object : RequestPermissionLauncher {
            override fun request() {
                scope.launch {
                    val result = when (permission) {
                        Permission.Notifications -> requestNotificationPermission()
                        Permission.Bluetooth -> requestBluetoothPermission()
                        else -> AllowanceResult.Allowed
                    }
                    currentOnResult(result)
                }
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
internal fun getBluetoothAuthorization(): AllowanceResult? {
    return when (CBManager.authorization) {
        CBManagerAuthorizationAllowedAlways -> AllowanceResult.Allowed
        CBManagerAuthorizationNotDetermined -> null
        else -> AllowanceResult.Restricted
    }
}

public actual fun bluetoothScanRequiresLocation(): Boolean = false

@OptIn(ExperimentalForeignApi::class)
private suspend fun requestNotificationPermission(): AllowanceResult =
    suspendCancellableCoroutine { continuation ->
        UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(
            options = UNAuthorizationOptionAlert or UNAuthorizationOptionBadge or
                    UNAuthorizationOptionSound,
            completionHandler = { granted, error ->
                if (error != null) {
                    Log.e("Failed to request notification permission: ${error.localizedDescription}")
                }
                if (continuation.isActive) {
                    continuation.resume(
                        if (granted) AllowanceResult.Allowed else AllowanceResult.Restricted,
                    )
                }
            },
        )
    }

@OptIn(ExperimentalForeignApi::class)
private suspend fun requestBluetoothPermission(): AllowanceResult {
    return getBluetoothAuthorization() ?: BluetoothPermissionRequester().request()
}

@OptIn(ExperimentalForeignApi::class)
private class BluetoothPermissionRequester : NSObject(), CBCentralManagerDelegateProtocol {

    private val result = CompletableDeferred<AllowanceResult>()

    suspend fun request(): AllowanceResult {
        val manager = CBCentralManager(
            delegate = this,
            queue = null,
            options = mapOf(CBCentralManagerOptionShowPowerAlertKey to false),
        )
        try {
            return result.await()
        } finally {
            manager.delegate = null
        }
    }

    override fun centralManagerDidUpdateState(central: CBCentralManager) {
        getBluetoothAuthorization()?.let(result::complete)
    }
}
