import SwiftUI

// MARK: - Newsprint palette (mirrors Android exactly)
private let newsprintPaper     = Color(hex: 0xFAFAF6)
private let newsprintPaperEdge = Color(hex: 0xE3E0D6)
private let newsprintInk       = Color(hex: 0x111111)
private let newsprintMuted     = Color(hex: 0x555555)

// MARK: - NewspaperOverlay

struct NewspaperOverlay: View {
    let stories: [CurrentAffairItem]
    let startIndex: Int
    var onDismiss: () -> Void

    @State private var currentIndex: Int

    init(stories: [CurrentAffairItem], startIndex: Int, onDismiss: @escaping () -> Void) {
        self.stories    = stories
        self.startIndex = startIndex
        self.onDismiss  = onDismiss
        _currentIndex   = State(initialValue: max(0, min(startIndex, max(0, stories.count - 1))))
    }

    private func goPrev() {
        withAnimation(.spring(duration: 0.35, bounce: 0.05)) {
            currentIndex = currentIndex > 0 ? currentIndex - 1 : stories.count - 1
        }
    }

    private func goNext() {
        withAnimation(.spring(duration: 0.35, bounce: 0.05)) {
            currentIndex = currentIndex < stories.count - 1 ? currentIndex + 1 : 0
        }
    }

    var body: some View {
        ZStack {
            // Scrim
            Color.black.opacity(0.70)
                .ignoresSafeArea()
                .onTapGesture { onDismiss() }

            VStack(spacing: 0) {
                // ── Newspaper card ─────────────────────────────────────────
                VStack(spacing: 0) {
                    NPMasthead(
                        storyCount: stories.count,
                        currentPage: currentIndex + 1,
                        totalPages: stories.count,
                        onDismiss: onDismiss
                    )

                    if !stories.isEmpty {
                        NPFlipPager(stories: stories, currentIndex: $currentIndex)
                    }

                    NPPageDots(currentPage: currentIndex, totalPages: stories.count)
                }
                .background(newsprintPaper)
                .clipShape(RoundedRectangle(cornerRadius: 6))
                .overlay(RoundedRectangle(cornerRadius: 6).strokeBorder(newsprintInk, lineWidth: 1.5))
                .shadow(color: .black.opacity(0.55), radius: 28, x: 0, y: 8)
                .layoutPriority(1)

                Spacer().frame(height: 14)

                // ── Navigation buttons below card ──────────────────────────
                HStack(spacing: 12) {
                    Button(action: goPrev) {
                        HStack(spacing: 8) {
                            Image(systemName: "arrow.left")
                                .font(.system(size: 15, weight: .bold))
                            Text("PREV")
                                .font(.system(size: 13, weight: .black, design: .serif))
                                .tracking(1.5)
                        }
                        .foregroundStyle(newsprintPaper)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 13)
                        .background(newsprintInk)
                        .clipShape(RoundedRectangle(cornerRadius: 6))
                    }
                    .buttonStyle(.plain)

                    Button(action: goNext) {
                        HStack(spacing: 8) {
                            Text("NEXT")
                                .font(.system(size: 13, weight: .black, design: .serif))
                                .tracking(1.5)
                            Image(systemName: "arrow.right")
                                .font(.system(size: 15, weight: .bold))
                        }
                        .foregroundStyle(newsprintPaper)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 13)
                        .background(newsprintInk)
                        .clipShape(RoundedRectangle(cornerRadius: 6))
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, 8)
            .padding(.top, 56)
            .padding(.bottom, 16)
        }
    }
}

// MARK: - Flip pager
// Mirrors Android HorizontalPager + graphicsLayer:
//   pageOffset > 0  → page is on top, rotating around left edge (–135° × offset)
//   pageOffset < 0  → page is behind/revealed flat
//   translationX    = pageOffset × width (stacked, not sliding)

private struct NPFlipPager: View {
    let stories: [CurrentAffairItem]
    @Binding var currentIndex: Int

    @State private var dragOffset: CGFloat = 0
    @State private var isHorizontalDrag: Bool? = nil

