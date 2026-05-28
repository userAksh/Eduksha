import SwiftUI
import FirebaseFirestore

// MARK: - Domain models (local to this feature)

struct McqQuestion {
    let text: String
    let options: [String]
    let correctIndex: Int
}

struct SubjectiveQuestion {
    let text: String
    let expectedKeywords: [String]
}

struct GoalSubjectChoice: Identifiable {
    var id: String { name }
    let name: String
    let color: String
    let icon: String
}

struct GoalTopicChoice: Identifiable {
    var id: String { unitId }
    let unitId: String
    let unitName: String
    let subject: String
    let subjectColor: String
    let subjectIcon: String
    let scopeText: String
}

enum GoalStep: Equatable {
    case subject, topic, taskType, preview, submitting, done
}

enum GoalTaskType: String {
    case ourQuestions   = "OUR_QUESTIONS"
    case writeQA        = "WRITE_QA"
    case custom         = "CUSTOM"
    case reviseChapter  = "REVISE_CHAPTER"
}

enum GoalCustomMode { case wholeChapter, typed }

// MARK: - ViewModel

@MainActor
final class StudyGoalsViewModel: ObservableObject {
    let childId: String
    let childName: String
    let childGrade: Int
    let childBoard: String
    let parentUID: String

    @Published var isLoading = true
    @Published var errorMessage = ""
    @Published var blockingTask: StudyGoalsActiveTask? = nil

    @Published var subjects: [GoalSubjectChoice] = []
    @Published var topicsBySubject: [String: [GoalTopicChoice]] = [:]

    @Published var step: GoalStep = .subject
    @Published var selectedSubject: GoalSubjectChoice? = nil
    @Published var selectedTopic: GoalTopicChoice? = nil
    @Published var selectedType: GoalTaskType? = nil
    @Published var customMode: GoalCustomMode = .wholeChapter
    @Published var customNote = ""

    @Published var generatingPreview = false
    @Published var previewMcqs: [McqQuestion] = []
    @Published var previewSubjectiveQs: [SubjectiveQuestion] = []
    @Published var previewError = ""

    private let firebase = FirebaseService.shared

    init(childId: String, childName: String, childGrade: Int, childBoard: String, parentUID: String) {
        self.childId = childId
        self.childName = childName
        self.childGrade = childGrade
        self.childBoard = childBoard
        self.parentUID = parentUID
        Task { await load() }
    }

    func load() async {
        isLoading = true
        errorMessage = ""

        // Active task check is non-fatal — failure just means no blocking task
        blockingTask = try? await firebase.getActiveParentTask(parentUID: parentUID, childId: childId)

        // Concepts are essential — surface a retry error if they fail
        do {
            let concepts = try await firebase.getConcepts(grade: childGrade, board: childBoard)
            let (subs, topics) = buildCurriculum(from: concepts)
            subjects = subs
            topicsBySubject = topics
        } catch {
            errorMessage = "Couldn't load subjects. Check your connection and tap Retry."
        }

        isLoading = false
    }

    func selectSubject(_ subject: GoalSubjectChoice) {
        selectedSubject = subject
        selectedTopic = nil
        step = .topic
    }

    func selectTopic(_ topic: GoalTopicChoice) {
        selectedTopic = topic
        step = .taskType
    }

    func selectType(_ type: GoalTaskType) {
        selectedType = type
        previewMcqs = []
        previewSubjectiveQs = []
        previewError = ""
        customNote = ""
        customMode = .wholeChapter
        step = .preview
        Task {
            switch type {
            case .ourQuestions: await generateMcqs()
            case .writeQA: await generateSubjective()
            default: break
            }
        }
    }

    func selectCustomMode(_ mode: GoalCustomMode) {
        customMode = mode
        selectedType = mode == .wholeChapter ? .reviseChapter : .custom
    }

    func back() {
        switch step {
        case .topic:    step = .subject
        case .taskType: step = .topic
        case .preview:  step = .taskType
        default: break
        }
    }

    func generateMcqs() async {
        guard let topic = selectedTopic else { return }
        generatingPreview = true
        previewError = ""
        defer { generatingPreview = false }

        let prompt = """
        Generate exactly 5 MCQ questions about "\(topic.unitName)" for Grade \(childGrade) \(topic.subject) students.
        Context: \(String(topic.scopeText.prefix(600)))

        Return ONLY a valid JSON array, no markdown, no explanation:
        [{"text":"question","options":["A","B","C","D"],"correctIndex":0},...]
        """
        do {
            let text = try await firebase.generateGeminiContent(prompt: prompt)
            let parsed = parseMcqs(from: text)
            previewMcqs = parsed
            if parsed.isEmpty { previewError = "Couldn't generate questions. Try again." }
        } catch let err as NSError where err.domain == "GeminiSim" {
            // Simulator: no Firebase Auth session — use sample questions for UI testing
            previewMcqs = sampleMcqs(for: topic.unitName)
        } catch {
            previewError = "Couldn't generate questions. Try again."
        }
    }

