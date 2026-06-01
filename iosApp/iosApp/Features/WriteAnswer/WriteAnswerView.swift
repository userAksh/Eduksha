import SwiftUI
import PhotosUI
import FirebaseFunctions

// MARK: - Local types

private enum WAPhase {
    case modePicker, collectingNotebook, collectingTextbook
    case generatingQuestions, collectingPerQuestion
    case grading, results, error
}

private enum WAMode { case notebook, generated, aiTextbook }

private struct WAQuestion: Identifiable {
    let id: String
    let text: String
}

private struct WAAnswerGrade {
    let questionId: String
    let questionSeen: String
    let isAttempted: Bool
    let isCorrect: Bool
    let softVerdict: String
}

private struct WAGradingResult {
    let grades: [WAAnswerGrade]
    var attemptedCount: Int { grades.filter { $0.isAttempted }.count }
    var correctCount: Int { grades.filter { $0.isCorrect }.count }
    var accuracy: Double {
        attemptedCount == 0 ? 0 : Double(correctCount) / Double(attemptedCount)
    }
}

// MARK: - Main view

struct WriteAnswerView: View {
    let displayName: String
    let subject: String
    let grade: Int
    let scopeText: String
    let childId: String
    var canChangeMode: Bool = true
    var onBack: () -> Void = {}
    var onLaunchAntiCheatQuiz: (String, String) -> Void = { _, _ in }

    @State private var phase: WAPhase = .modePicker
    @State private var mode: WAMode? = nil
    @State private var notebookPhotos: [UIImage] = []
    @State private var textbookPhotos: [UIImage] = []
    @State private var questions: [WAQuestion] = []
    @State private var photosByQuestion: [String: [UIImage]] = [:]
    @State private var result: WAGradingResult? = nil
    @State private var errorMsg: String? = nil
    @State private var isGeneratingAntiCheat = false

