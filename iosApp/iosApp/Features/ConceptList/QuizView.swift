import SwiftUI

// MARK: - Entry

struct QuizView: View {
    let conceptId: String
    let childId: String
    let unitName: String
    let subject: String
    var childName: String = ""
    var parentTaskId: String = ""
    var onBack: () -> Void = {}

    @State private var phase: QuizPhase = .loading
    @State private var questions: [McqQuestion] = []
    @State private var currentIndex = 0
    @State private var selections: [Int: Int] = [:]
    @State private var revealed: Set<Int> = []
    @State private var loadError: String? = nil
    @State private var streakDays: Int = 0
    @State private var showSnapShare: Bool = false
    @State private var snapShareItem: ShareItem? = nil
    @State private var attemptsTaken: Int = 0
    @State private var hasSharedSnap: Bool = false

    private var currentQuestion: McqQuestion? { questions[safe: currentIndex] }
    private var currentSelection: Int? { selections[currentIndex] }
    private var isCurrentRevealed: Bool { revealed.contains(currentIndex) }
    private var isLastQuestion: Bool { currentIndex == questions.count - 1 }
    private var score: Int {
        revealed.filter { selections[$0] == questions[safe: $0]?.correctIndex }.count
    }

    var body: some View {
        VStack(spacing: 0) {
            // Top bar
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
                Text(navTitle)
                    .font(.system(size: 17, weight: .bold))
                    .foregroundStyle(Color.dobiTextPrimary)
                Spacer()
                Color.clear.frame(width: 36, height: 36)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(Color.dobiPageBg)

            switch phase {
            case .loading:
                loadingBody
            case .emptyBank:
                emptyBankBody
            case .intro:
                QuizIntroBody(unitName: unitName, totalQuestions: questions.count, onStart: { phase = .question })
            case .question:
                if let q = currentQuestion {
                    QuizQuestionBody(
                        question: q,
                        questionNumber: currentIndex + 1,
                        totalQuestions: questions.count,
                        selectedIndex: currentSelection,
                        isRevealed: isCurrentRevealed,
                        canGoPrevious: currentIndex > 0,
                        onSelect: selectOption,
                        onNext: nextQuestion,
                        onPrevious: { currentIndex -= 1 }
                    )
                }
            case .done:
                QuizDoneBody(
                    unitName: unitName,
                    questions: questions,
                    selections: selections,
                    score: score,
                    canRetry: attemptsTaken < 2,
                    showSnapSend: !hasSharedSnap,
                    onRetry: retry,
                    onSnapSend: { showSnapShare = true },
                    onDone: onBack
                )
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.dobiPageBg.ignoresSafeArea())
        .navigationBarHidden(true)
        .onAppear { loadQuestions() }
        .overlay {
            if showSnapShare {
                SnapShareDialog(
                    streakDays: streakDays,
                    childName: childName.isEmpty ? "Your child" : childName,
                    onDismiss: { showSnapShare = false },
                    onShare: { text in
                        showSnapShare = false
                        hasSharedSnap = true
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) {
                            snapShareItem = ShareItem(text: text)
                        }
                    }
                )
                .ignoresSafeArea()
                .transition(.opacity.animation(.easeInOut(duration: 0.2)))
            }
        }
        .animation(.easeInOut(duration: 0.2), value: showSnapShare)
        .sheet(item: $snapShareItem) { item in
            ShareSheet(items: [item.text])
        }
    }

    private var navTitle: String {
        switch phase {
        case .loading, .emptyBank, .intro: return unitName.isEmpty ? "Quick Quiz" : unitName
        case .question: return "Quick Quiz"
        case .done: return "Quiz Result"
        }
    }

    @ViewBuilder private var loadingBody: some View {
        Spacer()
        VStack(spacing: 14) {
            ProgressView().scaleEffect(1.2).tint(Color.dobiPrimary)
            if let err = loadError {
                Text(err)
                    .font(.system(size: 14))
                    .foregroundStyle(Color.dobiTextPrimary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)
                Button("Try Again") { loadQuestions() }
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(Color.dobiPrimary)
                    .padding(.top, 4)
            } else {
                Text("Preparing your quiz…")
                    .font(.system(size: 14))
                    .foregroundStyle(Color.dobiTextPrimary)
                Text("Dobi is making questions for this lesson.")
                    .font(.system(size: 12))
                    .foregroundStyle(Color.dobiTextSecondary)
            }
        }
        Spacer()
    }

