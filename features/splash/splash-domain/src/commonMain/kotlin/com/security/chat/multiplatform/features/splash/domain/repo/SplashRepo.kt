package com.security.chat.multiplatform.features.splash.domain.repo

public interface SplashRepo {

    public suspend fun isUserAuthorized(): Boolean

}