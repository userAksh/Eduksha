import SwiftUI

struct RoleSelectionView: View {
    var onParentSelected: () -> Void
    var onChildSelected: () -> Void

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                Spacer().frame(height: 52)
                DobiLogoRow()
                Spacer().frame(height: 40)

                Text("Who is using the app?")
                    .font(.dobiLargeTitle())
                    .foregroundStyle(Color.dobiTextPrimary)
                    .multilineTextAlignment(.center)

                Spacer().frame(height: 8)

                Text("Choose your role to get started")
                    .font(.dobiBody())
                    .foregroundStyle(Color.dobiTextSecondary)

                Spacer().frame(height: 36)

                RoleCard(
                    emoji: "👦",
                    title: "Child",
                    description: "I want to track\nmy learning",
                    action: onChildSelected
                )

                Spacer().frame(height: 16)

                RoleCard(
                    emoji: "👩",
                    title: "Parent",
                    description: "I want to track my\nchild's learning",
                    action: onParentSelected
                )

                Spacer().frame(height: 32)
                DisclaimerRow()
                Spacer().frame(height: 36)
            }
            .padding(.horizontal, 24)
        }
        .background(Color.dobiBackground)
    }
}

// ── Role card ─────────────────────────────────────────────────────────────────

private struct RoleCard: View {
    let emoji: String
    let title: String
    let description: String
    let action: () -> Void

    var body: some View {
        DobiCard {
            HStack(spacing: 16) {
                EmojiAvatar(emoji: emoji, size: 84, background: .dobiPrimaryContainer)

                VStack(alignment: .leading, spacing: 4) {
                    Text("I am a")
                        .font(.dobiSmall())
                        .foregroundStyle(Color.dobiTextSecondary)
                    Text(title)
                        .font(.dobiTitle1())
                        .foregroundStyle(Color.dobiPrimary)
                    Text(description)
                        .font(.dobiSmall())
                        .foregroundStyle(Color.dobiTextSecondary)
                        .lineSpacing(4)
                }

                Spacer()

                Circle()
                    .fill(Color.dobiPrimary)
                    .frame(width: 44, height: 44)
                    .overlay(
                        Image(systemName: "arrow.right")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundStyle(.white)
                    )
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 20)
        }
        .onTapGesture { action() }
    }
}

// ── Disclaimer ────────────────────────────────────────────────────────────────

private struct DisclaimerRow: View {
    var body: some View {
        HStack(spacing: 5) {
            Text("🔒").font(.system(size: 13))
            Text("Parent login required. Children are added by parents.")
                .font(.dobiSmall())
                .foregroundStyle(Color.dobiTextSecondary)
                .multilineTextAlignment(.center)
        }
    }
}

#Preview {
    RoleSelectionView(onParentSelected: {}, onChildSelected: {})
}
