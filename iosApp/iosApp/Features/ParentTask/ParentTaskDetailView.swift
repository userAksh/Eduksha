import SwiftUI

// Mirrors Android ParentTaskDetailScreen exactly.
// Status routing: REQUESTED → AssignBody, PENDING_REVIEW → ReviewBody, else → AlreadyHandledBody.

private let PTBg        = Color(hex: 0xF8FAFF)
private let PTPrimary   = Color(hex: 0x2F7DFF)
private let PTPrimarySoft = Color(hex: 0xDBEAFE)
private let PTTextPrim  = Color(hex: 0x1A2B4A)
private let PTTextSec   = Color(hex: 0x6B7A99)
private let PTBorder    = Color(hex: 0xE5EBF5)
private let PTSelected  = Color(hex: 0xEFF6FF)
private let PTDisabled  = Color(hex: 0xCBD5E1)

struct ParentTaskDetailView: View {
    let taskId: String
    var onBack: () -> Void = {}
    var onFinished: () -> Void = {}

    @State private var task: ParentTaskFull? = nil
    @State private var isLoading = true
    @State private var loadError: String? = nil

    // Assign state
    @State private var selectedType: String? = nil
    @State private var customNote = ""
    @State private var mcqCount = 5

    // Review state
    @State private var reviewNote = ""

    // Shared submission state
    @State private var isSubmitting = false
    @State private var submitError: String? = nil

    var body: some View {
        VStack(spacing: 0) {
            topBar

            if isLoading {
                Spacer()
                ProgressView().tint(PTPrimary)
                Spacer()
            } else if let err = loadError {
                Spacer()
                Text(err).font(.system(size: 14)).foregroundStyle(PTTextSec).padding()
                Spacer()
            } else if let task {
                ScrollView {
                    VStack(alignment: .leading, spacing: 12) {
                        Spacer().frame(height: 8)
                        switch task.status {
                        case "REQUESTED":
                            assignBody(task: task)
                        case "PENDING_REVIEW":
                            reviewBody(task: task)
                        default:
                            alreadyHandledBody(task: task)
                        }
                        Spacer().frame(height: 24)
                    }
                    .padding(.horizontal, 18)
                }
            }
        }
        .background(PTBg.ignoresSafeArea())
        .navigationBarHidden(true)
        .task { await loadTask() }
    }

    // MARK: Top bar