    func generateSubjective() async {
        guard let topic = selectedTopic else { return }
        generatingPreview = true
        previewError = ""
        defer { generatingPreview = false }

        let prompt = """
        Generate exactly 5 short-answer questions about "\(topic.unitName)" for Grade \(childGrade) \(topic.subject) students.
        Context: \(String(topic.scopeText.prefix(600)))

        Return ONLY a valid JSON array, no markdown, no explanation:
        [{"text":"question","expectedKeywords":["keyword1","keyword2"]},...]
        """
        do {
            let text = try await firebase.generateGeminiContent(prompt: prompt)
            let parsed = parseSubjective(from: text)
            previewSubjectiveQs = parsed
            if parsed.isEmpty { previewError = "Couldn't generate questions. Try again." }
        } catch let err as NSError where err.domain == "GeminiSim" {
            previewSubjectiveQs = sampleSubjectiveQs(for: topic.unitName)
        } catch {
            previewError = "Couldn't generate questions. Try again."
        }
    }

    private func sampleMcqs(for unit: String) -> [McqQuestion] {
        [
            McqQuestion(text: "What is the main topic of \(unit)?", options: ["Option A", "Option B", "Option C", "Option D"], correctIndex: 0),
            McqQuestion(text: "Which of the following best describes \(unit)?", options: ["Description A", "Description B", "Description C", "Description D"], correctIndex: 1),
            McqQuestion(text: "A key concept in \(unit) is:", options: ["Concept A", "Concept B", "Concept C", "Concept D"], correctIndex: 2),
            McqQuestion(text: "Which statement about \(unit) is correct?", options: ["Statement A", "Statement B", "Statement C", "Statement D"], correctIndex: 0),
            McqQuestion(text: "The most important aspect of \(unit) is:", options: ["Aspect A", "Aspect B", "Aspect C", "Aspect D"], correctIndex: 3),
        ]
    }

    private func sampleSubjectiveQs(for unit: String) -> [SubjectiveQuestion] {
        [
            SubjectiveQuestion(text: "Explain the main concept of \(unit) in your own words.", expectedKeywords: ["explain", "concept"]),
            SubjectiveQuestion(text: "What are three key points you learned from \(unit)?", expectedKeywords: ["three", "points"]),
            SubjectiveQuestion(text: "Give an example that relates to \(unit).", expectedKeywords: ["example"]),
            SubjectiveQuestion(text: "How does \(unit) connect to what you learned previously?", expectedKeywords: ["connect", "previous"]),
            SubjectiveQuestion(text: "Why is \(unit) important to understand?", expectedKeywords: ["important", "understand"]),
        ]
    }

    func assign() async {
        guard let topic = selectedTopic, let type = selectedType else { return }
        if type == .custom && customNote.trimmingCharacters(in: .whitespaces).isEmpty {
            errorMessage = "Add a short instruction first."
            return
        }
        step = .submitting
        errorMessage = ""
        let now = Int64(Date().timeIntervalSince1970 * 1000)
        var data: [String: Any] = [
            "parentId": parentUID,
            "childId": childId,
            "unitId": topic.unitId,
            "unitName": topic.unitName,
            "subject": topic.subject,
            "subjectColor": topic.subjectColor,
            "subjectIcon": topic.subjectIcon,
            "type": type.rawValue,
            "customNote": type == .custom ? customNote.trimmingCharacters(in: .whitespaces) : "",
            "status": "ASSIGNED",
            "mcqCount": type == .ourQuestions ? max(previewMcqs.count, 5) : 5,
            "requestedAtMillis": now,
            "assignedAtMillis": now,
            "reminderSentAtMillis": Int64(0),
            "assignedMcqs": type == .ourQuestions
                ? previewMcqs.map { ["text": $0.text, "options": $0.options, "correctIndex": $0.correctIndex] as [String: Any] }
                : [] as [[String: Any]],
            "assignedSubjectiveQs": type == .writeQA
                ? previewSubjectiveQs.map { ["text": $0.text, "expectedKeywords": $0.expectedKeywords] as [String: Any] }
                : [] as [[String: Any]]
        ]
        _ = data  // suppress unused warning
        do {
            _ = try await firebase.assignParentTask(data)
            step = .done
        } catch {
            step = .preview
            errorMessage = "Couldn't assign the task. Try again."
        }
    }

