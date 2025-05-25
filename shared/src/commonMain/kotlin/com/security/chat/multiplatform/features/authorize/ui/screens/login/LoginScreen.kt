package com.security.chat.multiplatform.features.authorize.ui.screens.login

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

@Composable
fun LoginScreen(
    component: LoginComponent,
) {
    Column(
        modifier = Modifier
            .background(Color.White)
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        Text("LoginScreen")
        Button(
            onClick = { component.onSignInClicked() },
            content = {
                Text("go to auth")
            },
        )
    }
}