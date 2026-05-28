import Foundation
import UIKit
import FirebaseAuth
import FirebaseFirestore

// UIViewController already implements present/dismiss — this makes it usable as Firebase's uiDelegate
extension UIViewController: @retroactive AuthUIDelegate {}

// MARK: - FirebaseService
// Thin wrapper around Firebase Auth + Firestore for the parent auth flow.

@MainActor
final class FirebaseService {
    static let shared = FirebaseService()
    private init() {}

    private let db = Firestore.firestore()

    // MARK: Phone Auth

    func sendOTP(phone: String) async throws -> String {
        #if targetEnvironment(simulator)
        // Firebase 11's PhoneAuthProvider has an internal isMainThread assertion that fires even
        // when called from the main thread, because it internally creates a Task with @isolated(any)
        // that runs on the cooperative pool. There is no call-site fix — it's a Firebase 11 bug.
        // On simulator we bypass phone auth entirely and return a stub verification ID.
        NSLog("[OTP] Simulator: skipping Firebase phone auth, returning stub ID for \(phone)")
        try await Task.sleep(nanoseconds: 400_000_000) // simulate network latency
        return "SIMULATOR_VID_\(phone)"
        #else
        let rootVC = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first?.windows
            .first(where: \.isKeyWindow)?
            .rootViewController
        return try await withCheckedThrowingContinuation { continuation in
            DispatchQueue.main.async {
                PhoneAuthProvider.provider().verifyPhoneNumber(phone, uiDelegate: rootVC) { verificationID, error in
                    if let error {
                        continuation.resume(throwing: error)
                    } else if let verificationID {
                        continuation.resume(returning: verificationID)
                    } else {
                        continuation.resume(throwing: NSError(domain: "PhoneAuth", code: -1,
                            userInfo: [NSLocalizedDescriptionKey: "No verification ID returned"]))
                    }
                }
            }
        }
        #endif
    }

    func verifyOTP(verificationID: String, code: String) async throws -> String {
        let credential = PhoneAuthProvider.provider()
            .credential(withVerificationID: verificationID, verificationCode: code)
        let result = try await Auth.auth().signIn(with: credential)
        return result.user.uid
    }

    func signOut() throws {
        try Auth.auth().signOut()
    }

    var currentUID: String? { Auth.auth().currentUser?.uid }

    func addAuthStateListener(_ handler: @escaping (String?) -> Void) -> AuthStateDidChangeListenerHandle {
        Auth.auth().addStateDidChangeListener { _, user in
            handler(user?.uid)
        }
    }

    func removeAuthStateListener(_ handle: AuthStateDidChangeListenerHandle) {
        Auth.auth().removeStateDidChangeListener(handle)
    }

    // MARK: Parent Profile

    func saveParentProfile(uid: String, name: String, phone: String) async throws {
        try await db.collection("parents").document(uid).setData([
            "name": name,
            "phone": phone,
            "updatedAt": FieldValue.serverTimestamp()
        ], merge: true)
    }

    func findParentUID(byPhone phone: String) async throws -> String? {
        let e164 = phone.hasPrefix("+") ? phone : "+91" + phone
        let digits = e164.replacingOccurrences(of: "+91", with: "")
        NSLog("[UID] Looking up parent for phone=\(phone), e164=\(e164), digits=\(digits)")
        for p in [e164, digits] {
            let snap = try await db.collection("parents")
                .whereField("phone", isEqualTo: p)
                .limit(to: 1)
                .getDocuments()
            NSLog("[UID] Query for phone=\(p) → \(snap.documents.count) docs")
            if let doc = snap.documents.first {
                NSLog("[UID] Found UID=\(doc.documentID), data=\(doc.data())")
                return doc.documentID
            }
        }
        // Nothing found — dump first 5 parent docs to see actual phone format
        let allSnap = try await db.collection("parents").limit(to: 5).getDocuments()
        NSLog("[UID] No match. Sample parents: \(allSnap.documents.map { "\($0.documentID): \($0.data()["phone"] ?? "nil")" })")
        return nil
    }

    func getParentProfile(uid: String) async throws -> (name: String, phone: String)? {
        let doc = try await db.collection("parents").document(uid).getDocument()
        guard doc.exists,
              let name = doc["name"] as? String,
              let phone = doc["phone"] as? String else { return nil }
        return (name, phone)
    }

    // MARK: Children