    func sendReminder() async {
        guard let taskId = blockingTask?.id else { return }
        try? await firebase.sendParentTaskReminder(taskId: taskId)
    }

    // MARK: - Curriculum builder

    private func buildCurriculum(from concepts: [StudyGoalsConcept]) -> ([GoalSubjectChoice], [String: [GoalTopicChoice]]) {
        var bySubject: [String: [StudyGoalsConcept]] = [:]
        for concept in concepts {
            if !concept.subject.isEmpty { bySubject[concept.subject, default: []].append(concept) }
        }
        let subjects = bySubject.map { (subject, list) -> GoalSubjectChoice in
            let anchor = list.first!
            return GoalSubjectChoice(name: subject, color: anchor.subjectColor, icon: anchor.subjectIcon.isEmpty ? "📚" : anchor.subjectIcon)
        }.sorted { $0.name.lowercased() < $1.name.lowercased() }

        var topicsBySubject: [String: [GoalTopicChoice]] = [:]
        for (subject, list) in bySubject {
            var byUnit: [String: [StudyGoalsConcept]] = [:]
            for c in list {
                let key = c.unitId.isEmpty ? c.unit : c.unitId
                if !key.isEmpty { byUnit[key, default: []].append(c) }
            }
            let topics = byUnit.map { (unitId, conceptsInUnit) -> GoalTopicChoice in
                let anchor = conceptsInUnit.min(by: { $0.order < $1.order }) ?? conceptsInUnit.first!
                let unitName = anchor.unit.isEmpty ? anchor.name : anchor.unit
                let scopeText = conceptsInUnit
                    .map { "\($0.name): \($0.description)".trimmingCharacters(in: .whitespaces) }
                    .joined(separator: "\n")
                return GoalTopicChoice(
                    unitId: unitId,
                    unitName: unitName,
                    subject: subject,
                    subjectColor: anchor.subjectColor,
                    subjectIcon: anchor.subjectIcon.isEmpty ? "📖" : anchor.subjectIcon,
                    scopeText: scopeText
                )
            }.sorted { $0.unitName.lowercased() < $1.unitName.lowercased() }
            topicsBySubject[subject] = topics
        }
        return (subjects, topicsBySubject)
    }

    // MARK: - JSON parsers

    private func parseMcqs(from json: String) -> [McqQuestion] {
        guard let data = extractJSONArray(from: json),
              let arr = try? JSONSerialization.jsonObject(with: data) as? [[String: Any]] else { return [] }
        return arr.compactMap { dict -> McqQuestion? in
            guard let text = dict["text"] as? String,
                  let options = dict["options"] as? [String],
                  let correctIndex = dict["correctIndex"] as? Int else { return nil }
            return McqQuestion(text: text, options: options, correctIndex: correctIndex)
        }
    }

    private func parseSubjective(from json: String) -> [SubjectiveQuestion] {
        guard let data = extractJSONArray(from: json),
              let arr = try? JSONSerialization.jsonObject(with: data) as? [[String: Any]] else { return [] }
        return arr.compactMap { dict -> SubjectiveQuestion? in
            guard let text = dict["text"] as? String else { return nil }
            let keywords = dict["expectedKeywords"] as? [String] ?? []
            return SubjectiveQuestion(text: text, expectedKeywords: keywords)
        }
    }

    private func extractJSONArray(from text: String) -> Data? {
        var clean = text
            .replacingOccurrences(of: "```json", with: "")
            .replacingOccurrences(of: "```", with: "")
            .trimmingCharacters(in: .whitespacesAndNewlines)
        if let start = clean.firstIndex(of: "["), let end = clean.lastIndex(of: "]") {
            clean = String(clean[start...end])
        }
        return clean.data(using: .utf8)
    }
}

// MARK: - Main View

struct StudyGoalsView: View {
    let childId: String
    let childName: String
    let childGrade: Int
    let childBoard: String
    let parentUID: String
    var onBack: () -> Void = {}
    var onAssigned: () -> Void = {}

    @StateObject private var vm: StudyGoalsViewModel

    init(
        childId: String,
        childName: String,
        childGrade: Int,
        childBoard: String,
        parentUID: String,
        onBack: @escaping () -> Void = {},
        onAssigned: @escaping () -> Void = {}
    ) {
        self.childId = childId
        self.childName = childName
        self.childGrade = childGrade
        self.childBoard = childBoard
        self.parentUID = parentUID
        self.onBack = onBack
        self.onAssigned = onAssigned
        _vm = StateObject(wrappedValue: StudyGoalsViewModel(
            childId: childId,
            childName: childName,
            childGrade: childGrade,
            childBoard: childBoard,
            parentUID: parentUID
        ))
    }

