import Foundation
import FirebaseFirestore

// MARK: - EngagementAggregator
// Mirrors Android ChildEngagementRepository.
// Reads uploads, subjective_results, quiz_attempts to compute ChildEngagement.

private let MIN_ATTEMPTS_FOR_WEAK_SUBJECT = 3

@MainActor
final class EngagementAggregator {
    private let db = Firestore.firestore()
    private var listeners: [ListenerRegistration] = []

    deinit {
        listeners.forEach { $0.remove() }
    }

    func listen(childId: String, onChange: @escaping (ChildEngagement) -> Void) {
        listeners.forEach { $0.remove() }
        listeners = []

        let calendar = Calendar.current
        let today = calendar.startOfDay(for: Date())
        // Match Android's mondayOfThisWeekStart() — explicit (weekday+5)%7 so
        // we always land on Monday regardless of the locale's firstWeekday.
        let weekdayComponent = calendar.component(.weekday, from: today) // 1=Sun…7=Sat
        let offsetFromMon = (weekdayComponent + 5) % 7
        guard let monday = calendar.date(byAdding: .day, value: -offsetFromMon, to: today) else { return }

        var uploadsData: [QueryDocumentSnapshot] = []
        var subjectiveData: [QueryDocumentSnapshot] = []
        var quizUnitsData: [QueryDocumentSnapshot] = []
        var quizConceptsData: [QueryDocumentSnapshot] = []

        func recompute() {
            let result = compute(
                childId: childId,
                uploads: uploadsData,
                subjective: subjectiveData,
                quizUnits: quizUnitsData,
                quizConcepts: quizConceptsData,
                monday: monday,
                today: today,
                calendar: calendar
            )
            onChange(result)
        }

        let l1 = db.collection("uploads")
            .whereField("childId", isEqualTo: childId)
            .addSnapshotListener { snap, _ in uploadsData = snap?.documents ?? []; recompute() }

        let l2 = db.collection("subjective_results")
            .whereField("childId", isEqualTo: childId)
            .addSnapshotListener { snap, _ in subjectiveData = snap?.documents ?? []; recompute() }

        let l3 = db.collection("quiz_attempts").document(childId).collection("units")
            .addSnapshotListener { snap, _ in quizUnitsData = snap?.documents ?? []; recompute() }

        let l4 = db.collection("quiz_attempts").document(childId).collection("concepts")
            .addSnapshotListener { snap, _ in quizConceptsData = snap?.documents ?? []; recompute() }

        listeners = [l1, l2, l3, l4]
    }

    func stop() {
        listeners.forEach { $0.remove() }
        listeners = []
    }
}

// MARK: - Timestamp helpers

// Android stores timestamps as epoch-millisecond Longs, not Firestore Timestamps.
// Quiz attempts use "lastUpdated"; uploads + subjective_results use "timestamp".
private func epochMs(_ data: [String: Any], key: String) -> Int64? {
    let v = data[key]
    if let ts = v as? Timestamp { return Int64(ts.seconds * 1000) + Int64(ts.nanoseconds / 1_000_000) }
    if let n = v as? Int64  { return n }
    if let n = v as? Int    { return Int64(n) }
    if let n = v as? Double { return n > 0 ? Int64(n) : nil }
    return nil
}

private func startOfDay(_ ms: Int64, calendar: Calendar) -> Date {
    calendar.startOfDay(for: Date(timeIntervalSince1970: Double(ms) / 1000.0))
}

// MARK: - Computation

