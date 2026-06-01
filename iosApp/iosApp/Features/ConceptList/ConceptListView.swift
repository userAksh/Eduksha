import SwiftUI

// ── Models ─────────────────────────────────────────────────────────────────────

struct Subject: Identifiable {
    let id: String
    let name: String
    let emoji: String
    let color: String       // hex string e.g. "#3B7BFF"
    var units: [Unit]
    var isExpanded = false
}

struct Unit: Identifiable {
    let id: String
    let name: String
    var concepts: [Concept]
    var isExpanded = false
}

struct Concept: Identifiable {
    let id: String
    let name: String
    let isCompleted: Bool
    let unitId: String
    let unitName: String
    let subject: String
    let subjectColor: String
}

enum ConceptListMode { case `default`, practiseLesson, testYourself }

// ── Main view ──────────────────────────────────────────────────────────────────

struct ConceptListView: View {
    var childId: String
    var mode: ConceptListMode = .default
    var onBack: () -> Void = {}
    var onConceptTap: (Concept) -> Void = { _ in }
    // unit tap — fired in .practiseLesson and .testYourself modes (direct navigate, no expand)
    var onUnitTap: ((Unit, String, String) -> Void)? = nil  // (unit, subjectName, subjectColor)

    @EnvironmentObject private var appState: AppState
    @State private var searchQuery = ""
    @State private var subjects: [Subject] = []
    @State private var isLoading = false

