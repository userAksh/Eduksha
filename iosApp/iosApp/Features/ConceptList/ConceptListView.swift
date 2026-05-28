import SwiftUI

// ── Models ─────────────────────────────────────────────────────────────────────

struct Subject: Identifiable {
    let id: String
    let name: String
    let emoji: String
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
}

enum ConceptListMode { case `default`, practiseLesson, testYourself }

// ── Main view ──────────────────────────────────────────────────────────────────

struct ConceptListView: View {
    var mode: ConceptListMode = .default
    var onBack: () -> Void = {}
    var onConceptTap: (Concept) -> Void = { _ in }

    @State private var searchQuery = ""
    @State private var subjects: [Subject] = sampleSubjects

    private var title: String {
        switch mode {
        case .practiseLesson: return "Pick a Lesson"
        case .testYourself: return "Test Yourself"
        default: return "Concepts"
        }
    }

    private var filtered: [Subject] {
        guard !searchQuery.isEmpty else { return subjects }
        return subjects.compactMap { subject in
            let matchingUnits = subject.units.compactMap { unit -> Unit? in
                let matchingConcepts = unit.concepts.filter {
                    $0.name.localizedCaseInsensitiveContains(searchQuery)
                }
                if unit.name.localizedCaseInsensitiveContains(searchQuery) || !matchingConcepts.isEmpty {
                    var u = unit
                    u.concepts = matchingConcepts.isEmpty ? unit.concepts : matchingConcepts
                    return u
                }
                return nil
            }
            if subject.name.localizedCaseInsensitiveContains(searchQuery) || !matchingUnits.isEmpty {
                var s = subject
                s.units = matchingUnits.isEmpty ? subject.units : matchingUnits
                return s
            }
            return nil
        }
    }

    var body: some View {
        ZStack(alignment: .top) {
            Color.dobiPageBg.ignoresSafeArea()

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
                .padding(.horizontal, 20)
                .padding(.top, 56)
                .padding(.bottom, 12)

                // Search bar
                HStack(spacing: 10) {
                    Image(systemName: "magnifyingglass")
                        .font(.system(size: 15))
                        .foregroundStyle(Color.dobiTextTertiary)
                    TextField("Search subjects, topics…", text: $searchQuery)
                        .font(.dobiBody())
                        .foregroundStyle(Color.dobiTextPrimary)
                        .autocorrectionDisabled()
                    if !searchQuery.isEmpty {
                        Button { searchQuery = "" } label: {
                            Image(systemName: "xmark.circle.fill")
                                .foregroundStyle(Color.dobiTextTertiary)
                        }
                    }
                }
                .padding(.horizontal, 14)
                .padding(.vertical, 10)
                .background(Color.white)
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .overlay(RoundedRectangle(cornerRadius: 12).strokeBorder(Color.dobiBorder, lineWidth: 1))
                .padding(.horizontal, 20)
                .padding(.bottom, 12)

                if filtered.isEmpty {
                    VStack(spacing: 12) {
                        EmojiAvatar(emoji: "🔍", size: 64, background: .dobiPastelSkyBlue)
                        Text("No results for \"\(searchQuery)\"")
                            .font(.dobiBody())
                            .foregroundStyle(Color.dobiTextSecondary)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.top, 60)
                } else {
                    ScrollView {
                        LazyVStack(spacing: 12, pinnedViews: []) {
                            ForEach(filtered.indices, id: \.self) { si in
                                SubjectSection(
                                    subject: subjects[si],
                                    onToggle: { subjects[si].isExpanded.toggle() },
                                    onUnitToggle: { ui in subjects[si].units[ui].isExpanded.toggle() },
                                    onConceptTap: onConceptTap
                                )
                            }
                            Spacer().frame(height: 40)
                        }
                        .padding(.horizontal, 20)
                    }
                }
            }
        }
        .navigationBarHidden(true)
    }
}

// ── Subject section ────────────────────────────────────────────────────────────

private let subjectEmojis: [String: String] = [
    "Science": "🔬", "Mathematics": "📐", "English": "📖",
    "Social Science": "🌍", "Hindi": "🇮🇳", "Sanskrit": "📜"
]

private struct SubjectSection: View {
    let subject: Subject
    var onToggle: () -> Void
    var onUnitToggle: (Int) -> Void
    var onConceptTap: (Concept) -> Void

