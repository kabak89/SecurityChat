package com.security.chat.multiplatform.common.platformspecific

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.arkivanov.decompose.defaultComponentContext
import com.security.chat.multiplatform.features.push.navigation.api.IntentBuilderContract
import com.security.chat.multiplatform.features.root.component.RootComponentImpl
import com.security.chat.multiplatform.features.root.component.api.RootComponent
import com.security.chat.multiplatform.features.root.ui.screens.root.RootContent

public class MainActivity : ComponentActivity() {

    private lateinit var root: RootComponentImpl

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        root = RootComponentImpl(
            componentContext = defaultComponentContext(),
            initialDeepLink = intent.toDeepLink(),
        )

        setContent {
            RootContent(root)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.toDeepLink()?.let(root::handleDeepLink)
    }
}

private fun Intent.toDeepLink(): RootComponent.DeepLink? {
    if (action != IntentBuilderContract.ACTION_OPEN_CHAT) return null
    val chatId = getStringExtra(IntentBuilderContract.EXTRA_CHAT_ID) ?: return null
    return RootComponent.DeepLink.OpenChat(chatId = chatId)
}
