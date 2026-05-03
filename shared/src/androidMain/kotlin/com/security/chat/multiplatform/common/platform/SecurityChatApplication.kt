package com.security.chat.multiplatform.common.platform

import android.app.Application
import com.security.chat.multiplatform.applifecycle.AppLifecycleChanger
import com.security.chat.multiplatform.di.initDI
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext

public class SecurityChatApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        initDI {
            androidContext(this@SecurityChatApplication)
        }

        val appLifecycleChanger: AppLifecycleChanger = get()
        appLifecycleChanger.onAppStarted()
    }
}
