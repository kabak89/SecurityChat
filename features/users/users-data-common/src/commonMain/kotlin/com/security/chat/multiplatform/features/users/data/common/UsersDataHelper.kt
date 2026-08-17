package com.security.chat.multiplatform.features.users.data.common

import com.security.chat.multiplatform.features.users.data.common.entity.UserInfo
import com.security.chat.multiplatform.features.users.data.network.UsersNetworkManager
import com.security.chat.multiplatform.features.users.data.storage.UsersStorage

public interface UsersDataHelper {
    /**
     * method returns cached user info or download and cache it if info is absent
     */
    public suspend fun getOrFetchUser(userId: String): UserInfo
}

internal class UsersDataHelperImpl(
    private val usersStorage: UsersStorage,
    private val usersNetworkManager: UsersNetworkManager,
) : UsersDataHelper {
    override suspend fun getOrFetchUser(userId: String): UserInfo {
        val cachedUser = usersStorage.getUser(userId)
        if (cachedUser != null) {
            return UserInfo(
                id = cachedUser.id,
                username = cachedUser.name,
            )
        }

        val networkUser = usersNetworkManager.getUser(userId)
        usersStorage.saveUser(
            com.security.chat.multiplatform.features.users.data.storage.entity.UserSM(
                id = networkUser.userId,
                publicKey = networkUser.publicKey,
                name = networkUser.name,
            ),
        )

        return UserInfo(
            id = networkUser.userId,
            username = networkUser.name,
        )
    }
}
