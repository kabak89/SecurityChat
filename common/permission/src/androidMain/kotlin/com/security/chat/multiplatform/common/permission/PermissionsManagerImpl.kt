package com.security.chat.multiplatform.common.permission

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null),
        )
        if (activity == null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        (activity ?: context).startActivity(intent)
    }

    override fun isPermissionAllowed(permission: Permission): AllowanceResult {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU &&
            permission == Permission.Notifications
        ) {
            return AllowanceResult.Allowed
        }

        val permissionString = permission.toAndroid()
        val resultInt = ContextCompat.checkSelfPermission(context, permissionString)

        return when (resultInt) {
            PackageManager.PERMISSION_GRANTED -> AllowanceResult.Allowed
            PackageManager.PERMISSION_DENIED -> AllowanceResult.Restricted

            else -> {
                error("Unknown permission result: $resultInt")
            }
        }
    }
}
