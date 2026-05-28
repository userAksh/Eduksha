import SwiftUI
import SafariServices

// ── Screen palette (mirrors Android exactly) ───────────────────────────────────
private let PageBg          = Color(hex: 0xF3F6FD)
private let StatOrange      = Color(hex: 0xF59E0B)
private let StatOrangeBg    = Color(hex: 0xFFF3D6)
private let StatPink        = Color(hex: 0xEF4444)
private let StatPinkBg      = Color(hex: 0xFFE4E6)
private let DividerCol      = Color(hex: 0xF0F2F7)
private let ProgressTrack   = Color(hex: 0xE6EAF5)
private let DobiSuccess     = Color(hex: 0x22C55E)
private let DobiSuccessBg   = Color(hex: 0xDCFCE7)
private let DobiPrimary     = Color(hex: 0x2F7DFF)
private let DobiPrimarySoft = Color(hex: 0xDBEAFE)
private let DobiTextPrim    = Color(hex: 0x1A2B4A)
private let DobiTextSec     = Color(hex: 0x6B7A99)
private let DobiError       = Color(hex: 0xEF4444)

// ── In-app Safari sheet ────────────────────────────────────────────────────────

private struct IdentifiableURL: Identifiable {
    let id = UUID()
    let url: URL
}

private struct SafariSheet: UIViewControllerRepresentable {
    let url: URL
    func makeUIViewController(context: Context) -> SFSafariViewController {
        SFSafariViewController(url: url)
    }
    func updateUIViewController(_ vc: SFSafariViewController, context: Context) {}
}

// ── Entry point ────────────────────────────────────────────────────────────────

struct ParentHomeView: View {
    var onNavigateToChildHome: (String) -> Void = { _ in }
    var onNavigateToAddChild: () -> Void = {}
    var onNavigateToInsights: (String) -> Void = { _ in }
    var onNavigateToStudyGoals: (String) -> Void = { _ in }
    var onNavigateToParentTask: (String) -> Void = { _ in }
    var onNavigateToActivityDetail: (String, String, String) -> Void = { _, _, _ in }
    var onSignOut: () -> Void = {}

    @EnvironmentObject private var appState: AppState
    @State private var selectedTab: ParentTab = .children
    @State private var safariItem: IdentifiableURL? = nil

    enum ParentTab { case children, dashboard, settings }

