import SwiftUI

// MARK: - Local models

private struct CTPSubject: Identifiable {
    let id: String
    let name: String
    let color: String
    let icon: String
    init(name: String, color: String, icon: String) {
        self.id = name; self.name = name; self.color = color; self.icon = icon
    }
    var parsedColor: Color {
        let hex = color.trimmingCharacters(in: .init(charactersIn: "#"))
        guard !hex.isEmpty, let val = UInt32(hex, radix: 16) else { return Color.dobiPrimary }
        return Color(hex: val)
    }
}

private struct CTPUnit: Identifiable {
    let id: String
    let unitId: String
    let unitName: String
    let concepts: [CTPConcept]
}

private struct CTPConcept: Identifiable {
    let id: String
    let name: String
}

struct CTPSelection: Identifiable {
    let id: String
    let title: String
    let isLesson: Bool
    let subject: String
    let unitId: String
    let unitName: String
    var subjectColor: String = ""
    var subjectIcon: String = ""
}

// MARK: - Main view

struct ClassTestPrepView: View {
    let childId: String
    let grade: Int
    var onBack: () -> Void = {}
    var onLaunchMcq: (_ conceptId: String, _ unitName: String, _ subject: String) -> Void = { _, _, _ in }
    var onLaunchWriteAnswers: (
        _ displayName: String, _ subject: String, _ grade: Int, _ scopeText: String
    ) -> Void = { _, _, _, _ in }
    var onLaunchAiPractice: (
        _ conceptId: String, _ unitId: String, _ unitName: String,
        _ conceptName: String, _ subject: String, _ subjectColor: String, _ subjectIcon: String,
        _ teacherHonorific: String, _ teacherName: String, _ testDate: Date, _ scopeText: String
    ) -> Void = { _, _, _, _, _, _, _, _, _, _, _ in }

    private static let maxSelections = 2
    private static let defaultSubjects: [CTPSubject] = [
        CTPSubject(name: "Science",        color: "#22C55E", icon: "🔬"),
        CTPSubject(name: "Mathematics",    color: "#3B82F6", icon: "🔢"),
        CTPSubject(name: "English",        color: "#EC4899", icon: "📖"),
        CTPSubject(name: "Social Science", color: "#F59E0B", icon: "🌍"),
        CTPSubject(name: "Hindi",          color: "#EF4444", icon: "✍️"),
        CTPSubject(name: "Sanskrit",       color: "#8B5CF6", icon: "📜")
    ]

    @State private var isLoading = true
    @State private var allConcepts: [StudyGoalsConcept] = []
    @State private var subjects: [CTPSubject] = []
    @State private var selectedSubject: String? = nil
    @State private var units: [CTPUnit] = []
    @State private var selectedItems: [CTPSelection] = []
    @FocusState private var teacherNameFocused: Bool
    @State private var teacherHonorific = ""
    @State private var teacherName = ""
    @State private var testDate = Date()
    @State private var showDatePicker = false
    @State private var showLimitInfo = false
    @State private var limitMessage: String? = nil
    @State private var showModePopup = false

    private var canStart: Bool { !teacherName.trimmingCharacters(in: .whitespaces).isEmpty && !selectedItems.isEmpty }

    private var formattedDate: String {
        let f = DateFormatter()
        f.dateFormat = "d MMM yyyy"
        return f.string(from: testDate)
    }

