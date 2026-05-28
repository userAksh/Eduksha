import SwiftUI
import FirebaseCore
import FirebaseAuth

// TODO: restore `import shared` + KoinInitKt.doInitKoin() once Kotlin/Native supports iOS 26 SDK

@main
struct iOSApp: App {
    @StateObject private var appState = AppState()

    init() {
        FirebaseApp.configure()
        #if targetEnvironment(simulator)
        // Simulator has no APNs — bypass app verification so test phone numbers work
        Auth.auth().settings?.isAppVerificationDisabledForTesting = true
        #endif
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(appState)
        }
    }
}
