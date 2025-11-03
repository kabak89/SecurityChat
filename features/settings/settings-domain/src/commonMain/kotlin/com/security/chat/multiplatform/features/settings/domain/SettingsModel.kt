package com.security.chat.multiplatform.features.settings.domain

import com.security.chat.multiplatform.common.core.domain.BaseModel
import com.security.chat.multiplatform.common.core.domain.ScopedModel
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.features.settings.domain.entity.Theme
import com.security.chat.multiplatform.features.settings.domain.repo.SettingsRepo
import kotlinx.coroutines.flow.Flow
import ru.kode.remo.Task0
import ru.kode.remo.Task1

public interface SettingsModel : ScopedModel {

    public val logout: Task0<Unit>
    public val enableTheme: Task1<Theme, Unit>

    public fun getTheme(): Flow<Theme>

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

    override val enableTheme: Task1<Theme, Unit> =
        task { theme ->
            settingsRepo.setupTheme(theme = theme)
        }

    override fun getTheme(): Flow<Theme> {
        return settingsRepo.getTheme()
    }

}