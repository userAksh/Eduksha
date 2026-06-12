import SwiftUI
import AVKit
import FirebaseAuth
import Razorpay

// ── Paywall-local colors (mirror Android PaywallScreen.kt) ────────────────────
private let paywallPurple     = Color(hex: 0x6C3EE8)
private let paywallPurpleDark = Color(hex: 0x4A1DB5)
private let paywallPink       = Color(hex: 0xE8367A)
private let paywallPurpleSoft = Color(hex: 0xF0EBFF)
private let paywallBorder     = Color(hex: 0xE0D9F7)
private let paywallText       = Color(hex: 0x1A0A3B)
private let paywallTextSub    = Color(hex: 0x7B6FA0)

// ── Razorpay coordinator (bridges Swift delegate → ViewModel) ─────────────────
private final class RazorpayCoordinator: NSObject, RazorpayPaymentCompletionProtocol {
    var onSuccess: () -> Void
    var onError:   (String) -> Void
    private var rzp: RazorpayCheckout?

    init(onSuccess: @escaping () -> Void, onError: @escaping (String) -> Void) {
        self.onSuccess = onSuccess
        self.onError   = onError
    }

    func open(keyId: String, options: [String: Any]) {
        // Use keyWindow from the active foreground scene (iOS 15+ API)
        let keyWindow = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first { $0.activationState == .foregroundActive }?
            .keyWindow
        guard let rootVC = keyWindow?.rootViewController else {
            NSLog("[Razorpay] ERROR: no rootViewController found on active scene")
            DispatchQueue.main.async { self.onError("Could not open payment screen. Please try again.") }
            return
        }
        // Walk up to the topmost presented controller
        var topVC: UIViewController = rootVC
        while let presented = topVC.presentedViewController { topVC = presented }

        NSLog("[Razorpay] opening payment sheet vc=%@ keyId=%@", String(describing: type(of: topVC)), keyId)
        rzp = RazorpayCheckout.initWithKey(keyId, andDelegate: self)
        rzp?.open(options, displayController: topVC)
    }

    // Required by RazorpayPaymentCompletionProtocol
    func onPaymentSuccess(_ payment_id: String, andData response: [AnyHashable: Any]) {
        NSLog("[Razorpay] onPaymentSuccess paymentId=%@", payment_id)
        DispatchQueue.main.async { self.onSuccess() }
    }

    func onPaymentError(_ code: Int32, description str: String, andData response: [AnyHashable: Any]) {
        NSLog("[Razorpay] onPaymentError code=%d description=%@", code, str)
        DispatchQueue.main.async {
            // Code 0 = user dismissed / cancelled the checkout.
            // Treat this as a silent back-navigation, not a failure — no error
            // screen, user just lands back on the paywall to try again or change plan.
            if code == 0 {
                NSLog("[Razorpay] user cancelled checkout — no error screen")
                return
            }
            self.onError(str)
        }
    }
}

// ── Main PaywallView ──────────────────────────────────────────────────────────
struct PaywallView: View {
    var onNavigateToParentHome: () -> Void

    @StateObject private var viewModel = PaywallViewModel()
    @State private var coordinator: RazorpayCoordinator? = nil

    var body: some View {
        GeometryReader { geo in
            ZStack {
                // Background image (full screen)
                Image("paywall_bg")
                    .resizable()
                    .scaledToFill()
                    .ignoresSafeArea()

                if viewModel.isCheckingSubscription {
                    // Hold the card until we know the user isn't already subscribed,
                    // preventing a visible paywall flash before routing to parent home.
                    ProgressView()
                        .tint(.white)
                        .scaleEffect(1.4)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    VStack(spacing: 0) {
                        // Hero video — top 32% of screen height, muted loop
                        PaywallVideoPlayer()
                            .frame(height: geo.size.height * 0.32)
                            .clipped()

                        // Floating card — remaining 68%
                        PaywallCard(viewModel: viewModel)
                            .padding(.horizontal, 8)
                            .padding(.bottom, 10)
                    }
                }

                // Payment failure overlay — rendered in the same ZStack to
                // avoid UIKit presentation conflicts when Razorpay re-opens.
                if viewModel.paymentFailed {
                    PaymentFailedView(
                        reason: viewModel.paymentFailureReason,
                        onRetry: { viewModel.retryPayment() },
                        onChangePlan: { viewModel.dismissPaymentFailed() }
                    )
                    .transition(.opacity.animation(.easeInOut(duration: 0.2)))
                }
            }
        }
        .ignoresSafeArea()
        .navigationBarHidden(true)
        .onChange(of: viewModel.navigateToHome) { navigates in
            if navigates {
                viewModel.onNavigationConsumed()
                onNavigateToParentHome()
            }
        }
        .onChange(of: viewModel.checkoutData) { data in
            guard let data else { return }
            viewModel.onCheckoutConsumed()
            openRazorpay(data: data)
        }
    }

