import SwiftUI

struct EnterChildPinView: View {
    let childName: String
    var onVerify: (String, @escaping (Bool) -> Void) -> Void
    var onForgotPin: () -> Void
    var onBack: () -> Void

    @State private var pin = ""
    @State private var isVerifying = false
    @State private var showError = false

    var body: some View {
        ZStack {
            Color.dobiPageBg.ignoresSafeArea()

            VStack(spacing: 32) {
                // Back button + greeting
                HStack {
                    Button(action: onBack) {
                        Image(systemName: "arrow.left")
                            .font(.system(size: 18, weight: .semibold))
                            .foregroundStyle(Color.dobiTextPrimary)
                            .frame(width: 40, height: 40)
                            .background(Color.white)
                            .clipShape(Circle())
                            .shadow(color: .black.opacity(0.08), radius: 6, x: 0, y: 2)
                    }
                    Spacer()
                }
                .padding(.horizontal, 20)
                .padding(.top, 56)

                VStack(spacing: 8) {
                    EmojiAvatar(emoji: "👦", size: 80, background: .dobiPrimaryContainer)
                    Text("Hi, \(childName)!")
                        .font(.dobiTitle1())
                        .foregroundStyle(Color.dobiTextPrimary)
                    Text("Enter your 4-digit PIN to continue")
                        .font(.dobiBody())
                        .foregroundStyle(Color.dobiTextSecondary)
                }

                // PIN dots
                HStack(spacing: 20) {
                    ForEach(0..<4, id: \.self) { i in
                        ZStack {
                            Circle()
                                .strokeBorder(i < pin.count ? Color.dobiPrimary : Color.dobiBorder, lineWidth: 2)
                                .frame(width: 56, height: 56)
                            if i < pin.count {
                                Circle()
                                    .fill(Color.dobiPrimary)
                                    .frame(width: 20, height: 20)
                            }
                        }
                    }
                }
                .animation(.spring(duration: 0.2), value: pin.count)

                if showError {
                    Text("Incorrect PIN. Please try again.")
                        .font(.dobiCaptionMedium())
                        .foregroundStyle(Color.dobiError)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(Color.dobiErrorContainer)
                        .clipShape(RoundedRectangle(cornerRadius: 10))
                }

                // Number pad
                NumericKeypad { key in
                    handleKey(key)
                }

                Button("Forgot PIN?", action: onForgotPin)
                    .font(.dobiCaptionMedium())
                    .foregroundStyle(Color.dobiTextSecondary)

                Spacer()
            }
        }
        .navigationBarHidden(true)
    }

    private func handleKey(_ key: KeypadKey) {
        switch key {
        case .digit(let d):
            guard pin.count < 4, !isVerifying else { return }
            pin += d
            showError = false
            if pin.count == 4 {
                isVerifying = true
                let entered = pin
                onVerify(entered) { success in
                    isVerifying = false
                    if !success {
                        showError = true
                        pin = ""
                    }
                }
            }
        case .backspace:
            guard !isVerifying else { return }
            if !pin.isEmpty { pin.removeLast() }
            showError = false
        }
    }
}

// ── Numeric keypad ────────────────────────────────────────────────────────────

enum KeypadKey {
    case digit(String)
    case backspace
}

private struct NumericKeypad: View {
    let onKey: (KeypadKey) -> Void
    private let rows = [["1","2","3"],["4","5","6"],["7","8","9"],["","0","⌫"]]

    var body: some View {
        VStack(spacing: 16) {
            ForEach(rows, id: \.self) { row in
                HStack(spacing: 24) {
                    ForEach(row, id: \.self) { label in
                        if label.isEmpty {
                            Color.clear.frame(width: 72, height: 72)
                        } else {
                            Button {
                                if label == "⌫" { onKey(.backspace) }
                                else { onKey(.digit(label)) }
                            } label: {
                                ZStack {
                                    Circle()
                                        .fill(label == "⌫" ? Color.dobiErrorContainer : Color.white)
                                        .frame(width: 72, height: 72)
                                        .shadow(color: .black.opacity(0.06), radius: 4, x: 0, y: 2)
                                    if label == "⌫" {
                                        Image(systemName: "delete.left")
                                            .font(.system(size: 18, weight: .medium))
                                            .foregroundStyle(Color.dobiError)
                                    } else {
                                        Text(label)
                                            .font(.system(size: 22, weight: .semibold))
                                            .foregroundStyle(Color.dobiTextPrimary)
                                    }
                                }
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
            }
        }
    }
}

#Preview {
    EnterChildPinView(
        childName: "Aarav",
        onVerify: { _, completion in completion(true) },
        onForgotPin: {},
        onBack: {}
    )
}
