package com.security.chat.multiplatform.features.authorize.ui.screens.signup

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.security.chat.multiplatform.common.core.ui.SingleEventEffect
import com.security.chat.multiplatform.features.authorize.component.SignUpComponent
import kotlinx.coroutines.flow.Flow
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SignUpScreen(
    component: SignUpComponent,
) {
    try {
        if (component.getDiScope().closed) return
    } catch (e: Exception) {
        println(e)
        return
    }

    val vm: SignUpViewModel = koinViewModel(
        viewModelStoreOwner = component,
        scope = component.getDiScope(),
    )

    val state = vm.viewState.collectAsStateWithLifecycle().value

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
            .background(Color.White)
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            Button(
                modifier = Modifier
                    .padding(horizontal = 16.dp),
                onClick = onSignInClicked,
                content = {
                    Text("Sign In")
                },
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
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                onClick = onSignUpClicked,
                enabled = state.nextButtonEnabled,
                content = {
                    Text("Sign Up")
                },
            )
        }
    }
}