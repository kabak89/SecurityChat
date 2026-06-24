package com.security.chat.multiplatform.features.authorize.ui.screens.signin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.security.chat.multiplatform.common.core.ui.Screen
import com.security.chat.multiplatform.common.core.ui.SingleEventEffect
import com.security.chat.multiplatform.common.ui.kit.components.ButtonContent
import com.security.chat.multiplatform.common.ui.kit.components.ButtonPrimary
import com.security.chat.multiplatform.common.ui.kit.components.alertdialog.AlertDialogComponent
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme
import com.security.chat.multiplatform.features.authorize.component.api.SignInComponent
import com.security.chat.multiplatform.features.authorize.component.api.UserState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@Composable
internal fun SignInScreen(
    component: SignInComponent,
) {
    Screen(component) { state: SignInState, vm: SignInViewModel ->
        SignInContent(
            modifier = Modifier
                .fillMaxSize(),
            state = state,
            events = vm.viewEvent,
            onPrivateKeyTextChanged = vm::onPrivateKeyTextChanged,
            onSignInClicked = vm::onSignInClicked,
            onSignUpClicked = component::onSignUpClicked,
            onAuthorized = {
                component.onSuccessfulSignIn(
                    userState = UserState(state.isOnboardingPassed),
                )
            },
        )
    }
}

@Composable
private fun SignInContent(
    modifier: Modifier = Modifier,
    state: SignInState,
    events: Flow<SignInEvent>,
    onPrivateKeyTextChanged: (String) -> Unit,
    onSignInClicked: () -> Unit,
    onSignUpClicked: () -> Unit,
    onAuthorized: () -> Unit,
) {
    SingleEventEffect(
        sideEffectFlow = events,
        collector = { event ->
            when (event) {
                SignInEvent.Authorized -> onAuthorized()
            }
        },
    )
    val hazeState = rememberHazeState()
    Column(
        modifier = modifier
            .background(AppTheme.colors.backgroundPrimary)
            .fillMaxSize()
            .statusBarsPadding()
            .hazeSource(hazeState),
    ) {
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            ButtonPrimary(
                modifier = Modifier
                    .padding(horizontal = 16.dp),
                onClicked = onSignUpClicked,
                content = ButtonContent.Text("Sign Up"),
            )
        }
        Spacer(Modifier.height(16.dp))
        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            value = state.privateKey,
            onValueChange = onPrivateKeyTextChanged,
            placeholder = {
                Text("Private key")
            },
            enabled = !state.isLoading,
            maxLines = 3,
        )
        Spacer(Modifier.height(16.dp))
        if (state.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(alignment = Alignment.CenterHorizontally),
            )
        } else {
            ButtonPrimary(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                content = ButtonContent.Text("Sign In"),
                onClicked = onSignInClicked,
                enabled = state.isSignInEnabled,
            )
        }
    }
    if (state.alertDialogDescriptor != null) {
        AlertDialogComponent(
            content = state.alertDialogDescriptor.content,
            hazeState = hazeState,
            onDismissRequest = state.alertDialogDescriptor.dismissAction,
            onPositiveButtonClicked = state.alertDialogDescriptor.positiveAction,
            onNegativeButtonClicked = state.alertDialogDescriptor.negativeAction,
        )
    }
}

@Preview
@Composable
internal fun SignInContentPreview() {
    AppTheme {
        SignInContent(
            modifier = Modifier.fillMaxSize(),
            state = SignInState(
                privateKey = "PRIVATE KEY",
                isLoading = false,
                isSignInEnabled = false,
                alertDialogDescriptor = null,
                isOnboardingPassed = false,
            ),
            events = emptyFlow(),
            onPrivateKeyTextChanged = {},
            onSignInClicked = {},
            onSignUpClicked = {},
            onAuthorized = {},
        )
    }
}

@Preview
@Composable
internal fun SignInContentPreviewLoading() {
    AppTheme {
        SignInContent(
            modifier = Modifier.fillMaxSize(),
            state = SignInState(
                privateKey = "PRIVATE KEY",
                isLoading = true,
                isSignInEnabled = false,
                alertDialogDescriptor = null,
                isOnboardingPassed = false,
            ),
            events = emptyFlow(),
            onPrivateKeyTextChanged = {},
            onSignInClicked = {},
            onSignUpClicked = {},
            onAuthorized = {},
        )
    }
}