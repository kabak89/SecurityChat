package com.security.chat.multiplatform.features.call.ui.screens.call

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.security.chat.multiplatform.common.core.localization.StringRes
import com.security.chat.multiplatform.common.core.ui.Screen
import com.security.chat.multiplatform.common.icons.kit.DrawableRes
import com.security.chat.multiplatform.common.ui.kit.components.CenterContent
import com.security.chat.multiplatform.common.ui.kit.components.SideContent
import com.security.chat.multiplatform.common.ui.kit.components.ToolbarComponent
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme
import com.security.chat.multiplatform.features.call.component.api.CallMainComponent
import org.jetbrains.compose.resources.stringResource
import securitychat.common.icons_kit.generated.resources.ic_back
import securitychat.common.localization.generated.resources.call_title

@Composable
internal fun CallScreen(
    component: CallMainComponent,
) {
    Screen(
        component = component,
        screenName = "CallScreen",
    ) { _: CallState, _: CallViewModel ->
        CallContent(
            modifier = Modifier.fillMaxSize(),
            onBackClicked = component::onBackClicked,
        )
    }
}

@Composable
private fun CallContent(
    modifier: Modifier,
    onBackClicked: () -> Unit,
) {
    Column(
        modifier = modifier
            .background(AppTheme.colors.backgroundPrimary)
            .systemBarsPadding(),
    ) {
        ToolbarComponent(
            modifier = Modifier.fillMaxWidth(),
            startContent = SideContent.Button(
                icon = DrawableRes.ic_back,
                onClicked = onBackClicked,
            ),
            centerContent = CenterContent.Title(
                text = stringResource(StringRes.call_title),
            ),
        )
    }
}

@Preview
@Composable
internal fun CallScreenPreview() {
    AppTheme {
        CallContent(
            modifier = Modifier.fillMaxSize(),
            onBackClicked = {},
        )
    }
}