    func fetchChildren(parentUID: String) async throws -> [ChildProfile] {
        let snap = try await db.collection("parents")
            .document(parentUID)
            .collection("children")
            .getDocuments()
        return snap.documents.compactMap { ChildProfile(from: $0) }
    }

    // MARK: Study Goals

    func getConcepts(grade: Int, board: String) async throws -> [StudyGoalsConcept] {
        let snap = try await db.collection("concepts")
            .whereField("grade", isEqualTo: grade)
            .whereField("board", isEqualTo: board)
            .getDocuments()
        return snap.documents.compactMap { StudyGoalsConcept(from: $0) }
    }

    func getActiveParentTask(parentUID: String, childId: String) async throws -> StudyGoalsActiveTask? {
        let activeStatuses = ["REQUESTED", "ASSIGNED", "PENDING_REVIEW", "REJECTED"]
        let snap = try await db.collection("parentTaskRequests")
            .whereField("parentId", isEqualTo: parentUID)
            .getDocuments()
        guard let doc = snap.documents.first(where: {
            let d = $0.data()
            return (d["childId"] as? String) == childId &&
                   activeStatuses.contains(d["status"] as? String ?? "")
        }) else { return nil }
        let d = doc.data()
        return StudyGoalsActiveTask(
            id: doc.documentID,
            subject: d["subject"] as? String ?? "",
            displayName: (d["unitName"] as? String ?? "").isEmpty ? (d["subject"] as? String ?? "Current task") : (d["unitName"] as? String ?? "Current task"),
            status: d["status"] as? String ?? "ASSIGNED",
            assignedAtMillis: (d["assignedAtMillis"] as? Int64) ?? Int64((d["assignedAtMillis"] as? Double) ?? 0),
            reminderSentAtMillis: (d["reminderSentAtMillis"] as? Int64) ?? Int64((d["reminderSentAtMillis"] as? Double) ?? 0)
        )
    }

    func assignParentTask(_ data: [String: Any]) async throws -> String {
        let ref = db.collection("parentTaskRequests").document()
        try await ref.setData(data)
        return ref.documentID
    }

    func sendParentTaskReminder(taskId: String) async throws {
        try await db.collection("parentTaskRequests").document(taskId).updateData([
            "reminderSentAtMillis": Int64(Date().timeIntervalSince1970 * 1000)
        ])
    }

    // Calls Firebase VertexAI REST API (Gemini) using the current user's ID token.
    func generateGeminiContent(prompt: String) async throws -> String {
        #if targetEnvironment(simulator)
        // Firebase Auth currentUser is nil in simulator (simulated login path).
        // Return a sentinel so callers can provide sample data.
        throw NSError(domain: "GeminiSim", code: 0,
                      userInfo: [NSLocalizedDescriptionKey: "simulator"])
        #else
        guard let user = Auth.auth().currentUser else {
            throw NSError(domain: "GeminiAuth", code: -1,
                          userInfo: [NSLocalizedDescriptionKey: "Not authenticated"])
        }
        let idToken = try await user.getIDToken()
        let urlStr = "https://firebasevertexai.googleapis.com/v1beta/projects/dobi-ac085/locations/us-central1/publishers/google/models/gemini-2.0-flash:generateContent"
        var request = URLRequest(url: URL(string: urlStr)!)
        request.httpMethod = "POST"
        request.setValue("Firebase \(idToken)", forHTTPHeaderField: "Authorization")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        let body: [String: Any] = [
            "contents": [["role": "user", "parts": [["text": prompt]]]],
            "generationConfig": ["temperature": 0.7, "maxOutputTokens": 2048]
        ]
        request.httpBody = try JSONSerialization.data(withJSONObject: body)
        let (data, response) = try await URLSession.shared.data(for: request)
        guard let http = response as? HTTPURLResponse, http.statusCode == 200 else {
            let code = (response as? HTTPURLResponse)?.statusCode ?? -1
            let body = String(data: data, encoding: .utf8) ?? ""
            throw NSError(domain: "GeminiHTTP", code: code,
                          userInfo: [NSLocalizedDescriptionKey: "HTTP \(code): \(body)"])
        }
        guard let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let candidates = json["candidates"] as? [[String: Any]],
              let text = (candidates.first?["content"] as? [String: Any])?["parts"] as? [[String: Any]],
              let result = text.first?["text"] as? String else {
            throw NSError(domain: "GeminiParse", code: -1,
                          userInfo: [NSLocalizedDescriptionKey: "Unexpected response shape"])
        }
        return result
        #endif
    }

