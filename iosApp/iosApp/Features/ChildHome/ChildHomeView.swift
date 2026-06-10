import SwiftUI

struct ChildHomeView: View {
    let childId: String
    let childName: String
    var onNavigateToConceptList: () -> Void = {}
    var onNavigateToPractiseLesson: () -> Void = {}
    var onNavigateToHomeworkLog: () -> Void = {}
    var onNavigateToClassTestPrep: () -> Void = {}
    var onNavigateToTuitionWork: () -> Void = {}
    var onNavigateToTuitionHomework: () -> Void = {}
    var onNavigateToTuitionProblems: () -> Void = {}
    var onNavigateToRevisitTopics: () -> Void = {}
    var onNavigateToNeedRevision: () -> Void = {}
    var onNavigateToTestYourself: () -> Void = {}
    var onNavigateToStreakHome: () -> Void = {}
    var onNavigateToParentTask: (String) -> Void = { _ in }
    var onNavigateToParentTasksList: () -> Void = {}
    var onNavigateToEarnReward: () -> Void = {}
    var onNavigateToQuizResult: (_ conceptId: String, _ unitId: String) -> Void = { _, _ in }
    var onNavigateToUploadGroup: (_ groupKey: String) -> Void = { _ in }
    var onRequestParentAccess: () -> Void = {}

    @EnvironmentObject private var appState: AppState
    @State private var selectedTab = 0
    @State private var selectedModeIndex: Int? = nil
    @State private var tutorHonorific: String = UserDefaults.standard.string(forKey: "tutor_honorific") ?? ""
    @State private var tutorName: String = UserDefaults.standard.string(forKey: "tutor_name") ?? ""
    @State private var childShareItem: ShareItem? = nil
    @State private var isInviteLoading = false

    private var myTasks: [ParentTaskItem] {
        appState.parentTasks.filter { $0.childId == childId }
    }

    private var streakDays: Int {
        appState.engagements[childId]?.streakDays ?? 0
    }

    var body: some View {
        ZStack(alignment: .bottom) {
            // ZStack-based tab container — TabView.page has a SwiftUI bug where
            // programmatic selection binding changes are silently dropped on iOS 16+.
            ZStack {
                ChildDashboardTab(
                    childName: childName,
                    parentTasks: myTasks,
                    streakDays: streakDays,
                    selectedModeIndex: $selectedModeIndex,
                    onConceptList: onNavigateToConceptList,
                    onHomework: onNavigateToHomeworkLog,
                    onClassTest: onNavigateToClassTestPrep,
                    onStreak: onNavigateToStreakHome,
                    onParentTask: onNavigateToParentTask,
                    onSeeAllTasks: onNavigateToParentTasksList,
                    onEarnReward: onNavigateToEarnReward,
                    onInviteFriend: triggerChildInvite
                )
                .opacity(selectedTab == 0 ? 1 : 0)
                .allowsHitTesting(selectedTab == 0)

                MyLearningsTab(
                    childId: childId,
                    onNavigateToQuizResult: onNavigateToQuizResult,
                    onNavigateToParentTask: onNavigateToParentTask,
                    onNavigateToUploadGroup: onNavigateToUploadGroup
                )
                .opacity(selectedTab == 1 ? 1 : 0)
                .allowsHitTesting(selectedTab == 1)

                ChildProfileTab(
                    childId: childId,
                    childName: childName,
                    onSwitchToParent: onRequestParentAccess,
                    onInviteFriend: triggerChildInvite
                )
                .opacity(selectedTab == 2 ? 1 : 0)
                .allowsHitTesting(selectedTab == 2)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)

            if selectedModeIndex == nil {
                ChildBottomNav(selected: $selectedTab)
                    .transition(.move(edge: .bottom))
            }
        }
        // Revision popup rendered as overlay so it's centered above everything
        .overlay {
            if let mode = selectedModeIndex {
                ZStack {
                    Color.black.opacity(0.45)
                        .ignoresSafeArea()
                        .onTapGesture { withAnimation(.spring(duration: 0.25)) { selectedModeIndex = nil } }

                    ChildRevisionDialog(
                        modeIndex: mode,
                        onDismiss: { withAnimation(.spring(duration: 0.25)) { selectedModeIndex = nil } },
                        onOptionClick: { optionIndex in
                            withAnimation { selectedModeIndex = nil }
                            switch (mode, optionIndex) {
                            case (0, 0): onNavigateToPractiseLesson()
                            case (0, 1): onNavigateToHomeworkLog()
                            case (0, 2): onNavigateToClassTestPrep()
                            case (1, 0): onNavigateToTuitionWork()
                            case (1, 1): onNavigateToTuitionHomework()
                            case (1, 2): onNavigateToTuitionProblems()
                            case (2, 0): onNavigateToRevisitTopics()
                            case (2, 1): onNavigateToNeedRevision()
                            case (2, 2): onNavigateToTestYourself()
                            default: break
                            }
                        },
                        tutorHonorific: tutorHonorific,
                        tutorName: tutorName,
                        onTutorSave: { h, n in
                            tutorHonorific = h
                            tutorName = n
                            UserDefaults.standard.set(h, forKey: "tutor_honorific")
                            UserDefaults.standard.set(n, forKey: "tutor_name")
                        }
                    )
                    .padding(.horizontal, 24)
                    .transition(.scale(scale: 0.85).combined(with: .opacity))
                }
            }
        }
        .animation(.spring(duration: 0.3), value: selectedModeIndex == nil)
        .navigationBarHidden(true)
        .ignoresSafeArea(edges: .bottom)
        .sheet(item: $childShareItem) { item in
            ShareSheet(items: [item.text])
        }
        .task(id: childId) {
            await InviteService.shared.tryClaimPendingInvite(childId: childId)
        }
    }

    private func triggerChildInvite() {
        guard !isInviteLoading else { return }
        isInviteLoading = true
        Task {
            let text = await InviteService.shared.createChildInviteText(
                childId: childId,
                childName: childName
            )
            isInviteLoading = false
            if let text { childShareItem = ShareItem(text: text) }
        }
    }
}

// MARK: - Bottom nav

private struct ChildBottomNav: View {
    @Binding var selected: Int
    private let items: [(icon: String, label: String)] = [
        ("house.fill", "Home"),
        ("book.fill", "My Learnings"),
        ("person.fill", "Profile"),
    ]

    var body: some View {
        HStack(spacing: 0) {
            ForEach(items.indices, id: \.self) { i in
                Button {
                    selected = i
                } label: {
                    VStack(spacing: 4) {
                        Image(systemName: items[i].icon)
                            .font(.system(size: 20, weight: selected == i ? .semibold : .regular))
                            .foregroundStyle(selected == i ? Color.dobiPrimary : Color.dobiTextTertiary)
                            .scaleEffect(selected == i ? 1.1 : 1)
                            .animation(.spring(duration: 0.2), value: selected)
                        Text(items[i].label)
                            .font(.dobiSmall())
                            .foregroundStyle(selected == i ? Color.dobiPrimary : Color.dobiTextTertiary)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 16)
                }
                .buttonStyle(.plain)
            }
        }
        .background(
            Color.white
                .shadow(color: .black.opacity(0.08), radius: 12, x: 0, y: -4)
        )
        .ignoresSafeArea(edges: .bottom)
    }
}

// MARK: - Dashboard tab

