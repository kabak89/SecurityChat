package com.security.chat.multiplatform.features.settings.domain

import com.security.chat.multiplatform.common.core.domain.BaseModel
import com.security.chat.multiplatform.common.core.domain.ScopedModel
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.features.settings.domain.repo.SettingsRepo
import ru.kode.remo.Task0

public interface SettingsModel : ScopedModel {

    public val logout: Task0<Unit>

}

internal class SettingsModelImpl(
    private val settingsRepo: SettingsRepo,
    dispatcherProvider: DispatcherProviderInterface,
) : SettingsModel,
    BaseModel(
        dispatcher = dispatcherProvider.Default,
    ) {

    override val logout: Task0<Unit> =
        task { ->
            settingsRepo.logout()
        }

}