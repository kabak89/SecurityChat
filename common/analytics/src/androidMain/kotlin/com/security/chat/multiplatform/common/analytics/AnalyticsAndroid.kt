package com.security.chat.multiplatform.common.analytics

import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import com.security.chat.multiplatform.common.log.Log

internal class AnalyticsAndroid : Analytics {

    private val firebaseAnalytics: FirebaseAnalytics
        get() = FirebaseAnalytics.getInstance(FirebaseApp.getInstance().applicationContext)

    override fun logEvent(name: String, params: Map<String, String>) {
        Log.d { "logEvent: name: $name, params: $params" }
        firebaseAnalytics.logEvent(name) {
            params.forEach { (key, value) ->
                param(key, value)
            }
        }
    }

    override fun logScreenView(screenName: String, screenClass: String?) {
        Log.d { "logScreenView: screenName: $screenName, screenClass: $screenClass" }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            if (screenClass != null) {
                param(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
            }
        }
    }
}
