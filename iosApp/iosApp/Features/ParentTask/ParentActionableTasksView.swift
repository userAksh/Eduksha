import SwiftUI

// All actionable parent tasks across all children, grouped by child name.
// Shown when the parent taps "View all" on the Tasks needing attention card.

private let ATBg       = Color(hex: 0xF5F7FB)
private let ATCard     = Color.white
private let ATTextPrim = Color.dobiTextPrimary
private let ATTextSec  = Color.dobiTextSecondary
private let ATTwelveH: Int64 = 12 * 60 * 60 * 1000

private func timeAgo(_ ms: Int64) -> String {
    guard ms > 0 else { return "" }
    let seconds = Int64(Date().timeIntervalSince1970 * 1000) - ms
    guard seconds >= 0 else { return "just now" }
    let mins  = seconds / 60000
    let hours = mins / 60
    let days  = hours / 24
    if days >= 1  { return days == 1  ? "1 day ago"  : "\(days) days ago" }
    if hours >= 1 { return hours == 1 ? "1 hour ago" : "\(hours) hours ago" }
    if mins >= 1  { return mins == 1  ? "1 min ago"  : "\(mins) mins ago" }
    return "just now"
}

struct ParentActionableTasksView: View {
    var onBack: () -> Void = {}
    var onTaskTap: (String) -> Void = { _ in }
    var onSendReminder: (String) -> Void = { _ in }

    @EnvironmentObject private var appState: AppState

    private var childById: [String: ChildProfile] {
        Dictionary(uniqueKeysWithValues: appState.children.map { ($0.id, $0) })
    }

    private var actionableTasks: [ParentTaskItem] {
        let now = Int64(Date().timeIntervalSince1970 * 1000)
        return appState.parentTasks
            .filter { task in
                switch task.status {
                case "REQUESTED", "PENDING_REVIEW": return true
                case "ASSIGNED":
                    return task.assignedAtMillis > 0 &&
                           now - task.assignedAtMillis >= ATTwelveH &&
                           (task.reminderSentAtMillis == 0 || now - task.reminderSentAtMillis >= ATTwelveH)
                default: return false
                }
            }
            .sorted { actionableTimestamp($0) < actionableTimestamp($1) }
    }

    // Groups preserve the sort order from above (oldest first within each child)
    private var grouped: [(child: ChildProfile?, childId: String, tasks: [ParentTaskItem])] {
        var seen: [String] = []
        var map: [String: [ParentTaskItem]] = [:]
        for task in actionableTasks {
            if map[task.childId] == nil { seen.append(task.childId) }
            map[task.childId, default: []].append(task)
        }
        return seen.map { id in (childById[id], id, map[id] ?? []) }
    }

    var body: some View {
        VStack(spacing: 0) {
            topBar
            if actionableTasks.isEmpty {
                emptyState
            } else {
                ScrollView(showsIndicators: false) {
                    VStack(spacing: 0) {
                        Spacer().frame(height: 12)
                        ForEach(grouped, id: \.childId) { group in
                            ChildSection(
                                child: group.child,
                                tasks: group.tasks,
                                onTaskTap: onTaskTap,
                                onSendReminder: onSendReminder
                            )
                            Spacer().frame(height: 16)
                        }
                        Spacer().frame(height: 32)
                    }
                    .padding(.horizontal, 18)
                }
            }
        }
        .background(ATBg.ignoresSafeArea())
        .navigationBarHidden(true)
    }

    private var topBar: some View {
        HStack(spacing: 12) {
            Button(action: onBack) {
                Image(systemName: "arrow.left")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundStyle(ATTextPrim)
                    .frame(width: 36, height: 36)
            }
            .buttonStyle(.plain)
            Text("Tasks needing attention")
                .font(.system(size: 17, weight: .bold))
                .foregroundStyle(ATTextPrim)
            Spacer()
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(ATBg)
    }

    private var emptyState: some View {
        VStack(spacing: 12) {
            Spacer()
            Text("✅")
                .font(.system(size: 40))
            Text("All caught up!")
                .font(.system(size: 17, weight: .bold))
                .foregroundStyle(ATTextPrim)
            Text("No tasks need your attention right now.")
                .font(.system(size: 14))
                .foregroundStyle(ATTextSec)
                .multilineTextAlignment(.center)
            Spacer()
        }
        .padding()
    }

    private func actionableTimestamp(_ task: ParentTaskItem) -> Int64 {
        switch task.status {
        case "PENDING_REVIEW":
            return task.submittedAtMillis > 0 ? task.submittedAtMillis : task.assignedAtMillis
        case "REQUESTED":
            return task.requestedAtMillis
        default:
            return task.reminderSentAtMillis > 0 ? task.reminderSentAtMillis : task.assignedAtMillis
        }
    }
}

// MARK: - Child section

private struct ChildSection: View {
    let child: ChildProfile?
    let tasks: [ParentTaskItem]
    var onTaskTap: (String) -> Void
    var onSendReminder: (String) -> Void

