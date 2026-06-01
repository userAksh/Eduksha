import Foundation
import FirebaseAuth
import FirebaseFirestore
import FirebaseMessaging

// MARK: - AppState
// Single source of truth for auth state + parent data + children + engagement.

@MainActor
final class AppState: ObservableObject {
    @Published var isAuthenticated = false
    @Published var parentUID: String? = nil
    @Published var parentName: String = ""
    @Published var parentPhone: String = ""
    @Published var children: [ChildProfile] = []
    @Published var engagements: [String: ChildEngagement] = [:]  // childId → engagement
    @Published var weekActivities: [String: [DayActivity]] = [:]  // childId → Mon…Sun
    @Published var insights: [String: LearningInsight] = [:]      // childId → top insight
    @Published var parentTasks: [ParentTaskItem] = []             // all parent task requests
    @Published var pendingTaskTap: (taskId: String, role: String)? = nil  // notification tap

    private let firebase = FirebaseService.shared
    private var authHandle: AuthStateDidChangeListenerHandle?
    private var childrenListener: ListenerRegistration?
    private var parentTasksListener: ListenerRegistration?
    private var aggregators: [String: EngagementAggregator] = [:]
    private var latestFcmToken: String? = nil
    private var fcmTokenObserver: Any?
    private var taskTapObserver: Any?

    init() {
        setupNotificationObservers()

        #if targetEnvironment(simulator)
        // Firebase Auth always has nil currentUser on simulator (we bypass phone auth with a
        // hardcoded UID). Registering the auth listener would fire uid=nil and call reset().
        // Instead, persist login state in UserDefaults and restore it directly on launch.
        if UserDefaults.standard.bool(forKey: "simulatorLoggedIn") {
            let uid = "MEUUziPfpLRG0BSIA29Z4o0TUbK2"
            parentUID = uid
            isAuthenticated = true
            Task { @MainActor in
                await self.loadParentProfile(uid: uid)
                self.startListeningChildren(uid: uid)
            }
        }
        return
        #endif

        authHandle = firebase.addAuthStateListener { [weak self] uid in
            Task { @MainActor [weak self] in
                guard let self else { return }
                if let uid {
                    self.isAuthenticated = true
                    self.parentUID = uid
                    await self.loadParentProfile(uid: uid)
                    self.startListeningChildren(uid: uid)
                    // Save whatever FCM token we already have (token may arrive before auth)
                    if let token = self.latestFcmToken {
                        Task { try? await self.firebase.saveFcmToken(parentUID: uid, token: token) }
                    }
                } else {
                    self.reset()
                }
            }
        }
    }

    deinit {
        if let h = authHandle { Auth.auth().removeStateDidChangeListener(h) }
        childrenListener?.remove()
        if let obs = fcmTokenObserver { NotificationCenter.default.removeObserver(obs) }
        if let obs = taskTapObserver  { NotificationCenter.default.removeObserver(obs) }
        // aggregators self-clean via their own deinit
    }

    // MARK: Auth actions

    func sendOTP(phone: String) async throws -> String {
        try await firebase.sendOTP(phone: phone)
    }

    func verifyOTP(verificationID: String, code: String) async throws {
        #if targetEnvironment(simulator)
        // Firebase async Auth calls crash on simulator (Firebase 11 @isolated(any) threading bug).
        // Persist login state in UserDefaults so it survives rebuilds (init() reads it back).
        let realUID = "MEUUziPfpLRG0BSIA29Z4o0TUbK2"
        UserDefaults.standard.set(true, forKey: "simulatorLoggedIn")
        parentUID = realUID
        isAuthenticated = true
        await loadParentProfile(uid: realUID)
        startListeningChildren(uid: realUID)
        return
        #endif

        let uid = try await firebase.verifyOTP(verificationID: verificationID, code: code)
        parentUID = uid
        if !parentPhone.isEmpty {
            try? await firebase.saveParentProfile(uid: uid, name: parentName, phone: parentPhone)
        }
    }

    func saveProfile(name: String, phone: String) async throws {
        guard let uid = parentUID else { return }
        try await firebase.saveParentProfile(uid: uid, name: name, phone: phone)
        parentName = name
        parentPhone = phone
    }

    func signOut() throws {
        #if targetEnvironment(simulator)
        UserDefaults.standard.removeObject(forKey: "simulatorLoggedIn")
        reset()
        return
        #endif
        try firebase.signOut()
    }

    // MARK: Private helpers

    private func loadParentProfile(uid: String) async {
        guard let profile = try? await firebase.getParentProfile(uid: uid) else { return }
        parentName = profile.name
        parentPhone = profile.phone
    }

    func sendReminder(taskId: String) {
        Task { try? await firebase.sendParentTaskReminder(taskId: taskId) }
    }

    private func startListeningChildren(uid: String) {
        childrenListener?.remove()
        childrenListener = firebase.listenChildren(parentUID: uid) { [weak self] updated in
            guard let self else { return }
            self.children = updated
            self.syncAggregators(for: updated)
        }
        parentTasksListener?.remove()
        parentTasksListener = firebase.listenParentTasks(parentUID: uid) { [weak self] tasks in
            self?.parentTasks = tasks
        }
    }

    private func syncAggregators(for children: [ChildProfile]) {
        let ids = Set(children.map(\.id))

        // Remove aggregators for removed children
        for (id, agg) in aggregators where !ids.contains(id) {
            agg.stop()
            aggregators.removeValue(forKey: id)
        }

        // Add aggregators for new children
        for child in children where aggregators[child.id] == nil {
            let agg = EngagementAggregator()
            agg.listen(childId: child.id) { [weak self] eng in
                self?.engagements[child.id] = eng
            }
            aggregators[child.id] = agg
            Task { [weak self, id = child.id] in
                guard let self else { return }
                let items = await self.firebase.fetchActivityItems(childId: id)
                self.weekActivities[id] = bucketWeek(items: items)
                self.insights[id] = computeLearningInsight(items: items)
            }
        }
    }

    private func setupNotificationObservers() {
        // FCM token: cache it and write to Firestore whenever we're authenticated
        fcmTokenObserver = NotificationCenter.default.addObserver(
            forName: .didReceiveFcmToken, object: nil, queue: .main
        ) { [weak self] note in
            guard let self, let token = note.object as? String else { return }
            self.latestFcmToken = token
            guard let uid = self.parentUID else { return }  // save immediately if already authed
            Task { try? await self.firebase.saveFcmToken(parentUID: uid, token: token) }
        }

        // Notification tap: set pendingTaskTap so AppRootView can navigate
        taskTapObserver = NotificationCenter.default.addObserver(
            forName: .didTapTaskNotification, object: nil, queue: .main
        ) { [weak self] note in
            guard let info   = note.userInfo,
                  let taskId = info["taskId"] as? String,
                  let role   = info["role"]   as? String else { return }
            self?.pendingTaskTap = (taskId: taskId, role: role)
        }
    }

    private func reset() {
        isAuthenticated = false
        parentUID = nil
        parentName = ""
        parentPhone = ""
        children = []
        engagements = [:]
        weekActivities = [:]
        insights = [:]
        parentTasks = []
        childrenListener?.remove()
        childrenListener = nil
        parentTasksListener?.remove()
        parentTasksListener = nil
        aggregators.values.forEach { $0.stop() }
        aggregators = [:]
    }
}
