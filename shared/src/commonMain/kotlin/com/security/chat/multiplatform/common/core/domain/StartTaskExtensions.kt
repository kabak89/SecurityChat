package com.security.chat.multiplatform.common.core.domain

import kotlinx.coroutines.Job
import ru.kode.remo.StartScheduled
import ru.kode.remo.Task0
import ru.kode.remo.Task1
import ru.kode.remo.Task2
import ru.kode.remo.Task3

/**
 * Lazy start task when subscribers count will be > 0. Fast task sometimes emit result faster then
 * someone subscribes to it
 */
fun Task0<*>.startOnSubscribe(): Job {
    return start(
        scheduled = StartScheduled.Lazily(
            minResultsSubscribers = 1,
            minStateSubscribers = 1,
        ),
    )
}

/**
 * Lazy start task when subscribers count will be > 0. Fast task sometimes emit result faster then
 * someone subscribes to it
 */
fun <P1, R> Task1<P1, R>.startOnSubscribe(
    argument: P1,
): Job {
    return start(
        argument = argument,
        scheduled = StartScheduled.Lazily(
            minResultsSubscribers = 1,
            minStateSubscribers = 1,
        ),
    )
}

/**
 * Lazy start task when subscribers count will be > 0. Fast task sometimes emit result faster then
 * someone subscribes to it
 */
fun <P1, P2, R> Task2<P1, P2, R>.startOnSubscribe(
    argument1: P1,
    argument2: P2,
): Job {
    return start(
        argument1 = argument1,
        argument2 = argument2,
        scheduled = StartScheduled.Lazily(
            minResultsSubscribers = 1,
            minStateSubscribers = 1,
        ),
    )
}

/**
 * Lazy start task when subscribers count will be > 0. Fast task sometimes emit result faster then
 * someone subscribes to it
 */
fun <P1, P2, P3, R> Task3<P1, P2, P3, R>.startOnSubscribe(
    argument1: P1,
    argument2: P2,
    argument3: P3,
): Job {
    return start(
        argument1 = argument1,
        argument2 = argument2,
        argument3 = argument3,
        scheduled = StartScheduled.Lazily(
            minResultsSubscribers = 1,
            minStateSubscribers = 1,
        ),
    )
}

/**
 * Extension to start task with default params
 */
fun Task0<*>.start(): Job {
    return start()
}

/**
 * Extension to start task with default params
 */
fun <P1, R> Task1<P1, R>.start(
    argument: P1,
): Job {
    return start(argument = argument)
}

/**
 * Extension to start task with default params
 */
fun <P1, P2, R> Task2<P1, P2, R>.start(
    argument1: P1,
    argument2: P2,
): Job {
    return start(
        argument1 = argument1,
        argument2 = argument2,
    )
}

/**
 * Extension to start task with default params
 */
fun <P1, P2, P3, R> Task3<P1, P2, P3, R>.start(
    argument1: P1,
    argument2: P2,
    argument3: P3,
): Job {
    return start(
        argument1 = argument1,
        argument2 = argument2,
        argument3 = argument3,
    )
}