    func saveChild(
        parentUID: String,
        childId: String?,
        name: String,
        gender: String,
        grade: Int,
        board: String,
        pin: String
    ) async throws -> String {
        #if targetEnvironment(simulator)
        // Firestore writes require auth; simulator has no Firebase Auth session.
        // Return a generated ID without writing — children listener shows real Firestore data.
        NSLog("[SaveChild] Simulator: skipping Firestore write, returning fake ID")
        try await Task.sleep(nanoseconds: 600_000_000)
        return childId ?? UUID().uuidString
        #else
        let coll = db.collection("parents").document(parentUID).collection("children")
        let ref = childId.map { coll.document($0) } ?? coll.document()
        try await ref.setData([
            "name": name,
            "gender": gender,
            "grade": grade,
            "board": board,
            "pin": pin,
            "parentId": parentUID,
            "updatedAt": FieldValue.serverTimestamp()
        ], merge: true)
        return ref.documentID
        #endif
    }

    // MARK: Activity Log

    func fetchActivityItems(childId: String) async -> [ActivityLogItem] {
        var items: [ActivityLogItem] = []

        // 1. uploads — timestamp field is "timestamp" (Long ms), title from topic/conceptName/unitName
        if let snap = try? await db.collection("uploads")
            .whereField("childId", isEqualTo: childId)
            .getDocuments() {
            for doc in snap.documents {
                let d = doc.data()
                let kind = d["kind"] as? String ?? ""
                guard let category = categoryForUploadKind(kind) else { continue }
                let ts = longMillis(d["timestamp"])
                let topic       = d["topic"]       as? String ?? ""
                let conceptName = d["conceptName"] as? String ?? ""
                let unitName    = d["unitName"]    as? String ?? ""
                let title = topic.isEmpty
                    ? (conceptName.isEmpty ? (unitName.isEmpty ? "Untitled upload" : unitName) : conceptName)
                    : topic
                let subject = d["subject"] as? String ?? ""
                let photos  = d["photoUrls"] as? [String] ?? []
                let conceptId = d["conceptId"] as? String ?? ""
                let unitId    = d["unitId"]    as? String ?? ""
                let groupKey: String = {
                    if !topic.isEmpty    { return "tuition:\(subject.lowercased()):\(topic.lowercased())" }
                    if !conceptId.isEmpty { return "concept:\(conceptId)" }
                    if !unitId.isEmpty    { return "unit:\(unitId)" }
                    return "unknown:\(doc.documentID)"
                }()
                items.append(ActivityLogItem(
                    id: doc.documentID, source: .upload,
                    category: category, title: title,
                    subject: subject, uploadKind: kind,
                    timestamp: ts, score: nil, photoUrls: photos,
                    detail: .upload(groupKey: groupKey, photoUrls: photos)
                ))
            }
        }

        // 2. homeworks — timestamp is a Firestore Timestamp, fallback to parsing "date" string
        if let snap = try? await db.collection("homeworks")
            .whereField("childId", isEqualTo: childId)
            .getDocuments() {
            for doc in snap.documents {
                let d = doc.data()
                let ts: Int64
                if let firestoreTs = d["timestamp"] as? Timestamp {
                    ts = firestoreTs.seconds * 1000
                } else {
                    ts = parseIsoDateMillis(d["date"] as? String ?? "")
                }
                let subject     = d["subject"]     as? String ?? ""
                let chapterName = d["chapterName"] as? String ?? ""
                let title = chapterName.isEmpty
                    ? (subject.isEmpty ? "School Homework" : "\(subject) Homework")
                    : chapterName
                let photos = d["photoUrls"] as? [String] ?? []
                items.append(ActivityLogItem(
                    id: doc.documentID, source: .homework,
                    category: .homeWork, title: title,
                    subject: subject, uploadKind: "school_homework",
                    timestamp: ts, score: nil, photoUrls: photos,
                    detail: .homework(homeworkId: doc.documentID)
                ))
            }
        }

        // 3. subjective_results — timestamp is "timestamp" (Long), score from correctCount/attemptedCount
        if let snap = try? await db.collection("subjective_results")
            .whereField("childId", isEqualTo: childId)
            .getDocuments() {
            for doc in snap.documents {
                let d = doc.data()
                let ts      = longMillis(d["timestamp"])
                let subject = d["subject"] as? String ?? ""
                let title   = (d["displayName"] as? String ?? "").isEmpty
                    ? "Write-answer session"
                    : (d["displayName"] as! String)
                let correct = intVal(d["correctCount"])
                let total   = intVal(d["attemptedCount"])
                items.append(ActivityLogItem(
                    id: doc.documentID, source: .quizSubjective,
                    category: .selfLearning, title: title,
                    subject: subject, uploadKind: "",
                    timestamp: ts,
                    score: total > 0 ? (correct, total) : nil,
                    photoUrls: [],
                    detail: .quizSubjective(resultId: doc.documentID)
                ))
            }
        }

        // 4. quiz_attempts/{childId}/units + concepts — skip if attempts == 0,
        //    timestamp is "lastUpdated", score from lastCorrect/lastTotal
        for sub in ["units", "concepts"] {
            if let snap = try? await db.collection("quiz_attempts")
                .document(childId)
                .collection(sub)
                .getDocuments() {
                for doc in snap.documents {
                    let d = doc.data()
                    let attempts = intVal(d["attempts"])
                    guard attempts > 0 else { continue }
                    let ts      = longMillis(d["lastUpdated"])
                    let subject = d["subject"]     as? String ?? ""
                    let title   = (d["displayName"] as? String ?? "").isEmpty ? "Quiz" : (d["displayName"] as! String)
                    let correct = intVal(d["lastCorrect"])
                    let total   = intVal(d["lastTotal"])
                    let unitId    = sub == "units"    ? doc.documentID : ""
                    let conceptId = sub == "concepts" ? doc.documentID : ""
                    items.append(ActivityLogItem(
                        id: "\(sub)_\(doc.documentID)", source: .quizMcq,
                        category: .selfLearning, title: title,
                        subject: subject, uploadKind: "",
                        timestamp: ts,
                        score: total > 0 ? (correct, total) : nil,
                        photoUrls: [],
                        detail: .quizMcq(unitId: unitId, conceptId: conceptId)
                    ))
                }
            }
        }

        return items.sorted { $0.timestamp > $1.timestamp }
    }

