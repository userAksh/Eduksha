import SwiftUI

struct ChildProfile: Identifiable, Hashable {
    let id: String
    let name: String
    let grade: String
    let board: String
    var gender: String = "male"
    var pin: String = ""
}

private let cardPalette: [(bg: Color, accent: Color, emoji: String)] = [
    (Color(hex: 0xDBEAFE), Color(hex: 0x1D4ED8), "👦"),
    (Color(hex: 0xFCE7F3), Color(hex: 0xBE185D), "👧"),
    (Color(hex: 0xD1FAE5), Color(hex: 0x047857), "🧒"),
    (Color(hex: 0xFEF3C7), Color(hex: 0xB45309), "👦"),
    (Color(hex: 0xEDE9FE), Color(hex: 0x6D28D9), "👧"),
]

struct SelectChildView: View {
    let children: [ChildProfile]
    var isLoading: Bool = false
    var error: String? = nil
    var onSelect: (ChildProfile) -> Void

    var body: some View {
        // VStack(spacing: -28) makes the white sheet overlap the blue header by 28px
        // while keeping layout + touch targets aligned with the visual position.
        // Using offset(y: -28) instead would misalign the touch targets (offset moves
        // visual only; spacing moves both visual and layout).
        VStack(spacing: -28) {
            // Blue header
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

            ZStack {
                // Background with rounded top corners — applied as background, NOT clipShape,
                // so the ZStack's hit-testing rectangle stays unclipped for the ScrollView buttons.
                Color.dobiPageBg
                    .clipShape(UnevenRoundedRectangle(topLeadingRadius: 28, topTrailingRadius: 28))
                    .ignoresSafeArea(edges: .bottom)
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
                                ChildCard(child: child, index: index, onTap: {
                                    NSLog("[SelectChild] onSelect called for child id=\(child.id) name=\(child.name)")
                                    onSelect(child)
                                })
                            }
                            Spacer().frame(height: 24)
                        }
                        .padding(.horizontal, 20)
                    }
                    .onAppear { NSLog("[SelectChild] showing \(children.count) children") }
                }
            }
            .ignoresSafeArea(edges: .bottom)
            .zIndex(1)
        }
        .navigationBarHidden(true)
    }
}

private struct ChildCard: View {
    let child: ChildProfile
    let index: Int
    let onTap: () -> Void

    private var palette: (bg: Color, accent: Color, emoji: String) { cardPalette[index % cardPalette.count] }

    var body: some View {
        Button(action: {
            NSLog("[ChildCard] Button tapped for child=\(child.name)")
            onTap()
        }) {
            DobiCard(cornerRadius: 20) {
                HStack(spacing: 16) {
                    EmojiAvatar(emoji: palette.emoji, size: 60, background: palette.bg)

                    VStack(alignment: .leading, spacing: 6) {
                        Text(child.name)
                            .font(.dobiBodyBold())
                            .foregroundStyle(Color.dobiTextPrimary)
                        HStack(spacing: 6) {
                            DobiChip(label: "Class \(child.grade)", background: palette.bg, foreground: palette.accent)
                            DobiChip(label: child.board, background: palette.bg, foreground: palette.accent)
                        }
                    }

                    Spacer()

                    Circle()
                        .fill(palette.accent)
                        .frame(width: 38, height: 38)
                        .overlay(
                            Image(systemName: "arrow.right")
                                .font(.system(size: 14, weight: .semibold))
                                .foregroundStyle(.white)
                        )
                }
                .padding(.horizontal, 18)
                .padding(.vertical, 16)
                // Spacer() inside HStack is transparent — contentShape makes the full row tappable
                .contentShape(Rectangle())
            }
        }
        .buttonStyle(.plain)
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