    var body: some View {
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
                Text("Write Answers")
                    .font(.dobiTitle3())
                    .foregroundStyle(Color.dobiTextPrimary)
                Spacer()
                Color.clear.frame(width: 36, height: 36)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(Color.white)

            phaseContent
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .padding(.horizontal, 20)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.white.ignoresSafeArea())
        .navigationBarHidden(true)
    }

    @ViewBuilder
    private var phaseContent: some View {
        switch phase {
        case .modePicker:
            WAModePickerView(
                displayName: displayName,
                onNotebook: {
                    mode = .notebook
                    phase = .collectingNotebook
                },
                onGenerated: { pickGeneratedMode() }
            )
        case .collectingNotebook:
            WANotebookCollectView(
                displayName: displayName,
                photos: notebookPhotos,
                errorMsg: errorMsg,
                canChangeMode: canChangeMode,
                onAdd: { imgs in notebookPhotos.append(contentsOf: imgs) },
                onRemove: { i in notebookPhotos.remove(at: i); errorMsg = nil },
                onSubmit: { submitNotebook() },
                onChangeMode: { goBackToModePicker() }
            )
        case .collectingTextbook:
            WATextbookCollectView(
                displayName: displayName,
                photos: textbookPhotos,
                errorMsg: errorMsg,
                canChangeMode: canChangeMode,
                onAdd: { imgs in textbookPhotos.append(contentsOf: imgs) },
                onRemove: { i in textbookPhotos.remove(at: i); errorMsg = nil },
                onSubmit: { submitTextbook() },
                onChangeMode: { goBackToModePicker() }
            )
        case .generatingQuestions:
            WALoadingView(message: "Cooking up some questions for you…")
        case .collectingPerQuestion:
            WAPerQuestionView(
                displayName: displayName,
                questions: questions,
                photosByQuestion: photosByQuestion,
                errorMsg: errorMsg,
                canChangeMode: canChangeMode,
                canSubmit: photosByQuestion.values.contains { !$0.isEmpty },
                onPhotoAdded: { qId, img in
                    var existing = photosByQuestion[qId] ?? []
                    existing.append(img)
                    photosByQuestion[qId] = existing
                },
                onSubmit: { submitGenerated() },
                onChangeMode: { goBackToModePicker() }
            )
        case .grading:
            WALoadingView(message: isGeneratingAntiCheat ? "Preparing your quiz…" : "Reading your answers…")
        case .results:
            if let r = result {
                WAResultsView(
                    displayName: displayName,
                    result: r,
                    onLaunchQuiz: { launchAntiCheatQuiz() },
                    onDone: onBack
                )
            }
        case .error:
            WAErrorView(message: errorMsg ?? "Something went wrong.", onBack: onBack)
        }
    }

    // MARK: - Logic

    private func pickGeneratedMode() {
        mode = .generated
        phase = .generatingQuestions
        Task { @MainActor in
            do {
                let qs = try await callGenerateSubjectiveQuestions()
                questions = qs
                phase = .collectingPerQuestion
            } catch {
                errorMsg = friendlyError(error)
                phase = .error
            }
        }
    }

    private func submitNotebook() {
        guard !notebookPhotos.isEmpty else { return }
        phase = .grading
        errorMsg = nil
        Task { @MainActor in
            do {
                let r = try await callGradeNotebookAnswers()
                result = r
                phase = .results
            } catch {
                errorMsg = friendlyError(error)
                phase = .collectingNotebook
            }
        }
    }

    private func submitTextbook() {
        guard !textbookPhotos.isEmpty else { return }
        mode = .aiTextbook
        phase = .generatingQuestions
        errorMsg = nil
        Task { @MainActor in
            do {
                let qs = try await callGenerateSubjectiveQuestions()
                questions = qs
                phase = .collectingPerQuestion
            } catch {
                errorMsg = friendlyError(error)
                phase = .collectingTextbook
            }
        }
    }

    private func submitGenerated() {
        guard photosByQuestion.values.contains(where: { !$0.isEmpty }) else { return }
        phase = .grading
        errorMsg = nil
        Task { @MainActor in
            do {
                let r = try await callGradeStructuredAnswers()
                result = r
                phase = .results
            } catch {
                errorMsg = friendlyError(error)
                phase = .collectingPerQuestion
            }
        }
    }

    private func launchAntiCheatQuiz() {
        isGeneratingAntiCheat = true
        let cacheKey = "wa_anticheat_\(UUID().uuidString)"
        Task { @MainActor in
            do {
                let qs = try await FirebaseService.shared.generateQuizQuestions(
                    conceptId: cacheKey,
                    conceptName: displayName,
                    conceptDescription: scopeText,
                    subject: subject,
                    grade: grade,
                    unitName: displayName
                )
                GeneratedQuizCache.shared.put(cacheKey, qs)
                isGeneratingAntiCheat = false
                onLaunchAntiCheatQuiz(cacheKey, displayName)
            } catch {
                isGeneratingAntiCheat = false
                errorMsg = friendlyError(error)
            }
        }
    }

    private func goBackToModePicker() {
        mode = nil
        errorMsg = nil
        notebookPhotos = []
        textbookPhotos = []
        questions = []
        photosByQuestion = [:]
        phase = .modePicker
    }

    // MARK: - Cloud Function calls (generateSubjectiveQuestions / gradeNotebookAnswers /
    //         gradeStructuredAnswers are not yet deployed — will fail gracefully)

    private func callGenerateSubjectiveQuestions() async throws -> [WAQuestion] {
        let fn = Functions.functions().httpsCallable("generateSubjectiveQuestions")
        let data: [String: Any] = [
            "scopeText": scopeText,
            "subject": subject,
            "grade": grade,
            "targetCount": computeTargetCount()
        ]
        let r = try await fn.call(data)
        guard let dict = r.data as? [String: Any],
              let raw = dict["questions"] as? [[String: Any]] else {
            throw WAError.invalidResponse
        }
        let qs = raw.compactMap { m -> WAQuestion? in
            guard let text = m["text"] as? String else { return nil }
            return WAQuestion(id: m["id"] as? String ?? UUID().uuidString, text: text)
        }
        if qs.isEmpty { throw WAError.noQuestions }
        return qs
    }

    private func encodeImages(_ images: [UIImage]) -> [String] {
        images.compactMap { img in
            let size = img.size
            let scale = min(1024.0 / max(size.width, size.height), 1.0)
            let newSize = scale < 1.0
                ? CGSize(width: size.width * scale, height: size.height * scale)
                : size
            let downsampled = UIGraphicsImageRenderer(size: newSize)
                .image { _ in img.draw(in: CGRect(origin: .zero, size: newSize)) }
            return downsampled.jpegData(compressionQuality: 0.75)?.base64EncodedString()
        }
    }

    private func callGradeNotebookAnswers() async throws -> WAGradingResult {
        let fn = Functions.functions(region: "us-central1").httpsCallable("gradeNotebookAnswers")
        let b64Images = encodeImages(notebookPhotos)
        guard !b64Images.isEmpty else { throw WAError.noPhotos }
        let data: [String: Any] = [
            "scopeText": scopeText,
            "subject": subject,
            "grade": grade,
            "images": b64Images
        ]
        let r = try await fn.call(data)
        return try parseGradingResult(r.data)
    }

    private func callGradeStructuredAnswers() async throws -> WAGradingResult {
        let fn = Functions.functions(region: "us-central1").httpsCallable("gradeStructuredAnswers")
        var photosByQId: [String: [String]] = [:]
        for (qId, images) in photosByQuestion {
            let encoded = encodeImages(images)
            if !encoded.isEmpty { photosByQId[qId] = encoded }
        }
        let questionsData: [[String: Any]] = questions.map { q in
            ["id": q.id, "text": q.text, "max_marks": 2]
        }
        let data: [String: Any] = [
            "scopeText": scopeText,
            "subject": subject,
            "grade": grade,
            "questions": questionsData,
            "photosByQuestionId": photosByQId
        ]
        let r = try await fn.call(data)
        return try parseGradingResult(r.data)
    }

    private func parseGradingResult(_ data: Any) throws -> WAGradingResult {
        guard let dict = data as? [String: Any] else {
            return WAGradingResult(grades: [])
        }
        if dict["is_on_topic_overall"] as? Bool == false {
            let reason = dict["off_topic_reason"] as? String ?? ""
            throw WAError.offTopic(reason.isEmpty ? "Your photos don't seem to match this topic. Please try again with the right pages." : reason)
        }
        guard let raw = dict["grades"] as? [[String: Any]] else {
            return WAGradingResult(grades: [])
        }
        let grades = raw.compactMap { m -> WAAnswerGrade? in
            guard let qId = m["question_id"] as? String else { return nil }
            return WAAnswerGrade(
                questionId: qId,
                questionSeen: m["question_seen"] as? String ?? "",
                isAttempted: m["is_attempted"] as? Bool ?? false,
                isCorrect: m["is_correct"] as? Bool ?? false,
                softVerdict: m["soft_verdict"] as? String ?? ""
            )
        }
        return WAGradingResult(grades: grades)
    }

    private func computeTargetCount() -> Int {
        let lines = scopeText.components(separatedBy: "\n")
        // Count individual concepts within the concept line (comma-separated), matching Android exactly.
        let conceptCount = lines
            .filter { $0.hasPrefix("Concept") }
            .map { $0.components(separatedBy: ":").dropFirst().joined(separator: ":").components(separatedBy: ",").count }
            .reduce(0, +)
        let lessonCount = lines.filter { $0.hasPrefix("Lesson:") }.count
        if conceptCount == 1 { return 3 }
        if conceptCount >= 2 { return 5 }
        if lessonCount >= 2 { return 8 }
        return 5
    }

    private func friendlyError(_ err: Error) -> String {
        let msg = err.localizedDescription.lowercased()
        if msg.contains("not found") || msg.contains("404") || msg.contains("unimplemented") {
            return "This feature isn't available yet. Stay tuned!"
        }
        if msg.contains("unavailable") || msg.contains("503") || msg.contains("overloaded") {
            return "Dobi's brain is busy right now. Please try again in a minute."
        }
        if msg.contains("internet") || msg.contains("network") || msg.contains("offline") {
            return "No internet right now. Check your connection and try again."
        }
        return err.localizedDescription
    }
}

