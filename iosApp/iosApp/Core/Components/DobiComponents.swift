import SwiftUI

// ── Primary gradient button ────────────────────────────────────────────────────

struct DobiPrimaryButton: View {
    let title: String
    var isLoading: Bool = false
    var isEnabled: Bool = true
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            ZStack {
                if isLoading {
                    ProgressView().tint(.white)
                } else {
                    HStack(spacing: 10) {
                        Text(title)
                            .font(.dobiBodyBold())
                            .foregroundStyle(.white)
                        Circle()
                            .fill(.white.opacity(0.25))
                            .frame(width: 30, height: 30)
                            .overlay(Image(systemName: "arrow.right").font(.system(size: 13, weight: .bold)).foregroundStyle(.white))
                    }
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: 52)
            .background(isEnabled ? Color.dobiPrimary : Color.dobiPrimaryLight.opacity(0.5))
            .clipShape(RoundedRectangle(cornerRadius: 16))
        }
        .disabled(!isEnabled || isLoading)
    }
}

// ── OTP input row ──────────────────────────────────────────────────────────────

struct OTPInputRow: View {
    @Binding var otp: String
    var digitCount: Int = 6
    var isVerifying: Bool = false
    @FocusState private var focused: Bool

    var body: some View {
        ZStack {
            // Hidden real TextField that captures input
            TextField("", text: $otp)
                .keyboardType(.numberPad)
                .focused($focused)
                .opacity(0)
                .frame(width: 1, height: 1)
                .onChange(of: otp) { new in
                    let digits = new.filter(\.isNumber)
                    if digits.count <= digitCount { otp = digits }
                }

            HStack(spacing: 8) {
                ForEach(0..<digitCount, id: \.self) { i in
                    let char: Character? = i < otp.count ? otp[otp.index(otp.startIndex, offsetBy: i)] : nil
                    let isCurrent = otp.count == i && !isVerifying

                    ZStack {
                        RoundedRectangle(cornerRadius: 12)
                            .fill(char != nil ? Color.dobiPrimaryContainer : Color.white)
                        RoundedRectangle(cornerRadius: 12)
                            .strokeBorder(
                                (isCurrent || char != nil) ? Color.dobiPrimary : Color.dobiBorder,
                                lineWidth: isCurrent ? 2 : 1.5
                            )
                        Text(char.map(String.init) ?? (isCurrent ? "·" : ""))
                            .font(char != nil ? .system(size: 18, weight: .bold) : .system(size: 22))
                            .foregroundStyle(char != nil ? Color.dobiPrimary : Color.dobiTextTertiary)
                    }
                    .frame(height: 52)
                }
            }
        }
        .onTapGesture { focused = true }
        .onAppear { focused = true }
    }
}

// ── Avatar circle ──────────────────────────────────────────────────────────────

struct EmojiAvatar: View {
    let emoji: String
    var size: CGFloat = 64
    var background: Color = .dobiPrimaryContainer

    var body: some View {
        ZStack {
            Circle().fill(background)
            Text(emoji).font(.system(size: size * 0.56))
        }
        .frame(width: size, height: size)
    }
}

// ── Chip ──────────────────────────────────────────────────────────────────────

struct DobiChip: View {
    let label: String
    var background: Color = Color.dobiPastelSkyBlue
    var foreground: Color = Color.dobiPrimary

    var body: some View {
        Text(label)
            .font(.dobiSmall())
            .fontWeight(.medium)
            .foregroundStyle(foreground)
            .padding(.horizontal, 10)
            .padding(.vertical, 4)
            .background(background)
            .clipShape(Capsule())
    }
}

// ── Card shell ────────────────────────────────────────────────────────────────

struct DobiCard<Content: View>: View {
    var cornerRadius: CGFloat = 20
    @ViewBuilder let content: Content

    var body: some View {
        content
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: cornerRadius))
            .shadow(color: Color(hex: 0x2563EB, alpha: 0.08), radius: 12, x: 0, y: 4)
    }
}

// ── Section header ────────────────────────────────────────────────────────────

struct SectionHeader: View {
    let title: String
    var action: String? = nil
    var onAction: (() -> Void)? = nil

    var body: some View {
        HStack {
            Text(title).font(.dobiTitle3()).foregroundStyle(Color.dobiTextPrimary)
            Spacer()
            if let action, let onAction {
                Button(action: onAction) {
                    Text(action).font(.dobiCaptionMedium()).foregroundStyle(Color.dobiPrimary)
                }
            }
        }
    }
}

// ── Dobi logo ─────────────────────────────────────────────────────────────────

struct DobiLogoRow: View {
    var body: some View {
        HStack(spacing: 10) {
            RoundedRectangle(cornerRadius: 12)
                .fill(Color.dobiPrimary)
                .frame(width: 44, height: 44)
                .overlay(Text("😊").font(.system(size: 26)))
            Text("Dobi")
                .font(.system(size: 22, weight: .bold))
                .foregroundStyle(Color.dobiPrimary)
        }
    }
}
