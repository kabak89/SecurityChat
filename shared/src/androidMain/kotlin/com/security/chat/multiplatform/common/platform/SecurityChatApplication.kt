package com.security.chat.multiplatform.common.platform

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.security.chat.multiplatform.applifecycle.AppLifecycleChanger
import com.security.chat.multiplatform.common.platformspecific.MainActivity
import com.security.chat.multiplatform.di.initDI
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

public class SecurityChatApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        initDI {
            androidContext(this@SecurityChatApplication)
        }

        val appLifecycleChanger: AppLifecycleChanger = get()
        appLifecycleChanger.onAppStarted()

        observeActivity()
    }

    private fun observeActivity() {
        registerActivityLifecycleCallbacks(
            object : ActivityLifecycleCallbacks {
                private var activityModule: Module? = null

                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

                override fun onActivityStarted(activity: Activity) {}

                override fun onActivityResumed(activity: Activity) {
                    if (activity is MainActivity) {
                        val module: Module = module {
                            single { activity } bind Activity::class
                        }

                        activityModule = module
                        loadKoinModules(module)
                    }
                }

                override fun onActivityPaused(activity: Activity) {
                    if (activity is MainActivity) {
                        activityModule?.let { unloadKoinModules(it) }
                    }
                }

                override fun onActivityStopped(activity: Activity) {}

                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

                override fun onActivityDestroyed(activity: Activity) {}
            },
        )
    }
}
