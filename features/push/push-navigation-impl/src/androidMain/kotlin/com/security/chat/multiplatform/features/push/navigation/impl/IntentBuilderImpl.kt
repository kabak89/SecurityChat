package com.security.chat.multiplatform.features.push.navigation.impl

import android.content.Context
import android.content.Intent
import com.security.chat.multiplatform.features.push.navigation.api.IntentBuilder

internal class IntentBuilderImpl : IntentBuilder {

    override fun getOpenAppIntent(context: Context): Intent {
        val launchIntentForPackage = requireNotNull(
            context.packageManager.getLaunchIntentForPackage(context.packageName),
        )
        return launchIntentForPackage
    }
}