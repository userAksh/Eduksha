import Foundation

// MARK: - ChildEngagement
// Mirrors Android ChildEngagementRepository output

struct ChildEngagement {
    let childId: String
    let daysActiveThisWeek: Int       // Mon–today, distinct calendar days with any activity
    let isActiveToday: Bool
    let streakDays: Int               // consecutive days with activity ending today/yesterday
    let accuracyPct: Int?             // nil if not enough attempts; pooled across all subjects
    let weakSubject: String?          // subject with lowest accuracy (min 3 attempts)
    let weakSubjectAccuracyPct: Int?  // accuracy for the weak subject specifically
    let lastActivityMs: Int64         // ms-since-epoch of most recent activity; 0 if none
    let weekDayKinds: [DayKind]       // Mon=0 … Sun=6, what each day had

    static let empty = ChildEngagement(
        childId: "",
        daysActiveThisWeek: 0,
        isActiveToday: false,
        streakDays: 0,
        accuracyPct: nil,
        weakSubject: nil,
        weakSubjectAccuracyPct: nil,
        lastActivityMs: 0,
        weekDayKinds: Array(repeating: .none, count: 7)
    )
}

// MARK: - LearningInsight
// Mirrors Android LearningInsightEngine.kt

enum InsightTone { case positive, info, amber, orange, softRed }

struct LearningInsight {
    let priority: Int
    let title: String
    let body: String
    let helpHint: String?
    let tone: InsightTone

    init(priority: Int, title: String, body: String, helpHint: String? = nil, tone: InsightTone) {
        self.priority = priority
        self.title = title
        self.body = body
        self.helpHint = helpHint
        self.tone = tone
    }

    static let placeholder = LearningInsight(
        priority: 0,
        title: "Building this week's picture",
        body: "As uploads and quizzes come in, insights will appear here.",
        tone: .info
    )
}

enum DayKind {
    case revision    // has uploads only
    case homework    // has quiz attempts only
    case both        // has uploads + quiz attempts
    case none
}

// MARK: - ParentTaskItem
// Mirrors Android ParentTask domain model. Sourced from parentTaskRequests collection.

struct ParentTaskItem {
    let id: String
    let childId: String
    let subject: String
    let displayName: String       // unitName if set, else subject
    let subjectIcon: String       // emoji e.g. "📚"
    let type: String              // REVISE_CHAPTER / WRITE_QA / OUR_QUESTIONS / CUSTOM
    let status: String            // REQUESTED / ASSIGNED / PENDING_REVIEW / REJECTED / COMPLETED
    let isUnitScope: Bool         // true = lesson/chapter scope, false = concept scope
    let mcqCount: Int             // number of questions for OUR_QUESTIONS type
    let assignedAtMillis: Int64
    let requestedAtMillis: Int64
    let reminderSentAtMillis: Int64
    let submittedAtMillis: Int64   // set when child submits (status → PENDING_REVIEW)
    let reviewedAtMillis: Int64    // set when parent accepts/rejects
}

// MARK: - McqQuestion

struct McqQuestion: Identifiable {
    let id: String
    let text: String
    let options: [String]
    let correctIndex: Int
    let explanation: String

    init(id: String = UUID().uuidString, text: String, options: [String],
         correctIndex: Int, explanation: String = "") {
        self.id = id; self.text = text; self.options = options
        self.correctIndex = correctIndex; self.explanation = explanation
    }
}

// MARK: - ParentTaskFull
// Full task document fetched for ParentTaskDetailView.

struct ParentTaskFull {
    let id: String
    let childId: String
    let subject: String
    let displayName: String
    let subjectIcon: String
    let type: String
    let status: String
    let isUnitScope: Bool
    let unitId: String
    let conceptId: String
    let mcqCount: Int
    let customNote: String
    let parentReviewNote: String
    let assignedSubjectiveQs: [ParentTaskQuestion]
    let submissionPhotoUrls: [String]
    let submissionScore: Int?
    let submissionTotal: Int?
}

struct ParentTaskQuestion: Identifiable {
    let id: String
    let text: String
}
