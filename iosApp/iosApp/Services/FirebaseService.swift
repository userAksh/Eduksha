import Foundation
import UIKit
import FirebaseAuth
import FirebaseFirestore
import FirebaseFunctions
import FirebaseStorage


// MARK: - FirebaseService
// Thin wrapper around Firebase Auth + Firestore for the parent auth flow.

@MainActor
final class FirebaseService {
    static let shared = FirebaseService()
    private init() {}

    private let db = Firestore.firestore()

    // MARK: Phone Auth

    func sendOTP(phone: String) async throws -> String {
        NSLog("[OTP] sendOTP called, phone=\(phone), isMainThread=\(Thread.isMainThread)")
        #if targetEnvironment(simulator)
        NSLog("[OTP] Simulator path — returning stub ID")
        try await Task.sleep(nanoseconds: 400_000_000)
        return "SIMULATOR_VID_\(phone)"
        #else
        NSLog("[OTP] Device path — calling REST API (bypassing Firebase SDK crash on iOS 26)")
        // Firebase SDK 11.15.0 crashes on iOS 26 inside verifyPhoneNumber — it reads
        // UIWindowScene.windows before checking isAppVerificationDisabledForTesting, hitting a
        // nil force-unwrap. Bypass the SDK entirely and call the REST API directly.
        // Firebase backend accepts recaptchaToken:"recaptchaToken" as a test-bypass sentinel
        // and allows configured test phone numbers (e.g. +919999999999) through without real SMS.
        let apiKey = "AIzaSyCR8kR8CvANrCgKgGbAJyzYXnYS-pd3lVA"
        guard let url = URL(string: "https://identitytoolkit.googleapis.com/v1/accounts:sendVerificationCode?key=\(apiKey)") else {
            NSLog("[OTP] ERROR: Bad URL")
            throw NSError(domain: "PhoneAuth", code: -1, userInfo: [NSLocalizedDescriptionKey: "Bad URL"])
        }
        NSLog("[OTP] Building request for \(phone)")
        var req = URLRequest(url: url)
        req.httpMethod = "POST"
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        let body: [String: Any] = ["phoneNumber": phone, "recaptchaToken": "recaptchaToken"]
        req.httpBody = try JSONSerialization.data(withJSONObject: body)
        NSLog("[OTP] Sending REST request...")
        let (data, response) = try await URLSession.shared.data(for: req)
        let httpStatus = (response as? HTTPURLResponse)?.statusCode ?? -1
        let rawBody = String(data: data, encoding: .utf8) ?? "<non-utf8>"
        NSLog("[OTP] REST response status=\(httpStatus) body=\(rawBody)")
        if let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
           let sessionInfo = json["sessionInfo"] as? String {
            NSLog("[OTP] SUCCESS — got sessionInfo (len=\(sessionInfo.count))")
            return sessionInfo
        }
        let msg: String
        if let errJson = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
           let errObj = errJson["error"] as? [String: Any],
           let errMsg = errObj["message"] as? String {
            msg = errMsg
        } else {
            msg = rawBody
        }
        NSLog("[OTP] FAILED — error: \(msg)")
        throw NSError(domain: "PhoneAuth", code: httpStatus, userInfo: [NSLocalizedDescriptionKey: msg])
        #endif
    }

