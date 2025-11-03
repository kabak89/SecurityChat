package com.security.chat.multiplatform.features.settings.domain.repo

import com.security.chat.multiplatform.features.settings.domain.entity.Theme
import kotlinx.coroutines.flow.Flow

public interface SettingsRepo {

    public suspend fun logout()
    public suspend fun setupTheme(theme: Theme)
    public fun getTheme(): Flow<Theme>

}