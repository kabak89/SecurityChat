package com.security.chat.multiplatform.common.analytics

import com.security.chat.multiplatform.common.log.Log
import kotlin.concurrent.Volatile

internal class AnalyticsIos : Analytics {

    override fun logEvent(name: String, params: Map<String, String>) {
        Log.d { "logEvent: name: $name, params: $params" }
        bridgeOrWarn()?.logEvent(name, params)
    }

    override fun logScreenView(screenName: String, screenClass: String?) {
        Log.d { "logScreenView: screenName: $screenName, screenClass: $screenClass" }
        bridgeOrWarn()?.logScreenView(screenName, screenClass)
    }

    private fun bridgeOrWarn(): IosAnalyticsBridge? {
        val bridge = IosAnalyticsBridgeHolder.bridge
        if (bridge == null && !isMissingBridgeReported) {
            isMissingBridgeReported = true
            println("AnalyticsIos: no Swift bridge registered, events are dropped")
        }
        return bridge
    }

    private companion object {

        @Volatile
        var isMissingBridgeReported: Boolean = false
    }
}
