import Foundation
import FirebaseFirestore

@MainActor
final class StreakHomeViewModel: ObservableObject {
    let childId: String
    let childName: String

    @Published var streakDays: Int = 0
    @Published var weekActivity: [Bool] = Array(repeating: false, count: 7)  // Mon … Sun
    @Published var totalActiveDays: Int = 0
    @Published var bestStreak: Int = 0
    @Published var totalUploads: Int = 0
    @Published var isActiveToday: Bool = false

    private var listener: ListenerRegistration?
    private let db = Firestore.firestore()

    init(childId: String, childName: String) {
        self.childId = childId
        self.childName = childName
        startListening()
    }

    deinit { listener?.remove() }

    private func startListening() {
        listener = db.collection("uploads")
            .whereField("childId", isEqualTo: childId)
            .addSnapshotListener { [weak self] snap, _ in
                guard let self else { return }
                let timestamps: [Int64] = snap?.documents.compactMap {
                    $0.data()["timestamp"] as? Int64
                } ?? []
                self.compute(from: timestamps)
            }
    }

    private func compute(from timestamps: [Int64]) {
        let nowMs = Int64(Date().timeIntervalSince1970 * 1000)
        let epochDays = Set(timestamps.filter { $0 > 0 }.map { epochDay(from: $0) })

        streakDays      = currentStreak(from: epochDays)
        bestStreak      = historicalBestStreak(from: epochDays)
        totalActiveDays = epochDays.count
        totalUploads    = timestamps.count
        weekActivity    = weekActivityMap(from: timestamps, nowMs: nowMs)
        isActiveToday   = epochDays.contains(epochDay(from: nowMs))
    }

    private func epochDay(from ms: Int64) -> Int {
        Int(ms / 86_400_000)
    }

    private func currentStreak(from days: Set<Int>) -> Int {
        if days.isEmpty { return 0 }
        let today = epochDay(from: Int64(Date().timeIntervalSince1970 * 1000))
        // Allow streak if today has no activity but yesterday does (matches Android logic)
        var d = days.contains(today) ? today : (days.contains(today - 1) ? today - 1 : today)
        var streak = 0
        while days.contains(d) { streak += 1; d -= 1 }
        return streak
    }

    private func historicalBestStreak(from days: Set<Int>) -> Int {
        if days.isEmpty { return 0 }
        let sorted = days.sorted()
        var best = 1; var current = 1
        for i in 1..<sorted.count {
            current = sorted[i] == sorted[i - 1] + 1 ? current + 1 : 1
            best = max(best, current)
        }
        return best
    }

    private func weekActivityMap(from timestamps: [Int64], nowMs: Int64) -> [Bool] {
        let cal = Calendar.current
        let today = Date(timeIntervalSince1970: Double(nowMs) / 1000)
        // Calendar.weekday: Sun=1, Mon=2 … Sat=7 → days since Monday: Mon=0 … Sun=6
        let daysFromMonday = (cal.component(.weekday, from: today) + 5) % 7
        guard let monday = cal.date(byAdding: .day, value: -daysFromMonday, to: today) else {
            return Array(repeating: false, count: 7)
        }
        return (0..<7).map { offset in
            guard let day = cal.date(byAdding: .day, value: offset, to: monday) else { return false }
            let dayStart = Int64(cal.startOfDay(for: day).timeIntervalSince1970 * 1000)
            let dayEnd   = dayStart + 86_400_000
            return timestamps.contains { $0 >= dayStart && $0 < dayEnd }
        }
    }
}
