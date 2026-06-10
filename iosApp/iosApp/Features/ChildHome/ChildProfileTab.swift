import SwiftUI

// MARK: - Profile tab

struct ChildProfileTab: View {
    let childId: String
    let childName: String
    var onSwitchToParent: () -> Void
    var onInviteFriend: () -> Void = {}

    @EnvironmentObject private var appState: AppState
    @State private var topicsCount = 0
    @State private var quizzesCount = 0
    @State private var averageAccuracy = 0
    @State private var subjects: [ProfileSubjectStat] = []
    @State private var allItems: [ActivityLogItem] = []
    @State private var selectedSubject: ProfileSubjectStat? = nil

    private var child: ChildProfile? { appState.children.first { $0.id == childId } }
    private var streakDays: Int { appState.engagements[childId]?.streakDays ?? 0 }

    private var classBoard: String {
        let grade = child?.grade ?? ""
        let board = child?.board ?? "CBSE"
        let g = grade.isEmpty ? "" : "Class \(grade)"
        return g.isEmpty ? board : "\(g) • \(board)"
    }

    private var avatarEmoji: String {
        switch (child?.gender ?? "male").lowercased() {
        case "girl", "female", "f": return "👧"
        case "boy", "male", "m":    return "👦"
        default:                    return "🧒"
        }
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                Spacer().frame(height: 8)

                CPIdentityCard(
                    name: childName,
                    classBoard: classBoard,
                    avatarEmoji: avatarEmoji,
                    streakDays: streakDays
                )

                CPStatsHero(
                    streakDays: streakDays,
                    topicsCount: topicsCount,
                    quizzesCount: quizzesCount,
                    averageAccuracy: averageAccuracy
                )

                CPSectionHeader(title: "Subjects you've explored", emoji: "🎓")

                if subjects.isEmpty {
                    CPEmptySubjectsCard()
                } else {
                    VStack(spacing: 10) {
                        ForEach(subjects) { s in
                            CPSubjectRow(subject: s, onTap: { selectedSubject = s })
                        }
                    }
                }

                CPSectionHeader(
                    title: "Studying with friends",
                    emoji: "🤝",
                    trailing: "Invite",
                    onTrailingClick: onInviteFriend
                )

                CPFriendsCard(onInvite: onInviteFriend)

                Spacer().frame(height: 4)
                CPSwitchProfileCard(onClick: onSwitchToParent)

                Spacer().frame(height: 100)
            }
            .padding(.horizontal, 20)
            .padding(.top, 16)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.dobiPageBg)
        .sheet(item: $selectedSubject) { s in
            CPSubjectDetailView(
                subject: s,
                items: allItems.filter { $0.subject.trimmingCharacters(in: .whitespaces) == s.name },
                childId: childId,
                onDismiss: { selectedSubject = nil }
            )
        }
        .onAppear {
            Task {
                let items = await FirebaseService.shared.fetchActivityItems(childId: childId)
                allItems = items
                let stats = computeProfileStats(from: items)
                topicsCount = stats.topicsCount
                quizzesCount = stats.quizzesCount
                averageAccuracy = stats.averageAccuracy
                subjects = stats.subjects
            }
        }
    }
}

// MARK: - Profile stats model

struct ProfileSubjectStat: Identifiable {
    var id: String { name }
    let name: String
    let emoji: String
    let topicsCount: Int
    let accuracyPct: Int?
}

private struct ProfileStats {
    var topicsCount: Int = 0
    var quizzesCount: Int = 0
    var averageAccuracy: Int = 0
    var subjects: [ProfileSubjectStat] = []
}

