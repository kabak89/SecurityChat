package com.security.chat.multiplatform.features.push.navigation.api

import android.content.Context
import android.content.Intent

public interface IntentBuilder {
    public fun getOpenAppIntent(context: Context): Intent
}