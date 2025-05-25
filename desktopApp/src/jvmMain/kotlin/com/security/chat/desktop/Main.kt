package com.security.chat.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.extensions.compose.lifecycle.LifecycleController
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.security.chat.multiplatform.DefaultRootComponent
import com.security.chat.multiplatform.RootContent
import javax.swing.SwingUtilities

fun main() {
    val lifecycle = LifecycleRegistry()

    val root = runOnUiThread {
        DefaultRootComponent(
            componentContext = DefaultComponentContext(
                lifecycle = lifecycle,
            ),
        )
    }

    application {
        val windowState = rememberWindowState()

        LifecycleController(
            lifecycleRegistry = lifecycle,
            windowState = windowState,
        )

        Window(onCloseRequest = ::exitApplication) {
            RootContent(root)
        }
    }
}

internal fun <T> runOnUiThread(block: () -> T): T {
    if (SwingUtilities.isEventDispatchThread()) {
        return block()
    }

    var error: Throwable? = null
    var result: T? = null

    SwingUtilities.invokeAndWait {
        try {
            result = block()
        } catch (e: Throwable) {
            error = e
        }
    }

    error?.also { throw it }

    @Suppress("UNCHECKED_CAST")
    return result as T
}