    private var topBar: some View {
        HStack(spacing: 12) {
            Button(action: onBack) {
                Image(systemName: "arrow.left")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundStyle(PTTextPrim)
                    .frame(width: 36, height: 36)
            }
            .buttonStyle(.plain)
            Text("Request from your child")
                .font(.system(size: 17, weight: .bold))
                .foregroundStyle(PTTextPrim)
            Spacer()
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(PTBg)
    }

    // MARK: Assign body (REQUESTED)

    @ViewBuilder
    private func assignBody(task: ParentTaskFull) -> some View {
        Text("Your child wants to study this")
            .font(.system(size: 12))
            .foregroundStyle(PTTextSec)

        TaskHeaderCard(task: task)

        Text("Pick a task")
            .font(.system(size: 14, weight: .bold))
            .foregroundStyle(PTTextPrim)
            .padding(.top, 8).padding(.leading, 4)

        TaskTypeOption(
            emoji: "📖",
            title: "Revise the chapter",
            subtitle: "Child uploads photos of revision notes.",
            isSelected: selectedType == "REVISE_CHAPTER",
            onTap: { selectedType = "REVISE_CHAPTER" }
        )
        TaskTypeOption(
            emoji: "✍️",
            title: "Write Q&A from textbook",
            subtitle: "Child writes questions + answers and uploads the photo.",
            isSelected: selectedType == "WRITE_QA",
            onTap: { selectedType = "WRITE_QA" }
        )
        TaskTypeOption(
            emoji: "🎯",
            title: "Our set of questions",
            subtitle: "\(mcqCount) AI-generated MCQs on this \(task.isUnitScope ? "lesson" : "concept").",
            isSelected: selectedType == "OUR_QUESTIONS",
            onTap: { selectedType = "OUR_QUESTIONS" }
        ) {
            McqCountSelector(count: $mcqCount)
        }
        TaskTypeOption(
            emoji: "📝",
            title: "Custom task",
            subtitle: "Type any task — child uploads a photo as proof.",
            isSelected: selectedType == "CUSTOM",
            onTap: { selectedType = "CUSTOM" }
        ) {
            TextEditor(text: $customNote)
                .frame(height: 80)
                .padding(8)
                .background(Color(hex: 0xF8FAFF))
                .clipShape(RoundedRectangle(cornerRadius: 10))
                .overlay(RoundedRectangle(cornerRadius: 10).strokeBorder(PTBorder, lineWidth: 1))
                .font(.system(size: 13))
                .foregroundStyle(PTTextPrim)
                .padding(.top, 8)
                .overlay(alignment: .topLeading) {
                    if customNote.isEmpty {
                        Text("What do you want them to do?")
                            .font(.system(size: 13))
                            .foregroundStyle(PTTextSec)
                            .padding(.leading, 12)
                            .padding(.top, 16)
                            .allowsHitTesting(false)
                    }
                }
        }

        if let err = submitError {
            Text(err)
                .font(.system(size: 12))
                .foregroundStyle(Color(hex: 0xB91C1C))
                .padding(.leading, 4)
        }

        Spacer().frame(height: 8)

        let canAssign = selectedType != nil && !isSubmitting &&
            (selectedType != "CUSTOM" || !customNote.trimmingCharacters(in: .whitespaces).isEmpty)
        PrimaryButton(
            label: isSubmitting ? "Assigning…" : "Give this task",
            isLoading: isSubmitting,
            enabled: canAssign,
            action: assignTask
        )
    }

    // MARK: Review body (PENDING_REVIEW)

    @ViewBuilder
    private func reviewBody(task: ParentTaskFull) -> some View {
        Text("Your child submitted this task")
            .font(.system(size: 12))
            .foregroundStyle(PTTextSec)

        TaskHeaderCard(task: task)

        // Quiz score card
        if task.type == "OUR_QUESTIONS", let total = task.submissionTotal, total > 0 {
            VStack(alignment: .leading, spacing: 4) {
                Text("Quiz score")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(PTTextSec)
                Text("\(task.submissionScore ?? 0) / \(total) correct")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundStyle(PTTextPrim)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(14)
            .background(PTSelected)
            .clipShape(RoundedRectangle(cornerRadius: 14))
            .overlay(RoundedRectangle(cornerRadius: 14).strokeBorder(Color(hex: 0xCFE0FF), lineWidth: 1))
        }

        // Submission photos
        if !task.submissionPhotoUrls.isEmpty {
            Text("Photos uploaded")
                .font(.system(size: 13, weight: .bold))
                .foregroundStyle(PTTextPrim)
                .padding(.top, 4).padding(.leading, 4)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(task.submissionPhotoUrls, id: \.self) { url in
                        AsyncImage(url: URL(string: url)) { phase in
                            switch phase {
                            case .success(let img):
                                img.resizable().scaledToFill()
                                    .frame(width: 140, height: 140)
                                    .clipShape(RoundedRectangle(cornerRadius: 12))
                            case .failure:
                                RoundedRectangle(cornerRadius: 12)
                                    .fill(Color(hex: 0xE5EBF5))
                                    .frame(width: 140, height: 140)
                                    .overlay(Image(systemName: "photo").foregroundStyle(PTTextSec))
                            default:
                                RoundedRectangle(cornerRadius: 12)
                                    .fill(Color(hex: 0xE5EBF5))
                                    .frame(width: 140, height: 140)
                                    .overlay(ProgressView().tint(PTPrimary))
                            }
                        }
                    }
                }
            }
        }

        // Review note field
        Text("Note for your child (optional for accept, required for redo)")
            .font(.system(size: 13, weight: .bold))
            .foregroundStyle(PTTextPrim)
            .padding(.top, 4).padding(.leading, 4)

        ZStack(alignment: .topLeading) {
            TextEditor(text: $reviewNote)
                .frame(height: 100)
                .padding(8)
                .background(Color(hex: 0xF8FAFF))
                .clipShape(RoundedRectangle(cornerRadius: 10))
                .overlay(RoundedRectangle(cornerRadius: 10).strokeBorder(PTBorder, lineWidth: 1))
                .font(.system(size: 13))
                .foregroundStyle(PTTextPrim)
            if reviewNote.isEmpty {
                Text("Add a quick comment…")
                    .font(.system(size: 13))
                    .foregroundStyle(PTTextSec)
                    .padding(.leading, 12)
                    .padding(.top, 16)
                    .allowsHitTesting(false)
            }
        }

        if let err = submitError {
            Text(err)
                .font(.system(size: 12))
                .foregroundStyle(Color(hex: 0xB91C1C))
                .padding(.leading, 4)
        }

        HStack(spacing: 12) {
            SecondaryButton(
                label: "Ask to redo",
                isLoading: isSubmitting,
                action: rejectTask
            )
            PrimaryButton(
                label: isSubmitting ? "Saving…" : "Accept",
                isLoading: isSubmitting,
                enabled: !isSubmitting,
                action: acceptTask
            )
        }
    }

