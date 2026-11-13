package com.security.chat.multiplatform.common.background.work

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.await
import com.security.chat.multiplatform.common.background.work.BackgroundWorkManager.Type
import com.security.chat.multiplatform.common.core.component.SCOPE_ID_APP
import com.security.chat.multiplatform.common.log.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import org.koin.mp.KoinPlatform
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

internal actual fun Scope.createBackgroundWorkManager(
    registry: BackgroundWorkerRegistry,
): BackgroundWorkManager {
    return AndroidBackgroundWorkManager(
        workManager = WorkManager.getInstance(get<Context>()),
        registry = registry,
        scope = get(named(SCOPE_ID_APP)),
    )
}

private class AndroidBackgroundWorkManager(
    private val workManager: WorkManager,
    private val registry: BackgroundWorkerRegistry,
    private val scope: CoroutineScope,
) : BackgroundWorkManager {

    private val mutex = Mutex()

    override suspend fun enqueue(
        key: String,
        worker: KClass<out BackgroundWorker>,
        conditions: BackgroundWorkConditions,
        type: Type,
        policy: BackgroundWorkPolicy,
        priority: BackgroundWorkPriority,
        input: Map<String, String>,
    ) {
        validateWorkRequest(
            key = key,
            schedule = conditions.schedule,
            policy = policy,
            priority = priority,
        )
        val name = "$WORK_NAME_PREFIX$key"
        val workerId = registry.get(worker).id
        val workInput = input.toMap()
        mutex.withLock {
            when (val schedule = conditions.schedule) {
                BackgroundWorkSchedule.OneShot -> {
                    if (policy != BackgroundWorkPolicy.KEEP) {
                        val existing = workManager.getWorkInfosForUniqueWorkFlow(name).first()
                        val hasActivePeriodicWork = existing.any {
                            it.periodicityInfo != null && !it.state.isFinished
                        }
                        require(!hasActivePeriodicWork) {
                            "Cannot append to repeating background work: $key"
                        }
                        val hasPendingWork = existing.any {
                            it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.BLOCKED
                        }
                        if (policy == BackgroundWorkPolicy.KEEP_PENDING && hasPendingWork) {
                            return@withLock
                        }
                    }
                    submitOneShot(
                        name = name,
                        workerId = workerId,
                        requiresInternet = conditions.requiresInternet,
                        type = type,
                        policy = policy,
                        priority = priority,
                        input = workInput,
                    ).await()
                }

                is BackgroundWorkSchedule.Repeatable -> {
                    val repeatMillis = schedule.interval.inWholeMilliseconds
                    require(repeatMillis >= PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS) {
                        "Android repeating work requires an interval of at least 15 minutes"
                    }
                    val workData = createWorkData(
                        workerId = workerId,
                        type = type,
                        legacyNotificationText = null,
                        input = workInput,
                    )
                    val request = PeriodicWorkRequestBuilder<AndroidBackgroundWorker>(
                        repeatInterval = repeatMillis,
                        repeatIntervalTimeUnit = TimeUnit.MILLISECONDS,
                    ).setConstraints(createConstraints(conditions.requiresInternet))
                        .setInputData(workData)
                        .build()
                    workManager.enqueueUniquePeriodicWork(
                        uniqueWorkName = name,
                        existingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.KEEP,
                        request = request,
                    ).await()
                }
            }
        }
    }

    override suspend fun cancel(key: String) {
        mutex.withLock { workManager.cancelUniqueWork("$WORK_NAME_PREFIX$key").await() }
    }

    override fun enqueueOneShot(
        key: String,
        worker: KClass<out BackgroundWorker>,
        requiresInternet: Boolean,
        type: Type,
        policy: BackgroundWorkPolicy,
        priority: BackgroundWorkPriority,
        input: Map<String, String>,
    ): Deferred<Unit> {
        validateWorkRequest(
            key = key,
            schedule = BackgroundWorkSchedule.OneShot,
            policy = policy,
            priority = priority,
        )
        if (policy == BackgroundWorkPolicy.KEEP_PENDING) {
            return scope.async(start = CoroutineStart.UNDISPATCHED) {
                enqueue(
                    key = key,
                    worker = worker,
                    conditions = BackgroundWorkConditions(
                        schedule = BackgroundWorkSchedule.OneShot,
                        requiresInternet = requiresInternet,
                    ),
                    type = type,
                    policy = policy,
                    priority = priority,
                    input = input,
                )
            }
        }
        val operation = submitOneShot(
            name = "$WORK_NAME_PREFIX$key",
            workerId = registry.get(worker).id,
            requiresInternet = requiresInternet,
            type = type,
            policy = policy,
            priority = priority,
            input = input,
        )
        return scope.async {
            operation.await()
            Unit
        }
    }

    private fun submitOneShot(
        name: String,
        workerId: String,
        requiresInternet: Boolean,
        type: Type,
        policy: BackgroundWorkPolicy,
        priority: BackgroundWorkPriority,
        input: Map<String, String>,
    ): Operation {
        val legacyNotificationText = when (priority) {
            BackgroundWorkPriority.Normal -> null
            is BackgroundWorkPriority.Expedited -> {
                (priority.legacyForeground as? Type.Foreground)?.notificationText
            }
        }
        val workData = createWorkData(
            workerId = workerId,
            type = type,
            legacyNotificationText = legacyNotificationText,
            input = input,
        )
        val builder = OneTimeWorkRequestBuilder<AndroidBackgroundWorker>()
            .setConstraints(createConstraints(requiresInternet))
            .setInputData(workData)
        val canExpedite = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ||
                legacyNotificationText != null
        if (priority is BackgroundWorkPriority.Expedited && canExpedite) {
            builder.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
        }
        val existingPolicy = when (policy) {
            BackgroundWorkPolicy.KEEP -> ExistingWorkPolicy.KEEP
            BackgroundWorkPolicy.APPEND_OR_REPLACE,
            BackgroundWorkPolicy.KEEP_PENDING,
                -> ExistingWorkPolicy.APPEND_OR_REPLACE
        }
        return workManager.enqueueUniqueWork(
            uniqueWorkName = name,
            existingWorkPolicy = existingPolicy,
            request = builder.build(),
        )
    }

    private fun createWorkData(
        workerId: String,
        type: Type,
        legacyNotificationText: String?,
        input: Map<String, String>,
    ): Data {
        return Data.Builder()
            .putString(KEY_WORKER_ID, workerId)
            .putString(KEY_NOTIFICATION_TEXT, (type as? Type.Foreground)?.notificationText)
            .apply {
                legacyNotificationText?.let { putString(KEY_LEGACY_NOTIFICATION_TEXT, it) }
            }
            .putAll(input.mapKeys { (key, _) -> "$INPUT_KEY_PREFIX$key" })
            .build()
    }

    private fun createConstraints(requiresInternet: Boolean): Constraints {
        val networkType = if (requiresInternet) NetworkType.CONNECTED else NetworkType.NOT_REQUIRED
        return Constraints.Builder().setRequiredNetworkType(networkType).build()
    }
}

