package com.security.chat.multiplatform.features.profile.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
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
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme
import com.security.chat.multiplatform.features.profile.component.api.ProfileMainComponent
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import securitychat.common.icons_kit.generated.resources.Res
import securitychat.common.icons_kit.generated.resources.ic_back

@Composable
internal fun ProfileMainScreen(
    component: ProfileMainComponent,
) {
    try {
        if (component.getDiScope().closed) return
    } catch (e: Exception) {
        println(e)
        return
    }

    val vm: ProfileMainViewModel = koinViewModel(
        viewModelStoreOwner = component,
        scope = component.getDiScope(),
    )

    val state = vm.viewState.collectAsStateWithLifecycle().value

    ProfileMainScreenContent(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        state = state,
        onBackClicked = component::onExitClicked,
    )
}

@Composable
private fun ProfileMainScreenContent(
    modifier: Modifier,
    state: ProfileMainState,
    onBackClicked: () -> Unit,
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
                title = state.title,
                onBackClicked = onBackClicked,
            )
        }
    }
}

@Composable
private fun ToolbarComponent(
    modifier: Modifier = Modifier,
    title: String,
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
            text = title,
            textAlign = TextAlign.Center,
            style = AppTheme.typography.default,
            color = AppTheme.colors.textPrimary,
        )
        Spacer(
            modifier = Modifier
                .weight(0.2f),
        )
    }
}

@Preview
@Composable
internal fun ProfileMainScreenPreview() {
    AppTheme {
        ProfileMainScreenContent(
            modifier = Modifier.fillMaxSize(),
            state = ProfileMainState(title = "Profile"),
            onBackClicked = {},
        )
    }
}
