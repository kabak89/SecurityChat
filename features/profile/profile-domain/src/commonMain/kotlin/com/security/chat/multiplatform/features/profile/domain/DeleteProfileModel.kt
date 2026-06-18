package com.security.chat.multiplatform.features.profile.domain

import com.security.chat.multiplatform.common.core.domain.BaseModel
import com.security.chat.multiplatform.common.core.domain.ScopedModel
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.features.profile.domain.repo.ProfileRepo
import ru.kode.remo.Task0

public interface DeleteProfileModel : ScopedModel {
    public val deleteProfile: Task0<Unit>
}

internal class DeleteProfileModelImpl(
    private val profileRepo: ProfileRepo,
    dispatcherProvider: DispatcherProviderInterface,
) : DeleteProfileModel,
    BaseModel(
        dispatcher = dispatcherProvider.Default,
    ) {

    override val deleteProfile: Task0<Unit> =
        task { ->
            profileRepo.deleteProfile()
        }
}