    var body: some View {
        ZStack(alignment: .bottom) {
            PageBg.ignoresSafeArea()

            Group {
                switch selectedTab {
                case .children:
                    ParentChildrenTab(
                        children: appState.children,
                        engagements: appState.engagements,
                        parentName: appState.parentName,
                        parentTasks: appState.parentTasks,
                        onEnterChildHome: onNavigateToChildHome,
                        onAddChild: onNavigateToAddChild,
                        onNavigateToParentTask: onNavigateToParentTask,
                        onSendReminder: appState.sendReminder
                    )
                case .dashboard:
                    ParentDashboardTab(
                        children: appState.children,
                        engagements: appState.engagements,
                        weekActivities: appState.weekActivities,
                        insights: appState.insights,
                        onNavigateToInsights: onNavigateToInsights,
                        onNavigateToAddChild: onNavigateToAddChild,
                        onNavigateToActivityDetail: onNavigateToActivityDetail
                    )
                case .settings:
                    ParentSettingsTab(
                        children: appState.children,
                        parentName: appState.parentName,
                        parentPhone: appState.parentPhone,
                        onEditProfile: {},
                        onStudyGoals: onNavigateToStudyGoals,
                        onAddChild: onNavigateToAddChild,
                        onPrivacyData: { if let u = URL(string: "https://eduksha.in/privacy.html") { safariItem = IdentifiableURL(url: u) } },
                        onFaq: { if let u = URL(string: "https://eduksha.in/#faq") { safariItem = IdentifiableURL(url: u) } },
                        onHelpSupport: { if let u = URL(string: "https://eduksha.in/support.html") { safariItem = IdentifiableURL(url: u) } },
                        onSignOut: onSignOut
                    )
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)

            ParentBottomNavBar(selected: $selectedTab)
        }
        .navigationBarHidden(true)
        .ignoresSafeArea(edges: .bottom)
        .sheet(item: $safariItem) { item in
            SafariSheet(url: item.url)
                .ignoresSafeArea()
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// MARK: – Children Tab
// ═══════════════════════════════════════════════════════════════════════════════

struct ParentChildrenTab: View {
    let children: [ChildProfile]
    var engagements: [String: ChildEngagement] = [:]
    var parentName: String = ""
    var parentTasks: [ParentTaskItem] = []
    var onEnterChildHome: (String) -> Void
    var onAddChild: () -> Void
    var onNavigateToParentTask: (String) -> Void = { _ in }
    var onSendReminder: (String) -> Void = { _ in }

    private var childById: [String: ChildProfile] {
        Dictionary(uniqueKeysWithValues: children.map { ($0.id, $0) })
    }

    private var actionableTasks: [ParentTaskItem] {
        let now = Int64(Date().timeIntervalSince1970 * 1000)
        let twentyFourH: Int64 = 24 * 60 * 60 * 1000
        return parentTasks.filter { task in
            switch task.status {
            case "REQUESTED", "PENDING_REVIEW": return true
            case "ASSIGNED":
                return task.assignedAtMillis > 0 &&
                       now - task.assignedAtMillis >= twentyFourH &&
                       (task.reminderSentAtMillis == 0 || now - task.reminderSentAtMillis >= twentyFourH)
            default: return false
            }
        }
    }

    private func activeTasksCount(for childId: String) -> Int {
        parentTasks.filter {
            $0.childId == childId &&
            ["REQUESTED", "ASSIGNED", "PENDING_REVIEW"].contains($0.status)
        }.count
    }

    // Track whether we've had tasks at least once so we can jump to top when
    // the tasks card first appears (async load inserts content above children).
    @State private var hadTasks = false

    var body: some View {
        VStack(spacing: 0) {
            // ── Sticky header (non-scrollable) ────────────────────────────────
            ParentHeaderRow(parentName: parentName)
                .background(PageBg)

            // ── Scrollable content ────────────────────────────────────────────
            ScrollViewReader { proxy in
                ScrollView {
                    VStack(spacing: 0) {
                        // Invisible anchor always at the very top of scroll content
                        Color.clear.frame(height: 0).id("childrenTop")

                        Spacer().frame(height: 4)

                        // Tasks needing attention card (mirrors Android TasksCard)
                        if let firstTask = actionableTasks.first {
                            TasksCard(
                                firstTask: firstTask,
                                moreCount: max(0, actionableTasks.count - 1),
                                childName: childById[firstTask.childId]?.name ?? "",
                                onTap: { onNavigateToParentTask(firstTask.id) },
                                onViewAll: { onNavigateToParentTask(firstTask.id) },
                                onSendReminder: { onSendReminder(firstTask.id) }
                            )
                            .padding(.horizontal, 20)
                            Spacer().frame(height: 24)
                        } else {
                            Spacer().frame(height: 8)
                        }

                        // Section header
                        ChildrenSectionHeader(count: children.count)
                            .padding(.horizontal, 20)
                        Spacer().frame(height: 14)

                        // Child cards
                        ForEach(Array(children.enumerated()), id: \.element.id) { idx, child in
                            ChildStatCard(
                                child: child,
                                index: idx,
                                engagement: engagements[child.id] ?? .empty,
                                activeTasksCount: activeTasksCount(for: child.id),
                                onClick: { onEnterChildHome(child.id) }
                            )
                            .padding(.horizontal, 20)
                            Spacer().frame(height: 14)
                        }

                        // Add more children card
                        AddMoreChildrenCard(onAddChild: onAddChild)
                            .padding(.horizontal, 20)
                        Spacer().frame(height: 100)
                    }
                }
                .background(PageBg)
                // Pin to top on first appear so LazyVStack-style drift doesn't happen
                .onAppear {
                    proxy.scrollTo("childrenTop", anchor: .top)
                }
                // When tasks load for the first time, scroll back to top so the
                // newly inserted tasks card isn't hidden above the visible area
                .onChange(of: actionableTasks.isEmpty) { isEmpty in
                    if !isEmpty && !hadTasks {
                        hadTasks = true
                        proxy.scrollTo("childrenTop", anchor: .top)
                    }
                }
            }
        }
        .background(PageBg)
    }
}

// ── Header row ─────────────────────────────────────────────────────────────────

private struct ParentHeaderRow: View {
    var parentName: String = ""

    var body: some View {
        // Bell (44pt) sits at trailing=14. Reserve 58pt on the right so the
        // illustration never slides under it (mirrors Android padding(end=52dp)).
        ZStack(alignment: .topTrailing) {
            HStack(alignment: .center, spacing: 0) {
                VStack(alignment: .leading, spacing: 2) {
                    let name = parentName.trimmingCharacters(in: .whitespaces)
                    Text(name.isEmpty ? "Hello, there! 👋" : "Hello, \(name)! 👋")
                        .font(.system(size: 20, weight: .bold))
                        .foregroundStyle(DobiTextPrim)
                    Text("Track your children's learning")
                        .font(.system(size: 12))
                        .foregroundStyle(DobiTextSec)
                }
                .padding(.leading, 20)

                Spacer()

                Image("parent_home_illustration")
                    .resizable()
                    .scaledToFit()
                    .frame(height: 96)
            }
            .padding(.bottom, 8)
            .padding(.trailing, 58)  // 44pt bell + 14pt gap

            // Bell sits at the true top-right, never overlaps illustration
            BellButton()
                .padding(.top, 8)
                .padding(.trailing, 14)
        }
    }
}

private struct BellButton: View {
    var body: some View {
        ZStack(alignment: .topTrailing) {
            ZStack {
                Circle()
                    .fill(Color.white)
                    .frame(width: 44, height: 44)
                Image(systemName: "bell.fill")
                    .font(.system(size: 18))
                    .foregroundStyle(DobiTextPrim)
            }
            Circle()
                .fill(DobiError)
                .frame(width: 9, height: 9)
                .offset(x: -3, y: 3)
        }
    }
}

// ── Children section header ────────────────────────────────────────────────────

private struct ChildrenSectionHeader: View {
    let count: Int

    var body: some View {
        HStack(alignment: .top) {
            VStack(alignment: .leading, spacing: 2) {
                Text("Your Children")
                    .font(.system(size: 20, weight: .bold))
                    .foregroundStyle(DobiTextPrim)
                Text("View progress, insights and help them grow")
                    .font(.system(size: 13))
                    .foregroundStyle(DobiTextSec)
            }
            Spacer()
            HStack(spacing: 6) {
                Image(systemName: "person.2.fill")
                    .font(.system(size: 13))
                    .foregroundStyle(DobiPrimary)
                Text("\(count) profile\(count == 1 ? "" : "s")")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(DobiPrimary)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(DobiPrimarySoft)
            .clipShape(RoundedRectangle(cornerRadius: 10))
        }
    }
}

// ── Child stat card ────────────────────────────────────────────────────────────

private let avatarEmojis = ["👦", "👧", "🧒", "👶"]
private let avatarColors: [Color] = [
    Color(hex: 0xDBEAFE), Color(hex: 0xFCE7F3), Color(hex: 0xD1FAE5), Color(hex: 0xFEF9C3)
]

private struct ChildStatCard: View {
    let child: ChildProfile
    let index: Int
    var engagement: ChildEngagement = .empty
    var activeTasksCount: Int = 0
    var onClick: () -> Void

    private var weeklyDaysActive: Int { engagement.daysActiveThisWeek }
    private var isActiveToday: Bool { engagement.isActiveToday }
    private var accuracyPct: Int? { engagement.accuracyPct }

    var body: some View {
        Button(action: onClick) {
            VStack(spacing: 0) {
                // Top row: avatar + info + ring + arrow
                HStack(spacing: 14) {
                    // Avatar
                    ZStack {
                        Circle()
                            .fill(avatarColors[index % avatarColors.count])
                            .frame(width: 60, height: 60)
                        Text(avatarEmojis[index % avatarEmojis.count])
                            .font(.system(size: 34))
                    }

                    // Name + chips
                    VStack(alignment: .leading, spacing: 6) {
                        Text(child.name)
                            .font(.system(size: 17, weight: .bold))
                            .foregroundStyle(DobiTextPrim)
                            .lineLimit(1)
                        HStack(spacing: 6) {
                            InfoChip("Class \(child.grade)")
                            InfoChip(child.board)
                        }
                    }

                    Spacer()

                    // Progress ring
                    ProgressRing(percent: accuracyPct)

                    // Arrow
                    Image(systemName: "chevron.right")
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundStyle(DobiPrimary)
                }
                .padding(.horizontal, 18)
                .padding(.top, 18)
                .padding(.bottom, 16)

                // Divider
                Rectangle()
                    .fill(DividerCol)
                    .frame(height: 1)
                    .padding(.horizontal, 18)

                // Stat tiles row
                HStack(spacing: 0) {
                    TasksTile(activeCount: activeTasksCount)
                        .frame(maxWidth: .infinity)
                    VerticalSep()
                    WeeklyStreakTile(daysActive: weeklyDaysActive)
                        .frame(maxWidth: .infinity)
                    VerticalSep()
                    TodayLearningTile(isActiveToday: isActiveToday)
                        .frame(maxWidth: .infinity)
                }
                .padding(.vertical, 16)
                .padding(.horizontal, 18)
            }
        }
        .buttonStyle(.plain)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .shadow(color: Color.black.opacity(0.07), radius: 8, x: 0, y: 3)
    }
}

private struct InfoChip: View {
    let text: String
    init(_ text: String) { self.text = text }

    var body: some View {
        Text(text)
            .font(.system(size: 11, weight: .semibold))
            .foregroundStyle(DobiPrimary)
            .padding(.horizontal, 10)
            .padding(.vertical, 3)
            .background(DobiPrimarySoft)
            .clipShape(RoundedRectangle(cornerRadius: 8))
    }
}

private struct ProgressRing: View {
    let percent: Int?

    var body: some View {
        let pct = (percent ?? 0).clamped(to: 0...100)
        let fraction = Double(pct) / 100.0

        ZStack {
            Circle()
                .stroke(ProgressTrack, lineWidth: 5)
                .frame(width: 54, height: 54)
            if fraction > 0 {
                Circle()
                    .trim(from: 0, to: fraction)
                    .stroke(DobiPrimary, style: StrokeStyle(lineWidth: 5, lineCap: .round))
                    .rotationEffect(.degrees(-90))
                    .frame(width: 54, height: 54)
            }
            Text(percent != nil ? "\(pct)%" : "—")
                .font(.system(size: 12, weight: .bold))
                .foregroundStyle(DobiTextPrim)
        }
    }
}

private struct VerticalSep: View {
    var body: some View {
        Rectangle()
            .fill(DividerCol)
            .frame(width: 1, height: 50)
    }
}

// ── Stat tiles ─────────────────────────────────────────────────────────────────

private struct TasksTile: View {
    let activeCount: Int
    var hasPending: Bool { activeCount > 0 }

    var body: some View {
        VStack(spacing: 4) {
            HStack(spacing: 6) {
                ZStack {
                    Circle()
                        .fill(hasPending ? StatPinkBg : DobiSuccessBg)
                        .frame(width: 22, height: 22)
                    if hasPending {
                        Text("\(activeCount)")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundStyle(StatPink)
                    } else {
                        Image(systemName: "checkmark")
                            .font(.system(size: 10, weight: .bold))
                            .foregroundStyle(DobiSuccess)
                    }
                }
                Text(hasPending ? "\(activeCount) active" : "All done!")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(DobiTextPrim)
            }
            Text("Tasks by parents")
                .font(.system(size: 11))
                .foregroundStyle(DobiTextSec)
                .multilineTextAlignment(.center)
        }
    }
}

private struct WeeklyStreakTile: View {
    let daysActive: Int

    var body: some View {
        VStack(spacing: 6) {
            HStack(spacing: 4) {
                ForEach(0..<7, id: \.self) { i in
                    Circle()
                        .fill(i < daysActive ? DobiPrimary : DividerCol)
                        .frame(width: 7, height: 7)
                }
            }
            Text("\(daysActive) of 7")
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(DobiTextPrim)
            Text("Streaks this week")
                .font(.system(size: 11))
                .foregroundStyle(DobiTextSec)
                .multilineTextAlignment(.center)
        }
    }
}

private struct TodayLearningTile: View {
    let isActiveToday: Bool

    var body: some View {
        VStack(spacing: 4) {
            HStack(spacing: 6) {
                ZStack {
                    Circle()
                        .fill(isActiveToday ? DobiSuccessBg : StatOrangeBg)
                        .frame(width: 22, height: 22)
                    Image(systemName: isActiveToday ? "checkmark" : "clock.fill")
                        .font(.system(size: 10, weight: .bold))
                        .foregroundStyle(isActiveToday ? DobiSuccess : StatOrange)
                }
                Text(isActiveToday ? "Done!" : "Not yet")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(DobiTextPrim)
            }
            Text("Today's learning")
                .font(.system(size: 11))
                .foregroundStyle(DobiTextSec)
                .multilineTextAlignment(.center)
        }
    }
}

// ── Tasks needing attention card (mirrors Android TasksCard) ──────────────────

private struct TasksCard: View {
    let firstTask: ParentTaskItem
    let moreCount: Int
    let childName: String
    var onTap: () -> Void
    var onViewAll: () -> Void
    var onSendReminder: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack {
                Text("Tasks needing your attention")
                    .font(.system(size: 15, weight: .bold))
                    .foregroundStyle(DobiTextPrim)
                Spacer()
                if moreCount > 0 {
                    Button(action: onViewAll) {
                        HStack(spacing: 2) {
                            Text("View all")
                                .font(.system(size: 13, weight: .semibold))
                                .foregroundStyle(DobiPrimary)
                            Image(systemName: "chevron.right")
                                .font(.system(size: 13, weight: .semibold))
                                .foregroundStyle(DobiPrimary)
                        }
                    }
                    .buttonStyle(.plain)
                }
            }
            ParentActionableTaskRow(
                task: firstTask,
                childName: childName,
                onTap: onTap,
                onSendReminder: onSendReminder
            )
        }
        .padding(16)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 18))
        .shadow(color: .black.opacity(0.05), radius: 4, x: 0, y: 2)
    }
}

private struct ParentActionableTaskRow: View {
    let task: ParentTaskItem
    let childName: String
    var onTap: () -> Void
    var onSendReminder: () -> Void

    private var pillStyle: (bg: Color, fg: Color, label: String) {
        switch task.status {
        case "REQUESTED":      return (Color(hex: 0xFFF4E0), Color(hex: 0xB45309), "New request")
        case "PENDING_REVIEW": return (DobiPrimarySoft,      DobiPrimary,           "Needs review")
        default:               return (Color(hex: 0xFFE4E6), Color(hex: 0xBE123C), "Send reminder")
        }
    }

    private var headline: String {
        let scope = task.displayName.isEmpty ? "a topic" : task.displayName
        switch task.status {
        case "REQUESTED": return "Wants to study: \(scope)"
        case "PENDING_REVIEW":
            switch task.type {
            case "REVISE_CHAPTER": return "Submitted revision: \(scope)"
            case "WRITE_QA":       return "Submitted Q&A: \(scope)"
            case "OUR_QUESTIONS":  return "Quiz done: \(scope)"
            default:               return "Submitted: \(scope)"
            }
        default: return "Not done yet: \(scope)"
        }
    }

    private var subtitle: String {
        var parts: [String] = []
        if !childName.isEmpty { parts.append(childName) }
        parts.append(task.isUnitScope ? "Lesson" : "Concept")
        if !task.subject.isEmpty { parts.append(task.subject) }
        return parts.joined(separator: " · ")
    }

