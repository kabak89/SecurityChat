package com.security.chat.multiplatform.features.push.navigation.impl

import android.content.Context
import android.content.Intent
import com.security.chat.multiplatform.common.platformspecific.MainActivity
import com.security.chat.multiplatform.features.push.navigation.api.IntentBuilder
import com.security.chat.multiplatform.features.push.navigation.api.IntentBuilderContract

internal class IntentBuilderImpl : IntentBuilder {

    override fun getOpenGroupChatIntent(
        context: Context,
        chatId: String,
    ): Intent {
        return mainActivityIntent(context).apply {
            action = IntentBuilderContract.ACTION_OPEN_GROUP_CHAT
            putExtra(IntentBuilderContract.EXTRA_CHAT_ID, chatId)
        }
    }

    private fun mainActivityIntent(context: Context): Intent {
        return Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
}