    @ViewBuilder private var emptyBankBody: some View {
        Spacer()
        VStack(spacing: 18) {
            ZStack {
                Circle()
                    .fill(Color.dobiPrimaryContainer)
                    .frame(width: 80, height: 80)
                Image(systemName: "checkmark.rectangle.stack")
                    .font(.system(size: 36))
                    .foregroundStyle(Color.dobiPrimary)
            }
            Text("Curated set coming soon")
                .font(.system(size: 17, weight: .bold))
                .foregroundStyle(Color.dobiTextPrimary)
            Text("We're still preparing questions for this lesson.\nTry the Upload Your Pages option in the meantime!")
                .font(.system(size: 14))
                .foregroundStyle(Color.dobiTextSecondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 24)
            Button("Go back") { onBack() }
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(Color.dobiPrimary)
                .padding(.horizontal, 24)
                .padding(.vertical, 12)
                .overlay(RoundedRectangle(cornerRadius: 14).stroke(Color.dobiPrimary, lineWidth: 1.5))
        }
        Spacer()
    }

    // MARK: - Actions

    private func loadQuestions() {
        phase = .loading
        loadError = nil
        Task { @MainActor in
            // 0. Check in-memory cache (used by AI Practice and Write Answers anti-cheat flows)
            if let cached = GeneratedQuizCache.shared.get(conceptId), !cached.isEmpty {
                GeneratedQuizCache.shared.remove(conceptId)
                questions = cached
                selections = [:]
                revealed = []
                currentIndex = 0
                phase = .intro
                return
            }

            // 1. Always try AI first (Cloud Function) — same as Android
            let grade = conceptId.hasPrefix("sci7") ? 7 : 6
            do {
                let fetched = try await FirebaseService.shared.generateQuizQuestions(
                    conceptId: conceptId,
                    conceptName: unitName,
                    conceptDescription: "",
                    subject: subject,
                    grade: grade,
                    unitName: unitName
                )
                if !fetched.isEmpty {
                    questions = fetched
                    selections = [:]
                    revealed = []
                    currentIndex = 0
                    phase = .intro
                    return
                }
            } catch {
                print("⚠️ generateMcq failed: \(error)")
            }

            // 2. Local curated bank fallback
            let local = CuratedQuestionBank.forConcept(conceptId)
            if !local.isEmpty {
                questions = local
                selections = [:]
                revealed = []
                currentIndex = 0
                phase = .intro
            } else {
                phase = .emptyBank
            }
        }
    }

    private func selectOption(_ index: Int) {
        guard !isCurrentRevealed else { return }
        selections[currentIndex] = index
        revealed.insert(currentIndex)
    }

    private func nextQuestion() {
        guard isCurrentRevealed else { return }
        if isLastQuestion {
            phase = .done
            attemptsTaken += 1
            let finalScore = score
            let finalTotal = questions.count
            Task { @MainActor in
                streakDays = await FirebaseService.shared.fetchCurrentStreak(childId: childId)
            }
            Task { @MainActor in
                await FirebaseService.shared.saveQuizAttempt(
                    childId: childId,
                    conceptId: conceptId,
                    correct: finalScore,
                    total: finalTotal,
                    selections: selections,
                    displayName: unitName,
                    subject: subject,
                    questions: questions
                )
                if !parentTaskId.isEmpty {
                    try? await FirebaseService.shared.submitByChildQuizScore(
                        taskId: parentTaskId, score: finalScore, total: finalTotal)
                }
            }
        } else {
            currentIndex += 1
        }
    }

    private func retry() {
        selections = [:]
        revealed = []
        currentIndex = 0
        phase = .intro
    }

    private func friendlyError(_ msg: String) -> String {
        let lower = msg.lowercased()
        if lower.contains("unavailable") || lower.contains("overloaded") || lower.contains("503") {
            return "Dobi's brain is a little busy. Please try again in a moment."
        }
        if lower.contains("network") || lower.contains("offline") || lower.contains("timeout") {
            return "No internet right now. Check your connection and try again."
        }
        return "Couldn't prepare the quiz. Please try again.\n\(msg)"
    }
}

