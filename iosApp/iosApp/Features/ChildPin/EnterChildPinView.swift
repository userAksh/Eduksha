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
            Color.white.ignoresSafeArea()

            VStack(spacing: 28) {
                // Lock icon — matches Android rounded-square container
                RoundedRectangle(cornerRadius: 20)
                    .fill(Color(hex: 0xDEECF8))
                    .frame(width: 96, height: 96)
                    .overlay(Text("🔒").font(.system(size: 48)))
                    .padding(.top, 24)

                // Child name + subtitle
                VStack(spacing: 6) {
                    Text(childName)
                        .font(.system(size: 28, weight: .bold))
                        .foregroundStyle(Color.dobiTextPrimary)
                    Text("Enter the 4-digit PIN to open this profile.")
                        .font(.dobiBody())
                        .foregroundStyle(Color.dobiTextSecondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 32)
                }

                // PIN boxes — square with rounded corners, matching Android style
                HStack(spacing: 16) {
                    ForEach(0..<4, id: \.self) { i in
                        ZStack {
                            RoundedRectangle(cornerRadius: 12)
                                .fill(Color.white)
                            RoundedRectangle(cornerRadius: 12)
                                .strokeBorder(
                                    i < pin.count ? Color.dobiPrimary : Color(hex: 0xCDD5E0),
                                    lineWidth: 2
                                )
                            if i < pin.count {
                                Circle()
                                    .fill(Color.dobiTextPrimary)
                                    .frame(width: 10, height: 10)
                            }
                        }
                        .frame(width: 68, height: 68)
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
                } else {
                    // Keep height stable so keypad doesn't jump
                    Color.clear.frame(height: 34)
                }

                // Forgot PIN — blue, matching Android
                Button("Forgot PIN?", action: onForgotPin)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(Color.dobiPrimary)

                // Custom keypad
                NumericKeypad { key in
                    handleKey(key)
                }

                Spacer()
            }
        }
        .navigationTitle("Enter PIN")
        .navigationBarTitleDisplayMode(.inline)
        .navigationBarBackButtonHidden(true)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button(action: onBack) {
                    Image(systemName: "arrow.left")
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundStyle(Color.dobiTextPrimary)
                }
            }
        }
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
        VStack(spacing: 12) {
            ForEach(rows, id: \.self) { row in
                HStack(spacing: 20) {
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
    NavigationStack {
        EnterChildPinView(
            childName: "Aftab",
            onVerify: { _, completion in completion(true) },
            onForgotPin: {},
            onBack: {}
        )
    }
}
