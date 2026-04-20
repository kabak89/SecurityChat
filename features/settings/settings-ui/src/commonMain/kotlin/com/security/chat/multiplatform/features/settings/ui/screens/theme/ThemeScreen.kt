package com.security.chat.multiplatform.features.settings.ui.screens.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.security.chat.multiplatform.common.core.ui.Screen
import com.security.chat.multiplatform.common.icons.kit.DrawableRes
import com.security.chat.multiplatform.common.ui.kit.components.CenterContent
import com.security.chat.multiplatform.common.ui.kit.components.SideContent
import com.security.chat.multiplatform.common.ui.kit.components.ToolbarComponent
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme
import com.security.chat.multiplatform.features.settings.component.api.ThemeComponent
import com.security.chat.multiplatform.features.settings.ui.screens.theme.entity.ThemeItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import securitychat.common.icons_kit.generated.resources.ic_back

@Composable
internal fun ThemeScreen(
    component: ThemeComponent,
) {
    Screen(component) { state: ThemeState, vm: ThemeViewModel ->
        ThemeScreenContent(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            state = state,
            events = vm.viewEvent,
            onBackClicked = component::onBackClicked,
            onItemClicked = vm::onItemClicked,
        )
    }
}

@Composable
private fun ThemeScreenContent(
    modifier: Modifier,
    state: ThemeState,
    events: Flow<ThemeEvent>,
    onBackClicked: () -> Unit,
    onItemClicked: (item: ThemeItem) -> Unit,
) {
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
                    text = "Theme",
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
                                modifier = Modifier
                                    .fillMaxWidth(),
                                item = item,
                                onItemClicked = {
                                    onItemClicked(item)
                                },
                            )
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun ItemComponent(
    modifier: Modifier = Modifier,
    item: ThemeItem,
    onItemClicked: () -> Unit,
) {
    Row(
        modifier = modifier
            .clickable(onClick = onItemClicked),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = item.enabled,
            onClick = { onItemClicked() },
        )
        Text(
            modifier = Modifier
                .padding(all = 16.dp),
            text = item.title,
        )
    }
}

@Preview
@Composable
internal fun ThemeScreenPreview() {
    AppTheme {
        ThemeScreenContent(
            modifier = Modifier.fillMaxSize(),
            state = ThemeState(
                items = listOf(
                    ThemeItem(
                        enabled = true,
                        title = "Auto",
                        type = ThemeItem.Type.Auto,
                    ),
                ),
            ),
            events = emptyFlow(),
            onBackClicked = {},
            onItemClicked = {},
        )
    }
}