    private func openRazorpay(data: PaywallCheckoutData) {
        NSLog("[Paywall] openRazorpay subscriptionId=%@ keyId=%@", data.subscriptionId, data.keyId)
        let c = RazorpayCoordinator(
            onSuccess: { viewModel.onPaymentSuccess() },
            onError:   { viewModel.onPaymentError($0) }
        )
        coordinator = c

        var options: [String: Any] = [
            "name":            "Eduksha",
            "description":     "AI-powered learning for your child",
            "subscription_id": data.subscriptionId,
            "prefill": [
                "email":   data.userEmail ?? "",
                "contact": data.userPhone ?? "",
            ],
            "hidden": [
                "contact": true,
                "email":   true,
            ],
            "theme": ["color": "#6C3EE8"],
        ]
        c.open(keyId: data.keyId, options: options)
    }
}

// ── Card shell ────────────────────────────────────────────────────────────────
private struct PaywallCard: View {
    @ObservedObject var viewModel: PaywallViewModel

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                PlanTabRow(selected: viewModel.selectedTab, onSelect: viewModel.selectTab)
                    .padding(.top, 18)
                    .padding(.horizontal, 14)

                Spacer().frame(height: 16)

                Group {
                    if viewModel.selectedTab == .trial {
                        TrialCard()
                            .padding(.horizontal, 14)
                    } else {
                        HStack(spacing: 10) {
                            ForEach([PaywallPlan.monthly, .quarterly, .halfYearly], id: \.planType) { plan in
                                PlanCard(
                                    plan:     plan,
                                    selected: viewModel.selectedPlan == plan,
                                    onSelect: { viewModel.selectPlan(plan) }
                                )
                            }
                        }
                        .padding(.horizontal, 14)
                        .fixedSize(horizontal: false, vertical: true)
                    }
                }

                Spacer().frame(height: 20)

                ReferralCodeSection(viewModel: viewModel)
                    .padding(.horizontal, 14)

                Spacer().frame(height: 16)

                if let error = viewModel.error {
                    Text(error)
                        .font(.system(size: 13))
                        .foregroundColor(Color(hex: 0xEF4444))
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal, 14)
                        .padding(.bottom, 8)
                }

                SubscribeButton(
                    plan:      viewModel.selectedTab == .trial ? .trial : viewModel.selectedPlan,
                    isLoading: viewModel.isLoading,
                    onClick:   { viewModel.startCheckout() }
                )
                .padding(.horizontal, 14)

                Spacer().frame(height: 12)
                MoneyBackBanner().padding(.horizontal, 14)
                Spacer().frame(height: 12)
                SecurePaymentsFooter().padding(.horizontal, 14)
                Spacer().frame(height: 8)
            }
        }
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 24))
        .shadow(color: .black.opacity(0.12), radius: 12, x: 0, y: 4)
    }
}

// ── Tab row ───────────────────────────────────────────────────────────────────
private struct PlanTabRow: View {
    let selected: PaywallTab
    let onSelect: (PaywallTab) -> Void

    private let tabs: [PaywallTab] = [.autoPay, .trial]