    private var firstName: String { childName.components(separatedBy: " ").first ?? childName }

    private var topBarTitle: String {
        if vm.blockingTask != nil { return "Study Goals" }
        switch vm.step {
        case .subject:    return "Pick a subject"
        case .topic:      return vm.selectedSubject?.name ?? "Pick a topic"
        case .taskType:   return "How to practise?"
        case .preview:
            switch vm.selectedType {
            case .ourQuestions, .writeQA:       return "Review questions"
            case .custom, .reviseChapter:        return "Custom task"
            case nil:                            return "Review"
            }
        case .submitting, .done: return "Study Goals"
        }
    }

    var body: some View {
        VStack(spacing: 0) {
            SGTopBar(title: topBarTitle) {
                if vm.step == .subject || vm.blockingTask != nil || vm.step == .done {
                    onBack()
                } else if vm.step == .submitting {
                    // swallow during submit
                } else {
                    vm.back()
                }
            }

            Group {
                if vm.isLoading {
                    SGLoadingBlock(message: "Loading…")
                } else if !vm.errorMessage.isEmpty && vm.subjects.isEmpty && vm.blockingTask == nil {
                    SGErrorBlock(message: vm.errorMessage, onRetry: { Task { await vm.load() } })
                } else if let task = vm.blockingTask {
                    SGBlockingBlock(
                        childName: firstName,
                        task: task,
                        onSendReminder: { Task { await vm.sendReminder() } },
                        onClose: onBack
                    )
                } else {
                    switch vm.step {
                    case .subject:
                        SGSubjectStep(subjects: vm.subjects, onPick: vm.selectSubject)
                    case .topic:
                        SGTopicStep(
                            topics: vm.topicsBySubject[vm.selectedSubject?.name ?? ""] ?? [],
                            onPick: vm.selectTopic
                        )
                    case .taskType:
                        SGTaskTypeStep(
                            topicName: vm.selectedTopic?.unitName ?? "",
                            onPick: vm.selectType
                        )
                    case .preview:
                        SGPreviewStep(vm: vm, firstName: firstName, onAssign: { Task { await vm.assign() } })
                    case .submitting:
                        SGLoadingBlock(message: "Assigning…")
                    case .done:
                        SGDoneBlock(childName: firstName)
                    }
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .background(Color(hex: 0xFFF8EE).ignoresSafeArea())
        .navigationBarHidden(true)
        .onChange(of: vm.step) { newStep in
            if newStep == .done {
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.9) { onAssigned() }
            }
        }
    }
}

// MARK: - Top bar

private struct SGTopBar: View {
    let title: String
    let onBack: () -> Void

    var body: some View {
        HStack(spacing: 0) {
            Button(action: onBack) {
                Image(systemName: "arrow.left")
                    .font(.system(size: 20, weight: .medium))
                    .foregroundStyle(Color(hex: 0x16213E))
                    .frame(width: 40, height: 40)
                    .contentShape(Circle())
            }
            .buttonStyle(.plain)

            Spacer().frame(width: 4)

            Text(title)
                .font(.system(size: 17, weight: .bold))
                .foregroundStyle(Color(hex: 0x16213E))
                .lineLimit(1)

            Spacer()
        }
        .frame(height: 56)
        .padding(.horizontal, 12)
    }
}

// MARK: - Error block (concepts failed to load)

private struct SGErrorBlock: View {
    let message: String
    let onRetry: () -> Void

    var body: some View {
        VStack(spacing: 16) {
            Spacer()
            Image(systemName: "exclamationmark.triangle.fill")
                .font(.system(size: 36))
                .foregroundStyle(Color(hex: 0xF59E0B))
            Text(message)
                .font(.system(size: 14))
                .foregroundStyle(Color(hex: 0x6B7895))
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)
            Button(action: onRetry) {
                Text("Retry")
                    .font(.system(size: 15, weight: .bold))
                    .foregroundStyle(.white)
                    .padding(.horizontal, 32)
                    .padding(.vertical, 12)
                    .background(Color(hex: 0x3B7BFF))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
            }
            .buttonStyle(.plain)
            Spacer()
        }
        .frame(maxWidth: .infinity)
    }
}

// MARK: - Loading block

private struct SGLoadingBlock: View {
    var message: String = "Loading…"

