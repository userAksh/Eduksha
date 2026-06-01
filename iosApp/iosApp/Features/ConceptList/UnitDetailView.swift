import SwiftUI

// MARK: - Screen

struct UnitDetailView: View {
    let unitId: String
    let childId: String
    let unitName: String
    let subject: String
    var onBack: () -> Void = {}
    var onNavigateToUploadWork: (_ kind: String, _ subjectColor: String, _ subjectIcon: String) -> Void = { _, _, _ in }
    var onNavigateToAnswerQuestions: (_ subjectColor: String, _ subjectIcon: String) -> Void = { _, _ in }

    @EnvironmentObject private var appState: AppState

    @State private var concepts: [StudyGoalsConcept] = []
    @State private var isLoading = true
    @State private var hasLoaded = false
    @State private var coveredConceptIds: Set<String> = []
    @State private var isRevisionMarked = false
    @State private var isTogglingRevision = false
    @State private var hasActivity = false
    @State private var selectedWorkMode: CDWorkMode? = nil
    @State private var selectedDifficulty: CDDifficulty? = nil
    @State private var isSaving = false
    @State private var isSaved = false
    @State private var toastMessage: String? = nil

    private var subjectColor: String { concepts.first?.subjectColor ?? "" }
    private var subjectIcon: String { concepts.first?.subjectIcon ?? "" }

    private var canMarkAsDone: Bool {
        !coveredConceptIds.isEmpty && selectedDifficulty != nil && hasActivity
    }

