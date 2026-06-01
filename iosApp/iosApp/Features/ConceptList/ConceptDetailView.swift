import SwiftUI

// MARK: - Screen

struct ConceptDetailView: View {
    let conceptId: String
    let childId: String
    var onBack: () -> Void = {}
    var onNavigateToUploadWork: (_ kind: String, _ unitName: String, _ conceptName: String, _ subject: String, _ subjectColor: String, _ subjectIcon: String, _ unitId: String) -> Void = { _, _, _, _, _, _, _ in }
    var onNavigateToAnswerQuestions: (_ unitName: String, _ conceptName: String, _ subject: String, _ subjectColor: String, _ unitId: String) -> Void = { _, _, _, _, _ in }

    @State private var concept: StudyGoalsConcept? = nil
    @State private var isLoading = true
    @State private var hasLoaded = false
    @State private var isRevisionMarked = false
    @State private var isTogglingRevision = false
    @State private var selectedDifficulty: CDDifficulty? = nil
    @State private var selectedWorkMode: CDWorkMode? = nil
    @State private var hasActivity = false
    @State private var isSaving = false
    @State private var isSaved = false
    @State private var toastMessage: String? = nil

    private var canMarkAsDone: Bool {
        hasActivity && selectedDifficulty != nil
    }

    var body: some View {
        VStack(spacing: 0) {
            // Nav bar — compact, no dead space
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
            } else if let concept {
                ScrollView {
                    VStack(alignment: .leading, spacing: 0) {
                        // Concept title
                        Text(concept.name)
                            .font(.system(size: 22, weight: .bold))
                            .foregroundStyle(Color.dobiTextPrimary)

                        // Breadcrumb chips
                        HStack(spacing: 8) {
                            CDBreadcrumbChip(label: concept.subject)
                            if !concept.unit.isEmpty {
                                CDBreadcrumbChip(label: concept.unit)
                            }
                        }
                        .padding(.top, 10)

                        // Revision toggle
                        CDRevisionToggle(
                            isMarked: isRevisionMarked,
                            isBusy: isTogglingRevision,
                            onClick: { toggleRevision(concept: concept) }
                        )
                        .padding(.top, 16)

                        // Description
                        Text(concept.description.isEmpty ? " " : concept.description)
                            .font(.system(size: 14))
                            .foregroundStyle(Color.dobiTextSecondary)
                            .lineSpacing(4)
                            .padding(.top, 16)

                        // Section 1 — Work mode
                        CDSectionLabel(number: "1", title: "Show us how you studied this concept")
                            .padding(.top, 28)

                        HStack(spacing: 8) {
                            CDWorkModeCard(
                                icon: "graduationcap.fill",
                                title: "Upload Class Work",
                                subtitle: "Notes I made\nin class",
                                selected: selectedWorkMode == .classWork,
                                onClick: {
                                    selectedWorkMode = .classWork
                                    onNavigateToUploadWork("class_work", concept.unit, concept.name, concept.subject, concept.subjectColor, concept.subjectIcon, concept.unitId)
                                }
                            )
                            CDWorkModeCard(
                                icon: "book.fill",
                                title: "Upload Self Learning",
                                subtitle: "Notes I made\non my own",
                                selected: selectedWorkMode == .selfLearning,
                                onClick: {
                                    selectedWorkMode = .selfLearning
                                    onNavigateToUploadWork("self_learning", concept.unit, concept.name, concept.subject, concept.subjectColor, concept.subjectIcon, concept.unitId)
                                }
                            )
                            CDWorkModeCard(
                                icon: "questionmark.circle",
                                title: "Answer Questions",
                                subtitle: "I practiced\nwith questions",
                                selected: selectedWorkMode == .questions,
                                onClick: {
                                    selectedWorkMode = .questions
                                    onNavigateToAnswerQuestions(concept.unit, concept.name, concept.subject, concept.subjectColor, concept.unitId)
                                }
                            )
                        }
                        .padding(.top, 12)

                        // Section 2 — Difficulty
                        CDSectionLabel(number: "2", title: "How difficult was it?")
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

                        // Mark as Done CTA
                        Button(action: { handleMarkAsDone(concept: concept) }) {
                            ZStack {
                                if isSaving {
                                    ProgressView()
                                        .scaleEffect(0.9)
                                        .tint(Color.white)
                                } else {
                                    Text("Mark as Done")
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
            } else {
                Spacer()
                Text("Concept not found")
                    .font(.system(size: 14))
                    .foregroundStyle(Color.dobiTextSecondary)
                Spacer()
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
                loadConcept()
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

    private func loadConcept() {
        Task { @MainActor in
            isLoading = true
            async let conceptFetch = FirebaseService.shared.getConceptById(conceptId: conceptId)
            async let revisionFetch = FirebaseService.shared.isRevisionMarked(childId: childId, conceptId: conceptId)
            async let activityFetch = FirebaseService.shared.hasConceptActivity(childId: childId, conceptId: conceptId)
            concept = try? await conceptFetch
            isRevisionMarked = await revisionFetch
            hasActivity = await activityFetch
            isLoading = false
        }
    }

    private func reloadActivity() {
        Task { @MainActor in
            hasActivity = await FirebaseService.shared.hasConceptActivity(childId: childId, conceptId: conceptId)
        }
    }

    private func toggleRevision(concept: StudyGoalsConcept) {
        guard !isTogglingRevision else { return }
        isTogglingRevision = true
        Task { @MainActor in
            let nowMarked = await FirebaseService.shared.toggleRevisionMark(
                childId: childId,
                conceptId: conceptId,
                conceptName: concept.name,
                subject: concept.subject,
                subjectColor: concept.subjectColor,
                subjectIcon: concept.subjectIcon,
                unitId: concept.unitId,
                unitName: concept.unit
            )
            isRevisionMarked = nowMarked
            isTogglingRevision = false
        }
    }

    private func handleMarkAsDone(concept: StudyGoalsConcept) {
        guard hasActivity else {
            showToast("First attempt a quiz, upload class work, or upload self learning to mark this as done.")
            return
        }
        guard let difficulty = selectedDifficulty else {
            showToast("Please select how difficult this concept was.")
            return
        }
        isSaving = true
        Task { @MainActor in
            let mode = selectedWorkMode ?? .questions
            await FirebaseService.shared.saveConceptCompletion(
                childId: childId,
                conceptId: conceptId,
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

// MARK: - Supporting types

enum CDDifficulty: String {
    case easy = "EASY"
    case ok = "OK"
    case hard = "HARD"
}

enum CDWorkMode: String {
    case classWork = "CLASS_WORK"
    case selfLearning = "SELF_LEARNING"
    case questions = "QUESTIONS"
}

// MARK: - Section label

struct CDSectionLabel: View {
    let number: String
    let title: String

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text("\(number). \(title)")
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(Color.dobiTextPrimary)
            Text("Choose one")
                .font(.system(size: 12))
                .foregroundStyle(Color.dobiTextSecondary)
        }
    }
}

// MARK: - Breadcrumb chip

struct CDBreadcrumbChip: View {
    let label: String

    var body: some View {
        Text(label)
            .font(.system(size: 12, weight: .medium))
            .foregroundStyle(Color.dobiPrimary)
            .padding(.horizontal, 10)
            .padding(.vertical, 5)
            .background(Color.dobiPrimaryContainer)
            .clipShape(RoundedRectangle(cornerRadius: 8))
    }
}

// MARK: - Revision toggle

struct CDRevisionToggle: View {
    let isMarked: Bool
    let isBusy: Bool
    let onClick: () -> Void

    private let accent = Color(hex: 0xF59E0B)

    var body: some View {
        let containerBg = isMarked ? accent.opacity(0.12) : Color(hex: 0xF8FAFF)
        let border = isMarked ? accent.opacity(0.5) : Color(hex: 0xE5E7EB)
        let titleColor = isMarked ? accent : Color.dobiTextPrimary
        let title = isMarked ? "Marked for Revision" : "Mark for Revision"
        let subtitle = isMarked
            ? "We'll keep this in your Need Revision list."
            : "Tap to add this to your Need Revision list."

        Button(action: onClick) {
            HStack(spacing: 12) {
                ZStack {
                    RoundedRectangle(cornerRadius: 12)
                        .fill(isMarked ? accent : Color.white)
                        .frame(width: 40, height: 40)
                    if isBusy {
                        ProgressView()
                            .scaleEffect(0.7)
                            .tint(isMarked ? Color.white : accent)
                    } else {
                        Image(systemName: isMarked ? "checkmark" : "bookmark")
                            .font(.system(size: 16, weight: .medium))
                            .foregroundStyle(isMarked ? Color.white : accent)
                    }
                }

                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(titleColor)
                    Text(subtitle)
                        .font(.system(size: 11))
                        .foregroundStyle(Color.dobiTextSecondary)
                        .lineLimit(2)
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                Image(systemName: isMarked ? "bookmark.fill" : "bookmark")
                    .font(.system(size: 16))
                    .foregroundStyle(isMarked ? accent : Color.dobiTextSecondary)
            }
            .padding(14)
            .background(containerBg)
            .overlay(
                RoundedRectangle(cornerRadius: 14)
                    .stroke(border, lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 14))
        }
        .buttonStyle(.plain)
        .disabled(isBusy)
    }
}

// MARK: - Work mode card

struct CDWorkModeCard: View {
    let icon: String
    let title: String
    let subtitle: String
    let selected: Bool
    let onClick: () -> Void

    var body: some View {
        Button(action: onClick) {
            VStack(spacing: 10) {
                ZStack {
                    RoundedRectangle(cornerRadius: 12)
                        .fill(selected ? Color.dobiPrimary.opacity(0.12) : Color(hex: 0xF1F5F9))
                        .frame(width: 48, height: 48)
                    Image(systemName: icon)
                        .font(.system(size: 22))
                        .foregroundStyle(selected ? Color.dobiPrimary : Color.dobiTextSecondary)
                }
                Text(title)
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(selected ? Color.dobiPrimary : Color.dobiTextPrimary)
                    .multilineTextAlignment(.center)
                    .lineLimit(2)
                    .fixedSize(horizontal: false, vertical: true)
                Text(subtitle)
                    .font(.system(size: 10))
                    .foregroundStyle(Color.dobiTextSecondary)
                    .multilineTextAlignment(.center)
                    .lineLimit(2)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .padding(12)
            .frame(maxWidth: .infinity)
            .background(selected ? Color.dobiPrimaryContainer : Color.white)
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .stroke(selected ? Color.dobiPrimary : Color(hex: 0xE2E8F0), lineWidth: selected ? 2 : 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 16))
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Difficulty card

struct CDDifficultyCard: View {
    let emoji: String
    let label: String
    let description: String
    let selected: Bool
    let selectedColor: Color
    let selectedBg: Color
    let onClick: () -> Void

    var body: some View {
        Button(action: onClick) {
            VStack(spacing: 6) {
                Text(emoji)
                    .font(.system(size: 28))
                Text(label)
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(selected ? selectedColor : Color.dobiTextPrimary)
                Text(description)
                    .font(.system(size: 10))
                    .foregroundStyle(Color.dobiTextSecondary)
                    .multilineTextAlignment(.center)
                    .lineLimit(2)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .padding(.vertical, 16)
            .padding(.horizontal, 6)
            .frame(maxWidth: .infinity)
            .background(selected ? selectedBg : Color.white)
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .stroke(selected ? selectedColor : Color(hex: 0xE2E8F0), lineWidth: selected ? 2 : 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 16))
        }
        .buttonStyle(.plain)
    }
}
