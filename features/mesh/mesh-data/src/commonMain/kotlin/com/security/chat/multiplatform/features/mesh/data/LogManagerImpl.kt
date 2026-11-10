package com.security.chat.multiplatform.features.mesh.data

import com.security.chat.multiplatform.common.core.time.TimeProvider
import com.security.chat.multiplatform.common.log.Log
import com.security.chat.multiplatform.features.mesh.data.storage.MeshLogStorage
import com.security.chat.multiplatform.features.mesh.domain.LogManager
import com.security.chat.multiplatform.features.mesh.domain.entity.MeshLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow

internal class LogManagerImpl(
    private val storage: MeshLogStorage,
    private val timeProvider: TimeProvider,
    appScope: CoroutineScope,
    private val isDebug: Boolean,
) : LogManager {

    private val pendingLogs = if (isDebug) Channel<PendingLog>(Channel.UNLIMITED) else null

    init {
        pendingLogs?.receiveAsFlow()
            ?.onEach { log ->
                try {
                    storage.insert(log.recordedAtMillis, log.message)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    Log.e(error, "Could not persist mesh log")
                }
            }
            ?.launchIn(appScope)
    }

    override fun d(message: () -> String) {
        if (!isDebug) {
            Log.d(message)
            return
        }
        val text = message()
        record(text)
        Log.d { text }
    }

    override fun e(message: String, error: Throwable?) {
        val detail = error?.message
        record(if (detail == null) message else "$message\n$detail")
        if (error == null) {
            Log.e(message)
        } else {
            Log.e(error, message)
        }
    }

    override fun getLogsFlow(): Flow<List<MeshLog>> {
        if (!isDebug) return flowOf(emptyList())
        return storage.getLogsFlow()
            .map { rows ->
                rows.map { row ->
                    MeshLog(
                        id = row.id,
                        recordedAtMillis = row.recordedAtMillis,
                        message = row.message,
                    )
                }
            }
            .catch { error -> Log.e(error, "Could not load mesh logs") }
    }

    private fun record(message: String) {
        pendingLogs?.trySend(PendingLog(timeProvider.now().toEpochMilliseconds(), message))
    }

    private class PendingLog(val recordedAtMillis: Long, val message: String)
}
