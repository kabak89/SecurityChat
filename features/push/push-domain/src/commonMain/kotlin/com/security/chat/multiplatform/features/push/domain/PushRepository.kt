package com.security.chat.multiplatform.features.push.domain

public interface PushRepository {
    public suspend fun registerCurrentToken()
    public suspend fun onTokenRefreshed(token: String)
}