private struct ChildDashboardTab: View {
    let childName: String
    let parentTasks: [ParentTaskItem]
    let streakDays: Int
    @Binding var selectedModeIndex: Int?
    var onConceptList: () -> Void
    var onHomework: () -> Void
    var onClassTest: () -> Void
    var onStreak: () -> Void
    var onParentTask: (String) -> Void
    var onSeeAllTasks: () -> Void
    var onEarnReward: () -> Void = {}
    var onInviteFriend: () -> Void = {}

    private var firstName: String { childName.components(separatedBy: " ").first ?? childName }
    private var assignedTasks: [ParentTaskItem] { parentTasks.filter { $0.status == "ASSIGNED" } }
    private var pendingTasks: [ParentTaskItem] { parentTasks.filter { ["ASSIGNED", "PENDING_REVIEW", "REJECTED"].contains($0.status) } }
    private var requestedTasks: [ParentTaskItem] { parentTasks.filter { $0.status == "REQUESTED" } }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                // Greeting
                VStack(alignment: .leading, spacing: 4) {
                    Text("Hi, \(firstName)! 👋")
                        .font(.system(size: 22, weight: .bold))
                        .foregroundStyle(Color.dobiTextPrimary)
                    Text("Keep up the great learning!")
                        .font(.dobiBody())
                        .foregroundStyle(Color.dobiTextSecondary)
                }
                .padding(.horizontal, 20)
                .padding(.top, 28)

                // Pending parent task bar — shows above streak when ASSIGNED tasks exist
                if !assignedTasks.isEmpty {
                    ChildPendingTaskBar(tasks: assignedTasks, onClick: onParentTask)
                        .padding(.horizontal, 20)
                        .padding(.top, 20)
                }

                // Streak banner
                ChildStreakBanner(
                    streakDays: streakDays,
                    onInviteFriend: onInviteFriend,
                    onViewDashboard: onStreak
                )
                .padding(.horizontal, 20)
                .padding(.top, 20)

                // Revision mode row
                ChildRevisionModeRow(onRevisionModeClick: { idx in
                    withAnimation(.spring(duration: 0.3)) { selectedModeIndex = idx }
                })
                .padding(.horizontal, 20)
                .padding(.top, 20)

                // Start Learning CTA
                Button(action: onConceptList) {
                    HStack(spacing: 8) {
                        Image(systemName: "play.fill")
                            .font(.system(size: 16, weight: .medium))
                        Text("Start Learning")
                            .font(.system(size: 15, weight: .semibold))
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 52)
                    .background(Color.dobiPrimary)
                    .foregroundStyle(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }
                .buttonStyle(.plain)
                .padding(.horizontal, 20)
                .padding(.top, 20)

                // Tasks from parents section
                HStack {
                    Text("Tasks from Parents")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(Color.dobiTextPrimary)
                    Spacer()
                    if !parentTasks.isEmpty {
                        Text("See all")
                            .font(.dobiBody())
                            .foregroundStyle(Color.dobiPrimary)
                            .onTapGesture { onSeeAllTasks() }
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 28)

                Group {
                    if !pendingTasks.isEmpty {
                        ChildTasksSummaryBanner(tasks: pendingTasks, onTaskClick: onParentTask, onSeeAll: onSeeAllTasks)
                    } else if !requestedTasks.isEmpty {
                        ChildTasksSummaryBanner(tasks: requestedTasks, onTaskClick: onParentTask, onSeeAll: onSeeAllTasks)
                    } else {
                        ChildTasksEmptyCard(onClick: onEarnReward)
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 12)

                Spacer().frame(height: 100)
            }
            .frame(maxWidth: .infinity)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.dobiPageBg)
    }
}

// MARK: - Pending task bar (ASSIGNED tasks above streak)

private func childTaskTitle(_ task: ParentTaskItem) -> String {
    let icon  = task.subjectIcon.isEmpty ? "📚" : task.subjectIcon
    let scope = task.displayName.isEmpty ? "your topic" : task.displayName
    let title: String
    if task.status == "REQUESTED" {
        title = "Waiting for parent: \(scope)"
    } else {
        switch task.type {
        case "REVISE_CHAPTER": title = "Revise: \(scope)"
        case "WRITE_QA":       title = "Write Q&A: \(scope)"
        case "OUR_QUESTIONS":  title = "\(task.mcqCount) questions: \(scope)"
        case "CUSTOM":         title = "Task on: \(scope)"
        default:               title = scope
        }
    }
    return "\(icon) \(title)"
}

private struct ChildPendingTaskBar: View {
    let tasks: [ParentTaskItem]  // non-empty guaranteed by caller
    let onClick: (String) -> Void

    private var mostRecent: ParentTaskItem {
        tasks.max(by: { $0.assignedAtMillis < $1.assignedAtMillis }) ?? tasks[0]
    }
    private var moreCount: Int { max(0, tasks.count - 1) }

    var body: some View {
        DobiCard(cornerRadius: 18) {
            VStack(alignment: .leading, spacing: 14) {
                HStack {
                    Text("Tasks needing your attention")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundStyle(Color.dobiTextPrimary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    if moreCount > 0 {
                        HStack(spacing: 2) {
                            Text("View all")
                                .font(.system(size: 13, weight: .semibold))
                                .foregroundStyle(Color.dobiPrimary)
                            Image(systemName: "chevron.right")
                                .font(.system(size: 11, weight: .semibold))
                                .foregroundStyle(Color.dobiPrimary)
                        }
                        .onTapGesture { onClick(mostRecent.id) }
                    }
                }

                HStack(spacing: 12) {
                    ZStack {
                        Circle()
                            .fill(Color(hex: 0xFFF4E0))
                            .frame(width: 48, height: 48)
                        Text(mostRecent.subjectIcon.isEmpty ? "📚" : mostRecent.subjectIcon)
                            .font(.system(size: 24))
                    }

                    VStack(alignment: .leading, spacing: 2) {
                        Text(childTaskTitle(mostRecent))
                            .font(.system(size: 14, weight: .bold))
                            .foregroundStyle(Color.dobiTextPrimary)
                            .lineLimit(2)
                        Text((mostRecent.isUnitScope ? "Lesson" : "Concept") +
                             (mostRecent.subject.isEmpty ? "" : " · \(mostRecent.subject)"))
                            .font(.system(size: 12))
                            .foregroundStyle(Color.dobiTextSecondary)
                    }

                    Spacer()

                    Text("New task")
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundStyle(Color(hex: 0xB45309))
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(Color(hex: 0xFFF4E0))
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                }
                .contentShape(Rectangle())
                .onTapGesture { onClick(mostRecent.id) }
            }
            .padding(16)
        }
    }
}

// MARK: - Streak banner

private struct ChildStreakBanner: View {
    let streakDays: Int
    let onInviteFriend: () -> Void
    let onViewDashboard: () -> Void

    private let skyTop = Color(hex: 0xE7F1FF)
    private let skyBottom = Color(hex: 0xCFE2FF)
    private let accentBlue = Color(hex: 0x2F7DFF)
    private let titleNavy = Color(hex: 0x0F2A52)
    private let subtleBorder = Color(hex: 0xB9D4FF)

    private var todayIdx: Int {
        // Swift Calendar weekday: Sun=1, Mon=2, ..., Sat=7 → Mon=0...Sun=6
        let weekday = Calendar.current.component(.weekday, from: Date())
        return (weekday + 5) % 7
    }

