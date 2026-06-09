package com.security.chat.multiplatform.common.permission

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.content.ContextCompat
import com.security.chat.multiplatform.common.permission.entity.AllowanceResult
import com.security.chat.multiplatform.common.permission.entity.Permission
import com.security.chat.multiplatform.common.permission.mapper.toAndroid
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


internal actual class PermissionsManagerImpl : PermissionsManager, KoinComponent {

    private val context: Context by inject()

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

            PackageManager.PERMISSION_DENIED -> {
                val activity: Activity by inject()
                val shouldShowRequestPermissionRationale = shouldShowRequestPermissionRationale(
                    activity,
                    permissionString,
                )
                AllowanceResult.Restricted(
                    isPermanent = !shouldShowRequestPermissionRationale,
                )
            }

            else -> {
                error("Unknown permission result: $resultInt")
            }
        }
    }
}