    var body: some View {
        let (pillBg, pillFg, pillLabel) = pillStyle
        let isReminder = task.status == "ASSIGNED"

        Button(action: onTap) {
            HStack(spacing: 12) {
                ZStack {
                    Circle().fill(pillBg).frame(width: 48, height: 48)
                    Text(task.subjectIcon.isEmpty ? "📚" : task.subjectIcon)
                        .font(.system(size: 24))
                }

                VStack(alignment: .leading, spacing: 2) {
                    Text(headline)
                        .font(.system(size: 14, weight: .bold))
                        .foregroundStyle(DobiTextPrim)
                        .lineLimit(2)
                        .fixedSize(horizontal: false, vertical: true)
                    Text(subtitle)
                        .font(.system(size: 12))
                        .foregroundStyle(DobiTextSec)
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                Button(action: isReminder ? onSendReminder : onTap) {
                    Text(pillLabel)
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundStyle(pillFg)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(pillBg)
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                }
                .buttonStyle(.plain)
            }
        }
        .buttonStyle(.plain)
    }
}

// ── Add more children card ─────────────────────────────────────────────────────

private struct AddMoreChildrenCard: View {
    var onAddChild: () -> Void

    var body: some View {
        HStack(spacing: 14) {
            ZStack {
                RoundedRectangle(cornerRadius: 14)
                    .fill(DobiPrimary)
                    .frame(width: 52, height: 52)
                Image(systemName: "shield.fill")
                    .font(.system(size: 22))
                    .foregroundStyle(.white)
            }

            VStack(alignment: .leading, spacing: 3) {
                Text("Add another child")
                    .font(.system(size: 15, weight: .bold))
                    .foregroundStyle(DobiTextPrim)
                Text("Track their progress and set learning goals")
                    .font(.system(size: 12))
                    .foregroundStyle(DobiTextSec)
                    .lineSpacing(2)
            }

            Spacer()

            Button(action: onAddChild) {
                HStack(spacing: 4) {
                    Image(systemName: "plus")
                        .font(.system(size: 12, weight: .semibold))
                    Text("Add")
                        .font(.system(size: 12, weight: .semibold))
                }
                .foregroundStyle(.white)
                .padding(.horizontal, 12)
                .padding(.vertical, 10)
                .background(DobiPrimary)
                .clipShape(RoundedRectangle(cornerRadius: 12))
            }
            .buttonStyle(.plain)
        }
        .padding(16)
        .background(DobiPrimarySoft)
        .clipShape(RoundedRectangle(cornerRadius: 18))
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// MARK: – Dashboard Tab
// ═══════════════════════════════════════════════════════════════════════════════

private let DashHeaderBg1     = Color(hex: 0xFDF6F0)
private let DashHeaderBg2     = Color(hex: 0xD6E6F7, alpha: 0.55)
private let DashDark          = Color(hex: 0x16213E)
private let DashMuted         = Color(hex: 0x6B7895)
private let SwitcherBorderCol = Color(hex: 0xE3ECFF)
private let DividerBlue       = Color(hex: 0xE3EAF8)
private let AddBorderCol      = Color(hex: 0xB7D2FF)

// InsightStatTile palette
private let StudiedTint      = Color(hex: 0x22C55E)
private let StudiedTintBg    = Color(hex: 0xDCFCE7)
private let StudiedCardBg    = Color(hex: 0xEFFAF1)
private let RevisedTint      = Color(hex: 0x7C5CF8)
private let RevisedTintBg    = Color(hex: 0xE8E4FF)
private let RevisedCardBg    = Color(hex: 0xF1EEFF)
private let AttentionTint    = Color(hex: 0xF59E0B)
private let AttentionTintBg  = Color(hex: 0xFFE7B5)
private let AttentionCardBg  = Color(hex: 0xFFF1DA)

// Weekly pattern
private let PatternLine      = Color(hex: 0xC8D1E1)
private let PatSkipBg        = Color(hex: 0xE5E7EB)
private let PatSkipTint      = Color(hex: 0x9CA3AF)
private let InsightBannerBg  = Color(hex: 0xEFF3FF)
private let InsightBannerBdr = Color(hex: 0xDCE4F7)

// DayKind is defined in AppModels.swift

// ── Main dashboard shell ───────────────────────────────────────────────────────

struct ParentDashboardTab: View {
    let children: [ChildProfile]
    var engagements: [String: ChildEngagement] = [:]
    var weekActivities: [String: [DayActivity]] = [:]
    var insights: [String: LearningInsight] = [:]
    var onNavigateToInsights: (String) -> Void = { _ in }
    var onNavigateToAddChild: () -> Void = {}
    var onNavigateToActivityDetail: (String, String, String) -> Void = { _, _, _ in }

    @State private var selectedId: String = ""
    @State private var weekPhotoUrls: [String] = []
    @State private var weekQuizLoading = false
    @State private var weekQuizResult: ADQuizResult? = nil
    @State private var weekSubjectiveLoading = false
    @State private var weekSubjectiveResult: ADSubjectiveResult? = nil

    private var selectedIndex: Int { children.firstIndex(where: { $0.id == selectedId }) ?? 0 }
    private var selected: ChildProfile? { children.first { $0.id == selectedId } ?? children.first }

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                DashTopBar(children: children, selectedId: $selectedId, onAddChild: onNavigateToAddChild)

                VStack(spacing: 16) {
                    if let child = selected {
                        let eng = engagements[child.id] ?? .empty
                        let week = weekActivities[child.id] ?? emptyWeek
                        let insight = insights[child.id] ?? .placeholder
                        DashChildSummaryCard(
                            child: child, index: selectedIndex, engagement: eng,
                            insight: insight,
                            onViewReport: { onNavigateToInsights(child.id) }
                        )
                        .padding(.horizontal, 16)
                        DashActivityCategoriesCard(onOpenActivity: { key in
                            onNavigateToActivityDetail(key, child.id, child.name)
                        })
                        .padding(.horizontal, 16)
                        DashWeeklyPattern(
                            firstName: child.name.components(separatedBy: " ").first ?? child.name,
                            weekData: eng.weekDayKinds,
                            week: week,
                            onItemTap: { route in handleWeekTap(route, childId: child.id) }
                        )
                        .padding(.horizontal, 16)
                    }
                }
                .padding(.top, 12)
                .padding(.bottom, 100)
                .background(
                    LinearGradient(
                        colors: [DashHeaderBg2, Color.white],
                        startPoint: .top,
                        endPoint: .bottom  // fades gradually all the way to the last card
                    )
                )
            }
        }
        .background(Color.white)
        // Cream bleeds behind the status bar to match the dashboard header top
        .overlay(alignment: .top) {
            DashHeaderBg1
                .ignoresSafeArea(edges: .top)
                .frame(height: 0)
        }
        .onAppear { if selectedId.isEmpty { selectedId = children.first?.id ?? "" } }
        // Photo sheet
        .sheet(isPresented: Binding(get: { !weekPhotoUrls.isEmpty }, set: { if !$0 { weekPhotoUrls = [] } })) {
            DashPhotoSheet(urls: weekPhotoUrls)
        }
        // Quiz result full-screen
        .fullScreenCover(item: $weekQuizResult) { result in
            QuizResultView(result: result, onBack: { weekQuizResult = nil })
        }
        // Subjective result sheet
        .sheet(isPresented: Binding(
            get: { weekSubjectiveResult != nil || weekSubjectiveLoading },
            set: { if !$0 { weekSubjectiveResult = nil } }
        )) {
            if weekSubjectiveLoading {
                ProgressView().tint(DobiPrimary).frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if let r = weekSubjectiveResult {
                DashSubjectiveSheet(result: r)
            }
        }
        // Quiz loading overlay
        .overlay {
            if weekQuizLoading {
                Color.black.opacity(0.35).ignoresSafeArea()
                ProgressView().tint(.white).scaleEffect(1.3)
            }
        }
    }

    private func handleWeekTap(_ route: ActivityDetailRoute, childId: String) {
        switch route {
        case .upload(_, let urls) where !urls.isEmpty:
            weekPhotoUrls = urls
        case .quizMcq(let unitId, let conceptId):
            Task {
                weekQuizLoading = true
                weekQuizResult = await FirebaseService.shared.fetchQuizResult(childId: childId, unitId: unitId, conceptId: conceptId)
                weekQuizLoading = false
            }
        case .quizSubjective(let resultId):
            Task {
                weekSubjectiveLoading = true
                weekSubjectiveResult = await FirebaseService.shared.fetchSubjectiveResult(resultId: resultId)
                weekSubjectiveLoading = false
            }
        default:
            break
        }
    }
}

// ── Top bar: cream header + lamp + floating child switcher ─────────────────────
//
// Layout matches Android ParentDashboardScreen exactly:
//  • Cream section (210pt): greeting left, illustration right (bottom-aligned)
//  • Switcher card (94pt) overlaps cream/blue boundary: 28pt in cream, 66pt in blue
//  • Blue section (90pt): accommodates card's lower portion + bottom breathing room
//
// Implemented as ZStack(top) with:
//   - background VStack  : solid cream then solid blue
//   - foreground VStack  : cream HStack + card, spacing=-28 creates the cream overlap

private struct DashTopBar: View {
    let children: [ChildProfile]
    @Binding var selectedId: String
    var onAddChild: () -> Void = {}