private func computeProfileStats(from items: [ActivityLogItem]) -> ProfileStats {
    var topicKeys = Set<String>()
    var quizCorrect = 0
    var quizTotal = 0
    var quizzesCount = 0

    struct SubjectAcc { var topics = Set<String>(); var correct = 0; var total = 0 }
    var perSubject: [String: SubjectAcc] = [:]

    for item in items {
        let subject = item.subject.trimmingCharacters(in: .whitespaces)
        switch item.source {
        case .upload:
            let key: String
            switch item.detail {
            case .upload(let gk, _): key = gk
            default: key = item.id
            }
            topicKeys.insert(key)
            if !subject.isEmpty {
                perSubject[subject, default: SubjectAcc()].topics.insert(key)
            }

        case .quizMcq, .quizSubjective:
            topicKeys.insert("quiz-\(item.id)")
            quizzesCount += 1
            if let score = item.score {
                quizCorrect += score.correct
                quizTotal += score.total
            }
            if !subject.isEmpty {
                let key = "quiz-\(item.id)"
                perSubject[subject, default: SubjectAcc()].topics.insert(key)
                if let score = item.score {
                    perSubject[subject]!.correct += score.correct
                    perSubject[subject]!.total  += score.total
                }
            }

        case .homework:
            topicKeys.insert(item.id)
            if !subject.isEmpty {
                perSubject[subject, default: SubjectAcc()].topics.insert(item.id)
            }
        }
    }

    let avgAccuracy = quizTotal > 0 ? Int(Float(quizCorrect) / Float(quizTotal) * 100) : 0

    let subjectStats = perSubject
        .map { (name, acc) in
            ProfileSubjectStat(
                name: name,
                emoji: subjectEmoji(name),
                topicsCount: acc.topics.count,
                accuracyPct: acc.total > 0 ? Int(Float(acc.correct) / Float(acc.total) * 100) : nil
            )
        }
        .sorted { $0.topicsCount > $1.topicsCount }
        .prefix(6)

    return ProfileStats(
        topicsCount: topicKeys.count,
        quizzesCount: quizzesCount,
        averageAccuracy: avgAccuracy,
        subjects: Array(subjectStats)
    )
}

private func subjectEmoji(_ subject: String) -> String {
    switch subject.lowercased().trimmingCharacters(in: .whitespaces) {
    case "science":                             return "🔬"
    case "physics":                             return "⚛️"
    case "chemistry":                           return "🧪"
    case "biology":                             return "🌱"
    case "maths", "mathematics", "math":       return "🧮"
    case "english":                             return "📖"
    case "hindi":                               return "✒️"
    case "social science", "social studies", "social": return "🌏"
    case "history":                             return "🏺"
    case "geography":                           return "🗺️"
    case "civics":                              return "⚖️"
    case "computer", "computer science", "computers": return "💻"
    case "evs", "environmental studies":        return "🌳"
    case "sanskrit":                            return "📜"
    case "tuition":                             return "🎒"
    default:                                    return "📘"
    }
}

// MARK: - Identity card

private struct CPIdentityCard: View {
    let name: String
    let classBoard: String
    let avatarEmoji: String
    let streakDays: Int

    private let accent = Color(hex: 0x7C8BFF)

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [Color(hex: 0xE8F1FF), Color(hex: 0xF1E8FF), Color(hex: 0xFFE8F1)],
                startPoint: .topLeading, endPoint: .bottomTrailing
            )
            .clipShape(RoundedRectangle(cornerRadius: 24))
            .overlay(RoundedRectangle(cornerRadius: 24).stroke(Color.white.opacity(0.6), lineWidth: 1))

            HStack(spacing: 16) {
                // Avatar
                ZStack {
                    Circle()
                        .fill(Color.white.opacity(0.7))
                        .frame(width: 84, height: 84)
                    ZStack {
                        Circle()
                            .fill(LinearGradient(
                                colors: [Color(hex: 0x7C8BFF), Color(hex: 0x5B6BFF)],
                                startPoint: .topLeading, endPoint: .bottomTrailing
                            ))
                            .frame(width: 72, height: 72)
                        Text(avatarEmoji).font(.system(size: 38))
                    }
                    .shadow(color: accent.opacity(0.4), radius: 8, x: 0, y: 4)
                }

                VStack(alignment: .leading, spacing: 4) {
                    Text(name.isEmpty ? "Hi there!" : name)
                        .font(.system(size: 22, weight: .bold))
                        .foregroundStyle(Color.dobiTextPrimary)
                        .lineLimit(1)
                    Text(classBoard)
                        .font(.system(size: 13, weight: .medium))
                        .foregroundStyle(Color.dobiTextSecondary)
                    HStack(spacing: 6) {
                        CPMiniBadge(text: "🌟 Learner")
                        if streakDays > 0 {
                            CPMiniBadge(text: "🔥 \(streakDays)-day streak")
                        }
                    }
                    .padding(.top, 4)
                }
                Spacer()
            }
            .padding(20)
        }
        .shadow(color: accent.opacity(0.25), radius: 12, x: 0, y: 4)
    }
}