// MARK: - Phase

private enum QuizPhase { case loading, emptyBank, intro, question, done }

// MARK: - Intro

private struct QuizIntroBody: View {
    let unitName: String
    let totalQuestions: Int
    let onStart: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                VStack(spacing: 0) {
                    Spacer().frame(height: 8)

                    // QUIZ TIME! pill
                    HStack(spacing: 8) {
                        Image(systemName: "checkmark.rectangle.stack")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundStyle(Color.dobiPrimary)
                        Text("QUIZ TIME!")
                            .font(.system(size: 14, weight: .bold))
                            .foregroundStyle(Color.dobiPrimary)
                    }
                    .padding(.horizontal, 18)
                    .padding(.vertical, 10)
                    .background(Color.dobiPrimaryContainer)
                    .overlay(Capsule().stroke(Color.dobiPrimary, lineWidth: 2))
                    .clipShape(Capsule())
                    .padding(.top, 20)

                    Spacer().frame(height: 20)

                    Text("You've learned the basics of")
                        .font(.system(size: 14))
                        .foregroundStyle(Color.dobiTextSecondary)
                        .multilineTextAlignment(.center)

                    Text(unitName.isEmpty ? "this lesson" : unitName)
                        .font(.system(size: 20, weight: .bold))
                        .foregroundStyle(Color.dobiTextPrimary)
                        .multilineTextAlignment(.center)
                        .padding(.top, 4)

                    Text("Let's see how much you remember!")
                        .font(.system(size: 14))
                        .foregroundStyle(Color.dobiTextSecondary)
                        .multilineTextAlignment(.center)
                        .padding(.top, 6)

                    Spacer().frame(height: 20)

                    // Questions count pill
                    HStack(spacing: 6) {
                        Image(systemName: "questionmark.circle")
                            .font(.system(size: 14))
                            .foregroundStyle(Color.dobiPrimary)
                        Text("\(totalQuestions) Questions")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundStyle(Color.dobiPrimary)
                    }
                    .padding(.horizontal, 14)
                    .padding(.vertical, 8)
                    .background(Color.white)
                    .overlay(Capsule().stroke(Color.dobiPrimary, lineWidth: 1.5))
                    .clipShape(Capsule())

                    Spacer().frame(height: 28)

                    QuizRulesCard(totalQuestions: totalQuestions)
                        .padding(.horizontal, 20)

                    Spacer().frame(height: 24)
                }
            }

            Button(action: onStart) {
                HStack(spacing: 10) {
                    Image(systemName: "arrow.right")
                        .font(.system(size: 16, weight: .semibold))
                    Text("Start the Quiz")
                        .font(.system(size: 15, weight: .semibold))
                }
                .foregroundStyle(Color.white)
                .frame(maxWidth: .infinity)
                .frame(height: 54)
                .background(Color.dobiPrimary)
                .clipShape(RoundedRectangle(cornerRadius: 16))
            }
            .buttonStyle(.plain)
            .padding(.horizontal, 20)
            .padding(.bottom, 20)
        }
    }
}

private struct QuizRulesCard: View {
    let totalQuestions: Int

    var body: some View {
        let rules = [
            "This quiz has \(totalQuestions) questions with 4 options each.",
            "Only one option is correct per question.",
            "You need at least 70% to pass.",
            "No time limit — take your time and think.",
            "If you don't pass, revisit the lesson and try again.",
            "Your best score will be recorded."
        ]
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                Rectangle().fill(Color(hex: 0xE5EBF5)).frame(height: 1)
                Text("QUIZ RULES")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(Color.dobiTextSecondary)
                    .padding(.horizontal, 12)
                Rectangle().fill(Color(hex: 0xE5EBF5)).frame(height: 1)
            }
            .padding(.bottom, 14)

