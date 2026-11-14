package com.security.chat.multiplatform.common.background.work

import com.security.chat.multiplatform.common.background.work.BackgroundWorkManager.Type
import com.security.chat.multiplatform.common.core.component.SCOPE_ID_APP
import com.security.chat.multiplatform.common.core.network.ConnectivityObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import kotlin.reflect.KClass

internal actual fun Scope.createBackgroundWorkManager(
    registry: BackgroundWorkerRegistry,
): BackgroundWorkManager {
    val connectivityObserver: ConnectivityObserver = get()
    return JvmBackgroundWorkManager(
        scope = get(named(SCOPE_ID_APP)),
        registry = registry,
        isOnline = connectivityObserver.isOnline,
    )
}

internal class JvmBackgroundWorkManager(
    private val scope: CoroutineScope,
    private val registry: BackgroundWorkerRegistry,
    private val isOnline: StateFlow<Boolean>,
) : BackgroundWorkManager {

    private val mutex = Mutex()
    private val queues = mutableMapOf<String, WorkQueue>()

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
        val work = ScheduledWork(
            worker = registry.get(worker),
            conditions = conditions,
            input = input.toMap(),
        )
        while (true) {
            val cancelling = mutex.withLock {
                val queue = queues[key]
                if (queue == null) {
                    val pending = ArrayDeque(listOf(work))
                    queues[key] = WorkQueue(pending, startQueue(key, pending))
                    return
                }
                if (!queue.job.isCancelled) {
                    if (policy != BackgroundWorkPolicy.KEEP) {
                        val head = queue.pending.first()
                        require(head.conditions.schedule == BackgroundWorkSchedule.OneShot) {
                            "Cannot append to repeating background work: $key"
                        }
                        val runningCount = if (queue.running) 1 else 0
                        if (policy == BackgroundWorkPolicy.APPEND_OR_REPLACE ||
                            queue.pending.size == runningCount
                        ) {
                            queue.pending.addLast(work)
                        }
                    }
                    return
                }
                queue.job
            }
            cancelling.join()
        }
    }

    override suspend fun cancel(key: String) {
        val job = mutex.withLock { queues[key]?.job?.also { it.cancel() } }
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

    @OptIn(DelicateCoroutinesApi::class)
    private fun startQueue(key: String, pending: ArrayDeque<ScheduledWork>): Job {
        /** Enter the cleanup block even when cancellation precedes the first dispatch. */
        return scope.launch(start = CoroutineStart.ATOMIC) {
            try {
                runQueue(key, pending)
            } finally {
                withContext(NonCancellable) {
                    mutex.withLock {
                        if (queues[key]?.pending === pending) queues.remove(key)
                    }
                }
            }
        }
    }

    private suspend fun runQueue(key: String, pending: ArrayDeque<ScheduledWork>) {
        while (true) {
            val work = mutex.withLock { pending.first() }
            val succeeded = runSchedule(key, work)
            mutex.withLock {
                queues.getValue(key).running = false
                pending.removeFirst()
                if (!succeeded || pending.isEmpty()) {
                    queues.remove(key)
                    return
                }
            }
        }
    }

    private suspend fun runSchedule(key: String, work: ScheduledWork): Boolean {
        do {
            currentCoroutineContext().ensureActive()
            if (work.conditions.requiresInternet) {
                isOnline.first { it }
            }
            currentCoroutineContext().ensureActive()
            mutex.withLock { queues.getValue(key).running = true }
            val succeeded = work.worker.run(work.input)
            when (val schedule = work.conditions.schedule) {
                BackgroundWorkSchedule.OneShot -> return succeeded
                is BackgroundWorkSchedule.Repeatable -> delay(schedule.interval)
            }
        } while (true)
    }

    private class ScheduledWork(
        val worker: RegisteredBackgroundWorker,
        val conditions: BackgroundWorkConditions,
        val input: Map<String, String>,
    )

    private class WorkQueue(val pending: ArrayDeque<ScheduledWork>, val job: Job) {
        var running = false
    }
}
