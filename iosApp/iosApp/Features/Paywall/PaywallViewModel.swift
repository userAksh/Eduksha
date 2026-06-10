import Foundation
import FirebaseAuth
import FirebaseFunctions
import FirebaseFirestore

enum PaywallTab: String {
    case autoPay  = "autopay"
    case trial    = "trial"

    var displayLabel: String {
        switch self {
        case .autoPay:  return "AUTO\nPAY"
        case .trial:    return "TRIAL\n@ ₹1"
        }
    }
}

enum PaywallPlan {
    case monthly, quarterly, halfYearly, trial

    var planType: String {
        switch self {
        case .monthly:    return "monthly"
        case .quarterly:  return "quarterly"
        case .halfYearly: return "halfyearly"
        case .trial:      return "trial"
        }
    }
    var label: String {
        switch self {
        case .monthly:    return "Monthly"
        case .quarterly:  return "Quarterly"
        case .halfYearly: return "Half Yearly"
        case .trial:      return "Trial"
        }
    }
    var durationLabel: String {
        switch self {
        case .monthly:    return "per month"
        case .quarterly:  return "for 3 months"
        case .halfYearly: return "for 6 months"
        case .trial:      return "then ₹149/month"
        }
    }
    var priceRs: Int {
        switch self {
        case .monthly:    return 149
        case .quarterly:  return 249
        case .halfYearly: return 449
        case .trial:      return 1
        }
    }
    var originalPriceRs: Int? {
        switch self {
        case .quarterly:  return 447
        case .halfYearly: return 894
        default:          return nil
        }
    }
    var savingLabel: String? {
        switch self {
        case .quarterly:  return "Save 44%"
        case .halfYearly: return "Save 50%"
        case .trial:      return "Cancel anytime"
        default:          return nil
        }
    }
    var isMostPopular: Bool { self == .quarterly }
    var ctaOriginalLabel: String? {
        switch self {
        case .quarterly:  return "₹447"
        case .halfYearly: return "₹894"
        default:          return nil
        }
    }
    var ctaDiscountedLabel: String {
        switch self {
        case .monthly:    return "₹149/month"
        case .quarterly:  return "₹249"
        case .halfYearly: return "₹449"
        case .trial:      return "₹1"
        }
    }
    var systemIcon: String {
        switch self {
        case .monthly:    return "indianrupeesign.circle"
        case .quarterly:  return "chart.bar.fill"
        case .halfYearly: return "star.fill"
        case .trial:      return "indianrupeesign.circle"
        }
    }
    static func == (lhs: PaywallPlan, rhs: PaywallPlan) -> Bool {
        lhs.planType == rhs.planType
    }
}

struct PaywallCheckoutData: Equatable {
    let subscriptionId: String
    let keyId: String
    let userPhone: String?
    let userEmail: String?
}

@MainActor
final class PaywallViewModel: ObservableObject {
    @Published var selectedTab: PaywallTab = .trial
    @Published var selectedPlan: PaywallPlan = .trial
    @Published var isLoading = false
    @Published var isCheckingSubscription = false
    @Published var error: String? = nil
    @Published var navigateToHome = false
    @Published var checkoutData: PaywallCheckoutData? = nil
    @Published var paymentFailed = false
    @Published var paymentFailureReason = ""
    @Published var referralCode = ""
    @Published var referralApplied = false
    @Published var referralError: String? = nil

    private let functions = Functions.functions(region: "asia-south1")
    private let firestore = Firestore.firestore()

    init() {
        Task { await checkExistingSubscription() }
    }

    private func checkExistingSubscription() async {
        // AppState already checked subscription before routing here, so this is
        // a silent safety net for edge cases (e.g. subscribed on another device
        // after this session started).
        guard let userId = Auth.auth().currentUser?.uid else { return }
        do {
            let doc = try await firestore.collection("subscriptions").document(userId).getDocument()
            if doc.data()?["isPremium"] as? Bool == true {
                navigateToHome = true
            }
        } catch {}
    }

    func selectTab(_ tab: PaywallTab) {
        selectedTab = tab
        selectedPlan = (tab == .trial) ? .trial : .quarterly
        error = nil
    }

    func selectPlan(_ plan: PaywallPlan) {
        selectedPlan = plan
        error = nil
    }

    func applyReferralCode() {
        let trimmed = referralCode.trimmingCharacters(in: .whitespaces)
        guard !trimmed.isEmpty else {
            referralError = "Please enter a referral code"
            return
        }
        referralApplied = true
        referralError = nil
    }

    func removeReferralCode() {
        referralCode = ""
        referralApplied = false
        referralError = nil
    }

    func startCheckout() {
        guard let userId = Auth.auth().currentUser?.uid else {
            NSLog("[Paywall] startCheckout: no current user, aborting")
            return
        }
        let plan: PaywallPlan = (selectedTab == .trial) ? .trial : selectedPlan
        NSLog("[Paywall] startCheckout userId=%@ planType=%@ tab=%@", userId, plan.planType, selectedTab.rawValue)
        isLoading = true
        error = nil

        Task {
            do {
                var data: [String: Any] = [
                    "planType": plan.planType,
                    "userId":   userId,
                ]
                if referralApplied {
                    data["referralCode"] = referralCode.trimmingCharacters(in: .whitespaces)
                }
                NSLog("[Paywall] calling createSubscription cloud function...")
                let result = try await functions.httpsCallable("createSubscription").call(data)
                NSLog("[Paywall] createSubscription raw result: %@", String(describing: result.data))
                guard let dict = result.data as? [String: Any],
                      let subscriptionId = dict["subscriptionId"] as? String,
                      let keyId = dict["keyId"] as? String else {
                    NSLog("[Paywall] guard failed — unexpected data shape: %@", String(describing: result.data))
                    error = "Failed to create subscription. Please try again."
                    isLoading = false
                    return
                }
                NSLog("[Paywall] got subscriptionId=%@ keyId=%@", subscriptionId, keyId)
                checkoutData = PaywallCheckoutData(
                    subscriptionId: subscriptionId,
                    keyId: keyId,
                    userPhone: Auth.auth().currentUser?.phoneNumber,
                    userEmail: "\(userId)@eduksha.app"
                )
                isLoading = false
            } catch {
                NSLog("[Paywall] createSubscription error: %@", error.localizedDescription)
                self.error = error.localizedDescription
                isLoading = false
            }
        }
    }

    func onCheckoutConsumed() { checkoutData = nil }

    func onPaymentSuccess() {
        // Write isPremium immediately on the client. The webhook is the
        // authoritative source but may have latency or not be configured yet.
        // Both iOS and Android read subscriptions/{uid}.isPremium, so this
        // single Firestore write makes the subscription visible cross-platform.
        if let userId = Auth.auth().currentUser?.uid {
            firestore.collection("subscriptions").document(userId)
                .setData(["isPremium": true, "status": "active"], merge: true)
        }
        navigateToHome = true
    }

    func onPaymentError(_ description: String) {
        paymentFailureReason = description.isEmpty ? "Payment failed. Please try again." : description
        paymentFailed = true
    }

    func retryPayment() {
        paymentFailed = false
        startCheckout()
    }

    func dismissPaymentFailed() {
        paymentFailed = false
        paymentFailureReason = ""
    }

    func onNavigationConsumed() { navigateToHome = false }
}
