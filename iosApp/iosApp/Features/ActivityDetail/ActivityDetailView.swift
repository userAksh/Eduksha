import SwiftUI

// ActivityCategory, ActivitySource, ActivityLogItem are in ActivityModels.swift

enum ADTimeFilter: String, CaseIterable {
    case today = "Today"
    case week  = "1 Week"
    case month = "1 Month"

    var label: String { rawValue }

    var cutoffMillis: Int64 {
        let now = Int64(Date().timeIntervalSince1970 * 1000)
        switch self {
        case .today:
            let start = Calendar.current.startOfDay(for: Date())
            return Int64(start.timeIntervalSince1970 * 1000)
        case .week:  return now - 7  * 24 * 60 * 60 * 1000
        case .month: return now - 30 * 24 * 60 * 60 * 1000
        }
    }

    var rangeLabel: String {
        switch self {
        case .today: return "today"
        case .week:  return "in the last 7 days"
        case .month: return "in the last 30 days"
        }
    }
}

// MARK: - ViewModel

@MainActor
final class ActivityDetailViewModel: ObservableObject {
    let categoryKey: String
    let childId: String
    let childName: String

    @Published var isLoading = true
    @Published var filter: ADTimeFilter = .week
    @Published var photoSheetUrls: [String] = []
    @Published var quizSheet: ADQuizResult? = nil
    @Published var quizSheetLoading = false
    @Published var subjectiveSheet: ADSubjectiveResult? = nil
    @Published var subjectiveSheetLoading = false
    @Published private var allItems: [ActivityLogItem] = []

    var filteredItems: [ActivityLogItem] {
        let category = ActivityCategory.from(key: categoryKey) ?? .schoolWork
        let cutoff = filter.cutoffMillis
        return allItems.filter { $0.category == category && $0.timestamp >= cutoff }
    }

    private let firebase = FirebaseService.shared

    init(categoryKey: String, childId: String, childName: String) {
        self.categoryKey = categoryKey
        self.childId = childId
        self.childName = childName
        Task { await load() }
    }

    func load() async {
        isLoading = true
        allItems = await firebase.fetchActivityItems(childId: childId)
        isLoading = false
    }

    func handleTap(_ item: ActivityLogItem) {
        switch item.detail {
        case .upload(_, let urls) where !urls.isEmpty:
            photoSheetUrls = urls
        case .homework(_, let urls) where !urls.isEmpty:
            photoSheetUrls = urls
        case .quizMcq(let unitId, let conceptId):
            Task { await loadQuizResult(unitId: unitId, conceptId: conceptId) }
        case .quizSubjective(let resultId):
            Task { await loadSubjectiveResult(resultId: resultId) }
        default:
            break
        }
    }

    private func loadQuizResult(unitId: String, conceptId: String) async {
        quizSheetLoading = true
        quizSheet = await FirebaseService.shared.fetchQuizResult(childId: childId, unitId: unitId, conceptId: conceptId)
        quizSheetLoading = false
    }

    private func loadSubjectiveResult(resultId: String) async {
        subjectiveSheetLoading = true
        subjectiveSheet = await FirebaseService.shared.fetchSubjectiveResult(resultId: resultId)
        subjectiveSheetLoading = false
    }
}

// MARK: - Root view

struct ActivityDetailView: View {
    let categoryKey: String
    let childId: String
    let childName: String
    var onBack: () -> Void = {}

    @StateObject private var vm: ActivityDetailViewModel

    private static let screenBg = Color(hex: 0xFDF6F0)

    init(categoryKey: String, childId: String, childName: String, onBack: @escaping () -> Void = {}) {
        self.categoryKey = categoryKey
        self.childId = childId
        self.childName = childName
        self.onBack = onBack
        _vm = StateObject(wrappedValue: ActivityDetailViewModel(
            categoryKey: categoryKey, childId: childId, childName: childName
        ))
    }

    private var category: ActivityCategory {
        ActivityCategory.from(key: categoryKey) ?? .schoolWork
    }

