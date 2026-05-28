import SwiftUI

// MARK: - Quiz MCQ result models

struct ADQuizResult: Identifiable {
    var id: String { "\(displayName)_\(correct)_\(total)_\(questions.count)" }
    let displayName: String
    let subject: String
    let correct: Int
    let total: Int
    let questions: [ADMcqQuestion]
    let selections: [Int: Int]   // questionIndex → selectedOptionIndex
}

struct ADMcqQuestion {
    let text: String
    let options: [String]
    let correctIndex: Int
    let explanation: String
}

// MARK: - Subjective result models

struct ADSubjectiveResult {
    let displayName: String
    let subject: String
    let attempted: Int
    let correct: Int
    let accuracyPct: Int
    let timestamp: Int64
    let items: [ADSubjectiveResultItem]
}

struct ADSubjectiveResultItem {
    let questionText: String
    let answerSeen: String
    let strictFeedback: String
    let isAttempted: Bool
    let isCorrect: Bool
}

// MARK: - Week pattern models (mirrors Android WeekPattern.kt)

struct DayItem {
    let title: String
    let subject: String
    let route: ActivityDetailRoute
    let typeTag: String
}

struct DayActivity {
    let revisions: [DayItem]
    let homeworks: [DayItem]

    var kind: DayKind {
        switch (revisions.isEmpty, homeworks.isEmpty) {
        case (true, true):   return .none
        case (true, false):  return .homework
        case (false, true):  return .revision
        default:             return .both
        }
    }

    static let empty = DayActivity(revisions: [], homeworks: [])
}

let emptyWeek: [DayActivity] = Array(repeating: .empty, count: 7)

// Bucket a child's activity log into Mon(0)…Sun(6) for the week containing nowMs.
// Pass nowMs = 0 (default) to use the current time.
func bucketWeek(items: [ActivityLogItem], nowMs: Int64 = 0) -> [DayActivity] {
    let cal = Calendar.current
    let nowDate = nowMs > 0 ? Date(timeIntervalSince1970: Double(nowMs) / 1000.0) : Date()
    let today = cal.startOfDay(for: nowDate)
    let weekday = cal.component(.weekday, from: today)   // 1=Sun…7=Sat
    let offsetFromMon = (weekday + 5) % 7
    guard let monday = cal.date(byAdding: .day, value: -offsetFromMon, to: today) else {
        return emptyWeek
    }
    let msPerDay: Int64 = 24 * 60 * 60 * 1000
    let monMs = Int64(monday.timeIntervalSince1970 * 1000)
    let endMs  = monMs + 7 * msPerDay

    var revisions: [[DayItem]] = Array(repeating: [], count: 7)
    var homeworks: [[DayItem]] = Array(repeating: [], count: 7)

    for item in items {
        guard item.timestamp >= monMs, item.timestamp < endMs else { continue }
        let idx = max(0, min(6, Int((item.timestamp - monMs) / msPerDay)))
        let tag: String
        switch item.source {
        case .quizMcq:        tag = "Quiz"
        case .quizSubjective: tag = "Write Answer"
        case .homework:       tag = "Homework"
        case .upload:
            switch item.uploadKind {
            case "class_work":    tag = "Class Work"
            case "self_learning": tag = "Self Study"
            default:              tag = "Upload"
            }
        }
        let dayItem = DayItem(title: item.title, subject: item.subject, route: item.detail, typeTag: tag)
        if item.source == .homework || (item.source == .upload && item.category == .homeWork) {
            homeworks[idx].append(dayItem)
        } else {
            revisions[idx].append(dayItem)
        }
    }

    return (0..<7).map { DayActivity(revisions: revisions[$0], homeworks: homeworks[$0]) }
}

// MARK: - Activity domain models (shared between view and FirebaseService)

enum ActivityCategory: String {
    case schoolWork   = "school_work"
    case homeWork     = "home_work"
    case selfLearning = "self_learning"

    static func from(key: String) -> ActivityCategory? { ActivityCategory(rawValue: key) }

    var title: String {
        switch self {
        case .schoolWork:   return "School Work"
        case .homeWork:     return "Home Work"
        case .selfLearning: return "Self Learning"
        }
    }
    var icon: String {
        switch self {
        case .schoolWork:   return "book.fill"
        case .homeWork:     return "pencil.and.list.clipboard"
        case .selfLearning: return "book.closed.fill"
        }
    }
    var tint: Color {
        switch self {
        case .schoolWork:   return Color.dobiPrimary
        case .homeWork:     return Color(hex: 0xF59E0B)
        case .selfLearning: return Color.dobiError
        }
    }
    var tintBg: Color {
        switch self {
        case .schoolWork:   return Color.dobiPastelSkyBlue
        case .homeWork:     return Color(hex: 0xFFF8EC)
        case .selfLearning: return Color.dobiPastelPink
        }
    }
}

enum ActivitySource { case upload, quizMcq, quizSubjective, homework }

enum ActivityDetailRoute {
    case upload(groupKey: String, photoUrls: [String])
    case quizMcq(unitId: String, conceptId: String)
    case quizSubjective(resultId: String)
    case homework(homeworkId: String)
}

struct ActivityLogItem: Identifiable {
    let id: String
    let source: ActivitySource
    let category: ActivityCategory
    let title: String
    let subject: String
    let uploadKind: String
    let timestamp: Int64
    let score: (correct: Int, total: Int)?
    let photoUrls: [String]
    let detail: ActivityDetailRoute
}
