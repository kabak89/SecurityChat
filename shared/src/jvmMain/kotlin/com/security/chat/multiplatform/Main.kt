package com.security.chat.multiplatform

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.extensions.compose.lifecycle.LifecycleController
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.doOnCreate
import com.security.chat.multiplatform.applifecycle.AppLifecycleChanger
import org.koin.core.component.KoinComponent
import javax.swing.SwingUtilities

fun main() {
    val lifecycle = LifecycleRegistry()

    lifecycle.doOnCreate {
        println("ApplicationLifecycle onCreate")

        val appLifecycleChanger: AppLifecycleChanger = DiInjector().getKoin().get()
        appLifecycleChanger.onAppStarted()
    }

    val root = runOnUiThread {
        RootComponentImpl(
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

        Window(
            onCloseRequest = ::exitApplication,
            title = "Security Chat",
            content = {
                RootContent(root)
            },
        )
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

private class DiInjector : KoinComponent