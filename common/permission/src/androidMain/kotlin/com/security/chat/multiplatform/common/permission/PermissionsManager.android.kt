package com.security.chat.multiplatform.common.permission

import androidx.activity.compose.LocalActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import com.security.chat.multiplatform.common.log.Log
import com.security.chat.multiplatform.common.permission.entity.AllowanceResult
import com.security.chat.multiplatform.common.permission.entity.Permission
import com.security.chat.multiplatform.common.permission.entity.RequestPermissionLauncher
import com.security.chat.multiplatform.common.permission.mapper.toAndroid

@Composable
public actual fun createLauncher(
    permission: Permission,
    onResult: (allowanceResult: AllowanceResult) -> Unit,
): RequestPermissionLauncher {
    val activity = LocalActivity.current

    val activityLauncher: ManagedActivityResultLauncher<String, Boolean> =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { result ->
                when (result) {
                    true -> onResult(AllowanceResult.Allowed)
                    false -> {
                        val permissionString = permission.toAndroid()

                        val shouldShowRequestPermissionRationale = if (activity == null) {
                            Log.e("local activity is null")
                            false
                        } else {
                            shouldShowRequestPermissionRationale(
                                activity,
                                permissionString,
                            )
                        }
                        onResult(
                            AllowanceResult.Restricted(
                                isPermanent = !shouldShowRequestPermissionRationale,
                            ),
                        )
                    }
                }
            },
        )

    return remember(activityLauncher) {
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