    var body: some View {
        ZStack(alignment: .top) {
            // ── Two-tone background ───────────────────────────────────────────
            VStack(spacing: 0) {
                DashHeaderBg1.frame(height: 170)   // cream — height matches image content bottom
                DashHeaderBg2                       // blue fills remaining
            }

            // ── Foreground: cream content + card ─────────────────────────────
            // spacing: -28 so card overlaps 28pt into the cream section
            VStack(spacing: -28) {
                // Cream area: ZStack mirrors Android's Box — image at BottomEnd.
                // Double-frame clip: scale image to 220pt then show only the bottom
                // 170pt, hiding the built-in empty top whitespace in the asset.
                ZStack {
                    // Greeting at top-left (mirrors Android Column offset y=40dp)
                    VStack(alignment: .leading, spacing: 6) {
                        Text("Hello! 👋")
                            .font(.system(size: 22, weight: .bold))
                            .foregroundStyle(DashDark)
                        Text("Let's support your child's\nlearning journey.")
                            .font(.system(size: 13))
                            .foregroundStyle(DashMuted)
                            .lineSpacing(4)
                    }
                    .padding(.leading, 24)
                    .padding(.top, 36)
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)

                    // Scale to 220pt, clip to bottom 170pt → removes ~50pt of empty top whitespace
                    Image("dashboard_header_bg")
                        .resizable()
                        .scaledToFit()
                        .frame(height: 220)
                        .frame(height: 170, alignment: .bottom)
                        .clipped()
                        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottomTrailing)
                }
                .frame(height: 170)
                .clipped()

                // Child switcher card (rendered after cream → on top; overlaps 28pt into cream)
                HStack(spacing: 0) {
                    ForEach(Array(children.enumerated()), id: \.element.id) { idx, child in
                        DashChildSwitcherItem(
                            name: child.name.components(separatedBy: " ").first ?? child.name,
                            avatarName: avatarImageName(index: idx),
                            isSelected: child.id == selectedId,
                            onTap: { selectedId = child.id }
                        )
                        .frame(maxWidth: .infinity)
                    }
                    Rectangle().fill(DividerBlue).frame(width: 1, height: 48)
                    Button(action: onAddChild) {
                        VStack(spacing: 4) {
                            ZStack {
                                Circle().strokeBorder(AddBorderCol, lineWidth: 1.5).frame(width: 48, height: 48)
                                Image(systemName: "plus")
                                    .font(.system(size: 20, weight: .medium))
                                    .foregroundStyle(DobiPrimary)
                            }
                            Text("Add")
                                .font(.system(size: 12, weight: .semibold))
                                .foregroundStyle(DashDark)
                        }
                    }
                    .buttonStyle(.plain)
                    .frame(maxWidth: .infinity)
                }
                .frame(height: 94)
                .background(Color.white)
                .clipShape(RoundedRectangle(cornerRadius: 28))
                .overlay(RoundedRectangle(cornerRadius: 28).strokeBorder(SwitcherBorderCol, lineWidth: 1))
                .shadow(color: Color(hex: 0x3B7BFF, alpha: 0.10), radius: 12, x: 0, y: 4)
                .padding(.horizontal, 20)
            }
        }
        .frame(height: 260) // 170 cream + 90 blue
    }
}

private func avatarImageName(index: Int) -> String {
    switch index % 3 {
    case 0: return "dashboard_avatar_ritika"
    case 1: return "dashboard_avatar_aarav"
    default: return "dashboard_avatar_vivaan"
    }
}

private struct DashChildSwitcherItem: View {
    let name: String
    let avatarName: String
    let isSelected: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(spacing: 5) {
                Image(avatarName)
                    .resizable()
                    .scaledToFill()
                    .frame(width: 48, height: 48)
                    .clipShape(Circle())
                    .overlay(Circle().strokeBorder(
                        isSelected ? DobiPrimary : Color(hex: 0xE6EAF1),
                        lineWidth: isSelected ? 2 : 1
                    ))
                Text(name)
                    .font(.system(size: 12, weight: .bold))
                    .foregroundStyle(isSelected ? DobiPrimary : DashDark)
                    .lineLimit(1)
                if isSelected {
                    RoundedRectangle(cornerRadius: 50).fill(DobiPrimary).frame(width: 28, height: 3)
                } else {
                    Color.clear.frame(height: 6)
                }
            }
        }
        .buttonStyle(.plain)
    }
}

// ── Child summary card ─────────────────────────────────────────────────────────

private struct DashChildSummaryCard: View {
    let child: ChildProfile
    let index: Int
    var engagement: ChildEngagement = .empty
    var insight: LearningInsight = .placeholder
    var onViewReport: () -> Void = {}

    private var daysStudied: Int { engagement.daysActiveThisWeek }
    private var weakSubject: String { engagement.weakSubject ?? "–" }
    private var weakHint: String {
        if let acc = engagement.weakSubjectAccuracyPct { return "\(acc)% accuracy" }
        return "Not enough data yet"
    }
    // Mirrors Android toDashChild.lastRevisedLabel
    private var lastActiveText: String {
        if engagement.lastActivityMs == 0 { return "No activity yet" }
        let daysAgo = Int((Int64(Date().timeIntervalSince1970 * 1000) - engagement.lastActivityMs) / (24 * 60 * 60 * 1000))
        switch daysAgo {
        case 0:  return "Today"
        case 1:  return "Yesterday"
        default: return "\(daysAgo) days ago"
        }
    }
    private var lastActiveHint: String {
        engagement.lastActivityMs == 0 ? "Start revising to build the habit." : "Keep the streak going!"
    }

    // Mirrors Android statusFor(child) — uses streakDays, not daysActiveThisWeek
    private var status: (text: String, tint: Color, bg: Color) {
        if engagement.streakDays >= 3 { return ("Learning Well",              DobiSuccess,         Color(hex: 0xDCFCE7)) }
        if engagement.streakDays  > 0 { return ("Needs revision consistency", AttentionTint,       Color(hex: 0xFFEFCC)) }
        return                                  ("Needs more attention",       Color(hex: 0xEF4444), Color(hex: 0xFFE4E0))
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            // ── Student header ────────────────────────────────────────────────
            HStack(alignment: .top, spacing: 0) {
                VStack(alignment: .leading, spacing: 0) {
                    Text(child.name.components(separatedBy: " ").first ?? child.name)
                        .font(.system(size: 28, weight: .bold))
                        .foregroundStyle(DobiTextPrim)
                        .lineLimit(1)
                    Spacer().frame(height: 4)
                    Text("Class \(child.grade) · \(child.board)")
                        .font(.system(size: 14))
                        .foregroundStyle(DobiTextSec)
                    Spacer().frame(height: 12)
                    // Status pill
                    HStack(spacing: 8) {
                        Circle().fill(status.tint).frame(width: 8, height: 8)
                        Text(status.text)
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundStyle(status.tint)
                    }
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .background(status.bg)
                    .clipShape(Capsule())
                }
                Spacer()
                Image("dashboard_hero")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 130, height: 130)
            }

            // ── 3 insight stat tiles ──────────────────────────────────────────
            HStack(spacing: 10) {
                InsightStatTile(
                    title: "\(daysStudied)/7",
                    subtitle: "Studied this week",
                    hint: daysStudied >= 5 ? "Great consistency!" : daysStudied >= 3 ? "Keep building" : "Aim for daily",
                    icon: "calendar.badge.checkmark",
                    tint: StudiedTint, tintBg: StudiedTintBg, cardBg: StudiedCardBg
                )
                InsightStatTile(
                    title: "Last active",
                    subtitle: lastActiveText,
                    hint: lastActiveHint,
                    icon: "arrow.clockwise",
                    tint: RevisedTint, tintBg: RevisedTintBg, cardBg: RevisedCardBg
                )
                InsightStatTile(
                    title: "Needs attention",
                    subtitle: weakSubject,
                    hint: weakHint,
                    icon: "book.fill",
                    tint: AttentionTint, tintBg: AttentionTintBg, cardBg: AttentionCardBg
                )
            }

            // ── Insight block (dynamic tone, mirrors Android ParentInsightBlock) ────
            DashInsightBlock(insight: insight)

            // ── Divider + view report row ─────────────────────────────────────
            Rectangle().fill(DividerCol).frame(height: 1)

            Button(action: onViewReport) {
                HStack(spacing: 10) {
                    ZStack {
                        Circle().fill(DobiPrimarySoft).frame(width: 28, height: 28)
                        Image(systemName: "chart.bar.fill")
                            .font(.system(size: 13))
                            .foregroundStyle(DobiPrimary)
                    }
                    Text("View full report")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(DobiPrimary)
                    Spacer()
                    Image(systemName: "chevron.right")
                        .font(.system(size: 15))
                        .foregroundStyle(DobiPrimary)
                }
                .padding(.vertical, 4)
            }
            .buttonStyle(.plain)
        }
        .padding(18)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 24))
        .overlay(RoundedRectangle(cornerRadius: 24).strokeBorder(AddBorderCol, lineWidth: 1))
    }
}

private struct InsightStatTile: View {
    let title: String
    let subtitle: String
    let hint: String
    let icon: String
    let tint: Color
    let tintBg: Color
    let cardBg: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            ZStack {
                Circle().fill(tintBg).frame(width: 32, height: 32)
                Image(systemName: icon).font(.system(size: 15)).foregroundStyle(tint)
            }
            Spacer().frame(height: 10)
            Text(title)
                .font(.system(size: 13, weight: .bold))
                .foregroundStyle(tint)
                .lineLimit(2)
                .fixedSize(horizontal: false, vertical: true)
            Spacer().frame(height: 4)
            Text(subtitle)
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(DobiTextPrim)
                .lineLimit(2)
                .fixedSize(horizontal: false, vertical: true)
            Spacer().frame(height: 4)
            Text(hint)
                .font(.system(size: 11))
                .foregroundStyle(DobiTextSec)
                .lineLimit(3)
                .fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .padding(12)
        .background(cardBg)
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}

// ── Dynamic insight block (mirrors Android ParentInsightBlock) ────────────────

private struct DashInsightBlock: View {
    let insight: LearningInsight

    private var palette: (bg: Color, accent: Color, divider: Color, circleBg: Color) {
        switch insight.tone {
        case .positive: return (Color(hex: 0xE6F8EC), Color(hex: 0x16A34A), Color(hex: 0xCDEED7), Color(hex: 0xD4F5DF))
        case .info:     return (Color(hex: 0xEEF3FF), Color(hex: 0x2F7DFF), Color(hex: 0xD6DEF2), Color(hex: 0xE8F1FF))
        case .amber:    return (Color(hex: 0xFFF4DD), Color(hex: 0xB45309), Color(hex: 0xF1E1BD), Color(hex: 0xFCE7B5))
        case .orange:   return (Color(hex: 0xFFE9D6), Color(hex: 0xC2410C), Color(hex: 0xF1CFB1), Color(hex: 0xFCD3B0))
        case .softRed:  return (Color(hex: 0xFFE6E1), Color(hex: 0xB91C1C), Color(hex: 0xF1C7C0), Color(hex: 0xFCCFC7))
        }
    }

    private var icon: String {
        switch insight.tone {
        case .positive: return "sparkles"
        case .info:     return "brain.head.profile"
        default:        return "info.circle"
        }
    }

