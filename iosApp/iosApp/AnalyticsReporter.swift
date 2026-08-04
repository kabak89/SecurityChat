import FirebaseAnalytics
import shared

/// `Analytics` is qualified everywhere: the Kotlin interface of the same name is exported by the
/// shared framework, so the bare name is ambiguous in this file.
final class AnalyticsReporter: IosAnalyticsBridge {

    func logEvent(name: String, params: [String: String]) {
        FirebaseAnalytics.Analytics.logEvent(name, parameters: params)
    }

    func logScreenView(screenName: String, screenClass: String?) {
        var parameters: [String: Any] = [
            AnalyticsParameterScreenName: screenName,
        ]
        if let screenClass {
            parameters[AnalyticsParameterScreenClass] = screenClass
        }
        FirebaseAnalytics.Analytics.logEvent(AnalyticsEventScreenView, parameters: parameters)
    }
}