            ForEach(rules, id: \.self) { rule in
                HStack(alignment: .top, spacing: 12) {
                    // Diamond bullet
                    Canvas { ctx, size in
                        var path = Path()
                        path.move(to: CGPoint(x: size.width / 2, y: 0))
                        path.addLine(to: CGPoint(x: size.width, y: size.height / 2))
                        path.addLine(to: CGPoint(x: size.width / 2, y: size.height))
                        path.addLine(to: CGPoint(x: 0, y: size.height / 2))
                        path.closeSubpath()
                        ctx.fill(path, with: .color(Color.dobiPrimary))
                    }
                    .frame(width: 8, height: 8)
                    .padding(.top, 5)

                    Text(rule)
                        .font(.system(size: 14))
                        .foregroundStyle(Color.dobiTextPrimary)
                        .lineSpacing(3)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                .padding(.bottom, 12)
            }
        }
        .padding(20)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .overlay(RoundedRectangle(cornerRadius: 20).stroke(Color(hex: 0xE5EBF5), lineWidth: 1))
    }
}

// MARK: - Question

private struct QuizQuestionBody: View {
    let question: McqQuestion
    let questionNumber: Int
    let totalQuestions: Int
    let selectedIndex: Int?
    let isRevealed: Bool
    let canGoPrevious: Bool
    let onSelect: (Int) -> Void
    let onNext: () -> Void
    let onPrevious: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            // Progress bar
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Rectangle().fill(Color(hex: 0xE5EBF5)).frame(height: 4)
                    Rectangle()
                        .fill(Color.dobiPrimary)
                        .frame(width: geo.size.width * CGFloat(questionNumber) / CGFloat(totalQuestions), height: 4)
                        .animation(.easeInOut(duration: 0.3), value: questionNumber)
                }
            }
            .frame(height: 4)

            ScrollView {
                VStack(spacing: 0) {
                    Spacer().frame(height: 20)

                    Text("Question")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundStyle(Color.dobiTextPrimary)
                        .frame(maxWidth: .infinity, alignment: .center)

                    Spacer().frame(height: 10)

                    Text(question.text)
                        .font(.system(size: 16))
                        .foregroundStyle(Color.dobiTextPrimary)
                        .multilineTextAlignment(.center)
                        .lineSpacing(3)
                        .frame(maxWidth: .infinity, alignment: .center)
                        .padding(.horizontal, 20)

                    Spacer().frame(height: 10)

                    Text("SELECT THE MOST APPROPRIATE ANSWER")
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundStyle(Color.dobiTextSecondary)
                        .frame(maxWidth: .infinity, alignment: .center)

                    Spacer().frame(height: 20)

                    VStack(spacing: 10) {
                        ForEach(question.options.indices, id: \.self) { i in
                            OptionRow(
                                text: question.options[i],
                                state: optionState(index: i),
                                onClick: { onSelect(i) }
                            )
                        }
                    }
                    .padding(.horizontal, 20)

                    if isRevealed && selectedIndex != question.correctIndex {
                        ExplanationCard(text: question.explanation)
                            .padding(.horizontal, 20)
                            .padding(.top, 8)
                    }

                    Spacer().frame(height: 20)
                }
            }

            QuizNavBar(
                counter: "\(questionNumber)/\(totalQuestions)",
                showPrevious: canGoPrevious,
                nextEnabled: selectedIndex != nil,
                isLastReveal: isRevealed && questionNumber == totalQuestions,
                onPrevious: onPrevious,
                onNext: onNext
            )
        }
    }

    private func optionState(index: Int) -> QOptionState {
        switch (isRevealed, index == selectedIndex, index == question.correctIndex) {
        case (false, true, _): return .selected
        case (true, _, true): return .correct
        case (true, true, false): return .wrong
        default: return .default_
        }
    }
}

private enum QOptionState { case default_, selected, correct, wrong }

private struct OptionRow: View {
    let text: String
    let state: QOptionState
    let onClick: () -> Void

    private var bg: Color {
        switch state {
        case .default_: return .white
        case .selected: return Color.dobiPrimaryContainer
        case .correct: return Color(hex: 0xDCFCE7)
        case .wrong: return Color(hex: 0xFEE2E2)
        }
    }
    private var border: Color {
        switch state {
        case .default_: return Color(hex: 0xE5EBF5)
        case .selected: return Color.dobiPrimary
        case .correct: return Color(hex: 0x16A34A)
        case .wrong: return Color(hex: 0xDC2626)
        }
    }
    private var textColor: Color {
        switch state {
        case .default_, .selected: return Color.dobiTextPrimary
        case .correct: return Color(hex: 0x16A34A)
        case .wrong: return Color(hex: 0xDC2626)
        }
    }

