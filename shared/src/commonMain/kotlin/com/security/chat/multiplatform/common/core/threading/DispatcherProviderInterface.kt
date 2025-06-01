package com.security.chat.multiplatform.common.core.threading

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.MainCoroutineDispatcher

interface DispatcherProviderInterface {

    val Default: CoroutineDispatcher
    val IO: CoroutineDispatcher
    val Unconfined: CoroutineDispatcher
    val Main: MainCoroutineDispatcher

}