/** Instantiated by WorkManager after process recreation. */
internal class AndroidBackgroundWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val workerId = inputData.getString(KEY_WORKER_ID)
        val registry = KoinPlatform.getKoin().get<BackgroundWorkerRegistry>()
        val worker = workerId?.let(registry::find)
        if (worker == null) {
            Log.e("Unknown Android background worker ID: $workerId")
            return Result.failure()
        }

        return try {
            val input = inputData.keyValueMap.filterKeys { it.startsWith(INPUT_KEY_PREFIX) }
                .mapKeys { (key, _) -> key.removePrefix(INPUT_KEY_PREFIX) }
                .mapValues { (key, value) ->
                    checkNotNull(value as? String) { "Invalid background worker input: $key" }
                }
            inputData.getString(KEY_NOTIFICATION_TEXT)?.let { text ->
                try {
                    setForeground(foregroundInfo(text))
                } catch (error: IllegalStateException) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        error is ForegroundServiceStartNotAllowedException
                    ) {
                        Log.d { "Retrying $workerId after a foreground service start restriction" }
                        return Result.retry()
                    }
                    throw error
                }
            }
            if (worker.run(input)) Result.success() else Result.failure()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.e(error, "Could not start Android background worker $workerId")
            Result.failure()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val text = inputData.getString(KEY_NOTIFICATION_TEXT)
            ?: inputData.getString(KEY_LEGACY_NOTIFICATION_TEXT)
        return foregroundInfo(checkNotNull(text) { "Foreground work requires notification text" })
    }

    private suspend fun foregroundInfo(text: String): ForegroundInfo {
        val customNotification = inputData.getString(KEY_WORKER_ID)?.let { workerId ->
            KoinPlatform.getKoin().getOrNull<ForegroundNotificationFactory>(named(workerId))
        }?.create(text)
        if (customNotification != null) {
            return ForegroundInfo(
                /* notificationId = */ customNotification.id,
                /* notification = */ customNotification.notification,
                /* foregroundServiceType = */ customNotification.serviceType,
            )
        }

        val context = applicationContext
        val appName = context.applicationInfo.loadLabel(context.packageManager)
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                /* id = */ CHANNEL_ID,
                /* name = */ appName,
                /* importance = */ NotificationManager.IMPORTANCE_LOW,
            ),
        )
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(appName)
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setOngoing(true)
            .build()
        val notificationId = id.hashCode().takeUnless { it == 0 } ?: Int.MIN_VALUE

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                /* notificationId = */ notificationId,
                /* notification = */ notification,
                /* foregroundServiceType = */ ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }
}

private const val WORK_NAME_PREFIX = "securitychat.background-work:"
private const val KEY_WORKER_ID = "worker-id"
private const val KEY_NOTIFICATION_TEXT = "notification-text"
private const val KEY_LEGACY_NOTIFICATION_TEXT = "legacy-notification-text"
private const val INPUT_KEY_PREFIX = "input:"
private const val CHANNEL_ID = "securitychat.background-work"
