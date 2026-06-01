import SwiftUI

// MARK: - Today's Learning

enum TodayNavTarget {
    case quizResult(unitId: String, conceptId: String)
    case subjective(resultId: String)
    case parentTask(taskId: String)
    case uploadGroup(groupKey: String)
}

struct TodayLearningEntry: Identifiable {
    let id: String
    let kind: TodayKind
    let title: String
    let subject: String
    let timeLabel: String
    var status: TodayStatus = .pending
    var scoreLabel: String? = nil
    var assignedByParent: Bool = false
    var timestamp: Int64 = 0
    var navTarget: TodayNavTarget? = nil
}

enum TodayKind { case homework, task, quiz }
enum TodayStatus { case completed, pending }

// MARK: - Revision Hub

struct RevisionMarkItem: Identifiable {
    let id: String
    let childId: String
    let scope: String   // "unit" or "concept"
    let targetId: String
    let name: String
    let subject: String
    let subjectColor: String
    let subjectIcon: String
    let unitName: String
    let markedAt: Int64

    var isUnit: Bool { scope == "unit" }

    var secondaryLabel: String {
        if !isUnit && !unitName.isEmpty {
            return subject.isEmpty ? unitName : "\(subject) • \(unitName)"
        }
        return subject.isEmpty ? "Topic" : subject
    }

    var parsedColor: Color {
        let hex = subjectColor.trimmingCharacters(in: .init(charactersIn: "#"))
        guard !hex.isEmpty, let val = UInt32(hex, radix: 16) else { return Color(hex: 0x94A3B8) }
        return Color(hex: val)
    }
}

// MARK: - Do You Know

struct CurrentAffairItem: Identifiable {
    let id: String
    let headline: String
    let category: String
    let timeAgo: String
    let emoji: String
    let body: String
}

// MARK: - Upload Item (Revisit Past Topics)

struct UploadItem: Identifiable {
    let id: String
    let childId: String
    let kind: String
    let subject: String
    let subjectColor: String
    let subjectIcon: String
    let unitId: String
    let unitName: String
    let conceptId: String
    let conceptName: String
    let topic: String
    let photoUrls: [String]
    let timestamp: Int64

    var displayTitle: String {
        if !topic.isEmpty { return topic }
        if !conceptName.isEmpty { return conceptName }
        if !unitName.isEmpty { return unitName }
        return subject.isEmpty ? "Uploaded Work" : subject
    }

    var kindLabel: String {
        switch kind {
        case "class_work":            return "Class Work"
        case "self_learning":         return "Self Learning"
        case "homework":              return "Homework"
        case "tuition_work_learning": return "Tuition Class"
        case "tuition_work_written":  return "Tuition Written"
        case "tuition_work_test":     return "Tuition Test"
        case "tuition_hw_learning":   return "Tuition HW"
        case "tuition_hw_written":    return "Tuition HW"
        default:                      return "Upload"
        }
    }

    var kindColorHex: UInt32 {
        switch kind {
        case "class_work":            return 0x3B82F6
        case "self_learning":         return 0x8B5CF6
        case "homework":              return 0xF97316
        case "tuition_work_learning": return 0x22C55E
        case "tuition_work_written":  return 0x0EA5E9
        case "tuition_work_test":     return 0xEF4444
        case "tuition_hw_learning":   return 0xF59E0B
        case "tuition_hw_written":    return 0xF59E0B
        default:                      return 0x94A3B8
        }
    }

    var isTuition: Bool { kind.hasPrefix("tuition_") }

    var groupKey: String {
        if !topic.isEmpty { return "tuition:\(subject.lowercased()):\(topic.lowercased())" }
        if !conceptId.isEmpty { return "concept:\(conceptId)" }
        if !unitId.isEmpty { return "unit:\(unitId)" }
        return "unknown:\(id)"
    }

    var parsedSubjectColor: Color {
        let hex = subjectColor.trimmingCharacters(in: .init(charactersIn: "#"))
        guard !hex.isEmpty, let val = UInt32(hex, radix: 16) else { return Color(hex: 0x94A3B8) }
        return Color(hex: val)
    }
}

// MARK: - Helpers

func relativeTimeLabel(timestamp: Int64, now: Int64) -> String {
    let diff = max(0, now - timestamp)
    let mins = diff / 60_000
    let hrs  = diff / 3_600_000
    switch true {
    case diff < 60_000:   return "Just now"
    case mins < 60:       return "\(mins) min ago"
    case hrs < 24:        return "\(hrs) hr ago"
    default:              return "\(hrs / 24) d ago"
    }
}

func startOfTodayMillis() -> Int64 {
    let cal = Calendar.current
    let start = cal.startOfDay(for: Date())
    return Int64(start.timeIntervalSince1970 * 1000)
}