    var body: some View {
        VStack(spacing: 0) {
            ADTopBar(
                title: category.title,
                childName: childName,
                icon: category.icon,
                iconTint: category.tint,
                iconTintBg: category.tintBg,
                onBack: onBack
            )

            ZStack {
                Color.white

                if vm.isLoading {
                    ProgressView()
                        .progressViewStyle(.circular)
                        .tint(Color.dobiPrimary)
                } else {
                    ScrollView {
                        LazyVStack(spacing: 0) {
                            ADFilterChipRow(selected: vm.filter) { vm.filter = $0 }
                                .padding(.horizontal, 16)
                                .padding(.top, 16)

                            Spacer().frame(height: 14)

                            ADAttemptedSummary(count: vm.filteredItems.count, filter: vm.filter)
                                .padding(.horizontal, 16)

                            Spacer().frame(height: 12)

                            if vm.filteredItems.isEmpty {
                                ADEmptyState(filter: vm.filter, category: category)
                                    .padding(.horizontal, 16)
                                    .padding(.vertical, 24)
                            } else {
                                ForEach(vm.filteredItems) { item in
                                    ADItemCard(item: item, onTap: { vm.handleTap(item) })
                                        .padding(.horizontal, 16)
                                        .padding(.vertical, 6)
                                }
                            }

                            Spacer().frame(height: 40)
                        }
                    }
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .background(Self.screenBg.ignoresSafeArea())
        .navigationBarHidden(true)
        .sheet(isPresented: Binding(
            get: { !vm.photoSheetUrls.isEmpty },
            set: { if !$0 { vm.photoSheetUrls = [] } }
        )) {
            ADPhotoSheet(urls: vm.photoSheetUrls)
        }
        .fullScreenCover(item: $vm.quizSheet) { result in
            QuizResultView(result: result, isParentView: true, onBack: { vm.quizSheet = nil })
        }
        .overlay {
            if vm.quizSheetLoading {
                Color.black.opacity(0.35).ignoresSafeArea()
                ProgressView()
                    .progressViewStyle(.circular)
                    .tint(.white)
                    .scaleEffect(1.3)
            }
        }
        .sheet(isPresented: Binding(
            get: { vm.subjectiveSheet != nil || vm.subjectiveSheetLoading },
            set: { if !$0 { vm.subjectiveSheet = nil } }
        )) {
            if vm.subjectiveSheetLoading {
                ProgressView()
                    .progressViewStyle(.circular)
                    .tint(Color.dobiPrimary)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if let result = vm.subjectiveSheet {
                ADSubjectiveResultSheet(result: result)
            }
        }
    }
}

// MARK: - Top bar

private struct ADTopBar: View {
    let title: String
    let childName: String
    let icon: String
    let iconTint: Color
    let iconTintBg: Color
    let onBack: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 0) {
                Button(action: onBack) {
                    Image(systemName: "arrow.left")
                        .font(.system(size: 20, weight: .medium))
                        .foregroundStyle(Color.dobiTextPrimary)
                        .frame(width: 40, height: 40)
                        .contentShape(Circle())
                }
                .buttonStyle(.plain)

                Spacer().frame(width: 4)

                ZStack {
                    RoundedRectangle(cornerRadius: 12)
                        .fill(iconTintBg)
                        .frame(width: 40, height: 40)
                    Image(systemName: icon)
                        .font(.system(size: 18, weight: .medium))
                        .foregroundStyle(iconTint)
                }

                Spacer().frame(width: 12)

                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.system(size: 19, weight: .bold))
                        .foregroundStyle(Color.dobiTextPrimary)
                    Text(childName.isEmpty ? "Attempts log" : "\(childName)'s attempts")
                        .font(.system(size: 12))
                        .foregroundStyle(Color.dobiTextSecondary)
                }

                Spacer()
            }
            .padding(.leading, 12)
            .padding(.trailing, 16)
            .padding(.top, 8)
            .padding(.bottom, 16)
        }
        .background(Color(hex: 0xFDF6F0))
    }
}

// MARK: - Filter chip row

private struct ADFilterChipRow: View {
    let selected: ADTimeFilter
    let onSelect: (ADTimeFilter) -> Void

    var body: some View {
        HStack(spacing: 8) {
            ForEach(ADTimeFilter.allCases, id: \.self) { f in
                let sel = selected == f
                Button { onSelect(f) } label: {
                    Text(f.label)
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(sel ? .white : Color.dobiTextPrimary)
                        .frame(maxWidth: .infinity)
                        .frame(height: 40)
                        .background(sel ? Color.dobiPrimary : Color.white)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                        .overlay(RoundedRectangle(cornerRadius: 12)
                            .strokeBorder(sel ? Color.dobiPrimary : Color.dobiBorderMuted, lineWidth: 1))
                }
                .buttonStyle(.plain)
                .animation(.spring(duration: 0.2), value: sel)
            }
        }
    }
}

