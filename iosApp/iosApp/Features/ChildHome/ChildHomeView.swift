import SwiftUI

struct ChildHomeView: View {
    let childId: String
    let childName: String
    var onNavigateToConceptList: () -> Void = {}
    var onNavigateToHomeworkLog: () -> Void = {}
    var onNavigateToClassTestPrep: () -> Void = {}
    var onNavigateToStreakHome: () -> Void = {}
    var onNavigateToParentTask: (String) -> Void = { _ in }
    var onRequestParentAccess: () -> Void = {}

    @EnvironmentObject private var appState: AppState
    @State private var selectedTab = 0
    @State private var selectedModeIndex: Int? = nil

    private var myTasks: [ParentTaskItem] {
        appState.parentTasks.filter { $0.childId == childId }
    }

    private var streakDays: Int {
        appState.engagements[childId]?.streakDays ?? 0
    }

    var body: some View {
        ZStack(alignment: .bottom) {
            TabView(selection: $selectedTab) {
                ChildDashboardTab(
                    childName: childName,
                    parentTasks: myTasks,
                    streakDays: streakDays,
                    selectedModeIndex: $selectedModeIndex,
                    onConceptList: onNavigateToConceptList,
                    onHomework: onNavigateToHomeworkLog,
                    onClassTest: onNavigateToClassTestPrep,
                    onStreak: onNavigateToStreakHome,
                    onParentTask: onNavigateToParentTask
                )
                .tag(0)

                MyLearningsView()
                    .tag(1)

                ChildProfileTab(childName: childName, onSwitchToParent: onRequestParentAccess)
                    .tag(2)
            }
            .tabViewStyle(.page(indexDisplayMode: .never))

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
                            case (0, 0): onNavigateToConceptList()
                            case (0, 1): onNavigateToHomeworkLog()
                            case (0, 2): onNavigateToClassTestPrep()
                            default: onNavigateToConceptList()
                            }
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
                    withAnimation(.spring(duration: 0.3)) { selected = i }
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
                    .padding(.vertical, 10)
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
                    onInviteFriend: {},
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
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 28)

                Group {
                    if !pendingTasks.isEmpty {
                        ChildTasksSummaryBanner(tasks: pendingTasks, onTaskClick: onParentTask)
                    } else if !requestedTasks.isEmpty {
                        ChildTasksSummaryBanner(tasks: requestedTasks, onTaskClick: onParentTask)
                    } else {
                        ChildTasksEmptyCard()
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 12)

                Spacer().frame(height: 100)
            }
            .frame(maxWidth: .infinity)
        }
        .background(Color.dobiPageBg)
    }
}

// MARK: - Pending task bar (ASSIGNED tasks above streak)

private func childTaskTitle(_ task: ParentTaskItem) -> String {
    let icon = task.subjectIcon.isEmpty ? "📚" : task.subjectIcon
    var name = task.displayName.isEmpty ? (task.subject.isEmpty ? "Task" : task.subject) : task.displayName
    switch task.type {
    case "REVISE_CHAPTER": name += " – Revise"
    case "WRITE_QA": name += " – Q&A"
    default: break
    }
    return "\(icon) \(name)"
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
            .padding(.bottom, 20)

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

// MARK: - Tasks from parents — summary banner

private struct ChildTasksSummaryBanner: View {
    let tasks: [ParentTaskItem]  // non-empty guaranteed by caller
    let onTaskClick: (String) -> Void

    private var mostRecent: ParentTaskItem { tasks[0] }
    private var moreCount: Int { tasks.count - 1 }
    private var actionNeeded: Int { tasks.filter { $0.status == "ASSIGNED" || $0.status == "REJECTED" }.count }

    var body: some View {
        Button { onTaskClick(mostRecent.id) } label: {
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
                        Text(mostRecent.subject.isEmpty ? "Task" : mostRecent.subject)
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
                }
            }
            .padding(16)
        }
        .buttonStyle(.plain)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .shadow(color: Color.black.opacity(0.05), radius: 3, x: 0, y: 1)
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

struct MyLearningsView: View {
    var body: some View {
        VStack(spacing: 20) {
            Spacer().frame(height: 60)
            EmojiAvatar(emoji: "📖", size: 80, background: .dobiPastelSkyBlue)
            Text("My Learnings")
                .font(.dobiTitle2())
                .foregroundStyle(Color.dobiTextPrimary)
            Text("Your quiz results, uploads\nand learning history will appear here.")
                .font(.dobiBody())
                .foregroundStyle(Color.dobiTextSecondary)
                .multilineTextAlignment(.center)
            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.dobiPageBg)
    }
}

// MARK: - Profile tab

private struct ChildProfileTab: View {
    let childName: String
    var onSwitchToParent: () -> Void

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                Spacer().frame(height: 60)
                EmojiAvatar(emoji: "👦", size: 80, background: .dobiPrimaryContainer)
                    .padding(.bottom, 12)
                Text(childName)
                    .font(.dobiTitle2())
                    .foregroundStyle(Color.dobiTextPrimary)
                Text("My Profile")
                    .font(.dobiBody())
                    .foregroundStyle(Color.dobiTextSecondary)
                    .padding(.bottom, 40)

                VStack(spacing: 0) {
                    SettingsRow(icon: "lock.shield",  title: "Privacy & Data", color: .dobiPrimary, action: {})
                    Divider().padding(.leading, 56)
                    SettingsRow(icon: "questionmark.circle", title: "FAQ", color: .dobiWarning, action: {})
                    Divider().padding(.leading, 56)
                    SettingsRow(icon: "headphones", title: "Help & Support", color: .dobiSuccess, action: {})
                }
                .background(Color.white)
                .clipShape(RoundedRectangle(cornerRadius: 16))
                .padding(.horizontal, 20)

                VStack(spacing: 0) {
                    SettingsRow(
                        icon: "rectangle.portrait.and.arrow.right",
                        title: "Switch to Parent",
                        color: .dobiError,
                        action: onSwitchToParent
                    )
                }
                .background(Color.white)
                .clipShape(RoundedRectangle(cornerRadius: 16))
                .padding(.horizontal, 20)
                .padding(.top, 16)

                Spacer().frame(height: 100)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.dobiPageBg)
    }
}

// MARK: - Settings row (shared)

struct SettingsRow: View {
    let icon: String
    let title: String
    let color: Color
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 14) {
                ZStack {
                    RoundedRectangle(cornerRadius: 8)
                        .fill(color.opacity(0.12))
                        .frame(width: 34, height: 34)
                    Image(systemName: icon)
                        .font(.system(size: 15, weight: .medium))
                        .foregroundStyle(color)
                }
                Text(title)
                    .font(.dobiBody())
                    .foregroundStyle(Color.dobiTextPrimary)
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(Color.dobiTextTertiary)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
        }
        .buttonStyle(.plain)
    }
}

#Preview {
    ChildHomeView(childId: "1", childName: "Aarav Sharma")
        .environmentObject(AppState())
}
