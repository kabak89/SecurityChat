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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.security.chat.multiplatform.common.core.ui.SingleEventEffect
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme
import com.security.chat.multiplatform.features.authorize.component.api.SignInComponent
import kotlinx.coroutines.flow.Flow
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun SignInScreen(
    component: SignInComponent,
) {
    try {
        if (component.getDiScope().closed) return
    } catch (e: Exception) {
        println(e)
        return
    }

    val vm: SignInViewModel = koinViewModel(
        viewModelStoreOwner = component,
        scope = component.getDiScope(),
    )

    val state = vm.viewState.collectAsStateWithLifecycle().value

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
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Button(
                modifier = Modifier
                    .padding(horizontal = 16.dp),
                onClick = onSignUpClicked,
                content = {
                    Text("Sign Up")
                },
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
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(AppTheme.colors.element),
                onClick = onSignInClicked,
                content = {
                    Text(
                        text = "Sign In",
                        color = AppTheme.colors.textPrimary,
                    )
                },
            )
        }
    }
}