private enum WAError: LocalizedError {
    case invalidResponse, noQuestions, noPhotos, offTopic(String)
    var errorDescription: String? {
        switch self {
        case .invalidResponse: return "Invalid response from server."
        case .noQuestions: return "No questions returned. Please try again."
        case .noPhotos: return "Couldn't read uploaded photos. Please try again."
        case .offTopic(let reason): return reason
        }
    }
}

// MARK: - Mode picker

private struct WAModePickerView: View {
    let displayName: String
    let onNotebook: () -> Void
    let onGenerated: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                Spacer().frame(height: 8)
                WATopicChip(text: displayName)
                Spacer().frame(height: 20)
                Text("How would you like to do it?")
                    .font(.system(size: 22, weight: .bold))
                    .foregroundStyle(Color.dobiTextPrimary)
                Spacer().frame(height: 6)
                Text("Pick one — Dobi will read your handwriting and tell you how you did.")
                    .font(.system(size: 13))
                    .foregroundStyle(Color.dobiTextSecondary)
                    .lineSpacing(4)
                Spacer().frame(height: 20)
                WAModeCardLarge(
                    systemIcon: "book.fill",
                    title: "I've practised in my notebook",
                    subtitle: "Snap photos of the answers you've already written. Dobi will check them.",
                    accent: Color.dobiPrimary,
                    onClick: onNotebook
                )
                Spacer().frame(height: 12)
                WAModeCardLarge(
                    systemIcon: "sparkles",
                    title: "Give me practice questions",
                    subtitle: "Dobi makes a few questions; write the answers in your notebook and snap each one.",
                    accent: Color(hex: 0xD946EF),
                    onClick: onGenerated
                )
                Spacer().frame(height: 24)
            }
        }
    }
}