private struct CPMiniBadge: View {
    let text: String
    var body: some View {
        Text(text)
            .font(.system(size: 11, weight: .semibold))
            .foregroundStyle(Color.dobiTextPrimary)
            .lineLimit(1)
            .fixedSize(horizontal: true, vertical: false)
            .padding(.horizontal, 10).padding(.vertical, 4)
            .background(Color.white.opacity(0.85))
            .clipShape(RoundedRectangle(cornerRadius: 20))
    }
}

// MARK: - Stats hero

private struct CPStatsHero: View {
    let streakDays: Int
    let topicsCount: Int
    let quizzesCount: Int
    let averageAccuracy: Int

    var body: some View {
        DobiCard(cornerRadius: 18) {
            HStack(spacing: 0) {
                CPStatCell(emoji: "🔥", value: "\(streakDays)", label: "day streak", accent: Color(hex: 0xE67E22))
                CPStatDivider()
                CPStatCell(emoji: "📚", value: "\(topicsCount)", label: "topics", accent: Color.dobiPrimary)
                CPStatDivider()
                CPStatCell(emoji: "🧠", value: "\(quizzesCount)", label: "quizzes", accent: Color(hex: 0x7C3AED))
                CPStatDivider()
                CPStatCell(
                    emoji: "🎯",
                    value: quizzesCount > 0 ? "\(averageAccuracy)%" : "—",
                    label: "accuracy",
                    accent: Color.dobiSuccess
                )
            }
            .padding(.horizontal, 6).padding(.vertical, 14)
        }
    }
}

private struct CPStatCell: View {
    let emoji: String
    let value: String
    let label: String
    let accent: Color

    var body: some View {
        VStack(spacing: 4) {
            Text(emoji).font(.system(size: 22))
            Text(value)
                .font(.system(size: 18, weight: .bold))
                .foregroundStyle(accent)
                .lineLimit(1)
            Text(label)
                .font(.system(size: 10, weight: .medium))
                .foregroundStyle(Color.dobiTextSecondary)
                .lineLimit(1)
        }
        .frame(maxWidth: .infinity)
    }
}

private struct CPStatDivider: View {
    var body: some View {
        Rectangle()
            .fill(Color(hex: 0xEEF1F8))
            .frame(width: 1, height: 36)
    }
}

// MARK: - Section header

private struct CPSectionHeader: View {
    let title: String
    let emoji: String
    var trailing: String? = nil
    var onTrailingClick: (() -> Void)? = nil

    var body: some View {
        HStack {
            HStack(spacing: 8) {
                Text(emoji).font(.system(size: 18))
                Text(title)
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(Color.dobiTextPrimary)
            }
            Spacer()
            if let label = trailing {
                Button(action: { onTrailingClick?() }) {
                    Text(label)
                        .font(.system(size: 13, weight: .medium))
                        .foregroundStyle(Color.dobiPrimary)
                }
                .buttonStyle(.plain)
            }
        }
    }
}

// MARK: - Subjects

private struct CPSubjectRow: View {
    let subject: ProfileSubjectStat
    var onTap: () -> Void = {}

