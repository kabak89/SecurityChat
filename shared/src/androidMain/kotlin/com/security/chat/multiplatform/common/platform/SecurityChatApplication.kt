package com.security.chat.multiplatform.common.platform

import android.app.Application
import com.security.chat.multiplatform.di.initKoin
import org.koin.android.ext.koin.androidContext

public class SecurityChatApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        initKoin {
            androidContext(this@SecurityChatApplication)
        }
    }
}