    var body: some View {
        Button(action: onClick) {
            HStack(spacing: 12) {
                OptionIndicator(state: state, accent: border)
                Text(text)
                    .font(.system(size: 14, weight: state == .default_ ? .regular : .semibold))
                    .foregroundStyle(textColor)
                    .lineSpacing(3)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 14)
            .background(bg)
            .overlay(RoundedRectangle(cornerRadius: 14).stroke(border, lineWidth: 1.5))
            .clipShape(RoundedRectangle(cornerRadius: 14))
            .animation(.easeInOut(duration: 0.2), value: state)
        }
        .buttonStyle(.plain)
    }
}

private struct OptionIndicator: View {
    let state: QOptionState
    let accent: Color

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 8)
                .fill(Color.white)
                .frame(width: 26, height: 26)
                .overlay(RoundedRectangle(cornerRadius: 8).stroke(accent, lineWidth: 1.5))
            switch state {
            case .selected, .correct:
                Image(systemName: "checkmark")
                    .font(.system(size: 13, weight: .bold))
                    .foregroundStyle(accent)
            case .wrong:
                Image(systemName: "xmark")
                    .font(.system(size: 13, weight: .bold))
                    .foregroundStyle(accent)
            case .default_:
                EmptyView()
            }
        }
    }
}

private struct ExplanationCard: View {
    let text: String

    var body: some View {
        let green = Color(hex: 0x16A34A)
        HStack(alignment: .top, spacing: 12) {
            ZStack {
                Circle().fill(green.opacity(0.15)).frame(width: 32, height: 32)
                Image(systemName: "lightbulb.fill")
                    .font(.system(size: 15))
                    .foregroundStyle(green)
            }
            Text(text.isEmpty ? "Take a moment to think about why the correct answer fits best." : text)
                .font(.system(size: 12))
                .foregroundStyle(green)
                .lineSpacing(2)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(14)
        .background(Color(hex: 0xDCFCE7))
        .overlay(RoundedRectangle(cornerRadius: 14).stroke(green.opacity(0.35), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }
}

private struct QuizNavBar: View {
    let counter: String
    let showPrevious: Bool
    let nextEnabled: Bool
    let isLastReveal: Bool
    let onPrevious: () -> Void
    let onNext: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            if showPrevious {
                NavArrowButton(direction: .back, enabled: true, isFinal: false, onClick: onPrevious)
            }
            HStack {
                Text(counter)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(Color.dobiTextPrimary)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background(Color.white)
            .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.dobiPrimary.opacity(0.3), lineWidth: 1.5))
            .clipShape(RoundedRectangle(cornerRadius: 16))

            NavArrowButton(direction: .forward, enabled: nextEnabled, isFinal: isLastReveal, onClick: onNext)
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 14)
        .background(Color.dobiPageBg)
    }
}

private enum NavDirection { case back, forward }

private struct NavArrowButton: View {
    let direction: NavDirection
    let enabled: Bool
    let isFinal: Bool
    let onClick: () -> Void

    private var bg: Color {
        if !enabled { return Color(hex: 0xE5EBF5) }
        if isFinal { return Color(hex: 0x16A34A) }
        return Color.dobiPrimary
    }

    var body: some View {
        Button(action: onClick) {
            Group {
                if isFinal {
                    HStack(spacing: 6) {
                        Text("Finish")
                            .font(.system(size: 14, weight: .semibold))
                        Image(systemName: "arrow.right")
                            .font(.system(size: 14, weight: .semibold))
                    }
                } else {
                    Image(systemName: direction == .forward ? "arrow.right" : "arrow.left")
                        .font(.system(size: 18, weight: .semibold))
                }
            }
            .foregroundStyle(enabled ? Color.white : Color.dobiTextSecondary)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background(bg)
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .animation(.easeInOut(duration: 0.2), value: bg)
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
    }
}

// MARK: - Done

private struct QuizDoneBody: View {
    let unitName: String
    let questions: [McqQuestion]
    let selections: [Int: Int]
    let score: Int
    let canRetry: Bool
    let showSnapSend: Bool
    let onRetry: () -> Void
    let onSnapSend: () -> Void
    let onDone: () -> Void

