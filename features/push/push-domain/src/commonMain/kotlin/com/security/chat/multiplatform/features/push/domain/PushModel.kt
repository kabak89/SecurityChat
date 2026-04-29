package com.security.chat.multiplatform.features.push.domain

public interface PushModel {
    public suspend fun registerCurrentToken()
}

internal class PushModelImpl(
    private val pushRepository: PushRepository,
) : PushModel {

    override suspend fun registerCurrentToken() {
        pushRepository.registerCurrentToken()
    }
}