    var body: some View {
        GeometryReader { geo in
            let width  = geo.size.width
            let height = geo.size.height
            // Guard against zero/NaN geometry during initial layout — prevents
            // the "CALayer bounds contains NaN" crash in the ScrollView layer.
            if width > 0 && height > 0 {
                ZStack {
                    ForEach(stories.indices, id: \.self) { i in
                        let rawOffset = CGFloat(i - currentIndex) + dragOffset / width

                        if abs(CGFloat(i - currentIndex)) <= 1 {
                            NPStoryPage(
                                story: stories[i],
                                pageNumber: i + 1,
                                totalPages: stories.count
                            )
                            .frame(width: width, height: height)
                            .offset(x: rawOffset * width)
                            .zIndex(rawOffset >= 0 ? 1 : 0)
                            .rotation3DEffect(
                                Angle(degrees: rawOffset > 0
                                      ? Double(-135 * min(rawOffset, 1.0))
                                      : 0),
                                axis: (x: 0, y: 1, z: 0),
                                anchor: .leading,
                                perspective: 0.4
                            )
                            .opacity(rawOffset > 0 ? 1 - Double(min(rawOffset, 1.0)) * 0.3 : 1)
                            .scaleEffect(rawOffset > 0 ? 1 - CGFloat(min(rawOffset, 1.0)) * 0.05 : 1)
                        }
                    }
                }
                // Clip the ZStack (not individual pages) to avoid corrupted
                // per-page clipping rects during 3D rotation.
                .clipped()
                // simultaneousGesture lets ScrollView inside each page keep vertical scrolls.
                // Direction is locked after the first movement so diagonal drags don't bleed.
                .simultaneousGesture(
                    DragGesture(minimumDistance: 5)
                        .onChanged { value in
                            // Lock direction once determined
                            if isHorizontalDrag == nil {
                                isHorizontalDrag = abs(value.translation.width) > abs(value.translation.height)
                            }
                            guard isHorizontalDrag == true else { return }

                            var raw = value.translation.width
                            // Rubber-band resistance at edges — no blank page, but resists
                            let atFirst = currentIndex == 0 && raw > 0
                            let atLast  = currentIndex == stories.count - 1 && raw < 0
                            if atFirst || atLast { raw *= 0.15 }
                            dragOffset = raw
                        }
                        .onEnded { value in
                            let wasHorizontal = isHorizontalDrag == true
                            isHorizontalDrag = nil
                            withAnimation(.spring(duration: 0.35, bounce: 0.05)) {
                                if wasHorizontal {
                                    if value.translation.width < -(width * 0.25) {
                                        // Swipe left → next (wraps to 0 from last)
                                        currentIndex = currentIndex < stories.count - 1 ? currentIndex + 1 : 0
                                    } else if value.translation.width > width * 0.25 {
                                        // Swipe right → prev (wraps to last from first)
                                        currentIndex = currentIndex > 0 ? currentIndex - 1 : stories.count - 1
                                    }
                                }
                                dragOffset = 0
                            }
                        }
                )
            }
        }
    }
}

// MARK: - Story page (scrollable content)

private struct NPStoryPage: View {
    let story: CurrentAffairItem
    let pageNumber: Int
    let totalPages: Int