    var body: some View {
        HStack(spacing: 4) {
            ForEach(tabs, id: \.rawValue) { tab in
                let isSelected = selected == tab
                Text(tab.displayLabel)
                    .font(.system(size: 10, weight: .bold))
                    .multilineTextAlignment(.center)
                    .foregroundColor(isSelected ? .white : paywallText)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 9)
                    .background(isSelected ? paywallPurple : Color.white)
                    .clipShape(RoundedRectangle(cornerRadius: 10))
                    .overlay(
                        RoundedRectangle(cornerRadius: 10)
                            .stroke(isSelected ? Color.clear : paywallBorder, lineWidth: 1)
                    )
                    .onTapGesture { onSelect(tab) }
            }
        }
        .padding(4)
        .background(paywallPurpleSoft)
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

// ── Plan card ─────────────────────────────────────────────────────────────────
private struct PlanCard: View {
    let plan:     PaywallPlan
    let selected: Bool
    let onSelect: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            // Icon circle
            ZStack {
                Circle()
                    .fill(selected ? paywallPurple : paywallPurpleSoft)
                    .frame(width: 32, height: 32)
                Image(systemName: plan.systemIcon)
                    .font(.system(size: 14))
                    .foregroundColor(selected ? .white : paywallPurple)
            }

            Spacer().frame(height: 6)

            // POPULAR badge slot — fixed height on all cards
            if plan.isMostPopular {
                Text("POPULAR")
                    .font(.system(size: 7, weight: .bold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 5)
                    .padding(.vertical, 2)
                    .background(paywallPurple)
                    .clipShape(RoundedRectangle(cornerRadius: 4))
            } else {
                Color.clear.frame(height: 15)
            }

            Spacer().frame(height: 4)

            Text(plan.label)
                .font(.system(size: 12, weight: .semibold))
                .foregroundColor(paywallText)
                .multilineTextAlignment(.center)
                .lineLimit(1)

            Spacer().frame(height: 4)

            // Original price slot — fixed height
            if let orig = plan.originalPriceRs {
                Text("₹\(orig)")
                    .font(.system(size: 10))
                    .foregroundColor(paywallTextSub)
                    .strikethrough()
                    .multilineTextAlignment(.center)
            } else {
                Color.clear.frame(height: 14)
            }

            Text("₹\(plan.priceRs)")
                .font(.system(size: 20, weight: .black))
                .foregroundColor(paywallText)
                .multilineTextAlignment(.center)

            Text(plan.durationLabel)
                .font(.system(size: 9))
                .foregroundColor(paywallTextSub)
                .multilineTextAlignment(.center)
                .lineSpacing(3)

            Spacer().frame(height: 6)

            // Saving badge slot — fixed height
            if let saving = plan.savingLabel {
                Text(saving)
                    .font(.system(size: 8, weight: .semibold))
                    .foregroundColor(selected ? .white : paywallPurple)
                    .padding(.horizontal, 5)
                    .padding(.vertical, 2)
                    .background(selected ? paywallPurple : Color(hex: 0xEDE8FF))
                    .clipShape(RoundedRectangle(cornerRadius: 4))
            } else {
                Color.clear.frame(height: 17)
            }

            Spacer()

            // Radio button
            ZStack {
                Circle()
                    .stroke(selected ? paywallPurple : Color(hex: 0xCCC0EE), lineWidth: 2)
                    .frame(width: 18, height: 18)
                if selected {
                    Circle()
                        .fill(paywallPurple)
                        .frame(width: 10, height: 10)
                }
            }
            .padding(.bottom, 4)
        }
        .padding(.vertical, 10)
        .padding(.horizontal, 6)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(selected ? Color(hex: 0xF3EEFF) : Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 14))
        .overlay(
            RoundedRectangle(cornerRadius: 14)
                .stroke(selected ? paywallPurple : paywallBorder, lineWidth: selected ? 2 : 1)
        )
        .onTapGesture { onSelect() }
    }
}

// ── Trial card ────────────────────────────────────────────────────────────────
private struct TrialCard: View {
    private let features = [
        "Auto-pay setup with first charge",
        "Cancel anytime before renewal",
        "Full access during trial",
    ]

    var body: some View {
        VStack(spacing: 0) {
            Text("Start with just")
                .font(.system(size: 14))
                .foregroundColor(paywallTextSub)
            Spacer().frame(height: 4)
            Text("₹1")
                .font(.system(size: 48, weight: .black))
                .foregroundColor(paywallPurple)
            Text("then ₹149/month")
                .font(.system(size: 14))
                .foregroundColor(paywallTextSub)
            Spacer().frame(height: 16)
            VStack(spacing: 8) {
                ForEach(features, id: \.self) { feature in
                    HStack(spacing: 10) {
                        Circle()
                            .fill(paywallPurple)
                            .frame(width: 6, height: 6)
                        Text(feature)
                            .font(.system(size: 13))
                            .foregroundColor(paywallText)
                        Spacer()
                    }
                }
            }
        }
        .padding(24)
        .frame(maxWidth: .infinity)
        .background(Color(hex: 0xF3EEFF))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(paywallPurple, lineWidth: 2)
        )
    }
}

