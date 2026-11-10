package com.security.chat.multiplatform.common.permission

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.security.chat.multiplatform.common.permission.entity.AllowanceResult
import com.security.chat.multiplatform.common.permission.entity.Permission
import com.security.chat.multiplatform.common.permission.mapper.toAndroid
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

internal actual class PermissionsManagerImpl : PermissionsManager, KoinComponent {

    private val context: Context by inject()

    override fun openAppSettings() {
        val activity = getKoin().getOrNull<Activity>()
        val intent = context.appSettingsIntent()
        if (activity == null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        (activity ?: context).startActivity(intent)
    }

    override fun isPermissionAllowed(permission: Permission): AllowanceResult {
        if (permission == Permission.Notifications) {
            return if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                AllowanceResult.Allowed
            } else {
                AllowanceResult.Restricted
            }
        }

        if (permission == Permission.LocalNetwork &&
            (Build.VERSION.SDK_INT < LOCAL_NETWORK_PERMISSION_MIN_SDK ||
                    context.applicationInfo.targetSdkVersion < LOCAL_NETWORK_PERMISSION_MIN_SDK)
        ) {
            return AllowanceResult.Allowed
        }

        val allowed = permission.toAndroid().all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        return if (allowed) {
            AllowanceResult.Allowed
        } else {
            AllowanceResult.Restricted
        }
    }

    override fun bluetoothScanRequiresBackgroundLocation(): Boolean {
        return Permission.BluetoothBackgroundLocation.toAndroid().isNotEmpty()
    }
}

private const val LOCAL_NETWORK_PERMISSION_MIN_SDK: Int = 37

internal fun Context.appSettingsIntent(): Intent {
    return Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null),
    )
}