    private var total: Int { questions.count }
    private var percent: Double { total == 0 ? 0 : Double(score) / Double(total) }
    private var percentInt: Int { Int(percent * 100) }
    private var scoreColor: Color {
        if percent >= 0.7 { return Color(hex: 0x16A34A) }
        if percent >= 0.4 { return Color(hex: 0xD97706) }
        return Color(hex: 0xDC2626)
    }

    var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                VStack(spacing: 0) {
                    Spacer().frame(height: 8)

                    ResultBanner(unitName: unitName)
                        .padding(.horizontal, 20)

                    Spacer().frame(height: 14)

                    // Attempted pill
                    HStack {
                        Spacer()
                        HStack(spacing: 8) {
                            Image(systemName: "questionmark.circle")
                                .font(.system(size: 14))
                                .foregroundStyle(Color.dobiPrimary)
                            Text("Attempted Questions: \(selections.count) / \(total)")
                                .font(.system(size: 13, weight: .semibold))
                                .foregroundStyle(Color.dobiTextPrimary)
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 10)
                        .background(Color.white)
                        .overlay(Capsule().stroke(Color.dobiPrimary.opacity(0.3), lineWidth: 1.5))
                        .clipShape(Capsule())
                        Spacer()
                    }

                    Spacer().frame(height: 20)

                    // RESULT divider (matches Android "RESULT" label)
                    HStack {
                        Rectangle().fill(Color(hex: 0xE5EBF5)).frame(height: 1)
                        Text("RESULT")
                            .font(.system(size: 11, weight: .semibold))
                            .foregroundStyle(Color.dobiTextSecondary)
                            .padding(.horizontal, 12)
                        Rectangle().fill(Color(hex: 0xE5EBF5)).frame(height: 1)
                    }
                    .padding(.horizontal, 20)

                    Spacer().frame(height: 16)

                    // Score card — horizontal layout with ring (matches Android)
                    ScoreCard(percentInt: percentInt, correct: score, total: total, scoreColor: scoreColor)
                        .padding(.horizontal, 20)

                    Spacer().frame(height: 20)

                    // Per-question review
                    VStack(spacing: 12) {
                        ForEach(questions.indices, id: \.self) { i in
                            QuestionReviewCard(
                                number: i + 1,
                                question: questions[i],
                                userPick: selections[i]
                            )
                        }
                    }
                    .padding(.horizontal, 20)

                    Spacer().frame(height: 8)
                }
            }

            // Bottom action bar — matches Android: outline Retake (if canRetry) + solid primary button
            VStack(spacing: 12) {
                if canRetry {
                    Button(action: onRetry) {
                        HStack(spacing: 8) {
                            Image(systemName: "arrow.clockwise")
                                .font(.system(size: 14, weight: .semibold))
                            Text("Retake Quiz")
                                .font(.system(size: 14, weight: .semibold))
                        }
                        .foregroundStyle(Color.dobiPrimary)
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                        .background(Color.white)
                        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.dobiPrimary, lineWidth: 1.5))
                        .clipShape(RoundedRectangle(cornerRadius: 16))
                    }
                    .buttonStyle(.plain)
                }

                // Single primary button: Snap & Send OR Done (mirrors Android)
                Button(action: showSnapSend ? onSnapSend : onDone) {
                    HStack(spacing: 8) {
                        if showSnapSend {
                            Text("📸").font(.system(size: 16))
                            Text("Snap & Send").font(.system(size: 15, weight: .semibold))
                        } else {
                            Text("Done").font(.system(size: 15, weight: .semibold))
                        }
                    }
                    .foregroundStyle(Color.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 52)
                    .background(Color.dobiPrimary)
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                }
                .buttonStyle(.plain)
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 14)
            .background(Color.dobiPageBg)
        }
    }
}

private struct ResultBanner: View {
    let unitName: String

