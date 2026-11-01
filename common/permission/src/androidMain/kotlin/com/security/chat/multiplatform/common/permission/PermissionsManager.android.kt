package com.security.chat.multiplatform.common.permission

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

    val activityLauncher: ManagedActivityResultLauncher<String, Boolean> =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { result ->
                when (result) {
                    true -> onResult(AllowanceResult.Allowed)
                    false -> onResult(AllowanceResult.Restricted)
                }
            },
        )

    return remember(activityLauncher, permission) {
        RequestPermissionLauncherImpl(
            launcher = activityLauncher,
            permission = permission,
        )
    }
}

private class RequestPermissionLauncherImpl(
    private val launcher: ManagedActivityResultLauncher<String, Boolean>,
    private val permission: Permission,
) : RequestPermissionLauncher {

    override fun request() {
        val permissionString = permission.toAndroid()
        launcher.launch(permissionString)
    }
}
