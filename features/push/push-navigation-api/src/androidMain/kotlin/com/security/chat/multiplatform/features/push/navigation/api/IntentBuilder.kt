package com.security.chat.multiplatform.features.push.navigation.api

import android.content.Context
import android.content.Intent

public interface IntentBuilder {
    public fun getOpenPersonalChatIntent(context: Context, chatId: String): Intent
    public fun getOpenGroupChatIntent(context: Context, chatId: String): Intent
}

public object IntentBuilderContract {
    public const val ACTION_OPEN_PERSONAL_CHAT: String =
        "com.security.chat.action.OPEN_PERSONAL_CHAT"
    public const val ACTION_OPEN_GROUP_CHAT: String = "com.security.chat.action.OPEN_GROUP_CHAT"
    public const val EXTRA_CHAT_ID: String = "extra_chat_id"
}