    var body: some View {
        ScrollView(.vertical, showsIndicators: false) {
            VStack(alignment: .leading, spacing: 0) {
                // Category strip
                HStack {
                    Text(story.category.uppercased())
                        .font(.system(size: 10, weight: .bold, design: .serif))
                        .foregroundStyle(newsprintPaper)
                        .tracking(1)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 3)
                        .background(newsprintInk)
                        .clipShape(RoundedRectangle(cornerRadius: 2))
                    Text("PAGE \(pageNumber) OF \(totalPages)")
                        .font(.system(size: 10, weight: .regular, design: .serif))
                        .foregroundStyle(newsprintMuted)
                        .tracking(0.8)
                }

                Spacer().frame(height: 12)

                // Headline
                Text(story.headline)
                    .font(.system(size: 24, weight: .black, design: .serif))
                    .foregroundStyle(newsprintInk)
                    .lineSpacing(3)
                    .fixedSize(horizontal: false, vertical: true)

                Spacer().frame(height: 8)

                Rectangle()
                    .fill(newsprintInk.opacity(0.4))
                    .frame(height: 1)

                Spacer().frame(height: 14)

                // Illustration frame
                ZStack {
                    LinearGradient(
                        colors: [newsprintInk.opacity(0.06), newsprintInk.opacity(0.12)],
                        startPoint: .top, endPoint: .bottom
                    )
                    Text(story.emoji).font(.system(size: 84))
                }
                .frame(maxWidth: .infinity)
                .frame(height: 140)
                .clipShape(RoundedRectangle(cornerRadius: 6))
                .overlay(RoundedRectangle(cornerRadius: 6).strokeBorder(newsprintInk.opacity(0.25), lineWidth: 1))

                Spacer().frame(height: 6)

                Text("— Illustration —")
                    .font(.system(size: 10, weight: .regular, design: .serif))
                    .italic()
                    .foregroundStyle(newsprintMuted)
                    .frame(maxWidth: .infinity, alignment: .center)

                Spacer().frame(height: 16)

                // Drop cap + body
                if !story.body.isEmpty {
                    HStack(alignment: .top, spacing: 6) {
                        Text(String(story.body.prefix(1)))
                            .font(.system(size: 52, weight: .black, design: .serif))
                            .foregroundStyle(newsprintInk)
                            .frame(height: 52)
                        Text(String(story.body.dropFirst()))
                            .font(.system(size: 15, design: .serif))
                            .foregroundStyle(newsprintInk)
                            .lineSpacing(4)
                            .fixedSize(horizontal: false, vertical: true)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                }

                Spacer().frame(height: 18)

                Rectangle()
                    .fill(newsprintInk.opacity(0.25))
                    .frame(height: 1)

                Spacer().frame(height: 8)

                Text("Curated for Eduksha readers  •  Stay curious!")
                    .font(.system(size: 11, weight: .regular, design: .serif))
                    .italic()
                    .foregroundStyle(newsprintMuted)
                    .frame(maxWidth: .infinity, alignment: .center)

                Spacer().frame(height: 20)
            }
            .padding(.horizontal, 18)
            .padding(.vertical, 18)
        }
        .background(newsprintPaper)
        .clipShape(RoundedRectangle(cornerRadius: 10))
        .overlay(RoundedRectangle(cornerRadius: 10).strokeBorder(newsprintInk.opacity(0.15), lineWidth: 1))
        .padding(.horizontal, 10)
        .padding(.vertical, 12)
        .shadow(color: .black.opacity(0.12), radius: 12, x: 0, y: 4)
    }
}

// MARK: - Masthead

private struct NPMasthead: View {
    let storyCount: Int
    let currentPage: Int
    let totalPages: Int
    let onDismiss: () -> Void

    private var romanEdition: String { toRoman(storyCount) }

    private var dateString: String {
        let f = DateFormatter()
        f.dateFormat = "EEEE, d MMMM yyyy"
        f.locale = Locale(identifier: "en_US")
        return f.string(from: Date()).uppercased()
    }