    // mirrors Android categoryForUploadKind — returns nil for unknown kinds (item is skipped)
    private func categoryForUploadKind(_ kind: String) -> ActivityCategory? {
        switch kind {
        case "class_work",
             "tuition_work_learning", "tuition_work_written", "tuition_work_test":
            return .schoolWork
        case "tuition_hw_learning", "tuition_hw_written":
            return .homeWork
        case "self_learning", "ai_test_prep":
            return .selfLearning
        default:
            return nil
        }
    }

    // Firestore stores longs as Int64 or Double depending on SDK version
    private func longMillis(_ v: Any?) -> Int64 {
        if let t = v as? Int64  { return t }
        if let t = v as? Double { return Int64(t) }
        if let t = v as? Timestamp { return t.seconds * 1000 }
        return 0
    }

    private func intVal(_ v: Any?) -> Int {
        if let n = v as? Int   { return n }
        if let n = v as? Int64 { return Int(n) }
        if let n = v as? Double { return Int(n) }
        return 0
    }

    func fetchQuizResult(childId: String, unitId: String, conceptId: String) async -> ADQuizResult? {
        let isConceptScope = !conceptId.isEmpty
        let targetId = isConceptScope ? conceptId : unitId
        let sub = isConceptScope ? "concepts" : "units"
        guard let doc = try? await db.collection("quiz_attempts")
            .document(childId)
            .collection(sub)
            .document(targetId)
            .getDocument(), doc.exists else { return nil }
        let d = doc.data() ?? [:]
        let displayName = d["displayName"] as? String ?? "Quiz"
        let subject     = d["subject"]     as? String ?? ""
        let correct     = intVal(d["lastCorrect"])
        let total       = intVal(d["lastTotal"])
        let rawSelStr   = d["selections"]  as? String ?? ""
        let selections  = parseSelectionsStr(rawSelStr)
        let rawQ        = d["lastQuestions"] as? [[String: Any]] ?? []
        let questions: [ADMcqQuestion] = rawQ.compactMap { m in
            guard let text = m["text"] as? String,
                  let opts = m["options"] as? [String],
                  let ci   = m["correctIndex"] as? Int ?? (m["correctIndex"] as? Int64).map(Int.init)
            else { return nil }
            return ADMcqQuestion(
                text: text, options: opts,
                correctIndex: ci,
                explanation: m["explanation"] as? String ?? ""
            )
        }
        return ADQuizResult(displayName: displayName, subject: subject,
                            correct: correct, total: total,
                            questions: questions, selections: selections)
    }

