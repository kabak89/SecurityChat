package com.security.chat.multiplatform.common.analytics

import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent

internal class AnalyticsAndroid : Analytics {

    private val firebaseAnalytics: FirebaseAnalytics
        get() = FirebaseAnalytics.getInstance(FirebaseApp.getInstance().applicationContext)

    override fun logEvent(name: String, params: Map<String, String>) {
        firebaseAnalytics.logEvent(name) {
            params.forEach { (key, value) ->
                param(key, value)
            }
        }
    }

    override fun logScreenView(screenName: String, screenClass: String?) {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            if (screenClass != null) {
                param(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
            }
        }
    }
}
