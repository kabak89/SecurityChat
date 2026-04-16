package com.security.chat.multiplatform.features.profile.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.security.chat.multiplatform.common.icons.kit.DrawableRes
import com.security.chat.multiplatform.common.ui.kit.components.CenterContent
import com.security.chat.multiplatform.common.ui.kit.components.SideContent
import com.security.chat.multiplatform.common.ui.kit.components.ToolbarComponent
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme
import com.security.chat.multiplatform.features.profile.component.api.ProfileMainComponent
import org.koin.compose.viewmodel.koinViewModel
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
        onBackClicked = component::onBackClicked,
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
                startContent = SideContent.Button(
                    icon = DrawableRes.ic_back,
                    onClicked = onBackClicked,
                ),
                centerContent = CenterContent.Title(
                    text = "Profile",
                ),
                endContent = null,
            )
        }
    }
}

@Preview
@Composable
internal fun ProfileMainScreenPreview() {
    AppTheme {
        ProfileMainScreenContent(
            modifier = Modifier.fillMaxSize(),
            state = ProfileMainState,
            onBackClicked = {},
        )
    }
}
