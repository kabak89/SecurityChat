package com.security.chat.multiplatform

import androidx.compose.runtime.remember
import androidx.compose.ui.uikit.OnFocusBehavior
import androidx.compose.ui.window.ComposeUIViewController
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.ApplicationLifecycle
import platform.UIKit.UIViewController

@Suppress("unused")
fun rootViewController(): UIViewController {
    return ComposeUIViewController(
        configure = {
            onFocusBehavior = OnFocusBehavior.DoNothing
            enforceStrictPlistSanityCheck = false
        },
        content = {
            val rootComponent = remember {
                RootComponentImpl(
                    componentContext = DefaultComponentContext(
                        lifecycle = ApplicationLifecycle(),
                    ),
                )
            }
            RootContent(
                rootComponent = rootComponent,
            )
        },
    )
}