package com.security.chat.multiplatform.features.call.data

import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.features.call.domain.CallManager

internal expect class CallManagerImpl(
    dispatcherProvider: DispatcherProviderInterface,
) : CallManager
