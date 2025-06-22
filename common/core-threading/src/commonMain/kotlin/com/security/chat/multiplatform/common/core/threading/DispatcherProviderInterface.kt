package com.security.chat.multiplatform.common.core.threading

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.MainCoroutineDispatcher

public interface DispatcherProviderInterface {

    public val Default: CoroutineDispatcher
    public val IO: CoroutineDispatcher
    public val Unconfined: CoroutineDispatcher
    public val Main: MainCoroutineDispatcher

}