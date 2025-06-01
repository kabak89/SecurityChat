package com.security.chat.multiplatform.features.splash.ui.screens.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.security.chat.multiplatform.common.core.ui.SingleEventEffect
import com.security.chat.multiplatform.features.splash.component.SplashComponent
import kotlinx.coroutines.flow.Flow
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SplashScreen(
    component: SplashComponent,
) {
    val vm: SplashViewModel = koinViewModel(
        viewModelStoreOwner = component,
        scope = component.getDiScope(),
    )

    val state = vm.viewState.collectAsStateWithLifecycle().value

    SplashContent(
        modifier = Modifier
            .fillMaxSize(),
        state = state,
        events = vm.viewEvent,
        onUserDetermineAsNotAuthorized = component::onAuthorizeClicked,
    )
}

@Composable
private fun SplashContent(
    modifier: Modifier = Modifier,
    state: SplashState,
    events: Flow<SplashEvent>,
    onUserDetermineAsNotAuthorized: () -> Unit,
) {
    SingleEventEffect(
        sideEffectFlow = events,
        collector = { event ->
            when (event) {
                SplashEvent.UserDetermineAsNotAuthorized -> {
                    onUserDetermineAsNotAuthorized()
                }
            }
        },
    )

    Box(
        modifier = modifier
            .background(Color.White),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center),
        ) {
            if (state.isLoading) {
                CircularProgressIndicator()
            }
            Text(
                text = "SplashScreen",
            )
        }
    }
}