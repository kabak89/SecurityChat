package com.security.chat.multiplatform.features.settings.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.security.chat.multiplatform.common.core.ui.SingleEventEffect
import com.security.chat.multiplatform.common.ui.kit.AlertDialogComponent
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme
import com.security.chat.multiplatform.features.settings.ui.component.SettingsMainComponent
import com.security.chat.multiplatform.features.settings.ui.screens.main.entity.DialogData
import com.security.chat.multiplatform.features.settings.ui.screens.main.entity.SettingItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import securitychat.common.icons_kit.generated.resources.Res
import securitychat.common.icons_kit.generated.resources.ic_back

@Composable
internal fun SettingsMainScreen(
    component: SettingsMainComponent,
) {
    try {
        if (component.getDiScope().closed) return
    } catch (e: Exception) {
        println(e)
        return
    }

    val vm: SettingsMainViewModel = koinViewModel(
        viewModelStoreOwner = component,
        scope = component.getDiScope(),
    )

    val state = vm.viewState.collectAsStateWithLifecycle().value

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
    )
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
) {
    SingleEventEffect(
        sideEffectFlow = events,
        collector = { event ->
            when (event) {
                SettingsMainEvent.UserLogOuted -> onUserLogOuted()
                SettingsMainEvent.GoToTheme -> onOnGoToThemeClicked()
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
                onBackClicked = onBackClicked,
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
        text = item.title,
    )
}

@Composable
private fun ToolbarComponent(
    modifier: Modifier = Modifier,
    onBackClicked: () -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(0.2f),
            horizontalArrangement = Arrangement.Start,
        ) {
            IconButton(
                modifier = Modifier
                    .size(48.dp),
                onClick = onBackClicked,
                content = {
                    Icon(
                        imageVector = vectorResource(Res.drawable.ic_back),
                        tint = AppTheme.colors.element,
                        contentDescription = null,
                    )
                },
            )
        }
        Text(
            modifier = Modifier
                .weight(0.6f),
            text = "Settings",
            textAlign = TextAlign.Center,
        )
        Spacer(
            modifier = Modifier
                .weight(0.2f),
        )
    }
}

@Preview
@Composable
internal fun SettingsMainScreenPreview() {
    AppTheme {
        SettingsMainScreenContent(
            modifier = Modifier.fillMaxSize(),
            state = SettingsMainState(
                items = listOf(
                    SettingItem.Logout(
                        title = "logout",
                    ),
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
                    SettingItem.Logout(
                        title = "logout",
                    ),
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
        )
    }
}