package com.security.chat.multiplatform.features.settings.ui.screens.theme

import androidx.lifecycle.viewModelScope
import com.security.chat.multiplatform.common.core.ui.BaseViewModel
import com.security.chat.multiplatform.features.settings.domain.SettingsModel
import com.security.chat.multiplatform.features.settings.ui.screens.theme.entity.ThemeItem
import com.security.chat.multiplatform.features.settings.ui.screens.theme.mapper.toDomain
import com.security.chat.multiplatform.features.settings.ui.screens.theme.mapper.toUi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

internal class ThemeViewModel(
    private val settingsModel: SettingsModel,
) : BaseViewModel<ThemeState, ThemeEvent>() {

    override fun onPostStart() {
        super.onPostStart()

        settingsModel.getTheme()
            .onEach { selectedTheme ->
                val selectedThemeType = selectedTheme.toUi()

                val newItems = currentViewState.items
                    .map { theme ->
                        if (theme.type == selectedThemeType) {
                            theme.copy(enabled = true)
                        } else {
                            theme.copy(enabled = false)
                        }
                    }

                updateState { it.copy(items = newItems) }
            }
            .launchIn(viewModelScope)
    }

    override fun createInitialState(): ThemeState {
        return ThemeState(
            items = listOf(
                ThemeItem(
                    type = ThemeItem.Type.Auto,
                    enabled = false,
                    title = "Auto",
                ),
                ThemeItem(
                    type = ThemeItem.Type.Dark,
                    enabled = false,
                    title = "Dark",
                ),
                ThemeItem(
                    type = ThemeItem.Type.Light,
                    enabled = false,
                    title = "Light",
                ),
            ),
        )
    }

    fun onItemClicked(item: ThemeItem) {
        settingsModel.enableTheme.start(item.type.toDomain())
    }

}