package com.security.chat.multiplatform.common.platform

import android.content.Context
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.security.chat.multiplatform.features.mesh.domain.LogManager
import com.security.chat.multiplatform.features.mesh.domain.MeshModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

internal fun schedulePeriodicMeshSync(context: Context) {
    val request = PeriodicWorkRequestBuilder<MeshSyncWorker>(15, TimeUnit.MINUTES).build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        uniqueWorkName = "mesh_sync",
        existingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.KEEP,
        request = request,
    )
}

internal fun scheduleMeshDiscoverySync(context: Context) {
    val request = OneTimeWorkRequestBuilder<MeshSyncWorker>()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        request.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
    }
    WorkManager.getInstance(context).enqueueUniqueWork(
        uniqueWorkName = "mesh_sync_on_discovery",
        existingWorkPolicy = ExistingWorkPolicy.KEEP,
        request = request.build(),
    )
}

internal class MeshSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val meshModel: MeshModel by inject()
    private val logManager: LogManager by inject()

    override suspend fun doWork(): Result {
        logManager.d { "MeshSyncWorker invoking sync" }
        meshModel.sync()
        return Result.success()
    }
}
