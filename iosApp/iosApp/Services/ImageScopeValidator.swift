import UIKit
import FirebaseFunctions

// Calls the validateImageScope Cloud Function, which runs on Google Cloud
// with Application Default Credentials and uses gemini-2.5-flash-lite via
// Vertex AI — same model as Android's Firebase.ai(backend=vertexAI()).
@MainActor
final class ImageScopeValidator {
    static let shared = ImageScopeValidator()
    private init() {}

    private let functions = Functions.functions(region: "us-central1")

    /// Returns nil on success, or a kid-friendly error string on mismatch.
    func validate(
        subject: String,
        grade: Int,
        topic: String,
        lessonContext: String = "",
        artefactLabel: String = "class work",
        images: [UIImage]
    ) async -> String? {
        NSLog("[ScopeValidator] ▶ validate subject=\(subject) grade=\(grade) topic=\(topic) images=\(images.count)")
        guard !images.isEmpty else { return "Please add at least one photo first." }

        // Downsample and encode each image
        var imageBase64: [String] = []
        for (i, image) in images.enumerated() {
            let downsampled = downsample(image, maxSide: 1024)
            guard let data = downsampled.jpegData(compressionQuality: 0.75) else {
                NSLog("[ScopeValidator] ⚠ jpegData failed for image \(i)")
                continue
            }
            imageBase64.append(data.base64EncodedString())
            NSLog("[ScopeValidator]   image[\(i)] jpeg=\(data.count)B")
        }
        guard !imageBase64.isEmpty else { return "Couldn't read uploaded photos." }

        let payload: [String: Any] = [
            "subject":      subject,
            "grade":        grade,
            "topic":        topic,
            "lessonContext": lessonContext,
            "artefactLabel": artefactLabel,
            "images":       imageBase64,
        ]

        NSLog("[ScopeValidator] ▶ calling validateImageScope function, images=\(imageBase64.count)")
        do {
            let result = try await functions.httpsCallable("validateImageScope").call(payload)
            guard let data = result.data as? [String: Any] else {
                NSLog("[ScopeValidator] ✖ unexpected result type: \(result.data)")
                return "Couldn't verify photos. Please try again."
            }
            let ok     = data["ok"]     as? Bool   ?? false
            let reason = data["reason"] as? String ?? ""
            NSLog("[ScopeValidator] ◀ ok=\(ok) reason=\(reason)")

            if ok { return nil }
            return reason.isEmpty ? "These photos don't seem to match the topic." : reason

        } catch let error as NSError {
            NSLog("[ScopeValidator] ✖ function error: \(error.localizedDescription) domain=\(error.domain) code=\(error.code)")
            if let details = error.userInfo["details"] {
                NSLog("[ScopeValidator]   details=\(details)")
            }
            return friendlyError(error)
        }
    }

    private func downsample(_ image: UIImage, maxSide: CGFloat) -> UIImage {
        let size  = image.size
        let scale = min(maxSide / max(size.width, size.height), 1.0)
        guard scale < 1.0 else { return image }
        let newSize = CGSize(width: size.width * scale, height: size.height * scale)
        return UIGraphicsImageRenderer(size: newSize)
            .image { _ in image.draw(in: CGRect(origin: .zero, size: newSize)) }
    }

    private func friendlyError(_ error: NSError) -> String {
        let msg = error.localizedDescription.lowercased()
        if msg.contains("unavailable") || msg.contains("internal") {
            return "Dobi's brain is a little busy right now. Please wait a minute and try again."
        }
        if msg.contains("deadline") || msg.contains("timeout") {
            return "No internet right now. Check your connection and try again."
        }
        if msg.contains("unauthenticated") {
            return "Couldn't verify photos. Please try again."
        }
        return "Couldn't verify photos. Please try again."
    }
}
