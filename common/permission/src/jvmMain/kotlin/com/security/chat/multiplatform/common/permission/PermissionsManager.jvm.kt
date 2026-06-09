package com.security.chat.multiplatform.common.permission

import androidx.compose.runtime.Composable
import com.security.chat.multiplatform.common.permission.entity.AllowanceResult
import com.security.chat.multiplatform.common.permission.entity.Permission
import com.security.chat.multiplatform.common.permission.entity.RequestPermissionLauncher

@Composable
public actual fun createLauncher(
    permission: Permission,
    onResult: (allowanceResult: AllowanceResult) -> Unit,
): RequestPermissionLauncher {
    return object : RequestPermissionLauncher {
        override fun request() {
            onResult(AllowanceResult.Allowed)
        }
    }
}