    func verifyOTP(verificationID: String, code: String) async throws -> String {
        NSLog("[OTP] verifyOTP called, vidLen=\(verificationID.count) code=\(code) isMainThread=\(Thread.isMainThread)")
        let credential = PhoneAuthProvider.provider()
            .credential(withVerificationID: verificationID, verificationCode: code)
        NSLog("[OTP] credential created, calling signIn...")
        let result = try await Auth.auth().signIn(with: credential)
        NSLog("[OTP] signIn succeeded, uid=\(result.user.uid)")
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

    func getConceptById(conceptId: String) async throws -> StudyGoalsConcept? {
        let doc = try await db.collection("concepts").document(conceptId).getDocument()
        guard doc.exists, let data = doc.data() else { return nil }
        // Wrap as QueryDocumentSnapshot is not possible; parse manually
        let subject = data["subject"] as? String ?? ""
        guard !subject.trimmingCharacters(in: .whitespaces).isEmpty else { return nil }
        return StudyGoalsConcept(
            id: doc.documentID,
            subject: subject.trimmingCharacters(in: .whitespaces),
            subjectColor: data["subjectColor"] as? String ?? "#3B7BFF",
            subjectIcon: data["subjectIcon"] as? String ?? "📚",
            unit: data["unit"] as? String ?? "",
            unitId: data["unitId"] as? String ?? "",
            name: data["name"] as? String ?? "",
            description: data["description"] as? String ?? "",
            order: data["order"] as? Int ?? 0,
            unitOrder: data["unitOrder"] as? Int ?? 0
        )
    }

    func isRevisionMarked(childId: String, conceptId: String) async -> Bool {
        if Auth.auth().currentUser == nil { try? await Auth.auth().signInAnonymously() }
        let docId = "\(childId)_concept_\(conceptId)"
        let doc = try? await db.collection("revision_marks").document(docId).getDocument()
        return doc?.exists == true
    }

    @discardableResult
    func toggleRevisionMark(
        childId: String, conceptId: String, conceptName: String,
        subject: String, subjectColor: String, subjectIcon: String,
        unitId: String, unitName: String
    ) async -> Bool {
        if Auth.auth().currentUser == nil { try? await Auth.auth().signInAnonymously() }
        let docId = "\(childId)_concept_\(conceptId)"
        let ref = db.collection("revision_marks").document(docId)
        let existing = (try? await ref.getDocument())?.exists == true
        if existing {
            try? await ref.delete()
            return false
        } else {
            let now = Int64(Date().timeIntervalSince1970 * 1000)
            try? await ref.setData([
                "id": docId,
                "childId": childId,
                "scope": "concept",
                "targetId": conceptId,
                "name": conceptName,
                "subject": subject,
                "subjectColor": subjectColor,
                "subjectIcon": subjectIcon,
                "unitId": unitId,
                "unitName": unitName,
                "markedAt": now
            ])
            return true
        }
    }

    func hasConceptActivity(childId: String, conceptId: String) async -> Bool {
        // Check uploads collection
        if let snap = try? await db.collection("uploads")
            .whereField("childId", isEqualTo: childId)
            .whereField("conceptId", isEqualTo: conceptId)
            .getDocuments(), !snap.documents.isEmpty {
            return true
        }
        // Check quiz_attempts/{childId}/concepts/{conceptId}
        if let doc = try? await db.collection("quiz_attempts")
            .document(childId)
            .collection("concepts")
            .document(conceptId)
            .getDocument(),
           doc.exists,
           let attempts = doc.data()?["attempts"] as? Int,
           attempts > 0 {
            return true
        }
        return false
    }

    func saveConceptCompletion(childId: String, conceptId: String, difficulty: String, mode: String) async {
        let now = Int64(Date().timeIntervalSince1970 * 1000)
        try? await db.collection("completions").addDocument(data: [
            "childId": childId,
            "conceptId": conceptId,
            "effort": difficulty,
            "mode": mode,
            "timestamp": now
        ])
    }

    func isUnitRevisionMarked(childId: String, unitId: String) async -> Bool {
        if Auth.auth().currentUser == nil { try? await Auth.auth().signInAnonymously() }
        let docId = "\(childId)_unit_\(unitId)"
        let doc = try? await db.collection("revision_marks").document(docId).getDocument()
        return doc?.exists == true
    }

    @discardableResult
    func toggleUnitRevisionMark(
        childId: String, unitId: String, unitName: String,
        subject: String, subjectColor: String, subjectIcon: String
    ) async -> Bool {
        if Auth.auth().currentUser == nil { try? await Auth.auth().signInAnonymously() }
        let docId = "\(childId)_unit_\(unitId)"
        let ref = db.collection("revision_marks").document(docId)
        let existing = (try? await ref.getDocument())?.exists == true
        if existing {
            try? await ref.delete()
            return false
        } else {
            let now = Int64(Date().timeIntervalSince1970 * 1000)
            try? await ref.setData([
                "id": docId, "childId": childId, "scope": "unit",
                "targetId": unitId, "name": unitName, "subject": subject,
                "subjectColor": subjectColor, "subjectIcon": subjectIcon,
                "unitId": unitId, "unitName": unitName, "markedAt": now
            ])
            return true
        }
    }

    func hasUnitActivity(childId: String, unitId: String) async -> Bool {
        if let snap = try? await db.collection("uploads")
            .whereField("childId", isEqualTo: childId)
            .whereField("unitId", isEqualTo: unitId)
            .getDocuments(), !snap.documents.isEmpty { return true }
        if let doc = try? await db.collection("quiz_attempts")
            .document(childId).collection("units").document(unitId).getDocument(),
           doc.exists,
           let attempts = doc.data()?["attempts"] as? Int, attempts > 0 { return true }
        return false
    }

    func saveUnitCompletion(childId: String, unitId: String, conceptIds: [String], difficulty: String, mode: String) async {
        let now = Int64(Date().timeIntervalSince1970 * 1000)
        try? await db.collection("completions").addDocument(data: [
            "childId": childId, "unitId": unitId,
            "conceptIds": conceptIds, "effort": difficulty,
            "mode": mode, "timestamp": now
        ])
    }

    func generateQuizQuestions(
        conceptId: String, conceptName: String, conceptDescription: String,
        subject: String, grade: Int, unitName: String
    ) async throws -> [McqQuestion] {
        // On simulator, phone auth (APNs) doesn't work — sign in anonymously to get
        // a real Firebase auth token so Cloud Functions accept the request.
        #if targetEnvironment(simulator)
        if Auth.auth().currentUser == nil {
            try await Auth.auth().signInAnonymously()
        }
        #endif
        let fn = Functions.functions().httpsCallable("generateMcq")
        let data: [String: Any] = [
            "conceptId": conceptId,
            "conceptName": conceptName,
            "conceptDescription": conceptDescription,
            "subject": subject,
            "grade": grade,
            "unitName": unitName,
            "targetCount": 10
        ]
        let result = try await fn.call(data)
        guard let dict = result.data as? [String: Any],
              let rawQuestions = dict["questions"] as? [[String: Any]] else {
            throw NSError(domain: "Quiz", code: -1, userInfo: [NSLocalizedDescriptionKey: "Invalid response from generateMcq"])
        }
        let parsed = rawQuestions.compactMap { m -> McqQuestion? in
            guard let text = m["text"] as? String,
                  let options = m["options"] as? [String],
                  options.count == 4 else { return nil }
            return McqQuestion(
                id: m["id"] as? String ?? UUID().uuidString,
                text: text,
                options: options,
                correctIndex: (m["correctIndex"] as? Int) ?? 0,
                explanation: m["explanation"] as? String ?? ""
            )
        }
        if parsed.isEmpty {
            throw NSError(domain: "Quiz", code: -2, userInfo: [NSLocalizedDescriptionKey: "No valid questions in response"])
        }
        return parsed
    }

    func saveQuizAttempt(
        childId: String, conceptId: String, correct: Int, total: Int,
        selections: [Int: Int], displayName: String = "", subject: String = "",
        questions: [McqQuestion] = []
    ) async {
        let now = Int64(Date().timeIntervalSince1970 * 1000)
        let ref = db.collection("quiz_attempts").document(childId)
            .collection("concepts").document(conceptId)
        let selectionsStr = selections.sorted { $0.key < $1.key }
            .map { "\($0.key):\($0.value)" }.joined(separator: ",")
        let encodedQuestions: [[String: Any]] = questions.map { q in
            ["id": q.id, "text": q.text, "options": q.options,
             "correctIndex": Int64(q.correctIndex), "explanation": q.explanation]
        }
        try? await ref.setData([
            "attempts": FieldValue.increment(Int64(1)),
            "lastCorrect": correct,
            "lastTotal": total,
            "lastUpdated": now,
            "selections": selectionsStr,
            "displayName": displayName,
            "subject": subject,
            "lastQuestions": encodedQuestions
        ], merge: true)
    }

    func fetchAttemptCount(childId: String, id: String, isUnit: Bool) async -> Int {
        let sub = isUnit ? "units" : "concepts"
        guard let doc = try? await db.collection("quiz_attempts")
            .document(childId).collection(sub).document(id).getDocument(),
              doc.exists,
              let attempts = doc.data()?["attempts"] as? Int
        else { return 0 }
        return attempts
    }

    struct QuizAttemptSummary {
        let attempts: Int
        let lastCorrect: Int
        let lastTotal: Int
        let accuracyPct: Int
        let lastUpdated: Int64
    }

    func fetchQuizAttemptSummary(childId: String, conceptId: String, unitId: String) async -> QuizAttemptSummary? {
        let (sub, docId): (String, String) = conceptId.isEmpty ? ("units", unitId) : ("concepts", conceptId)
        guard !docId.isEmpty else { return nil }
        guard let doc = try? await db.collection("quiz_attempts")
            .document(childId).collection(sub).document(docId).getDocument(),
              doc.exists, let d = doc.data() else { return nil }
        let attempts = d["attempts"] as? Int ?? 0
        guard attempts > 0 else { return nil }
        let lastCorrect = d["lastCorrect"] as? Int ?? 0
        let lastTotal = d["lastTotal"] as? Int ?? 0
        let lastUpdated = longMillis(d["lastUpdated"])
        let pct = lastTotal > 0 ? Int(Double(lastCorrect) / Double(lastTotal) * 100) : 0
        return QuizAttemptSummary(attempts: attempts, lastCorrect: lastCorrect, lastTotal: lastTotal,
                                  accuracyPct: pct, lastUpdated: lastUpdated)
    }

    func listenQuizAttempts(childId: String, onChange: @escaping () -> Void) -> [ListenerRegistration] {
        let base = db.collection("quiz_attempts").document(childId)
        return ["concepts", "units"].map { sub in
            base.collection(sub).addSnapshotListener { _, _ in onChange() }
        }
    }

    func uploadConceptWork(
        childId: String, conceptId: String, kind: String,
        unitId: String, unitName: String, conceptName: String,
        subject: String, subjectColor: String, subjectIcon: String,
        topic: String = "",
        dateMillis: Int64 = 0,
        images: [UIImage]
    ) async -> Bool {
        var photoURLs: [String] = []
        for image in images {
            guard let data = image.jpegData(compressionQuality: 0.8) else { continue }
            // Android path: uploads/{childId}/{uuid}.jpg
            let path = "uploads/\(childId)/\(UUID().uuidString).jpg"
            let ref = Storage.storage().reference().child(path)
            do {
                _ = try await ref.putDataAsync(data)
                let url = try await ref.downloadURL()
                photoURLs.append(url.absoluteString)
            } catch {
                NSLog("[FirebaseService] storage upload failed: \(error)")
            }
        }
        guard !photoURLs.isEmpty else { return false }
        let timestamp = dateMillis > 0 ? dateMillis : Int64(Date().timeIntervalSince1970 * 1000)
        let docRef = db.collection("uploads").document()
        try? await docRef.setData([
            "id": docRef.documentID,
            "childId": childId,
            "kind": kind,
            "subject": subject,
            "subjectColor": subjectColor,
            "subjectIcon": subjectIcon,
            "unitId": unitId,
            "unitName": unitName,
            "conceptId": conceptId,
            "conceptName": conceptName,
            "topic": topic,
            "photoUrls": photoURLs,
            "timestamp": timestamp,
            "createdAt": FieldValue.serverTimestamp()
        ])
        return true
    }

    // Mirrors Android HomeworkLogViewModel: saves to "homeworks" + mirrors to "uploads"
    func saveHomework(
        childId: String,
        subject: String,
        teacherName: String,
        chapterName: String,
        dateMillis: Int64,
        images: [UIImage]
    ) async -> String? {
        var photoURLs: [String] = []
        for image in images {
            guard let data = image.jpegData(compressionQuality: 0.8) else { continue }
            // Android path: homework_photos/{childId}/{uuid}.jpg
            let path = "homework_photos/\(childId)/\(UUID().uuidString).jpg"
            let ref = Storage.storage().reference().child(path)
            do {
                _ = try await ref.putDataAsync(data)
                let url = try await ref.downloadURL()
                photoURLs.append(url.absoluteString)
            } catch {
                NSLog("[FirebaseService] homework storage upload failed: \(error)")
                return "Couldn't upload photos. Please check your connection and try again."
            }
        }
        guard !photoURLs.isEmpty else { return "No photos were uploaded." }

        let isoDate: String = {
            let fmt = DateFormatter()
            fmt.dateFormat = "yyyy-MM-dd"
            return fmt.string(from: Date(timeIntervalSince1970: Double(dateMillis) / 1000))
        }()

        // 1. Save to "homeworks" collection
        do {
            try await db.collection("homeworks").addDocument(data: [
                "childId": childId,
                "subject": subject,
                "teacherName": teacherName,
                "chapterName": chapterName,
                "date": isoDate,
                "photoUrls": photoURLs,
                "timestamp": dateMillis
            ])
        } catch {
            return "Couldn't save homework. Please try again."
        }

        // 2. Mirror to "uploads" with kind="homework" (so it shows in Revisit Past Topics)
        let (color, icon) = subjectStyleFor(subject)
        let topic = chapterName.isEmpty
            ? "Homework — \(isoDate)"
            : "\(chapterName) — Homework"
        let uploadRef = db.collection("uploads").document()
        try? await uploadRef.setData([
            "id": uploadRef.documentID,
            "childId": childId,
            "kind": "homework",
            "subject": subject,
            "subjectColor": color,
            "subjectIcon": icon,
            "unitId": "",
            "unitName": "",
            "conceptId": "",
            "conceptName": "",
            "topic": topic,
            "photoUrls": photoURLs,
            "timestamp": dateMillis,
            "createdAt": FieldValue.serverTimestamp()
        ])
        return nil  // success
    }

    private func subjectStyleFor(_ subject: String) -> (color: String, icon: String) {
        switch subject {
        case "Science":        return ("#22C55E", "🔬")
        case "Mathematics":    return ("#3B82F6", "🔢")
        case "English":        return ("#EC4899", "📖")
        case "Social Science": return ("#F59E0B", "🌍")
        case "Hindi":          return ("#EF4444", "✍️")
        case "Sanskrit":       return ("#8B5CF6", "📜")
        default:               return ("#94A3B8", "📘")
        }
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

    func requestFromChild(
        parentId: String, childId: String,
        unitId: String, unitName: String,
        conceptId: String, conceptName: String,
        isUnitScope: Bool,
        subject: String, subjectColor: String, subjectIcon: String
    ) async throws {
        let ref = db.collection("parentTaskRequests").document()
        let now = Int64(Date().timeIntervalSince1970 * 1000)
        try await ref.setData([
            "id": ref.documentID,
            "parentId": parentId,
            "childId": childId,
            "unitId": unitId,
            "unitName": unitName,
            "conceptId": conceptId,
            "conceptName": conceptName,
            "isUnitScope": isUnitScope,
            "subject": subject,
            "subjectColor": subjectColor,
            "subjectIcon": subjectIcon,
            "status": "REQUESTED",
            "requestedAtMillis": now
        ])
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
                    detail: .homework(homeworkId: doc.documentID, photoUrls: photos)
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
        let attempts    = intVal(d["attempts"])
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
                            correct: correct, total: total, attempts: attempts,
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
        let rawQs = (d["assignedSubjectiveQs"] as? [[String: Any]]) ?? []
        let subjQs = rawQs.compactMap { q -> ParentTaskQuestion? in
            guard let text = q["text"] as? String else { return nil }
            return ParentTaskQuestion(id: q["id"] as? String ?? UUID().uuidString, text: text)
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
            unitId: d["unitId"] as? String ?? "",
            conceptId: d["conceptId"] as? String ?? "",
            mcqCount: intField("mcqCount") ?? 5,
            customNote: d["customNote"] as? String ?? "",
            parentReviewNote: d["parentReviewNote"] as? String ?? "",
            assignedSubjectiveQs: subjQs,
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

    // MARK: - Child-side task submission

    func submitByChildPhotos(taskId: String, childId: String, images: [UIImage]) async throws {
        if Auth.auth().currentUser == nil { try await Auth.auth().signInAnonymously() }
        var photoURLs: [String] = []
        for image in images {
            guard let data = image.jpegData(compressionQuality: 0.82) else { continue }
            // mirrors Android: uploads/<childId>/parenttask_<taskId>_<uuid>.jpg
            // so the same Storage rules that allow self-learning uploads apply here
            let path = "uploads/\(childId)/parenttask_\(taskId)_\(UUID().uuidString).jpg"
            let ref = Storage.storage().reference().child(path)
            _ = try await ref.putDataAsync(data)
            let url = try await ref.downloadURL()
            photoURLs.append(url.absoluteString)
        }
        guard !photoURLs.isEmpty else { throw URLError(.cannotParseResponse) }
        try await db.collection("parentTaskRequests").document(taskId).updateData([
            "status": "PENDING_REVIEW",
            "submissionPhotoUrls": photoURLs,
            "submittedAtMillis": Int64(Date().timeIntervalSince1970 * 1000)
        ])
    }

    // Mirrors Android FcmTokenRepositoryImpl.writeToken() — stores token so the
    // onParentTaskWrite Cloud Function can deliver pushes to this device.
    func saveFcmToken(parentUID: String, token: String) async throws {
        try await db.collection("parents").document(parentUID).setData(["fcmToken": token], merge: true)
        NSLog("[FCM] Token saved to parents/\(parentUID)")
    }

    func submitByChildQuizScore(taskId: String, score: Int, total: Int) async throws {
        try await db.collection("parentTaskRequests").document(taskId).updateData([
            "status": "PENDING_REVIEW",
            "submissionScore": score,
            "submissionTotal": total,
            "submissionPhotoUrls": [] as [String],
            "submittedAtMillis": Int64(Date().timeIntervalSince1970 * 1000)
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
                    let taskType = d["type"] as? String ?? "CUSTOM"
                    return ParentTaskItem(
                        id: doc.documentID,
                        childId: childId,
                        subject: subject,
                        displayName: displayName,
                        subjectIcon: d["subjectIcon"] as? String ?? "📚",
                        type: taskType,
                        status: d["status"] as? String ?? "ASSIGNED",
                        isUnitScope: d["isUnitScope"] as? Bool ?? (taskType != "OUR_QUESTIONS"),
                        mcqCount: d["mcqCount"] as? Int ?? Int((d["mcqCount"] as? Double) ?? 5),
                        assignedAtMillis: (d["assignedAtMillis"] as? Int64) ?? Int64((d["assignedAtMillis"] as? Double) ?? 0),
                        requestedAtMillis: (d["requestedAtMillis"] as? Int64) ?? Int64((d["requestedAtMillis"] as? Double) ?? 0),
                        reminderSentAtMillis: (d["reminderSentAtMillis"] as? Int64) ?? Int64((d["reminderSentAtMillis"] as? Double) ?? 0),
                        submittedAtMillis: (d["submittedAtMillis"] as? Int64) ?? Int64((d["submittedAtMillis"] as? Double) ?? 0),
                        reviewedAtMillis: (d["reviewedAtMillis"] as? Int64) ?? Int64((d["reviewedAtMillis"] as? Double) ?? 0)
                    )
                }
                onChange(items)
            }
    }

    // MARK: Revision Marks

    func listenRevisionMarks(childId: String, onChange: @escaping ([RevisionMarkItem]) -> Void) -> ListenerRegistration {
        db.collection("revision_marks")
            .whereField("childId", isEqualTo: childId)
            .addSnapshotListener { snap, _ in
                let marks = (snap?.documents ?? []).compactMap { doc -> RevisionMarkItem? in
                    let d = doc.data()
                    guard let name = d["name"] as? String, !name.isEmpty else { return nil }
                    return RevisionMarkItem(
                        id: doc.documentID,
                        childId: d["childId"] as? String ?? "",
                        scope: d["scope"] as? String ?? "concept",
                        targetId: d["targetId"] as? String ?? "",
                        name: name,
                        subject: d["subject"] as? String ?? "",
                        subjectColor: d["subjectColor"] as? String ?? "",
                        subjectIcon: d["subjectIcon"] as? String ?? "",
                        unitName: d["unitName"] as? String ?? "",
                        markedAt: self.longMillis(d["markedAt"])
                    )
                }.sorted { $0.markedAt > $1.markedAt }
                onChange(marks)
            }
    }

    func unmarkRevision(childId: String, scope: String, targetId: String) {
        let docId = "\(childId)_\(scope)_\(targetId)"
        db.collection("revision_marks").document(docId).delete()
    }

    func fetchUploads(childId: String) async -> [UploadItem] {
        // No composite index needed — whereField on childId only (single-field index auto-created),
        // sort by timestamp client-side.
        guard let snap = try? await db.collection("uploads")
            .whereField("childId", isEqualTo: childId)
            .limit(to: 200)
            .getDocuments() else { return [] }
        return snap.documents.compactMap { doc -> UploadItem? in
            let d = doc.data()
            return UploadItem(
                id: doc.documentID,
                childId: d["childId"] as? String ?? childId,
                kind: d["kind"] as? String ?? "",
                subject: d["subject"] as? String ?? "",
                subjectColor: d["subjectColor"] as? String ?? "",
                subjectIcon: d["subjectIcon"] as? String ?? "",
                unitId: d["unitId"] as? String ?? "",
                unitName: d["unitName"] as? String ?? "",
                conceptId: d["conceptId"] as? String ?? "",
                conceptName: d["conceptName"] as? String ?? "",
                topic: d["topic"] as? String ?? "",
                photoUrls: d["photoUrls"] as? [String] ?? [],
                timestamp: longMillis(d["timestamp"])
            )
        }.sorted { $0.timestamp > $1.timestamp }
    }

    func fetchRevisionMarksOnce(childId: String) async -> [RevisionMarkItem] {
        guard let snap = try? await db.collection("revision_marks")
            .whereField("childId", isEqualTo: childId)
            .getDocuments() else { return [] }
        return snap.documents.compactMap { doc -> RevisionMarkItem? in
            let d = doc.data()
            guard let name = d["name"] as? String, !name.isEmpty else { return nil }
            return RevisionMarkItem(
                id: doc.documentID,
                childId: d["childId"] as? String ?? childId,
                scope: d["scope"] as? String ?? "concept",
                targetId: d["targetId"] as? String ?? "",
                name: name,
                subject: d["subject"] as? String ?? "",
                subjectColor: d["subjectColor"] as? String ?? "",
                subjectIcon: d["subjectIcon"] as? String ?? "",
                unitName: d["unitName"] as? String ?? "",
                markedAt: longMillis(d["markedAt"])
            )
        }.sorted { $0.markedAt > $1.markedAt }
    }

    func sendReminderToParent(taskId: String) async {
        let now = Int64(Date().timeIntervalSince1970 * 1000)
        try? await db.collection("parentTaskRequests").document(taskId)
            .updateData(["parentReminderSentAtMillis": now])
    }

    // MARK: Daily News (Cloud Function)

    func fetchDailyNews() async -> Result<[CurrentAffairItem], Error> {
        do {
            if Auth.auth().currentUser == nil {
                try await Auth.auth().signInAnonymously()
            }
            let callable = Functions.functions().httpsCallable("getDailyNews")
            let result = try await callable.call([:] as [String: Any])
            guard let payload = result.data as? [String: Any],
                  let rawStories = payload["stories"] as? [[String: Any]] else {
                return .failure(NSError(domain: "DailyNews", code: -1,
                    userInfo: [NSLocalizedDescriptionKey: "Unexpected payload shape"]))
            }
            let stories = rawStories.enumerated().compactMap { (index, item) -> CurrentAffairItem? in
                let headline = (item["headline"] as? String ?? "").trimmingCharacters(in: .whitespaces)
                let body = (item["body"] as? String ?? "").trimmingCharacters(in: .whitespaces)
                guard !headline.isEmpty, !body.isEmpty else { return nil }
                return CurrentAffairItem(
                    id: (item["id"] as? String ?? "").isEmpty ? "story-\(index + 1)" : (item["id"] as! String),
                    headline: headline,
                    category: (item["category"] as? String ?? "").isEmpty ? "Fun fact" : (item["category"] as! String),
                    timeAgo: "Today",
                    emoji: (item["emoji"] as? String ?? "").isEmpty ? "💡" : (item["emoji"] as! String),
                    body: body
                )
            }
            if stories.isEmpty {
                return .failure(NSError(domain: "DailyNews", code: -2,
                    userInfo: [NSLocalizedDescriptionKey: "The paper isn't ready yet. Try again in a moment."]))
            }
            return .success(stories)
        } catch {
            return .failure(error)
        }
    }

    // MARK: Today's Learning items

    func fetchTodayItems(childId: String, parentTasks: [ParentTaskItem]) async -> [TodayLearningEntry] {
        let now = Int64(Date().timeIntervalSince1970 * 1000)
        let cutoff = startOfTodayMillis()
        var items: [TodayLearningEntry] = []

        // 1. uploads — same as Android; homeworks collection is NOT used here
        if let snap = try? await db.collection("uploads")
            .whereField("childId", isEqualTo: childId)
            .getDocuments() {
            for doc in snap.documents {
                let d = doc.data()
                let kind = d["kind"] as? String ?? ""
                let ts = longMillis(d["timestamp"])
                guard ts >= cutoff else { continue }
                let topic       = d["topic"]       as? String ?? ""
                let conceptName = d["conceptName"] as? String ?? ""
                let unitName    = d["unitName"]    as? String ?? ""
                let subject     = d["subject"]     as? String ?? ""
                let conceptId   = d["conceptId"]   as? String ?? ""
                let unitId      = d["unitId"]      as? String ?? ""
                let title = todayUploadTitle(topic: topic, conceptName: conceptName, unitName: unitName, kind: kind)
                let groupKey: String
                if !topic.isEmpty {
                    groupKey = "tuition:\(subject.lowercased()):\(topic.lowercased())"
                } else if !conceptId.isEmpty {
                    groupKey = "concept:\(conceptId)"
                } else if !unitId.isEmpty {
                    groupKey = "unit:\(unitId)"
                } else {
                    groupKey = "unknown:\(doc.documentID)"
                }
                items.append(TodayLearningEntry(
                    id: doc.documentID,
                    kind: uploadKindToToday(kind),
                    title: title,
                    subject: subject,
                    timeLabel: relativeTimeLabel(timestamp: ts, now: now),
                    status: .completed,
                    scoreLabel: nil,
                    timestamp: ts,
                    navTarget: .uploadGroup(groupKey: groupKey)
                ))
            }
        }

        // 2. subjective_results
        if let snap = try? await db.collection("subjective_results")
            .whereField("childId", isEqualTo: childId)
            .getDocuments() {
            for doc in snap.documents {
                let d = doc.data()
                let ts = longMillis(d["timestamp"])
                guard ts >= cutoff else { continue }
                let subject = d["subject"] as? String ?? ""
                let displayName = d["displayName"] as? String ?? ""
                let title = displayName.isEmpty ? "Write-answer session" : displayName
                let correct = intVal(d["correctCount"])
                let total   = intVal(d["attemptedCount"])
                items.append(TodayLearningEntry(
                    id: doc.documentID,
                    kind: .quiz,
                    title: title,
                    subject: subject,
                    timeLabel: relativeTimeLabel(timestamp: ts, now: now),
                    status: .completed,
                    scoreLabel: total > 0 ? "\(correct)/\(total)" : nil,
                    timestamp: ts,
                    navTarget: .subjective(resultId: doc.documentID)
                ))
            }
        }

        // 3. quiz_attempts (concepts + units)
        for sub in ["concepts", "units"] {
            if let snap = try? await db.collection("quiz_attempts")
                .document(childId)
                .collection(sub)
                .getDocuments() {
                for doc in snap.documents {
                    let d = doc.data()
                    let total = intVal(d["lastTotal"])
                    guard intVal(d["attempts"]) > 0, total > 0 else { continue }
                    let ts = longMillis(d["lastUpdated"])
                    guard ts >= cutoff else { continue }
                    let subject = d["subject"] as? String ?? ""
                    let displayName = d["displayName"] as? String ?? ""
                    let title = displayName.isEmpty ? "Quiz" : displayName
                    let correct = intVal(d["lastCorrect"])
                    let navTarget: TodayNavTarget = sub == "units"
                        ? .quizResult(unitId: doc.documentID, conceptId: "")
                        : .quizResult(unitId: "", conceptId: doc.documentID)
                    items.append(TodayLearningEntry(
                        id: "\(sub)_\(doc.documentID)",
                        kind: .quiz,
                        title: title,
                        subject: subject,
                        timeLabel: relativeTimeLabel(timestamp: ts, now: now),
                        status: .completed,
                        scoreLabel: total > 0 ? "\(correct)/\(total)" : nil,
                        timestamp: ts,
                        navTarget: navTarget
                    ))
                }
            }
        }

        // 4. parent tasks — activityAt mirrors Android: maxOf(requestedAt, assignedAt, submittedAt, reviewedAt)
        for task in parentTasks {
            let activityAt = max(task.requestedAtMillis, max(task.assignedAtMillis, max(task.submittedAtMillis, task.reviewedAtMillis)))
            guard activityAt >= cutoff else { continue }
            let isCompleted = ["ACCEPTED", "COMPLETED"].contains(task.status)
            let isPending   = ["ASSIGNED", "PENDING_REVIEW", "REJECTED", "REQUESTED"].contains(task.status)
            guard isCompleted || isPending else { continue }
            items.append(TodayLearningEntry(
                id: "parent-\(task.id)",
                kind: .task,
                title: task.displayName.isEmpty ? "Parent task" : task.displayName,
                subject: task.subject.isEmpty ? "Family" : task.subject,
                timeLabel: relativeTimeLabel(timestamp: activityAt, now: now),
                status: isCompleted ? .completed : .pending,
                assignedByParent: true,
                timestamp: activityAt,
                navTarget: .parentTask(taskId: task.id)
            ))
        }

        return items.sorted { $0.timestamp > $1.timestamp }
    }

    // Android uploadKindToToday: class_work/homework_log/homework → .homework, else → .task
    private func uploadKindToToday(_ kind: String) -> TodayKind {
        switch kind {
        case "class_work", "homework_log", "homework": return .homework
        default: return .task
        }
    }

    private func todayUploadTitle(topic: String, conceptName: String, unitName: String, kind: String) -> String {
        if !topic.isEmpty { return topic }
        if !conceptName.isEmpty { return conceptName }
        if !unitName.isEmpty { return unitName }
        return kind.isEmpty ? "Untitled" : kind.replacingOccurrences(of: "_", with: " ").capitalized
    }

    func listenUploads(childId: String, onChange: @escaping () -> Void) -> ListenerRegistration {
        db.collection("uploads")
            .whereField("childId", isEqualTo: childId)
            .addSnapshotListener { _, _ in onChange() }
    }

    func listenSubjectiveResults(childId: String, onChange: @escaping () -> Void) -> ListenerRegistration {
        db.collection("subjective_results")
            .whereField("childId", isEqualTo: childId)
            .addSnapshotListener { _, _ in onChange() }
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
    let unitOrder: Int

    init(id: String, subject: String, subjectColor: String, subjectIcon: String,
         unit: String, unitId: String, name: String, description: String,
         order: Int, unitOrder: Int) {
        self.id = id; self.subject = subject; self.subjectColor = subjectColor
        self.subjectIcon = subjectIcon; self.unit = unit; self.unitId = unitId
        self.name = name; self.description = description
        self.order = order; self.unitOrder = unitOrder
    }

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
        self.unitOrder = d["unitOrder"] as? Int ?? 0
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
        let pin = d["pin"] as? String ?? ""
        self.init(id: doc.documentID, name: name, grade: grade, board: board, gender: gender, pin: pin)
    }
}
