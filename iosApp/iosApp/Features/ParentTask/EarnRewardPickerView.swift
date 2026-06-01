import SwiftUI

// Child-side screen: pick a lesson or concept and send a request to the parent.
// Mirrors Android EarnRewardPickerScreen / EarnRewardPickerViewModel exactly.

private let ERBg      = Color(hex: 0xF8FAFF)
private let ERPrimary = Color(hex: 0x2F7DFF)
private let ERTextPri = Color(hex: 0x1A2B4A)
private let ERTextSec = Color(hex: 0x6B7A99)
private let ERBorder  = Color(hex: 0xE5EBF5)

private let kTwelveH: Int64 = 12 * 60 * 60 * 1000

// MARK: - Selection model

private enum ERPSelection: Equatable {
    case lesson(unitId: String, unitName: String)
    case concept(conceptId: String, unitId: String, conceptName: String)

    var unitId: String {
        switch self { case .lesson(let id, _): return id; case .concept(_, let id, _): return id }
    }
}

// MARK: - Group models (mirrors SubjectGroup / UnitGroup)

private struct ERPSubjectGroup: Identifiable {
    var id: String { subject }
    let subject: String
    let subjectColor: String
    let subjectIcon: String
    let units: [ERPUnitGroup]
}

private struct ERPUnitGroup: Identifiable {
    var id: String { unitId }
    let unitId: String
    let unitName: String
    let concepts: [StudyGoalsConcept]
}

// MARK: - View

struct EarnRewardPickerView: View {
    let childId: String
    let grade: Int
    let parentId: String
    var onBack: () -> Void = {}
    var onRequestSent: () -> Void = {}

    @EnvironmentObject private var appState: AppState

    @State private var allConcepts: [StudyGoalsConcept] = []
    @State private var isLoading = true
    @State private var loadError: String? = nil
    @State private var searchQuery = ""
    @State private var expandedSubjects: Set<String> = []
    @State private var selection: ERPSelection? = nil
    @State private var isSending = false
    @State private var sendError: String? = nil

    // Mirrors Android: a REQUESTED task created within 12 h blocks a new request
    private var pendingRequest: ParentTaskItem? {
        let now = Int64(Date().timeIntervalSince1970 * 1000)
        return appState.parentTasks
            .filter { $0.childId == childId && $0.status == "REQUESTED" }
            .filter { now - $0.requestedAtMillis < kTwelveH }
            .max(by: { $0.requestedAtMillis < $1.requestedAtMillis })
    }

    private var canSend: Bool { selection != nil && !isSending }

    // Build groups from allConcepts, filtered by searchQuery
    private var filteredGroups: [ERPSubjectGroup] {
        let q = searchQuery.lowercased().trimmingCharacters(in: .whitespaces)
        var bySubject: [String: [StudyGoalsConcept]] = [:]
        for c in allConcepts {
            if q.isEmpty ||
               c.name.lowercased().contains(q) ||
               c.unit.lowercased().contains(q) ||
               c.subject.lowercased().contains(q) {
                bySubject[c.subject, default: []].append(c)
            }
        }
        return bySubject.keys.sorted().compactMap { subject -> ERPSubjectGroup? in
            let concepts = bySubject[subject] ?? []
            guard !concepts.isEmpty else { return nil }
            let first = concepts[0]
            var byUnit: [String: [StudyGoalsConcept]] = [:]
            for c in concepts { byUnit[c.unitId, default: []].append(c) }
            let units = byUnit.values
                .sorted { ($0.first?.unitOrder ?? 0) < ($1.first?.unitOrder ?? 0) }
                .map { unitConcepts -> ERPUnitGroup in
                    let sorted = unitConcepts.sorted { $0.order < $1.order }
                    return ERPUnitGroup(unitId: sorted[0].unitId, unitName: sorted[0].unit, concepts: sorted)
                }
            return ERPSubjectGroup(subject: subject, subjectColor: first.subjectColor, subjectIcon: first.subjectIcon, units: units)
        }
    }