    var body: some View {
        let (bg, accent, dividerColor, circleBg) = palette
        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .top, spacing: 12) {
                ZStack {
                    Circle().fill(circleBg).frame(width: 34, height: 34)
                    Image(systemName: icon)
                        .font(.system(size: 16))
                        .foregroundStyle(accent)
                }
                VStack(alignment: .leading, spacing: 4) {
                    Text(insight.title)
                        .font(.system(size: 15, weight: .bold))
                        .foregroundStyle(accent)
                    Text(insight.body)
                        .font(.system(size: 13))
                        .foregroundStyle(DobiTextPrim)
                        .lineSpacing(3)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }

            if let hint = insight.helpHint {
                Spacer().frame(height: 12)
                Rectangle().fill(dividerColor).frame(height: 1)
                Spacer().frame(height: 12)
                HStack(alignment: .top, spacing: 10) {
                    ZStack {
                        Circle().fill(circleBg).frame(width: 28, height: 28)
                        Image(systemName: "lightbulb")
                            .font(.system(size: 14))
                            .foregroundStyle(accent)
                    }
                    // "What can help: " in bold accent + hint in DobiTextPrim
                    // mirrors Android buildHelpText(AnnotatedString)
                    (Text("What can help: ").fontWeight(.bold).foregroundColor(accent)
                        + Text(hint).foregroundColor(DobiTextPrim))
                        .font(.system(size: 13))
                        .lineSpacing(3)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
            }
        }
        .padding(14)
        .background(bg)
        .clipShape(RoundedRectangle(cornerRadius: 18))
    }
}

// ── Activity categories card ───────────────────────────────────────────────────

private struct ActivityCat {
    let key: String; let icon: String; let label: String; let tint: Color; let tintBg: Color
}
private let dashCategories: [ActivityCat] = [
    ActivityCat(key: "school_work",    icon: "graduationcap.fill",          label: "School\nWork",    tint: Color(hex: 0x2F7DFF), tintBg: Color(hex: 0xE8F1FF)),
    ActivityCat(key: "home_work",      icon: "pencil.and.list.clipboard",   label: "Home\nWork",      tint: Color(hex: 0xF59E0B), tintBg: Color(hex: 0xFFF1C7)),
    ActivityCat(key: "self_learning",  icon: "brain.head.profile",          label: "Self\nLearning",  tint: Color(hex: 0x22C55E), tintBg: Color(hex: 0xDCFCE7)),
]

private struct DashActivityCategoriesCard: View {
    var onOpenActivity: (String) -> Void = { _ in }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("Explore activity")
                .font(.system(size: 15, weight: .bold))
                .foregroundStyle(DobiTextPrim)
            Spacer().frame(height: 3)
            Text("What did your child do this week?")
                .font(.system(size: 12))
                .foregroundStyle(DobiTextSec)
            Spacer().frame(height: 14)
            HStack(spacing: 10) {
                ForEach(dashCategories, id: \.label) { cat in
                    Button { onOpenActivity(cat.key) } label: {
                        VStack(spacing: 8) {
                            ZStack {
                                Circle().fill(Color.white).frame(width: 36, height: 36)
                                Image(systemName: cat.icon)
                                    .font(.system(size: 18))
                                    .foregroundStyle(cat.tint)
                            }
                            Text(cat.label)
                                .font(.system(size: 12, weight: .semibold))
                                .foregroundStyle(DobiTextPrim)
                                .multilineTextAlignment(.center)
                                .lineLimit(2)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .padding(.horizontal, 10)
                        .background(cat.tintBg)
                        .clipShape(RoundedRectangle(cornerRadius: 16))
                    }
                    .buttonStyle(.plain)
                }
            }
        }
        .padding(16)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .overlay(RoundedRectangle(cornerRadius: 20).strokeBorder(DividerCol, lineWidth: 1))
    }
}

// ── Weekly learning pattern ────────────────────────────────────────────────────

private struct DashWeeklyPattern: View {
    let firstName: String
    var weekData: [DayKind] = Array(repeating: .none, count: 7)
    var week: [DayActivity] = emptyWeek
    var onItemTap: (ActivityDetailRoute) -> Void = { _ in }

    @State private var selectedDay: Int? = nil

    private let dayLabels = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"]

    private var weekDates: [String] {
        let cal = Calendar.current
        let today = Date()
        let weekday = cal.component(.weekday, from: today) // 1=Sun..7=Sat
        let offsetFromMon = (weekday + 5) % 7
        let monday = cal.date(byAdding: .day, value: -offsetFromMon, to: today)!
        let fmt = DateFormatter(); fmt.dateFormat = "d MMM"
        return (0..<7).map { fmt.string(from: cal.date(byAdding: .day, value: $0, to: monday)!) }
    }

    private var patternInsight: (String, String) {
        let both = weekData.filter { $0 == .both }.count
        let rev  = weekData.filter { $0 == .revision }.count
        let skip = weekData.filter { $0 == .none }.count
        if both >= 2  { return ("Great balance this week!", "\(both) balanced learning days. Keep it up!") }
        if rev  >= 4  { return ("Strong revision habit", "Revision on \(rev) days — try adding homework.") }
        if skip >= 4  { return ("Several skipped days", "\(skip) days had no learning. Try one daily revision.") }
        return ("Mixed week", "Keep building daily habits — even short sessions help.")
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            // Header
            HStack(spacing: 6) {
                Text("This week's learning")
                    .font(.system(size: 16, weight: .bold))
                    .foregroundStyle(DobiTextPrim)
                Image(systemName: "info.circle")
                    .font(.system(size: 14))
                    .foregroundStyle(DobiTextSec)
            }
            Spacer().frame(height: 2)
            Text("\(firstName)'s daily subjects and activities")
                .font(.system(size: 12))
                .foregroundStyle(DobiTextSec)
            Spacer().frame(height: 18)

            // Day name + date headers
            let dates = weekDates
            HStack(spacing: 0) {
                ForEach(0..<7, id: \.self) { i in
                    VStack(spacing: 2) {
                        Text(dayLabels[i])
                            .font(.system(size: 13, weight: .bold))
                            .foregroundStyle(DobiTextPrim)
                        Text(dates[i])
                            .font(.system(size: 11))
                            .foregroundStyle(DobiTextSec)
                    }
                    .frame(maxWidth: .infinity)
                }
            }
            Spacer().frame(height: 12)

            // Zigzag node strip — each node is tappable
            ZigzagNodeStrip(weekData: weekData, onDayClick: { selectedDay = $0 })
                .frame(height: 70)

            Spacer().frame(height: 18)

            // Weekly insight banner
            let (insightTitle, insightBody) = patternInsight
            HStack(spacing: 12) {
                ZStack {
                    Circle().fill(Color.white).frame(width: 38, height: 38)
                        .overlay(Circle().strokeBorder(InsightBannerBdr, lineWidth: 1))
                    Image(systemName: "sparkles")
                        .font(.system(size: 16))
                        .foregroundStyle(DobiPrimary)
                }
                VStack(alignment: .leading, spacing: 2) {
                    Text(insightTitle)
                        .font(.system(size: 14, weight: .bold))
                        .foregroundStyle(DobiTextPrim)
                    Text(insightBody)
                        .font(.system(size: 12))
                        .foregroundStyle(DobiTextSec)
                        .lineLimit(3)
                }
                Spacer()
                Image(systemName: "checkmark.seal.fill")
                    .font(.system(size: 24))
                    .foregroundStyle(DobiPrimary)
            }
            .padding(14)
            .background(InsightBannerBg)
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .overlay(RoundedRectangle(cornerRadius: 16).strokeBorder(InsightBannerBdr, lineWidth: 1))

            Spacer().frame(height: 12)

            // Legend card
            HStack(spacing: 8) {
                WeekLegendCell(kind: .revision, title: "Revision",    desc: "Notes, quizzes, uploads")
                WeekLegendCell(kind: .homework, title: "Homework",    desc: "School homework logged")
                WeekLegendCell(kind: .both,     title: "Both",        desc: "Homework + Revision")
                WeekLegendCell(kind: .none,     title: "No activity", desc: "Nothing recorded")
            }
            .padding(12)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .overlay(RoundedRectangle(cornerRadius: 16).strokeBorder(DividerCol, lineWidth: 1))
        }
        .padding(18)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .overlay(RoundedRectangle(cornerRadius: 20).strokeBorder(DividerCol, lineWidth: 1))
        .sheet(isPresented: Binding(get: { selectedDay != nil }, set: { if !$0 { selectedDay = nil } })) {
            if let idx = selectedDay {
                DayDetailSheet(
                    dayIndex: idx,
                    activity: week.indices.contains(idx) ? week[idx] : .empty,
                    onDismiss: { selectedDay = nil },
                    onItemClick: { route in
                        selectedDay = nil
                        onItemTap(route)
                    }
                )
            }
        }
    }
}

private struct ZigzagNodeStrip: View {
    let weekData: [DayKind]
    var onDayClick: ((Int) -> Void)? = nil

    var body: some View {
        GeometryReader { geo in
            let nodeSize: CGFloat = 48
            let slotW = geo.size.width / 7

            ZStack(alignment: .topLeading) {
                // Cubic bezier dashed connector
                Canvas { ctx, size in
                    let evenY = nodeSize / 2
                    let oddY  = size.height - nodeSize / 2
                    let centers: [CGPoint] = (0..<7).map { i in
                        CGPoint(x: (CGFloat(i) + 0.5) * slotW, y: i % 2 == 0 ? evenY : oddY)
                    }
                    var path = Path()
                    for i in 0..<7 {
                        if i == 0 { path.move(to: centers[0]) }
                        else {
                            let p = centers[i - 1], q = centers[i]
                            let mx = (p.x + q.x) / 2
                            path.addCurve(to: q, control1: CGPoint(x: mx, y: p.y), control2: CGPoint(x: mx, y: q.y))
                        }
                    }
                    ctx.stroke(path, with: .color(PatternLine),
                               style: StrokeStyle(lineWidth: 2, lineCap: .round, dash: [3, 5]))
                }

                // Day nodes — each is tappable
                HStack(spacing: 0) {
                    ForEach(0..<7, id: \.self) { i in
                        Button { onDayClick?(i) } label: {
                            VStack(spacing: 0) {
                                if i % 2 == 0 {
                                    DayNode(kind: weekData[i])
                                    Spacer()
                                } else {
                                    Spacer()
                                    DayNode(kind: weekData[i])
                                }
                            }
                        }
                        .buttonStyle(.plain)
                        .frame(maxWidth: .infinity)
                    }
                }
            }
        }
    }
}