// ── Subscribe CTA button ──────────────────────────────────────────────────────
private struct SubscribeButton: View {
    let plan:      PaywallPlan
    let isLoading: Bool
    let onClick:   () -> Void

    var body: some View {
        Button(action: onClick) {
            Group {
                if isLoading {
                    ProgressView()
                        .tint(.white)
                        .scaleEffect(0.9)
                } else if plan == .trial {
                    Text("⚡ START TRIAL FOR ₹1")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(.white)
                } else {
                    ctaText
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: 56)
        }
        .disabled(isLoading)
        .background(
            LinearGradient(
                colors: [paywallPurpleDark, paywallPurple, paywallPink],
                startPoint: .leading,
                endPoint: .trailing
            )
        )
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var ctaText: some View {
        HStack(spacing: 4) {
            Text("⚡ SUBSCRIBE NOW FOR ")
                .font(.system(size: 14, weight: .bold))
                .foregroundColor(.white)
            if let orig = plan.ctaOriginalLabel {
                Text(orig)
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(.white.opacity(0.65))
                    .strikethrough(color: .white.opacity(0.65))
            }
            Text(plan.ctaDiscountedLabel)
                .font(.system(size: 14, weight: .black))
                .foregroundColor(.white)
        }
    }
}

// ── Money back banner ─────────────────────────────────────────────────────────
private struct MoneyBackBanner: View {
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "shield.fill")
                .font(.system(size: 24))
                .foregroundColor(paywallPurple)
            VStack(alignment: .leading, spacing: 2) {
                Text("30-Day Money Back Guarantee")
                    .font(.system(size: 13, weight: .bold))
                    .foregroundColor(paywallText)
                Text("Not satisfied? Get a full refund — no questions asked.")
                    .font(.system(size: 11))
                    .foregroundColor(paywallTextSub)
                    .lineSpacing(3)
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(paywallPurpleSoft)
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

// ── Referral code section ─────────────────────────────────────────────────────
private struct ReferralCodeSection: View {
    @ObservedObject var viewModel: PaywallViewModel

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Have a referral code?")
                .font(.system(size: 13, weight: .semibold))
                .foregroundColor(paywallText)

            HStack(spacing: 8) {
                TextField("Enter referral code", text: $viewModel.referralCode)
                    .font(.system(size: 13))
                    .disabled(viewModel.referralApplied)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 10)
                    .background(Color.white)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(paywallBorder, lineWidth: 1))

                Button(viewModel.referralApplied ? "Remove" : "Apply") {
                    if viewModel.referralApplied { viewModel.removeReferralCode() }
                    else { viewModel.applyReferralCode() }
                }
                .font(.system(size: 13, weight: .semibold))
                .foregroundColor(.white)
                .padding(.horizontal, 14)
                .padding(.vertical, 10)
                .background(viewModel.referralApplied ? Color(hex: 0xEF4444) : paywallPurple)
                .clipShape(RoundedRectangle(cornerRadius: 12))
            }

            if let err = viewModel.referralError {
                Text(err).font(.system(size: 12)).foregroundColor(Color(hex: 0xEF4444))
            }
            if viewModel.referralApplied {
                HStack(spacing: 4) {
                    Image(systemName: "checkmark.circle.fill")
                        .font(.system(size: 14)).foregroundColor(Color(hex: 0x22C55E))
                    Text("Referral code applied successfully!")
                        .font(.system(size: 12)).foregroundColor(Color(hex: 0x22C55E))
                }
            }
        }
    }
}