    var body: some View {
        ZStack {
            VStack(spacing: 0) {
                navBar
                if isLoading {
                    Spacer()
                    ProgressView().tint(Color.dobiPrimary)
                    Spacer()
                } else {
                    scrollContent
                    stickyCTA
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Color.white.ignoresSafeArea())

            if showLimitInfo {
                limitSnackOverlay
            }

            if showModePopup {
                modePopupOverlay
            }
        }
        .animation(.spring(dampingFraction: 0.72), value: showModePopup)
        .animation(.easeInOut(duration: 0.22), value: showLimitInfo)
        .navigationBarHidden(true)
        .sheet(isPresented: $showDatePicker) {
            VStack(spacing: 0) {
                DatePicker("Test Date", selection: $testDate, displayedComponents: .date)
                    .datePickerStyle(.graphical)
                    .padding()
                Button("Done") { showDatePicker = false }
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(Color.dobiPrimary)
                    .padding(.bottom, 20)
            }
            .presentationDetents([.medium])
        }
        .task { await loadConcepts() }
    }

    // MARK: - Nav bar

    private var navBar: some View {
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
            Text("Class Test Prep")
                .font(.dobiTitle3())
                .foregroundStyle(Color.dobiTextPrimary)
            Spacer()
            Color.clear.frame(width: 36, height: 36)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(Color.white)
    }

    // MARK: - Scroll content

    private var scrollContent: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                // Hero
                Text("Let's Prepare 🚀")
                    .font(.system(size: 26, weight: .bold))
                    .foregroundStyle(Color.dobiTextPrimary)
                    .padding(.top, 4)
                Text("Tell us what your test is about and we'll set you up.")
                    .font(.system(size: 14))
                    .foregroundStyle(Color.dobiTextSecondary)
                    .lineSpacing(4)
                    .padding(.top, 6)

                // Subject
                fieldLabel("Subject").padding(.top, 22)
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 10) {
                        ForEach(subjects) { subj in
                            subjectChip(subj)
                        }
                    }
                    .padding(.horizontal, 1)
                }
                .padding(.top, 10)

                // Teacher's Name
                fieldLabel("Teacher's Name").padding(.top, 22)
                HStack(spacing: 8) {
                    ForEach(["Sir", "Madam"], id: \.self) { h in honorificChip(h) }
                }
                .padding(.top, 10)
                TextField(text: $teacherName, prompt: .dobiPlaceholder("e.g. Sharma")) {}
                    .font(.system(size: 15))
                    .foregroundStyle(Color.dobiTextPrimary)
                    .focused($teacherNameFocused)
                    .padding(14)
                    .background(Color.white)
                    .clipShape(RoundedRectangle(cornerRadius: 14))
                    .overlay(RoundedRectangle(cornerRadius: 14).stroke(
                        teacherNameFocused ? Color.dobiPrimary : Color(hex: 0xE2E8F0), lineWidth: 1
                    ))
                    .padding(.top, 10)
                    .onChange(of: teacherName) { val in
                        teacherName = String(val.unicodeScalars.filter { scalar in
                            CharacterSet.letters.union(.whitespaces).contains(scalar)
                                || scalar == Unicode.Scalar(".")
                        })
                    }

                // Test Date
                fieldLabel("Test Date").padding(.top, 22)
                dateRow.padding(.top, 10)

                // What's on test header
                HStack {
                    fieldLabel("What's on the test?")
                    Spacer()
                    Text("\(selectedItems.count)/\(Self.maxSelections) picked")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(Color.dobiPrimary)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 4)
                        .background(Color.dobiPrimaryContainer)
                        .clipShape(Capsule())
                }
                .padding(.top, 28)
                Text("Pick up to 2 lessons or concepts you want to revise.")
                    .font(.system(size: 12))
                    .foregroundStyle(Color.dobiTextSecondary)
                    .padding(.top, 4)

                // Unit/concept list
                if units.isEmpty {
                    emptyUnitState.padding(.top, 12)
                } else {
                    VStack(spacing: 10) {
                        ForEach(units) { unit in
                            unitGroupCard(unit)
                        }
                    }
                    .padding(.top, 12)
                }

                Spacer().frame(height: 24)
            }
            .padding(.horizontal, 20)
        }
    }

    // MARK: - Sticky CTA

    private var stickyCTA: some View {
        VStack(spacing: 0) {
            Divider()
            Button(action: startPractise) {
                Text("Start Practice")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 54)
                    .background(canStart ? Color.dobiPrimary : Color(hex: 0xBFD7F7))
                    .clipShape(RoundedRectangle(cornerRadius: 16))
            }
            .disabled(!canStart)
            .padding(.horizontal, 20)
            .padding(.vertical, 12)
        }
        .background(Color.white)
    }

    // MARK: - Overlays

    private var limitSnackOverlay: some View {
        VStack {
            Spacer()
            HStack(spacing: 10) {
                Text("⚠️").font(.system(size: 16))
                Text(limitMessage ?? "You can pick at most 2 items. Deselect one to add another.")
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(Color.dobiTextPrimary)
                    .lineSpacing(4)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(Color(hex: 0xFEF3C7))
            .clipShape(RoundedRectangle(cornerRadius: 14))
            .overlay(RoundedRectangle(cornerRadius: 14).stroke(Color(hex: 0xF59E0B), lineWidth: 1))
            .padding(.horizontal, 24)
            .padding(.bottom, 90)
            .transition(.opacity.combined(with: .scale(scale: 0.9)))
            .onAppear {
                Task {
                    try? await Task.sleep(nanoseconds: 2_800_000_000)
                    withAnimation { showLimitInfo = false; limitMessage = nil }
                }
            }
        }
    }

    private var modePopupOverlay: some View {
        ZStack {
            Color.black.opacity(0.45)
                .ignoresSafeArea()
                .onTapGesture { withAnimation(.spring(dampingFraction: 0.72)) { showModePopup = false } }

            modePopupCard
                .padding(.horizontal, 16)
                .transition(
                    .scale(scale: 0.45, anchor: UnitPoint(x: 0.5, y: 0.78))
                    .combined(with: .opacity)
                )
        }
    }

    private var modePopupCard: some View {
        VStack(spacing: 0) {
            HStack(spacing: 12) {
                ZStack {
                    RoundedRectangle(cornerRadius: 14)
                        .fill(Color.dobiPrimaryContainer)
                        .frame(width: 44, height: 44)
                    Text("🎯").font(.system(size: 22))
                }
                VStack(alignment: .leading, spacing: 2) {
                    Text("How would you like to practise?")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundStyle(Color.dobiTextPrimary)
                    Text("\(selectedItems.count) item\(selectedItems.count == 1 ? "" : "s") ready")
                        .font(.system(size: 12))
                        .foregroundStyle(Color.dobiTextSecondary)
                }
                Spacer()
            }
            .padding(.horizontal, 20)
            .padding(.top, 20)

            Spacer().frame(height: 16)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
                    modeCard(emoji: "📝", title: "Quick MCQs",     subtitle: "Multiple-choice questions", accent: Color.dobiPrimary,     action: handleMcq)
                    modeCard(emoji: "✍️", title: "Write Answers",  subtitle: "Short & long answers",      accent: Color(hex: 0x7C3AED),   action: handleWriteAnswers)
                    modeCard(emoji: "✨", title: "AI Practice",    subtitle: "Fresh questions by Dobi",   accent: Color(hex: 0xD946EF),   action: handleAiPractice)
                }
                .padding(.horizontal, 20)
            }

            Spacer().frame(height: 12)

            Text("Swipe to see all 3 options →")
                .font(.system(size: 11))
                .foregroundStyle(Color.dobiTextSecondary)

            Spacer().frame(height: 20)
        }
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 28))
        .shadow(color: .black.opacity(0.12), radius: 24, x: 0, y: 8)
    }

    // MARK: - Reusable sub-views

    @ViewBuilder
    private func fieldLabel(_ text: String) -> some View {
        Text(text)
            .font(.system(size: 14, weight: .semibold))
            .foregroundStyle(Color.dobiTextPrimary)
    }

    @ViewBuilder
    private func subjectChip(_ subj: CTPSubject) -> some View {
        let accent = subj.parsedColor
        let selected = selectedSubject == subj.name
        HStack(spacing: 8) {
            Text(subj.icon).font(.system(size: 16))
            Text(subj.name)
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(selected ? Color.white : Color.dobiTextPrimary)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 10)
        .background(selected ? accent : Color.white)
        .clipShape(Capsule())
        .overlay(Capsule().stroke(selected ? accent : Color(hex: 0xE5EBF5), lineWidth: 1.5))
        .onTapGesture { onSubjectSelected(subj.name) }
        .animation(.easeInOut(duration: 0.15), value: selected)
    }

    @ViewBuilder
    private func honorificChip(_ label: String) -> some View {
        let selected = teacherHonorific == label
        HStack(spacing: 6) {
            if selected {
                Image(systemName: "checkmark")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(.white)
            }
            Text(label)
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(selected ? Color.white : Color.dobiTextSecondary)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 8)
        .background(selected ? Color.dobiPrimary : Color.white)
        .clipShape(Capsule())
        .overlay(Capsule().stroke(selected ? Color.dobiPrimary : Color(hex: 0xE2E8F0), lineWidth: 1.5))
        .onTapGesture { teacherHonorific = selected ? "" : label }
        .animation(.easeInOut(duration: 0.15), value: selected)
    }

    private var dateRow: some View {
        HStack {
            Image(systemName: "calendar")
                .font(.system(size: 18))
                .foregroundStyle(Color.dobiPrimary)
            Text(formattedDate)
                .font(.system(size: 15, weight: .medium))
                .foregroundStyle(Color.dobiTextPrimary)
            Spacer()
            Text("Change")
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(Color.dobiPrimary)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 16)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 14))
        .overlay(RoundedRectangle(cornerRadius: 14).stroke(Color(hex: 0xE2E8F0), lineWidth: 1))
        .onTapGesture { showDatePicker = true }
    }

    private var emptyUnitState: some View {
        HStack {
            Spacer()
            Text("Pick a subject above to see lessons.")
                .font(.system(size: 14))
                .foregroundStyle(Color.dobiTextSecondary)
                .multilineTextAlignment(.center)
            Spacer()
        }
        .padding(20)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 14))
        .overlay(RoundedRectangle(cornerRadius: 14).stroke(Color(hex: 0xE5EBF5), lineWidth: 1))
    }

    @ViewBuilder
    private func unitGroupCard(_ unit: CTPUnit) -> some View {
        let lessonSelected = selectedItems.contains { $0.id == unit.unitId }
        VStack(alignment: .leading, spacing: 0) {
            selectableRow(
                title: unit.unitName,
                subtitle: "Whole lesson — \(unit.concepts.count) concepts",
                selected: lessonSelected,
                isLesson: true,
                inset: false,
                onTap: {
                    onItemToggled(CTPSelection(
                        id: unit.unitId, title: unit.unitName,
                        isLesson: true, subject: selectedSubject ?? "",
                        unitId: unit.unitId, unitName: unit.unitName
                    ))
                }
            )
            if !unit.concepts.isEmpty {
                Spacer().frame(height: 8)
                ForEach(unit.concepts) { concept in
                    selectableRow(
                        title: concept.name,
                        subtitle: lessonSelected ? "Included with the lesson" : nil,
                        selected: lessonSelected || selectedItems.contains { $0.id == concept.id },
                        isLesson: false,
                        inset: true,
                        onTap: {
                            onItemToggled(CTPSelection(
                                id: concept.id, title: concept.name,
                                isLesson: false, subject: selectedSubject ?? "",
                                unitId: unit.unitId, unitName: unit.unitName
                            ))
                        }
                    )
                    Spacer().frame(height: 4)
                }
            }
        }
        .padding(12)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color(hex: 0xE5EBF5), lineWidth: 1))
    }

    @ViewBuilder
    private func selectableRow(
        title: String,
        subtitle: String?,
        selected: Bool,
        isLesson: Bool,
        inset: Bool,
        onTap: @escaping () -> Void
    ) -> some View {
        HStack(spacing: 12) {
            ZStack {
                RoundedRectangle(cornerRadius: 7)
                    .fill(selected ? Color.dobiPrimary : Color.white)
                    .frame(width: 22, height: 22)
                    .overlay(
                        RoundedRectangle(cornerRadius: 7)
                            .stroke(selected ? Color.dobiPrimary : Color(hex: 0xCBD5E1), lineWidth: 1.5)
                    )
                if selected {
                    Image(systemName: "checkmark")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundStyle(.white)
                }
            }
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.system(size: 14, weight: isLesson ? .semibold : .medium))
                    .foregroundStyle(Color.dobiTextPrimary)
                    .lineLimit(2)
                if let sub = subtitle {
                    Text(sub)
                        .font(.system(size: 11))
                        .foregroundStyle(Color.dobiTextSecondary)
                }
            }
            Spacer()
            Text(isLesson ? "Lesson" : "Concept")
                .font(.system(size: 10, weight: .semibold))
                .foregroundStyle(isLesson ? Color.dobiPrimary : Color.dobiTextSecondary)
                .padding(.horizontal, 8)
                .padding(.vertical, 3)
                .background(isLesson ? Color.dobiPrimaryContainer : Color(hex: 0xF1F5F9))
                .clipShape(Capsule())
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .padding(.leading, inset ? 8 : 0)
        .background(selected ? Color.dobiPrimaryContainer : Color(hex: 0xFAFBFD))
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(selected ? Color.dobiPrimary : Color(hex: 0xEEF2F7), lineWidth: 1.5))
        .onTapGesture(perform: onTap)
        .animation(.easeInOut(duration: 0.15), value: selected)
    }

    @ViewBuilder
    private func modeCard(emoji: String, title: String, subtitle: String, accent: Color, action: @escaping () -> Void) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            ZStack {
                RoundedRectangle(cornerRadius: 10)
                    .fill(accent.opacity(0.12))
                    .frame(width: 34, height: 34)
                Text(emoji).font(.system(size: 18))
            }
            Spacer().frame(height: 8)
            Text(title)
                .font(.system(size: 13, weight: .bold))
                .foregroundStyle(Color.dobiTextPrimary)
                .lineLimit(2)
                .lineSpacing(2)
            Spacer().frame(height: 2)
            Text(subtitle)
                .font(.system(size: 10))
                .foregroundStyle(Color.dobiTextSecondary)
                .lineLimit(2)
            Spacer()
            HStack(spacing: 5) {
                Text("Start")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(accent)
                ZStack {
                    Circle().fill(accent).frame(width: 18, height: 18)
                    Image(systemName: "arrow.right")
                        .font(.system(size: 9, weight: .semibold))
                        .foregroundStyle(.white)
                }
            }
        }
        .frame(width: 118, height: 146)
        .padding(12)
        .background(Color(hex: 0xFAFBFD))
        .clipShape(RoundedRectangle(cornerRadius: 18))
        .overlay(RoundedRectangle(cornerRadius: 18).stroke(accent.opacity(0.25), lineWidth: 1.5))
        .onTapGesture(perform: action)
    }

    // MARK: - Logic

    private func loadConcepts() async {
        do {
            let concepts = try await FirebaseService.shared.getConcepts(grade: grade, board: "CBSE")
            allConcepts = concepts
            let merged = mergeWithDefaults(concepts)
            subjects = merged
            selectedSubject = merged.first?.name
            units = buildUnits(for: selectedSubject)
        } catch {}
        isLoading = false
    }

    private func mergeWithDefaults(_ concepts: [StudyGoalsConcept]) -> [CTPSubject] {
        var liveMap: [String: CTPSubject] = [:]
        for c in concepts where liveMap[c.subject] == nil {
            liveMap[c.subject] = CTPSubject(name: c.subject, color: c.subjectColor, icon: c.subjectIcon)
        }
        var result: [CTPSubject] = Self.defaultSubjects.map { def in liveMap[def.name] ?? def }
        let extras = liveMap.values.filter { l in !Self.defaultSubjects.contains(where: { $0.name == l.name }) }
        result += extras.sorted { $0.name < $1.name }
        return result
    }

    private func buildUnits(for subject: String?) -> [CTPUnit] {
        guard let subject else { return [] }
        let filtered = allConcepts.filter { $0.subject == subject }
        let grouped = Dictionary(grouping: filtered, by: { $0.unitId })
        return grouped.values
            .sorted { ($0.first?.unitOrder ?? 0) < ($1.first?.unitOrder ?? 0) }
            .map { cs in
                CTPUnit(
                    id: cs[0].unitId,
                    unitId: cs[0].unitId,
                    unitName: cs[0].unit,
                    concepts: cs.sorted { $0.order < $1.order }.map { CTPConcept(id: $0.id, name: $0.name) }
                )
            }
    }

    private func onSubjectSelected(_ subject: String) {
        guard selectedSubject != subject else { return }
        selectedSubject = subject
        units = buildUnits(for: subject)
        selectedItems = []
    }

    private func onItemToggled(_ item: CTPSelection) {
        if let idx = selectedItems.firstIndex(where: { $0.id == item.id }) {
            selectedItems.remove(at: idx)
            showLimitInfo = false; limitMessage = nil
            return
        }

        if !item.isLesson,
           let parent = selectedItems.first(where: { $0.isLesson && $0.unitId == item.unitId }),
           selectedItems.count > 1 {
            limitMessage = "\"\(parent.title)\" is already on your list — it covers this concept. Deselect another item first to focus on just this concept."
            withAnimation { showLimitInfo = true }
            return
        }

        if let anchor = selectedItems.first(where: { !$0.isLesson }), anchor.unitId != item.unitId {
            limitMessage = item.isLesson
                ? "You're already revising \"\(anchor.title)\" from \"\(anchor.unitName)\". Deselect it first to pick a different lesson."
                : "Concepts have to come from the same lesson. Deselect \"\(anchor.title)\" or pick its whole lesson instead."
            withAnimation { showLimitInfo = true }
            return
        }

        var resolved = selectedItems.filter { other in
            if item.isLesson && !other.isLesson && other.unitId == item.unitId { return false }
            if !item.isLesson && other.isLesson && other.unitId == item.unitId { return false }
            return true
        }

        if resolved.count >= Self.maxSelections {
            withAnimation { showLimitInfo = true; limitMessage = nil }
            return
        }

        resolved.append(item)
        selectedItems = resolved
        showLimitInfo = false; limitMessage = nil
    }

    private func startPractise() {
        guard canStart else { return }
        withAnimation(.spring(dampingFraction: 0.72)) { showModePopup = true }
    }

    private func handleMcq() {
        showModePopup = false
        guard let first = selectedItems.first else { return }
        let conceptId = first.isLesson
            ? (allConcepts.first { $0.unitId == first.unitId }?.id ?? first.id)
            : first.id
        onLaunchMcq(conceptId, first.unitName, first.subject)
    }

    private func handleWriteAnswers() {
        showModePopup = false
        guard !selectedItems.isEmpty else { return }
        let displayName = selectedItems.map { $0.title }.joined(separator: " + ")
        let subject = selectedSubject ?? (selectedItems.first?.subject ?? "")
        let scopeText = buildScopeText(selectedItems)
        onLaunchWriteAnswers(displayName, subject, grade, scopeText)
    }

    private func buildScopeText(_ items: [CTPSelection]) -> String {
        let lessons = items.filter { $0.isLesson }
        let concepts = items.filter { !$0.isLesson }
        var lines: [String] = []
        lessons.forEach { lines.append("Lesson: \($0.title)") }
        if !concepts.isEmpty {
            let parentLesson = concepts[0].unitName
            if !lessons.contains(where: { $0.title == parentLesson }) {
                lines.append("Lesson: \(parentLesson)")
            }
            if concepts.count == 1 {
                lines.append("Concept (focus narrowly on this): \(concepts[0].title)")
            } else {
                lines.append("Concepts (focus narrowly on these): " + concepts.map { $0.title }.joined(separator: ", "))
            }
        }
        return lines.joined(separator: "\n")
    }


    private func handleAiPractice() {
        showModePopup = false
        guard let first = selectedItems.first else { return }
        let conceptData = allConcepts.first { $0.id == first.id }
        let conceptId = first.isLesson
            ? (allConcepts.first { $0.unitId == first.unitId }?.id ?? first.id)
            : first.id
        let subjectColor = conceptData?.subjectColor ?? first.subjectColor
        let subjectIcon = conceptData?.subjectIcon ?? first.subjectIcon
        let displayName = selectedItems.map { $0.title }.joined(separator: " + ")
        let scopeText = buildScopeText(selectedItems)
        onLaunchAiPractice(
            conceptId, first.unitId, displayName, first.title, first.subject,
            subjectColor, subjectIcon, teacherHonorific, teacherName, testDate, scopeText
        )
    }
}
