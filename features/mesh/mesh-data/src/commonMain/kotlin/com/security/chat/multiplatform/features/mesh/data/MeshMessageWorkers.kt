package com.security.chat.multiplatform.features.mesh.data

import com.security.chat.multiplatform.common.background.work.BackgroundWorker

internal class ExchangeMeshMessagesWorker(private val service: MeshMessageService) :
    BackgroundWorker {

    override suspend fun run(input: Map<String, String>) {
        val retry = input[MESH_RETRY_EXCHANGE_INPUT] == "true"
        val key = if (retry) MESH_EXCHANGE_RETRY else MESH_EXCHANGE_WORK
        service.runWork(key = key, input = input) { ownerId, originDeviceId ->
            service.exchange(ownerId = ownerId, originDeviceId = originDeviceId, retry = retry)
        }
    }
}

internal class UploadMeshMessagesWorker(private val service: MeshMessageService) :
    BackgroundWorker {

    override suspend fun run(input: Map<String, String>) {
        service.runWork(key = MESH_UPLOAD_WORK, input = input, work = service::upload)
    }
}