    var body: some View {
        ZStack(alignment: .topTrailing) {
            LinearGradient(colors: [skyTop, skyBottom], startPoint: .top, endPoint: .bottom)
                .clipShape(RoundedRectangle(cornerRadius: 22))

            Text("🔥")
                .font(.system(size: 100))
                .opacity(0.10)
                .offset(x: 18, y: -22)

            VStack(alignment: .leading, spacing: 0) {
                // Streak header
                HStack(spacing: 12) {
                    ZStack {
                        Circle()
                            .fill(Color.white)
                            .frame(width: 46, height: 46)
                        Circle()
                            .strokeBorder(accentBlue.opacity(0.35), lineWidth: 2)
                            .frame(width: 46, height: 46)
                        Text("🔥").font(.system(size: 22))
                    }

                    VStack(alignment: .leading, spacing: 2) {
                        Text(streakDays > 0 ? "\(streakDays)-Day Streak" : "Start Your Streak!")
                            .font(.system(size: streakDays > 0 ? 22 : 20, weight: .bold))
                            .foregroundStyle(titleNavy)
                        Text(streakDays > 0
                             ? "You're on fire! Keep it going. 🔥"
                             : "Upload today's chapter to light up day 1 🔥")
                            .font(.system(size: 12))
                            .foregroundStyle(titleNavy.opacity(0.65))
                    }
                    Spacer()
                }
                .padding(.bottom, 14)

                // 7-day progress dots
                HStack(spacing: 6) {
                    let labels = ["M", "T", "W", "T", "F", "S", "S"]
                    let activeCount = min(streakDays, 7)
                    ForEach(0..<7, id: \.self) { idx in
                        let active = idx <= todayIdx && (todayIdx - idx) < activeCount
                        VStack(spacing: 4) {
                            ZStack {
                                Circle()
                                    .fill(Color.white)
                                    .frame(width: 28, height: 28)
                                if !active {
                                    Circle()
                                        .strokeBorder(subtleBorder, lineWidth: 1)
                                        .frame(width: 28, height: 28)
                                }
                                Text("🔥")
                                    .font(.system(size: 14))
                                    .opacity(active ? 1.0 : 0.28)
                            }
                            Text(labels[idx])
                                .font(.system(size: 10, weight: .semibold))
                                .foregroundStyle(titleNavy.opacity(active ? 0.9 : 0.45))
                        }
                        .frame(maxWidth: .infinity)
                    }
                }
                .padding(.bottom, 14)

                // CTAs
                HStack(spacing: 8) {
                    StreakCTAButton(emoji: "👥", label: "Invite Friend", accent: accentBlue, action: onInviteFriend)
                    StreakCTAButton(emoji: "📈", label: "Dashboard", accent: accentBlue, action: onViewDashboard)
                }
            }
            .padding(16)
        }
        .clipShape(RoundedRectangle(cornerRadius: 22))
        .overlay(RoundedRectangle(cornerRadius: 22).stroke(subtleBorder, lineWidth: 1))
        .shadow(color: accentBlue.opacity(0.15), radius: 10, x: 0, y: 4)
    }
}

private struct StreakCTAButton: View {
    let emoji: String
    let label: String
    let accent: Color
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 6) {
                Text(emoji).font(.system(size: 16))
                Text(label)
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(accent)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 11)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Revision mode row + cards

private struct RevisionModeData {
    let emoji: String
    let title: String
    let subtitle: String
    let cardColor: Color
    let iconBg: Color
    let accentColor: Color
}

private let childRevisionModes: [RevisionModeData] = [
    .init(emoji: "🏫", title: "School\nWork",  subtitle: "Revise from class",  cardColor: Color(hex: 0xDBEAFE), iconBg: Color(hex: 0xBFD7F7), accentColor: Color(hex: 0x2563EB)),
    .init(emoji: "📐", title: "Tuition\nWork", subtitle: "Revise from tutor",  cardColor: Color(hex: 0xEDE9FE), iconBg: Color(hex: 0xDDD6FE), accentColor: Color(hex: 0x7C3AED)),
    .init(emoji: "✨", title: "Self\nRevision", subtitle: "Study on your own", cardColor: Color(hex: 0xF0FDF4), iconBg: Color(hex: 0xDCFCE7), accentColor: Color(hex: 0x16A34A)),
]

private struct ChildRevisionModeRow: View {
    let onRevisionModeClick: (Int) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("What would you like to revise?")
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(Color.dobiTextPrimary)
            HStack(spacing: 10) {
                ForEach(childRevisionModes.indices, id: \.self) { i in
                    ChildRevisionCard(mode: childRevisionModes[i], onClick: { onRevisionModeClick(i) })
                }
            }
        }
    }
}

private struct ChildRevisionCard: View {
    let mode: RevisionModeData
    let onClick: () -> Void

