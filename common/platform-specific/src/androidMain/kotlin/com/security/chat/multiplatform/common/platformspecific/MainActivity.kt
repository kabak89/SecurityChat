package com.security.chat.multiplatform.common.platformspecific

import android.Manifest
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.arkivanov.decompose.retainedComponent
import com.security.chat.multiplatform.features.push.navigation.api.IntentBuilderContract
import com.security.chat.multiplatform.features.root.component.RootComponentImpl
import com.security.chat.multiplatform.features.root.component.api.RootComponent
import com.security.chat.multiplatform.features.root.ui.screens.root.RootContent

public class MainActivity : ComponentActivity() {

    private lateinit var root: RootComponentImpl

    private val localNetworkPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestLocalNetworkAccessIfNeeded()
        root = retainedComponent { retainedContext ->
            RootComponentImpl(
                componentContext = retainedContext,
                initialDeepLink = intent.toDeepLink(),
            )
        }

        setContent {
            RootContent(root)
        }
    }

    /**
     * On target SDK 37 (Android 17) access to the local network is blocked by
     * default and guarded by the [Manifest.permission.ACCESS_LOCAL_NETWORK] runtime
     * permission. It is only declared in debug builds (see src/debug manifest), where
     * the app talks to a server on the LAN; release builds use a remote HTTPS host and
     * never request it.
     */
    private fun requestLocalNetworkAccessIfNeeded() {
        val isDebuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        /** ACCESS_LOCAL_NETWORK is enforced starting with SDK 37 (Android 17). */
        if (!isDebuggable || Build.VERSION.SDK_INT < 37) return

        val permission = Manifest.permission.ACCESS_LOCAL_NETWORK
        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            localNetworkPermissionLauncher.launch(permission)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.toDeepLink()?.let(root::handleDeepLink)
    }
}

private fun Intent.toDeepLink(): RootComponent.DeepLink? {
    if (action == Intent.ACTION_SEND && type == "text/plain") {
        val sharedText = getStringExtra(Intent.EXTRA_TEXT) ?: return null
        return RootComponent.DeepLink.SendText(text = sharedText)
    }

    val chatId = getStringExtra(IntentBuilderContract.EXTRA_CHAT_ID) ?: return null

    return when (action) {
        IntentBuilderContract.ACTION_OPEN_GROUP_CHAT -> {
            RootComponent.DeepLink.OpenGroupChat(chatId = chatId)
        }

        else -> null
    }
}
