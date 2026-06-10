import Foundation
import FirebaseFunctions

// Single entry point for all invite + referral operations on iOS.
// Mirrors InviteRepositoryImpl on Android — same Cloud Functions, same Firestore docs.
@MainActor
final class InviteService {
    static let shared = InviteService()
    private init() {}

    private let functions = Functions.functions()

    // MARK: - UserDefaults keys

    private let kPendingInviteCode        = "pendingInviteCode"
    private let kInviteClaimDone          = "inviteClaimDone"
    private let kParentReferralCode       = "parentReferralCode"
    private let kParentReferralClaimDone  = "parentReferralClaimDone"

    // MARK: - Child invite (friend → friend)

    /// Calls createInvite, returns a ready-to-share text the OS share sheet can send.
    func createChildInviteText(childId: String, childName: String) async -> String? {
        do {
            let result = try await functions.httpsCallable("createInvite")
                .call(["childId": childId])
            guard let data = result.data as? [String: Any],
                  let code = data["code"] as? String, !code.isEmpty else { return nil }
            let first = childName.components(separatedBy: " ").first ?? childName
            return """
            Hi! \(first.isEmpty ? "Your friend" : first) invited you to Eduksha 🎓

            We track learning together — upload class work, take quizzes and keep streaks going!

            Download the app and enter invite code: \(code.uppercased())

            Download: https://eduksha.in/download
            """
        } catch {
            NSLog("[InviteService] createChildInvite failed: \(error)")
            return nil
        }
    }

    // MARK: - Parent invite (parent → parent referral)

    /// Calls createInvite with inviterType=parent, returns share text + code.
    func createParentInviteText(parentName: String) async -> String? {
        do {
            let result = try await functions.httpsCallable("createInvite")
                .call(["inviterType": "parent"])
            guard let data = result.data as? [String: Any],
                  let code = data["code"] as? String, !code.isEmpty else { return nil }
            let first = parentName.components(separatedBy: " ").first.flatMap { $0.isEmpty ? nil : $0 }
            let greeting = first.map { "Hi! \($0) invited you to Eduksha" } ?? "Hi! You've been invited to Eduksha"
            return """
            \(greeting) — the app that helps parents track their child's learning. 📚

            Set tasks, review homework, and stay connected with your child's progress.

            Download here 👇
            https://eduksha.in/download

            Or use referral code: \(code.uppercased()) when signing up.
            """
        } catch {
            NSLog("[InviteService] createParentInvite failed: \(error)")
            return nil
        }
    }

    // MARK: - Claim child invite

    /// Stores an invite code so it survives to the next ChildHome appear.
    func storePendingInviteCode(_ code: String) {
        let clean = code.trimmingCharacters(in: .whitespaces).lowercased()
        guard !clean.isEmpty else { return }
        UserDefaults.standard.set(clean, forKey: kPendingInviteCode)
    }

    /// Called on every ChildHome appear until successfully claimed.
    func tryClaimPendingInvite(childId: String) async {
        guard !UserDefaults.standard.bool(forKey: kInviteClaimDone) else { return }
        guard let code = UserDefaults.standard.string(forKey: kPendingInviteCode),
              !code.isEmpty else {
            UserDefaults.standard.set(true, forKey: kInviteClaimDone)
            return
        }
        do {
            let result = try await functions.httpsCallable("claimInvite")
                .call(["code": code, "childId": childId])
            let status = (result.data as? [String: Any])?["status"] as? String ?? ""
            NSLog("[InviteService] claimInvite status=\(status)")
            if status != "self" {
                UserDefaults.standard.set(true, forKey: kInviteClaimDone)
            }
        } catch {
            NSLog("[InviteService] claimInvite failed (will retry): \(error)")
            // Leave flag unset — retry on next ChildHome appear
        }
    }

    // MARK: - Claim parent referral

    /// Stores a manually-entered referral code from the OTP screen.
    func storeParentReferralCode(_ code: String) {
        let clean = code.trimmingCharacters(in: .whitespaces).lowercased()
        guard !clean.isEmpty else { return }
        UserDefaults.standard.set(clean, forKey: kParentReferralCode)
    }

    /// Called once after auth succeeds on ParentHome.
    func tryClaimParentReferral() async {
        guard !UserDefaults.standard.bool(forKey: kParentReferralClaimDone) else { return }
        guard let code = UserDefaults.standard.string(forKey: kParentReferralCode),
              !code.isEmpty else {
            UserDefaults.standard.set(true, forKey: kParentReferralClaimDone)
            return
        }
        do {
            _ = try await functions.httpsCallable("claimInvite").call(["code": code])
            NSLog("[InviteService] parent referral claimed: code=\(code)")
        } catch {
            NSLog("[InviteService] parent referral claim failed: \(error)")
        }
        UserDefaults.standard.set(true, forKey: kParentReferralClaimDone)
    }

    // MARK: - Universal link / deep link parsing

    /// Parses an invite code from URLs like:
    ///   https://eduksha.in/i/CODE
    ///   https://eduksha.in/invite?code=CODE
    ///   eduksha://invite?code=CODE
    func parseInviteURL(_ url: URL) -> String? {
        let comps = url.pathComponents
        if let idx = comps.firstIndex(of: "i"), idx + 1 < comps.count {
            let code = comps[idx + 1]
            if !code.isEmpty { return code.lowercased() }
        }
        if let urlComps = URLComponents(url: url, resolvingAgainstBaseURL: false),
           let code = urlComps.queryItems?.first(where: { $0.name == "code" })?.value,
           !code.isEmpty {
            return code.lowercased()
        }
        return nil
    }
}
