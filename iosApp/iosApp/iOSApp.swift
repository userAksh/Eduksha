import SwiftUI
import FirebaseCore
import FirebaseAuth
import FirebaseMessaging
import UserNotifications
import UIKit

// MARK: - Notification names (AppDelegate → AppState bridge)

extension Notification.Name {
    static let didReceiveFcmToken     = Notification.Name("com.dobi.didReceiveFcmToken")
    static let didTapTaskNotification = Notification.Name("com.dobi.didTapTaskNotification")
}

// MARK: - AppDelegate

class AppDelegate: NSObject, UIApplicationDelegate, MessagingDelegate, UNUserNotificationCenterDelegate {

    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        Messaging.messaging().delegate = self
        // Register for remote notifications immediately — required for Firebase
        // Phone Auth silent APNs verification (swizzling is disabled).
        UIApplication.shared.registerForRemoteNotifications()
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { granted, _ in
            NSLog("[FCM] notification permission granted=\(granted)")
        }
        return true
    }

    // Forward APNs token to both Firebase Auth and Firebase Messaging
    func application(_ application: UIApplication,
                     didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        #if DEBUG
        Auth.auth().setAPNSToken(deviceToken, type: .sandbox)
        #else
        Auth.auth().setAPNSToken(deviceToken, type: .prod)
        #endif
        Messaging.messaging().apnsToken = deviceToken
        NSLog("[FCM] APNs token registered len=\(deviceToken.count)")
    }

    func application(_ application: UIApplication,
                     didFailToRegisterForRemoteNotificationsWithError error: Error) {
        NSLog("[FCM] APNs registration failed: \(error.localizedDescription)")
    }

    // Forward silent APNs to Firebase Auth — required when swizzling is disabled
    func application(_ application: UIApplication,
                     didReceiveRemoteNotification userInfo: [AnyHashable: Any],
                     fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void) {
        if Auth.auth().canHandleNotification(userInfo) {
            completionHandler(.noData)
            return
        }
        completionHandler(.newData)
    }

    // Forward URL callbacks to Firebase Auth (reCAPTCHA + Phone Auth)
    func application(_ application: UIApplication, open url: URL,
                     options: [UIApplication.OpenURLOptionsKey: Any] = [:]) -> Bool {
        return Auth.auth().canHandle(url)
    }

    // MARK: MessagingDelegate — fires on fresh token or token refresh

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard let token = fcmToken else { return }
        NSLog("[FCM] Got registration token (len=\(token.count))")
        NotificationCenter.default.post(name: .didReceiveFcmToken, object: token)
    }

    // MARK: UNUserNotificationCenterDelegate

    // Show banner/sound even while app is foregrounded
    func userNotificationCenter(_ center: UNUserNotificationCenter,
                                willPresent notification: UNNotification,
                                withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        completionHandler([.banner, .sound, .badge])
    }

    // User tapped a notification — extract taskId + role and bridge to SwiftUI
    func userNotificationCenter(_ center: UNUserNotificationCenter,
                                didReceive response: UNNotificationResponse,
                                withCompletionHandler completionHandler: @escaping () -> Void) {
        let info = response.notification.request.content.userInfo
        if let taskId = info["taskId"] as? String, !taskId.isEmpty,
           let role   = info["role"]   as? String, !role.isEmpty {
            NSLog("[FCM] Notification tapped taskId=\(taskId) role=\(role)")
            NotificationCenter.default.post(
                name: .didTapTaskNotification,
                object: nil,
                userInfo: ["taskId": taskId, "role": role]
            )
        }
        completionHandler()
    }
}

// MARK: - App entry point

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate
    @StateObject private var appState = AppState()

    init() {
        FirebaseApp.configure()

        // Fresh-install detection: UserDefaults is wiped when the app is deleted,
        // but the iOS Keychain (where Firebase Auth stores its session) is not.
        // Without this check a reinstalled app skips login entirely.
        // We sign out before any auth-state listener is attached, so the app
        // always starts from RoleSelection on a clean install.
        let defaults = UserDefaults.standard
        if !defaults.bool(forKey: "appInstalledBefore") {
            try? Auth.auth().signOut()
            defaults.set(true, forKey: "appInstalledBefore")
        }

    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(appState)
        }
    }
}