    private func parseSelectionsStr(_ raw: String) -> [Int: Int] {
        guard !raw.isEmpty else { return [:] }
        var result: [Int: Int] = [:]
        for part in raw.split(separator: ",") {
            let kv = part.split(separator: ":")
            if kv.count == 2, let k = Int(kv[0]), let v = Int(kv[1]) {
                result[k] = v
            }
        }
        return result
    }

    func fetchSubjectiveResult(resultId: String) async -> ADSubjectiveResult? {
        guard let doc = try? await db.collection("subjective_results").document(resultId).getDocument(),
              doc.exists else { return nil }
        let d = doc.data() ?? [:]
        let displayName = d["displayName"] as? String ?? ""
        let subject     = d["subject"]     as? String ?? ""
        let attempted   = intVal(d["attemptedCount"])
        let correct     = intVal(d["correctCount"])
        let accuracy    = intVal(d["accuracyPct"])
        let ts          = longMillis(d["timestamp"])
        let rawItems    = d["items"] as? [[String: Any]] ?? []
        let items: [ADSubjectiveResultItem] = rawItems.map { itm in
            ADSubjectiveResultItem(
                questionText:   itm["questionText"]   as? String ?? "",
                answerSeen:     itm["answerSeen"]     as? String ?? "",
                strictFeedback: itm["strictFeedback"] as? String ?? "",
                isAttempted:    itm["isAttempted"]    as? Bool ?? false,
                isCorrect:      itm["isCorrect"]      as? Bool ?? false
            )
        }
        return ADSubjectiveResult(
            displayName: displayName.isEmpty ? "Written Answers" : displayName,
            subject: subject, attempted: attempted, correct: correct,
            accuracyPct: accuracy, timestamp: ts, items: items
        )
    }

    private func parseIsoDateMillis(_ date: String) -> Int64 {
        guard !date.isEmpty else { return 0 }
        let fmt = DateFormatter()
        fmt.dateFormat = "yyyy-MM-dd"
        fmt.locale = Locale(identifier: "en_US_POSIX")
        return Int64((fmt.date(from: date)?.timeIntervalSince1970 ?? 0) * 1000)
    }

    func fetchParentTask(taskId: String) async throws -> ParentTaskFull {
        let doc = try await db.collection("parentTaskRequests").document(taskId).getDocument()
        let d = doc.data() ?? [:]
        let unitName = d["unitName"] as? String ?? ""
        let subject = d["subject"] as? String ?? ""
        func intField(_ key: String) -> Int? {
            if let v = d[key] as? Int { return v }
            if let v = d[key] as? Double { return Int(v) }
            if let v = d[key] as? Int64 { return Int(v) }
            return nil
        }
        return ParentTaskFull(
            id: doc.documentID,
            childId: d["childId"] as? String ?? "",
            subject: subject,
            displayName: unitName.isEmpty ? (subject.isEmpty ? "Current task" : subject) : unitName,
            subjectIcon: d["subjectIcon"] as? String ?? "📚",
            type: d["type"] as? String ?? "CUSTOM",
            status: d["status"] as? String ?? "REQUESTED",
            isUnitScope: d["isUnitScope"] as? Bool ?? true,
            submissionPhotoUrls: (d["submissionPhotoUrls"] as? [String]) ?? [],
            submissionScore: intField("submissionScore"),
            submissionTotal: intField("submissionTotal")
        )
    }

    func assignByParent(taskId: String, type: String, customNote: String, mcqCount: Int) async throws {
        try await db.collection("parentTaskRequests").document(taskId).updateData([
            "type": type,
            "customNote": customNote,
            "mcqCount": mcqCount,
            "status": "ASSIGNED",
            "assignedAtMillis": Int64(Date().timeIntervalSince1970 * 1000)
        ])
    }

    func acceptByParent(taskId: String, reviewNote: String) async throws {
        try await db.collection("parentTaskRequests").document(taskId).updateData([
            "status": "ACCEPTED",
            "parentReviewNote": reviewNote,
            "reviewedAtMillis": Int64(Date().timeIntervalSince1970 * 1000)
        ])
    }