// MARK: - Notebook collect

private struct WANotebookCollectView: View {
    let displayName: String
    var photos: [UIImage]
    var errorMsg: String?
    var canChangeMode: Bool
    let onAdd: ([UIImage]) -> Void
    let onRemove: (Int) -> Void
    let onSubmit: () -> Void
    let onChangeMode: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                WATopicChip(text: displayName)
                Spacer().frame(height: 16)
                Text("Snap your notebook pages")
                    .font(.system(size: 17, weight: .bold))
                    .foregroundStyle(Color.dobiTextPrimary)
                Spacer().frame(height: 4)
                Text("Take clear photos of the page(s) where you wrote answers for this topic.")
                    .font(.system(size: 12))
                    .foregroundStyle(Color.dobiTextSecondary)
                    .lineSpacing(4)
                Spacer().frame(height: 14)
                if !photos.isEmpty {
                    WAPhotoStrip(images: photos, onRemove: onRemove)
                    Spacer().frame(height: 12)
                }
                WAPhotoButtons(onAdd: onAdd)
                if let err = errorMsg {
                    Spacer().frame(height: 14)
                    Text(err)
                        .font(.system(size: 13))
                        .foregroundStyle(Color(hex: 0xEF4444))
                        .lineSpacing(4)
                }
                Spacer().frame(height: 20)
                Button(action: onSubmit) {
                    Text("Submit Answers")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                        .background(photos.isEmpty ? Color(hex: 0xBFD7F7) : Color.dobiPrimary)
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                }
                .buttonStyle(.plain)
                .disabled(photos.isEmpty)
                if canChangeMode {
                    Spacer().frame(height: 8)
                    WAChangeModeLink(onChangeMode: onChangeMode)
                }
                Spacer().frame(height: 20)
            }
        }
    }
}

// MARK: - Textbook collect

private struct WATextbookCollectView: View {
    let displayName: String
    var photos: [UIImage]
    var errorMsg: String?
    var canChangeMode: Bool
    let onAdd: ([UIImage]) -> Void
    let onRemove: (Int) -> Void
    let onSubmit: () -> Void
    let onChangeMode: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                WATopicChip(text: displayName)
                Spacer().frame(height: 16)
                Text("Snap the textbook pages")
                    .font(.system(size: 17, weight: .bold))
                    .foregroundStyle(Color.dobiTextPrimary)
                Spacer().frame(height: 4)
                Text("Take clear photos of the questions or chapter pages you want to practise. Dobi will make a few short-answer questions from them.")
                    .font(.system(size: 12))
                    .foregroundStyle(Color.dobiTextSecondary)
                    .lineSpacing(4)
                Spacer().frame(height: 14)
                if !photos.isEmpty {
                    WAPhotoStrip(images: photos, onRemove: onRemove)
                    Spacer().frame(height: 12)
                }
                WAPhotoButtons(onAdd: onAdd)
                if let err = errorMsg {
                    Spacer().frame(height: 14)
                    Text(err)
                        .font(.system(size: 13))
                        .foregroundStyle(Color(hex: 0xEF4444))
                        .lineSpacing(4)
                }
                Spacer().frame(height: 20)
                Button(action: onSubmit) {
                    Text("Generate Questions")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                        .background(photos.isEmpty ? Color(hex: 0xBFD7F7) : Color.dobiPrimary)
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                }
                .buttonStyle(.plain)
                .disabled(photos.isEmpty)
                if canChangeMode {
                    Spacer().frame(height: 8)
                    WAChangeModeLink(onChangeMode: onChangeMode)
                }
                Spacer().frame(height: 20)
            }
        }
    }
}