    var body: some View {
        VStack(spacing: 12) {
            ProgressView()
                .progressViewStyle(.circular)
                .tint(Color(hex: 0x3B7BFF))
                .scaleEffect(1.2)
            Text(message)
                .font(.system(size: 13))
                .foregroundStyle(Color(hex: 0x6B7895))
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

// MARK: - Blocking block (active task)

private struct SGBlockingBlock: View {
    let childName: String
    let task: StudyGoalsActiveTask
    let onSendReminder: () -> Void
    let onClose: () -> Void

    private var canRemind: Bool {
        guard task.status == "ASSIGNED", task.assignedAtMillis > 0 else { return false }
        let now = Int64(Date().timeIntervalSince1970 * 1000)
        let h24: Int64 = 24 * 60 * 60 * 1000
        return now - task.assignedAtMillis >= h24 &&
               (task.reminderSentAtMillis == 0 || now - task.reminderSentAtMillis >= h24)
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                // Amber warning card
                VStack(alignment: .leading, spacing: 6) {
                    Text("\(childName) already has a task to finish")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundStyle(Color(hex: 0xB45309))
                    Text("You can assign a new task once the current one is completed and accepted.")
                        .font(.system(size: 13))
                        .foregroundStyle(Color(hex: 0x16213E))
                        .lineSpacing(4)
                }
                .padding(18)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color(hex: 0xFFEFD9))
                .clipShape(RoundedRectangle(cornerRadius: 18))

                // Task card
                VStack(alignment: .leading, spacing: 4) {
                    Text(task.displayName)
                        .font(.system(size: 15, weight: .bold))
                        .foregroundStyle(Color(hex: 0x16213E))
                    Text("Status: \(statusLabel(task.status))")
                        .font(.system(size: 12))
                        .foregroundStyle(Color(hex: 0x6B7895))
                }
                .padding(16)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color.white)
                .clipShape(RoundedRectangle(cornerRadius: 16))
                .overlay(RoundedRectangle(cornerRadius: 16).strokeBorder(Color(hex: 0xEDEAE0), lineWidth: 1))

                if canRemind {
                    Button(action: onSendReminder) {
                        Text("Send reminder")
                            .font(.system(size: 15, weight: .bold))
                            .foregroundStyle(.white)
                            .frame(maxWidth: .infinity)
                            .frame(height: 48)
                            .background(Color(hex: 0x3B7BFF))
                            .clipShape(RoundedRectangle(cornerRadius: 14))
                    }
                    .buttonStyle(.plain)
                } else if task.status == "ASSIGNED" {
                    Text("Reminder will unlock 24 hours after you assigned the task.")
                        .font(.system(size: 12))
                        .foregroundStyle(Color(hex: 0x6B7895))
                }

                Spacer().frame(height: 8)

                Button(action: onClose) {
                    Text("Close")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(Color(hex: 0x16213E))
                        .frame(maxWidth: .infinity)
                        .frame(height: 46)
                        .background(Color.white)
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                        .overlay(RoundedRectangle(cornerRadius: 14).strokeBorder(Color(hex: 0xEDEAE0), lineWidth: 1))
                }
                .buttonStyle(.plain)
            }
            .padding(20)
            .padding(.bottom, 40)
        }
    }

    private func statusLabel(_ s: String) -> String {
        switch s {
        case "REQUESTED":      return "Waiting for you to assign"
        case "ASSIGNED":       return "Working on it"
        case "PENDING_REVIEW": return "Submitted — needs your review"
        case "ACCEPTED":       return "Completed"
        case "REJECTED":       return "Redo in progress"
        default:               return s
        }
    }
}

// MARK: - Subject step

private struct SGSubjectStep: View {
    let subjects: [GoalSubjectChoice]
    let onPick: (GoalSubjectChoice) -> Void

    var body: some View {
        if subjects.isEmpty {
            VStack {
                Spacer()
                Text("No subjects available for this class yet.")
                    .font(.system(size: 14))
                    .foregroundStyle(Color(hex: 0x6B7895))
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 40)
                Spacer()
            }
        } else {
            ScrollView {
                VStack(spacing: 10) {
                    ForEach(subjects) { subject in
                        SGPickerRow(icon: subject.icon, title: subject.name, subtitle: "Pick a chapter inside") {
                            onPick(subject)
                        }
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
                .padding(.bottom, 40)
            }
        }
    }
}

// MARK: - Topic step

private struct SGTopicStep: View {
    let topics: [GoalTopicChoice]
    let onPick: (GoalTopicChoice) -> Void

    var body: some View {
        if topics.isEmpty {
            VStack {
                Spacer()
                Text("No chapters listed for this subject.")
                    .font(.system(size: 14))
                    .foregroundStyle(Color(hex: 0x6B7895))
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 40)
                Spacer()
            }
        } else {
            ScrollView {
                VStack(spacing: 10) {
                    ForEach(topics) { topic in
                        SGPickerRow(icon: topic.subjectIcon, title: topic.unitName, subtitle: topic.subject) {
                            onPick(topic)
                        }
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
                .padding(.bottom, 40)
            }
        }
    }
}

// MARK: - Task type step

private struct SGTaskTypeStep: View {
    let topicName: String
    let onPick: (GoalTaskType) -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                Text(topicName)
                    .font(.system(size: 18, weight: .bold))
                    .foregroundStyle(Color(hex: 0x16213E))
                    .padding(.bottom, 4)

