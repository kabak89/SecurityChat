package com.security.chat.multiplatform.features.mesh.data

import com.security.chat.multiplatform.common.core.network.NetworkManagerFactory
import com.security.chat.multiplatform.common.core.network.entity.NetworkConfig

internal class MeshRelayApi(
    networkManagerFactory: NetworkManagerFactory,
    networkConfig: NetworkConfig,
) {

    private val networkManager by lazy {
        networkManagerFactory.build(
            baseUrl = "${networkConfig.host}:${networkConfig.port}",
            needAuthorization = true,
        )
    }

    suspend fun upload(packet: SignedMeshData): SignedMeshData {
        return networkManager.runPost(relativePath = "/messages/relay", request = packet)
    }

    suspend fun getReceiptKey(): String {
        return networkManager.runGet<MeshReceiptKey>(relativePath = "/messages/relay/key").publicKey
    }
}
