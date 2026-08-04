import FirebaseCrashlytics
import shared

/// Reports non-fatal errors coming from Kotlin. Fatal ones are handled by NSExceptionKt,
/// which turns unhandled Kotlin exceptions into Crashlytics reports with a Kotlin stack trace.
final class CrashlyticsReporter: IosCrashReporterBridge {

    func recordException(name: String, reason: String, stackTrace: [String]) {
        let model = ExceptionModel(name: name, reason: reason)
        // Kotlin frames are already symbolicated, so there is no file or line to attach
        model.stackTrace = stackTrace.map { StackFrame(symbol: $0, file: "", line: 0) }
        Crashlytics.crashlytics().record(exceptionModel: model)
    }

    func log(message: String) {
        Crashlytics.crashlytics().log(message)
    }

    func setCustomKey(key: String, value: String) {
        Crashlytics.crashlytics().setCustomValue(value, forKey: key)
    }

    func setUserId(userId: String?) {
        Crashlytics.crashlytics().setUserID(userId ?? "")
    }
}
