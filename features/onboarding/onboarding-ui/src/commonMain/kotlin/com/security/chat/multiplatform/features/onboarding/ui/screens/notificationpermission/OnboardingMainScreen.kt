package com.security.chat.multiplatform.features.onboarding.ui.screens.notificationpermission

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.security.chat.multiplatform.common.core.ui.Screen
import com.security.chat.multiplatform.common.core.ui.SingleEventEffect
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme
import com.security.chat.multiplatform.features.onboarding.component.api.OnboardingMainComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun NotificationPermissionScreen(
    component: OnboardingMainComponent,
) {
    Screen(component) { state: NotificationPermissionState, vm: NotificationPermissionViewModel ->
        NotificationPermissionScreenContent(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            state = state,
            events = vm.viewEvent,
            onSkipClicked = component::onFinish,
            onFinishClicked = vm::onFinishOnboardingClicked,
            close = component::onFinish,
        )
    }
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = true,
        onBackCompleted = component::onFinish,
    )
}

@Composable
private fun NotificationPermissionScreenContent(
    modifier: Modifier,
    state: NotificationPermissionState,
    events: Flow<NotificationPermissionEvent>,
    onSkipClicked: () -> Unit,
    onFinishClicked: () -> Unit,
    close: () -> Unit,
) {
    SingleEventEffect(
        sideEffectFlow = events,
        collector = { event ->
            when (event) {
                NotificationPermissionEvent.Finished -> close()
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
                .fillMaxSize()
                .padding(all = 16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Onboarding",
            )
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                onClick = onFinishClicked,
            ) {
                Text(text = "Finish")
            }
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                onClick = onSkipClicked,
            ) {
                Text(text = "Skip")
            }
        }
    }
}

@Preview
@Composable
internal fun NotificationPermissionScreenPreview() {
    AppTheme {
        NotificationPermissionScreenContent(
            modifier = Modifier.fillMaxSize(),
            state = NotificationPermissionState(),
            events = emptyFlow(),
            onSkipClicked = {},
            onFinishClicked = {},
            close = {},
        )
    }
}
