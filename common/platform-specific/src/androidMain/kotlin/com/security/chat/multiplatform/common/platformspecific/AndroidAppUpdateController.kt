package com.security.chat.multiplatform.common.platformspecific

import android.app.Application
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.tasks.Task
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class AndroidAppUpdateController(application: Application) {
    private val appUpdateManager = AppUpdateManagerFactory.create(application)

    suspend fun checkForUpdates(): AppUpdateInfo = appUpdateManager.appUpdateInfo.awaitResult()

    suspend fun completeUpdate() {
        appUpdateManager.completeUpdate().awaitResult()
    }

    fun startUpdateFlow(
        info: AppUpdateInfo,
        launcher: ActivityResultLauncher<IntentSenderRequest>,
    ): Boolean =
        appUpdateManager.startUpdateFlowForResult(
            info,
            launcher,
            AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
        )

    fun registerListener(listener: InstallStateUpdatedListener) {
        appUpdateManager.registerListener(listener)
    }

    fun unregisterListener(listener: InstallStateUpdatedListener) {
        appUpdateManager.unregisterListener(listener)
    }
}

private suspend fun <T> Task<T>.awaitResult(): T =
    suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            when {
                task.isCanceled -> continuation.cancel()
                task.isSuccessful -> continuation.resume(task.result)
                else -> continuation.resumeWithException(
                    task.exception ?: IllegalStateException("Google Play update task failed"),
                )
            }
        }
    }
