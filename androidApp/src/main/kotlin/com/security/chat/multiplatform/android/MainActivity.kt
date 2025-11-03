package com.security.chat.multiplatform.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.arkivanov.decompose.defaultComponentContext
import com.security.chat.multiplatform.RootComponentImpl
import com.security.chat.multiplatform.RootContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = RootComponentImpl(componentContext = defaultComponentContext())

        setContent {
            RootContent(root)
        }
    }
}