    var body: some View {
        Button(action: onClick) {
            VStack(spacing: 8) {
                ZStack {
                    RoundedRectangle(cornerRadius: 14)
                        .fill(mode.iconBg)
                        .frame(width: 44, height: 44)
                    Text(mode.emoji).font(.system(size: 22))
                }
                Text(mode.title)
                    .font(.system(size: 11, weight: .bold))
                    .foregroundStyle(mode.accentColor)
                    .multilineTextAlignment(.center)
                    .lineSpacing(2)
                Text(mode.subtitle)
                    .font(.system(size: 10))
                    .foregroundStyle(Color.dobiTextSecondary)
                    .multilineTextAlignment(.center)
            }
            .padding(.horizontal, 10)
            .padding(.vertical, 14)
            .frame(maxWidth: .infinity)
            .background(mode.cardColor)
            .clipShape(RoundedRectangle(cornerRadius: 20))
            .shadow(color: mode.accentColor.opacity(0.18), radius: 6, x: 0, y: 2)
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Revision dialog popup

private struct RevisionSheetData {
    let title: String
    let emoji: String
    let options: [(emoji: String, title: String, subtitle: String)]
}

private let childRevisionSheets: [RevisionSheetData] = [
    .init(title: "School Work", emoji: "🏫", options: [
        ("📖", "Practise a Lesson",     "Go through a school lesson step by step"),
        ("📝", "Log Homework",           "Mark today's homework as completed"),
        ("📋", "Prep for a Class Test",  "Review and get ready for an upcoming test"),
    ]),
    .init(title: "Tuition Work", emoji: "📐", options: [
        ("📚", "Upload Tuition Work",     "Show what you did in today's class"),
        ("📝", "Upload Tuition Homework", "Share homework given by your tutor"),
        ("🤔", "Tuition Problems",        "Share your problems with your parents"),
    ]),
    .init(title: "Self Revision", emoji: "✨", options: [
        ("🔁", "Revisit Past Topics", "Go back to topics you've completed and strengthen understanding"),
        ("⚠️", "Need Revision",       "Topics you might be forgetting — stay sharp and confident"),
        ("🧠", "Test Yourself",       "Practice with quizzes or AI questions from your textbook"),
    ]),
]

private struct ChildRevisionDialog: View {
    let modeIndex: Int
    let onDismiss: () -> Void
    let onOptionClick: (Int) -> Void
    // Tuition mode only
    var tutorHonorific: String = ""
    var tutorName: String = ""
    var onTutorSave: (String, String) -> Void = { _, _ in }

    var body: some View {
        let sheet = childRevisionSheets[modeIndex]
        let mode = childRevisionModes[modeIndex]

        VStack(alignment: .leading, spacing: 0) {
            // Header
            HStack(spacing: 14) {
                ZStack {
                    RoundedRectangle(cornerRadius: 16)
                        .fill(mode.iconBg)
                        .frame(width: 48, height: 48)
                    Text(sheet.emoji).font(.system(size: 24))
                }
                VStack(alignment: .leading, spacing: 2) {
                    Text(sheet.title)
                        .font(.system(size: 16, weight: .bold))
                        .foregroundStyle(Color.dobiTextPrimary)
                    Text("What would you like to do?")
                        .font(.dobiCaption())
                        .foregroundStyle(Color.dobiTextSecondary)
                }
                Spacer()
            }
            .padding(.bottom, modeIndex == 1 ? 14 : 20)

            // Tutor editor — Tuition Work dialog only (modeIndex == 1)
            if modeIndex == 1 {
                CHITutorEditor(
                    accent: mode.accentColor,
                    iconBg: mode.iconBg,
                    initialHonorific: tutorHonorific,
                    initialName: tutorName,
                    onSave: onTutorSave
                )
                .padding(.bottom, 20)
            }

            ForEach(sheet.options.indices, id: \.self) { i in
                let opt = sheet.options[i]
                Button { onOptionClick(i) } label: {
                    HStack(spacing: 14) {
                        ZStack {
                            RoundedRectangle(cornerRadius: 14)
                                .fill(mode.iconBg)
                                .frame(width: 46, height: 46)
                            Text(opt.emoji).font(.system(size: 22))
                        }
                        VStack(alignment: .leading, spacing: 2) {
                            Text(opt.title)
                                .font(.system(size: 14, weight: .semibold))
                                .foregroundStyle(Color.dobiTextPrimary)
                            Text(opt.subtitle)
                                .font(.system(size: 12))
                                .foregroundStyle(Color.dobiTextSecondary)
                                .lineLimit(2)
                        }
                        Spacer()
                        Image(systemName: "chevron.right")
                            .font(.system(size: 14, weight: .medium))
                            .foregroundStyle(mode.accentColor)
                    }
                    .padding(16)
                    .background(Color.white)
                    .clipShape(RoundedRectangle(cornerRadius: 18))
                    .shadow(color: mode.accentColor.opacity(0.10), radius: 4, x: 0, y: 2)
                }
                .buttonStyle(.plain)
                .padding(.bottom, 10)
            }

            Button(action: onDismiss) {
                Text("Cancel")
                    .font(.system(size: 14, weight: .medium))
                    .foregroundStyle(Color.dobiTextSecondary)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 10)
            }
            .buttonStyle(.plain)
        }
        .padding(24)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 28))
    }
}

// MARK: - Tutor editor (Tuition Work dialog — mirrors Android TutorEditor)

private struct CHITutorEditor: View {
    let accent: Color
    let iconBg: Color
    let initialHonorific: String
    let initialName: String
    let onSave: (String, String) -> Void

    @State private var editing: Bool = false
    @State private var honorific: String = "Sir"
    @State private var name: String = ""

    private var containerBg: Color { iconBg.opacity(0.6) }

    var body: some View {
        Group {
            if !editing {
                savedView
            } else {
                editingView
            }
        }
        .onAppear {
            editing = initialName.isEmpty
            honorific = initialHonorific.isEmpty ? "Sir" : initialHonorific
            name = initialName
        }
    }

    // Saved chip — shows honorific + name + pencil icon, tapping enters editing
    private var savedView: some View {
        Button(action: { withAnimation(.spring(duration: 0.2)) { editing = true } }) {
            HStack(spacing: 10) {
                Text("👨‍🏫").font(.system(size: 22))
                VStack(alignment: .leading, spacing: 2) {
                    Text("Your tutor")
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundStyle(accent)
                    Text(name.isEmpty ? "Add tutor's name" : "\(honorific) \(name)".trimmingCharacters(in: .whitespaces))
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(name.isEmpty ? Color.dobiTextSecondary : Color.dobiTextPrimary)
                }
                Spacer()
                Image(systemName: "pencil")
                    .font(.system(size: 16))
                    .foregroundStyle(accent)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .frame(maxWidth: .infinity)
            .background(containerBg)
            .clipShape(RoundedRectangle(cornerRadius: 14))
        }
        .buttonStyle(.plain)
    }

    // Editing form — "Sir / Madam" chips + name field + Save / Cancel
    private var editingView: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: 10) {
                Text("👨‍🏫").font(.system(size: 22))
                VStack(alignment: .leading, spacing: 2) {
                    Text(initialName.isEmpty ? "Who's your tutor?" : "Update your tutor")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundStyle(Color.dobiTextPrimary)
                    Text("We'll remember this for you")
                        .font(.system(size: 11))
                        .foregroundStyle(Color.dobiTextSecondary)
                }
                Spacer()
            }

            // Sir / Madam chips
            HStack(spacing: 8) {
                ForEach(["Sir", "Madam"], id: \.self) { h in
                    let selected = honorific == h
                    Button(action: { honorific = h }) {
                        Text(h)
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundStyle(selected ? .white : accent)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 8)
                            .background(selected ? accent : Color.white)
                            .overlay(
                                RoundedRectangle(cornerRadius: 10)
                                    .stroke(accent.opacity(selected ? 1 : 0.3), lineWidth: 1)
                            )
                            .clipShape(RoundedRectangle(cornerRadius: 10))
                    }
                    .buttonStyle(.plain)
                    .animation(.spring(duration: 0.2), value: selected)
                }
            }
            .padding(.top, 12)

            // Name field
            TextField(text: $name, prompt: .dobiPlaceholder("Tutor's name (e.g. Sharma)")) {}
                .font(.system(size: 14))
                .foregroundStyle(Color.dobiTextPrimary)
                .autocorrectionDisabled()
                .onChange(of: name) { val in if val.count > 40 { name = String(val.prefix(40)) } }
                .padding(.horizontal, 14)
                .padding(.vertical, 12)
                .background(Color.white)
                .overlay(
                    RoundedRectangle(cornerRadius: 10)
                        .stroke(accent.opacity(0.3), lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: 10))
                .padding(.top, 10)

            // Save / Cancel row
            HStack(spacing: 10) {
                if !initialName.isEmpty {
                    Button(action: { withAnimation(.spring(duration: 0.2)) { editing = false } }) {
                        Text("Cancel")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundStyle(accent)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 10)
                            .background(Color.white)
                            .overlay(
                                RoundedRectangle(cornerRadius: 10)
                                    .stroke(accent.opacity(0.3), lineWidth: 1)
                            )
                            .clipShape(RoundedRectangle(cornerRadius: 10))
                    }
                    .buttonStyle(.plain)
                }
                let canSaveLocal = !name.trimmingCharacters(in: .whitespaces).isEmpty
                Button(action: {
                    guard canSaveLocal else { return }
                    onSave(honorific, name.trimmingCharacters(in: .whitespaces))
                    withAnimation(.spring(duration: 0.2)) { editing = false }
                }) {
                    HStack(spacing: 6) {
                        Image(systemName: "checkmark")
                            .font(.system(size: 12, weight: .bold))
                        Text("Save tutor")
                            .font(.system(size: 13, weight: .semibold))
                    }
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 10)
                    .background(canSaveLocal ? accent : accent.opacity(0.4))
                    .clipShape(RoundedRectangle(cornerRadius: 10))
                }
                .buttonStyle(.plain)
                .disabled(!canSaveLocal)
            }
            .padding(.top, 10)
        }
        .padding(14)
        .frame(maxWidth: .infinity)
        .background(containerBg)
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }
}

