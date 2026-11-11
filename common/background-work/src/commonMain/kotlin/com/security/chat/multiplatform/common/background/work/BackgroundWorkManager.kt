package com.security.chat.multiplatform.common.background.work

import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

public interface BackgroundWorker {

    public suspend fun run(input: Map<String, String>)
}

public sealed interface BackgroundWorkSchedule {

    public data object OneShot : BackgroundWorkSchedule

    public data class Repeatable(val interval: Duration) : BackgroundWorkSchedule {
        init {
            require(interval.isFinite() && interval >= 1.milliseconds) {
                "The repeat interval must be finite and at least one millisecond"
            }
        }
    }
}

public data class BackgroundWorkConditions(
    val schedule: BackgroundWorkSchedule,
    val requiresInternet: Boolean,
)

public enum class BackgroundWorkPolicy {
    KEEP,
    APPEND_OR_REPLACE,
}

public sealed interface BackgroundWorkPriority {

    public data object Normal : BackgroundWorkPriority

    /**
     * Requests expedited scheduling on Android 12 and later. Before Android 12,
     * [legacyForeground] controls the compatibility path. [BackgroundWorkManager.Type.Foreground]
     * supplies the notification and [BackgroundWorkManager.Type.Silent] uses normal scheduling.
     */
    public data class Expedited(
        val legacyForeground: BackgroundWorkManager.Type,
    ) : BackgroundWorkPriority
}

public interface BackgroundWorkManager {

    public sealed interface Type {

        public data object Silent : Type

        public data class Foreground(val notificationText: String) : Type
    }

    /**
     * [key] identifies a schedule or queue. [worker] identifies the class that runs this request.
     * [BackgroundWorkPolicy.KEEP] keeps existing work for the key. For one-shot work,
     * [BackgroundWorkPolicy.APPEND_OR_REPLACE] appends to a one-shot queue or starts a new one.
     * If a run fails, its queued successors are discarded. A repeating schedule uses KEEP
     * and [BackgroundWorkPriority.Normal]. iOS and JVM ignore [priority] when scheduling.
     * The system can delay runs. Android requires an interval of at least 15 minutes for repeating
     * work. Internet is a prerequisite when requested.
     * [Type.Foreground] enables foreground work on Android. iOS and JVM ignore [type].
     * Each run receives the [input] supplied at enqueue. On Android, input and scheduling
     * metadata share WorkManager's 10 KB serialized data limit.
     */
    public suspend fun enqueue(
        key: String,
        worker: KClass<out BackgroundWorker>,
        conditions: BackgroundWorkConditions,
        type: Type,
        policy: BackgroundWorkPolicy,
        priority: BackgroundWorkPriority,
        input: Map<String, String> = emptyMap(),
    )

    /** Cancels pending work and requests cancellation of the active run for [key]. */
    public suspend fun cancel(key: String)
}

internal fun validateWorkRequest(
    key: String,
    schedule: BackgroundWorkSchedule,
    policy: BackgroundWorkPolicy,
    priority: BackgroundWorkPriority,
) {
    require(key.isNotBlank()) { "A background work key must not be blank" }
    if (schedule is BackgroundWorkSchedule.Repeatable) {
        require(policy == BackgroundWorkPolicy.KEEP) {
            "Repeating background work requires the KEEP policy"
        }
        require(priority == BackgroundWorkPriority.Normal) {
            "Repeating background work requires normal priority"
        }
    }
}
