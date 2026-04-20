package com.security.chat.multiplatform.features.settings.ui.screens.signup

import androidx.compose.foundation.background
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
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme
import com.security.chat.multiplatform.features.authorize.component.api.SignUpComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@Composable
internal fun SignUpScreen(
    component: SignUpComponent,
) {
    Screen(component) { state: SignUpState, vm: SignUpViewModel ->
        SignUpContent(
            modifier = Modifier
                .fillMaxSize(),
            state = state,
            events = vm.viewEvent,
            onSignInClicked = component::onSignInClicked,
            onUsernameTextChanged = vm::onUsernameTextChanged,
            onPasswordTextChanged = vm::onPasswordTextChanged,
            onPasswordRepeatTextChanged = vm::onPasswordRepeatTextChanged,
            onSignUpClicked = vm::onSignUpClicked,
            onSuccessfulSignUp = component::onSuccessfulSignUp,
        )
    }
}

@Composable
private fun SignUpContent(
    modifier: Modifier = Modifier,
    state: SignUpState,
    events: Flow<SignUpEvent>,
    onSignInClicked: () -> Unit,
    onUsernameTextChanged: (String) -> Unit,
    onPasswordTextChanged: (String) -> Unit,
    onPasswordRepeatTextChanged: (String) -> Unit,
    onSignUpClicked: () -> Unit,
    onSuccessfulSignUp: () -> Unit,
) {
    SingleEventEffect(
        sideEffectFlow = events,
        collector = { event ->
            when (event) {
                SignUpEvent.SuccessSignUp -> {
                    onSuccessfulSignUp()
                }
            }
        },
    )
    Column(
        modifier = modifier
            .background(AppTheme.colors.backgroundPrimary)
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            ButtonPrimary(
                modifier = Modifier
                    .padding(horizontal = 16.dp),
                onClicked = onSignInClicked,
                content = ButtonContent.Text("Sign In"),
            )
        }
        Spacer(Modifier.height(16.dp))
        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            value = state.username,
            onValueChange = onUsernameTextChanged,
            placeholder = {
                Text("Username")
            },
            enabled = !state.isLoading,
        )
        Spacer(Modifier.height(16.dp))
        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            value = state.password,
            onValueChange = onPasswordTextChanged,
            placeholder = {
                Text("Password")
            },
            enabled = !state.isLoading,
        )
        Spacer(Modifier.height(16.dp))
        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            value = state.passwordRepeat,
            onValueChange = onPasswordRepeatTextChanged,
            placeholder = {
                Text("Repeat password")
            },
            enabled = !state.isLoading,
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
                onClicked = onSignUpClicked,
                enabled = state.nextButtonEnabled,
                content = ButtonContent.Text("Sign Up"),
            )
        }
    }
}

@Preview
@Composable
internal fun SignUpContentPreview() {
    AppTheme {
        SignUpContent(
            modifier = Modifier.fillMaxSize(),
            state = SignUpState(
                username = "john.doe",
                password = "password",
                passwordRepeat = "password",
                isLoading = false,
                nextButtonEnabled = true,
            ),
            events = emptyFlow(),
            onSignInClicked = {},
            onUsernameTextChanged = {},
            onPasswordTextChanged = {},
            onPasswordRepeatTextChanged = {},
            onSignUpClicked = {},
            onSuccessfulSignUp = {},
        )
    }
}

@Preview
@Composable
internal fun SignUpContentPreviewDisabled() {
    AppTheme {
        SignUpContent(
            modifier = Modifier.fillMaxSize(),
            state = SignUpState(
                username = "john.doe",
                password = "password",
                passwordRepeat = "pass",
                isLoading = false,
                nextButtonEnabled = false,
            ),
            events = emptyFlow(),
            onSignInClicked = {},
            onUsernameTextChanged = {},
            onPasswordTextChanged = {},
            onPasswordRepeatTextChanged = {},
            onSignUpClicked = {},
            onSuccessfulSignUp = {},
        )
    }
}

@Preview
@Composable
internal fun SignUpContentPreviewLoading() {
    AppTheme {
        SignUpContent(
            modifier = Modifier.fillMaxSize(),
            state = SignUpState(
                username = "john.doe",
                password = "password",
                passwordRepeat = "password",
                isLoading = true,
                nextButtonEnabled = true,
            ),
            events = emptyFlow(),
            onSignInClicked = {},
            onUsernameTextChanged = {},
            onPasswordTextChanged = {},
            onPasswordRepeatTextChanged = {},
            onSignUpClicked = {},
            onSuccessfulSignUp = {},
        )
    }
}