// MARK: - Tasks from parents — summary banner

private struct ChildTasksSummaryBanner: View {
    let tasks: [ParentTaskItem]  // non-empty guaranteed by caller
    let onTaskClick: (String) -> Void
    let onSeeAll: () -> Void

    private var mostRecent: ParentTaskItem { tasks[0] }
    private var moreCount: Int { tasks.count - 1 }
    private var actionNeeded: Int { tasks.filter { $0.status == "ASSIGNED" || $0.status == "REJECTED" }.count }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: 12) {
                ZStack {
                    RoundedRectangle(cornerRadius: 12)
                        .fill(Color(hex: 0xFFF1E0))
                        .frame(width: 44, height: 44)
                    Text(mostRecent.subjectIcon.isEmpty ? "🎯" : mostRecent.subjectIcon)
                        .font(.system(size: 20))
                }

                VStack(alignment: .leading, spacing: 2) {
                    Text(childTaskTitle(mostRecent))
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(Color.dobiTextPrimary)
                        .lineLimit(2)
                    Text({
                        var s = mostRecent.isUnitScope ? "Lesson" : "Concept"
                        if !mostRecent.subject.isEmpty { s += " · \(mostRecent.subject)" }
                        return s
                    }())
                        .font(.system(size: 12))
                        .foregroundStyle(Color.dobiTextSecondary)
                }

                Spacer()

                VStack(alignment: .trailing, spacing: 4) {
                    ChildTaskStatusBadge(status: mostRecent.status)
                    if moreCount > 0 {
                        Text("+\(moreCount) more")
                            .font(.system(size: 10, weight: .semibold))
                            .foregroundStyle(Color.dobiPrimary)
                    }
                }
            }

            if tasks.count > 1 {
                HStack {
                    Text(actionNeeded > 0
                         ? "⚡ \(actionNeeded) task\(actionNeeded > 1 ? "s" : "") need\(actionNeeded == 1 ? "s" : "") your attention"
                         : "✓ All \(tasks.count) tasks completed")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(actionNeeded > 0 ? Color(hex: 0xB45309) : Color(hex: 0x15803D))
                    Spacer()
                    Image(systemName: "chevron.right")
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(actionNeeded > 0 ? Color(hex: 0xB45309) : Color(hex: 0x15803D))
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 10)
                .background(actionNeeded > 0 ? Color(hex: 0xFFF7E6) : Color(hex: 0xF8FAFF))
                .clipShape(RoundedRectangle(cornerRadius: 10))
                .padding(.top, 12)
                .contentShape(Rectangle())
                .onTapGesture { onSeeAll() }
            }
        }
        .padding(16)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .shadow(color: Color.black.opacity(0.05), radius: 3, x: 0, y: 1)
        .contentShape(Rectangle())
        .onTapGesture { onTaskClick(mostRecent.id) }
    }
}

private struct ChildTaskStatusBadge: View {
    let status: String

    private var config: (label: String, bg: Color, fg: Color) {
        switch status {
        case "ASSIGNED":       return ("New",       Color(hex: 0xFFF4E0), Color(hex: 0xB45309))
        case "PENDING_REVIEW": return ("Submitted", Color(hex: 0xEEF2FF), Color(hex: 0x4F46E5))
        case "REJECTED":       return ("Redo",      Color(hex: 0xFEE2E2), Color(hex: 0xDC2626))
        case "COMPLETED":      return ("Done",      Color(hex: 0xDCFCE7), Color(hex: 0x16A34A))
        case "REQUESTED":      return ("Requested", Color(hex: 0xF5F3FF), Color(hex: 0x7C3AED))
        default:               return (status,      Color.dobiPageBg,     Color.dobiTextSecondary)
        }
    }

    var body: some View {
        Text(config.label)
            .font(.system(size: 10, weight: .semibold))
            .foregroundStyle(config.fg)
            .padding(.horizontal, 8)
            .padding(.vertical, 4)
            .background(config.bg)
            .clipShape(RoundedRectangle(cornerRadius: 6))
    }
}

// MARK: - Tasks from parents — empty state

private struct ChildTasksEmptyCard: View {
    var onClick: () -> Void = {}
    private let ctaAccent = Color(hex: 0xE67E22)

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [Color(hex: 0xFFF4E0), Color(hex: 0xFFE8F1), Color(hex: 0xEEE8FF)],
                startPoint: .top, endPoint: .bottom
            )

            VStack(spacing: 0) {
                HStack {
                    Text("✨").font(.system(size: 18))
                    Spacer()
                    Text("⭐").font(.system(size: 14))
                }
                .padding(.horizontal, 18)
                .padding(.top, 14)

                VStack(spacing: 18) {
                    ZStack {
                        Circle()
                            .fill(Color.white.opacity(0.55))
                            .frame(width: 92, height: 92)
                        ZStack {
                            Circle()
                                .fill(LinearGradient(
                                    colors: [Color(hex: 0xFFD08A), Color(hex: 0xFF9F6B)],
                                    startPoint: .topLeading, endPoint: .bottomTrailing
                                ))
                                .frame(width: 72, height: 72)
                            Text("🎁").font(.system(size: 38))
                        }
                        .shadow(color: ctaAccent.opacity(0.4), radius: 8, x: 0, y: 4)
                    }

                    VStack(spacing: 8) {
                        Text("Confident with your studies?")
                            .font(.system(size: 17, weight: .bold))
                            .foregroundStyle(Color.dobiTextPrimary)
                            .multilineTextAlignment(.center)
                        Text("Ask your parents to give you a task and win an assured reward upon submission.")
                            .font(.system(size: 13))
                            .foregroundStyle(Color.dobiTextSecondary)
                            .multilineTextAlignment(.center)
                            .lineSpacing(5)
                    }

                    Button(action: onClick) {
                        HStack(spacing: 6) {
                            Text("🏆").font(.system(size: 14))
                            Text("Earn your first reward")
                                .font(.system(size: 12, weight: .semibold))
                                .foregroundStyle(ctaAccent)
                        }
                        .padding(.horizontal, 14)
                        .padding(.vertical, 9)
                        .background(Color.white)
                        .clipShape(RoundedRectangle(cornerRadius: 22))
                        .shadow(color: ctaAccent.opacity(0.2), radius: 4, x: 0, y: 2)
                    }
                    .buttonStyle(.plain)
                }
                .padding(.horizontal, 24)
                .padding(.vertical, 28)
            }
        }
        .clipShape(RoundedRectangle(cornerRadius: 24))
        .overlay(RoundedRectangle(cornerRadius: 24).stroke(Color.white.opacity(0.6), lineWidth: 1))
        .shadow(color: ctaAccent.opacity(0.15), radius: 10, x: 0, y: 4)
    }
}

// MARK: - My Learnings tab

private struct MyLearningsTab: View {
    let childId: String
    var onNavigateToQuizResult: (String, String) -> Void = { _, _ in }
    var onNavigateToParentTask: (String) -> Void = { _ in }
    var onNavigateToUploadGroup: (String) -> Void = { _ in }