// MARK: - Per-question collect

private struct WAPerQuestionView: View {
    let displayName: String
    let questions: [WAQuestion]
    let photosByQuestion: [String: [UIImage]]
    var errorMsg: String?
    var canChangeMode: Bool
    var canSubmit: Bool
    let onPhotoAdded: (String, UIImage) -> Void
    let onSubmit: () -> Void
    let onChangeMode: () -> Void

    @State private var cameraForQuestion: String? = nil
    @State private var showCamera = false
    @State private var cameraImage: UIImage? = nil

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                WATopicChip(text: displayName)
                Spacer().frame(height: 14)
                Text("Write the answers in your notebook, then snap each one")
                    .font(.system(size: 17, weight: .bold))
                    .foregroundStyle(Color.dobiTextPrimary)
                    .lineSpacing(4)
                Spacer().frame(height: 4)
                Text("Tap the camera next to a question to upload that answer.")
                    .font(.system(size: 12))
                    .foregroundStyle(Color.dobiTextSecondary)
                Spacer().frame(height: 14)
                ForEach(questions.indices, id: \.self) { i in
                    WAQuestionRow(
                        index: i + 1,
                        question: questions[i],
                        photos: photosByQuestion[questions[i].id] ?? [],
                        onCameraCapture: {
                            cameraForQuestion = questions[i].id
                            showCamera = true
                        }
                    )
                    Spacer().frame(height: 10)
                }
                if let err = errorMsg {
                    Spacer().frame(height: 8)
                    Text(err)
                        .font(.system(size: 13))
                        .foregroundStyle(Color(hex: 0xEF4444))
                        .lineSpacing(4)
                }
                Spacer().frame(height: 16)
                Button(action: onSubmit) {
                    Text("Submit Answers")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                        .background(canSubmit ? Color.dobiPrimary : Color(hex: 0xBFD7F7))
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                }
                .buttonStyle(.plain)
                .disabled(!canSubmit)
                if canChangeMode {
                    Spacer().frame(height: 8)
                    WAChangeModeLink(onChangeMode: onChangeMode)
                }
                Spacer().frame(height: 20)
            }
        }
        .fullScreenCover(isPresented: $showCamera, onDismiss: handleCameraResult) {
            UWCameraView(image: $cameraImage).ignoresSafeArea()
        }
    }

    private func handleCameraResult() {
        if let qId = cameraForQuestion, let img = cameraImage {
            onPhotoAdded(qId, img)
        }
        cameraForQuestion = nil
        cameraImage = nil
    }
}

private struct WAQuestionRow: View {
    let index: Int
    let question: WAQuestion
    var photos: [UIImage]
    let onCameraCapture: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .top, spacing: 10) {
                ZStack {
                    Circle()
                        .fill(Color.dobiPrimaryContainer)
                        .frame(width: 28, height: 28)
                    Text("\(index)")
                        .font(.system(size: 13, weight: .bold))
                        .foregroundStyle(Color.dobiPrimary)
                }
                Text(question.text)
                    .font(.system(size: 14))
                    .foregroundStyle(Color.dobiTextPrimary)
                    .lineSpacing(6)
                    .frame(maxWidth: .infinity, alignment: .leading)
                Spacer().frame(width: 8)
                Button(action: onCameraCapture) {
                    ZStack {
                        Circle()
                            .fill(Color.dobiPrimary)
                            .frame(width: 40, height: 40)
                        Image(systemName: "camera.fill")
                            .font(.system(size: 16))
                            .foregroundStyle(.white)
                    }
                }
                .buttonStyle(.plain)
            }
            if !photos.isEmpty {
                Spacer().frame(height: 10)
                WAPhotoStrip(images: photos, onRemove: { _ in })
            }
        }
        .padding(14)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 14))
        .overlay(RoundedRectangle(cornerRadius: 14).stroke(Color(hex: 0xE5EBF5), lineWidth: 1))
    }
}

// MARK: - Results view