private struct DayNode: View {
    let kind: DayKind

    var body: some View {
        switch kind {
        case .revision:
            ZStack {
                Circle().fill(DobiPrimary).frame(width: 48, height: 48)
                Image(systemName: "pencil").font(.system(size: 20, weight: .medium)).foregroundStyle(.white)
            }
        case .homework:
            ZStack {
                Circle().fill(StudiedTint).frame(width: 48, height: 48)
                Image(systemName: "book.fill").font(.system(size: 20)).foregroundStyle(.white)
            }
        case .none:
            ZStack {
                Circle().fill(PatSkipBg).frame(width: 48, height: 48)
                Image(systemName: "minus").font(.system(size: 20, weight: .semibold)).foregroundStyle(PatSkipTint)
            }
        case .both:
            ZStack {
                Circle().strokeBorder(DobiPrimary, lineWidth: 2.5).frame(width: 48, height: 48)
                Circle().fill(StudiedTint).frame(width: 40, height: 40)
                Image(systemName: "book.fill").font(.system(size: 18)).foregroundStyle(.white)
            }
        }
    }
}

// MARK: - Day detail bottom sheet

private let weekDayFullLabels = ["Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday"]

private struct DayDetailSheet: View {
    let dayIndex: Int
    let activity: DayActivity
    let onDismiss: () -> Void
    let onItemClick: (ActivityDetailRoute) -> Void

    private func summaryText() -> String {
        let rev = activity.revisions.count
        let hw  = activity.homeworks.count
        if rev == 0 && hw == 0 { return "No activity recorded." }
        if rev > 0 && hw > 0   { return "\(rev) revision\(rev == 1 ? "" : "s") • \(hw) homework\(hw == 1 ? "" : "s")" }
        if rev > 0              { return "\(rev) revision\(rev == 1 ? "" : "s") done" }
        return "\(hw) homework\(hw == 1 ? "" : "s") done"
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            // Sheet drag handle
            Capsule()
                .fill(Color(hex: 0xE0E0E0))
                .frame(width: 40, height: 4)
                .frame(maxWidth: .infinity)
                .padding(.top, 12)
                .padding(.bottom, 16)

            // Day name + summary
            Text(weekDayFullLabels[safe: dayIndex] ?? "Day")
                .font(.system(size: 22, weight: .bold))
                .foregroundStyle(DobiTextPrim)
            Spacer().frame(height: 4)
            Text(summaryText())
                .font(.system(size: 13))
                .foregroundStyle(DobiTextSec)
            Spacer().frame(height: 20)

            // Content
            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 0) {
                    if activity.revisions.isEmpty && activity.homeworks.isEmpty {
                        // Empty state
                        HStack {
                            Spacer()
                            VStack(spacing: 8) {
                                Image(systemName: "minus.circle")
                                    .font(.system(size: 28))
                                    .foregroundStyle(DobiTextSec)
                                Text("No activity recorded this day")
                                    .font(.system(size: 13))
                                    .foregroundStyle(DobiTextSec)
                            }
                            .padding(20)
                            Spacer()
                        }
                        .background(Color(hex: 0xFAFBFD))
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                        .overlay(RoundedRectangle(cornerRadius: 14).strokeBorder(DividerCol, lineWidth: 1))
                    } else {
                        if !activity.revisions.isEmpty {
                            DaySection(
                                title: "Revision",
                                accent: DobiPrimary,
                                accentBg: DobiPrimarySoft,
                                icon: "pencil",
                                items: activity.revisions,
                                onItemClick: onItemClick
                            )
                        }
                        if !activity.revisions.isEmpty && !activity.homeworks.isEmpty {
                            Spacer().frame(height: 18)
                        }
                        if !activity.homeworks.isEmpty {
                            DaySection(
                                title: "Homework",
                                accent: DobiSuccess,
                                accentBg: DobiSuccessBg,
                                icon: "book.fill",
                                items: activity.homeworks,
                                onItemClick: onItemClick
                            )
                        }
                    }
                    Spacer().frame(height: 24)
                }
            }
        }
        .padding(.horizontal, 24)
        .presentationDetents([.large])
        .presentationDragIndicator(.hidden)
    }
}

private struct DaySection: View {
    let title: String
    let accent: Color
    let accentBg: Color
    let icon: String
    let items: [DayItem]
    let onItemClick: (ActivityDetailRoute) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 10) {
                ZStack {
                    Circle().fill(accentBg).frame(width: 28, height: 28)
                    Image(systemName: icon)
                        .font(.system(size: 14))
                        .foregroundStyle(accent)
                }
                Text(title)
                    .font(.system(size: 15, weight: .bold))
                    .foregroundStyle(accent)
            }

            ForEach(Array(items.enumerated()), id: \.offset) { _, item in
                DayItemRow(item: item, onTap: { onItemClick(item.route) })
            }
        }
    }
}

private struct DayItemRow: View {
    let item: DayItem
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 0) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(item.title)
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(DobiTextPrim)
                        .lineLimit(2)
                        .fixedSize(horizontal: false, vertical: true)
                    HStack(spacing: 6) {
                        if !item.subject.isEmpty {
                            Text(item.subject)
                                .font(.system(size: 12))
                                .foregroundStyle(DobiTextSec)
                        }
                        if !item.typeTag.isEmpty {
                            DayTypeTag(tag: item.typeTag)
                        }
                    }
                }
                Spacer(minLength: 8)
                Image(systemName: "chevron.right")
                    .font(.system(size: 14))
                    .foregroundStyle(Color(hex: 0xA0ADC2))
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
            .background(Color(hex: 0xFAFBFD))
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .overlay(RoundedRectangle(cornerRadius: 12).strokeBorder(DividerCol, lineWidth: 1))
        }
        .buttonStyle(.plain)
    }
}

private struct DayTypeTag: View {
    let tag: String

    private var colors: (Color, Color) {
        switch tag {
        case "Quiz":         return (Color(hex: 0x3B7BFF), Color(hex: 0xEAF0FF))
        case "Write Answer": return (Color(hex: 0x7C3AED), Color(hex: 0xF3EEFF))
        case "Class Work":   return (Color(hex: 0x0891B2), Color(hex: 0xE0F7FA))
        case "Self Study":   return (Color(hex: 0x059669), Color(hex: 0xE6F7F1))
        case "Homework":     return (Color(hex: 0xD97706), Color(hex: 0xFFF3E0))
        default:             return (DobiTextSec,          Color(hex: 0xF3F4F6))
        }
    }

    var body: some View {
        let (tint, bg) = colors
        Text(tag)
            .font(.system(size: 10, weight: .semibold))
            .foregroundStyle(tint)
            .padding(.horizontal, 7)
            .padding(.vertical, 2)
            .background(bg)
            .clipShape(RoundedRectangle(cornerRadius: 6))
    }
}

// MARK: - Photo / Subjective sheets (used by ParentDashboardTab for week item taps)

private struct DashPhotoSheet: View {
    let urls: [String]
    @State private var page = 0
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        ZStack(alignment: .topTrailing) {
            Color.black.ignoresSafeArea()
            TabView(selection: $page) {
                ForEach(Array(urls.enumerated()), id: \.offset) { idx, url in
                    AsyncImage(url: URL(string: url)) { phase in
                        switch phase {
                        case .success(let img): img.resizable().scaledToFit()
                        case .failure: Image(systemName: "photo").font(.system(size: 48)).foregroundStyle(.white.opacity(0.4))
                        default: ProgressView().tint(.white)
                        }
                    }.tag(idx)
                }
            }
            .tabViewStyle(.page)
            .indexViewStyle(.page(backgroundDisplayMode: .always))

            Button { dismiss() } label: {
                Image(systemName: "xmark.circle.fill").font(.system(size: 28)).foregroundStyle(.white.opacity(0.8)).padding(16)
            }
        }
    }
}

private struct DashSubjectiveSheet: View {
    let result: ADSubjectiveResult
    @Environment(\.dismiss) private var dismiss

    private var accent: Color {
        result.accuracyPct >= 80 ? DobiSuccess : result.accuracyPct >= 50 ? Color(hex: 0xF59E0B) : DobiError
    }

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(result.displayName).font(.system(size: 17, weight: .bold)).foregroundStyle(DobiTextPrim)
                    if !result.subject.isEmpty { Text(result.subject).font(.system(size: 12)).foregroundStyle(DobiTextSec) }
                }
                Spacer()
                Button { dismiss() } label: { Image(systemName: "xmark.circle.fill").font(.system(size: 24)).foregroundStyle(DobiTextSec) }
            }
            .padding(.horizontal, 20).padding(.top, 20).padding(.bottom, 16)

            HStack(spacing: 0) {
                VStack(spacing: 4) {
                    Text("\(result.correct)/\(result.attempted)").font(.system(size: 18, weight: .bold)).foregroundStyle(accent)
                    Text("Correct").font(.system(size: 11)).foregroundStyle(DobiTextSec)
                }.frame(maxWidth: .infinity)
                Divider().frame(height: 32)
                VStack(spacing: 4) {
                    Text("\(result.accuracyPct)%").font(.system(size: 18, weight: .bold)).foregroundStyle(accent)
                    Text("Accuracy").font(.system(size: 11)).foregroundStyle(DobiTextSec)
                }.frame(maxWidth: .infinity)
            }
            .padding(.vertical, 12).background(accent.opacity(0.08)).clipShape(RoundedRectangle(cornerRadius: 12))
            .padding(.horizontal, 16).padding(.bottom, 12)

            ScrollView {
                LazyVStack(spacing: 10) {
                    ForEach(Array(result.items.enumerated()), id: \.offset) { idx, qa in
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Q\(idx + 1). \(qa.questionText)").font(.system(size: 13, weight: .semibold)).foregroundStyle(DobiTextPrim)
                            if !qa.answerSeen.isEmpty {
                                Text(qa.answerSeen).font(.system(size: 12)).foregroundStyle(DobiTextPrim)
                                    .padding(10).frame(maxWidth: .infinity, alignment: .leading)
                                    .background(DobiPrimarySoft).clipShape(RoundedRectangle(cornerRadius: 8))
                            }
                        }
                        .padding(14).background(Color.white).clipShape(RoundedRectangle(cornerRadius: 14))
                        .overlay(RoundedRectangle(cornerRadius: 14).strokeBorder(DividerCol, lineWidth: 1))
                    }
                    Spacer().frame(height: 24)
                }
                .padding(.horizontal, 16).padding(.top, 4)
            }
        }
        .background(Color(hex: 0xF8FAFF).ignoresSafeArea())
        .presentationDetents([.large])
        .presentationDragIndicator(.visible)
    }
}

