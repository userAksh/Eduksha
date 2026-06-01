import SwiftUI

// MARK: - Screen

struct ParentTasksListView: View {
    let childId: String
    var onBack: () -> Void = {}
    var onTaskClick: (String) -> Void = { _ in }
    var onNavigateToEarnReward: () -> Void = {}

    @EnvironmentObject private var appState: AppState
    @State private var sendingReminderForTaskId: String = ""
    @State private var reminderSentForTaskId: String = ""

    private var myTasks: [ParentTaskItem] {
        appState.parentTasks.filter { $0.childId == childId }
    }
    private var activeTasks: [ParentTaskItem] {
        myTasks.filter { $0.status != "REQUESTED" }
    }
    private var requestedTasks: [ParentTaskItem] {
        myTasks.filter { $0.status == "REQUESTED" }
    }
    private var hasRedoPending: Bool {
        activeTasks.contains { $0.status == "REJECTED" }
    }

    var body: some View {
        VStack(spacing: 0) {
            // Navigation bar
            HStack {
                Button(action: onBack) {
                    Image(systemName: "arrow.left")
                        .font(.system(size: 18, weight: .medium))
                        .foregroundStyle(Color.dobiTextPrimary)
                }
                .buttonStyle(.plain)
                Spacer()
                Text("Tasks from Parents")
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(Color.dobiTextPrimary)
                Spacer()
                Image(systemName: "arrow.left").opacity(0) // balance
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 16)
            .background(Color.dobiPageBg)

            ScrollView {
                VStack(spacing: 12) {
                    ForEach(activeTasks, id: \.id) { task in
                        PTLTaskCard(task: task, onTap: { onTaskClick(task.id) })
                    }

                    ForEach(requestedTasks, id: \.id) { task in
                        PTLPendingCard(
                            task: task,
                            isSendingReminder: sendingReminderForTaskId == task.id,
                            reminderSent: reminderSentForTaskId == task.id,
                            onSendReminder: {
                                Task { @MainActor in
                                    sendingReminderForTaskId = task.id
                                    await FirebaseService.shared.sendReminderToParent(taskId: task.id)
                                    reminderSentForTaskId = task.id
                                    sendingReminderForTaskId = ""
                                }
                            }
                        )
                    }

                    if !hasRedoPending && requestedTasks.isEmpty {
                        PTLRequestNextRow(onTap: onNavigateToEarnReward)
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 8)
                .padding(.bottom, 40)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.dobiPageBg.ignoresSafeArea())
        .navigationBarHidden(true)
    }
}

// MARK: - Active task card

private struct PTLTaskCard: View {
    let task: ParentTaskItem
    let onTap: () -> Void

    private func title() -> String {
        let scope = task.displayName.isEmpty ? (task.subject.isEmpty ? "your topic" : task.subject) : task.displayName
        switch task.type {
        case "REVISE_CHAPTER": return "Revise: \(scope)"
        case "WRITE_QA":       return "Write Q&A: \(scope)"
        case "OUR_QUESTIONS":  return "Questions: \(scope)"
        default:               return "Task on: \(scope)"
        }
    }

    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 12) {
                HStack(spacing: 12) {
                    ZStack {
                        RoundedRectangle(cornerRadius: 12)
                            .fill(Color(hex: 0xFFF1E0))
                            .frame(width: 44, height: 44)
                        Text(task.subjectIcon.isEmpty ? "🎯" : task.subjectIcon)
                            .font(.system(size: 20))
                    }

                    VStack(alignment: .leading, spacing: 2) {
                        Text(title())
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundStyle(Color.dobiTextPrimary)
                            .multilineTextAlignment(.leading)
                            .lineLimit(2)
                        Text(task.subject.isEmpty ? "Task" : task.subject)
                            .font(.system(size: 12))
                            .foregroundStyle(Color.dobiTextSecondary)
                    }

                    Spacer()

                    Image(systemName: "chevron.right")
                        .font(.system(size: 14, weight: .medium))
                        .foregroundStyle(Color.dobiTextSecondary)
                }

                HStack {
                    if !task.subject.isEmpty {
                        HStack(spacing: 4) {
                            if !task.subjectIcon.isEmpty {
                                Text(task.subjectIcon).font(.system(size: 11))
                            }
                            Text(task.subject)
                                .font(.system(size: 11, weight: .semibold))
                                .foregroundStyle(Color(hex: 0xE67E22))
                        }
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(Color(hex: 0xFFF4E0))
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                    }
                    Spacer()
                    PTLStatusBadge(status: task.status)
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

// MARK: - Pending request card (REQUESTED status)

private struct PTLPendingCard: View {
    let task: ParentTaskItem
    let isSendingReminder: Bool
    let reminderSent: Bool
    let onSendReminder: () -> Void

    private static let remindThresholdMs: Int64 = 12 * 60 * 60 * 1000

    private var canRemind: Bool {
        let nowMs = Int64(Date().timeIntervalSince1970 * 1000)
        return task.requestedAtMillis > 0 && (nowMs - task.requestedAtMillis) >= Self.remindThresholdMs
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 14) {
                ZStack {
                    RoundedRectangle(cornerRadius: 12)
                        .fill(Color(hex: 0xDBEAFE))
                        .frame(width: 44, height: 44)
                    Text(task.subjectIcon.isEmpty ? "📚" : task.subjectIcon)
                        .font(.system(size: 20))
                }

                VStack(alignment: .leading, spacing: 2) {
                    Text("Task requested to parent")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(Color.dobiTextPrimary)
                    Text(task.displayName.isEmpty ? task.subject : task.displayName)
                        .font(.system(size: 12))
                        .foregroundStyle(Color.dobiTextSecondary)
                }
            }

            Text("We've notified your parent. They'll assign a task for you soon.")
                .font(.system(size: 12))
                .foregroundStyle(Color.dobiTextSecondary)
                .lineSpacing(4)

            if canRemind {
                if reminderSent {
                    HStack(spacing: 8) {
                        Image(systemName: "checkmark")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundStyle(Color(hex: 0x16A34A))
                        Text("Reminder sent to parent!")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundStyle(Color(hex: 0x15803D))
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 10)
                    .background(Color(hex: 0xDCFCE7))
                    .clipShape(RoundedRectangle(cornerRadius: 10))
                } else {
                    Button(action: onSendReminder) {
                        HStack(spacing: 8) {
                            if isSendingReminder {
                                ProgressView()
                                    .scaleEffect(0.8)
                                    .tint(Color.dobiPrimary)
                            } else {
                                Image(systemName: "bell.badge")
                                    .font(.system(size: 14))
                                    .foregroundStyle(Color.dobiPrimary)
                            }
                            Text("Remind Parent")
                                .font(.system(size: 13, weight: .semibold))
                                .foregroundStyle(Color.dobiPrimary)
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: 44)
                        .overlay(
                            RoundedRectangle(cornerRadius: 12)
                                .stroke(Color.dobiPrimary, lineWidth: 1.5)
                        )
                    }
                    .buttonStyle(.plain)
                    .disabled(isSendingReminder)
                }
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(hex: 0xF8FAFF))
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .shadow(color: Color.black.opacity(0.04), radius: 2, x: 0, y: 1)
    }
}

// MARK: - Request next task row

private struct PTLRequestNextRow: View {
    var onTap: () -> Void = {}

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 10) {
                Text("🎁").font(.system(size: 18))
                VStack(alignment: .leading, spacing: 2) {
                    Text("Request your next task")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(Color.dobiTextPrimary)
                    Text("Pick another lesson and earn another reward.")
                        .font(.system(size: 11))
                        .foregroundStyle(Color.dobiTextSecondary)
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.system(size: 14, weight: .medium))
                    .foregroundStyle(Color(hex: 0xE67E22))
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .background(Color(hex: 0xFFF7E6))
            .overlay(RoundedRectangle(cornerRadius: 14).stroke(Color(hex: 0xFEEAB6), lineWidth: 1))
            .clipShape(RoundedRectangle(cornerRadius: 14))
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Status badge (reused from ChildHomeView naming convention)

private struct PTLStatusBadge: View {
    let status: String

    private var config: (label: String, bg: Color, fg: Color) {
        switch status {
        case "ASSIGNED":       return ("To do",     Color(hex: 0xDBEAFE), Color.dobiPrimary)
        case "PENDING_REVIEW": return ("Submitted", Color(hex: 0xFEF3C7), Color(hex: 0xB45309))
        case "ACCEPTED":       return ("✓ Done",    Color(hex: 0xDCFCE7), Color(hex: 0x15803D))
        case "REJECTED":       return ("Redo",      Color(hex: 0xFEE2E2), Color(hex: 0xB91C1C))
        case "REQUESTED":      return ("Waiting…",  Color(hex: 0xF1F5F9), Color.dobiTextSecondary)
        default:               return (status,       Color.dobiPageBg,    Color.dobiTextSecondary)
        }
    }

    var body: some View {
        Text(config.label)
            .font(.system(size: 11, weight: .medium))
            .foregroundStyle(config.fg)
            .padding(.horizontal, 8)
            .padding(.vertical, 3)
            .background(config.bg)
            .clipShape(RoundedRectangle(cornerRadius: 6))
    }
}
