import SwiftUI

struct OTPVerificationView: View {
    let phone: String
    var onVerify: (String, @escaping (String?) -> Void) -> Void
    var onResend: () -> Void
    var onBack: () -> Void

    @State private var otp = ""
    @State private var isVerifying = false
    @State private var error: String? = nil
    @State private var resendCountdown = 30

    private let timer = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    var body: some View {
        ZStack(alignment: .top) {
            Color(hex: 0xF2F6FD).ignoresSafeArea()

            VStack(spacing: 0) {
                // Hero row
                HStack(alignment: .bottom, spacing: 0) {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Verify your\nnumber")
                            .font(.dobiTitle1())
                            .foregroundStyle(Color.dobiTextPrimary)
                        Text(maskedPhone)
                            .font(.dobiBodyMedium())
                            .foregroundStyle(Color.dobiPrimary)
                    }
                    .padding(.leading, 24)
                    .padding(.bottom, 28)
                    .frame(maxWidth: .infinity, alignment: .leading)

                    Text("🔐")
                        .font(.system(size: 72))
                        .padding(.trailing, 20)
                        .padding(.bottom, 8)
                }
                .padding(.top, 80)

                // Card
                VStack(spacing: 20) {
                    HStack(spacing: 10) {
                        Image(systemName: "shield.lefthalf.filled")
                            .foregroundStyle(Color.dobiPrimary)
                            .font(.system(size: 18))
                        Text("Enter OTP")
                            .font(.dobiTitle3())
                            .foregroundStyle(Color.dobiTextPrimary)
                        Spacer()
                    }

                    OTPInputRow(otp: $otp, isVerifying: isVerifying)
                        .onChange(of: otp) { new in
                            if new.count == 6 && !isVerifying {
                                isVerifying = true
                                error = nil
                                onVerify(new) { err in
                                    isVerifying = false
                                    if let err {
                                        error = err
                                        otp = ""
                                    }
                                }
                            }
                        }

                    if isVerifying {
                        HStack(spacing: 8) {
                            ProgressView().scaleEffect(0.8)
                            Text("Verifying…")
                                .font(.dobiCaption())
                                .foregroundStyle(Color.dobiTextSecondary)
                        }
                    }

                    if let error {
                        HStack(spacing: 8) {
                            Text("⚠️")
                            Text(error)
                                .font(.dobiCaptionMedium())
                                .foregroundStyle(Color.dobiError)
                        }
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(Color.dobiErrorContainer)
                        .clipShape(RoundedRectangle(cornerRadius: 10))
                    }

                    // Spam warning
                    HStack(alignment: .top, spacing: 8) {
                        Image(systemName: "lock.fill")
                            .font(.system(size: 12))
                            .foregroundStyle(Color.dobiTextTertiary)
                            .padding(.top, 2)
                        Text("Can't find the OTP? Check your spam/promotional SMS folder.")
                            .font(.dobiSmall())
                            .foregroundStyle(Color.dobiTextSecondary)
                            .lineSpacing(3)
                    }
                    .padding(12)
                    .background(Color.dobiSurfaceAlt)
                    .clipShape(RoundedRectangle(cornerRadius: 10))

                    // Resend
                    HStack(spacing: 6) {
                        Image(systemName: "arrow.clockwise")
                            .font(.system(size: 12))
                            .foregroundStyle(resendCountdown > 0 ? Color.dobiTextTertiary : Color.dobiPrimary)
                        if resendCountdown > 0 {
                            Text("Resend OTP in \(resendCountdown)s")
                                .font(.dobiCaptionMedium())
                                .foregroundStyle(Color.dobiTextTertiary)
                        } else {
                            Button("Resend OTP") {
                                resendCountdown = 30
                                otp = ""
                                isVerifying = false
                                onResend()
                            }
                            .font(.dobiCaptionMedium())
                            .foregroundStyle(Color.dobiPrimary)
                        }
                    }
                }
                .padding(24)
                .background(Color.white)
                .clipShape(UnevenRoundedRectangle(topLeadingRadius: 28, topTrailingRadius: 28))
                .ignoresSafeArea(edges: .bottom)
            }

            // Back button
            Button(action: onBack) {
                Circle()
                    .fill(Color.white)
                    .frame(width: 40, height: 40)
                    .shadow(color: .black.opacity(0.1), radius: 6, x: 0, y: 2)
                    .overlay(
                        Image(systemName: "arrow.left")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundStyle(Color.dobiTextPrimary)
                    )
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.leading, 20)
            .padding(.top, 54)
        }
        .onReceive(timer) { _ in
            if resendCountdown > 0 { resendCountdown -= 1 }
        }
        .navigationBarHidden(true)
    }

    private var maskedPhone: String {
        guard phone.count >= 10 else { return "+91 \(phone)" }
        let suffix = phone.suffix(4)
        return "+91 ••••••\(suffix)"
    }
}

#Preview {
    OTPVerificationView(
        phone: "9876543210",
        onVerify: { _, completion in completion(nil) },
        onResend: {},
        onBack: {}
    )
}
