package com.security.chat.multiplatform.features.settings.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.security.chat.multiplatform.common.core.ui.Screen
import com.security.chat.multiplatform.common.core.ui.SingleEventEffect
import com.security.chat.multiplatform.common.core.ui.entity.resolve
import com.security.chat.multiplatform.common.icons.kit.DrawableRes
import com.security.chat.multiplatform.common.ui.kit.components.CenterContent
import com.security.chat.multiplatform.common.ui.kit.components.SideContent
import com.security.chat.multiplatform.common.ui.kit.components.ToolbarComponent
import com.security.chat.multiplatform.common.ui.kit.components.alertdialog.AlertDialogComponent
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme
import com.security.chat.multiplatform.features.settings.component.api.SettingsMainComponent
import com.security.chat.multiplatform.features.settings.ui.screens.main.entity.DialogData
import com.security.chat.multiplatform.features.settings.ui.screens.main.entity.SettingItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import securitychat.common.icons_kit.generated.resources.ic_back

@Composable
internal fun SettingsMainScreen(
    component: SettingsMainComponent,
) {
    Screen(component) { state: SettingsMainState, vm: SettingsMainViewModel ->
        SettingsMainScreenContent(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            state = state,
            events = vm.viewEvent,
            onBackClicked = component::onExitClicked,
            onItemClicked = vm::onItemClicked,
            onDismissDialog = vm::onDismissDialogClicked,
            onDialogActionClicked = vm::onDialogActionClicked,
            onUserLogOuted = component::onUserLogOuted,
            onOnGoToThemeClicked = component::onGoToThemeClicked,
            onOnGoToProfileClicked = component::onGoToProfileClicked,
        )
    }
}

@Composable
private fun SettingsMainScreenContent(
    modifier: Modifier,
    state: SettingsMainState,
    events: Flow<SettingsMainEvent>,
    onBackClicked: () -> Unit,
    onItemClicked: (item: SettingItem) -> Unit,
    onDismissDialog: () -> Unit,
    onDialogActionClicked: (action: DialogData.ButtonAction) -> Unit,
    onUserLogOuted: () -> Unit,
    onOnGoToThemeClicked: () -> Unit,
    onOnGoToProfileClicked: () -> Unit,
) {
    SingleEventEffect(
        sideEffectFlow = events,
        collector = { event ->
            when (event) {
                SettingsMainEvent.UserLogOuted -> onUserLogOuted()
                SettingsMainEvent.GoToTheme -> onOnGoToThemeClicked()
                SettingsMainEvent.GoToProfile -> onOnGoToProfileClicked()
            }
        },
    )

    Box(
        modifier = modifier
            .background(AppTheme.colors.backgroundPrimary)
            .fillMaxSize()
            .systemBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            ToolbarComponent(
                modifier = Modifier
                    .fillMaxWidth(),
                startContent = SideContent.Button(
                    icon = DrawableRes.ic_back,
                    onClicked = onBackClicked,
                ),
                centerContent = CenterContent.Title(
                    text = "Settings",
                ),
                endContent = null,
            )
            LazyColumn(
                modifier = Modifier
                    .weight(1f),
                content = {
                    state.items.forEach { item ->
                        item(key = item.hashCode()) {
                            ItemComponent(
                                modifier = Modifier.fillMaxWidth(),
                                item = item,
                                onItemClicked = { onItemClicked(item) },
                            )
                        }
                    }
                },
            )
        }
        if (state.dialogData != null) {
            AlertDialogComponent(
                title = state.dialogData.title,
                message = state.dialogData.message,
                positiveButtonText = state.dialogData.positiveButtonTitle,
                negativeButtonText = state.dialogData.negativeButtonTitle,
                onDismissRequest = onDismissDialog,
                onPositiveButtonClicked = {
                    onDialogActionClicked(state.dialogData.positiveButtonAction)
                },
                onNegativeButtonClicked = {
                    onDialogActionClicked(state.dialogData.negativeButtonAction)
                },
            )
        }
        if (state.requestInProgress) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center),
                color = AppTheme.colors.element,
            )
        }
    }
}

@Composable
private fun ItemComponent(
    modifier: Modifier = Modifier,
    item: SettingItem,
    onItemClicked: () -> Unit,
) {
    Text(
        modifier = modifier
            .clickable(onClick = onItemClicked)
            .padding(all = 16.dp),
        text = item.title.resolve(),
    )
}

@Preview
@Composable
internal fun SettingsMainScreenPreview() {
    AppTheme {
        SettingsMainScreenContent(
            modifier = Modifier.fillMaxSize(),
            state = SettingsMainState(
                items = listOf(
                    SettingItem.Logout,
                ),
                dialogData = null,
                requestInProgress = false,
            ),
            events = emptyFlow(),
            onBackClicked = {},
            onItemClicked = {},
            onDismissDialog = {},
            onDialogActionClicked = {},
            onUserLogOuted = {},
            onOnGoToThemeClicked = {},
            onOnGoToProfileClicked = {},
        )
    }
}

@Preview
@Composable
internal fun SettingsMainScreenPreviewWithDialog() {
    AppTheme {
        SettingsMainScreenContent(
            modifier = Modifier.fillMaxSize(),
            state = SettingsMainState(
                items = listOf(
                    SettingItem.Logout,
                ),
                dialogData = DialogData(
                    title = "Title",
                    message = "Message",
                    positiveButtonTitle = "Ok",
                    negativeButtonTitle = "Cancel",
                    positiveButtonAction = DialogData.ButtonAction.Ok,
                    negativeButtonAction = DialogData.ButtonAction.Cancel,
                ),
                requestInProgress = false,
            ),
            events = emptyFlow(),
            onBackClicked = {},
            onItemClicked = {},
            onDismissDialog = {},
            onDialogActionClicked = {},
            onUserLogOuted = {},
            onOnGoToThemeClicked = {},
            onOnGoToProfileClicked = {},
        )
    }
}