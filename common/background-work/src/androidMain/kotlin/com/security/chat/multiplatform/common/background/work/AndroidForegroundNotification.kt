package com.security.chat.multiplatform.common.background.work

import android.app.Notification
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import kotlin.reflect.KClass

public data class AndroidForegroundNotification(
    val id: Int,
    val notification: Notification,
    val serviceType: Int,
)

/** Registers an Android worker with its own foreground notification and service type. */
public fun <T : BackgroundWorker> Module.backgroundWorker(
    id: String,
    worker: KClass<T>,
    create: Scope.() -> T,
    foregroundNotification: suspend Scope.(String) -> AndroidForegroundNotification,
) {
    backgroundWorker(id = id, worker = worker, create = create)
    single(named(id)) {
        val scope = this
        ForegroundNotificationFactory { text -> scope.foregroundNotification(text) }
    }
}

internal fun interface ForegroundNotificationFactory {
    suspend fun create(text: String): AndroidForegroundNotification
}
