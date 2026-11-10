package com.security.chat.multiplatform.common.permission

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.security.chat.multiplatform.common.permission.entity.AllowanceResult
import com.security.chat.multiplatform.common.permission.entity.Permission
import com.security.chat.multiplatform.common.permission.entity.RequestPermissionLauncher
import com.security.chat.multiplatform.common.permission.mapper.toAndroid

@Composable
public actual fun createLauncher(
    permission: Permission,
    onResult: (allowanceResult: AllowanceResult) -> Unit,
): RequestPermissionLauncher {
    if (permission == Permission.Notifications &&
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
    ) {
        val context = LocalContext.current
        val currentOnResult by rememberUpdatedState(onResult)
        return remember(context) {
            object : RequestPermissionLauncher {
                override fun request() {
                    currentOnResult(
                        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                            AllowanceResult.Allowed
                        } else {
                            AllowanceResult.Restricted
                        },
                    )
                }
            }
        }
    }

    if (permission == Permission.BluetoothBackgroundLocation && Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
        return backgroundLocationSettingsLauncher(onResult)
    }

    val permissions = remember(permission) { permission.toAndroid() }
    val currentOnResult by rememberUpdatedState(onResult)
    val activityLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>> =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
            onResult = { results ->
                val allowed = permissions.all { results[it] == true }
                currentOnResult(if (allowed) AllowanceResult.Allowed else AllowanceResult.Restricted)
            },
        )

    return remember(activityLauncher, permissions) {
        object : RequestPermissionLauncher {
            override fun request() {
                if (permissions.isEmpty()) {
                    currentOnResult(AllowanceResult.Allowed)
                } else {
                    activityLauncher.launch(permissions)
                }
            }
        }
    }
}

public actual fun bluetoothScanRequiresLocation(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.S

@Composable
private fun backgroundLocationSettingsLauncher(
    onResult: (AllowanceResult) -> Unit,
): RequestPermissionLauncher {
    val context = LocalContext.current
    val currentOnResult by rememberUpdatedState(onResult)
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val allowed = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
            currentOnResult(if (allowed) AllowanceResult.Allowed else AllowanceResult.Restricted)
        }
    return remember(context, launcher) {
        object : RequestPermissionLauncher {
            override fun request() {
                launcher.launch(context.appSettingsIntent())
            }
        }
    }
}
