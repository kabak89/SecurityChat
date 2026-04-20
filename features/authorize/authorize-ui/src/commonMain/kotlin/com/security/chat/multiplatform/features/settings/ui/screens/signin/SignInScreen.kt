package com.security.chat.multiplatform.features.settings.ui.screens.signin

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
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.security.chat.multiplatform.common.core.ui.Screen
import com.security.chat.multiplatform.common.core.ui.SingleEventEffect
import com.security.chat.multiplatform.common.ui.kit.components.ButtonContent
import com.security.chat.multiplatform.common.ui.kit.components.ButtonPrimary
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme
import com.security.chat.multiplatform.features.authorize.component.api.SignInComponent
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
            onUsernameTextChanged = vm::onUsernameTextChanged,
            onPasswordTextChanged = vm::onPasswordTextChanged,
            onSignInClicked = vm::onSignInClicked,
            onSignUpClicked = component::onSignUpClicked,
            onAuthorized = component::onSuccessfulSignIn,
        )
    }
}

@Composable
private fun SignInContent(
    modifier: Modifier = Modifier,
    state: SignInState,
    events: Flow<SignInEvent>,
    onUsernameTextChanged: (String) -> Unit,
    onPasswordTextChanged: (String) -> Unit,
    onSignInClicked: () -> Unit,
    onSignUpClicked: () -> Unit,
    onAuthorized: () -> Unit,
) {
    SingleEventEffect(
        sideEffectFlow = events,
        collector = { event ->
            when (event) {
                SignInEvent.Authorized -> {
                    onAuthorized()
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
                .padding(horizontal = 16.dp)
                .semantics { contentType = ContentType.Username },
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
                .padding(horizontal = 16.dp)
                .semantics { contentType = ContentType.Password },
            value = state.password,
            onValueChange = onPasswordTextChanged,
            placeholder = {
                Text("Password")
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
                content = ButtonContent.Text("Sign In"),
                onClicked = onSignInClicked,
            )
        }
    }
}

@Preview
@Composable
internal fun SignInContentPreview() {
    AppTheme {
        SignInContent(
            modifier = Modifier.fillMaxSize(),
            state = SignInState(
                username = "john.doe",
                password = "password",
                isLoading = false,
            ),
            events = emptyFlow(),
            onUsernameTextChanged = {},
            onPasswordTextChanged = {},
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
                username = "john.doe",
                password = "password",
                isLoading = true,
            ),
            events = emptyFlow(),
            onUsernameTextChanged = {},
            onPasswordTextChanged = {},
            onSignInClicked = {},
            onSignUpClicked = {},
            onAuthorized = {},
        )
    }
}