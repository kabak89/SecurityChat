package com.security.chat.multiplatform.common.background.work

import com.security.chat.multiplatform.common.log.Log
import kotlinx.coroutines.CancellationException
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import org.koin.dsl.module
import kotlin.reflect.KClass

public val backgroundWorkModule: Module = module {
    single { BackgroundWorkerRegistry(getAll()) }
    single<BackgroundWorkManager> { createBackgroundWorkManager(get()) }
}

/** Registers a fresh-worker factory in an application-wide Koin module, under a stable ID. */
public fun <T : BackgroundWorker> Module.backgroundWorker(
    id: String,
    worker: KClass<T>,
    create: Scope.() -> T,
) {
    require(id.isNotBlank()) { "A background worker must have a stable ID" }
    val className = requireNotNull(worker.qualifiedName) { "A background worker must be named" }
    single(named(className)) {
        val scope = this
        RegisteredBackgroundWorker(id = id, type = worker, create = { scope.create() })
    }
}

internal expect fun Scope.createBackgroundWorkManager(
    registry: BackgroundWorkerRegistry,
): BackgroundWorkManager

internal class BackgroundWorkerRegistry(workers: List<RegisteredBackgroundWorker>) {

    private val byId = workers.associateBy { it.id }
    private val byType = workers.associateBy { it.type }

    init {
        require(byId.size == workers.size) { "Background worker IDs must be unique" }
    }

    fun get(type: KClass<out BackgroundWorker>): RegisteredBackgroundWorker {
        return requireNotNull(byType[type]) { "Unregistered background worker: $type" }
    }

    fun find(id: String): RegisteredBackgroundWorker? = byId[id]
}

internal class RegisteredBackgroundWorker(
    val id: String,
    val type: KClass<out BackgroundWorker>,
    private val create: () -> BackgroundWorker,
) {

    suspend fun run(input: Map<String, String>): Boolean {
        return try {
            create().run(input)
            true
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.e(error, "Background worker $id failed")
            false
        }
    }
}
