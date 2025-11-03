package com.security.chat.multiplatform.common.platform.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.arkivanov.decompose.defaultComponentContext
import com.security.chat.multiplatform.RootComponentImpl
import com.security.chat.multiplatform.RootContent
import com.security.chat.multiplatform.applifecycle.AppLifecycleChanger
import org.koin.android.ext.android.get

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = RootComponentImpl(
            componentContext = defaultComponentContext(),
            onCreate = {
                val appLifecycleChanger: AppLifecycleChanger = get()
                appLifecycleChanger.onAppStarted()
            },
        )

        setContent {
            RootContent(root)
        }
    }
}