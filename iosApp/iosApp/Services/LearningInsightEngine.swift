import Foundation

// MARK: - LearningInsightEngine
// Swift port of Android LearningInsightEngine.kt
// Surfaces the single highest-priority insight from a 7-rule set.

private let DAY_MS: Int64 = 24 * 60 * 60 * 1000

func computeLearningInsight(
    items: [ActivityLogItem],
    now: Int64 = Int64(Date().timeIntervalSince1970 * 1000)
) -> LearningInsight {
    let currentWeek = bucketWeek(items: items, nowMs: now)
    let priorWeek   = bucketWeek(items: items, nowMs: now - 7 * DAY_MS)

    if let i = revisionGapInsight(items: items, now: now)                        { return i }
    if let i = subjectNeglectInsight(items: items, now: now)                     { return i }
    if let i = homeworkWithoutRevisionInsight(items: items, now: now)            { return i }
    if let i = consistencyDropInsight(currentWeek: currentWeek, priorWeek: priorWeek) { return i }
    if let i = longInactivityInsight(items: items, now: now)                     { return i }
    if let i = balancedDaysInsight(currentWeek: currentWeek)                     { return i }
    if let i = positiveReinforcementInsight(currentWeek: currentWeek)            { return i }

    return LearningInsight(
        priority: 0,
        title: "Keep building daily habits",
        body: "Even short sessions help with retention.",
        tone: .info
    )
}

// MARK: P1 — Revision gap (most urgent)

private func revisionGapInsight(items: [ActivityLogItem], now: Int64) -> LearningInsight? {
    let revisions = items.filter { isRevisionItem($0) }
    if revisions.isEmpty { return nil }

    let lookback30d = now - 30 * DAY_MS

    // Group by topic, keep only groups with ≥1 item in the 30-day window
    var byTopic: [String: [ActivityLogItem]] = [:]
    for item in revisions {
        byTopic[topicKey(item), default: []].append(item)
    }
    byTopic = byTopic.filter { pair in pair.value.contains { $0.timestamp >= lookback30d } }
    if byTopic.isEmpty { return nil }

    // For each topic: find the most recent item, compute staleness
    // Tie-break by raw timestamp (older last-revision wins) for determinism
    struct Candidate { var item: ActivityLogItem; var days: Int }
    var candidates: [Candidate] = []
    for pair in byTopic {
        guard let latest = pair.value.max(by: { $0.timestamp < $1.timestamp }) else { continue }
        let d = Int((now - latest.timestamp) / DAY_MS)
        if d > 5 { candidates.append(Candidate(item: latest, days: d)) }
    }
    guard let best = candidates.max(by: { a, b in
        a.days != b.days ? a.days < b.days : a.item.timestamp > b.item.timestamp
    }) else { return nil }
    let stalest = best.item
    let stalestDays = best.days

    let label = displayTopicLabel(stalest)
    let tone: InsightTone = stalestDays > 12 ? .softRed : stalestDays >= 8 ? .orange : .amber
    return LearningInsight(
        priority: 1,
        title: "\(label) may need revision",
        body: "Last revised \(stalestDays) days ago. Regular revision improves retention.",
        helpHint: "A short recap can bring it back.",
        tone: tone
    )
}

// MARK: P2 — Subject neglect

private func subjectNeglectInsight(items: [ActivityLogItem], now: Int64) -> LearningInsight? {
    let withSubject = items.filter { !$0.subject.isEmpty }
    if withSubject.isEmpty { return nil }

    let lookback60d = now - 60 * DAY_MS

    // Group by subject, keep only subjects active in the 60d window
    var bySubject: [String: [ActivityLogItem]] = [:]
    for item in withSubject {
        bySubject[item.subject, default: []].append(item)
    }
    bySubject = bySubject.filter { pair in pair.value.contains { $0.timestamp >= lookback60d } }

    var neglectedSubject: String? = nil
    var neglectedDays = 0
    for pair in bySubject {
        let latest = pair.value.map { $0.timestamp }.max() ?? 0
        let d = Int((now - latest) / DAY_MS)
        if d > 7 && d > neglectedDays {
            neglectedDays = d
            neglectedSubject = pair.key
        }
    }
    guard let subject = neglectedSubject else { return nil }

    return LearningInsight(
        priority: 2,
        title: "\(subject) needs attention",
        body: "No revision activity for \(neglectedDays) days.",
        helpHint: "Even 10 minutes today can rebuild momentum.",
        tone: neglectedDays > 14 ? .orange : .amber
    )
}

// MARK: P3 — Homework without revision

