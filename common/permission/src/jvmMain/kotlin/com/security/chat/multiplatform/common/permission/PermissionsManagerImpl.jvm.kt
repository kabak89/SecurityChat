package com.security.chat.multiplatform.common.permission

import com.security.chat.multiplatform.common.permission.entity.AllowanceResult
import com.security.chat.multiplatform.common.permission.entity.Permission

internal actual class PermissionsManagerImpl : PermissionsManager {

    override fun isPermissionAllowed(permission: Permission): AllowanceResult {
        //TODO add actual realization
        return AllowanceResult.Allowed
    }
}