    var body: some View {
        Button(action: onTap) {
            DobiCard(cornerRadius: 14) {
                HStack(spacing: 12) {
                    ZStack {
                        RoundedRectangle(cornerRadius: 12)
                            .fill(Color(hex: 0xF4F6FE))
                            .frame(width: 40, height: 40)
                        Text(subject.emoji).font(.system(size: 22))
                    }
                    VStack(alignment: .leading, spacing: 2) {
                        Text(subject.name)
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundStyle(Color.dobiTextPrimary)
                        Text("\(subject.topicsCount) topic\(subject.topicsCount == 1 ? "" : "s")")
                            .font(.system(size: 11))
                            .foregroundStyle(Color.dobiTextSecondary)
                    }
                    Spacer()
                    if let acc = subject.accuracyPct {
                        let (fg, bg): (Color, Color) = acc >= 80
                            ? (Color.dobiSuccess, Color.dobiSuccess.opacity(0.12))
                            : acc >= 50
                            ? (Color.dobiWarning, Color.dobiWarning.opacity(0.12))
                            : (Color(hex: 0xEF4444), Color(hex: 0xFEE2E2))
                        Text("\(acc)%")
                            .font(.system(size: 12, weight: .bold))
                            .foregroundStyle(fg)
                            .padding(.horizontal, 8).padding(.vertical, 4)
                            .background(bg)
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                    }
                    Image(systemName: "chevron.right")
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(Color.dobiTextSecondary)
                }
                .padding(14)
            }
        }
        .buttonStyle(.plain)
    }
}

private struct CPEmptySubjectsCard: View {
    var body: some View {
        HStack(spacing: 12) {
            Circle()
                .fill(Color.dobiPrimaryContainer)
                .frame(width: 40, height: 40)
                .overlay(Text("🎓").font(.system(size: 20)))
            VStack(alignment: .leading, spacing: 2) {
                Text("Your subjects will show up here")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(Color.dobiTextPrimary)
                Text("Take a quiz or upload some work to start tracking your progress.")
                    .font(.system(size: 12))
                    .foregroundStyle(Color.dobiTextSecondary)
                    .lineSpacing(3)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(hex: 0xF8FAFF))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color(hex: 0xE5EBF5), lineWidth: 1))
    }
}

// MARK: - Friends

private struct CPFriendsCard: View {
    let onInvite: () -> Void

    var body: some View {
        DobiCard(cornerRadius: 16) {
            HStack(spacing: 12) {
                Circle()
                    .fill(Color.dobiSuccess.opacity(0.15))
                    .frame(width: 44, height: 44)
                    .overlay(Text("🚀").font(.system(size: 22)))

                VStack(alignment: .leading, spacing: 2) {
                    Text("No friends here yet")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(Color.dobiTextPrimary)
                    Text("Invite a friend so you can keep streaks together.")
                        .font(.system(size: 12))
                        .foregroundStyle(Color.dobiTextSecondary)
                        .lineSpacing(3)
                }

                Spacer()

                Button(action: onInvite) {
                    HStack(spacing: 5) {
                        Image(systemName: "person.badge.plus")
                            .font(.system(size: 12, weight: .semibold))
                        Text("Invite")
                            .font(.system(size: 12, weight: .semibold))
                    }
                    .foregroundStyle(.white)
                    .padding(.horizontal, 12).padding(.vertical, 8)
                    .background(Color.dobiPrimary)
                    .clipShape(RoundedRectangle(cornerRadius: 20))
                }
                .buttonStyle(.plain)
            }
            .padding(14)
        }
    }
}

// MARK: - Switch profile card

private struct CPSwitchProfileCard: View {
    let onClick: () -> Void

    var body: some View {
        Button(action: onClick) {
            HStack(spacing: 12) {
                Circle()
                    .fill(Color(hex: 0xF1F5F9))
                    .frame(width: 36, height: 36)
                    .overlay(
                        Image(systemName: "rectangle.portrait.and.arrow.right")
                            .font(.system(size: 16, weight: .medium))
                            .foregroundStyle(Color.dobiTextSecondary)
                    )
                VStack(alignment: .leading, spacing: 2) {
                    Text("Switch profile")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(Color.dobiTextPrimary)
                    Text("Hand the phone back to a parent or pick a different child.")
                        .font(.system(size: 12))
                        .foregroundStyle(Color.dobiTextSecondary)
                        .lineSpacing(2)
                        .multilineTextAlignment(.leading)
                }
                Spacer()
            }
            .padding(.horizontal, 14).padding(.vertical, 14)
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(.plain)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 14))
        .overlay(RoundedRectangle(cornerRadius: 14).stroke(Color(hex: 0xE5EBF5), lineWidth: 1))
    }
}