    var body: some View {
        VStack(spacing: 0) {
            topBar
            if let pending = pendingRequest {
                requestSentBody(pending)
            } else if isLoading {
                Spacer()
                ProgressView().tint(ERPrimary)
                Spacer()
            } else if let err = loadError {
                Spacer()
                Text(err).font(.system(size: 14)).foregroundStyle(ERTextSec).padding()
                Spacer()
            } else {
                pickerBody
            }
        }
        .background(ERBg.ignoresSafeArea())
        .navigationBarHidden(true)
        .task { await loadConcepts() }
    }

    // MARK: Top bar

    private var topBar: some View {
        HStack(spacing: 12) {
            Button(action: onBack) {
                Image(systemName: "arrow.left")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundStyle(ERTextPri)
                    .frame(width: 36, height: 36)
            }
            .buttonStyle(.plain)
            Text("Pick Your Topic")
                .font(.system(size: 17, weight: .bold))
                .foregroundStyle(ERTextPri)
            Spacer()
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(ERBg)
    }

    // MARK: Picker body (subject → unit → concept tree + send button)

    private var pickerBody: some View {
        VStack(spacing: 0) {
            // Helper header
            VStack(alignment: .leading, spacing: 2) {
                Text("Pick one lesson or one concept")
                    .font(.system(size: 15, weight: .bold))
                    .foregroundStyle(ERTextPri)
                Text("Your parents will pick the task type and assign it to you.")
                    .font(.system(size: 12))
                    .foregroundStyle(ERTextSec)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 16)
            .padding(.top, 4)
            .padding(.bottom, 8)

            // Search bar
            HStack(spacing: 8) {
                Image(systemName: "magnifyingglass")
                    .font(.system(size: 16))
                    .foregroundStyle(ERTextSec)
                TextField(text: $searchQuery, prompt: .dobiPlaceholder("Search concepts, subjects…")) {}
                    .font(.system(size: 14))
                    .foregroundStyle(ERTextPri)
                    .autocorrectionDisabled()
                if !searchQuery.isEmpty {
                    Button { searchQuery = "" } label: {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundStyle(ERTextSec)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .padding(.horizontal, 16)
            .padding(.bottom, 8)

            if filteredGroups.isEmpty {
                Spacer()
                Text(searchQuery.isEmpty ? "No concepts found" : "No results for \"\(searchQuery)\"")
                    .font(.system(size: 14))
                    .foregroundStyle(ERTextSec)
                Spacer()
            } else {
                ScrollView(showsIndicators: false) {
                    LazyVStack(spacing: 0, pinnedViews: []) {
                        ForEach(filteredGroups) { group in
                            SubjectRow(
                                group: group,
                                isExpanded: expandedSubjects.contains(group.subject),
                                onToggle: {
                                    if expandedSubjects.contains(group.subject) {
                                        expandedSubjects.remove(group.subject)
                                    } else {
                                        expandedSubjects.insert(group.subject)
                                    }
                                }
                            )
                            if expandedSubjects.contains(group.subject) {
                                ForEach(group.units) { unit in
                                    let lessonSelected: Bool = {
                                        if case .lesson(let uid, _) = selection { return uid == unit.unitId }
                                        return false
                                    }()
                                    UnitRow(
                                        unit: unit,
                                        isSelected: lessonSelected,
                                        onSelect: {
                                            if lessonSelected { selection = nil }
                                            else { selection = .lesson(unitId: unit.unitId, unitName: unit.unitName) }
                                        }
                                    )
                                    ForEach(unit.concepts, id: \.id) { concept in
                                        let conceptSelected: Bool = {
                                            if lessonSelected { return true }
                                            if case .concept(let cid, _, _) = selection { return cid == concept.id }
                                            return false
                                        }()
                                        ConceptRow(
                                            name: concept.name,
                                            isSelected: conceptSelected,
                                            isLockedByLesson: lessonSelected,
                                            onSelect: {
                                                guard !lessonSelected else { return }
                                                if case .concept(let cid, _, _) = selection, cid == concept.id {
                                                    selection = nil
                                                } else {
                                                    selection = .concept(conceptId: concept.id, unitId: unit.unitId, conceptName: concept.name)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        Spacer().frame(height: 120)
                    }
                }
            }

            // Send button pinned at bottom
            VStack(spacing: 0) {
                Divider()
                if let err = sendError {
                    Text(err)
                        .font(.system(size: 12))
                        .foregroundStyle(Color(hex: 0xDC2626))
                        .padding(.horizontal, 16)
                        .padding(.top, 8)
                }
                Button(action: sendRequest) {
                    Group {
                        if isSending {
                            ProgressView().tint(.white)
                        } else {
                            Text("Send Request to Parents")
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundStyle(.white)
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 50)
                    .background(canSend ? ERPrimary : Color(hex: 0xCBD5E1))
                    .clipShape(RoundedRectangle(cornerRadius: 14))
                }
                .buttonStyle(.plain)
                .disabled(!canSend)
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
            }
            .background(Color.white)
        }
    }

    // MARK: Request already sent state

    private func requestSentBody(_ task: ParentTaskItem) -> some View {
        let now = Int64(Date().timeIntervalSince1970 * 1000)
        let remainingMs = max(0, kTwelveH - (now - task.requestedAtMillis))
        let remainingH  = remainingMs / 3_600_000
        let remainingM  = (remainingMs % 3_600_000) / 60_000
        let timeLeft: String = {
            if remainingH > 0 { return "\(remainingH)h \(remainingM)m" }
            if remainingM > 0 { return "\(remainingM)m" }
            return "a moment"
        }()

        return VStack(spacing: 0) {
            Spacer()
            VStack(spacing: 20) {
                ZStack {
                    Circle().fill(Color(hex: 0xDBEAFE)).frame(width: 72, height: 72)
                    Image(systemName: "hourglass")
                        .font(.system(size: 30, weight: .medium))
                        .foregroundStyle(ERPrimary)
                }

                VStack(spacing: 6) {
                    Text("Request Sent to Parent")
                        .font(.system(size: 20, weight: .bold))
                        .foregroundStyle(ERTextPri)
                        .multilineTextAlignment(.center)
                    Text(task.displayName)
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(ERPrimary)
                        .multilineTextAlignment(.center)
                }

                VStack(spacing: 4) {
                    Text("⏳ Waiting for parent")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(ERTextPri)
                    Text("You can send a new request in \(timeLeft), or once your parent responds.")
                        .font(.system(size: 12))
                        .foregroundStyle(ERTextSec)
                        .multilineTextAlignment(.center)
                }
                .padding(16)
                .frame(maxWidth: .infinity)
                .background(Color(hex: 0xF1F5F9))
                .clipShape(RoundedRectangle(cornerRadius: 14))
                .padding(.horizontal, 24)
            }
            Spacer()
        }
    }

    // MARK: Helpers

    private func loadConcepts() async {
        do {
            let result = try await FirebaseService.shared.getConcepts(grade: grade, board: "CBSE")
            allConcepts = result
            // Auto-expand first subject
            if let first = result.first { expandedSubjects.insert(first.subject) }
            isLoading = false
        } catch {
            loadError = error.localizedDescription
            isLoading = false
        }
    }

    private func sendRequest() {
        guard let sel = selection, !isSending else { return }
        sendError = nil
        isSending = true

        let isUnit: Bool
        let unitId: String
        let unitName: String
        let conceptId: String
        let conceptName: String

        switch sel {
        case .lesson(let uid, let uname):
            isUnit = true; unitId = uid; unitName = uname; conceptId = ""; conceptName = ""
        case .concept(let cid, let uid, let cname):
            isUnit = false; unitId = uid; conceptId = cid; conceptName = cname
            unitName = allConcepts.first { $0.unitId == uid }?.unit ?? ""
        }

        let first = allConcepts.first { $0.unitId == unitId }
        let subject      = first?.subject ?? ""
        let subjectColor = first?.subjectColor ?? ""
        let subjectIcon  = first?.subjectIcon ?? ""

        Task {
            do {
                try await FirebaseService.shared.requestFromChild(
                    parentId: parentId, childId: childId,
                    unitId: unitId, unitName: unitName,
                    conceptId: conceptId, conceptName: conceptName,
                    isUnitScope: isUnit,
                    subject: subject, subjectColor: subjectColor, subjectIcon: subjectIcon
                )
                await MainActor.run { onRequestSent() }
            } catch {
                await MainActor.run {
                    sendError = "Couldn't send request. Try again."
                    isSending = false
                }
            }
        }
    }
}

// MARK: - Row components

private struct SubjectRow: View {
    let group: ERPSubjectGroup
    let isExpanded: Bool
    let onToggle: () -> Void

    private var accent: Color {
        let hex = group.subjectColor.trimmingCharacters(in: .init(charactersIn: "#"))
        guard let val = UInt32(hex, radix: 16) else { return Color(hex: 0x3B7BFF) }
        return Color(hex: val)
    }

    var body: some View {
        Button(action: onToggle) {
            HStack(spacing: 12) {
                ZStack {
                    RoundedRectangle(cornerRadius: 8)
                        .fill(accent.opacity(0.15))
                        .frame(width: 36, height: 36)
                    Text(group.subjectIcon).font(.system(size: 18))
                }
                Text(group.subject)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(Color(hex: 0x1A2B4A))
                Spacer()
                Text("\(group.units.count) Units")
                    .font(.system(size: 12))
                    .foregroundStyle(Color(hex: 0x6B7A99))
                Image(systemName: isExpanded ? "chevron.up" : "chevron.down")
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(Color(hex: 0x6B7A99))
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .background(Color.white)
        }
        .buttonStyle(.plain)
        Divider().padding(.leading, 16)
    }
}

private struct UnitRow: View {
    let unit: ERPUnitGroup
    let isSelected: Bool
    let onSelect: () -> Void

    var body: some View {
        Button(action: onSelect) {
            HStack(spacing: 12) {
                SelectionDot(isSelected: isSelected, small: false)
                VStack(alignment: .leading, spacing: 2) {
                    Text(unit.unitName)
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(Color(hex: 0x1A2B4A))
                    Text("\(unit.concepts.count) concepts\(isSelected ? " — all included" : "")")
                        .font(.system(size: 12))
                        .foregroundStyle(isSelected ? Color(hex: 0x2F7DFF) : Color(hex: 0x6B7A99))
                }
                Spacer()
            }
            .padding(.leading, 20).padding(.trailing, 16)
            .padding(.vertical, 12)
            .background(Color(hex: 0xF8FAFF))
        }
        .buttonStyle(.plain)
        Divider().padding(.leading, 40)
    }
}

private struct ConceptRow: View {
    let name: String
    let isSelected: Bool
    let isLockedByLesson: Bool
    let onSelect: () -> Void

    var body: some View {
        Button(action: onSelect) {
            HStack(spacing: 12) {
                SelectionDot(isSelected: isSelected, small: true)
                    .opacity(isLockedByLesson ? 0.6 : 1)
                Text(name)
                    .font(.system(size: 14))
                    .foregroundStyle(Color(hex: 0x1A2B4A))
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(.leading, 48).padding(.trailing, 16)
            .padding(.vertical, 12)
            .background(Color(hex: 0xF8FAFF))
        }
        .buttonStyle(.plain)
        Divider().padding(.leading, 60)
    }
}

private struct SelectionDot: View {
    let isSelected: Bool
    let small: Bool

    var body: some View {
        let outer: CGFloat = small ? 18 : 22
        let inner: CGFloat = small ? 10 : 12
        ZStack {
            Circle()
                .fill(isSelected ? Color(hex: 0x2F7DFF) : Color.white)
                .frame(width: outer, height: outer)
            if !isSelected {
                Circle()
                    .strokeBorder(Color(hex: 0xCBD5E1), lineWidth: 1.5)
                    .frame(width: outer, height: outer)
            }
            if isSelected {
                Image(systemName: "checkmark")
                    .font(.system(size: inner - 4, weight: .bold))
                    .foregroundStyle(.white)
            }
        }
    }
}