                SGTypeCard(emoji: "✏️", title: "Quick quiz", subtitle: "5 AI-generated MCQs. You'll review them before sending.") {
                    onPick(.ourQuestions)
                }
                SGTypeCard(emoji: "📝", title: "Subjective questions", subtitle: "5 short-answer questions to write & upload.") {
                    onPick(.writeQA)
                }
                SGTypeCard(emoji: "📚", title: "Custom", subtitle: "Assign the whole chapter, or type your own instruction.") {
                    onPick(.custom)
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)
            .padding(.bottom, 40)
        }
    }
}

// MARK: - Preview step

private struct SGPreviewStep: View {
    @ObservedObject var vm: StudyGoalsViewModel
    let firstName: String
    let onAssign: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            // Scrollable question list — fills remaining space so ScrollView can bound itself
            Group {
                switch vm.selectedType {
                case .ourQuestions:
                    SGMcqPreviewList(questions: vm.previewMcqs, loading: vm.generatingPreview, error: vm.previewError)
                case .writeQA:
                    SGSubjectivePreviewList(questions: vm.previewSubjectiveQs, loading: vm.generatingPreview, error: vm.previewError)
                case .custom, .reviseChapter:
                    SGCustomPreview(
                        selected: vm.customMode,
                        note: vm.customNote,
                        onModeSelect: vm.selectCustomMode,
                        onNoteChange: { vm.customNote = $0 }
                    )
                case nil:
                    EmptyView()
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)

            // Pinned bottom bar
            SGBottomBar(vm: vm, firstName: firstName, onAssign: onAssign)
        }
    }
}

private struct SGMcqPreviewList: View {
    let questions: [McqQuestion]
    let loading: Bool
    let error: String

