package com.security.chat.multiplatform.common.app.lifecycle

import kotlinx.coroutines.flow.Flow

public interface AppLifecycle {

    public fun getIsAppStartedFlow(): Flow<Boolean>

}