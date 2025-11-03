package com.security.chat.multiplatform.features.settings.data.storage

import com.security.chat.multiplatform.common.app.lifecycle.AppLifecycle
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.common.settings.PublicSettings
import com.security.chat.multiplatform.features.settings.data.storage.entity.ThemeSM
import com.security.chat.multiplatform.features.settings.data.storage.mapper.mapThemeToSM
import com.security.chat.multiplatform.features.settings.data.storage.mapper.mapThemeToString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

public interface SettingsStorage {

    public fun getCurrentThemeFlow(): Flow<ThemeSM>
    public suspend fun saveTheme(theme: ThemeSM)

}

internal class SettingsStorageImpl(
    private val publicSettings: PublicSettings,
    private val dispatcherProviderInterface: DispatcherProviderInterface,
    lifecycle: AppLifecycle,
    coroutineScope: CoroutineScope,
) : SettingsStorage {

    private val stateFlow: MutableStateFlow<ThemeSM?> = MutableStateFlow(null)

    init {
        lifecycle.getIsAppStartedFlow()
            .filter { it }
            .take(1)
            .onEach {
                val string = publicSettings.getString(KEY_THEME)
                val theme = mapThemeToSM(string)
                stateFlow.update { theme }
            }
            .launchIn(coroutineScope)
    }

    override fun getCurrentThemeFlow(): Flow<ThemeSM> {
        return stateFlow
            .filterNotNull()
            .distinctUntilChanged()
    }

    override suspend fun saveTheme(theme: ThemeSM) {
        withContext(dispatcherProviderInterface.IO) {
            publicSettings.putString(
                key = KEY_THEME,
                value = mapThemeToString(theme),
            )

            stateFlow.update { theme }
        }
    }

}

private const val KEY_THEME = "KEY_THEME"