    @EnvironmentObject private var appState: AppState
    @StateObject private var vm: MyLearningsViewModel

    init(childId: String,
         onNavigateToQuizResult: @escaping (String, String) -> Void = { _, _ in },
         onNavigateToParentTask: @escaping (String) -> Void = { _ in },
         onNavigateToUploadGroup: @escaping (String) -> Void = { _ in }) {
        self.childId = childId
        self.onNavigateToQuizResult = onNavigateToQuizResult
        self.onNavigateToParentTask = onNavigateToParentTask
        self.onNavigateToUploadGroup = onNavigateToUploadGroup
        _vm = StateObject(wrappedValue: MyLearningsViewModel(childId: childId))
    }

    private func handleTodayItemTap(_ entry: TodayLearningEntry) {
        guard let target = entry.navTarget else { return }
        switch target {
        case .quizResult(let unitId, let conceptId):
            onNavigateToQuizResult(conceptId, unitId)
        case .subjective(let resultId):
            vm.openSubjectiveResult(resultId)
        case .parentTask(let taskId):
            onNavigateToParentTask(taskId)
        case .uploadGroup(let groupKey):
            onNavigateToUploadGroup(groupKey)
        }
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                // Page title
                VStack(alignment: .leading, spacing: 4) {
                    Text("My Learnings")
                        .font(.system(size: 24, weight: .bold))
                        .foregroundStyle(Color.dobiTextPrimary)
                    Text("Your daily progress, saved lessons and what's new.")
                        .font(.dobiBody())
                        .foregroundStyle(Color.dobiTextSecondary)
                }
                .padding(.top, 28)
                .padding(.bottom, 24)

                // ── Section 1: Today's Learning ──
                MLSectionHeader(title: "Today's Learning", emoji: "✨")
                Spacer().frame(height: 12)

                if vm.todayItems.isEmpty {
                    TodayEmptyCard()
                } else {
                    VStack(spacing: 10) {
                        ForEach(vm.todayItems) { entry in
                            TodayEntryCard(entry: entry, onClick: { handleTodayItemTap(entry) })
                        }
                    }
                }

                Spacer().frame(height: 28)

                // ── Section 2: Revision Hub ──
                let previewMarks = Array(vm.revisionMarks.prefix(3))
                let hasMoreMarks = vm.revisionMarks.count > 3
                MLSectionHeader(title: "Revision Hub", emoji: "📚",
                                trailing: hasMoreMarks ? "See all" : nil, onTrailingClick: {})
                Spacer().frame(height: 4)
                Text("Topics you've marked for revision.")
                    .font(.dobiCaption())
                    .foregroundStyle(Color.dobiTextSecondary)
                Spacer().frame(height: 14)

                if previewMarks.isEmpty {
                    RevisionEmptyCard()
                } else {
                    VStack(spacing: 10) {
                        ForEach(previewMarks) { mark in
                            RevisionItemCard(mark: mark, onUnmark: { vm.onUnmark(mark) })
                        }
                    }
                }

                Spacer().frame(height: 28)

                // ── Section 3: Do You Know? ──
                let canOpenNews = !vm.currentAffairs.isEmpty
                MLSectionHeader(
                    title: "Do You Know?",
                    emoji: "💡",
                    trailing: canOpenNews ? "Open paper" : (vm.isLoadingNews ? "Loading…" : "Retry"),
                    onTrailingClick: { canOpenNews ? vm.openStory(at: 0) : vm.retryLoadNews() }
                )
                Spacer().frame(height: 4)
                Text("Today's stories — tap any one to open the paper.")
                    .font(.dobiCaption())
                    .foregroundStyle(Color.dobiTextSecondary)
                Spacer().frame(height: 14)

                if vm.currentAffairs.isEmpty {
                    NewsEmptyCard(isLoading: vm.isLoadingNews, error: vm.newsError, onRetry: vm.retryLoadNews)
                } else {
                    VStack(spacing: 10) {
                        ForEach(vm.currentAffairs.indices, id: \.self) { i in
                            CurrentAffairCard(news: vm.currentAffairs[i], onClick: { vm.openStory(at: i) })
                        }
                    }
                }

                Spacer().frame(height: 100)
            }
            .padding(.horizontal, 20)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.dobiPageBg)
        .onAppear { vm.onAppear(parentTasks: appState.parentTasks) }
        .overlay {
            if let idx = vm.selectedStoryIndex, !vm.currentAffairs.isEmpty {
                NewspaperOverlay(stories: vm.currentAffairs, startIndex: idx, onDismiss: vm.closeStory)
                    .transition(.opacity.combined(with: .scale(scale: 0.96)))
            }
        }
        .animation(.spring(duration: 0.3), value: vm.selectedStoryIndex != nil)
        .sheet(isPresented: Binding(
            get: { vm.subjectiveSheet != nil },
            set: { if !$0 { vm.closeSubjectiveSheet() } }
        )) {
            if let result = vm.subjectiveSheet {
                MLSubjectiveResultSheet(result: result, onDismiss: vm.closeSubjectiveSheet)
            } else {
                ProgressView().padding(40)
            }
        }
    }
}

// MARK: - Section header

private struct MLSectionHeader: View {
    let title: String
    let emoji: String
    var trailing: String? = nil
    var onTrailingClick: (() -> Void)? = nil

    var body: some View {
        HStack {
            HStack(spacing: 8) {
                Text(emoji).font(.system(size: 18))
                Text(title)
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(Color.dobiTextPrimary)
            }
            Spacer()
            if let label = trailing {
                Button(action: { onTrailingClick?() }) {
                    Text(label)
                        .font(.system(size: 14, weight: .medium))
                        .foregroundStyle(Color.dobiPrimary)
                }
                .buttonStyle(.plain)
            }
        }
    }
}

// MARK: - Today's Learning: empty state

private struct TodayEmptyCard: View {
    private let accent = Color(hex: 0x5B6BFF)

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [Color(hex: 0xE8F1FF), Color(hex: 0xF1E8FF), Color(hex: 0xFFE8F1)],
                startPoint: .top, endPoint: .bottom
            )
            // Decorative sparkles
            Group {
                Text("✨").font(.system(size: 18)).frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading).padding(.leading, 18).padding(.top, 14)
                Text("⭐").font(.system(size: 14)).frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topTrailing).padding(.trailing, 20).padding(.top, 26)
                Text("💫").font(.system(size: 16)).frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottomTrailing).padding(.trailing, 18).padding(.bottom, 14)
            }
            VStack(spacing: 18) {
                ZStack {
                    Circle().fill(Color.white.opacity(0.55)).frame(width: 92, height: 92)
                    ZStack {
                        Circle()
                            .fill(LinearGradient(colors: [Color(hex: 0x7C8BFF), accent], startPoint: .topLeading, endPoint: .bottomTrailing))
                            .frame(width: 72, height: 72)
                        Text("🚀").font(.system(size: 36))
                    }
                    .shadow(color: accent.opacity(0.4), radius: 8, x: 0, y: 4)
                }
                VStack(spacing: 8) {
                    Text("Let's get started!")
                        .font(.system(size: 17, weight: .bold))
                        .foregroundStyle(Color.dobiTextPrimary)
                    Text("You're yet to begin today's tasks. Start now and share your progress with friends and parents.")
                        .font(.system(size: 13))
                        .foregroundStyle(Color.dobiTextSecondary)
                        .multilineTextAlignment(.center)
                        .lineSpacing(5)
                }
                HStack(spacing: 6) {
                    Text("🎯").font(.system(size: 14))
                    Text("Begin today's learning")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(accent)
                }
                .padding(.horizontal, 14).padding(.vertical, 9)
                .background(Color.white)
                .clipShape(RoundedRectangle(cornerRadius: 22))
                .shadow(color: accent.opacity(0.2), radius: 4, x: 0, y: 2)
            }
            .padding(.horizontal, 24).padding(.vertical, 28)
        }
        .clipShape(RoundedRectangle(cornerRadius: 24))
        .overlay(RoundedRectangle(cornerRadius: 24).stroke(Color.white.opacity(0.6), lineWidth: 1))
        .shadow(color: accent.opacity(0.18), radius: 10, x: 0, y: 4)
    }
}