private func compute(
    childId: String,
    uploads: [QueryDocumentSnapshot],
    subjective: [QueryDocumentSnapshot],
    quizUnits: [QueryDocumentSnapshot],
    quizConcepts: [QueryDocumentSnapshot],
    monday: Date,
    today: Date,
    calendar: Calendar
) -> ChildEngagement {

    // ── 1. Active days this week from uploads (field: "timestamp", Long ms) ─────
    let uploadDays: Set<Date> = Set(uploads.compactMap { doc -> Date? in
        guard let ms = epochMs(doc.data(), key: "timestamp"), ms > 0 else { return nil }
        let d = startOfDay(ms, calendar: calendar)
        return d >= monday && d <= today ? d : nil
    })

    // Active days this week from quiz attempts (field: "lastUpdated", Long ms)
    let quizDays: Set<Date> = Set((quizUnits + quizConcepts).compactMap { doc -> Date? in
        guard let ms = epochMs(doc.data(), key: "lastUpdated"), ms > 0 else { return nil }
        let d = startOfDay(ms, calendar: calendar)
        return d >= monday && d <= today ? d : nil
    })

    let allActiveDays = uploadDays.union(quizDays)
    let daysActiveThisWeek = allActiveDays.count
    let isActiveToday = allActiveDays.contains(today)

    // ── 2. Streak: consecutive days ending today or yesterday (mirrors Android) ─
    // Android: var d = if (today !in days && (today-1) in days) today-1 else today
    func hasActivity(on day: Date) -> Bool {
        if allActiveDays.contains(day) { return true }
        // Check historical data (before this week's window)
        if uploads.contains(where: { doc in
            guard let ms = epochMs(doc.data(), key: "timestamp"), ms > 0 else { return false }
            return startOfDay(ms, calendar: calendar) == day
        }) { return true }
        if (quizUnits + quizConcepts).contains(where: { doc in
            guard let ms = epochMs(doc.data(), key: "lastUpdated"), ms > 0 else { return false }
            return startOfDay(ms, calendar: calendar) == day
        }) { return true }
        return false
    }

    var startDay = today
    if !hasActivity(on: today),
       let yesterday = calendar.date(byAdding: .day, value: -1, to: today),
       hasActivity(on: yesterday) {
        startDay = yesterday
    }

    var streakDays = 0
    var checkDay = startDay
    while hasActivity(on: checkDay) {
        streakDays += 1
        guard let prev = calendar.date(byAdding: .day, value: -1, to: checkDay) else { break }
        checkDay = prev
    }

    // ── 3. Accuracy ──────────────────────────────────────────────────────────────
    // Mirrors Android buildEngagement exactly:
    //   Overall accuracy  → sum ALL rows, NO subject filter (matches Android's
    //                        `totalCorrect = subjectives.sumOf + quizzes.sumOf`)
    //   Weak subject      → separately group by subject with isNotBlank() + MIN_ATTEMPTS
    //
    // Android field names: quiz_attempts → "lastCorrect"/"lastTotal"
    //                      subjective_results → "correctCount"/"attemptedCount"

    func intField(_ d: [String: Any], _ key: String) -> Int {
        d[key] as? Int ?? Int((d[key] as? Double) ?? 0)
    }

    // Overall accuracy — no subject filter
    var allCorrectRaw = 0
    var allTotalRaw   = 0
    for doc in quizUnits + quizConcepts {
        let d = doc.data()
        allCorrectRaw += intField(d, "lastCorrect")
        allTotalRaw   += intField(d, "lastTotal")
    }
    for doc in subjective {
        let d = doc.data()
        allCorrectRaw += intField(d, "correctCount")
        allTotalRaw   += intField(d, "attemptedCount")
    }
    // Android uses `.toInt()` (truncation, not rounding) — match exactly
    let accuracyPct: Int? = allTotalRaw > 0
        ? max(0, min(100, Int(Double(allCorrectRaw) / Double(allTotalRaw) * 100)))
        : nil

    // Per-subject breakdown — subject filter + MIN_ATTEMPTS threshold (weak subject only)
    struct SubjectStats { var correct = 0; var total = 0 }
    var subjectMap: [String: SubjectStats] = [:]
    for doc in quizUnits + quizConcepts {
        let d = doc.data()
        guard let subject = d["subject"] as? String, !subject.isEmpty else { continue }
        let correct = intField(d, "lastCorrect")
        let total   = intField(d, "lastTotal")
        guard total > 0 else { continue }
        subjectMap[subject, default: SubjectStats()].correct += correct
        subjectMap[subject, default: SubjectStats()].total   += total
    }
    for doc in subjective {
        let d = doc.data()
        guard let subject = d["subject"] as? String, !subject.isEmpty else { continue }
        let correct = intField(d, "correctCount")
        let total   = intField(d, "attemptedCount")
        guard total > 0 else { continue }
        subjectMap[subject, default: SubjectStats()].correct += correct
        subjectMap[subject, default: SubjectStats()].total   += total
    }

    let weakEntry = subjectMap
        .filter { $0.value.total >= MIN_ATTEMPTS_FOR_WEAK_SUBJECT }
        .min { Double($0.value.correct) / Double($0.value.total) < Double($1.value.correct) / Double($1.value.total) }
    let weakSubject: String? = weakEntry?.key
    let weakSubjectAccuracyPct: Int? = weakEntry.map {
        max(0, min(100, Int(Double($0.value.correct) / Double($0.value.total) * 100)))
    }

    // ── 4. Last activity timestamp ─────────────────────────────────────────────
    let allMs: [Int64] =
        uploads.compactMap { epochMs($0.data(), key: "timestamp") } +
        subjective.compactMap { epochMs($0.data(), key: "timestamp") } +
        (quizUnits + quizConcepts).compactMap { epochMs($0.data(), key: "lastUpdated") }
    let lastActivityMs: Int64 = allMs.max() ?? 0

    // ── 5. Per-day DayKind for the week (Mon=0 … Sun=6) ─────────────────────────
    var weekDayKinds: [DayKind] = Array(repeating: .none, count: 7)
    for i in 0..<7 {
        guard let day = calendar.date(byAdding: .day, value: i, to: monday) else { continue }
        if day > today { break }
        let hasUpload = uploadDays.contains(day)
        let hasQuiz   = quizDays.contains(day)
        switch (hasUpload, hasQuiz) {
        case (true, true):   weekDayKinds[i] = .both
        case (true, false):  weekDayKinds[i] = .revision
        case (false, true):  weekDayKinds[i] = .homework
        default:             weekDayKinds[i] = .none
        }
    }

    return ChildEngagement(
        childId: childId,
        daysActiveThisWeek: daysActiveThisWeek,
        isActiveToday: isActiveToday,
        streakDays: streakDays,
        accuracyPct: accuracyPct,
        weakSubject: weakSubject,
        weakSubjectAccuracyPct: weakSubjectAccuracyPct,
        lastActivityMs: lastActivityMs,
        weekDayKinds: weekDayKinds
    )
}
