package com.security.chat.multiplatform.common.analytics

import com.security.chat.multiplatform.common.log.Log

/**
 * Firebase Analytics has no JVM SDK, so on desktop all calls are no-ops.
 */
internal class AnalyticsJvm : Analytics {

    override fun logEvent(name: String, params: Map<String, String>) {
        Log.d { "logEvent: name: $name, params: $params" }
    }

    override fun logScreenView(screenName: String, screenClass: String?) {
        Log.d { "logScreenView: screenName: $screenName, screenClass: $screenClass" }
    }
}
