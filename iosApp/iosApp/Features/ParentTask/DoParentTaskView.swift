import SwiftUI
import PhotosUI

// Mirrors Android DoParentTaskScreen — shown to the CHILD when they tap a parent-assigned task.
// Status routing (from child's perspective):
//   REQUESTED          → waiting for parent to pick task type
//   PENDING_REVIEW     → submitted, parent reviewing
//   ACCEPTED           → accepted, show praise
//   ASSIGNED + quiz    → QuizBody: "Start quiz"
//   ASSIGNED/REJECTED + upload → UploadBody: pick photos + submit

private let DPTBg        = Color(hex: 0xF8FAFF)
private let DPTPrimary   = Color(hex: 0x2F7DFF)
private let DPTTextPrim  = Color(hex: 0x1A2B4A)
private let DPTTextSec   = Color(hex: 0x6B7A99)
private let DPTBorder    = Color(hex: 0xE5EBF5)
private let DPTDisabled  = Color(hex: 0xCBD5E1)

struct DoParentTaskView: View {
    let taskId: String
    let childId: String
    var onBack: () -> Void = {}
    var onFinished: () -> Void = {}
    var onLaunchQuiz: (ParentTaskFull) -> Void = { _ in }

    @EnvironmentObject private var appState: AppState

    @State private var task: ParentTaskFull? = nil
    @State private var isLoading = true
    @State private var loadError: String? = nil

    // Upload state
    @State private var selectedItems: [PhotosPickerItem] = []
    @State private var selectedImages: [UIImage] = []
    @State private var isValidatingPhotos = false
    @State private var isSubmitting = false
    @State private var submitError: String? = nil

    var body: some View {
        VStack(spacing: 0) {
            topBar

            if isLoading {
                Spacer()
                ProgressView().tint(DPTPrimary)
                Spacer()
            } else if let err = loadError {
                Spacer()
                Text(err).font(.system(size: 14)).foregroundStyle(DPTTextSec).padding()
                Spacer()
            } else if let task {
                ScrollView(showsIndicators: false) {
                    VStack(alignment: .leading, spacing: 12) {
                        Spacer().frame(height: 8)
                        bodyContent(task: task)
                        Spacer().frame(height: 24)
                    }
                    .padding(.horizontal, 18)
                }
            }
        }
        .background(DPTBg.ignoresSafeArea())
        .navigationBarHidden(true)
        .task { await loadTask() }
        .onChange(of: selectedItems) { _ in loadImages() }
    }

    // MARK: Top bar