    var body: some View {
        VStack(alignment: .center, spacing: 12) {
            HStack(spacing: 10) {
                Text("👍").font(.system(size: 22))
                Text("Thank you for taking the Quiz")
                    .font(.system(size: 17, weight: .bold))
                    .foregroundStyle(Color.white)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 28)
            .padding(.horizontal, 20)
            .background(
                LinearGradient(
                    colors: [Color(hex: 0x7AB8F5), Color(hex: 0xB78CF0), Color(hex: 0xF59CA1), Color(hex: 0xF5B36A)],
                    startPoint: .leading, endPoint: .trailing
                )
            )
            .clipShape(RoundedRectangle(cornerRadius: 20))

            if !unitName.isEmpty {
                Text("You've practised the fundamentals of \(unitName). Here's how you did.")
                    .font(.system(size: 14))
                    .foregroundStyle(Color.dobiTextSecondary)
                    .multilineTextAlignment(.center)
                    .lineSpacing(3)
            }
        }
    }
}

// Horizontal layout: left = SCORE label + big %, right = ring. Matches Android.
private struct ScoreCard: View {
    let percentInt: Int
    let correct: Int
    let total: Int
    let scoreColor: Color

    var body: some View {
        HStack(alignment: .center, spacing: 20) {
            VStack(alignment: .leading, spacing: 4) {
                Text("SCORE")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(Color.dobiTextSecondary)
                Text("\(percentInt)%")
                    .font(.system(size: 36, weight: .bold))
                    .foregroundStyle(Color.dobiTextPrimary)
                Text("\(correct) / \(total) correct")
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(Color.dobiTextSecondary)
            }
            Spacer()
            ScoreRing(percent: Double(percentInt) / 100.0, color: scoreColor, label: "\(percentInt)%")
        }
        .padding(20)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .overlay(RoundedRectangle(cornerRadius: 20).stroke(Color(hex: 0xE5EBF5), lineWidth: 1))
    }
}

private struct ScoreRing: View {
    let percent: Double
    let color: Color
    let label: String

    var body: some View {
        ZStack {
            Circle()
                .stroke(Color(hex: 0xE5EBF5), lineWidth: 8)
            Circle()
                .trim(from: 0, to: CGFloat(percent.clamped(to: 0...1)))
                .stroke(color, style: StrokeStyle(lineWidth: 8, lineCap: .round))
                .rotationEffect(.degrees(-90))
                .animation(.easeInOut(duration: 0.6), value: percent)
            Text(label)
                .font(.system(size: 13, weight: .bold))
                .foregroundStyle(Color.dobiTextPrimary)
        }
        .frame(width: 78, height: 78)
    }
}

// Matches Android: card shows question, then "Correct" pill, then "Your answer" pill if wrong.
private struct QuestionReviewCard: View {
    let number: Int
    let question: McqQuestion
    let userPick: Int?

    private var isCorrect: Bool { userPick == question.correctIndex }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Q\(number). \(question.text)")
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(Color.dobiTextPrimary)
                .lineSpacing(3)
                .frame(maxWidth: .infinity, alignment: .leading)

            // Always show correct answer
            ReviewPill(
                optionText: question.options[safe: question.correctIndex] ?? "",
                label: "Correct",
                accent: Color(hex: 0x16A34A),
                muted: false
            )

            // If wrong, show user's pick
            if let pick = userPick, !isCorrect {
                ReviewPill(
                    optionText: question.options[safe: pick] ?? "",
                    label: "Your answer",
                    accent: Color(hex: 0xDC2626),
                    muted: true
                )
            }

            // If not attempted
            if userPick == nil {
                ReviewPill(
                    optionText: "Not attempted",
                    label: "Your answer",
                    accent: Color(hex: 0x94A3B8),
                    muted: true
                )
            }
        }
        .padding(16)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 18))
        .overlay(RoundedRectangle(cornerRadius: 18).stroke(Color(hex: 0xE5EBF5), lineWidth: 1))
    }
}

private struct ReviewPill: View {
    let optionText: String
    let label: String
    let accent: Color
    let muted: Bool

    var body: some View {
        HStack(spacing: 10) {
            Text(optionText)
                .font(.system(size: 14))
                .foregroundStyle(Color.dobiTextPrimary)
                .lineSpacing(3)
                .frame(maxWidth: .infinity, alignment: .leading)
            Text(label)
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(accent)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(muted ? Color(hex: 0xF8FAFC) : accent.opacity(0.06))
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(accent.opacity(muted ? 0.4 : 0.7), lineWidth: 1.5))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

// MARK: - Helpers

private extension Array {
    subscript(safe index: Int) -> Element? {
        indices.contains(index) ? self[index] : nil
    }
}