// MARK: - Attempted summary

private struct ADAttemptedSummary: View {
    let count: Int
    let filter: ADTimeFilter

    var body: some View {
        Text("\(count) attempt\(count == 1 ? "" : "s") \(filter.rangeLabel)")
            .font(.system(size: 13))
            .foregroundStyle(Color.dobiTextSecondary)
            .frame(maxWidth: .infinity, alignment: .leading)
    }
}

// MARK: - Empty state

private struct ADEmptyState: View {
    let filter: ADTimeFilter
    let category: ActivityCategory

    var body: some View {
        VStack(spacing: 4) {
            Text("No \(category.title.lowercased()) yet")
                .font(.system(size: 15, weight: .bold))
                .foregroundStyle(Color.dobiTextPrimary)
            Text("Nothing recorded \(filter == .today ? "today" : filter == .week ? "this week" : "this month"). Check back after the next learning session.")
                .font(.system(size: 12))
                .foregroundStyle(Color.dobiTextSecondary)
                .multilineTextAlignment(.center)
        }
        .padding(20)
        .frame(maxWidth: .infinity)
        .background(Color.dobiPastelYellow)
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}

// MARK: - Item card

private struct ADCardButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.97 : 1.0)
            .animation(.spring(duration: 0.15), value: configuration.isPressed)
    }
}

private struct ADItemCard: View {
    let item: ActivityLogItem
    var onTap: () -> Void = {}

    private var visuals: (label: String, icon: String, tint: Color, bg: Color) {
        switch item.source {
        case .upload:
            return (uploadKindLabel(item.uploadKind), "photo.on.rectangle.angled", Color(hex: 0xF59E0B), Color(hex: 0xFFF8EC))
        case .quizMcq:
            return ("Quiz", "questionmark.circle.fill", Color.dobiPrimary, Color.dobiPastelSkyBlue)
        case .quizSubjective:
            return ("Write Q&A", "book.fill", Color.dobiSuccess, Color.dobiPastelMint)
        case .homework:
            return ("Homework", "pencil.and.list.clipboard", Color(hex: 0xF59E0B), Color(hex: 0xFFF8EC))
        }
    }

    private var cardContent: some View {
        HStack(spacing: 12) {
            // Thumbnail or icon
            if let thumb = item.photoUrls.first {
                AsyncImage(url: URL(string: thumb)) { phase in
                    switch phase {
                    case .success(let img):
                        img.resizable().scaledToFill()
                    default:
                        Color.dobiBorderMuted
                    }
                }
                .frame(width: 48, height: 48)
                .clipShape(RoundedRectangle(cornerRadius: 12))
            } else {
                let v = visuals
                ZStack {
                    RoundedRectangle(cornerRadius: 12)
                        .fill(v.bg)
                        .frame(width: 48, height: 48)
                    Image(systemName: v.icon)
                        .font(.system(size: 20))
                        .foregroundStyle(v.tint)
                }
            }

            // Title + tags
            VStack(alignment: .leading, spacing: 4) {
                Text(item.title)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(Color.dobiTextPrimary)
                    .lineLimit(2)
                    .lineSpacing(2)

                HStack(spacing: 6) {
                    let v = visuals
                    ADSourceTag(label: v.label, tint: v.tint, bg: v.bg)

                    if !item.subject.isEmpty {
                        ADSubjectPill(subject: item.subject)
                    }

                    Text(formatRelative(item.timestamp))
                        .font(.system(size: 11))
                        .foregroundStyle(Color.dobiTextSecondary)
                }
            }

            Spacer(minLength: 6)

            // Score badge
            if let score = item.score, score.total > 0 {
                let success = score.correct * 2 >= score.total
                ADScoreBadge(label: "\(score.correct)/\(score.total)", isSuccess: success)
            } else if item.source == .upload {
                ADScoreBadge(label: "Submitted", isSuccess: true)
            }
        }
        .padding(14)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .overlay(RoundedRectangle(cornerRadius: 16).strokeBorder(Color.dobiBorderMuted, lineWidth: 1))
        .contentShape(RoundedRectangle(cornerRadius: 16))
    }

    var body: some View {
        Button(action: onTap) { cardContent }
            .buttonStyle(ADCardButtonStyle())
    }

