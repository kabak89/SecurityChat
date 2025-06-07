package com.security.chat.multiplatform

import android.app.Application
import com.security.chat.multiplatform.common.ContextHolder

class SecurityChatApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        ContextHolder.setContext(this.applicationContext)
    }
}