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
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SplashScreen(
    component: SplashComponent,
) {
    val vm: SplashViewModel = koinViewModel(
        viewModelStoreOwner = component,
        scope = component.getDiScope(),
    )

    SplashContent(
        modifier = Modifier
            .fillMaxSize(),
        onAuthorizeClicked = component::onAuthorizeClicked,
    )
}

@Composable
private fun SplashContent(
    modifier: Modifier = Modifier,
    onAuthorizeClicked: () -> Unit,
) {
    Column(
        modifier = modifier
            .background(Color.White)
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        Text("SplashScreen")
        Button(
            onClick = onAuthorizeClicked,
            content = {
                Text("go to auth")
            },
        )
    }
}