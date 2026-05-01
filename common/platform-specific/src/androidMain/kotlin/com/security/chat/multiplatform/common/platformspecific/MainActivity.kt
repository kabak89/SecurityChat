package com.security.chat.multiplatform.common.platformspecific

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.arkivanov.decompose.defaultComponentContext
import com.security.chat.multiplatform.features.root.component.RootComponentImpl
import com.security.chat.multiplatform.features.root.ui.screens.root.RootContent

public class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = RootComponentImpl(
            componentContext = defaultComponentContext(),
        )

        setContent {
            RootContent(root)
        }
    }
}