    // MARK: Already handled body

    @ViewBuilder
    private func alreadyHandledBody(task: ParentTaskFull) -> some View {
        TaskHeaderCard(task: task)
        let msg: String = {
            switch task.status {
            case "ASSIGNED":  return "Task assigned. Waiting for your child to submit."
            case "ACCEPTED":  return "You've already accepted this submission."
            case "REJECTED":  return "You've asked your child to redo this."
            default:          return "This task has been handled."
            }
        }()
        Text(msg)
            .font(.system(size: 13))
            .foregroundStyle(PTTextSec)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(16)
            .background(Color(hex: 0xF8FAFF))
            .clipShape(RoundedRectangle(cornerRadius: 14))
            .overlay(RoundedRectangle(cornerRadius: 14).strokeBorder(PTBorder, lineWidth: 1))
    }

    // MARK: Actions

    private func loadTask() async {
        isLoading = true
        loadError = nil
        do {
            let fetched = try await FirebaseService.shared.fetchParentTask(taskId: taskId)
            task = fetched
            mcqCount = fetched.isUnitScope ? 10 : 5
        } catch {
            loadError = error.localizedDescription
        }
        isLoading = false
    }

    private func assignTask() {
        guard let type = selectedType, !isSubmitting else { return }
        if type == "CUSTOM" && customNote.trimmingCharacters(in: .whitespaces).isEmpty { return }
        isSubmitting = true
        submitError = nil
        Task {
            do {
                try await FirebaseService.shared.assignByParent(
                    taskId: taskId,
                    type: type,
                    customNote: customNote.trimmingCharacters(in: .whitespaces),
                    mcqCount: mcqCount
                )
                onFinished()
            } catch {
                submitError = error.localizedDescription
                isSubmitting = false
            }
        }
    }

    private func acceptTask() {
        guard !isSubmitting else { return }
        isSubmitting = true
        submitError = nil
        Task {
            do {
                try await FirebaseService.shared.acceptByParent(
                    taskId: taskId,
                    reviewNote: reviewNote.trimmingCharacters(in: .whitespaces)
                )
                onFinished()
            } catch {
                submitError = error.localizedDescription
                isSubmitting = false
            }
        }
    }

    private func rejectTask() {
        let note = reviewNote.trimmingCharacters(in: .whitespaces)
        if note.isEmpty {
            submitError = "Please add a note so your child knows what to fix."
            return
        }
        guard !isSubmitting else { return }
        isSubmitting = true
        submitError = nil
        Task {
            do {
                try await FirebaseService.shared.rejectByParent(
                    taskId: taskId,
                    reviewNote: note
                )
                onFinished()
            } catch {
                submitError = error.localizedDescription
                isSubmitting = false
            }
        }
    }
}

// MARK: - Shared subviews

private struct TaskHeaderCard: View {
    let task: ParentTaskFull

