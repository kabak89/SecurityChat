package com.security.chat.multiplatform.common.analytics

import kotlin.concurrent.Volatile

internal class AnalyticsIos : Analytics {

    override fun logEvent(name: String, params: Map<String, String>) {
        bridgeOrWarn()?.logEvent(name, params)
    }

    override fun logScreenView(screenName: String, screenClass: String?) {
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
