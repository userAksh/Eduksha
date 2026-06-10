import SwiftUI

struct PhoneAuthView: View {
    // Callback receives (name, phone, completion) where completion(nil) = success, completion(error) = show error
    var onSendOTP: (String, String, @escaping (String?) -> Void) -> Void

    @State private var name  = ""
    @State private var phone = ""
    @State private var isLoading = false
    @State private var error: String? = nil

    private var canSend: Bool { name.count >= 2 && phone.count == 10 && !isLoading }

    var body: some View {
        GeometryReader { geo in
            ScrollView {
                VStack(spacing: 0) {
                    // ── Hero ──────────────────────────────────────────────
                    ZStack(alignment: .bottomLeading) {
                        Color(hex: 0xF2F6FD)
                            .frame(height: geo.size.height * 0.38)

                        HStack(alignment: .bottom, spacing: 0) {
                            VStack(alignment: .leading, spacing: 10) {
                                Text("Partner in your\nchild's learning")
                                    .font(.dobiTitle1())
                                    .foregroundStyle(Color.dobiTextPrimary)
                                    .lineSpacing(4)
                                Text("Track progress, set goals,\nstay connected.")
                                    .font(.dobiCaption())
                                    .foregroundStyle(Color.dobiTextSecondary)
                                    .lineSpacing(4)
                            }
                            .padding(.leading, 24)
                            .padding(.bottom, 32)
                            .frame(maxWidth: .infinity, alignment: .leading)

                            Text("👩‍👧")
                                .font(.system(size: 80))
                                .padding(.trailing, 20)
                                .padding(.bottom, 12)
                        }
                        .frame(height: geo.size.height * 0.38, alignment: .bottom)
                    }

                    // ── Form card ─────────────────────────────────────────
                    VStack(spacing: 20) {
                        Text("Let's get started")
                            .font(.dobiTitle2())
                            .foregroundStyle(Color.dobiTextPrimary)
                            .frame(maxWidth: .infinity, alignment: .leading)

                        // Name field
                        HStack(spacing: 12) {
                            Image(systemName: "person")
                                .foregroundStyle(Color.dobiTextSecondary)
                                .frame(width: 20)
                            TextField(text: $name, prompt: .dobiPlaceholder("Your name")) {}
                                .font(.dobiBody())
                                .foregroundStyle(Color.dobiTextPrimary)
                                .autocorrectionDisabled()
                                .textContentType(.name)
                        }
                        .padding(16)
                        .background(Color.dobiSurfaceAlt)
                        .overlay(RoundedRectangle(cornerRadius: 14).strokeBorder(Color(hex: 0xE3E8F0), lineWidth: 1))
                        .clipShape(RoundedRectangle(cornerRadius: 14))

                        // Phone field
                        HStack(spacing: 0) {
                            HStack(spacing: 4) {
                                Image(systemName: "phone")
                                    .foregroundStyle(Color.dobiTextSecondary)
                                    .font(.system(size: 15))
                                Text("+91")
                                    .font(.dobiBodyMedium())
                                    .foregroundStyle(Color.dobiTextPrimary)
                                Image(systemName: "chevron.down")
                                    .font(.system(size: 10, weight: .medium))
                                    .foregroundStyle(Color.dobiTextTertiary)
                            }
                            .padding(.horizontal, 14)

                            Rectangle()
                                .fill(Color(hex: 0xE3E8F0))
                                .frame(width: 1, height: 30)

                            TextField(text: $phone, prompt: .dobiPlaceholder("10-digit mobile number")) {}
                                .font(.dobiBody())
                                .foregroundStyle(Color.dobiTextPrimary)
                                .keyboardType(.numberPad)
                                .padding(.horizontal, 14)
                                .onChange(of: phone) { new in
                                    phone = String(new.filter { $0.isNumber }.prefix(10))
                                }
                        }
                        .frame(height: 52)
                        .background(Color.dobiSurfaceAlt)
                        .overlay(RoundedRectangle(cornerRadius: 14).strokeBorder(Color(hex: 0xE3E8F0), lineWidth: 1))
                        .clipShape(RoundedRectangle(cornerRadius: 14))

                        if let error {
                            Text(error)
                                .font(.dobiCaptionMedium())
                                .foregroundStyle(Color.dobiError)
                                .frame(maxWidth: .infinity, alignment: .leading)
                        }

                        DobiPrimaryButton(
                            title: "Send OTP",
                            isLoading: isLoading,
                            isEnabled: canSend
                        ) {
                            isLoading = true
                            error = nil
                            onSendOTP(name, phone) { err in
                                isLoading = false
                                error = err
                            }
                        }

                        // Trust strip
                        HStack(spacing: 8) {
                            Image(systemName: "checkmark.seal.fill")
                                .foregroundStyle(Color.dobiSuccess)
                                .font(.system(size: 14))
                            Text("Your data is safe and never shared with anyone.")
                                .font(.dobiSmall())
                                .foregroundStyle(Color.dobiTextSecondary)
                        }
                        .padding(12)
                        .background(Color.dobiSuccessContainer)
                        .clipShape(RoundedRectangle(cornerRadius: 10))
                    }
                    .padding(24)
                    .background(Color.white)
                    .clipShape(UnevenRoundedRectangle(topLeadingRadius: 28, topTrailingRadius: 28))
                    .ignoresSafeArea(edges: .bottom)
                }
            }
            .ignoresSafeArea(edges: .top)
        }
        .background(Color.white)
    }
}

#Preview {
    PhoneAuthView(onSendOTP: { _, _, completion in completion(nil) })
}
