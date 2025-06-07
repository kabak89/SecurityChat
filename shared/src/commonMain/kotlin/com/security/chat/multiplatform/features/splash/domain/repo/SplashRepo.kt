package com.security.chat.multiplatform.features.splash.domain.repo

interface SplashRepo {

    suspend fun isUserAuthorized(): Boolean

}