    var body: some View {
        if loading { return AnyView(SGLoadingBlock(message: "Generating questions…")) }
        if !error.isEmpty && questions.isEmpty {
            return AnyView(
                VStack {
                    Spacer()
                    Text(error).font(.system(size: 14)).foregroundStyle(Color(hex: 0x6B7895)).multilineTextAlignment(.center).padding(.horizontal, 40)
                    Spacer()
                }
            )
        }
        return AnyView(
            ScrollView {
                VStack(spacing: 12) {
                    ForEach(Array(questions.enumerated()), id: \.offset) { index, q in
                        SGQuestionCard(number: index + 1, title: q.text) {
                            ForEach(Array(q.options.enumerated()), id: \.offset) { i, opt in
                                let isCorrect = i == q.correctIndex
                                HStack(spacing: 8) {
                                    ZStack {
                                        Circle()
                                            .fill(isCorrect ? Color(hex: 0xE6F7EC) : Color(hex: 0xF3F4F6))
                                            .frame(width: 20, height: 20)
                                        if isCorrect {
                                            Image(systemName: "checkmark")
                                                .font(.system(size: 10, weight: .bold))
                                                .foregroundStyle(Color(hex: 0x16A34A))
                                        }
                                    }
                                    Text(opt)
                                        .font(.system(size: 13, weight: isCorrect ? .semibold : .regular))
                                        .foregroundStyle(isCorrect ? Color(hex: 0x16A34A) : Color(hex: 0x16213E))
                                }
                                .padding(.vertical, 3)
                            }
                        }
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
                .padding(.bottom, 20)
            }
        )
    }
}

private struct SGSubjectivePreviewList: View {
    let questions: [SubjectiveQuestion]
    let loading: Bool
    let error: String

    var body: some View {
        if loading { return AnyView(SGLoadingBlock(message: "Generating questions…")) }
        if !error.isEmpty && questions.isEmpty {
            return AnyView(
                VStack {
                    Spacer()
                    Text(error).font(.system(size: 14)).foregroundStyle(Color(hex: 0x6B7895)).multilineTextAlignment(.center).padding(.horizontal, 40)
                    Spacer()
                }
            )
        }
        return AnyView(
            ScrollView {
                VStack(spacing: 12) {
                    ForEach(Array(questions.enumerated()), id: \.offset) { index, q in
                        SGQuestionCard(number: index + 1, title: q.text) {
                            if !q.expectedKeywords.isEmpty {
                                Spacer().frame(height: 4)
                                Text("Look for: \(q.expectedKeywords.joined(separator: ", "))")
                                    .font(.system(size: 11))
                                    .foregroundStyle(Color(hex: 0x6B7895))
                            }
                        }
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
                .padding(.bottom, 20)
            }
        )
    }
}

private struct SGCustomPreview: View {
    let selected: GoalCustomMode
    let note: String
    let onModeSelect: (GoalCustomMode) -> Void
    let onNoteChange: (String) -> Void

    var body: some View {
        ScrollView {
            VStack(spacing: 12) {
                SGCustomChoiceCard(
                    selected: selected == .wholeChapter,
                    emoji: "📖",
                    title: "Assign the whole chapter",
                    subtitle: "Child revises the chapter and uploads photos of their work."
                ) { onModeSelect(.wholeChapter) }

                SGCustomChoiceCard(
                    selected: selected == .typed,
                    emoji: "✍️",
                    title: "Type my own task",
                    subtitle: "Write a short instruction for your child."
                ) { onModeSelect(.typed) }

                if selected == .typed {
                    ZStack(alignment: .topLeading) {
                        if note.isEmpty {
                            Text("e.g. Read the lesson and tell me 3 examples in your own words.")
                                .font(.system(size: 13))
                                .foregroundStyle(Color(hex: 0x6B7895))
                                .padding(.top, 8)
                                .padding(.leading, 4)
                                .allowsHitTesting(false)
                        }
                        TextEditor(text: Binding(get: { note }, set: onNoteChange))
                            .font(.system(size: 14))
                            .foregroundStyle(Color(hex: 0x16213E))
                            .frame(minHeight: 96)
                            .scrollContentBackground(.hidden)
                    }
                    .padding(14)
                    .background(Color.white)
                    .clipShape(RoundedRectangle(cornerRadius: 14))
                    .overlay(RoundedRectangle(cornerRadius: 14).strokeBorder(Color(hex: 0xEDEAE0), lineWidth: 1))
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)
            .padding(.bottom, 20)
        }
    }
}

private struct SGBottomBar: View {
    @ObservedObject var vm: StudyGoalsViewModel
    let firstName: String
    let onAssign: () -> Void

    private var canAssign: Bool {
        switch vm.selectedType {
        case .ourQuestions:  return !vm.previewMcqs.isEmpty && !vm.generatingPreview
        case .writeQA:       return !vm.previewSubjectiveQs.isEmpty && !vm.generatingPreview
        case .custom:        return !vm.customNote.trimmingCharacters(in: .whitespaces).isEmpty
        case .reviseChapter: return true
        case nil:            return false
        }
    }

    private var showRegenerate: Bool {
        vm.selectedType == .ourQuestions || vm.selectedType == .writeQA
    }

    var body: some View {
        VStack(spacing: 0) {
            Divider()
            VStack(spacing: 0) {
                if !vm.errorMessage.isEmpty {
                    Text(vm.errorMessage)
                        .font(.system(size: 12))
                        .foregroundStyle(Color(hex: 0xDC2626))
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.bottom, 8)
                }
                HStack(spacing: 10) {
                    if showRegenerate {
                        Button {
                            Task {
                                if vm.selectedType == .ourQuestions { await vm.generateMcqs() }
                                else { await vm.generateSubjective() }
                            }
                        } label: {
                            Text("Regenerate")
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundStyle(Color(hex: 0x16213E))
                                .frame(maxWidth: .infinity)
                                .frame(height: 48)
                                .background(Color.white)
                                .clipShape(RoundedRectangle(cornerRadius: 14))
                                .overlay(RoundedRectangle(cornerRadius: 14).strokeBorder(Color(hex: 0xEDEAE0), lineWidth: 1.5))
                        }
                        .buttonStyle(.plain)
                        .disabled(vm.generatingPreview)
                        .opacity(vm.generatingPreview ? 0.5 : 1)
                    }

                    Button(action: onAssign) {
                        Text("Assign to \(firstName)")
                            .font(.system(size: 15, weight: .bold))
                            .foregroundStyle(.white)
                            .frame(maxWidth: .infinity)
                            .frame(height: 48)
                            .background(canAssign ? Color(hex: 0x3B7BFF) : Color(hex: 0x3B7BFF).opacity(0.4))
                            .clipShape(RoundedRectangle(cornerRadius: 14))
                    }
                    .buttonStyle(.plain)
                    .disabled(!canAssign)
                }
            }
            .padding(16)
            .background(Color(hex: 0xFFF8EE))
        }
    }
}

// MARK: - Done block

private struct SGDoneBlock: View {
    let childName: String

    var body: some View {
        VStack(spacing: 12) {
            Spacer()
            ZStack {
                Circle()
                    .fill(Color(hex: 0xE6F7EC))
                    .frame(width: 64, height: 64)
                Image(systemName: "checkmark")
                    .font(.system(size: 28, weight: .bold))
                    .foregroundStyle(Color(hex: 0x16A34A))
            }
            Text("Task sent to \(childName)")
                .font(.system(size: 16, weight: .bold))
                .foregroundStyle(Color(hex: 0x16213E))
            Spacer()
        }
        .frame(maxWidth: .infinity)
    }
}

// MARK: - Shared card components

private struct SGPickerRow: View {
    let icon: String
    let title: String
    let subtitle: String
    let onClick: () -> Void

    var body: some View {
        Button(action: onClick) {
            HStack(spacing: 12) {
                ZStack {
                    Circle()
                        .fill(Color(hex: 0xEEF5FF))
                        .frame(width: 40, height: 40)
                    Text(icon)
                        .font(.system(size: 20))
                }
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.system(size: 14, weight: .bold))
                        .foregroundStyle(Color(hex: 0x16213E))
                    Text(subtitle)
                        .font(.system(size: 12))
                        .foregroundStyle(Color(hex: 0x6B7895))
                        .lineLimit(1)
                }
                Spacer()
            }
            .padding(14)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 14))
            .overlay(RoundedRectangle(cornerRadius: 14).strokeBorder(Color(hex: 0xEDEAE0), lineWidth: 1))
            .contentShape(RoundedRectangle(cornerRadius: 14))
        }
        .buttonStyle(.plain)
    }
}

