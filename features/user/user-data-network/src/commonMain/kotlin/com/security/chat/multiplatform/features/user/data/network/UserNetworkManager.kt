package com.security.chat.multiplatform.features.user.data.network

import com.security.chat.multiplatform.common.core.network.NetworkManager
import com.security.chat.multiplatform.common.core.network.NetworkManagerFactory
import com.security.chat.multiplatform.common.core.network.entity.NetworkConfig
import com.security.chat.multiplatform.features.user.data.network.entity.ProfileNM
import com.security.chat.multiplatform.features.user.data.network.entity.ProfileResponse
import com.security.chat.multiplatform.features.user.data.network.entity.UpdateProfileNM
import com.security.chat.multiplatform.features.user.data.network.entity.UpdateProfileRequest
import com.security.chat.multiplatform.features.user.data.network.mapper.toNM

public interface UserNetworkManager {
    public suspend fun getProfile(id: String): ProfileNM
    public suspend fun updateProfile(params: UpdateProfileNM): ProfileNM
}

internal class UserNetworkManagerImpl(
    private val networkConfig: NetworkConfig,
    private val networkManagerFactory: NetworkManagerFactory,
) : UserNetworkManager {

    private val networkManager: NetworkManager by lazy {
        networkManagerFactory.build(baseUrl = "${networkConfig.host}:${networkConfig.port}")
    }

    override suspend fun getProfile(id: String): ProfileNM {
        return networkManager.runGet<ProfileResponse>(
            relativePath = "/profile",
            request = mapOf("id" to id),
        )
            .toNM()
    }

    override suspend fun updateProfile(params: UpdateProfileNM): ProfileNM {
        return networkManager.runPost<UpdateProfileRequest, ProfileResponse>(
            relativePath = "/profile",
            request = UpdateProfileRequest(
                id = params.id,
                login = params.name,
            ),
        )
            .toNM()
    }
}