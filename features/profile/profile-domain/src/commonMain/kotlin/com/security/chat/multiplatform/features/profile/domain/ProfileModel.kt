package com.security.chat.multiplatform.features.profile.domain

import com.security.chat.multiplatform.common.core.domain.BaseModel
import com.security.chat.multiplatform.common.core.domain.ScopedModel
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface

public interface ProfileModel : ScopedModel

internal class ProfileModelImpl(
    dispatcherProvider: DispatcherProviderInterface,
) : ProfileModel,
    BaseModel(
        dispatcher = dispatcherProvider.Default,
    )