private func homeworkWithoutRevisionInsight(items: [ActivityLogItem], now: Int64) -> LearningInsight? {
    let cutoff = now - 3 * DAY_MS
    let homeworkRecent = items.contains { isHomeworkItem($0) && $0.timestamp >= cutoff }
    let revisionRecent = items.contains { isRevisionItem($0) && $0.timestamp >= cutoff }
    if !homeworkRecent || revisionRecent { return nil }

    return LearningInsight(
        priority: 3,
        title: "Homework is regular, but revision is low",
        body: "Concepts are being completed, but not revisited.",
        helpHint: "Add a short revision to balance learning.",
        tone: .amber
    )
}

// MARK: P4 — Consistency drop

private func consistencyDropInsight(currentWeek: [DayActivity], priorWeek: [DayActivity]) -> LearningInsight? {
    let current = currentWeek.filter { $0.kind != .none }.count
    let prior   = priorWeek.filter { $0.kind != .none }.count
    if prior < 3 || current + 1 >= prior { return nil }

    let inactiveDays = currentWeek.filter { $0.kind == .none }.count
    let dayWord = inactiveDays == 1 ? "day" : "days"
    return LearningInsight(
        priority: 4,
        title: "Learning consistency dropped this week",
        body: "\(inactiveDays) inactive \(dayWord) detected.",
        helpHint: "A short daily revision can rebuild the rhythm.",
        tone: .amber
    )
}

// MARK: P5 — Long inactivity

private func longInactivityInsight(items: [ActivityLogItem], now: Int64) -> LearningInsight? {
    guard let mostRecent = items.max(by: { $0.timestamp < $1.timestamp }) else {
        return LearningInsight(
            priority: 5,
            title: "No recent learning activity yet",
            body: "Once an upload or quiz is logged, it will show up here.",
            tone: .info
        )
    }
    let daysSince = Int((now - mostRecent.timestamp) / DAY_MS)
    if daysSince <= 4 { return nil }

    return LearningInsight(
        priority: 5,
        title: "No recent learning activity detected",
        body: "\(daysSince) days since the last session.",
        helpHint: "A short revision can help restart momentum.",
        tone: daysSince > 10 ? .orange : .amber
    )
}

// MARK: P7 — Balanced learning days

private func balancedDaysInsight(currentWeek: [DayActivity]) -> LearningInsight? {
    let balanced = currentWeek.filter { $0.kind == .both }.count
    if balanced < 2 { return nil }

    return LearningInsight(
        priority: 7,
        title: "\(balanced) balanced learning days this week",
        body: "Strong mix of learning and revision.",
        tone: .positive
    )
}

// MARK: P6 — Positive reinforcement

private func positiveReinforcementInsight(currentWeek: [DayActivity]) -> LearningInsight? {
    let studyDays    = currentWeek.filter { $0.kind != .none }.count
    let revisionDays = currentWeek.filter { !$0.revisions.isEmpty }.count
    if studyDays < 5 || revisionDays < 2 { return nil }

    return LearningInsight(
        priority: 6,
        title: "Great learning balance this week",
        body: "Homework and revision are both consistent.",
        tone: .positive
    )
}

// MARK: - Helpers

private func isRevisionItem(_ item: ActivityLogItem) -> Bool {
    switch item.source {
    case .quizMcq, .quizSubjective: return true
    case .upload:   return item.category != .homeWork
    case .homework: return false
    }
}

private func isHomeworkItem(_ item: ActivityLogItem) -> Bool {
    switch item.source {
    case .homework: return true
    case .upload:   return item.category == .homeWork
    default:        return false
    }
}

private func topicKey(_ item: ActivityLogItem) -> String {
    switch item.detail {
    case .upload(let groupKey, _):             return "upload:\(groupKey)"
    case .quizMcq(let unitId, let conceptId): return "quiz-mcq:\(unitId):\(conceptId)"
    case .quizSubjective(let resultId):        return "subjective:\(resultId)"
    case .homework(let homeworkId, _):         return "homework:\(homeworkId)"
    }
}

private func displayTopicLabel(_ item: ActivityLogItem) -> String {
    let raw = item.title.trimmingCharacters(in: .whitespaces).isEmpty
        ? item.subject.trimmingCharacters(in: .whitespaces)
        : item.title.trimmingCharacters(in: .whitespaces)
    if raw.isEmpty { return "A topic" }
    // If title contains " — " (em-dash separator), extract the topic part after it
    let afterDash = raw.components(separatedBy: " — ").last?.trimmingCharacters(in: .whitespaces) ?? raw
    let cleaned = afterDash.isEmpty ? raw : afterDash
    return cleaned.count > 32
        ? String(cleaned.prefix(30)).trimmingCharacters(in: .whitespaces) + "…"
        : cleaned
}