// ── Secure payments footer ────────────────────────────────────────────────────
private struct SecurePaymentsFooter: View {
    var body: some View {
        HStack(spacing: 5) {
            Image(systemName: "lock.fill")
                .font(.system(size: 11))
                .foregroundColor(paywallTextSub)
            Text("Secure payments powered by ")
                .font(.system(size: 11))
                .foregroundColor(paywallTextSub)
            Text("Razorpay")
                .font(.system(size: 11, weight: .semibold))
                .foregroundColor(paywallPurple)
        }
        .frame(maxWidth: .infinity)
    }
}

// ── Paywall hero video (muted, looping) ──────────────────────────────────────

private let paywallVideoURL = URL(string:
    "https://firebasestorage.googleapis.com/v0/b/dobi-ac085.firebasestorage.app/o/paywall_video.mp4?alt=media&token=0e1eb69c-88de-4efa-b661-62bd1d7d8d29"
)!

private struct PaywallVideoPlayer: UIViewRepresentable {
    func makeUIView(context: Context) -> UIView {
        let player = AVPlayer(url: paywallVideoURL)
        player.volume = 0
        player.actionAtItemEnd = .none
        let view = LoopingPlayerView(player: player)
        player.play()
        return view
    }

    func updateUIView(_ uiView: UIView, context: Context) {}

    final class LoopingPlayerView: UIView {
        override class var layerClass: AnyClass { AVPlayerLayer.self }
        private var playerLayer: AVPlayerLayer { layer as! AVPlayerLayer }
        private var token: Any?

        init(player: AVPlayer) {
            super.init(frame: .zero)
            playerLayer.player = player
            playerLayer.videoGravity = .resizeAspectFill
            token = NotificationCenter.default.addObserver(
                forName: .AVPlayerItemDidPlayToEndTime,
                object: player.currentItem,
                queue: .main
            ) { _ in
                player.seek(to: .zero)
                player.play()
            }
        }

        required init?(coder: NSCoder) { fatalError() }

        deinit {
            if let token { NotificationCenter.default.removeObserver(token) }
        }
    }
}

// ── Payment failed overlay ─────────────────────────────────────────────────────
private struct PaymentFailedView: View {
    let reason: String
    let onRetry: () -> Void
    let onChangePlan: () -> Void

    var body: some View {
        ZStack {
            Color.black.opacity(0.55).ignoresSafeArea()

            VStack(spacing: 0) {
                Spacer()

                VStack(spacing: 24) {
                    // Icon
                    ZStack {
                        Circle()
                            .fill(Color(hex: 0xFEE2E2))
                            .frame(width: 72, height: 72)
                        Image(systemName: "xmark.circle.fill")
                            .font(.system(size: 36))
                            .foregroundColor(Color(hex: 0xEF4444))
                    }

                    VStack(spacing: 8) {
                        Text("Payment Failed")
                            .font(.system(size: 22, weight: .bold))
                            .foregroundColor(paywallText)
                        Text(reason)
                            .font(.system(size: 14))
                            .foregroundColor(paywallTextSub)
                            .multilineTextAlignment(.center)
                            .lineSpacing(4)
                            .padding(.horizontal, 8)
                    }

                    VStack(spacing: 12) {
                        // Try Again — primary
                        Button(action: onRetry) {
                            Text("Try Again")
                                .font(.system(size: 16, weight: .bold))
                                .foregroundColor(.white)
                                .frame(maxWidth: .infinity)
                                .frame(height: 52)
                        }
                        .background(
                            LinearGradient(
                                colors: [paywallPurpleDark, paywallPurple, paywallPink],
                                startPoint: .leading, endPoint: .trailing
                            )
                        )
                        .clipShape(RoundedRectangle(cornerRadius: 14))

                        // Change Plan — secondary
                        Button(action: onChangePlan) {
                            Text("Change Plan")
                                .font(.system(size: 16, weight: .semibold))
                                .foregroundColor(paywallPurple)
                                .frame(maxWidth: .infinity)
                                .frame(height: 52)
                        }
                        .overlay(
                            RoundedRectangle(cornerRadius: 14)
                                .stroke(paywallPurple, lineWidth: 1.5)
                        )
                    }
                }
                .padding(28)
                .background(Color.white)
                .clipShape(RoundedRectangle(cornerRadius: 28))
                .shadow(color: .black.opacity(0.15), radius: 20, x: 0, y: 8)
                .padding(.horizontal, 20)

                Spacer()
            }
        }
    }
}
