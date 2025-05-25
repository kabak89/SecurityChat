package com.security.chat.multiplatform.features.authorize.ui.screens.signin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.security.chat.multiplatform.features.authorize.component.SignInComponent

@Composable
fun SignInScreen(
    component: SignInComponent,
) {
    Column(
        modifier = Modifier
            .background(Color.White)
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        Text("SignInScreen")
    }
}