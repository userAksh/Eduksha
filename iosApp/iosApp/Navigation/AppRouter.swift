import SwiftUI

// ── App-level destination enum ─────────────────────────────────────────────────

enum AppDestination: Hashable {
    case roleSelection
    case phoneAuth
    case otpVerification(phone: String, verificationID: String)
    case selectChild(children: [ChildProfile])
    case enterChildPin(child: ChildProfile)
    case childHome(childId: String, childName: String)
    case parentHome
    case conceptList
    case homeworkLog
    case streakHome(childName: String)
    case insights(childId: String, childName: String, grade: String, board: String)
    case studyGoals(childId: String, childName: String)
    case addChild
    case activityDetail(categoryKey: String, childId: String, childName: String)
    case parentTaskDetail(taskId: String)

    func hash(into hasher: inout Hasher) {
        switch self {
        case .roleSelection:                              hasher.combine(0)
        case .phoneAuth:                                  hasher.combine(1)
        case .otpVerification(let p, _):                  hasher.combine(2); hasher.combine(p)
        case .selectChild:                                hasher.combine(3)
        case .enterChildPin(let c):                       hasher.combine(4); hasher.combine(c.id)
        case .childHome(let id, _):                       hasher.combine(5); hasher.combine(id)
        case .parentHome:                                 hasher.combine(6)
        case .conceptList:                                hasher.combine(7)
        case .homeworkLog:                                hasher.combine(8)
        case .streakHome(let n):                          hasher.combine(9); hasher.combine(n)
        case .insights(let id, _, _, _):                  hasher.combine(10); hasher.combine(id)
        case .studyGoals(let id, _):                      hasher.combine(11); hasher.combine(id)
        case .addChild:                                   hasher.combine(12)
        case .activityDetail(let key, let id, _):         hasher.combine(13); hasher.combine(key); hasher.combine(id)
        case .parentTaskDetail(let id):                    hasher.combine(14); hasher.combine(id)
        }
    }

    static func == (lhs: AppDestination, rhs: AppDestination) -> Bool {
        lhs.hashValue == rhs.hashValue
    }
}

// ── Router ─────────────────────────────────────────────────────────────────────

@MainActor
final class AppRouter: ObservableObject {
    @Published var path = NavigationPath()

    func push(_ dest: AppDestination) { path.append(dest) }
    func pop() { if !path.isEmpty { path.removeLast() } }
    func popToRoot() { path = NavigationPath() }
    func replace(with dest: AppDestination) { path = NavigationPath(); path.append(dest) }
}

// ── Root view ──────────────────────────────────────────────────────────────────

struct AppRootView: View {
    @StateObject private var router = AppRouter()
    @EnvironmentObject private var appState: AppState

    var body: some View {
        NavigationStack(path: $router.path) {
            RoleSelectionView(
                onParentSelected: { router.push(.phoneAuth) },
                onChildSelected: {
                    if appState.children.isEmpty {
                        // No children yet — go to parent home to add
                        router.push(.parentHome)
                    } else {
                        router.push(.selectChild(children: appState.children))
                    }
                }
            )
            .navigationDestination(for: AppDestination.self) { dest in
                destinationView(for: dest)
            }
        }
        .environmentObject(router)
        .onAppear {
            // Handles the case where isAuthenticated is already true on launch
            // (e.g. restored from UserDefaults on simulator, or Firebase Auth session on device).
            if appState.isAuthenticated {
                router.replace(with: .parentHome)
            }
        }
        .onChange(of: appState.isAuthenticated) { authed in
            if authed { router.replace(with: .parentHome) }
        }
    }