    var body: some View {
        VStack(spacing: 0) {
            Rectangle().fill(newsprintInk).frame(height: 1).padding(.horizontal, 14)
            Spacer().frame(height: 4)

            HStack {
                Text("VOL. I · NO. \(romanEdition)")
                    .font(.system(size: 9, weight: .semibold, design: .serif))
                    .foregroundStyle(newsprintInk).tracking(1.5)
                Spacer()
                Text("DAILY EDITION  ·  ₹0")
                    .font(.system(size: 9, weight: .semibold, design: .serif))
                    .foregroundStyle(newsprintInk).tracking(1.5)
            }
            .padding(.horizontal, 14)

            Spacer().frame(height: 4)

            // HStack layout avoids ZStack hit-testing issues with the close button
            HStack(alignment: .center, spacing: 0) {
                Color.clear.frame(width: 44, height: 1)  // balance spacer

                Spacer()

                VStack(spacing: 0) {
                    Text("The")
                        .font(.system(size: 14, weight: .regular, design: .serif)).italic()
                        .foregroundStyle(newsprintInk)
                    Text("EDUKSHA TIMES")
                        .font(.system(size: 28, weight: .black, design: .serif))
                        .foregroundStyle(newsprintInk).tracking(3)
                }

                Spacer()

                Button(action: onDismiss) {
                    Image(systemName: "xmark")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(newsprintInk)
                        .frame(width: 44, height: 44)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
            }
            .padding(.horizontal, 6)

            Spacer().frame(height: 2)

            Text("✦  ALL THE NEWS THAT'S KID TO PRINT  ✦")
                .font(.system(size: 9, weight: .regular, design: .serif)).italic()
                .foregroundStyle(newsprintMuted).tracking(2)
                .frame(maxWidth: .infinity, alignment: .center)

            Spacer().frame(height: 6)

            Rectangle().fill(newsprintInk).frame(height: 2.5).padding(.horizontal, 12)
            Spacer().frame(height: 2)
            Rectangle().fill(newsprintInk).frame(height: 0.7).padding(.horizontal, 12)
            Spacer().frame(height: 4)

            HStack {
                Text(dateString)
                    .font(.system(size: 9, weight: .semibold, design: .serif))
                    .foregroundStyle(newsprintInk).tracking(1.2)
                Spacer()
                Text("☀  CLEAR & CURIOUS")
                    .font(.system(size: 9, weight: .regular, design: .serif)).italic()
                    .foregroundStyle(newsprintMuted).tracking(1)
                Spacer().frame(width: 10)
                Text("PAGE \(currentPage) / \(totalPages)")
                    .font(.system(size: 9, weight: .semibold, design: .serif))
                    .foregroundStyle(newsprintInk).tracking(1.2)
            }
            .padding(.horizontal, 16)

            Spacer().frame(height: 3)
            Rectangle().fill(newsprintInk).frame(height: 0.7).padding(.horizontal, 14)
        }
        .padding(.top, 8)
        .padding(.bottom, 4)
        .background(newsprintPaper)
    }
}

// MARK: - Page dots bar (chevrons removed — external PREV/NEXT buttons handle navigation)

private struct NPPageDots: View {
    let currentPage: Int
    let totalPages: Int

    var body: some View {
        VStack(spacing: 0) {
            Rectangle().fill(newsprintInk.opacity(0.6)).frame(height: 0.7).padding(.horizontal, 12)

            HStack(spacing: 6) {
                ForEach(0..<totalPages, id: \.self) { i in
                    let active = i == currentPage
                    Circle()
                        .fill(active ? newsprintInk : newsprintInk.opacity(0.3))
                        .frame(width: active ? 8 : 6, height: active ? 8 : 6)
                        .animation(.spring(duration: 0.2), value: currentPage)
                }
            }
            .padding(.vertical, 12)
        }
        .background(newsprintPaper)
    }
}

// MARK: - Roman numeral helper

private func toRoman(_ n: Int) -> String {
    guard n > 0 else { return "I" }
    let vals:  [Int]    = [10, 9, 5, 4, 1]
    let syms:  [String] = ["X","IX","V","IV","I"]
    var num = n; var result = ""
    for (v, s) in zip(vals, syms) { while num >= v { result += s; num -= v } }
    return result
}

// MARK: - Category palette (used by CurrentAffairCard in ChildHomeView)

func categoryPalette(_ category: String) -> (accent: Color, tint: Color) {
    switch category.lowercased() {
    case "sports":                  return (Color(hex: 0x16A34A), Color(hex: 0xDCFCE7))
    case "science":                 return (Color(hex: 0x2563EB), Color(hex: 0xDBEAFE))
    case "space":                   return (Color(hex: 0x7C3AED), Color(hex: 0xEDE9FE))
    case "technology":              return (Color(hex: 0x0891B2), Color(hex: 0xCFFAFE))
    case "environment", "nature":   return (Color(hex: 0x15803D), Color(hex: 0xBBF7D0))
    case "world":                   return (Color(hex: 0xDC2626), Color(hex: 0xFEE2E2))
    case "fun fact":                return (Color(hex: 0xE67E22), Color(hex: 0xFFF4E0))
    default:                        return (Color(hex: 0x6B7A99), Color(hex: 0xF1F5F9))
    }
}