    private var avatarEmoji: String {
        switch child?.gender {
        case "girl":  return "👧"
        case "other": return "🧒"
        default:      return "👦"
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            // Child header
            HStack(spacing: 8) {
                Text(avatarEmoji).font(.system(size: 20))
                Text(child?.name ?? "Unknown")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundStyle(ATTextPrim)
            }
            .padding(.leading, 2)

            // Task rows
            VStack(spacing: 8) {
                ForEach(tasks, id: \.id) { task in
                    ATTaskRow(
                        task: task,
                        onTap: { onTaskTap(task.id) },
                        onSendReminder: { onSendReminder(task.id) }
                    )
                }
            }
        }
    }
}

// MARK: - Task row

private struct ATTaskRow: View {
    let task: ParentTaskItem
    var onTap: () -> Void
    var onSendReminder: () -> Void

    private var relevantTimestamp: Int64 {
        switch task.status {
        case "PENDING_REVIEW":
            return task.submittedAtMillis > 0 ? task.submittedAtMillis : task.assignedAtMillis
        case "REQUESTED":
            return task.requestedAtMillis
        default:
            return task.reminderSentAtMillis > 0 ? task.reminderSentAtMillis : task.assignedAtMillis
        }
    }

    private var pill: (bg: Color, fg: Color, label: String) {
        switch task.status {
        case "REQUESTED":      return (Color(hex: 0xFFF4E0), Color(hex: 0xB45309), "New request")
        case "PENDING_REVIEW": return (Color.dobiPrimarySoft, Color.dobiPrimary,   "Needs review")
        default:               return (Color(hex: 0xFFE4E6), Color(hex: 0xBE123C), "Send reminder")
        }
    }

    private var headline: String {
        let scope = task.displayName.isEmpty ? "a topic" : task.displayName
        switch task.status {
        case "REQUESTED":      return "Wants to study: \(scope)"
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

    var body: some View {
        HStack(spacing: 12) {
            // Subject icon
            ZStack {
                Circle().fill(Color(hex: 0xFFF4E0)).frame(width: 40, height: 40)
                Text(task.subjectIcon.isEmpty ? "📚" : task.subjectIcon)
                    .font(.system(size: 20))
            }

            // Text
            VStack(alignment: .leading, spacing: 4) {
                Text(headline)
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(ATTextPrim)
                    .lineLimit(2)
                HStack(spacing: 6) {
                    if !task.subject.isEmpty {
                        Text(task.subject)
                            .font(.system(size: 11))
                            .foregroundStyle(ATTextSec)
                    }
                    let ago = timeAgo(relevantTimestamp)
                    if !ago.isEmpty {
                        if !task.subject.isEmpty {
                            Text("·")
                                .font(.system(size: 11))
                                .foregroundStyle(ATTextSec)
                        }
                        Text(ago)
                            .font(.system(size: 11))
                            .foregroundStyle(ATTextSec)
                    }
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            // Status pill or action
            if task.status == "ASSIGNED" {
                Button(action: onSendReminder) {
                    Text("Remind")
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundStyle(pill.fg)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 5)
                        .background(pill.bg)
                        .clipShape(Capsule())
                }
                .buttonStyle(.plain)
            } else {
                Text(pill.label)
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(pill.fg)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 5)
                    .background(pill.bg)
                    .clipShape(Capsule())
            }
        }
        .padding(14)
        .background(ATCard)
        .clipShape(RoundedRectangle(cornerRadius: 14))
        .shadow(color: .black.opacity(0.04), radius: 3, x: 0, y: 1)
        .contentShape(Rectangle())
        .onTapGesture { if task.status != "ASSIGNED" { onTap() } }
    }
}