// Safe array subscript
private extension Array {
    subscript(safe index: Int) -> Element? {
        indices.contains(index) ? self[index] : nil
    }
}

private struct WeekLegendCell: View {
    let kind: DayKind; let title: String; let desc: String

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            SmallDayNode(kind: kind)
            Text(title)
                .font(.system(size: 11, weight: .bold))
                .foregroundStyle(DobiTextPrim)
            Text(desc)
                .font(.system(size: 10))
                .foregroundStyle(DobiTextSec)
                .lineLimit(3)
                .fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

private struct SmallDayNode: View {
    let kind: DayKind

    var body: some View {
        switch kind {
        case .revision:
            ZStack {
                Circle().fill(DobiPrimary).frame(width: 28, height: 28)
                Image(systemName: "pencil").font(.system(size: 12)).foregroundStyle(.white)
            }
        case .homework:
            ZStack {
                Circle().fill(StudiedTint).frame(width: 28, height: 28)
                Image(systemName: "book.fill").font(.system(size: 12)).foregroundStyle(.white)
            }
        case .none:
            ZStack {
                Circle().fill(PatSkipBg).frame(width: 28, height: 28)
                Image(systemName: "minus").font(.system(size: 12)).foregroundStyle(PatSkipTint)
            }
        case .both:
            ZStack {
                Circle().strokeBorder(DobiPrimary, lineWidth: 2).frame(width: 28, height: 28)
                Circle().fill(StudiedTint).frame(width: 22, height: 22)
                Image(systemName: "book.fill").font(.system(size: 10)).foregroundStyle(.white)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// MARK: – Settings / Profile Tab
// ═══════════════════════════════════════════════════════════════════════════════

private let SettingsBg      = Color(hex: 0xFFF8EE)
private let SettingsCard    = Color.white
private let SettingsBorder  = Color(hex: 0xEDEAE0)
private let SettingsDivider = Color(hex: 0xF1EEE2)
private let PrimaryBlue     = Color(hex: 0x3B7BFF)
private let PrimaryText     = Color(hex: 0x16213E)
private let SecondaryText   = Color(hex: 0x6B7895)
private let TertiaryText    = Color(hex: 0xA0ADC2)
private let ProfileAvatarBg = Color(hex: 0xFFE7B5)
private let PositiveGreen   = Color(hex: 0x16A34A)
private let PositiveSoft    = Color(hex: 0xE6F7EC)
private let AmberDeep       = Color(hex: 0xEA580C)
private let AmberSoft       = Color(hex: 0xFFEFD9)
private let PurpleDeep      = Color(hex: 0x7C5CF8)
private let PurpleSoft      = Color(hex: 0xEAE3FF)
private let DangerSoft      = Color(hex: 0xFEE2E2)
private let DangerText      = Color(hex: 0xDC2626)
private let SwitcherBorderS = Color(hex: 0xE6EAF1)
private let SwitcherDivS    = Color(hex: 0xE0E8F2)
private let AddBorderS      = Color(hex: 0xB8C5DA)

struct ParentSettingsTab: View {
    let children: [ChildProfile]
    var parentName: String = ""
    var parentPhone: String = ""
    var onEditProfile: () -> Void = {}
    var onStudyGoals: (String) -> Void = { _ in }
    var onAddChild: () -> Void = {}
    var onPrivacyData: () -> Void = {}
    var onFaq: () -> Void = {}
    var onHelpSupport: () -> Void = {}
    var onSignOut: () -> Void = {}

    @State private var weeklyReports = true
    @State private var whatsappReports = true
    @State private var selectedChildId = ""
    @State private var showLanguagePicker = false
    @AppStorage("selectedLanguage") private var selectedLanguage = "English"

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 14, pinnedViews: []) {
                Spacer().frame(height: 0).padding(.top, 8)

                // Profile card
                ProfileCard(name: parentName, phone: parentPhone, onTap: onEditProfile)
                    .padding(.horizontal, 16)

                // Children section
                VStack(alignment: .leading, spacing: 8) {
                    SettingsSectionLabel("Children")
                        .padding(.horizontal, 20)
                    ChildrenSwitcherCard(
                        children: children,
                        selectedId: $selectedChildId,
                        onAddChild: onAddChild
                    )
                    .padding(.horizontal, 16)
                }

                // Learning section
                VStack(alignment: .leading, spacing: 8) {
                    SettingsSectionLabel("Learning")
                        .padding(.horizontal, 20)
                    SettingsGroupCard {
                        SettingsRowItem(
                            icon: "clock.fill", iconTint: PositiveGreen, iconBg: PositiveSoft,
                            title: "Weekly Reports",
                            subtitle: "Get a weekly summary every Sunday"
                        ) {
                            Toggle("", isOn: $weeklyReports)
                                .labelsHidden()
                                .tint(PositiveGreen)
                        }
                        SettingsDividerLine()
                        SettingsRowItem(
                            icon: "phone.fill", iconTint: PositiveGreen, iconBg: PositiveSoft,
                            title: "WhatsApp Reports",
                            subtitle: "Receive progress updates on WhatsApp"
                        ) {
                            Toggle("", isOn: $whatsappReports)
                                .labelsHidden()
                                .tint(PositiveGreen)
                        }
                        SettingsDividerLine()
                        SettingsRowItem(
                            icon: "scope", iconTint: PurpleDeep, iconBg: PurpleSoft,
                            title: "Study Goals",
                            subtitle: "Assign topics and tasks to your child"
                        ) {
                            Image(systemName: "chevron.right")
                                .font(.system(size: 14))
                                .foregroundStyle(TertiaryText)
                        } onTap: {
                            let id = selectedChildId.isEmpty ? (children.first?.id ?? "") : selectedChildId
                            onStudyGoals(id)
                        }
                        SettingsDividerLine()
                        SettingsRowItem(
                            icon: "globe", iconTint: PrimaryBlue, iconBg: Color(hex: 0xEEF5FF),
                            title: "Language",
                            subtitle: "Choose app language"
                        ) {
                            HStack(spacing: 6) {
                                Text(selectedLanguage == "Hindi" ? "हिंदी" : "English")
                                    .font(.system(size: 13, weight: .semibold))
                                    .foregroundStyle(PrimaryBlue)
                                Image(systemName: "chevron.right")
                                    .font(.system(size: 14))
                                    .foregroundStyle(TertiaryText)
                            }
                        } onTap: { showLanguagePicker = true }
                    }
                    .padding(.horizontal, 16)
                }

                // Account section
                VStack(alignment: .leading, spacing: 8) {
                    SettingsSectionLabel("Account")
                        .padding(.horizontal, 20)
                    SettingsGroupCard {
                        SettingsRowItem(
                            icon: "lock.fill", iconTint: PositiveGreen, iconBg: PositiveSoft,
                            title: "Privacy & Data",
                            subtitle: "Manage your data and permissions"
                        ) {
                            Image(systemName: "chevron.right")
                                .font(.system(size: 14)).foregroundStyle(TertiaryText)
                        } onTap: { onPrivacyData() }
                        SettingsDividerLine()
                        SettingsRowItem(
                            icon: "questionmark.circle", iconTint: PrimaryBlue, iconBg: Color(hex: 0xEEF5FF),
                            title: "FAQ",
                            subtitle: "Common questions and answers"
                        ) {
                            Image(systemName: "chevron.right")
                                .font(.system(size: 14)).foregroundStyle(TertiaryText)
                        } onTap: { onFaq() }
                        SettingsDividerLine()
                        SettingsRowItem(
                            icon: "headphones", iconTint: AmberDeep, iconBg: AmberSoft,
                            title: "Help & Support",
                            subtitle: "Get in touch with us"
                        ) {
                            Image(systemName: "chevron.right")
                                .font(.system(size: 14)).foregroundStyle(TertiaryText)
                        } onTap: { onHelpSupport() }
                        SettingsDividerLine()
                        SettingsRowItem(
                            icon: "rectangle.portrait.and.arrow.right", iconTint: DangerText, iconBg: DangerSoft,
                            title: "Sign Out",
                            subtitle: "Log out from your account",
                            titleColor: DangerText
                        ) {
                            Image(systemName: "chevron.right")
                                .font(.system(size: 14)).foregroundStyle(TertiaryText)
                        } onTap: { onSignOut() }
                    }
                    .padding(.horizontal, 16)
                }

                // Trust footer
                HStack(spacing: 6) {
                    Image(systemName: "checkmark.circle.fill")
                        .font(.system(size: 12))
                        .foregroundStyle(TertiaryText)
                    Text("Your data is encrypted and never shared")
                        .font(.system(size: 11))
                        .foregroundStyle(SecondaryText)
                }
                .padding(.top, 4)

                Spacer().frame(height: 100)
            }
        }
        .background(SettingsBg)
        .onAppear { if selectedChildId.isEmpty { selectedChildId = children.first?.id ?? "" } }
        .onChange(of: children.map(\.id)) { ids in
            if selectedChildId.isEmpty, let first = ids.first { selectedChildId = first }
        }
        .sheet(isPresented: $showLanguagePicker) {
            LanguagePickerSheet(selectedLanguage: $selectedLanguage)
                .presentationDetents([.height(220)])
                .presentationDragIndicator(.visible)
        }
    }
}

private struct ProfileCard: View {
    var name: String = ""
    var phone: String = ""
    var onTap: () -> Void = {}