    private func uploadKindLabel(_ kind: String) -> String {
        switch kind {
        case "class_work":              return "Class work"
        case "self_learning":           return "Self learning"
        case "ai_test_prep":            return "AI practice"
        case "tuition_work_learning":   return "Tuition learning"
        case "tuition_work_written":    return "Tuition written"
        case "tuition_work_test":       return "Tuition test"
        case "tuition_hw_learning":     return "Homework reading"
        case "tuition_hw_written":      return "Homework"
        default:                        return "Upload"
        }
    }

    private func formatRelative(_ ts: Int64) -> String {
        guard ts > 0 else { return "—" }
        let now = Int64(Date().timeIntervalSince1970 * 1000)
        let deltaSec = max(0, (now - ts)) / 1000
        let deltaMin = deltaSec / 60
        switch deltaMin {
        case 0:          return "Just now"
        case 1..<60:     return "\(deltaMin)m ago"
        case 60..<1440:  return "\(deltaMin / 60)h ago"
        case 1440..<2880: return "Yesterday"
        case 2880..<43200: return "\(deltaMin / 1440)d ago"
        default:         return "\(deltaMin / 43200)mo ago"
        }
    }
}

private struct ADSourceTag: View {
    let label: String; let tint: Color; let bg: Color
    var body: some View {
        Text(label)
            .font(.system(size: 10, weight: .semibold))
            .foregroundStyle(tint)
            .padding(.horizontal, 7)
            .padding(.vertical, 2)
            .background(bg)
            .clipShape(RoundedRectangle(cornerRadius: 6))
    }
}

private struct ADSubjectPill: View {
    let subject: String
    private var color: Color {
        switch subject.lowercased() {
        case "math", "mathematics":                                      return Color(hex: 0x7C3AED)
        case "science":                                                  return Color.dobiPrimary
        case "english":                                                  return Color.dobiError
        case "hindi":                                                    return Color(hex: 0xF59E0B)
        case "social", "social studies", "social science",
             "history", "geography":                                     return Color(hex: 0x14B8A6)
        default:                                                         return Color.dobiTextSecondary
        }
    }
    var body: some View {
        Text(subject)
            .font(.system(size: 10, weight: .semibold))
            .foregroundStyle(color)
            .padding(.horizontal, 7)
            .padding(.vertical, 2)
            .background(color.opacity(0.16))
            .clipShape(RoundedRectangle(cornerRadius: 6))
    }
}

private struct ADScoreBadge: View {
    let label: String; let isSuccess: Bool
    var body: some View {
        HStack(spacing: 4) {
            if isSuccess {
                Image(systemName: "checkmark.circle.fill")
                    .font(.system(size: 11))
                    .foregroundStyle(Color.dobiSuccess)
            }
            Text(label)
                .font(.system(size: 11, weight: .bold))
                .foregroundStyle(isSuccess ? Color.dobiSuccess : Color.dobiError)
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 5)
        .background(isSuccess ? Color.dobiSuccessContainer : Color(hex: 0xFFF1F1))
        .clipShape(RoundedRectangle(cornerRadius: 8))
    }
}

private struct ADStatCell: View {
    let value: String; let label: String; let color: Color
    var body: some View {
        VStack(spacing: 4) {
            Text(value)
                .font(.system(size: 18, weight: .bold))
                .foregroundStyle(color)
            Text(label)
                .font(.system(size: 11))
                .foregroundStyle(Color.dobiTextSecondary)
        }
    }
}

// MARK: - Subjective result sheet (Write Q&A)

private struct ADSubjectiveResultSheet: View {
    let result: ADSubjectiveResult
    @Environment(\.dismiss) private var dismiss

    private var accent: Color {
        result.accuracyPct >= 80 ? Color.dobiSuccess
            : result.accuracyPct >= 50 ? Color(hex: 0xF59E0B)
            : Color.dobiError
    }

    var body: some View {
        VStack(spacing: 0) {
            // Top bar
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(result.displayName)
                        .font(.system(size: 17, weight: .bold))
                        .foregroundStyle(Color.dobiTextPrimary)
                    if !result.subject.isEmpty {
                        Text(result.subject)
                            .font(.system(size: 12))
                            .foregroundStyle(Color.dobiTextSecondary)
                    }
                }
                Spacer()
                Button { dismiss() } label: {
                    Image(systemName: "xmark.circle.fill")
                        .font(.system(size: 24))
                        .foregroundStyle(Color.dobiTextSecondary)
                }
            }
            .padding(.horizontal, 20)
            .padding(.top, 20)
            .padding(.bottom, 16)