    var body: some View {
        VStack(spacing: 0) {
            // Subject header
            Button(action: onToggle) {
                HStack(spacing: 12) {
                    Text(subject.emoji)
                        .font(.system(size: 22))
                        .frame(width: 40, height: 40)
                        .background(Color.dobiPrimaryContainer)
                        .clipShape(RoundedRectangle(cornerRadius: 10))
                    Text(subject.name)
                        .font(.dobiBodyBold())
                        .foregroundStyle(Color.dobiTextPrimary)
                    Spacer()
                    Image(systemName: subject.isExpanded ? "chevron.up" : "chevron.down")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(Color.dobiTextTertiary)
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 14)
                .background(Color.white)
                .clipShape(RoundedRectangle(cornerRadius: 16))
                .shadow(color: Color(hex: 0x2563EB, alpha: 0.06), radius: 8, x: 0, y: 2)
            }
            .buttonStyle(.plain)

            if subject.isExpanded {
                VStack(spacing: 8) {
                    ForEach(subject.units.indices, id: \.self) { ui in
                        UnitSection(
                            unit: subject.units[ui],
                            onToggle: { onUnitToggle(ui) },
                            onConceptTap: onConceptTap
                        )
                    }
                }
                .padding(.top, 8)
                .padding(.leading, 16)
            }
        }
    }
}

private struct UnitSection: View {
    let unit: Unit
    var onToggle: () -> Void
    var onConceptTap: (Concept) -> Void

    var body: some View {
        VStack(spacing: 0) {
            Button(action: onToggle) {
                HStack(spacing: 10) {
                    Image(systemName: "book.closed.fill")
                        .font(.system(size: 13))
                        .foregroundStyle(Color.dobiPrimary)
                    Text(unit.name)
                        .font(.dobiCaptionMedium())
                        .foregroundStyle(Color.dobiTextPrimary)
                    Spacer()
                    Image(systemName: unit.isExpanded ? "chevron.up" : "chevron.down")
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundStyle(Color.dobiTextTertiary)
                }
                .padding(.horizontal, 14)
                .padding(.vertical, 10)
                .background(Color.dobiSurfaceAlt)
                .clipShape(RoundedRectangle(cornerRadius: 12))
            }
            .buttonStyle(.plain)

            if unit.isExpanded {
                VStack(spacing: 6) {
                    ForEach(unit.concepts) { concept in
                        ConceptRow(concept: concept, onTap: { onConceptTap(concept) })
                    }
                }
                .padding(.top, 6)
                .padding(.leading, 12)
            }
        }
    }
}

private struct ConceptRow: View {
    let concept: Concept
    var onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 10) {
                ZStack {
                    Circle()
                        .strokeBorder(concept.isCompleted ? Color.dobiSuccess : Color.dobiBorder, lineWidth: 1.5)
                        .frame(width: 22, height: 22)
                    if concept.isCompleted {
                        Image(systemName: "checkmark")
                            .font(.system(size: 10, weight: .bold))
                            .foregroundStyle(Color.dobiSuccess)
                    }
                }
                Text(concept.name)
                    .font(.dobiBody())
                    .foregroundStyle(concept.isCompleted ? Color.dobiTextSecondary : Color.dobiTextPrimary)
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.system(size: 11, weight: .medium))
                    .foregroundStyle(Color.dobiTextTertiary)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 10))
        }
        .buttonStyle(.plain)
    }
}

// ── Sample data ────────────────────────────────────────────────────────────────

private let sampleSubjects: [Subject] = [
    Subject(id: "sci", name: "Science", emoji: "🔬", units: [
        Unit(id: "sci-1", name: "Motion and Measurement", concepts: [
            Concept(id: "c1", name: "Types of motion", isCompleted: true),
            Concept(id: "c2", name: "Measuring length", isCompleted: true),
            Concept(id: "c3", name: "Speed and velocity", isCompleted: false),
        ]),
        Unit(id: "sci-2", name: "Light and Shadows", concepts: [
            Concept(id: "c4", name: "Sources of light", isCompleted: false),
            Concept(id: "c5", name: "Transparent & opaque", isCompleted: false),
        ])
    ]),
    Subject(id: "math", name: "Mathematics", emoji: "📐", units: [
        Unit(id: "math-1", name: "Fractions", concepts: [
            Concept(id: "c6", name: "Types of fractions", isCompleted: true),
            Concept(id: "c7", name: "Adding fractions", isCompleted: false),
        ]),
        Unit(id: "math-2", name: "Algebra Basics", concepts: [
            Concept(id: "c8", name: "Variables and expressions", isCompleted: false),
            Concept(id: "c9", name: "Simple equations", isCompleted: false),
        ])
    ]),
    Subject(id: "eng", name: "English", emoji: "📖", units: [
        Unit(id: "eng-1", name: "Grammar", concepts: [
            Concept(id: "c10", name: "Nouns and pronouns", isCompleted: true),
            Concept(id: "c11", name: "Tenses", isCompleted: false),
        ])
    ]),
]

#Preview {
    ConceptListView()
}
