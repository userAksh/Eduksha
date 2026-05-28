import SwiftUI

struct ChildProfile: Identifiable {
    let id: String
    let name: String
    let grade: String
    let board: String
    var gender: String = "male"  // "male" | "female" — from Firestore field
}

private let avatarEmojis   = ["👦", "👧", "🧒", "👶", "🐣"]
private let avatarBgColors: [Color] = [.dobiPastelSkyBlue, .dobiPastelMint, .dobiPastelLavender, .dobiPastelYellow, .dobiPastelPink]

struct SelectChildView: View {
    let children: [ChildProfile]
    var isLoading: Bool = false
    var error: String? = nil
    var onSelect: (ChildProfile) -> Void

    var body: some View {
        VStack(spacing: 0) {
            // ── Blue header ───────────────────────────────────────────────
            ZStack {
                Color.dobiPrimary.ignoresSafeArea(edges: .top)
                VStack(spacing: 10) {
                    Spacer().frame(height: 16)
                    RoundedRectangle(cornerRadius: 14)
                        .fill(Color.white.opacity(0.2))
                        .frame(width: 56, height: 56)
                        .overlay(Text("👨‍👩‍👧‍👦").font(.system(size: 32)))

                    Text("Select Profile")
                        .font(.dobiTitle2())
                        .foregroundStyle(.white)
                    Text("Who is using the app right now?")
                        .font(.dobiCaption())
                        .foregroundStyle(.white.opacity(0.8))
                    Spacer().frame(height: 16)
                }
            }
            .frame(height: 200)

            // ── Children list ─────────────────────────────────────────────
            ZStack {
                Color.dobiPageBg.ignoresSafeArea()

                if isLoading {
                    ProgressView().padding(.top, 60)
                } else if let error {
                    Text(error)
                        .font(.dobiBody())
                        .foregroundStyle(Color.dobiTextSecondary)
                        .padding(.top, 60)
                } else {
                    ScrollView {
                        VStack(spacing: 12) {
                            Spacer().frame(height: 20)
                            ForEach(Array(children.enumerated()), id: \.element.id) { index, child in
                                ChildCard(child: child, index: index, onTap: { onSelect(child) })
                            }
                            Spacer().frame(height: 24)
                        }
                        .padding(.horizontal, 20)
                    }
                }
            }
            .clipShape(UnevenRoundedRectangle(topLeadingRadius: 28, topTrailingRadius: 28))
            .ignoresSafeArea(edges: .bottom)
            .offset(y: -28)
        }
        .navigationBarHidden(true)
    }
}

private struct ChildCard: View {
    let child: ChildProfile
    let index: Int
    let onTap: () -> Void

    private var emoji: String { avatarEmojis[index % avatarEmojis.count] }
    private var bgColor: Color { avatarBgColors[index % avatarBgColors.count] }

    var body: some View {
        DobiCard(cornerRadius: 20) {
            HStack(spacing: 16) {
                EmojiAvatar(emoji: emoji, size: 60, background: bgColor)

                VStack(alignment: .leading, spacing: 6) {
                    Text(child.name)
                        .font(.dobiBodyBold())
                        .foregroundStyle(Color.dobiTextPrimary)
                    HStack(spacing: 6) {
                        DobiChip(label: "Class \(child.grade)", background: .dobiPastelLavender, foreground: Color(hex: 0x4B50B0))
                        DobiChip(label: child.board, background: .dobiPastelMint, foreground: Color(hex: 0x16733A))
                    }
                }

                Spacer()

                Circle()
                    .fill(bgColor)
                    .frame(width: 44, height: 44)
                    .overlay(
                        Image(systemName: "arrow.right")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundStyle(Color.dobiTextSecondary)
                    )
            }
            .padding(.horizontal, 18)
            .padding(.vertical, 16)
        }
        .onTapGesture { onTap() }
    }
}

#Preview {
    SelectChildView(
        children: [
            ChildProfile(id: "1", name: "Aarav Sharma", grade: "7", board: "CBSE"),
            ChildProfile(id: "2", name: "Ritika Gupta", grade: "6", board: "CBSE")
        ],
        onSelect: { _ in }
    )
}
