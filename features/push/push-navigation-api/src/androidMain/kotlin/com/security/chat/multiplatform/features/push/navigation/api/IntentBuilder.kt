package com.security.chat.multiplatform.features.push.navigation.api

import android.content.Context
import android.content.Intent

public interface IntentBuilder {
    public fun getOpenAppIntent(context: Context): Intent
    public fun getOpenChatIntent(context: Context, chatId: String): Intent
}

public object IntentBuilderContract {
    public const val ACTION_OPEN_CHAT: String = "com.security.chat.action.OPEN_CHAT"
    public const val EXTRA_CHAT_ID: String = "extra_chat_id"
}