            // Summary strip
            HStack(spacing: 0) {
                ADStatCell(value: "\(result.correct)/\(result.attempted)", label: "Correct", color: accent)
                    .frame(maxWidth: .infinity)
                Divider().frame(height: 32)
                ADStatCell(value: "\(result.accuracyPct)%", label: "Accuracy", color: accent)
                    .frame(maxWidth: .infinity)
            }
            .padding(.vertical, 12)
            .background(accent.opacity(0.08))
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .padding(.horizontal, 16)
            .padding(.bottom, 12)

            // Q&A list
            ScrollView {
                LazyVStack(spacing: 10) {
                    ForEach(Array(result.items.enumerated()), id: \.offset) { idx, qa in
                        ADQACard(index: idx + 1, qa: qa)
                    }
                    Spacer().frame(height: 24)
                }
                .padding(.horizontal, 16)
                .padding(.top, 4)
            }
        }
        .background(Color(hex: 0xF8FAFF).ignoresSafeArea())
        .presentationDetents([.large])
        .presentationDragIndicator(.visible)
    }
}

private struct ADQACard: View {
    let index: Int
    let qa: ADSubjectiveResultItem

    private var verdictColor: Color {
        !qa.isAttempted ? Color.dobiTextSecondary
            : qa.isCorrect ? Color.dobiSuccess
            : Color.dobiError
    }
    private var verdictLabel: String {
        !qa.isAttempted ? "Skipped" : qa.isCorrect ? "Correct" : "Needs work"
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .top, spacing: 8) {
                Text("Q\(index)")
                    .font(.system(size: 11, weight: .bold))
                    .foregroundStyle(Color.dobiTextSecondary)
                    .padding(.top, 1)
                Text(qa.questionText)
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(Color.dobiTextPrimary)
                Spacer()
                Text(verdictLabel)
                    .font(.system(size: 10, weight: .semibold))
                    .foregroundStyle(verdictColor)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 3)
                    .background(verdictColor.opacity(0.12))
                    .clipShape(RoundedRectangle(cornerRadius: 6))
            }

            if !qa.answerSeen.isEmpty {
                VStack(alignment: .leading, spacing: 3) {
                    Text("Answer")
                        .font(.system(size: 10, weight: .semibold))
                        .foregroundStyle(Color.dobiTextSecondary)
                    Text(qa.answerSeen)
                        .font(.system(size: 12))
                        .foregroundStyle(Color.dobiTextPrimary)
                }
                .padding(10)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color.dobiPastelSkyBlue)
                .clipShape(RoundedRectangle(cornerRadius: 8))
            }

            if !qa.strictFeedback.isEmpty {
                HStack(alignment: .top, spacing: 6) {
                    Image(systemName: "lightbulb.fill")
                        .font(.system(size: 10))
                        .foregroundStyle(Color(hex: 0xF59E0B))
                    Text(qa.strictFeedback)
                        .font(.system(size: 11))
                        .foregroundStyle(Color.dobiTextSecondary)
                }
            }
        }
        .padding(14)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 14))
        .overlay(RoundedRectangle(cornerRadius: 14).strokeBorder(Color.dobiBorderMuted, lineWidth: 1))
    }
}

// MARK: - Photo sheet

private struct ADPhotoSheet: View {
    let urls: [String]
    @State private var page = 0
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        ZStack(alignment: .topTrailing) {
            Color.black.ignoresSafeArea()

            TabView(selection: $page) {
                ForEach(Array(urls.enumerated()), id: \.offset) { idx, url in
                    AsyncImage(url: URL(string: url)) { phase in
                        switch phase {
                        case .success(let img):
                            img.resizable().scaledToFit()
                        case .failure:
                            Image(systemName: "photo")
                                .font(.system(size: 48))
                                .foregroundStyle(.white.opacity(0.4))
                        default:
                            ProgressView().tint(.white)
                        }
                    }
                    .tag(idx)
                }
            }
            .tabViewStyle(.page)
            .indexViewStyle(.page(backgroundDisplayMode: .always))

            Button {
                dismiss()
            } label: {
                Image(systemName: "xmark.circle.fill")
                    .font(.system(size: 28))
                    .foregroundStyle(.white.opacity(0.8))
                    .padding(16)
            }

            if urls.count > 1 {
                Text("\(page + 1) / \(urls.count)")
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(.white.opacity(0.7))
                    .padding(.top, 20)
                    .frame(maxWidth: .infinity)
            }
        }
    }
}