// MARK: - Subject detail sheet

struct CPSubjectDetailView: View {
    let subject: ProfileSubjectStat
    let items: [ActivityLogItem]
    let childId: String
    var onDismiss: () -> Void = {}

    @State private var photoUrls: [String] = []
    @State private var quizResult: ADQuizResult? = nil
    @State private var quizLoading = false
    @State private var subjectiveResult: ADSubjectiveResult? = nil
    @State private var subjectiveLoading = false

    private var sorted: [ActivityLogItem] {
        items.sorted { $0.timestamp > $1.timestamp }
    }

    var body: some View {
        VStack(spacing: 0) {
            // Header
            HStack {
                Button(action: onDismiss) {
                    Image(systemName: "xmark")
                        .font(.system(size: 15, weight: .medium))
                        .foregroundStyle(Color.dobiTextSecondary)
                        .frame(width: 32, height: 32)
                        .background(Color(hex: 0xF1F5F9))
                        .clipShape(Circle())
                }
                .buttonStyle(.plain)
                Spacer()
                VStack(spacing: 2) {
                    Text("\(subject.emoji) \(subject.name)")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundStyle(Color.dobiTextPrimary)
                    Text("\(subject.topicsCount) topic\(subject.topicsCount == 1 ? "" : "s")\(subject.accuracyPct.map { " • \($0)% accuracy" } ?? "")")
                        .font(.system(size: 12))
                        .foregroundStyle(Color.dobiTextSecondary)
                }
                Spacer()
                Color.clear.frame(width: 32, height: 32)
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 16)
            .background(Color.white)

            Divider()

            if sorted.isEmpty {
                Spacer()
                VStack(spacing: 10) {
                    Text("📭").font(.system(size: 40))
                    Text("No activities yet")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(Color.dobiTextPrimary)
                    Text("Take a quiz or upload work for \(subject.name) to see it here.")
                        .font(.system(size: 13))
                        .foregroundStyle(Color.dobiTextSecondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 40)
                }
                Spacer()
            } else {
                ScrollView {
                    LazyVStack(spacing: 8) {
                        ForEach(sorted) { item in
                            CPActivityItemRow(item: item, onTap: { handleTap(item) })
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 14)
                    .padding(.bottom, 40)
                }
            }
        }
        .background(Color.dobiPageBg.ignoresSafeArea())
        // Photo sheet
        .sheet(isPresented: Binding(get: { !photoUrls.isEmpty }, set: { if !$0 { photoUrls = [] } })) {
            CPPhotoSheet(urls: photoUrls, onDismiss: { photoUrls = [] })
        }
        // Quiz MCQ full-screen
        .fullScreenCover(item: $quizResult) { result in
            QuizResultView(result: result, isParentView: false, onBack: { quizResult = nil })
        }
        // Subjective result sheet
        .sheet(isPresented: Binding(
            get: { subjectiveResult != nil || subjectiveLoading },
            set: { if !$0 { subjectiveResult = nil } }
        )) {
            if subjectiveLoading {
                ProgressView().tint(Color.dobiPrimary).frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if let r = subjectiveResult {
                CPSubjectiveSheet(result: r, onDismiss: { subjectiveResult = nil })
            }
        }
        // Quiz loading overlay
        .overlay {
            if quizLoading {
                Color.black.opacity(0.35).ignoresSafeArea()
                ProgressView().tint(.white).scaleEffect(1.3)
            }
        }
    }

    private func handleTap(_ item: ActivityLogItem) {
        switch item.detail {
        case .upload(_, let urls) where !urls.isEmpty:
            photoUrls = urls
        case .homework(_, let urls) where !urls.isEmpty:
            photoUrls = urls
        case .quizMcq(let unitId, let conceptId):
            Task {
                quizLoading = true
                quizResult = await FirebaseService.shared.fetchQuizResult(childId: childId, unitId: unitId, conceptId: conceptId)
                quizLoading = false
            }
        case .quizSubjective(let resultId):
            Task {
                subjectiveLoading = true
                subjectiveResult = await FirebaseService.shared.fetchSubjectiveResult(resultId: resultId)
                subjectiveLoading = false
            }
        default: break
        }
    }
}

// MARK: - Activity item row (used inside CPSubjectDetailView)

private struct CPActivityItemRow: View {
    let item: ActivityLogItem
    var onTap: () -> Void = {}

    private var isTappable: Bool {
        switch item.detail {
        case .upload(_, let urls):   return !urls.isEmpty
        case .homework(_, let urls): return !urls.isEmpty
        case .quizMcq:               return true
        case .quizSubjective:        return true
        }
    }

    private var typeTag: String {
        switch item.source {
        case .quizMcq:        return "Quiz"
        case .quizSubjective: return "Write Answer"
        case .homework:       return "Homework"
        case .upload:
            switch item.uploadKind {
            case "class_work":    return "Class Work"
            case "self_learning": return "Self Study"
            default:              return "Upload"
            }
        }
    }

    private var typeConfig: (icon: String, color: Color, bg: Color) {
        switch typeTag {
        case "Quiz":         return ("questionmark.circle.fill", Color.dobiPrimary, Color.dobiPrimaryContainer)
        case "Write Answer": return ("pencil.circle.fill", Color(hex: 0x7C5CF8), Color(hex: 0xEDE9FE))
        case "Homework":     return ("checkmark.seal.fill", Color(hex: 0xF59E0B), Color(hex: 0xFFF3E0))
        case "Class Work":   return ("book.fill", Color(hex: 0x10B981), Color(hex: 0xD1FAE5))
        case "Self Study":   return ("lightbulb.fill", Color(hex: 0xEF4444), Color(hex: 0xFEE2E2))
        default:             return ("arrow.up.doc.fill", Color.dobiTextSecondary, Color(hex: 0xF1F5F9))
        }
    }

    private var dateLabel: String {
        guard item.timestamp > 0 else { return "" }
        let date = Date(timeIntervalSince1970: Double(item.timestamp) / 1000.0)
        let cal = Calendar.current
        if cal.isDateInToday(date)     { return "Today" }
        if cal.isDateInYesterday(date) { return "Yesterday" }
        let fmt = DateFormatter(); fmt.dateFormat = "d MMM"
        return fmt.string(from: date)
    }

    var body: some View {
        Button(action: { if isTappable { onTap() } }) {
            HStack(spacing: 12) {
                let cfg = typeConfig
                ZStack {
                    RoundedRectangle(cornerRadius: 10)
                        .fill(cfg.bg)
                        .frame(width: 40, height: 40)
                    Image(systemName: cfg.icon)
                        .font(.system(size: 18))
                        .foregroundStyle(cfg.color)
                }

                VStack(alignment: .leading, spacing: 3) {
                    Text(item.title.isEmpty ? typeTag : item.title)
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(Color.dobiTextPrimary)
                        .lineLimit(2)
                    HStack(spacing: 6) {
                        Text(typeTag)
                            .font(.system(size: 10, weight: .semibold))
                            .foregroundStyle(cfg.color)
                            .padding(.horizontal, 6).padding(.vertical, 2)
                            .background(cfg.bg)
                            .clipShape(RoundedRectangle(cornerRadius: 4))
                        if !dateLabel.isEmpty {
                            Text("·").foregroundStyle(Color.dobiTextSecondary).font(.system(size: 11))
                            Text(dateLabel).font(.system(size: 11)).foregroundStyle(Color.dobiTextSecondary)
                        }
                    }
                }

                Spacer()

                if let score = item.score, score.total > 0 {
                    let pct = Int(Double(score.correct) / Double(score.total) * 100)
                    let fg: Color = pct >= 80 ? Color.dobiSuccess : pct >= 50 ? Color.dobiWarning : Color(hex: 0xEF4444)
                    VStack(spacing: 1) {
                        Text("\(score.correct)/\(score.total)")
                            .font(.system(size: 12, weight: .bold))
                            .foregroundStyle(fg)
                        Text("\(pct)%")
                            .font(.system(size: 10))
                            .foregroundStyle(fg.opacity(0.75))
                    }
                }

                if isTappable {
                    Image(systemName: "chevron.right")
                        .font(.system(size: 11, weight: .medium))
                        .foregroundStyle(Color.dobiTextSecondary)
                }
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 14))
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Photo sheet (child profile context)

private struct CPPhotoSheet: View {
    let urls: [String]
    var onDismiss: () -> Void = {}

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Spacer()
                Button(action: onDismiss) {
                    Image(systemName: "xmark.circle.fill")
                        .font(.system(size: 28))
                        .foregroundStyle(Color.dobiTextSecondary)
                }
                .buttonStyle(.plain)
            }
            .padding(.horizontal, 20)
            .padding(.top, 16)
            .padding(.bottom, 8)

            ScrollView {
                LazyVStack(spacing: 12) {
                    ForEach(urls, id: \.self) { url in
                        AsyncImage(url: URL(string: url)) { phase in
                            switch phase {
                            case .success(let img):
                                img.resizable().scaledToFit()
                                    .clipShape(RoundedRectangle(cornerRadius: 12))
                            case .failure:
                                RoundedRectangle(cornerRadius: 12).fill(Color(hex: 0xF1F5F9))
                                    .frame(height: 180)
                                    .overlay(Image(systemName: "photo").foregroundStyle(Color.dobiTextSecondary))
                            default:
                                RoundedRectangle(cornerRadius: 12).fill(Color(hex: 0xF1F5F9))
                                    .frame(height: 180)
                                    .overlay(ProgressView())
                            }
                        }
                        .padding(.horizontal, 16)
                    }
                }
                .padding(.bottom, 40)
            }
        }
        .background(Color.white.ignoresSafeArea())
    }
}

// MARK: - Subjective result sheet (child profile context)

private struct CPSubjectiveSheet: View {
    let result: ADSubjectiveResult
    var onDismiss: () -> Void = {}

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(result.displayName.isEmpty ? "Write Answer" : result.displayName)
                        .font(.system(size: 16, weight: .bold))
                        .foregroundStyle(Color.dobiTextPrimary)
                    Text("\(result.correct)/\(result.attempted) correct · \(result.accuracyPct)%")
                        .font(.system(size: 12))
                        .foregroundStyle(Color.dobiTextSecondary)
                }
                Spacer()
                Button(action: onDismiss) {
                    Image(systemName: "xmark.circle.fill")
                        .font(.system(size: 26))
                        .foregroundStyle(Color.dobiTextSecondary)
                }
                .buttonStyle(.plain)
            }
            .padding(.horizontal, 20)
            .padding(.top, 20)
            .padding(.bottom, 16)

            Divider()

            ScrollView {
                LazyVStack(spacing: 10) {
                    ForEach(Array(result.items.enumerated()), id: \.offset) { idx, item in
                        VStack(alignment: .leading, spacing: 8) {
                            HStack(alignment: .top, spacing: 10) {
                                ZStack {
                                    Circle()
                                        .fill(item.isCorrect ? Color.dobiSuccess.opacity(0.15) : Color(hex: 0xFEE2E2))
                                        .frame(width: 28, height: 28)
                                    Image(systemName: item.isCorrect ? "checkmark" : "xmark")
                                        .font(.system(size: 12, weight: .bold))
                                        .foregroundStyle(item.isCorrect ? Color.dobiSuccess : Color(hex: 0xEF4444))
                                }
                                Text(item.questionText)
                                    .font(.system(size: 13, weight: .semibold))
                                    .foregroundStyle(Color.dobiTextPrimary)
                                    .lineSpacing(3)
                            }
                            if !item.answerSeen.isEmpty {
                                Text(item.answerSeen)
                                    .font(.system(size: 12))
                                    .foregroundStyle(Color.dobiTextSecondary)
                                    .padding(.leading, 38)
                                    .lineSpacing(3)
                            }
                        }
                        .padding(14)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(Color.white)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                    }
                }
                .padding(.horizontal, 16)
                .padding(.top, 14)
                .padding(.bottom, 40)
            }
            .background(Color.dobiPageBg)
        }
        .background(Color.dobiPageBg.ignoresSafeArea())
    }
}
