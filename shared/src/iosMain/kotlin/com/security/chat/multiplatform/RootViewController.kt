package com.security.chat.multiplatform

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.uikit.OnFocusBehavior
import androidx.compose.ui.window.ComposeUIViewController
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.PredictiveBackGestureOverlay
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.lifecycle.ApplicationLifecycle
import com.arkivanov.essenty.lifecycle.subscribe
import com.security.chat.multiplatform.applifecycle.AppLifecycleChanger
import com.security.chat.multiplatform.common.log.Log
import com.security.chat.multiplatform.features.root.component.RootComponentImpl
import com.security.chat.multiplatform.features.root.ui.screens.root.RootContent
import org.koin.core.component.KoinComponent
import platform.UIKit.UIViewController

@Suppress("unused")
public fun rootViewController(): UIViewController {
    val lifecycle = ApplicationLifecycle()
    lifecycle.subscribe(
        onCreate = {
            Log.d { "ApplicationLifecycle onCreate" }

            val appLifecycleChanger: AppLifecycleChanger = DiInjector().getKoin().get()
            appLifecycleChanger.onAppStarted()
        },
    )

    val backDispatcher = BackDispatcher()

    val rootComponent = RootComponentImpl(
        componentContext = DefaultComponentContext(
            lifecycle = lifecycle,
            backHandler = backDispatcher,
        ),
        initialDeepLink = null,
    )

    return ComposeUIViewController(
        configure = {
            onFocusBehavior = OnFocusBehavior.DoNothing
            enforceStrictPlistSanityCheck = false
        },
        content = {
            PredictiveBackGestureOverlay(
                backDispatcher = backDispatcher,
                backIcon = null,
                modifier = Modifier.fillMaxSize(),
                endEdgeEnabled = false,
            ) {
                RootContent(
                    rootComponent = rootComponent,
                )
            }
        },
    )
}

private class DiInjector : KoinComponent