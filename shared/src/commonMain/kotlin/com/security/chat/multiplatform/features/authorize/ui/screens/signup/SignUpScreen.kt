package com.security.chat.multiplatform.features.authorize.ui.screens.signup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.security.chat.multiplatform.features.authorize.component.LoginComponent
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SignUpScreen(
    component: LoginComponent,
) {
    val vm: SignUpViewModel = koinViewModel(viewModelStoreOwner = component)

    SignUpContent(
        modifier = Modifier
            .fillMaxSize(),
        onSignInClicked = component::onSignInClicked,
    )
}

@Composable
private fun SignUpContent(
    modifier: Modifier = Modifier,
    onSignInClicked: () -> Unit,
) {
    Column(
        modifier = modifier
            .background(Color.White)
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        Text("LoginScreen")
        Button(
            onClick = onSignInClicked,
            content = {
                Text("go to auth")
            },
        )
    }
}