    private var title: String {
        switch mode {
        case .practiseLesson: return "Pick a Lesson"
        case .testYourself: return "Test Yourself"
        default: return "Concepts"
        }
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
                    Text(title)
                        .font(.dobiTitle3())
                        .foregroundStyle(Color.dobiTextPrimary)
                    Spacer()
                    Color.clear.frame(width: 36, height: 36)
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .background(Color.dobiPageBg)

                // Search bar
                HStack(spacing: 10) {
                    Image(systemName: "magnifyingglass")
                        .font(.system(size: 15))
                        .foregroundStyle(Color.dobiTextSecondary)
                    TextField(text: $searchQuery, prompt: .dobiPlaceholder("Search concepts, subjects…")) {}
                        .font(.system(size: 14))
                        .foregroundStyle(Color.dobiTextPrimary)
                        .autocorrectionDisabled()
                    if !searchQuery.isEmpty {
                        Button { searchQuery = "" } label: {
                            Image(systemName: "xmark.circle.fill")
                                .foregroundStyle(Color.dobiTextSecondary)
                        }
                    }
                }
                .padding(.horizontal, 14)
                .padding(.vertical, 10)
                .background(Color.white)
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .padding(.horizontal, 16)
                .padding(.vertical, 8)

                if isLoading {
                    Spacer()
                    ProgressView()
                        .scaleEffect(1.2)
                        .tint(Color.dobiPrimary)
                    Spacer()
                } else if displaySubjects.isEmpty && !searchQuery.isEmpty {
                    Spacer()
                    Text("No results for \"\(searchQuery)\"")
                        .font(.system(size: 14))
                        .foregroundStyle(Color.dobiTextSecondary)
                    Spacer()
                } else if subjects.isEmpty {
                    Spacer()
                    Text("No concepts found")
                        .font(.system(size: 14))
                        .foregroundStyle(Color.dobiTextSecondary)
                    Spacer()
                } else {
                    ScrollView {
                        VStack(spacing: 0) {
                            ForEach(displaySubjects) { subjectView in
                                VStack(spacing: 0) {
                                    CLSubjectRow(
                                        subject: subjectView,
                                        isExpanded: subjectView.isExpanded,
                                        onToggle: {
                                            if let si = subjects.firstIndex(where: { $0.id == subjectView.id }) {
                                                subjects[si].isExpanded.toggle()
                                            }
                                        }
                                    )
                                    if subjectView.isExpanded {
                                        ForEach(subjectView.units) { unitView in
                                            let subjectName = subjectView.name
                                            VStack(spacing: 0) {
                                                CLUnitRow(
                                                    unit: unitView,
                                                    isPractise: mode == .practiseLesson || mode == .testYourself,
                                                    onToggle: {
                                                        if mode == .practiseLesson || mode == .testYourself {
                                                            onUnitTap?(unitView, subjectName, subjectView.color)
                                                        } else if let si = subjects.firstIndex(where: { $0.id == subjectView.id }),
                                                                  let ui = subjects[si].units.firstIndex(where: { $0.id == unitView.id }) {
                                                            subjects[si].units[ui].isExpanded.toggle()
                                                        }
                                                    }
                                                )
                                                if unitView.isExpanded || mode == .practiseLesson || mode == .testYourself {
                                                    ForEach(unitView.concepts) { concept in
                                                        CLConceptRow(
                                                            concept: concept,
                                                            onTap: { onConceptTap(concept) }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            Color.clear.frame(height: 24)
                        }
                        .background(Color.white)
                    }
                    .background(Color.white)
                }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.dobiPageBg.ignoresSafeArea())
        .navigationBarHidden(true)
        .onAppear { loadConcepts() }
    }

    // MARK: - Filtering

    // Mirrors Android's filterGroups exactly:
    // - Filters concepts within units (only matching concepts shown)
    // - Filters units within subjects (only units with matching concepts shown)
    // - Filters subjects (only subjects with matching units shown)
    // - Auto-expands matched subjects and units
    // When query is blank, returns subjects as-is (preserving expand state).
    private var displaySubjects: [Subject] {
        guard !searchQuery.isEmpty else { return subjects }
        let q = searchQuery.lowercased().trimmingCharacters(in: .whitespaces)
        return subjects.compactMap { subject in
            // Subject name matches → include all units as-is
            if subject.name.lowercased().contains(q) {
                var s = subject; s.isExpanded = true; return s
            }
            // Filter units
            let matchingUnits: [Unit] = subject.units.compactMap { unit in
                // Unit name matches → include all concepts in that unit
                if unit.name.lowercased().contains(q) {
                    var u = unit; u.isExpanded = true; return u
                }
                // Filter individual concepts
                let matching = unit.concepts.filter { $0.name.lowercased().contains(q) }
                guard !matching.isEmpty else { return nil }
                var u = unit; u.concepts = matching; u.isExpanded = true; return u
            }
            guard !matchingUnits.isEmpty else { return nil }
            var s = subject; s.units = matchingUnits; s.isExpanded = true; return s
        }
    }

    // MARK: - Data loading

    private func loadConcepts() {
        guard !isLoading, subjects.isEmpty else { return }
        isLoading = true
        Task { @MainActor in
            let child = appState.children.first { $0.id == childId }
            let grade = Int(child?.grade ?? "6") ?? 6
            let board = child?.board ?? "CBSE"
            let raw = (try? await FirebaseService.shared.getConcepts(grade: grade, board: board)) ?? []
            subjects = buildSubjectTree(from: raw)
            isLoading = false
        }
    }

    private func buildSubjectTree(from rawConcepts: [StudyGoalsConcept]) -> [Subject] {
        let conceptOrderById: [String: Int] = Dictionary(
            uniqueKeysWithValues: rawConcepts.map { ($0.id, $0.order) }
        )

        var subjectEmojiMap: [String: String] = [:]
        var subjectColorMap: [String: String] = [:]
        var unitOrderMap: [String: Int] = [:]
        var unitNameMap: [String: String] = [:]
        var unitToSubjectMap: [String: String] = [:]
        var unitConcepts: [String: [StudyGoalsConcept]] = [:]

        for c in rawConcepts {
            subjectEmojiMap[c.subject] = c.subjectIcon
            subjectColorMap[c.subject] = c.subjectColor
            unitOrderMap[c.unitId] = c.unitOrder
            unitNameMap[c.unitId] = c.unit
            unitToSubjectMap[c.unitId] = c.subject
            unitConcepts[c.unitId, default: []].append(c)
        }

        let subjectOrder = ["Mathematics", "Science", "English", "Social Science", "Hindi", "Sanskrit"]
        let sortedSubjects = subjectEmojiMap.keys.sorted {
            let ai = subjectOrder.firstIndex(of: $0) ?? 99
            let bi = subjectOrder.firstIndex(of: $1) ?? 99
            return ai < bi
        }

        return sortedSubjects.enumerated().map { (index, subjectName) in
            let emoji = subjectEmojiMap[subjectName] ?? "📚"
            let color = subjectColorMap[subjectName] ?? "#3B7BFF"
            let subjectUnitIds = unitToSubjectMap.filter { $0.value == subjectName }.keys
            let sortedUnitIds = subjectUnitIds.sorted { unitOrderMap[$0] ?? 0 < unitOrderMap[$1] ?? 0 }
            let units: [Unit] = sortedUnitIds.map { unitId in
                let raw = unitConcepts[unitId] ?? []
                let sorted = raw.sorted { (a: StudyGoalsConcept, b: StudyGoalsConcept) -> Bool in
                    (conceptOrderById[a.id] ?? 0) < (conceptOrderById[b.id] ?? 0)
                }
                let concepts = sorted.map {
                    Concept(id: $0.id, name: $0.name, isCompleted: false,
                            unitId: unitId, unitName: unitNameMap[unitId] ?? unitId,
                            subject: subjectName, subjectColor: color)
                }
                return Unit(id: unitId, name: unitNameMap[unitId] ?? unitId, concepts: concepts)
            }
            // Match Android: first subject is expanded by default
            return Subject(id: subjectName, name: subjectName, emoji: emoji, color: color,
                           units: units, isExpanded: index == 0)
        }
    }
}

// ── Subject row ────────────────────────────────────────────────────────────────

private struct CLSubjectRow: View {
    let subject: Subject
    var isExpanded: Bool = false
    let onToggle: () -> Void

    var body: some View {
        Button(action: onToggle) {
            HStack(spacing: 12) {
                // Colored emoji box matching Android
                ZStack {
                    RoundedRectangle(cornerRadius: 8)
                        .fill(colorFromHexString(subject.color).opacity(0.15))
                        .frame(width: 36, height: 36)
                    Text(subject.emoji)
                        .font(.system(size: 18))
                }

                Text(subject.name)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(Color.dobiTextPrimary)
                    .frame(maxWidth: .infinity, alignment: .leading)

                Text("\(subject.units.count) Units")
                    .font(.system(size: 12))
                    .foregroundStyle(Color.dobiTextSecondary)

                Image(systemName: isExpanded ? "chevron.up" : "chevron.down")
                    .font(.system(size: 14, weight: .medium))
                    .foregroundStyle(Color.dobiTextSecondary)
                    .frame(width: 20, height: 20)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .frame(maxWidth: .infinity)
            .background(Color.white)
        }
        .buttonStyle(.plain)

        // Bottom divider
        Rectangle()
            .fill(Color(hex: 0xF1F5F9))
            .frame(height: 1)
    }
}

// ── Unit row ───────────────────────────────────────────────────────────────────

private struct CLUnitRow: View {
    let unit: Unit
    var isPractise: Bool = false
    let onToggle: () -> Void

    var body: some View {
        Button(action: onToggle) {
            HStack(spacing: 0) {
                // Left indent
                Spacer().frame(width: 24)

                // Vertical accent bar
                RoundedRectangle(cornerRadius: 2)
                    .fill(Color.dobiPrimary.opacity(0.4))
                    .frame(width: 3, height: 32)

                Spacer().frame(width: 12)

                Text(unit.name)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(Color.dobiTextPrimary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .multilineTextAlignment(.leading)

                Text("\(unit.concepts.count) Concepts")
                    .font(.system(size: 12))
                    .foregroundStyle(Color.dobiTextSecondary)

                Spacer().frame(width: 8)

                Image(systemName: isPractise ? "chevron.right" : (unit.isExpanded ? "chevron.up" : "chevron.down"))
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(Color.dobiTextSecondary)
                    .frame(width: 18, height: 18)

                Spacer().frame(width: 16)
            }
            .padding(.vertical, 12)
            .frame(maxWidth: .infinity)
            .background(Color(hex: 0xF8FAFF))
        }
        .buttonStyle(.plain)

        // Bottom divider with left indent
        HStack {
            Spacer().frame(width: 40)
            Rectangle()
                .fill(Color(hex: 0xF1F5F9))
                .frame(height: 1)
        }
        .background(Color(hex: 0xF8FAFF))
    }
}

// ── Concept row ────────────────────────────────────────────────────────────────

private struct CLConceptRow: View {
    let concept: Concept
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 0) {
                // Deep left indent matching Android's 52dp
                Spacer().frame(width: 52)

                Text(concept.name)
                    .font(.system(size: 14))
                    .foregroundStyle(Color.dobiTextPrimary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .multilineTextAlignment(.leading)

                Image(systemName: "chevron.right")
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(Color.dobiTextSecondary)
                    .frame(width: 18, height: 18)

                Spacer().frame(width: 16)
            }
            .padding(.vertical, 12)
            .frame(maxWidth: .infinity)
            .background(Color(hex: 0xF8FAFF))
        }
        .buttonStyle(.plain)

        // Bottom divider with deep left indent
        HStack {
            Spacer().frame(width: 52)
            Rectangle()
                .fill(Color(hex: 0xF1F5F9))
                .frame(height: 1)
        }
        .background(Color(hex: 0xF8FAFF))
    }
}

// ── Hex string → Color ─────────────────────────────────────────────────────────

private func colorFromHexString(_ hex: String) -> Color {
    let clean = hex.hasPrefix("#") ? String(hex.dropFirst()) : hex
    guard let value = UInt32(clean, radix: 16) else { return Color(hex: 0x3B7BFF) }
    return Color(hex: value)
}