    func rejectByParent(taskId: String, reviewNote: String) async throws {
        try await db.collection("parentTaskRequests").document(taskId).updateData([
            "status": "REJECTED",
            "parentReviewNote": reviewNote,
            "reviewedAtMillis": Int64(Date().timeIntervalSince1970 * 1000),
            "submissionPhotoUrls": [] as [String],
            "submissionScore": NSNull(),
            "submissionTotal": NSNull()
        ])
    }

    func listenParentTasks(parentUID: String, onChange: @escaping ([ParentTaskItem]) -> Void) -> ListenerRegistration {
        return db.collection("parentTaskRequests")
            .whereField("parentId", isEqualTo: parentUID)
            .addSnapshotListener { snap, _ in
                let items = (snap?.documents ?? []).compactMap { doc -> ParentTaskItem? in
                    let d = doc.data()
                    guard let childId = d["childId"] as? String else { return nil }
                    let unitName = d["unitName"] as? String ?? ""
                    let subject = d["subject"] as? String ?? ""
                    let displayName = unitName.isEmpty ? (subject.isEmpty ? "Current task" : subject) : unitName
                    return ParentTaskItem(
                        id: doc.documentID,
                        childId: childId,
                        subject: subject,
                        displayName: displayName,
                        subjectIcon: d["subjectIcon"] as? String ?? "📚",
                        type: d["type"] as? String ?? "CUSTOM",
                        status: d["status"] as? String ?? "ASSIGNED",
                        assignedAtMillis: (d["assignedAtMillis"] as? Int64) ?? Int64((d["assignedAtMillis"] as? Double) ?? 0),
                        reminderSentAtMillis: (d["reminderSentAtMillis"] as? Int64) ?? Int64((d["reminderSentAtMillis"] as? Double) ?? 0)
                    )
                }
                onChange(items)
            }
    }

    func listenChildren(parentUID: String, onChange: @escaping ([ChildProfile]) -> Void) -> ListenerRegistration {
        NSLog("[Children] Starting listener for parentUID=\(parentUID)")
        return db.collection("parents")
            .document(parentUID)
            .collection("children")
            .addSnapshotListener { snap, error in
                if let error {
                    NSLog("[Children] Listener error: \(error)")
                    return
                }
                guard let snap else { return }
                NSLog("[Children] Got \(snap.documents.count) child docs for parentUID=\(parentUID)")
                for doc in snap.documents { NSLog("[Children] doc=\(doc.documentID) data=\(doc.data())") }
                onChange(snap.documents.compactMap { ChildProfile(from: $0) })
            }
    }
}

// MARK: - Study Goals data models

struct StudyGoalsConcept {
    let id: String
    let subject: String
    let subjectColor: String
    let subjectIcon: String
    let unit: String
    let unitId: String
    let name: String
    let description: String
    let order: Int

    init?(from doc: QueryDocumentSnapshot) {
        let d = doc.data()
        guard let subject = d["subject"] as? String, !subject.trimmingCharacters(in: .whitespaces).isEmpty else { return nil }
        self.id = doc.documentID
        self.subject = subject.trimmingCharacters(in: .whitespaces)
        self.subjectColor = d["subjectColor"] as? String ?? "#3B7BFF"
        self.subjectIcon = d["subjectIcon"] as? String ?? "📚"
        self.unit = d["unit"] as? String ?? ""
        self.unitId = d["unitId"] as? String ?? ""
        self.name = d["name"] as? String ?? ""
        self.description = d["description"] as? String ?? ""
        self.order = d["order"] as? Int ?? 0
    }
}

struct StudyGoalsActiveTask {
    let id: String
    let subject: String
    let displayName: String
    let status: String
    let assignedAtMillis: Int64
    let reminderSentAtMillis: Int64
}

// MARK: - ChildProfile Firestore init

private extension ChildProfile {
    init?(from doc: QueryDocumentSnapshot) {
        let d = doc.data()
        guard let name = d["name"] as? String else { return nil }
        let grade: String
        if let g = d["grade"] as? Int { grade = "\(g)" }
        else if let g = d["grade"] as? String { grade = g }
        else { return nil }
        let board = d["board"] as? String ?? "CBSE"
        let gender = d["gender"] as? String ?? "male"
        let parentId = d["parentId"] as? String ?? ""
        self.init(id: doc.documentID, name: name, grade: grade, board: board, gender: gender)
        _ = parentId // suppress unused warning; stored in Firestore but not needed in model
    }
}
