package com.security.chat.multiplatform.common.core.threading

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.MainCoroutineDispatcher

internal class DispatcherProviderInterfaceImpl : DispatcherProviderInterface {

    override val Default: CoroutineDispatcher = Dispatchers.Default
    override val IO: CoroutineDispatcher = Dispatchers.IO
    override val Unconfined: CoroutineDispatcher = Dispatchers.Unconfined
    override val Main: MainCoroutineDispatcher = Dispatchers.Main

}