import FirebaseCore
import FirebaseCrashlytics
import NSExceptionKtCrashlytics
import SwiftUI
import shared

@main
struct iOSApp: App {

    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate

    var body: some Scene {
        WindowGroup {
            ContentView()
                .ignoresSafeArea() // Compose draws edge-to-edge and handles insets itself
        }
    }
}

final class AppDelegate: NSObject, UIApplicationDelegate {

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        FirebaseApp.configure()
        #if DEBUG
        let isCrashReportingEnabled = false
        #else
        let isCrashReportingEnabled = true
        #endif
        // Crashlytics persists this flag, so it is set on every launch rather than only turned off
        Crashlytics.crashlytics().setCrashlyticsCollectionEnabled(isCrashReportingEnabled)
        // Reports unhandled Kotlin exceptions with a readable stack trace instead of a bare SIGABRT
        NSExceptionKt.addReporter(.crashlytics(causedByStrategy: .append))
        IosCrashReporterBridgeKt.setIosCrashReporterBridge(bridge: CrashlyticsReporter())
        KoinInitializerIosKt.doInitKoin()
        return true
    }
}
