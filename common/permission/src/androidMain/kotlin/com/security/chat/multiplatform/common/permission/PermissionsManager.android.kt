package com.security.chat.multiplatform.common.permission

import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.security.chat.multiplatform.common.permission.entity.AllowanceResult
import com.security.chat.multiplatform.common.permission.entity.Permission
import com.security.chat.multiplatform.common.permission.entity.RequestPermissionLauncher
import com.security.chat.multiplatform.common.permission.mapper.toAndroid

@Composable
public actual fun createLauncher(
    permission: Permission,
    onResult: (allowanceResult: AllowanceResult) -> Unit,
): RequestPermissionLauncher {
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