private struct WAResultsView: View {
    let displayName: String
    let result: WAGradingResult
    let onLaunchQuiz: () -> Void
    let onDone: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                Spacer().frame(height: 16)
                Text("Here's how you did 🌱")
                    .font(.system(size: 22, weight: .bold))
                    .foregroundStyle(Color.dobiTextPrimary)
                Spacer().frame(height: 6)
                Text(displayName)
                    .font(.system(size: 13))
                    .foregroundStyle(Color.dobiTextSecondary)
                Spacer().frame(height: 16)
                WAStatRow(result: result)
                Spacer().frame(height: 20)
                Text("Question by question")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundStyle(Color.dobiTextPrimary)
                Spacer().frame(height: 10)
                ForEach(result.grades.indices, id: \.self) { i in
                    WAAnswerCard(index: i + 1, grade: result.grades[i])
                    Spacer().frame(height: 8)
                }
                Spacer().frame(height: 20)
                Button(action: onLaunchQuiz) {
                    Text("Quick check — 5 quiz questions")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                        .background(Color.dobiPrimary)
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                }
                .buttonStyle(.plain)
                Spacer().frame(height: 10)
                Text("Skip for now")
                    .font(.system(size: 13))
                    .foregroundStyle(Color.dobiTextSecondary)
                    .frame(maxWidth: .infinity)
                    .multilineTextAlignment(.center)
                    .onTapGesture(perform: onDone)
                Spacer().frame(height: 24)
            }
        }
    }
}

private struct WAStatRow: View {
    let result: WAGradingResult

    var body: some View {
        HStack {
            Spacer()
            WAStat(label: "Attempted", value: "\(result.attemptedCount)/\(result.grades.count)")
            Spacer()
            WAStat(label: "Correct", value: "\(result.correctCount)")
            Spacer()
            WAStat(label: "Accuracy", value: "\(Int(result.accuracy * 100))%")
            Spacer()
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .background(Color.dobiPrimaryContainer)
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }
}

private struct WAStat: View {
    let label: String
    let value: String

    var body: some View {
        VStack(spacing: 2) {
            Text(value)
                .font(.system(size: 18, weight: .bold))
                .foregroundStyle(Color.dobiPrimary)
            Text(label)
                .font(.system(size: 11))
                .foregroundStyle(Color.dobiTextSecondary)
        }
    }
}

private struct WAAnswerCard: View {
    let index: Int
    let grade: WAAnswerGrade

    private var accent: Color {
        if !grade.isAttempted { return Color(hex: 0x94A3B8) }
        return grade.isCorrect ? Color(hex: 0x22C55E) : Color(hex: 0xF59E0B)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: 8) {
                Circle().fill(accent).frame(width: 8, height: 8)
                Text("Q\(index)")
                    .font(.system(size: 13, weight: .bold))
                    .foregroundStyle(Color.dobiTextPrimary)
                Spacer().frame(width: 10)
                Text(grade.softVerdict)
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(accent)
            }
            if !grade.questionSeen.isEmpty {
                Spacer().frame(height: 6)
                Text(grade.questionSeen)
                    .font(.system(size: 12))
                    .foregroundStyle(Color.dobiTextSecondary)
                    .lineSpacing(4)
            }
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color(hex: 0xE5EBF5), lineWidth: 1))
    }
}

// MARK: - Error view

private struct WAErrorView: View {
    let message: String
    let onBack: () -> Void

    var body: some View {
        VStack(spacing: 16) {
            Spacer()
            Text(message)
                .font(.system(size: 14))
                .foregroundStyle(Color(hex: 0xEF4444))
                .multilineTextAlignment(.center)
            Button("Go Back", action: onBack)
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(Color.dobiPrimary)
            Spacer()
        }
        .frame(maxWidth: .infinity)
    }
}

// MARK: - Loading view

private struct WALoadingView: View {
    let message: String

    var body: some View {
        VStack(spacing: 16) {
            Spacer()
            ProgressView().tint(Color.dobiPrimary)
            Text(message)
                .font(.system(size: 14))
                .foregroundStyle(Color.dobiTextSecondary)
                .multilineTextAlignment(.center)
            Spacer()
        }
        .frame(maxWidth: .infinity)
    }
}

// MARK: - Topic chip

private struct WATopicChip: View {
    let text: String

    var body: some View {
        HStack(spacing: 8) {
            Text("📘").font(.system(size: 14))
            Text(text)
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(Color.dobiPrimary)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(Color.dobiPrimaryContainer)
        .clipShape(RoundedRectangle(cornerRadius: 10))
    }
}

// MARK: - Change mode link

private struct WAChangeModeLink: View {
    let onChangeMode: () -> Void