    private var topBar: some View {
        HStack(spacing: 12) {
            Button(action: onBack) {
                Image(systemName: "arrow.left")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundStyle(DPTTextPrim)
                    .frame(width: 36, height: 36)
            }
            .buttonStyle(.plain)
            Text("Your Task")
                .font(.system(size: 17, weight: .bold))
                .foregroundStyle(DPTTextPrim)
            Spacer()
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(DPTBg)
    }

    // MARK: Body routing (mirrors Android DoParentTaskScreen)

    @ViewBuilder
    private func bodyContent(task: ParentTaskFull) -> some View {
        switch task.status {
        case "REQUESTED":
            statusBody(
                task: task, emoji: "⏳",
                title: "Waiting for your parent",
                subtitle: "Your parent will pick a task and send it back. We'll let you know."
            )
        case "PENDING_REVIEW":
            statusBody(
                task: task, emoji: "📬",
                title: "Submitted!",
                subtitle: "Your parent will review this soon. Hang tight."
            )
        case "ACCEPTED":
            statusBody(
                task: task, emoji: "🎉",
                title: "Great job!",
                subtitle: task.parentReviewNote.isEmpty
                    ? "Your parent accepted your task."
                    : task.parentReviewNote
            )
        default:
            // ASSIGNED or REJECTED — show the action body
            if task.type == "OUR_QUESTIONS" {
                quizBody(task: task)
            } else {
                uploadBody(task: task)
            }
        }
    }

    // MARK: Status body

    @ViewBuilder
    private func statusBody(task: ParentTaskFull, emoji: String, title: String, subtitle: String) -> some View {
        DPTTaskHeaderCard(task: task)
        VStack(spacing: 10) {
            Text(emoji).font(.system(size: 36))
            Text(title)
                .font(.system(size: 16, weight: .bold))
                .foregroundStyle(DPTTextPrim)
            Text(subtitle)
                .font(.system(size: 13))
                .foregroundStyle(DPTTextSec)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(20)
        .background(Color(hex: 0xEFF6FF))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .overlay(RoundedRectangle(cornerRadius: 16).strokeBorder(Color(hex: 0xCFE0FF), lineWidth: 1))
    }

    // MARK: Quiz body (OUR_QUESTIONS)

    @ViewBuilder
    private func quizBody(task: ParentTaskFull) -> some View {
        Text("Your parent picked this task")
            .font(.system(size: 12))
            .foregroundStyle(DPTTextSec)

        DPTTaskHeaderCard(task: task)

        VStack(alignment: .leading, spacing: 6) {
            HStack(spacing: 10) {
                Text("🎯").font(.system(size: 22))
                Text("Answer \(task.mcqCount) questions")
                    .font(.system(size: 15, weight: .bold))
                    .foregroundStyle(DPTTextPrim)
            }
            Text("Your score will be sent to your parent for review.")
                .font(.system(size: 12))
                .foregroundStyle(DPTTextSec)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(Color(hex: 0xFFF7E6))
        .clipShape(RoundedRectangle(cornerRadius: 14))
        .overlay(RoundedRectangle(cornerRadius: 14).strokeBorder(Color(hex: 0xFEEAB6), lineWidth: 1))

        DPTPrimaryButton(label: "Start quiz", isLoading: false, enabled: true) {
            onLaunchQuiz(task)
        }
    }

    // MARK: Upload body (REVISE_CHAPTER / WRITE_QA / CUSTOM / REJECTED)

    @ViewBuilder
    private func uploadBody(task: ParentTaskFull) -> some View {
        Text("Your parent picked this task")
            .font(.system(size: 12))
            .foregroundStyle(DPTTextSec)

        DPTTaskHeaderCard(task: task)

        // Instruction card
        let instruction = instructionText(for: task)
        if !instruction.isEmpty {
            Text(instruction)
                .font(.system(size: 13))
                .foregroundStyle(DPTTextPrim)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(14)
                .background(Color(hex: 0xFFF7E6))
                .clipShape(RoundedRectangle(cornerRadius: 14))
                .overlay(RoundedRectangle(cornerRadius: 14).strokeBorder(Color(hex: 0xFEEAB6), lineWidth: 1))
        }

        // Assigned subjective questions
        if !task.assignedSubjectiveQs.isEmpty {
            VStack(alignment: .leading, spacing: 8) {
                Text("Questions from your parent")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(DPTTextSec)
                ForEach(Array(task.assignedSubjectiveQs.enumerated()), id: \.element.id) { i, q in
                    Text("\(i + 1). \(q.text)")
                        .font(.system(size: 13))
                        .foregroundStyle(DPTTextPrim)
                        .lineSpacing(3)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(12)
                        .background(Color.white)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                        .overlay(RoundedRectangle(cornerRadius: 12).strokeBorder(Color(hex: 0xE5E7EB), lineWidth: 1))
                }
            }
        }

        // Custom note
        if !task.customNote.isEmpty {
            VStack(alignment: .leading, spacing: 4) {
                Text("Note from your parent")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(DPTTextSec)
                Text(task.customNote)
                    .font(.system(size: 14))
                    .foregroundStyle(DPTTextPrim)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(14)
            .background(Color(hex: 0xEFF6FF))
            .clipShape(RoundedRectangle(cornerRadius: 14))
            .overlay(RoundedRectangle(cornerRadius: 14).strokeBorder(Color(hex: 0xCFE0FF), lineWidth: 1))
        }

        // Parent review note if rejected
        if task.status == "REJECTED", !task.parentReviewNote.isEmpty {
            VStack(alignment: .leading, spacing: 4) {
                Text("Parent's feedback")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(Color(hex: 0xB91C1C))
                Text(task.parentReviewNote)
                    .font(.system(size: 13))
                    .foregroundStyle(DPTTextPrim)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(14)
            .background(Color(hex: 0xFFF1F1))
            .clipShape(RoundedRectangle(cornerRadius: 14))
            .overlay(RoundedRectangle(cornerRadius: 14).strokeBorder(Color(hex: 0xFECACA), lineWidth: 1))
        }

        // Photo section
        Text("Add photos")
            .font(.system(size: 14, weight: .bold))
            .foregroundStyle(DPTTextPrim)
            .padding(.top, 4).padding(.leading, 4)

        if selectedImages.isEmpty {
            PhotosPicker(selection: $selectedItems, matching: .images, photoLibrary: .shared()) {
                VStack(spacing: 8) {
                    Image(systemName: "plus")
                        .font(.system(size: 28, weight: .semibold))
                        .foregroundStyle(DPTPrimary)
                    Text("Tap to add photos")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(DPTPrimary)
                }
                .frame(maxWidth: .infinity)
                .frame(height: 140)
                .background(Color(hex: 0xEFF6FF))
                .clipShape(RoundedRectangle(cornerRadius: 14))
                .overlay(RoundedRectangle(cornerRadius: 14).strokeBorder(Color(hex: 0xCFE0FF), lineWidth: 1.5))
            }
            .buttonStyle(.plain)
        } else {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(Array(selectedImages.enumerated()), id: \.offset) { idx, img in
                        ZStack(alignment: .topTrailing) {
                            Image(uiImage: img)
                                .resizable()
                                .scaledToFill()
                                .frame(width: 110, height: 110)
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                            Button {
                                selectedImages.remove(at: idx)
                                if idx < selectedItems.count { selectedItems.remove(at: idx) }
                            } label: {
                                Image(systemName: "xmark")
                                    .font(.system(size: 10, weight: .bold))
                                    .foregroundStyle(.white)
                                    .frame(width: 20, height: 20)
                                    .background(Color.black.opacity(0.6))
                                    .clipShape(Circle())
                            }
                            .buttonStyle(.plain)
                            .padding(4)
                        }
                    }
                    // Add more button
                    PhotosPicker(selection: $selectedItems, matching: .images, photoLibrary: .shared()) {
                        Image(systemName: "plus")
                            .font(.system(size: 24, weight: .semibold))
                            .foregroundStyle(DPTPrimary)
                            .frame(width: 110, height: 110)
                            .background(Color(hex: 0xEFF6FF))
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                            .overlay(RoundedRectangle(cornerRadius: 12).strokeBorder(Color(hex: 0xCFE0FF), lineWidth: 1))
                    }
                    .buttonStyle(.plain)
                }
            }
        }

        if let err = submitError {
            Text(err)
                .font(.system(size: 12))
                .foregroundStyle(Color(hex: 0xB91C1C))
                .padding(.leading, 4)
        }

        DPTPrimaryButton(
            label: isValidatingPhotos ? "Checking photos…" : (isSubmitting ? "Submitting…" : "Submit to parent"),
            isLoading: isValidatingPhotos || isSubmitting,
            enabled: !selectedImages.isEmpty && !isSubmitting && !isValidatingPhotos
        ) {
            submitPhotos(task: task)
        }
    }

    // MARK: Helpers

    private func instructionText(for task: ParentTaskFull) -> String {
        switch task.type {
        case "REVISE_CHAPTER":
            return "Revise the lesson, then take photos of your revision notes and upload them."
        case "WRITE_QA":
            return task.assignedSubjectiveQs.isEmpty
                ? "Write important Q&A from your textbook, then upload a photo."
                : "Answer these questions on paper, then upload a photo of your answers."
        case "CUSTOM":
            return "Read your parent's note above. When done, upload a photo as proof."
        default:
            return ""
        }
    }

    private func loadImages() {
        Task {
            var images: [UIImage] = []
            for item in selectedItems {
                if let data = try? await item.loadTransferable(type: Data.self),
                   let img = UIImage(data: data) {
                    images.append(img)
                }
            }
            await MainActor.run { selectedImages = images }
        }
    }

    private func loadTask() async {
        isLoading = true
        loadError = nil
        do {
            task = try await FirebaseService.shared.fetchParentTask(taskId: taskId)
        } catch {
            loadError = error.localizedDescription
        }
        isLoading = false
    }

    private func submitPhotos(task: ParentTaskFull) {
        guard !selectedImages.isEmpty, !isSubmitting, !isValidatingPhotos else { return }
        submitError = nil
        let grade = Int(appState.children.first { $0.id == childId }?.grade ?? "6") ?? 6
        Task {
            // Validate photos with Gemini before uploading (mirrors UploadWorkView)
            isValidatingPhotos = true
            let validationError = await ImageScopeValidator.shared.validate(
                subject: task.subject,
                grade: grade,
                topic: task.displayName,
                lessonContext: task.displayName,
                artefactLabel: "parent task work",
                images: selectedImages
            )
            isValidatingPhotos = false
            if let err = validationError {
                submitError = err
                return
            }

            isSubmitting = true
            do {
                try await FirebaseService.shared.submitByChildPhotos(
                    taskId: taskId, childId: task.childId, images: selectedImages)
                onFinished()
            } catch {
                submitError = "Upload failed: \(error.localizedDescription)"
                isSubmitting = false
            }
        }
    }
}

// MARK: - Shared subviews

private struct DPTTaskHeaderCard: View {
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
                    .foregroundStyle(DPTTextPrim)
                let scope = task.isUnitScope ? "Lesson" : "Concept"
                Text(task.subject.isEmpty ? scope : "\(scope) · \(task.subject)")
                    .font(.system(size: 12))
                    .foregroundStyle(DPTTextSec)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(14)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .overlay(RoundedRectangle(cornerRadius: 16).strokeBorder(DPTBorder, lineWidth: 1))
    }
}

private struct DPTPrimaryButton: View {
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
            .background(enabled ? DPTPrimary : DPTDisabled)
            .clipShape(RoundedRectangle(cornerRadius: 14))
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
    }
}
