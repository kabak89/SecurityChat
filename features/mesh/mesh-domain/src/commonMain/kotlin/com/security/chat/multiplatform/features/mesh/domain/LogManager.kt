package com.security.chat.multiplatform.features.mesh.domain

import com.security.chat.multiplatform.features.mesh.domain.entity.MeshLog
import kotlinx.coroutines.flow.Flow

public interface LogManager {

    public fun d(message: () -> String)

    public fun e(message: String, error: Throwable? = null)

    public fun getLogsFlow(): Flow<List<MeshLog>>
}
