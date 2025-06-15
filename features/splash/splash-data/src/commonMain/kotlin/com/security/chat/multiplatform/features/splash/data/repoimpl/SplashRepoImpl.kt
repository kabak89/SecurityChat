package com.security.chat.multiplatform.features.splash.data.repoimpl

import com.security.chat.multiplatform.features.splash.domain.repo.SplashRepo
import com.security.chat.multiplatform.features.user.data.storage.UserStorage

public class SplashRepoImpl(
    private val userStorage: UserStorage,
) : SplashRepo {

    override suspend fun isUserAuthorized(): Boolean {
        return userStorage.isUserAuthorized()
    }

}