// MARK: - Today's Learning: entry card

private struct TodayEntryCard: View {
    let entry: TodayLearningEntry
    var onClick: () -> Void = {}

    private var style: (emoji: String, label: String, accent: Color, tint: Color) {
        switch entry.kind {
        case .homework: return ("📝", "Homework", Color(hex: 0xE67E22), Color(hex: 0xFFF4E0))
        case .task:     return ("🎯", "Task",     Color(hex: 0x7C3AED), Color(hex: 0xEDE9FE))
        case .quiz:     return ("🧠", "Quiz",     Color(hex: 0x16A34A), Color(hex: 0xDCFCE7))
        }
    }

    private var accentColor: Color {
        entry.status == .pending ? Color.dobiWarning : style.accent
    }

    var body: some View {
        Button(action: onClick) {
            DobiCard(cornerRadius: 16) {
                HStack(spacing: 12) {
                    // Left accent strip + emoji bubble
                    HStack(spacing: 12) {
                        RoundedRectangle(cornerRadius: 3)
                            .fill(accentColor)
                            .frame(width: 4, height: 48)

                        ZStack {
                            RoundedRectangle(cornerRadius: 12)
                                .fill(style.tint)
                                .frame(width: 44, height: 44)
                            Text(style.emoji).font(.system(size: 22))
                        }
                    }

                    // Text content
                    VStack(alignment: .leading, spacing: 4) {
                        HStack(spacing: 6) {
                            // Kind pill
                            Text(style.label)
                                .font(.system(size: 10, weight: .semibold))
                                .foregroundStyle(style.accent)
                                .lineLimit(1)
                                .padding(.horizontal, 6).padding(.vertical, 3)
                                .background(style.tint)
                                .clipShape(RoundedRectangle(cornerRadius: 6))
                                .fixedSize(horizontal: true, vertical: false)
                            // Status pill
                            TodayStatusPill(status: entry.status)
                                .fixedSize(horizontal: true, vertical: false)
                            // By Parent badge
                            if entry.assignedByParent {
                                Text("By Parent")
                                    .font(.system(size: 10, weight: .semibold))
                                    .foregroundStyle(Color(hex: 0x5B21B6))
                                    .lineLimit(1)
                                    .padding(.horizontal, 6).padding(.vertical, 3)
                                    .background(Color(hex: 0xEDE9FE))
                                    .clipShape(RoundedRectangle(cornerRadius: 6))
                                    .fixedSize(horizontal: true, vertical: false)
                            }
                        }
                        Text(entry.title)
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundStyle(Color.dobiTextPrimary)
                            .lineLimit(2)
                        Text(entry.subject)
                            .font(.system(size: 12))
                            .foregroundStyle(Color.dobiTextSecondary)
                    }

                    Spacer()

                    // Right: time + score/check
                    VStack(alignment: .trailing, spacing: 4) {
                        Text(entry.timeLabel)
                            .font(.system(size: 11))
                            .foregroundStyle(Color.dobiTextSecondary)
                        if let score = entry.scoreLabel {
                            Text(score)
                                .font(.system(size: 15, weight: .bold))
                                .foregroundStyle(style.accent)
                            Text("score")
                                .font(.system(size: 10))
                                .foregroundStyle(Color.dobiTextSecondary)
                        } else if entry.status == .completed {
                            Circle()
                                .fill(Color.dobiSuccess.opacity(0.15))
                                .frame(width: 28, height: 28)
                                .overlay(
                                    Image(systemName: "checkmark")
                                        .font(.system(size: 12, weight: .semibold))
                                        .foregroundStyle(Color.dobiSuccess)
                                )
                        } else {
                            Image(systemName: "chevron.right")
                                .font(.system(size: 14, weight: .medium))
                                .foregroundStyle(Color.dobiTextSecondary)
                        }
                    }
                }
                .padding(14)
            }
        }
        .buttonStyle(.plain)
    }
}

private struct TodayStatusPill: View {
    let status: TodayStatus

    var body: some View {
        let (label, fg, bg): (String, Color, Color) = status == .completed
            ? ("Done", Color.dobiSuccess, Color.dobiSuccess.opacity(0.12))
            : ("Pending", Color.dobiWarning, Color.dobiWarning.opacity(0.12))
        HStack(spacing: 4) {
            Circle().fill(fg).frame(width: 5, height: 5)
            Text(label)
                .font(.system(size: 10, weight: .semibold))
                .foregroundStyle(fg)
                .lineLimit(1)
                .fixedSize(horizontal: true, vertical: false)
        }
        .padding(.horizontal, 6).padding(.vertical, 3)
        .background(bg)
        .clipShape(RoundedRectangle(cornerRadius: 6))
    }
}

// MARK: - Revision Hub: empty state

private struct RevisionEmptyCard: View {
    var body: some View {
        HStack(spacing: 12) {
            Circle()
                .fill(Color.dobiPrimaryContainer)
                .frame(width: 44, height: 44)
                .overlay(Text("🔖").font(.system(size: 22)))
            VStack(alignment: .leading, spacing: 2) {
                Text("No topics flagged yet")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(Color.dobiTextPrimary)
                Text("Open a lesson or concept and tap Mark for Revision. We'll keep them here so you can come back to them later.")
                    .font(.system(size: 12))
                    .foregroundStyle(Color.dobiTextSecondary)
                    .lineSpacing(3)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(hex: 0xF8FAFF))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color(hex: 0xE5EBF5), lineWidth: 1))
    }
}

// MARK: - Revision Hub: item card

private struct RevisionItemCard: View {
    let mark: RevisionMarkItem
    let onUnmark: () -> Void

    var body: some View {
        let accent = mark.parsedColor
        DobiCard(cornerRadius: 18) {
            HStack(spacing: 12) {
                ZStack {
                    RoundedRectangle(cornerRadius: 14)
                        .fill(accent.opacity(0.18))
                        .frame(width: 48, height: 48)
                    Text(mark.subjectIcon.isEmpty ? "📘" : mark.subjectIcon)
                        .font(.system(size: 24))
                }

                VStack(alignment: .leading, spacing: 2) {
                    HStack(spacing: 4) {
                        Image(systemName: "bookmark.fill")
                            .font(.system(size: 11, weight: .medium))
                            .foregroundStyle(Color(hex: 0xF59E0B))
                        Text(mark.isUnit ? "Lesson" : "Concept")
                            .font(.system(size: 10, weight: .semibold))
                            .foregroundStyle(Color(hex: 0xF59E0B))
                    }
                    Text(mark.name)
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(Color.dobiTextPrimary)
                    Text(mark.secondaryLabel)
                        .font(.system(size: 12))
                        .foregroundStyle(Color.dobiTextSecondary)
                }

                Spacer()

                Button(action: onUnmark) {
                    Image(systemName: "xmark")
                        .font(.system(size: 14, weight: .medium))
                        .foregroundStyle(Color.dobiTextSecondary)
                        .frame(width: 36, height: 36)
                }
                .buttonStyle(.plain)
            }
            .padding(14)
            .background(accent.opacity(0.06))
        }
    }
}

