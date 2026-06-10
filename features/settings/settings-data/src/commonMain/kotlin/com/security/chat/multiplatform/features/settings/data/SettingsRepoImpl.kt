package com.security.chat.multiplatform.features.settings.data

import com.security.chat.multiplatform.features.settings.data.common.SettingsDataHelper
import com.security.chat.multiplatform.features.settings.data.mapper.toDomain
import com.security.chat.multiplatform.features.settings.data.mapper.toSm
import com.security.chat.multiplatform.features.settings.data.storage.SettingsStorage
import com.security.chat.multiplatform.features.settings.domain.entity.Theme
import com.security.chat.multiplatform.features.settings.domain.repo.SettingsRepo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class SettingsRepoImpl(
    private val settingsDataHelper: SettingsDataHelper,
    private val settingsStorage: SettingsStorage,
) : SettingsRepo {

    override suspend fun logout() {
        settingsDataHelper.clearLocalStorages()
    }

    override suspend fun setupTheme(theme: Theme) {
        settingsStorage.saveTheme(theme.toSm())
    }

    override fun getTheme(): Flow<Theme> {
        return settingsStorage.getCurrentThemeFlow()
            .map { it.toDomain() }
            .distinctUntilChanged()
    }

}