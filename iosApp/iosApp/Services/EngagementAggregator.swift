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
        // Remove any previous listeners for this child
        listeners.forEach { $0.remove() }
        listeners = []

        let calendar = Calendar.current
        let today = calendar.startOfDay(for: Date())
        guard let monday = calendar.date(from: calendar.dateComponents(
            [.yearForWeekOfYear, .weekOfYear], from: today)) else { return }

        // We read all three collections and recompute whenever any of them changes.
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

        let uploadsRef = db.collection("uploads")
            .whereField("childId", isEqualTo: childId)
        let l1 = uploadsRef.addSnapshotListener { snap, _ in
            uploadsData = snap?.documents ?? []
            recompute()
        }

        let subjectiveRef = db.collection("subjective_results")
            .whereField("childId", isEqualTo: childId)
        let l2 = subjectiveRef.addSnapshotListener { snap, _ in
            subjectiveData = snap?.documents ?? []
            recompute()
        }

        let quizUnitsRef = db.collection("quiz_attempts")
            .document(childId)
            .collection("units")
        let l3 = quizUnitsRef.addSnapshotListener { snap, _ in
            quizUnitsData = snap?.documents ?? []
            recompute()
        }

        let quizConceptsRef = db.collection("quiz_attempts")
            .document(childId)
            .collection("concepts")
        let l4 = quizConceptsRef.addSnapshotListener { snap, _ in
            quizConceptsData = snap?.documents ?? []
            recompute()
        }

        listeners = [l1, l2, l3, l4]
    }

    func stop() {
        listeners.forEach { $0.remove() }
        listeners = []
    }
}

// MARK: - Computation (pure, off MainActor-safe via value types)

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

    // ── 1. Collect active days this week (Mon → today) from uploads ──────────
    let uploadDays: Set<Date> = Set(uploads.compactMap { doc -> Date? in
        guard let ts = doc["timestamp"] as? Timestamp else { return nil }
        let d = calendar.startOfDay(for: ts.dateValue())
        return d >= monday && d <= today ? d : nil
    })

    // Days with quiz activity this week
    let quizDays: Set<Date> = Set((quizUnits + quizConcepts).compactMap { doc -> Date? in
        guard let ts = doc["timestamp"] as? Timestamp else { return nil }
        let d = calendar.startOfDay(for: ts.dateValue())
        return d >= monday && d <= today ? d : nil
    })

    let allActiveDays = uploadDays.union(quizDays)
    let daysActiveThisWeek = allActiveDays.count
    let isActiveToday = allActiveDays.contains(today)

    // ── 2. Streak: consecutive days ending today (or yesterday if not today) ──
    var streakDays = 0
    var checkDay = today
    while true {
        let isActive = allActiveDays.contains(checkDay) ||
            // also check historical (before monday window) — need all upload days
            uploads.contains(where: {
                guard let ts = $0["timestamp"] as? Timestamp else { return false }
                return calendar.startOfDay(for: ts.dateValue()) == checkDay
            }) ||
            (quizUnits + quizConcepts).contains(where: {
                guard let ts = $0["timestamp"] as? Timestamp else { return false }
                return calendar.startOfDay(for: ts.dateValue()) == checkDay
            })
        if !isActive { break }
        streakDays += 1
        guard let prev = calendar.date(byAdding: .day, value: -1, to: checkDay) else { break }
        checkDay = prev
    }

    // ── 3. Accuracy from quiz_attempts/units + /concepts ─────────────────────
    struct SubjectStats { var correct = 0; var total = 0 }
    var subjectMap: [String: SubjectStats] = [:]

    for doc in quizUnits + quizConcepts {
        let d = doc.data()
        guard let subject = d["subject"] as? String,
              let correct = d["correctAnswers"] as? Int,
              let total = d["totalQuestions"] as? Int,
              total > 0 else { continue }
        subjectMap[subject, default: SubjectStats()].correct += correct
        subjectMap[subject, default: SubjectStats()].total += total
    }

    let allCorrect = subjectMap.values.reduce(0) { $0 + $1.correct }
    let allTotal = subjectMap.values.reduce(0) { $0 + $1.total }
    let accuracyPct: Int? = allTotal > 0 ? Int(round(Double(allCorrect) / Double(allTotal) * 100)) : nil

    let weakEntry: (key: String, value: SubjectStats)? = subjectMap
        .filter { $0.value.total >= MIN_ATTEMPTS_FOR_WEAK_SUBJECT }
        .min { a, b in
            let aAcc = Double(a.value.correct) / Double(a.value.total)
            let bAcc = Double(b.value.correct) / Double(b.value.total)
            return aAcc < bAcc
        }
    let weakSubject: String? = weakEntry?.key
    let weakSubjectAccuracyPct: Int? = weakEntry.map { entry in
        Int(round(Double(entry.value.correct) / Double(entry.value.total) * 100))
    }

    // ── 4. Last activity timestamp ──────────────────────────────────────────────
    let allTimestamps: [Int64] = (uploads.compactMap { doc -> Int64? in
        guard let ts = doc["timestamp"] as? Timestamp else { return nil }
        return Int64(ts.seconds * 1000)
    }) + (subjective.compactMap { doc -> Int64? in
        guard let ts = doc["timestamp"] as? Timestamp else { return nil }
        return Int64(ts.seconds * 1000)
    }) + ((quizUnits + quizConcepts).compactMap { doc -> Int64? in
        guard let ts = doc["timestamp"] as? Timestamp else { return nil }
        return Int64(ts.seconds * 1000)
    })
    let lastActivityMs: Int64 = allTimestamps.max() ?? 0

    // ── 5. Per-day DayKind for the week (Mon=0 … Sun=6) ─────────────────────
    var weekDayKinds: [DayKind] = Array(repeating: .none, count: 7)
    for i in 0..<7 {
        guard let day = calendar.date(byAdding: .day, value: i, to: monday) else { continue }
        if day > today { break }
        let hasUpload = uploadDays.contains(day)
        let hasQuiz = quizDays.contains(day)
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
