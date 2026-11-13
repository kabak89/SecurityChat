@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.security.chat.multiplatform.common.background.work

import com.security.chat.multiplatform.common.background.work.BackgroundWorkManager.Type
import com.security.chat.multiplatform.common.core.component.SCOPE_ID_APP
import com.security.chat.multiplatform.common.core.network.ConnectivityObserver
import com.security.chat.multiplatform.common.log.Log
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import org.koin.mp.KoinPlatform
import platform.BackgroundTasks.BGProcessingTaskRequest
import platform.BackgroundTasks.BGTask
import platform.BackgroundTasks.BGTaskScheduler
import platform.Foundation.NSDate
import platform.Foundation.NSError
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSUserDefaults
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeIntervalSince1970
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationState
import kotlin.reflect.KClass

/** Call from the application's launch delegate after loading its Koin modules. */
public fun registerIosBackgroundWork() {
    val manager = KoinPlatform.getKoin().get<BackgroundWorkManager>() as IosBackgroundWorkManager
    manager.register()
}

internal actual fun Scope.createBackgroundWorkManager(
    registry: BackgroundWorkerRegistry,
): BackgroundWorkManager {
    return IosBackgroundWorkManager(
        scope = get(named(SCOPE_ID_APP)),
        registry = registry,
        connectivityObserver = get(),
    )
}

