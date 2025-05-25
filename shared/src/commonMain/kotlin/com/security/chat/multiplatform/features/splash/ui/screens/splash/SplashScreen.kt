package com.security.chat.multiplatform.features.splash.ui.screens.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.security.chat.multiplatform.features.splash.component.SplashComponent

@Composable
fun SplashScreen(
    component: SplashComponent,
) {
    Column(
        modifier = Modifier
            .background(Color.White)
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        Text("SplashScreen")
        Button(
            onClick = { component.onAuthorizeClicked() },
            content = {
                Text("go to auth")
            },
        )
    }
}