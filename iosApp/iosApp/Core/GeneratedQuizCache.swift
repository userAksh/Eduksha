import Foundation

@MainActor
final class GeneratedQuizCache {
    static let shared = GeneratedQuizCache()
    private var store: [String: [McqQuestion]] = [:]

    func put(_ key: String, _ questions: [McqQuestion]) { store[key] = questions }
    func get(_ key: String) -> [McqQuestion]? { store[key] }
    func remove(_ key: String) { store.removeValue(forKey: key) }
}