    @ViewBuilder
    private func destinationView(for dest: AppDestination) -> some View {
        switch dest {
        case .roleSelection:
            RoleSelectionView(
                onParentSelected: { router.push(.phoneAuth) },
                onChildSelected: {
                    if appState.isAuthenticated {
                        router.push(.selectChild(children: appState.children))
                    } else {
                        router.push(.phoneAuth)
                    }
                }
            )

        case .phoneAuth:
            PhoneAuthView(
                onSendOTP: { name, phone, completion in
                    appState.parentName = name
                    appState.parentPhone = phone
                    Task { @MainActor in
                        NSLog("[Router] sendOTP task started, isMainThread=\(Thread.isMainThread)")
                        do {
                            let vid = try await appState.sendOTP(phone: "+91" + phone)
                            NSLog("[Router] sendOTP got vid=\(vid)")
                            completion(nil)
                            router.push(.otpVerification(phone: phone, verificationID: vid))
                        } catch {
                            NSLog("[Router] sendOTP error: \(error)")
                            completion(error.localizedDescription)
                        }
                    }
                }
            )

        case .otpVerification(let phone, let verificationID):
            OTPVerificationView(
                phone: phone,
                onVerify: { code, completion in
                    Task { @MainActor in
                        do {
                            try await appState.verifyOTP(verificationID: verificationID, code: code)
                            completion(nil)
                            // AppState auth listener fires → .onChange → router.replace(.parentHome)
                        } catch {
                            completion(error.localizedDescription)
                        }
                    }
                },
                onResend: {
                    Task { @MainActor in
                        guard let id = try? await appState.sendOTP(phone: "+91" + phone) else { return }
                        router.replace(with: .otpVerification(phone: phone, verificationID: id))
                    }
                },
                onBack: { router.pop() }
            )

        case .selectChild(let children):
            SelectChildView(
                children: children,
                onSelect: { child in router.push(.enterChildPin(child: child)) }
            )

        case .enterChildPin(let child):
            EnterChildPinView(
                childName: child.name.components(separatedBy: " ").first ?? child.name,
                onVerify: { enteredPin, completion in
                    if child.pin.isEmpty || enteredPin == child.pin {
                        completion(true)
                        router.push(.childHome(childId: child.id, childName: child.name))
                    } else {
                        completion(false)
                    }
                },
                onForgotPin: {},
                onBack: { router.pop() }
            )

        case .childHome(let childId, let childName):
            ChildHomeView(
                childId: childId,
                childName: childName,
                onNavigateToConceptList: { router.push(.conceptList) },
                onNavigateToHomeworkLog: { router.push(.homeworkLog) },
                onNavigateToClassTestPrep: { router.push(.conceptList) },
                onNavigateToStreakHome: {
                    router.push(.streakHome(childName: childName.components(separatedBy: " ").first ?? childName))
                },
                onNavigateToParentTask: { taskId in router.push(.parentTaskDetail(taskId: taskId)) },
                onRequestParentAccess: { router.popToRoot() }
            )
            .environmentObject(appState)

        case .parentHome:
            ParentHomeView(
                onNavigateToChildHome: { id in
                    let child = appState.children.first { $0.id == id }
                    if let child { router.push(.childHome(childId: child.id, childName: child.name)) }
                },
                onNavigateToAddChild: { router.push(.addChild) },
                onNavigateToInsights: { id in
                    let child = appState.children.first { $0.id == id }
                    if let child {
                        router.push(.insights(childId: child.id, childName: child.name, grade: child.grade, board: child.board))
                    }
                },
                onNavigateToStudyGoals: { id in
                    let child = appState.children.first { $0.id == id }
                    if let child {
                        router.push(.studyGoals(childId: child.id, childName: child.name.components(separatedBy: " ").first ?? child.name))
                    }
                },
                onNavigateToParentTask: { taskId in
                    router.push(.parentTaskDetail(taskId: taskId))
                },
                onNavigateToActivityDetail: { categoryKey, childId, childName in
                    router.push(.activityDetail(categoryKey: categoryKey, childId: childId, childName: childName))
                },
                onSignOut: {
                    try? appState.signOut()
                    router.popToRoot()
                }
            )
            .environmentObject(appState)

        case .conceptList:
            ConceptListView(onBack: { router.pop() }, onConceptTap: { _ in })

        case .homeworkLog:
            HomeworkLogView(onBack: { router.pop() }, onSaved: { router.pop() })

        case .streakHome(let childName):
            StreakHomeView(childName: childName, onBack: { router.pop() }, onUploadToday: {})

        case .insights(let childId, let childName, let grade, let board):
            let insChild = appState.children.first { $0.id == childId }
            InsightsView(
                childName: childName,
                childGrade: grade,
                childBoard: board,
                childGender: insChild?.gender ?? "male",
                engagement: appState.engagements[childId] ?? .empty,
                onBack: { router.pop() },
                onStudyGoals: {
                    router.push(.studyGoals(childId: childId, childName: childName))
                },
                onSubjectTap: { _ in }
            )

        case .studyGoals(let childId, let childName):
            let sgChild = appState.children.first { $0.id == childId }
            let sgGrade = Int(sgChild?.grade ?? "6") ?? 6
            StudyGoalsView(
                childId: childId,
                childName: childName,
                childGrade: sgGrade,
                childBoard: sgChild?.board ?? "CBSE",
                parentUID: appState.parentUID ?? "",
                onBack: { router.pop() },
                onAssigned: { router.pop() }
            )

        case .addChild:
            AddChildView(onDone: { router.pop() }, onBack: { router.pop() })

        case .activityDetail(let categoryKey, let childId, let childName):
            ActivityDetailView(
                categoryKey: categoryKey,
                childId: childId,
                childName: childName,
                onBack: { router.pop() }
            )

        case .parentTaskDetail(let taskId):
            ParentTaskDetailView(
                taskId: taskId,
                onBack: { router.pop() },
                onFinished: { router.pop() }
            )
        }
    }
}
