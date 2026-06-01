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
    case conceptList(childId: String)
    case conceptDetail(conceptId: String, childId: String)
    case homeworkLog(childId: String)
    case streakHome(childName: String)
    case insights(childId: String, childName: String, grade: String, board: String)
    case studyGoals(childId: String, childName: String)
    case addChild
    case activityDetail(categoryKey: String, childId: String, childName: String)
    case doParentTask(taskId: String, childId: String)
    case parentTaskDetail(taskId: String)
    case parentTasksList(childId: String)
    case parentActionableTasks
    case earnRewardPicker(childId: String, grade: Int, parentId: String)
    case uploadWork(kind: String, conceptId: String, childId: String, unitName: String, conceptName: String, subject: String, subjectColor: String, subjectIcon: String, unitId: String, grade: Int)
    case questionSource(conceptId: String, childId: String, unitName: String, conceptName: String, subject: String, subjectColor: String, unitId: String)
    case revisitDetail(groupKey: String, childId: String)
    case testYourselfFormat(source: String, conceptId: String, unitId: String, unitName: String, conceptName: String, subject: String, subjectColor: String, childId: String, grade: Int)
    case practiseLessonPicker(childId: String)
    case unitDetail(unitId: String, childId: String, unitName: String, subject: String)
    case quiz(conceptId: String, childId: String, unitName: String, subject: String)
    case quizForParentTask(conceptId: String, childId: String, unitName: String, subject: String, parentTaskId: String)
    case tuitionUploadOptions(childId: String, grade: Int, mode: String)
    case tuitionLearning(childId: String, grade: Int, kind: String)
    case tuitionProblems
    case revisitTopics(childId: String)
    case needRevision(childId: String)
    case classTestPrep(childId: String, grade: Int)
    case writeAnswer(displayName: String, subject: String, grade: Int, scopeText: String, childId: String)
    case aiPracticeUpload(childId: String, grade: Int, conceptId: String, unitId: String, displayName: String, conceptName: String, subject: String, subjectColor: String, subjectIcon: String, teacherHonorific: String, teacherName: String, testDateInterval: Double, scopeText: String)
    case quizResult(conceptId: String, unitId: String, childId: String)

    func hash(into hasher: inout Hasher) {
        switch self {
        case .roleSelection:                              hasher.combine(0)
        case .phoneAuth:                                  hasher.combine(1)
        case .otpVerification(let p, _):                  hasher.combine(2); hasher.combine(p)
        case .selectChild:                                hasher.combine(3)
        case .enterChildPin(let c):                       hasher.combine(4); hasher.combine(c.id)
        case .childHome(let id, _):                       hasher.combine(5); hasher.combine(id)
        case .parentHome:                                 hasher.combine(6)
        case .conceptList(let id):                        hasher.combine(7); hasher.combine(id)
        case .conceptDetail(let id, _):                  hasher.combine(16); hasher.combine(id)
        case .homeworkLog(let id):                        hasher.combine(8); hasher.combine(id)
        case .streakHome(let n):                          hasher.combine(9); hasher.combine(n)
        case .insights(let id, _, _, _):                  hasher.combine(10); hasher.combine(id)
        case .studyGoals(let id, _):                      hasher.combine(11); hasher.combine(id)
        case .addChild:                                   hasher.combine(12)
        case .activityDetail(let key, let id, _):         hasher.combine(13); hasher.combine(key); hasher.combine(id)
        case .doParentTask(let id, _):                     hasher.combine(90); hasher.combine(id)
        case .parentTaskDetail(let id):                    hasher.combine(14); hasher.combine(id)
        case .parentTasksList(let id):                     hasher.combine(15); hasher.combine(id)
        case .parentActionableTasks:                       hasher.combine(92)
        case .earnRewardPicker(let cid, let g, _):         hasher.combine(93); hasher.combine(cid); hasher.combine(g)
        case .uploadWork(let k, let id, _, _, _, _, _, _, _, _): hasher.combine(17); hasher.combine(k); hasher.combine(id)
        case .questionSource(let id, _, _, _, _, _, _):     hasher.combine(18); hasher.combine(id)
        case .revisitDetail(let gk, let cid):              hasher.combine(30); hasher.combine(gk); hasher.combine(cid)
        case .testYourselfFormat(let src, let cid, let uid, _, _, _, _, let kid, _): hasher.combine(31); hasher.combine(src); hasher.combine(cid); hasher.combine(uid); hasher.combine(kid)
        case .practiseLessonPicker(let id):               hasher.combine(28); hasher.combine(id)
        case .unitDetail(let id, _, _, _):                hasher.combine(29); hasher.combine(id)
        case .quiz(let id, _, _, _):                      hasher.combine(19); hasher.combine(id)
        case .quizForParentTask(let id, _, _, _, let pid): hasher.combine(91); hasher.combine(id); hasher.combine(pid)
        case .tuitionUploadOptions(let id, _, let m):     hasher.combine(20); hasher.combine(id); hasher.combine(m)
        case .tuitionLearning(let id, _, let k):          hasher.combine(21); hasher.combine(id); hasher.combine(k)
        case .tuitionProblems:                            hasher.combine(22)
        case .revisitTopics(let id):                      hasher.combine(23); hasher.combine(id)
        case .needRevision(let id):                       hasher.combine(24); hasher.combine(id)
        case .classTestPrep(let id, let g):               hasher.combine(25); hasher.combine(id); hasher.combine(g)
        case .writeAnswer(let name, _, _, _, let id):      hasher.combine(26); hasher.combine(name); hasher.combine(id)
        case .aiPracticeUpload(let id, _, let cid, _, _, _, _, _, _, _, _, _, _): hasher.combine(27); hasher.combine(id); hasher.combine(cid)
        case .quizResult(let cid, let uid, let kid): hasher.combine(32); hasher.combine(cid); hasher.combine(uid); hasher.combine(kid)
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

    func push(_ dest: AppDestination) {
        NSLog("[AppRouter.push] dest=\(dest) pathBefore=\(path.count)")
        path.append(dest)
        NSLog("[AppRouter.push] pathAfter=\(path.count)")
    }
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
        .onChange(of: appState.pendingTaskTap?.taskId) { taskId in
            guard let taskId, appState.isAuthenticated else { return }
            appState.pendingTaskTap = nil
            // Both parent-role and child-role notifications land on the parent device
            // (only the parent FCM token is registered on iOS).
            router.push(.parentTaskDetail(taskId: taskId))
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
                onSelect: { child in
                    NSLog("[AppRouter] onSelect → pushing enterChildPin for id=\(child.id) pathCount=\(router.path.count)")
                    router.push(.enterChildPin(child: child))
                    NSLog("[AppRouter] after push pathCount=\(router.path.count)")
                }
            )

        case .enterChildPin(let child):
            let _ = NSLog("[AppRouter] building EnterChildPinView for child=\(child.name)")
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
            let childGrade = Int(appState.children.first { $0.id == childId }?.grade ?? "6") ?? 6
            ChildHomeView(
                childId: childId,
                childName: childName,
                onNavigateToConceptList: { router.push(.conceptList(childId: childId)) },
                onNavigateToPractiseLesson: { router.push(.practiseLessonPicker(childId: childId)) },
                onNavigateToHomeworkLog: { router.push(.homeworkLog(childId: childId)) },
                onNavigateToClassTestPrep: { router.push(.classTestPrep(childId: childId, grade: childGrade)) },
                onNavigateToTuitionWork: {
                    router.push(.tuitionUploadOptions(childId: childId, grade: childGrade, mode: "work"))
                },
                onNavigateToTuitionHomework: {
                    router.push(.tuitionUploadOptions(childId: childId, grade: childGrade, mode: "homework"))
                },
                onNavigateToTuitionProblems: { router.push(.tuitionProblems) },
                onNavigateToRevisitTopics: { router.push(.revisitTopics(childId: childId)) },
                onNavigateToNeedRevision: { router.push(.needRevision(childId: childId)) },
                onNavigateToTestYourself: { router.push(.conceptList(childId: childId)) },
                onNavigateToStreakHome: {
                    router.push(.streakHome(childName: childName.components(separatedBy: " ").first ?? childName))
                },
                onNavigateToParentTask: { taskId in router.push(.doParentTask(taskId: taskId, childId: childId)) },
                onNavigateToParentTasksList: { router.push(.parentTasksList(childId: childId)) },
                onNavigateToEarnReward: {
                    guard let parentId = appState.parentUID else { return }
                    router.push(.earnRewardPicker(childId: childId, grade: childGrade, parentId: parentId))
                },
                onNavigateToQuizResult: { conceptId, unitId in
                    router.push(.quizResult(conceptId: conceptId, unitId: unitId, childId: childId))
                },
                onNavigateToUploadGroup: { groupKey in
                    router.push(.revisitDetail(groupKey: groupKey, childId: childId))
                },
                onRequestParentAccess: { router.popToRoot() }
            )
            .environmentObject(appState)

        case .parentHome:
            ParentHomeView(
                onNavigateToChildHome: { id in
                    let child = appState.children.first { $0.id == id }
                    if let child { router.push(.enterChildPin(child: child)) }
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
                onNavigateToAllTasks: { router.push(.parentActionableTasks) },
                onNavigateToActivityDetail: { categoryKey, childId, childName in
                    router.push(.activityDetail(categoryKey: categoryKey, childId: childId, childName: childName))
                },
                onSignOut: {
                    try? appState.signOut()
                    router.popToRoot()
                }
            )
            .environmentObject(appState)

        case .conceptList(let childId):
            ConceptListView(
                childId: childId,
                mode: .testYourself,
                onBack: { router.pop() },
                onConceptTap: { concept in
                    router.push(.questionSource(
                        conceptId: concept.id, childId: childId,
                        unitName: concept.unitName, conceptName: concept.name,
                        subject: concept.subject, subjectColor: concept.subjectColor,
                        unitId: concept.unitId
                    ))
                },
                onUnitTap: { unit, subjectName, subjectColor in
                    router.push(.questionSource(
                        conceptId: "", childId: childId,
                        unitName: unit.name, conceptName: "",
                        subject: subjectName, subjectColor: subjectColor,
                        unitId: unit.id
                    ))
                }
            )
            .environmentObject(appState)

        case .conceptDetail(let conceptId, let childId):
            ConceptDetailView(
                conceptId: conceptId,
                childId: childId,
                onBack: { router.pop() },
                onNavigateToUploadWork: { kind, unitName, conceptName, subject, subjectColor, subjectIcon, unitId in
                    let grade = Int(appState.children.first { $0.id == childId }?.grade ?? "6") ?? 6
                    router.push(.uploadWork(kind: kind, conceptId: conceptId, childId: childId,
                                            unitName: unitName, conceptName: conceptName,
                                            subject: subject, subjectColor: subjectColor,
                                            subjectIcon: subjectIcon, unitId: unitId, grade: grade))
                },
                onNavigateToAnswerQuestions: { unitName, conceptName, subject, subjectColor, unitId in
                    router.push(.questionSource(conceptId: conceptId, childId: childId,
                                                unitName: unitName, conceptName: conceptName,
                                                subject: subject, subjectColor: subjectColor,
                                                unitId: unitId))
                }
            )

        case .homeworkLog(let childId):
            HomeworkLogView(childId: childId, onBack: { router.pop() }, onSaved: { router.pop() })

        case .streakHome(let childName):
            StreakHomeView(childName: childName, onBack: { router.pop() }, onUploadToday: {})

        case .insights(let childId, let childName, let grade, let board):
            let insChild = appState.children.first { $0.id == childId }
            let insWeek = appState.weekActivities[childId] ?? emptyWeek
            InsightsView(
                childName: childName,
                childGrade: grade,
                childBoard: board,
                childGender: insChild?.gender ?? "male",
                engagement: appState.engagements[childId] ?? .empty,
                weekKinds: insWeek.map { $0.kind },
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

        case .doParentTask(let taskId, let childId):
            DoParentTaskView(
                taskId: taskId,
                childId: childId,
                onBack: { router.pop() },
                onFinished: { router.pop() },
                onLaunchQuiz: { task in
                    // Push QuizView with parentTaskId so it submits score to the task on completion
                    let quizConceptId = task.isUnitScope ? task.unitId : task.conceptId
                    router.push(.quizForParentTask(
                        conceptId: quizConceptId,
                        childId: childId,
                        unitName: task.displayName,
                        subject: task.subject,
                        parentTaskId: taskId
                    ))
                }
            )

        case .parentActionableTasks:
            ParentActionableTasksView(
                onBack: { router.pop() },
                onTaskTap: { taskId in router.push(.parentTaskDetail(taskId: taskId)) },
                onSendReminder: { taskId in appState.sendReminder(taskId: taskId) }
            )
            .environmentObject(appState)

        case .earnRewardPicker(let childId, let grade, let parentId):
            EarnRewardPickerView(
                childId: childId,
                grade: grade,
                parentId: parentId,
                onBack: { router.pop() },
                onRequestSent: { router.pop() }
            )
            .environmentObject(appState)

        case .parentTaskDetail(let taskId):
            ParentTaskDetailView(
                taskId: taskId,
                onBack: { router.pop() },
                onFinished: { router.pop() }
            )

        case .parentTasksList(let childId):
            let pltGrade = Int(appState.children.first { $0.id == childId }?.grade ?? "6") ?? 6
            ParentTasksListView(
                childId: childId,
                onBack: { router.pop() },
                onTaskClick: { taskId in router.push(.doParentTask(taskId: taskId, childId: childId)) },
                onNavigateToEarnReward: {
                    guard let parentId = appState.parentUID else { return }
                    router.push(.earnRewardPicker(childId: childId, grade: pltGrade, parentId: parentId))
                }
            )
            .environmentObject(appState)

        case .uploadWork(let kind, let conceptId, let childId, let unitName, let conceptName, let subject, let subjectColor, let subjectIcon, let unitId, let grade):
            UploadWorkView(
                kind: kind,
                conceptId: conceptId,
                childId: childId,
                unitName: unitName,
                conceptName: conceptName,
                subject: subject,
                subjectColor: subjectColor,
                subjectIcon: subjectIcon,
                unitId: unitId,
                grade: grade,
                onBack: { router.pop() },
                onSaveComplete: { router.pop() }
            )

        case .questionSource(let conceptId, let childId, let unitName, let conceptName, let subject, let subjectColor, let unitId):
            let qsGrade = Int(appState.children.first { $0.id == childId }?.grade ?? "6") ?? 6
            QuestionSourceView(
                conceptId: conceptId,
                childId: childId,
                unitName: unitName,
                conceptName: conceptName,
                subject: subject,
                unitId: unitId,
                onBack: { router.pop() },
                onPickCuratedSet: {
                    router.push(.testYourselfFormat(
                        source: "curated", conceptId: conceptId, unitId: unitId,
                        unitName: unitName, conceptName: conceptName,
                        subject: subject, subjectColor: subjectColor,
                        childId: childId, grade: qsGrade
                    ))
                },
                onPickUploadAndGenerate: {
                    router.push(.testYourselfFormat(
                        source: "ai", conceptId: conceptId, unitId: unitId,
                        unitName: unitName, conceptName: conceptName,
                        subject: subject, subjectColor: subjectColor,
                        childId: childId, grade: qsGrade
                    ))
                }
            )

        case .practiseLessonPicker(let childId):
            ConceptListView(
                childId: childId,
                mode: .practiseLesson,
                onBack: { router.pop() },
                onConceptTap: { concept in
                    router.push(.conceptDetail(conceptId: concept.id, childId: childId))
                },
                onUnitTap: { unit, subjectName, _ in
                    router.push(.unitDetail(unitId: unit.id, childId: childId,
                                            unitName: unit.name, subject: subjectName))
                }
            )
            .environmentObject(appState)

        case .unitDetail(let unitId, let childId, let unitName, let subject):
            let udChild = appState.children.first { $0.id == childId }
            let udGrade = Int(udChild?.grade ?? "6") ?? 6
            UnitDetailView(
                unitId: unitId,
                childId: childId,
                unitName: unitName,
                subject: subject,
                onBack: { router.pop() },
                onNavigateToUploadWork: { kind, subjectColor, subjectIcon in
                    router.push(.uploadWork(kind: kind, conceptId: unitId, childId: childId,
                                            unitName: unitName, conceptName: "",
                                            subject: subject, subjectColor: subjectColor,
                                            subjectIcon: subjectIcon, unitId: unitId, grade: udGrade))
                },
                onNavigateToAnswerQuestions: { subjectColor, _ in
                    router.push(.questionSource(conceptId: unitId, childId: childId,
                                                unitName: unitName, conceptName: "",
                                                subject: subject, subjectColor: subjectColor,
                                                unitId: unitId))
                }
            )
            .environmentObject(appState)

        case .quiz(let conceptId, let childId, let unitName, let subject):
            QuizView(
                conceptId: conceptId,
                childId: childId,
                unitName: unitName,
                subject: subject,
                onBack: { router.pop() }
            )

        case .quizForParentTask(let conceptId, let childId, let unitName, let subject, let parentTaskId):
            QuizView(
                conceptId: conceptId,
                childId: childId,
                unitName: unitName,
                subject: subject,
                parentTaskId: parentTaskId,
                onBack: { router.pop() }
            )

        case .tuitionUploadOptions(let childId, let grade, let mode):
            TuitionUploadOptionsView(
                mode: mode,
                childId: childId,
                grade: grade,
                onBack: { router.pop() },
                onSelectKind: { kind in
                    router.push(.tuitionLearning(childId: childId, grade: grade, kind: kind))
                }
            )

        case .tuitionLearning(let childId, let grade, let kind):
            TuitionLearningView(
                kind: kind,
                childId: childId,
                grade: grade,
                onBack: { router.pop() },
                onSaved: { router.pop() }
            )

        case .tuitionProblems:
            TuitionProblemsView(onBack: { router.pop() })

        case .revisitTopics(let childId):
            RevisitTopicsView(
                childId: childId,
                onBack: { router.pop() },
                onTopicClick: { groupKey, title, subject, subjectColor, subjectIcon, unitId, conceptId in
                    router.push(.revisitDetail(groupKey: groupKey, childId: childId))
                }
            )

        case .revisitDetail(let groupKey, let childId):
            RevisitDetailView(
                groupKey: groupKey,
                childId: childId,
                onBack: { router.pop() },
                onTakeQuiz: { unitId, conceptId, unitName, subject in
                    let quizId = conceptId.isEmpty ? unitId : conceptId
                    router.push(.quiz(conceptId: quizId, childId: childId,
                                      unitName: unitName, subject: subject))
                },
                onViewLastResult: { unitId, conceptId, unitName, subject in
                    router.push(.quizResult(conceptId: conceptId, unitId: unitId, childId: childId))
                }
            )

        case .needRevision(let childId):
            NeedRevisionView(
                childId: childId,
                onBack: { router.pop() },
                onOpenItem: { mark in
                    if mark.isUnit {
                        router.push(.unitDetail(unitId: mark.targetId, childId: childId,
                                                unitName: mark.name, subject: mark.subject))
                    } else {
                        router.push(.conceptDetail(conceptId: mark.targetId, childId: childId))
                    }
                }
            )

        case .testYourselfFormat(let source, let conceptId, let unitId, let unitName, let conceptName, let subject, let subjectColor, let childId, let grade):
            let isUnitScope = conceptId.isEmpty
            let displayName = isUnitScope ? unitName : (conceptName.isEmpty ? unitName : conceptName)
            let scopeText = isUnitScope
                ? "Lesson: \(unitName)"
                : "Lesson: \(unitName)\nConcept (focus narrowly on this): \(displayName)"
            TestYourselfFormatView(
                source: source,
                conceptId: conceptId,
                unitId: unitId,
                unitName: unitName,
                conceptName: conceptName,
                subject: subject,
                subjectColor: subjectColor,
                childId: childId,
                grade: grade,
                onBack: { router.pop() },
                onPickMcq: {
                    if source == "ai" {
                        let uploadId = conceptId.isEmpty ? unitId : conceptId
                        router.push(.uploadWork(kind: "self_learning", conceptId: uploadId,
                                                childId: childId, unitName: unitName, conceptName: conceptName,
                                                subject: subject, subjectColor: subjectColor, subjectIcon: "",
                                                unitId: unitId, grade: grade))
                    } else {
                        let quizId = conceptId.isEmpty ? unitId : conceptId
                        router.push(.quiz(conceptId: quizId, childId: childId,
                                          unitName: unitName, subject: subject))
                    }
                },
                onPickSubjective: {
                    router.push(.writeAnswer(displayName: displayName, subject: subject,
                                             grade: grade, scopeText: scopeText, childId: childId))
                }
            )

        case .writeAnswer(let displayName, let subject, let grade, let scopeText, let childId):
            WriteAnswerView(
                displayName: displayName,
                subject: subject,
                grade: grade,
                scopeText: scopeText,
                childId: childId,
                canChangeMode: true,
                onBack: { router.pop() },
                onLaunchAntiCheatQuiz: { cacheKey, name in
                    router.push(.quiz(conceptId: cacheKey, childId: childId,
                                      unitName: name, subject: subject))
                }
            )

        case .aiPracticeUpload(let childId, let grade, let conceptId, let unitId, let displayName, let conceptName, let subject, let subjectColor, let subjectIcon, let teacherHonorific, let teacherName, let testDateInterval, let scopeText):
            UploadWorkView(
                kind: "ai_test_prep",
                conceptId: conceptId,
                childId: childId,
                unitName: displayName,
                conceptName: conceptName,
                subject: subject,
                subjectColor: subjectColor,
                subjectIcon: subjectIcon,
                unitId: unitId,
                grade: grade,
                scopeText: scopeText,
                initialTeacherHonorific: teacherHonorific,
                initialTeacherName: teacherName,
                initialDate: Date(timeIntervalSince1970: testDateInterval),
                onBack: { router.pop() },
                onSaveComplete: { router.pop() },
                onQuizReady: { cacheKey, name in
                    router.push(.quiz(conceptId: cacheKey, childId: childId,
                                      unitName: name, subject: subject))
                }
            )

        case .classTestPrep(let childId, let grade):
            ClassTestPrepView(
                childId: childId,
                grade: grade,
                onBack: { router.pop() },
                onLaunchMcq: { conceptId, unitName, subject in
                    router.push(.quiz(conceptId: conceptId, childId: childId,
                                      unitName: unitName, subject: subject))
                },
                onLaunchWriteAnswers: { displayName, subject, itemGrade, scopeText in
                    router.push(.writeAnswer(displayName: displayName, subject: subject,
                                             grade: itemGrade, scopeText: scopeText,
                                             childId: childId))
                },
                onLaunchAiPractice: { conceptId, unitId, unitName, conceptName, subject, subjectColor, subjectIcon, teacherHonorific, teacherName, testDate, scopeText in
                    router.push(.aiPracticeUpload(
                        childId: childId, grade: grade,
                        conceptId: conceptId, unitId: unitId,
                        displayName: unitName, conceptName: conceptName,
                        subject: subject, subjectColor: subjectColor, subjectIcon: subjectIcon,
                        teacherHonorific: teacherHonorific, teacherName: teacherName,
                        testDateInterval: testDate.timeIntervalSince1970,
                        scopeText: scopeText
                    ))
                }
            )

        case .quizResult(let conceptId, let unitId, let childId):
            QuizResultLoaderView(
                conceptId: conceptId, unitId: unitId, childId: childId,
                onBack: { router.pop() },
                onRetake: { unitName, subject in
                    let quizId = conceptId.isEmpty ? unitId : conceptId
                    router.push(.quiz(conceptId: quizId, childId: childId,
                                      unitName: unitName, subject: subject))
                }
            )
        }
    }
}

// ── Quiz result loader ─────────────────────────────────────────────────────────

private struct QuizResultLoaderView: View {
    let conceptId: String
    let unitId: String
    let childId: String
    var onBack: () -> Void = {}
    var onRetake: ((_ unitName: String, _ subject: String) -> Void)? = nil

    @State private var result: ADQuizResult? = nil
    @State private var loadFinished = false

    var body: some View {
        Group {
            if let r = result {
                QuizResultView(
                    result: r,
                    onRetake: onRetake.map { cb in { cb(r.displayName, r.subject) } },
                    onBack: onBack
                )
            } else {
                VStack(spacing: 0) {
                    HStack {
                        Button(action: onBack) {
                            Image(systemName: "arrow.left")
                                .font(.system(size: 17, weight: .semibold))
                                .foregroundStyle(Color.dobiTextPrimary)
                                .frame(width: 36, height: 36)
                                .background(Color.white)
                                .clipShape(Circle())
                                .shadow(color: .black.opacity(0.08), radius: 4, x: 0, y: 2)
                        }
                        Spacer()
                        Text("Quiz Result")
                            .font(.system(size: 17, weight: .bold))
                            .foregroundStyle(Color.dobiTextPrimary)
                        Spacer()
                        Color.clear.frame(width: 36, height: 36)
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                    .background(Color.white)

                    Spacer()
                    if loadFinished {
                        VStack(spacing: 12) {
                            Text("Couldn't load result")
                                .font(.system(size: 17, weight: .bold))
                                .foregroundStyle(Color.dobiTextPrimary)
                            Text("No saved result found for this quiz.")
                                .font(.system(size: 14))
                                .foregroundStyle(Color.dobiTextSecondary)
                            Button("Go back", action: onBack)
                                .font(.system(size: 14, weight: .semibold))
                                .foregroundStyle(Color.dobiPrimary)
                        }
                    } else {
                        ProgressView().scaleEffect(1.2).tint(Color.dobiPrimary)
                    }
                    Spacer()
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(Color.white.ignoresSafeArea())
                .navigationBarHidden(true)
            }
        }
        .task { await load() }
    }

    private func load() async {
        guard var r = await FirebaseService.shared.fetchQuizResult(
            childId: childId, unitId: unitId, conceptId: conceptId
        ) else {
            await MainActor.run { self.loadFinished = true }
            return
        }
        // Android always reloads questions from the curated bank for child view.
        // When lastQuestions is missing (quiz taken before the save fix), inject
        // curated questions so the breakdown and Retake button both work.
        if r.questions.isEmpty, !conceptId.isEmpty {
            let curated = CuratedQuestionBank.forConcept(conceptId)
            if !curated.isEmpty {
                let adQuestions = curated.map { q in
                    ADMcqQuestion(text: q.text, options: q.options,
                                  correctIndex: q.correctIndex, explanation: q.explanation)
                }
                r = ADQuizResult(displayName: r.displayName, subject: r.subject,
                                 correct: r.correct, total: r.total, attempts: r.attempts,
                                 questions: adQuestions, selections: r.selections)
            }
        }
        await MainActor.run {
            self.result = r
            self.loadFinished = true
        }
    }
}