private class IosBackgroundWorkManager(
    private val scope: CoroutineScope,
    private val registry: BackgroundWorkerRegistry,
    private val connectivityObserver: ConnectivityObserver,
) : BackgroundWorkManager {

    private val scheduler = BGTaskScheduler.sharedScheduler
    private val preferences = NSUserDefaults.standardUserDefaults

    /** Older queue entries omit the input field. */
    private val queueJson = Json { explicitNulls = false }
    private val mutex = Mutex()
    private val executionMutex = Mutex()
    private val foregroundWakeups = Channel<Unit>(Channel.CONFLATED)
    private val scheduled = loadWork()
    private val running = mutableMapOf<String, RunningWork>()
    private val activeBatches = mutableSetOf<Boolean>()

    fun register() {
        for (requiresInternet in listOf(false, true)) {
            val identifier = requestIdentifier(requiresInternet)
            val registered = scheduler.registerForTaskWithIdentifier(
                identifier = identifier,
                usingQueue = null,
                launchHandler = { task -> task?.let { launch(it, requiresInternet) } },
            )
            check(registered) { "Missing iOS background task identifier $identifier in Info.plist" }
        }
        NSNotificationCenter.defaultCenter.addObserverForName(
            name = UIApplicationDidBecomeActiveNotification,
            `object` = null,
            queue = null,
        ) {
            foregroundWakeups.trySend(Unit)
        }
        foregroundWakeups.receiveAsFlow()
            .onEach { runForegroundWork() }
            .launchIn(scope)
        connectivityObserver.isOnline
            .filter { it }
            .onEach { foregroundWakeups.trySend(Unit) }
            .launchIn(scope)
        scope.launch {
            mutex.withLock {
                reschedule(false)
                reschedule(true)
            }
        }
    }

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
        val workerId = registry.get(worker).id
        val workInput = input.toMap()
        mutex.withLock {
            val previous = scheduled[key].orEmpty()
            val head = previous.firstOrNull()
            if (head != null) {
                if (policy != BackgroundWorkPolicy.KEEP) {
                    require(head.repeatMillis == null) {
                        "Cannot append to repeating background work: $key"
                    }
                }
                val runningCount = if (running[key]?.work === head) 1 else 0
                val keepPending =
                    policy == BackgroundWorkPolicy.KEEP_PENDING && previous.size > runningCount
                if (policy == BackgroundWorkPolicy.KEEP || keepPending) {
                    reschedule(head.requiresInternet)
                    return@withLock
                }
            }
            val work = ScheduledWork(
                workerId = workerId,
                repeatMillis = when (val schedule = conditions.schedule) {
                    BackgroundWorkSchedule.OneShot -> null
                    is BackgroundWorkSchedule.Repeatable -> schedule.interval.inWholeMilliseconds
                },
                requiresInternet = conditions.requiresInternet,
                dueAt = nowMillis(),
                input = workInput,
            )
            scheduled[key] = previous + work
            saveWork()
            reschedule((head ?: work).requiresInternet)
        }
        if (conditions.schedule == BackgroundWorkSchedule.OneShot) {
            foregroundWakeups.trySend(Unit)
        }
    }

    override suspend fun cancel(key: String) {
        val job = mutex.withLock {
            scheduled.remove(key)?.let { queue ->
                saveWork()
                reschedule(queue.first().requiresInternet)
            }
            running[key]?.job?.also { it.cancel() }
        }
        job?.join()
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

    private suspend fun runForegroundWork() {
        executionMutex.withLock {
            val isActive = withContext(Dispatchers.Main) {
                UIApplication.sharedApplication.applicationState ==
                        UIApplicationState.UIApplicationStateActive
            }
            if (!isActive) return
            runDueWork(requiresInternet = false, oneShotOnly = true)
            if (connectivityObserver.isOnline.value) {
                runDueWork(requiresInternet = true, oneShotOnly = true)
            }
        }
    }

    private fun launch(task: BGTask, requiresInternet: Boolean) {
        var succeeded = false
        val job = scope.launch(start = CoroutineStart.LAZY) {
            succeeded = executionMutex.withLock { runBatch(requiresInternet) }
        }
        task.expirationHandler = { job.cancel() }
        job.invokeOnCompletion { error ->
            task.expirationHandler = null
            task.setTaskCompletedWithSuccess(error == null && succeeded)
        }
        job.start()
    }

    private suspend fun runBatch(requiresInternet: Boolean): Boolean {
        mutex.withLock {
            if (!activeBatches.add(requiresInternet)) return true
            reschedule(requiresInternet)
        }
        try {
            return runDueWork(requiresInternet = requiresInternet, oneShotOnly = false)
        } finally {
            withContext(NonCancellable) {
                mutex.withLock {
                    activeBatches.remove(requiresInternet)
                    reschedule(requiresInternet)
                }
            }
        }
    }

    private suspend fun runDueWork(requiresInternet: Boolean, oneShotOnly: Boolean): Boolean {
        val due = mutex.withLock {
            val now = nowMillis()
            scheduled.filterValues { queue ->
                val head = queue.first()
                head.requiresInternet == requiresInternet &&
                        head.dueAt <= now &&
                        (!oneShotOnly || head.repeatMillis == null)
            }
        }
        return supervisorScope {
            due.map { (key, queue) ->
                async {
                    runQueue(
                        key = key,
                        queue = queue,
                        requiresInternet = requiresInternet,
                        foreground = oneShotOnly,
                    )
                }
            }.awaitAll().all { it }
        }
    }

    private suspend fun runQueue(
        key: String,
        queue: List<ScheduledWork>,
        requiresInternet: Boolean,
        foreground: Boolean,
    ): Boolean {
        for (work in queue) {
            if (work.requiresInternet != requiresInternet || work.dueAt > nowMillis()) break
            if (foreground && requiresInternet && !connectivityObserver.isOnline.value) break
            if (!runWorker(key, work)) return false
        }
        return true
    }

    private suspend fun runWorker(key: String, work: ScheduledWork): Boolean {
        return supervisorScope {
            val job = mutex.withLock {
                if (scheduled[key]?.first() !== work || key in running) return@supervisorScope true
                async(start = CoroutineStart.LAZY) {
                    val worker = registry.find(work.workerId)
                    if (worker == null) {
                        Log.e("Unknown iOS background worker ID: ${work.workerId}")
                        false
                    } else {
                        worker.run(work.input.orEmpty())
                    }
                }.also { running[key] = RunningWork(work, it) }
            }

            var result: Boolean? = null
            try {
                job.await().also { result = it }
            } catch (_: CancellationException) {
                currentCoroutineContext().ensureActive()
                result = false
                mutex.withLock { scheduled[key]?.first() !== work }
            } finally {
                withContext(NonCancellable) {
                    job.join()
                    mutex.withLock {
                        running.remove(key)
                        val succeeded = result
                        val current = scheduled[key]
                        if (succeeded == null || current == null || current.first() !== work) {
                            return@withLock
                        }
                        finishWork(key = key, queue = current, succeeded = succeeded)
                        if (work.requiresInternet !in activeBatches) {
                            reschedule(work.requiresInternet)
                        }
                    }
                }
            }
        }
    }

    private fun finishWork(key: String, queue: List<ScheduledWork>, succeeded: Boolean) {
        val work = queue.first()
        val repeatMillis = work.repeatMillis
        val remaining = when {
            repeatMillis != null && registry.find(work.workerId) != null -> {
                listOf(work.copy(dueAt = nowMillis() + repeatMillis))
            }

            succeeded -> queue.drop(1)
            else -> emptyList()
        }
        val next = remaining.firstOrNull()
        if (next == null) {
            scheduled.remove(key)
        } else {
            scheduled[key] = remaining
        }
        saveWork()
        if (next != null && next.requiresInternet != work.requiresInternet) {
            reschedule(next.requiresInternet)
            foregroundWakeups.trySend(Unit)
        }
    }

    private fun reschedule(requiresInternet: Boolean) {
        try {
            submit(requiresInternet)
        } catch (error: IllegalStateException) {
            Log.e(error, "Could not reschedule iOS background work")
        }
    }

    private fun submit(requiresInternet: Boolean) {
        val identifier = requestIdentifier(requiresInternet)
        val dueAt = scheduled.values.asSequence().map { it.first() }
            .filter { it.requiresInternet == requiresInternet }
            .minOfOrNull { it.dueAt }
        if (dueAt == null) {
            scheduler.cancelTaskRequestWithIdentifier(identifier)
            return
        }
        /** Keep a pending system request while a batch runs in case the process is terminated. */
        val earliest = if (requiresInternet in activeBatches) {
            maxOf(dueAt, nowMillis() + 60_000L)
        } else {
            dueAt
        }
        val request = BGProcessingTaskRequest(identifier).apply {
            requiresNetworkConnectivity = requiresInternet
            earliestBeginDate = NSDate.dateWithTimeIntervalSince1970(earliest / 1_000.0)
        }
        memScoped {
            val error = alloc<ObjCObjectVar<NSError?>>()
            error.value = null
            check(scheduler.submitTaskRequest(request, error.ptr)) {
                "Could not submit $identifier: ${error.value?.localizedDescription}"
            }
        }
    }

    private fun loadWork(): MutableMap<String, List<ScheduledWork>> {
        val json = preferences.stringForKey(WORK_QUEUE_KEY) ?: return mutableMapOf()
        return try {
            queueJson.parseToJsonElement(json).jsonObject.mapValues { (key, entry) ->
                if (entry is JsonArray) {
                    queueJson.decodeFromJsonElement<List<ScheduledWork>>(entry)
                } else {
                    /** Older queues used the worker ID as the key and stored one job per key. */
                    val legacyWork = JsonObject(
                        entry.jsonObject + ("workerId" to JsonPrimitive(key)),
                    )
                    listOf(queueJson.decodeFromJsonElement<ScheduledWork>(legacyWork))
                }
            }.filterValues { it.isNotEmpty() }.toMutableMap()
        } catch (error: IllegalArgumentException) {
            Log.e(error, "Could not read the iOS background work queue")
            mutableMapOf()
        }
    }

    private fun saveWork() {
        preferences.setObject(queueJson.encodeToString(scheduled), forKey = WORK_QUEUE_KEY)
    }

    private class RunningWork(val work: ScheduledWork, val job: Job)
}

@Serializable
private data class ScheduledWork(
    val workerId: String,
    val repeatMillis: Long?,
    val requiresInternet: Boolean,
    val dueAt: Long,
    val input: Map<String, String>?,
)

private fun requestIdentifier(requiresInternet: Boolean): String {
    return if (requiresInternet) "$REQUEST_PREFIX.online" else "$REQUEST_PREFIX.local"
}

private fun nowMillis(): Long = (NSDate().timeIntervalSince1970 * 1_000).toLong()

private const val WORK_QUEUE_KEY = "securitychat.background-work.queue"
private const val REQUEST_PREFIX = "com.security.chat.background-work"
