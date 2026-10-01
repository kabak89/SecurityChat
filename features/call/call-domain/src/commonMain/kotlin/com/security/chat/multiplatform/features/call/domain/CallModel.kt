package com.security.chat.multiplatform.features.call.domain

import com.security.chat.multiplatform.common.core.domain.BaseModel
import com.security.chat.multiplatform.common.core.domain.ScopedModel
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.features.call.domain.repo.CallRepo

public interface CallModel : ScopedModel

internal class CallModelImpl(
    private val callRepo: CallRepo,
    dispatcherProvider: DispatcherProviderInterface,
) : CallModel,
    BaseModel(
        dispatcher = dispatcherProvider.Default,
    )
