package com.security.chat.multiplatform

import androidx.compose.ui.uikit.OnFocusBehavior
import androidx.compose.ui.window.ComposeUIViewController
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.ApplicationLifecycle
import com.arkivanov.essenty.lifecycle.subscribe
import platform.UIKit.UIViewController

@Suppress("unused")
fun rootViewController(): UIViewController {
    val lifecycle = ApplicationLifecycle()
    lifecycle.subscribe(
        onCreate = {
            println("ApplicationLifecycle onCreate")
        },
    )
    val rootComponent = RootComponentImpl(
        componentContext = DefaultComponentContext(
            lifecycle = lifecycle,
        ),
    )

    return ComposeUIViewController(
        configure = {
            onFocusBehavior = OnFocusBehavior.DoNothing
            enforceStrictPlistSanityCheck = false
        },
        content = {
            RootContent(
                rootComponent = rootComponent,
            )
        },
    )
}