    var body: some View {
        Text("Change mode")
            .font(.system(size: 12))
            .foregroundStyle(Color.dobiTextSecondary)
            .frame(maxWidth: .infinity)
            .multilineTextAlignment(.center)
            .onTapGesture(perform: onChangeMode)
    }
}

// MARK: - Mode card large (horizontal)

private struct WAModeCardLarge: View {
    let systemIcon: String
    let title: String
    let subtitle: String
    let accent: Color
    let onClick: () -> Void

    var body: some View {
        HStack(alignment: .center, spacing: 14) {
            ZStack {
                RoundedRectangle(cornerRadius: 14)
                    .fill(accent.opacity(0.12))
                    .frame(width: 54, height: 54)
                Image(systemName: systemIcon)
                    .font(.system(size: 24))
                    .foregroundStyle(accent)
            }
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.system(size: 15, weight: .bold))
                    .foregroundStyle(Color.dobiTextPrimary)
                Text(subtitle)
                    .font(.system(size: 12))
                    .foregroundStyle(Color.dobiTextSecondary)
                    .lineSpacing(4)
                    .fixedSize(horizontal: false, vertical: true)
            }
            Spacer()
        }
        .padding(16)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 18))
        .overlay(RoundedRectangle(cornerRadius: 18).stroke(Color(hex: 0xE5EBF5), lineWidth: 1))
        .onTapGesture(perform: onClick)
    }
}

// MARK: - Photo buttons (Camera + Gallery)

private struct WAPhotoButtons: View {
    let onAdd: ([UIImage]) -> Void

    @State private var pickerItems: [PhotosPickerItem] = []
    @State private var showCamera = false
    @State private var cameraImage: UIImage? = nil

    var body: some View {
        HStack(spacing: 10) {
            Button { showCamera = true } label: {
                WAPhotoOutlineButton(icon: "camera", label: "Camera")
            }
            .buttonStyle(.plain)

            PhotosPicker(selection: $pickerItems, maxSelectionCount: 10, matching: .images) {
                WAPhotoOutlineButton(icon: "photo.on.rectangle", label: "Gallery")
            }
        }
        .fullScreenCover(isPresented: $showCamera, onDismiss: addCameraImage) {
            UWCameraView(image: $cameraImage).ignoresSafeArea()
        }
        .onChange(of: pickerItems) { items in
            Task {
                var loaded: [UIImage] = []
                for item in items {
                    if let data = try? await item.loadTransferable(type: Data.self),
                       let img = UIImage(data: data) { loaded.append(img) }
                }
                await MainActor.run { onAdd(loaded); pickerItems = [] }
            }
        }
    }

    private func addCameraImage() {
        if let img = cameraImage { onAdd([img]); cameraImage = nil }
    }
}

private struct WAPhotoOutlineButton: View {
    let icon: String
    let label: String

    var body: some View {
        HStack(spacing: 8) {
            Image(systemName: icon).font(.system(size: 18)).foregroundStyle(Color.dobiPrimary)
            Text(label).font(.system(size: 13, weight: .semibold)).foregroundStyle(Color.dobiPrimary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 14)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 14))
        .overlay(RoundedRectangle(cornerRadius: 14).stroke(Color(hex: 0xE5EBF5), lineWidth: 1))
    }
}

// MARK: - Photo strip

private struct WAPhotoStrip: View {
    var images: [UIImage]
    let onRemove: (Int) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            ForEach(stride(from: 0, to: images.count, by: 3).map { $0 }, id: \.self) { rowStart in
                HStack(spacing: 8) {
                    ForEach(rowStart..<min(rowStart + 3, images.count), id: \.self) { i in
                        ZStack(alignment: .topTrailing) {
                            Image(uiImage: images[i])
                                .resizable()
                                .scaledToFill()
                                .frame(width: 86, height: 86)
                                .clipShape(RoundedRectangle(cornerRadius: 10))
                                .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color(hex: 0xE2E8F0), lineWidth: 1))
                            Button { onRemove(i) } label: {
                                Image(systemName: "xmark")
                                    .font(.system(size: 10, weight: .bold))
                                    .foregroundStyle(.white)
                                    .frame(width: 20, height: 20)
                                    .background(Color.black.opacity(0.55))
                                    .clipShape(Circle())
                            }
                            .buttonStyle(.plain)
                            .padding(4)
                        }
                    }
                }
            }
        }
    }
}
