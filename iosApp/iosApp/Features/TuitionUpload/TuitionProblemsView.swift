import SwiftUI

private let problemOptions = [
    "Did not understand class",
    "Topic not covered properly",
    "Need help in subject",
    "Doubts not solved",
    "Too crowded, not getting attention",
]

struct TuitionProblemsView: View {
    var onBack: () -> Void = {}

    @State private var selected: Set<String> = []
    @State private var otherText = ""
    @State private var isSending = false
    @State private var sent = false

    private let accent = Color(hex: 0x7C3AED)

    private var canSend: Bool { (!selected.isEmpty || !otherText.trimmingCharacters(in: .whitespaces).isEmpty) && !isSending }

    var body: some View {
        ZStack {
            if sent {
                SentStateView(accent: accent)
                    .transition(.opacity)
            } else {
                formBody
                    .transition(.opacity)
            }
        }
        .animation(.easeInOut(duration: 0.3), value: sent)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.dobiPageBg.ignoresSafeArea())
        .navigationBarHidden(true)
        .onChange(of: sent) { isSent in
            if isSent {
                Task {
                    try? await Task.sleep(nanoseconds: 1_400_000_000)
                    await MainActor.run { onBack() }
                }
            }
        }
    }

    private var formBody: some View {
        VStack(spacing: 0) {
            HStack {
                Button(action: onBack) {
                    Image(systemName: "arrow.left")
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundStyle(Color.dobiTextPrimary)
                        .frame(width: 36, height: 36)
                        .background(Color.white)
                        .clipShape(Circle())
                        .shadow(color: .black.opacity(0.08), radius: 4, x: 0, y: 2)
                }
                Spacer()
                Text("Tuition Problems")
                    .font(.dobiTitle3())
                    .foregroundStyle(Color.dobiTextPrimary)
                Spacer()
                Color.clear.frame(width: 36, height: 36)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(Color.dobiPageBg)

            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    // Header
                    HStack(spacing: 12) {
                        Text("🤔").font(.system(size: 28))
                        VStack(alignment: .leading, spacing: 2) {
                            Text("What's bothering you?")
                                .font(.system(size: 15, weight: .bold))
                                .foregroundStyle(Color.dobiTextPrimary)
                            Text("Tap whatever feels true. Your parents will see this.")
                                .font(.system(size: 12))
                                .foregroundStyle(Color.dobiTextSecondary)
                        }
                        Spacer()
                    }
                    .padding(.horizontal, 14)
                    .padding(.vertical, 14)
                    .background(accent.opacity(0.10))
                    .clipShape(RoundedRectangle(cornerRadius: 14))
                    .padding(.top, 16)

                    VStack(spacing: 10) {
                        ForEach(problemOptions, id: \.self) { option in
                            ProblemRow(
                                label: option,
                                selected: selected.contains(option),
                                accent: accent,
                                onToggle: {
                                    if selected.contains(option) { selected.remove(option) }
                                    else { selected.insert(option) }
                                }
                            )
                        }
                    }
                    .padding(.top, 20)

                    Text("Anything else? (optional)")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(Color.dobiTextPrimary)
                        .padding(.top, 24)

                    Text("Tell your parents in your own words if your problem isn't above.")
                        .font(.system(size: 12))
                        .foregroundStyle(Color.dobiTextSecondary)
                        .padding(.top, 2)

                    ZStack(alignment: .topLeading) {
                        if otherText.isEmpty {
                            Text("Write what's on your mind…")
                                .font(.system(size: 15))
                                .foregroundStyle(Color.dobiTextSecondary)
                                .padding(.horizontal, 14)
                                .padding(.vertical, 13)
                        }
                        TextEditor(text: $otherText)
                            .font(.system(size: 15))
                            .foregroundStyle(Color.dobiTextPrimary)
                            .scrollContentBackground(.hidden)
                            .frame(height: 112)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 8)
                            .onChange(of: otherText) { val in
                                if val.count > 280 { otherText = String(val.prefix(280)) }
                            }
                    }
                    .background(Color.white)
                    .overlay(
                        RoundedRectangle(cornerRadius: 14)
                            .stroke(otherText.isEmpty ? Color(hex: 0xE2E8F0) : accent, lineWidth: otherText.isEmpty ? 1 : 1.5)
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 14))
                    .padding(.top, 8)

                    Text("\(otherText.count)/280")
                        .font(.system(size: 11))
                        .foregroundStyle(Color.dobiTextSecondary)
                        .frame(maxWidth: .infinity, alignment: .trailing)
                        .padding(.top, 4)

                    Button(action: handleSend) {
                        ZStack {
                            if isSending {
                                ProgressView().scaleEffect(0.9).tint(Color.white)
                            } else {
                                Text("Tell your parents")
                                    .font(.system(size: 15, weight: .semibold))
                                    .foregroundStyle(Color.white)
                            }
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                        .background(canSend ? accent : Color(hex: 0xDDD6FE))
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                    }
                    .buttonStyle(.plain)
                    .disabled(!canSend)
                    .padding(.top, 24)

                    if !canSend && !isSending {
                        Text("Pick at least one — or write your own — to continue.")
                            .font(.system(size: 12))
                            .foregroundStyle(Color.dobiTextSecondary)
                            .padding(.top, 10)
                    }

                    Spacer().frame(height: 40)
                }
                .padding(.horizontal, 20)
                .padding(.top, 4)
            }
        }
    }

    private func handleSend() {
        guard canSend else { return }
        isSending = true
        Task {
            try? await Task.sleep(nanoseconds: 400_000_000)
            await MainActor.run { isSending = false; sent = true }
        }
    }
}

private struct ProblemRow: View {
    let label: String
    let selected: Bool
    let accent: Color
    let onToggle: () -> Void

    var body: some View {
        Button(action: onToggle) {
            HStack(spacing: 14) {
                ZStack {
                    RoundedRectangle(cornerRadius: 7)
                        .fill(selected ? accent : Color.white)
                        .frame(width: 24, height: 24)
                    RoundedRectangle(cornerRadius: 7)
                        .stroke(selected ? accent : Color(hex: 0xE2E8F0), lineWidth: 1.5)
                        .frame(width: 24, height: 24)
                    if selected {
                        Image(systemName: "checkmark")
                            .font(.system(size: 12, weight: .bold))
                            .foregroundStyle(Color.white)
                    }
                }
                Text(label)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(Color.dobiTextPrimary)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 14)
            .background(selected ? accent.opacity(0.08) : Color.white)
            .overlay(
                RoundedRectangle(cornerRadius: 14)
                    .stroke(selected ? accent : Color(hex: 0xE2E8F0), lineWidth: 1.5)
            )
            .clipShape(RoundedRectangle(cornerRadius: 14))
        }
        .buttonStyle(.plain)
        .animation(.spring(duration: 0.2), value: selected)
    }
}

private struct SentStateView: View {
    let accent: Color

    var body: some View {
        VStack(spacing: 20) {
            ZStack {
                Circle()
                    .fill(accent.opacity(0.12))
                    .frame(width: 96, height: 96)
                Text("🎉").font(.system(size: 48))
            }
            Text("Sent to your parents")
                .font(.system(size: 18, weight: .bold))
                .foregroundStyle(Color.dobiTextPrimary)
            Text("They'll see this in their app and can talk to your tutor or you.")
                .font(.system(size: 14))
                .foregroundStyle(Color.dobiTextSecondary)
                .multilineTextAlignment(.center)
        }
        .padding(.horizontal, 32)
    }
}