private struct SGTypeCard: View {
    let emoji: String
    let title: String
    let subtitle: String
    let onClick: () -> Void

    var body: some View {
        Button(action: onClick) {
            HStack(spacing: 14) {
                Text(emoji).font(.system(size: 28))
                VStack(alignment: .leading, spacing: 4) {
                    Text(title)
                        .font(.system(size: 15, weight: .bold))
                        .foregroundStyle(Color(hex: 0x16213E))
                    Text(subtitle)
                        .font(.system(size: 12))
                        .foregroundStyle(Color(hex: 0x6B7895))
                        .lineSpacing(3)
                }
                Spacer()
            }
            .padding(16)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .overlay(RoundedRectangle(cornerRadius: 16).strokeBorder(Color(hex: 0xEDEAE0), lineWidth: 1))
            .contentShape(RoundedRectangle(cornerRadius: 16))
        }
        .buttonStyle(.plain)
    }
}

private struct SGCustomChoiceCard: View {
    let selected: Bool
    let emoji: String
    let title: String
    let subtitle: String
    let onClick: () -> Void

    var body: some View {
        Button(action: onClick) {
            HStack(spacing: 12) {
                Text(emoji).font(.system(size: 24))
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.system(size: 14, weight: .bold))
                        .foregroundStyle(Color(hex: 0x16213E))
                    Text(subtitle)
                        .font(.system(size: 12))
                        .foregroundStyle(Color(hex: 0x6B7895))
                        .lineSpacing(3)
                }
                Spacer()
                if selected {
                    Image(systemName: "checkmark")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(Color(hex: 0x3B7BFF))
                }
            }
            .padding(14)
            .background(selected ? Color(hex: 0xEEF5FF) : Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .overlay(RoundedRectangle(cornerRadius: 16).strokeBorder(
                selected ? Color(hex: 0x3B7BFF) : Color(hex: 0xEDEAE0),
                lineWidth: selected ? 2 : 1
            ))
            .contentShape(RoundedRectangle(cornerRadius: 16))
        }
        .buttonStyle(.plain)
        .animation(.spring(duration: 0.2), value: selected)
    }
}

private struct SGQuestionCard<Detail: View>: View {
    let number: Int
    let title: String
    @ViewBuilder let detail: () -> Detail

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .top, spacing: 8) {
                ZStack {
                    Circle()
                        .fill(Color(hex: 0xEEF5FF))
                        .frame(width: 22, height: 22)
                    Text("\(number)")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundStyle(Color(hex: 0x3B7BFF))
                }
                Text(title)
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(Color(hex: 0x16213E))
                    .lineSpacing(4)
                    .fixedSize(horizontal: false, vertical: true)
            }
            Spacer().frame(height: 8)
            detail()
        }
        .padding(14)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 14))
        .overlay(RoundedRectangle(cornerRadius: 14).strokeBorder(Color(hex: 0xEDEAE0), lineWidth: 1))
    }
}

#Preview {
    StudyGoalsView(
        childId: "preview",
        childName: "Aarav",
        childGrade: 6,
        childBoard: "CBSE",
        parentUID: "preview"
    )
}
