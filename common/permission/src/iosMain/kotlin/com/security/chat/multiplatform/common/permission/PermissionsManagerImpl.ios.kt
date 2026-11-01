package com.security.chat.multiplatform.common.permission

import com.security.chat.multiplatform.common.log.Log
import com.security.chat.multiplatform.common.permission.entity.AllowanceResult
import com.security.chat.multiplatform.common.permission.entity.Permission
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusDenied
import platform.UserNotifications.UNAuthorizationStatusEphemeral
import platform.UserNotifications.UNAuthorizationStatusNotDetermined
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
internal actual class PermissionsManagerImpl : PermissionsManager {

    override fun openAppSettings() {
        val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
        UIApplication.sharedApplication.openURL(
            url = url,
            options = emptyMap<Any?, Any>(),
            completionHandler = null,
        )
    }

    override suspend fun checkPermission(permission: Permission): AllowanceResult {
        if (permission != Permission.Notifications) return isPermissionAllowed(permission)

        return suspendCancellableCoroutine { continuation ->
            UNUserNotificationCenter.currentNotificationCenter()
                .getNotificationSettingsWithCompletionHandler { settings ->
                    val result = when (val status = settings?.authorizationStatus) {
                        UNAuthorizationStatusAuthorized,
                        UNAuthorizationStatusProvisional,
                        UNAuthorizationStatusEphemeral,
                            -> AllowanceResult.Allowed

                        UNAuthorizationStatusNotDetermined,
                        UNAuthorizationStatusDenied,
                            -> AllowanceResult.Restricted

                        else -> {
                            Log.e("Unknown notification authorization status: $status")
                            AllowanceResult.Restricted
                        }
                    }
                    if (continuation.isActive) {
                        continuation.resume(result)
                    }
                }
        }
    }

    override fun isPermissionAllowed(permission: Permission): AllowanceResult {
        //TODO add actual realization
        return AllowanceResult.Allowed
    }
}