// MARK: - Do You Know: current affair card

private struct CurrentAffairCard: View {
    let news: CurrentAffairItem
    let onClick: () -> Void

    var body: some View {
        let colors = categoryPalette(news.category)
        Button(action: onClick) {
            DobiCard(cornerRadius: 16) {
                HStack(spacing: 12) {
                    RoundedRectangle(cornerRadius: 3)
                        .fill(colors.accent)
                        .frame(width: 4, height: 56)

                    ZStack {
                        RoundedRectangle(cornerRadius: 14)
                            .fill(colors.tint)
                            .frame(width: 48, height: 48)
                        Text(news.emoji).font(.system(size: 24))
                    }

                    VStack(alignment: .leading, spacing: 6) {
                        HStack(spacing: 6) {
                            Text(news.category)
                                .font(.system(size: 10, weight: .semibold))
                                .foregroundStyle(colors.accent)
                                .padding(.horizontal, 6).padding(.vertical, 2)
                                .background(colors.tint)
                                .clipShape(RoundedRectangle(cornerRadius: 4))
                            Text(news.timeAgo)
                                .font(.system(size: 11))
                                .foregroundStyle(Color.dobiTextSecondary)
                        }
                        Text(news.headline)
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundStyle(Color.dobiTextPrimary)
                            .lineLimit(3)
                            .lineSpacing(3)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
                .padding(14)
                .contentShape(Rectangle())
            }
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Do You Know: empty / loading / error state

private struct NewsEmptyCard: View {
    let isLoading: Bool
    var error: String? = nil
    let onRetry: () -> Void

    private var title: String {
        if isLoading { return "Putting today's paper together…" }
        if error != nil { return "Couldn't load today's paper" }
        return "Fresh stories coming soon"
    }
    private var subtitle: String {
        if isLoading { return "Hand-picked, kid-safe stories. Just a few seconds…" }
        if let e = error { return e }
        return "We'll pull the day's interesting news for you every 24 hours."
    }

    var body: some View {
        Button(action: { if !isLoading { onRetry() } }) {
            HStack(spacing: 12) {
                Circle()
                    .fill(error != nil ? Color(hex: 0xFEE2E2) : Color.dobiSuccess.opacity(0.15))
                    .frame(width: 44, height: 44)
                    .overlay(Text(error != nil ? "📵" : "📰").font(.system(size: 22)))
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(Color.dobiTextPrimary)
                    Text(subtitle)
                        .font(.system(size: 12))
                        .foregroundStyle(Color.dobiTextSecondary)
                        .lineSpacing(3)
                    if error != nil && !isLoading {
                        Text("Tap to retry")
                            .font(.system(size: 12, weight: .medium))
                            .foregroundStyle(Color.dobiPrimary)
                            .padding(.top, 4)
                    }
                }
            }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color(hex: 0xF8FAFF))
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color(hex: 0xE5EBF5), lineWidth: 1))
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Subjective result sheet (shown from My Learnings Today tap)

private struct MLSubjectiveResultSheet: View {
    let result: ADSubjectiveResult
    let onDismiss: () -> Void

    private var accent: Color {
        result.accuracyPct >= 80 ? Color.dobiSuccess
            : result.accuracyPct >= 50 ? Color(hex: 0xF59E0B)
            : Color.dobiError
    }

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(result.displayName)
                        .font(.system(size: 17, weight: .bold))
                        .foregroundStyle(Color.dobiTextPrimary)
                    if !result.subject.isEmpty {
                        Text(result.subject)
                            .font(.system(size: 12))
                            .foregroundStyle(Color.dobiTextSecondary)
                    }
                }
                Spacer()
                Button(action: onDismiss) {
                    Image(systemName: "xmark.circle.fill")
                        .font(.system(size: 24))
                        .foregroundStyle(Color.dobiTextSecondary)
                }
                .buttonStyle(.plain)
            }
            .padding(.horizontal, 20)
            .padding(.top, 20)
            .padding(.bottom, 16)

            HStack(spacing: 0) {
                MLStatCell(value: "\(result.correct)/\(result.attempted)", label: "Correct", color: accent)
                    .frame(maxWidth: .infinity)
                Divider().frame(height: 32)
                MLStatCell(value: "\(result.accuracyPct)%", label: "Accuracy", color: accent)
                    .frame(maxWidth: .infinity)
            }
            .padding(.vertical, 12)
            .background(accent.opacity(0.08))
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .padding(.horizontal, 16)
            .padding(.bottom, 12)

            ScrollView {
                LazyVStack(spacing: 10) {
                    ForEach(Array(result.items.enumerated()), id: \.offset) { idx, qa in
                        MLQACard(index: idx + 1, qa: qa)
                    }
                    Spacer().frame(height: 24)
                }
                .padding(.horizontal, 16)
                .padding(.top, 4)
            }
        }
        .background(Color(hex: 0xF8FAFF).ignoresSafeArea())
        .presentationDetents([.large])
        .presentationDragIndicator(.visible)
    }
}

private struct MLStatCell: View {
    let value: String
    let label: String
    let color: Color

    var body: some View {
        VStack(spacing: 2) {
            Text(value)
                .font(.system(size: 18, weight: .bold))
                .foregroundStyle(color)
            Text(label)
                .font(.system(size: 11))
                .foregroundStyle(Color.dobiTextSecondary)
        }
    }
}

private struct MLQACard: View {
    let index: Int
    let qa: ADSubjectiveResultItem

    private var verdictColor: Color {
        !qa.isAttempted ? Color.dobiTextSecondary
            : qa.isCorrect ? Color.dobiSuccess
            : Color.dobiError
    }
    private var verdictLabel: String {
        !qa.isAttempted ? "Skipped" : qa.isCorrect ? "Correct" : "Needs work"
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("Q\(index)")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(Color.dobiTextSecondary)
                Spacer()
                Text(verdictLabel)
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(verdictColor)
                    .padding(.horizontal, 8).padding(.vertical, 3)
                    .background(verdictColor.opacity(0.12))
                    .clipShape(RoundedRectangle(cornerRadius: 6))
            }
            Text(qa.questionText)
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(Color.dobiTextPrimary)
            if !qa.answerSeen.isEmpty {
                Text("Your answer: \(qa.answerSeen)")
                    .font(.system(size: 13))
                    .foregroundStyle(Color.dobiTextSecondary)
            }
            if !qa.strictFeedback.isEmpty && !qa.isCorrect {
                Text(qa.strictFeedback)
                    .font(.system(size: 13))
                    .foregroundStyle(Color.dobiSuccess)
            }
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color(hex: 0xE5EBF5), lineWidth: 1))
    }
}

#Preview {
    ChildHomeView(childId: "1", childName: "Aarav Sharma")
        .environmentObject(AppState())
}
