package com.security.chat.multiplatform.features.onboarding.ui.screens.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.security.chat.multiplatform.common.core.localization.StringRes
import com.security.chat.multiplatform.common.core.ui.Screen
import com.security.chat.multiplatform.common.core.ui.SingleEventEffect
import com.security.chat.multiplatform.common.ui.kit.MAX_CONTENT_WIDTH_DP
import com.security.chat.multiplatform.common.ui.kit.components.ButtonContent
import com.security.chat.multiplatform.common.ui.kit.components.ButtonPrimary
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme
import com.security.chat.multiplatform.features.onboarding.component.api.WelcomeComponent
import org.jetbrains.compose.resources.stringResource
import securitychat.common.localization.generated.resources.common_next

@Composable
internal fun WelcomeScreen(
    component: WelcomeComponent,
) {
    Screen(
        component = component,
        screenName = "WelcomeScreen",
    ) { _: WelcomeState, vm: WelcomeViewModel ->
        SingleEventEffect(vm.viewEvent) { event ->
            when (event) {
                WelcomeEvent.RequestNotificationPermission -> component.onContinue()
                WelcomeEvent.Finished -> component.onFinish()
            }
        }
        WelcomeScreenContent(onContinue = vm::onContinueClicked)
    }
}

@Composable
private fun WelcomeScreenContent(
    onContinue: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.backgroundPrimary)
            .systemBarsPadding(),
    ) {
        ButtonPrimary(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .widthIn(max = MAX_CONTENT_WIDTH_DP.dp)
                .fillMaxWidth()
                .padding(16.dp),
            content = ButtonContent.Text(
                text = stringResource(StringRes.common_next),
            ),
            onClicked = onContinue,
        )
    }
}

@Preview
@Composable
internal fun WelcomeScreenPreview() {
    AppTheme {
        WelcomeScreenContent(onContinue = {})
    }
}