    var body: some View {
        VStack(spacing: 0) {
            // Nav bar
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
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 12)
            .background(Color.dobiPageBg)

            if isLoading {
                Spacer()
                ProgressView()
                    .scaleEffect(1.2)
                    .tint(Color.dobiPrimary)
                Spacer()
            } else if concepts.isEmpty {
                Spacer()
                Text("Lesson not found")
                    .font(.system(size: 14))
                    .foregroundStyle(Color.dobiTextSecondary)
                Spacer()
            } else {
                ScrollView {
                    VStack(alignment: .leading, spacing: 0) {

                        // "Lesson" badge
                        HStack(spacing: 4) {
                            Text("📘")
                                .font(.system(size: 12))
                            Text("Lesson")
                                .font(.system(size: 11, weight: .semibold))
                                .foregroundStyle(Color.dobiPrimary)
                        }
                        .padding(.horizontal, 8)
                        .padding(.vertical, 3)
                        .background(Color.dobiPrimaryContainer)
                        .clipShape(RoundedRectangle(cornerRadius: 6))

                        // Unit title
                        Text(unitName)
                            .font(.system(size: 22, weight: .bold))
                            .foregroundStyle(Color.dobiTextPrimary)
                            .padding(.top, 10)

                        // Breadcrumb chips
                        HStack(spacing: 8) {
                            CDBreadcrumbChip(label: subject)
                            CDBreadcrumbChip(label: "\(concepts.count) concepts")
                        }
                        .padding(.top, 10)

                        // Revision toggle
                        CDRevisionToggle(
                            isMarked: isRevisionMarked,
                            isBusy: isTogglingRevision,
                            onClick: { toggleRevision() }
                        )
                        .padding(.top, 16)

                        // "What's covered" card
                        UDConceptsCard(
                            concepts: concepts,
                            coveredConceptIds: coveredConceptIds,
                            onToggle: { id in
                                if coveredConceptIds.contains(id) {
                                    coveredConceptIds.remove(id)
                                } else {
                                    coveredConceptIds.insert(id)
                                }
                            }
                        )
                        .padding(.top, 20)

                        // Section 1 — Work mode
                        CDSectionLabel(number: "1", title: "Show us how you studied this lesson")
                            .padding(.top, 28)

                        HStack(spacing: 8) {
                            CDWorkModeCard(
                                icon: "graduationcap.fill",
                                title: "Upload Class Work",
                                subtitle: "Notes I made\nin class",
                                selected: selectedWorkMode == .classWork,
                                onClick: {
                                    selectedWorkMode = .classWork
                                    onNavigateToUploadWork("class_work", subjectColor, subjectIcon)
                                }
                            )
                            CDWorkModeCard(
                                icon: "book.fill",
                                title: "Upload Self Learning",
                                subtitle: "Notes I made\non my own",
                                selected: selectedWorkMode == .selfLearning,
                                onClick: {
                                    selectedWorkMode = .selfLearning
                                    onNavigateToUploadWork("self_learning", subjectColor, subjectIcon)
                                }
                            )
                            CDWorkModeCard(
                                icon: "questionmark.circle",
                                title: "Answer Questions",
                                subtitle: "I practiced\nwith questions",
                                selected: selectedWorkMode == .questions,
                                onClick: {
                                    selectedWorkMode = .questions
                                    onNavigateToAnswerQuestions(subjectColor, subjectIcon)
                                }
                            )
                        }
                        .padding(.top, 12)

                        // Section 2 — Difficulty
                        CDSectionLabel(number: "2", title: "How difficult was the whole lesson?")
                            .padding(.top, 28)

                        HStack(spacing: 8) {
                            CDDifficultyCard(
                                emoji: "😊",
                                label: "Easy",
                                description: "I understood\nit well",
                                selected: selectedDifficulty == .easy,
                                selectedColor: Color(hex: 0x16A34A),
                                selectedBg: Color(hex: 0xDCFCE7),
                                onClick: { selectedDifficulty = .easy }
                            )
                            CDDifficultyCard(
                                emoji: "😐",
                                label: "OK",
                                description: "It was okay,\nsome doubts",
                                selected: selectedDifficulty == .ok,
                                selectedColor: Color(hex: 0xD97706),
                                selectedBg: Color(hex: 0xFEF3C7),
                                onClick: { selectedDifficulty = .ok }
                            )
                            CDDifficultyCard(
                                emoji: "😓",
                                label: "Hard",
                                description: "I found it\ndifficult",
                                selected: selectedDifficulty == .hard,
                                selectedColor: Color(hex: 0xDC2626),
                                selectedBg: Color(hex: 0xFEE2E2),
                                onClick: { selectedDifficulty = .hard }
                            )
                        }
                        .padding(.top, 12)

                        // Mark Lesson as Done CTA
                        Button(action: { handleMarkAsDone() }) {
                            ZStack {
                                if isSaving {
                                    ProgressView()
                                        .scaleEffect(0.9)
                                        .tint(Color.white)
                                } else {
                                    Text("Mark Lesson as Done")
                                        .font(.system(size: 15, weight: .semibold))
                                        .foregroundStyle(Color.white)
                                }
                            }
                            .frame(maxWidth: .infinity)
                            .frame(height: 52)
                            .background(canMarkAsDone ? Color.dobiPrimary : Color(hex: 0xBFD7F7))
                            .clipShape(RoundedRectangle(cornerRadius: 14))
                        }
                        .buttonStyle(.plain)
                        .disabled(isSaving)
                        .padding(.top, 32)
                        .padding(.bottom, 40)
                    }
                    .padding(.horizontal, 20)
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.dobiPageBg.ignoresSafeArea())
        .navigationBarHidden(true)
        .overlay(alignment: .bottom) {
            if let msg = toastMessage {
                Text(msg)
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(Color.white)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 20)
                    .padding(.vertical, 12)
                    .background(Color.black.opacity(0.78))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                    .padding(.horizontal, 32)
                    .padding(.bottom, 56)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
        .animation(.easeInOut(duration: 0.25), value: toastMessage)
        .onAppear {
            if !hasLoaded {
                loadUnit()
                hasLoaded = true
            } else {
                reloadActivity()
            }
        }
        .onChange(of: isSaved) { saved in
            if saved { onBack() }
        }
    }

    // MARK: - Actions

    private func loadUnit() {
        Task { @MainActor in
            isLoading = true
            let child = appState.children.first { $0.id == childId }
            let grade = Int(child?.grade ?? "6") ?? 6
            let board = child?.board ?? "CBSE"
            async let conceptsFetch = FirebaseService.shared.getConcepts(grade: grade, board: board)
            async let activityFetch = FirebaseService.shared.hasUnitActivity(childId: childId, unitId: unitId)
            async let revisionFetch = FirebaseService.shared.isUnitRevisionMarked(childId: childId, unitId: unitId)
            let all = (try? await conceptsFetch) ?? []
            concepts = all.filter { $0.unitId == unitId }.sorted { $0.order < $1.order }
            hasActivity = await activityFetch
            isRevisionMarked = await revisionFetch
            isLoading = false
        }
    }

    private func reloadActivity() {
        Task { @MainActor in
            hasActivity = await FirebaseService.shared.hasUnitActivity(childId: childId, unitId: unitId)
        }
    }

    private func toggleRevision() {
        guard !isTogglingRevision else { return }
        isTogglingRevision = true
        Task { @MainActor in
            isRevisionMarked = await FirebaseService.shared.toggleUnitRevisionMark(
                childId: childId,
                unitId: unitId,
                unitName: unitName,
                subject: subject,
                subjectColor: subjectColor,
                subjectIcon: subjectIcon
            )
            isTogglingRevision = false
        }
    }

    private func handleMarkAsDone() {
        guard hasActivity else {
            showToast("Please upload your work or answer questions first.")
            return
        }
        guard !coveredConceptIds.isEmpty else {
            showToast("Toggle at least one concept you covered.")
            return
        }
        guard let difficulty = selectedDifficulty else {
            showToast("Please select how difficult this lesson was.")
            return
        }
        let mode = selectedWorkMode ?? .questions
        isSaving = true
        Task { @MainActor in
            await FirebaseService.shared.saveUnitCompletion(
                childId: childId,
                unitId: unitId,
                conceptIds: Array(coveredConceptIds),
                difficulty: difficulty.rawValue,
                mode: mode.rawValue
            )
            isSaving = false
            isSaved = true
        }
    }

    private func showToast(_ message: String) {
        toastMessage = message
        Task {
            try? await Task.sleep(nanoseconds: 2_500_000_000)
            await MainActor.run { toastMessage = nil }
        }
    }
}

// MARK: - Concepts in lesson card

private struct UDConceptsCard: View {
    let concepts: [StudyGoalsConcept]
    let coveredConceptIds: Set<String>
    let onToggle: (String) -> Void

    private var showCoachMark: Bool { coveredConceptIds.isEmpty && !concepts.isEmpty }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("What's covered")
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(Color.dobiTextPrimary)
            Text("Toggle the concepts you actually studied.")
                .font(.system(size: 12))
                .foregroundStyle(Color.dobiTextSecondary)
                .padding(.top, 2)

            if showCoachMark {
                UDCoachTooltip()
                    .padding(.top, 10)
            }

            VStack(spacing: 0) {
                ForEach(concepts, id: \.id) { concept in
                    let checked = coveredConceptIds.contains(concept.id)
                    Button(action: { onToggle(concept.id) }) {
                        HStack(spacing: 12) {
                            UDToggle(checked: checked)
                            Text(concept.name)
                                .font(.system(size: 14))
                                .foregroundStyle(checked ? Color.dobiTextPrimary : Color.dobiTextSecondary)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .multilineTextAlignment(.leading)
                        }
                        .padding(.vertical, 8)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.top, showCoachMark ? 4 : 12)
        }
        .padding(16)
        .background(Color(hex: 0xF8FAFF))
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(Color(hex: 0xE5EBF5), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}

// MARK: - Coach tooltip

private struct UDCoachTooltip: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("✨ Tap a toggle to mark what you covered")
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(Color.white)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(Color.dobiPrimary)
                .clipShape(RoundedRectangle(cornerRadius: 12))

            // Pointer triangle
            Triangle()
                .fill(Color.dobiPrimary)
                .frame(width: 10, height: 6)
                .padding(.leading, 14)
        }
    }
}

private struct Triangle: Shape {
    func path(in rect: CGRect) -> Path {
        Path { p in
            p.move(to: CGPoint(x: rect.midX, y: rect.maxY))
            p.addLine(to: CGPoint(x: rect.minX, y: rect.minY))
            p.addLine(to: CGPoint(x: rect.maxX, y: rect.minY))
            p.closeSubpath()
        }
    }
}

// MARK: - Custom toggle

private struct UDToggle: View {
    let checked: Bool

    var body: some View {
        ZStack(alignment: .leading) {
            RoundedRectangle(cornerRadius: 11)
                .fill(checked ? Color.dobiPrimary : Color(hex: 0xD8DFEA))
                .frame(width: 36, height: 22)

            Circle()
                .fill(Color.white)
                .frame(width: 18, height: 18)
                .padding(.leading, checked ? 16 : 2)
                .animation(.spring(response: 0.3, dampingFraction: 0.7), value: checked)
        }
        .frame(width: 36, height: 22)
        .animation(.spring(response: 0.3, dampingFraction: 0.7), value: checked)
    }
}