    var body: some View {
        HStack(spacing: 12) {
            ZStack {
                Circle().fill(Color(hex: 0xFFF4E0)).frame(width: 44, height: 44)
                Text(task.subjectIcon.isEmpty ? "📚" : task.subjectIcon)
                    .font(.system(size: 22))
            }
            VStack(alignment: .leading, spacing: 2) {
                Text(task.displayName)
                    .font(.system(size: 15, weight: .bold))
                    .foregroundStyle(PTTextPrim)
                let scope = task.isUnitScope ? "Lesson" : "Concept"
                Text(task.subject.isEmpty ? scope : "\(scope) · \(task.subject)")
                    .font(.system(size: 12))
                    .foregroundStyle(PTTextSec)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(14)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .overlay(RoundedRectangle(cornerRadius: 16).strokeBorder(PTBorder, lineWidth: 1))
    }
}

private struct TaskTypeOption<Extra: View>: View {
    let emoji: String
    let title: String
    let subtitle: String
    let isSelected: Bool
    let onTap: () -> Void
    let extra: (() -> Extra)?

    init(emoji: String, title: String, subtitle: String, isSelected: Bool, onTap: @escaping () -> Void, @ViewBuilder extra: @escaping () -> Extra) {
        self.emoji = emoji; self.title = title; self.subtitle = subtitle
        self.isSelected = isSelected; self.onTap = onTap; self.extra = extra
    }

    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 0) {
                HStack(spacing: 10) {
                    Text(emoji).font(.system(size: 22))
                    VStack(alignment: .leading, spacing: 2) {
                        Text(title)
                            .font(.system(size: 14, weight: .bold))
                            .foregroundStyle(PTTextPrim)
                        Text(subtitle)
                            .font(.system(size: 12))
                            .foregroundStyle(PTTextSec)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    if isSelected {
                        Image(systemName: "checkmark")
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundStyle(PTPrimary)
                    }
                }
                if isSelected, let extra {
                    extra()
                }
            }
            .padding(14)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(isSelected ? PTSelected : Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 14))
            .overlay(
                RoundedRectangle(cornerRadius: 14)
                    .strokeBorder(isSelected ? PTPrimary : PTBorder, lineWidth: isSelected ? 1.5 : 1)
            )
        }
        .buttonStyle(.plain)
    }
}

// Convenience init when there's no extra view
extension TaskTypeOption where Extra == EmptyView {
    init(emoji: String, title: String, subtitle: String, isSelected: Bool, onTap: @escaping () -> Void) {
        self.init(emoji: emoji, title: title, subtitle: subtitle, isSelected: isSelected, onTap: onTap) { EmptyView() }
    }
}

private struct McqCountSelector: View {
    @Binding var count: Int

    var body: some View {
        HStack {
            Text("Number of questions")
                .font(.system(size: 12))
                .foregroundStyle(PTTextSec)
                .frame(maxWidth: .infinity, alignment: .leading)
            StepButton(icon: "minus", enabled: count > 5) {
                if count > 5 { count -= 1 }
            }
            Text("\(count)")
                .font(.system(size: 14, weight: .bold))
                .foregroundStyle(PTTextPrim)
                .frame(width: 32, alignment: .center)
            StepButton(icon: "plus", enabled: count < 10) {
                if count < 10 { count += 1 }
            }
        }
        .padding(.top, 10)
    }
}

private struct StepButton: View {
    let icon: String
    let enabled: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: icon)
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(.white)
                .frame(width: 28, height: 28)
                .background(enabled ? PTPrimary : PTDisabled)
                .clipShape(RoundedRectangle(cornerRadius: 8))
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
    }
}

private struct PrimaryButton: View {
    let label: String
    let isLoading: Bool
    let enabled: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Group {
                if isLoading {
                    ProgressView().tint(.white).scaleEffect(0.9)
                } else {
                    Text(label)
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(.white)
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: 50)
            .background(enabled ? PTPrimary : PTDisabled)
            .clipShape(RoundedRectangle(cornerRadius: 14))
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
    }
}

private struct SecondaryButton: View {
    let label: String
    let isLoading: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 6) {
                Image(systemName: "xmark")
                    .font(.system(size: 13, weight: .semibold))
                Text(label)
                    .font(.system(size: 14, weight: .semibold))
            }
            .foregroundStyle(PTTextPrim)
            .frame(maxWidth: .infinity)
            .frame(height: 50)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 14))
            .overlay(RoundedRectangle(cornerRadius: 14).strokeBorder(PTDisabled, lineWidth: 1.5))
        }
        .buttonStyle(.plain)
        .disabled(isLoading)
    }
}