    var body: some View {
        Button(action: onTap) {
        HStack(spacing: 14) {
            // Avatar with edit badge
            ZStack(alignment: .bottomTrailing) {
                Circle()
                    .fill(ProfileAvatarBg)
                    .frame(width: 76, height: 76)
                    .overlay(
                        Image(systemName: "person.fill")
                            .font(.system(size: 38))
                            .foregroundStyle(Color(hex: 0xB45309))
                    )
                ZStack {
                    Circle()
                        .fill(Color(hex: 0xEEF5FF))
                        .frame(width: 24, height: 24)
                        .overlay(Circle().strokeBorder(SettingsCard, lineWidth: 2))
                    Image(systemName: "pencil")
                        .font(.system(size: 10, weight: .bold))
                        .foregroundStyle(PrimaryBlue)
                }
            }

            VStack(alignment: .leading, spacing: 2) {
                Text(name.isEmpty ? "Your Profile" : name)
                    .font(.system(size: 18, weight: .bold))
                    .foregroundStyle(PrimaryText)
                Text(phone.isEmpty ? "Tap to edit your name and details" : phone)
                    .font(.system(size: 12))
                    .foregroundStyle(SecondaryText)
                Spacer().frame(height: 6)
                // Verified badge
                HStack(spacing: 4) {
                    Image(systemName: "shield.fill")
                        .font(.system(size: 11))
                        .foregroundStyle(PositiveGreen)
                    Text("Parent Account")
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundStyle(PositiveGreen)
                }
                .padding(.horizontal, 8)
                .padding(.vertical, 4)
                .background(PositiveSoft)
                .clipShape(Capsule())
            }

            Spacer()

            Image(systemName: "chevron.right")
                .font(.system(size: 16))
                .foregroundStyle(TertiaryText)
        }
        .padding(14)
        .background(SettingsCard)
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .overlay(RoundedRectangle(cornerRadius: 20).strokeBorder(SettingsBorder, lineWidth: 1))
        .contentShape(RoundedRectangle(cornerRadius: 20))
        }
        .buttonStyle(.plain)
    }
}

private struct ChildrenSwitcherCard: View {
    let children: [ChildProfile]
    @Binding var selectedId: String
    var onAddChild: () -> Void

    var body: some View {
        HStack(spacing: 0) {
            ForEach(Array(children.enumerated()), id: \.element.id) { idx, child in
                Button { selectedId = child.id } label: {
                    VStack(spacing: 5) {
                        Image(avatarImageName(index: idx))
                            .resizable()
                            .scaledToFill()
                            .frame(width: 48, height: 48)
                            .clipShape(Circle())
                            .overlay(
                                Circle().strokeBorder(
                                    child.id == selectedId ? PrimaryBlue : SwitcherBorderS,
                                    lineWidth: child.id == selectedId ? 2 : 1
                                )
                            )
                        Text(child.name.components(separatedBy: " ").first ?? child.name)
                            .font(.system(size: 12, weight: .bold))
                            .foregroundStyle(child.id == selectedId ? PrimaryBlue : PrimaryText)
                            .lineLimit(1)
                        if child.id == selectedId {
                            RoundedRectangle(cornerRadius: 50)
                                .fill(PrimaryBlue)
                                .frame(width: 28, height: 3)
                        } else {
                            Color.clear.frame(height: 3)
                        }
                    }
                    .frame(maxWidth: .infinity)
                }
                .buttonStyle(.plain)
            }

            if !children.isEmpty {
                Rectangle()
                    .fill(SwitcherDivS)
                    .frame(width: 1, height: 48)
            }

            Button(action: onAddChild) {
                VStack(spacing: 5) {
                    ZStack {
                        Circle()
                            .strokeBorder(AddBorderS, lineWidth: 1.5)
                            .frame(width: 48, height: 48)
                        Image(systemName: "plus")
                            .font(.system(size: 20, weight: .medium))
                            .foregroundStyle(PrimaryBlue)
                    }
                    Text("Add")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(PrimaryText)
                    Color.clear.frame(height: 3)
                }
                .frame(maxWidth: .infinity)
            }
            .buttonStyle(.plain)
        }
        .frame(height: 94)
        .padding(.horizontal, 14)
        .padding(.vertical, 8)
        .background(SettingsCard)
        .clipShape(RoundedRectangle(cornerRadius: 28))
        .overlay(RoundedRectangle(cornerRadius: 28).strokeBorder(SwitcherBorderS, lineWidth: 1))
    }
}

private struct SettingsSectionLabel: View {
    let text: String
    init(_ text: String) { self.text = text }

    var body: some View {
        Text(text)
            .font(.system(size: 14, weight: .bold))
            .foregroundStyle(PrimaryText)
            .padding(.leading, 4)
            .padding(.top, 6)
    }
}

private struct SettingsGroupCard<Content: View>: View {
    let content: Content
    init(@ViewBuilder content: () -> Content) { self.content = content() }

    var body: some View {
        VStack(spacing: 0) { content }
            .background(SettingsCard)
            .clipShape(RoundedRectangle(cornerRadius: 18))
            .overlay(RoundedRectangle(cornerRadius: 18).strokeBorder(SettingsBorder, lineWidth: 1))
    }
}

private struct SettingsRowItem<Trailing: View>: View {
    let icon: String
    let iconTint: Color
    let iconBg: Color
    let title: String
    let subtitle: String
    var titleColor: Color = PrimaryText
    let trailing: Trailing
    var onTap: () -> Void = {}

    init(
        icon: String, iconTint: Color, iconBg: Color,
        title: String, subtitle: String,
        titleColor: Color = PrimaryText,
        @ViewBuilder trailing: () -> Trailing,
        onTap: @escaping () -> Void = {}
    ) {
        self.icon = icon; self.iconTint = iconTint; self.iconBg = iconBg
        self.title = title; self.subtitle = subtitle; self.titleColor = titleColor
        self.trailing = trailing(); self.onTap = onTap
    }

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 12) {
                ZStack {
                    Circle()
                        .fill(iconBg)
                        .frame(width: 38, height: 38)
                    Image(systemName: icon)
                        .font(.system(size: 17))
                        .foregroundStyle(iconTint)
                }
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.system(size: 15, weight: .bold))
                        .foregroundStyle(titleColor)
                    Text(subtitle)
                        .font(.system(size: 12))
                        .foregroundStyle(SecondaryText)
                        .lineSpacing(2)
                }
                Spacer()
                trailing
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 14)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

private struct SettingsDividerLine: View {
    var body: some View {
        Rectangle()
            .fill(SettingsDivider)
            .frame(height: 1)
            .padding(.leading, 64)
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// MARK: – Language picker sheet
// ═══════════════════════════════════════════════════════════════════════════════

private struct LanguagePickerSheet: View {
    @Binding var selectedLanguage: String
    @Environment(\.dismiss) private var dismiss

    private let options: [(label: String, value: String)] = [
        ("English", "English"),
        ("हिंदी (Hindi)", "Hindi"),
    ]

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                Text("Choose Language")
                    .font(.system(size: 17, weight: .bold))
                    .foregroundStyle(PrimaryText)
                Spacer()
                Button { dismiss() } label: {
                    Image(systemName: "xmark.circle.fill")
                        .font(.system(size: 22))
                        .foregroundStyle(TertiaryText)
                }
            }
            .padding(.horizontal, 20)
            .padding(.top, 20)
            .padding(.bottom, 14)

            Divider()

            ForEach(options, id: \.value) { opt in
                Button {
                    selectedLanguage = opt.value
                    dismiss()
                } label: {
                    HStack {
                        Text(opt.label)
                            .font(.system(size: 16, weight: .medium))
                            .foregroundStyle(PrimaryText)
                        Spacer()
                        if selectedLanguage == opt.value {
                            Image(systemName: "checkmark")
                                .font(.system(size: 14, weight: .semibold))
                                .foregroundStyle(PrimaryBlue)
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.vertical, 16)
                }
                .buttonStyle(.plain)
                if opt.value != options.last?.value {
                    Divider().padding(.leading, 20)
                }
            }
        }
        .background(Color.white)
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// MARK: – Bottom nav bar
// ═══════════════════════════════════════════════════════════════════════════════

private struct ParentBottomNavBar: View {
    @Binding var selected: ParentHomeView.ParentTab

    private let items: [(icon: String, label: String, tab: ParentHomeView.ParentTab)] = [
        ("person.2.fill", "Children", .children),
        ("chart.bar.fill", "Dashboard", .dashboard),
        ("person.fill", "Profile", .settings),
    ]

    var body: some View {
        HStack(spacing: 0) {
            ForEach(items, id: \.label) { item in
                Button {
                    withAnimation(.spring(duration: 0.25)) { selected = item.tab }
                } label: {
                    VStack(spacing: 4) {
                        Image(systemName: item.icon)
                            .font(.system(size: 22, weight: selected == item.tab ? .semibold : .regular))
                            .foregroundStyle(selected == item.tab ? DobiPrimary : DobiTextSec)
                        Text(item.label)
                            .font(.system(size: 11, weight: selected == item.tab ? .semibold : .medium))
                            .foregroundStyle(selected == item.tab ? DobiPrimary : DobiTextSec)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 8)
                    .background(
                        selected == item.tab
                            ? DobiPrimarySoft.clipShape(RoundedRectangle(cornerRadius: 14))
                            : Color.clear.clipShape(RoundedRectangle(cornerRadius: 14))
                    )
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 10)
        .background(Color.white.shadow(color: .black.opacity(0.06), radius: 0, x: 0, y: -1))
        .ignoresSafeArea(edges: .bottom)
    }
}

// ── Helpers ────────────────────────────────────────────────────────────────────

extension Comparable {
    func clamped(to range: ClosedRange<Self>) -> Self {
        min(max(self, range.lowerBound), range.upperBound)
    }
}

// ── Preview ────────────────────────────────────────────────────────────────────

#Preview {
    ParentHomeView()
        .environmentObject(AppState())
}
