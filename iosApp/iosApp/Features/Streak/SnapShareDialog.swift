import SwiftUI

// MARK: - Model

private enum CardTheme { case blue, green, space }

private struct SnapTemplate: Identifiable {
    let id: Int
    let theme: CardTheme
    let gradTop: Color
    let gradBottom: Color
    let accentColor: Color
    let headline: String
    let streakEmoji: String
    let shareMessage: (Int, String) -> String
}

private extension SnapTemplate {
    static let all: [SnapTemplate] = [
        SnapTemplate(
            id: 0, theme: .blue,
            gradTop:     Color(red: 0.09, green: 0.20, blue: 0.55),
            gradBottom:  Color(red: 0.18, green: 0.36, blue: 1.00),
            accentColor: Color(red: 0.18, green: 0.36, blue: 1.00),
            headline: "Small steps today,\nbig progress tomorrow!",
            streakEmoji: "🔥",
            shareMessage: { (days: Int, name: String) -> String in
                "🏆 \(name) just hit a \(days)-day study streak on Eduksha! Small steps today, big progress tomorrow. 💪 #Eduksha #StudyStreak"
            }
        ),
        SnapTemplate(
            id: 1, theme: .green,
            gradTop:     Color(red: 0.08, green: 0.33, blue: 0.17),
            gradBottom:  Color(red: 0.18, green: 0.61, blue: 0.29),
            accentColor: Color(red: 0.13, green: 0.60, blue: 0.27),
            headline: "Consistency is\nbuilding something great.",
            streakEmoji: "🌱",
            shareMessage: { (days: Int, name: String) -> String in
                "🌿 \(name) has been learning for \(days) days straight on Eduksha! Consistency is the key. 🎯 #Eduksha #StudyStreak"
            }
        ),
        SnapTemplate(
            id: 2, theme: .space,
            gradTop:     Color(red: 0.07, green: 0.04, blue: 0.22),
            gradBottom:  Color(red: 0.18, green: 0.10, blue: 0.50),
            accentColor: Color(red: 0.48, green: 0.31, blue: 1.00),
            headline: "Every day counts.\nKeep reaching higher.",
            streakEmoji: "🚀",
            shareMessage: { (days: Int, name: String) -> String in
                "🚀 \(name) is on a \(days)-day streak on Eduksha — shooting for the stars! ⚡ #Eduksha #StudyStreak"
            }
        )
    ]
}

// MARK: - Overlay (full-screen)

struct SnapShareDialog: View {
    let streakDays: Int
    let childName: String
    let onDismiss: () -> Void
    let onShare: (String) -> Void

    @State private var page: Int = 1

    private var current: SnapTemplate { SnapTemplate.all[page] }

    var body: some View {
        ZStack {
            // Dark scrim — tap outside to dismiss
            Color.black.opacity(0.72)
                .ignoresSafeArea()
                .onTapGesture { onDismiss() }

            VStack(spacing: 0) {
                // Close row
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Share Your Streak")
                            .font(.system(size: 16, weight: .bold))
                            .foregroundStyle(.white)
                        Text("Pick a card and share with friends")
                            .font(.system(size: 12))
                            .foregroundStyle(.white.opacity(0.6))
                    }
                    Spacer()
                    Button(action: onDismiss) {
                        Image(systemName: "xmark")
                            .font(.system(size: 13, weight: .bold))
                            .foregroundStyle(.white)
                            .frame(width: 30, height: 30)
                            .background(.white.opacity(0.18))
                            .clipShape(Circle())
                    }
                    .buttonStyle(.plain)
                }
                .padding(.horizontal, 24)
                .padding(.bottom, 16)

                // Card carousel
                TabView(selection: $page) {
                    ForEach(SnapTemplate.all) { t in
                        ShareCard(template: t, streakDays: streakDays, childName: childName)
                            .padding(.horizontal, 12)
                            .tag(t.id)
                    }
                }
                .tabViewStyle(.page(indexDisplayMode: .never))
                .frame(height: 400)

                // Page dots
                HStack(spacing: 6) {
                    ForEach(SnapTemplate.all) { t in
                        Capsule()
                            .fill(t.id == page ? Color.white : Color.white.opacity(0.30))
                            .frame(width: t.id == page ? 18 : 6, height: 6)
                            .animation(.easeInOut(duration: 0.2), value: page)
                    }
                }
                .padding(.top, 14)

                // Share buttons
                HStack(spacing: 12) {
                    OverlayShareButton(
                        label: "WhatsApp",
                        icon: "💬",
                        bg: Color(red: 0.09, green: 0.51, blue: 0.25),
                        fg: .white
                    ) { onShare(current.shareMessage(streakDays, childName)) }

                    OverlayShareButton(
                        label: "Share",
                        icon: "↑",
                        bg: .white,
                        fg: Color(red: 0.10, green: 0.14, blue: 0.29),
                        isSystemIcon: true
                    ) { onShare(current.shareMessage(streakDays, childName)) }
                }
                .padding(.horizontal, 24)
                .padding(.top, 18)
            }
        }
    }
}

// MARK: - Card

private struct ShareCard: View {
    let template: SnapTemplate
    let streakDays: Int
    let childName: String

    private var filledDots: Int { min(streakDays, 7) }
    private let dayLabels = ["M", "T", "W", "T", "F", "S", "S"]

    var body: some View {
        VStack(spacing: 0) {
            // ── Top: gradient + illustration + streak number ──
            ZStack {
                LinearGradient(
                    colors: [template.gradTop, template.gradBottom],
                    startPoint: .top, endPoint: .bottom
                )

                Canvas { ctx, size in
                    switch template.theme {
                    case .blue:  drawMountains(ctx: ctx, size: size)
                    case .green: drawLeaves(ctx: ctx, size: size)
                    case .space: drawSpace(ctx: ctx, size: size)
                    }
                }

                // Streak number centred in the illustration
                VStack(spacing: 4) {
                    Text(template.streakEmoji)
                        .font(.system(size: 40))
                    HStack(alignment: .firstTextBaseline, spacing: 6) {
                        Text("\(streakDays)")
                            .font(.system(size: 56, weight: .black))
                            .foregroundStyle(.white)
                        Text("DAY\nSTREAK")
                            .font(.system(size: 13, weight: .bold))
                            .foregroundStyle(.white.opacity(0.80))
                            .lineSpacing(2)
                    }
                }
                .shadow(color: .black.opacity(0.35), radius: 8, y: 3)
            }
            .frame(height: 185)

            // ── Bottom: white info section ──
            VStack(alignment: .center, spacing: 0) {
                Spacer().frame(height: 18)

                // Week dots
                HStack(spacing: 0) {
                    ForEach(0..<7, id: \.self) { i in
                        let done = i < filledDots
                        VStack(spacing: 4) {
                            ZStack {
                                Circle()
                                    .fill(done ? template.accentColor : Color(red: 0.90, green: 0.92, blue: 0.96))
                                    .frame(width: 30, height: 30)
                                if done {
                                    Image(systemName: "checkmark")
                                        .font(.system(size: 12, weight: .bold))
                                        .foregroundStyle(.white)
                                }
                            }
                            Text(dayLabels[i])
                                .font(.system(size: 9, weight: .medium))
                                .foregroundStyle(Color(red: 0.42, green: 0.48, blue: 0.60))
                        }
                        .frame(maxWidth: .infinity)
                    }
                }
                .padding(.horizontal, 18)

                Spacer().frame(height: 16)

                // Divider
                Rectangle()
                    .fill(Color(red: 0.91, green: 0.93, blue: 0.97))
                    .frame(height: 1)
                    .padding(.horizontal, 18)

                Spacer().frame(height: 14)

                // Headline
                Text(template.headline)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(Color(red: 0.10, green: 0.17, blue: 0.29))
                    .multilineTextAlignment(.center)
                    .lineSpacing(3)
                    .padding(.horizontal, 20)

                Spacer().frame(height: 14)

                // Footer
                HStack(spacing: 5) {
                    Image(systemName: "lock.fill")
                        .font(.system(size: 9))
                        .foregroundStyle(Color(red: 0.62, green: 0.67, blue: 0.78))
                    Text(childName.isEmpty ? "Eduksha" : "\(childName) · Eduksha")
                        .font(.system(size: 11))
                        .foregroundStyle(Color(red: 0.62, green: 0.67, blue: 0.78))
                }

                Spacer().frame(height: 16)
            }
            .frame(maxWidth: .infinity)
            .background(Color.white)
        }
        .clipShape(RoundedRectangle(cornerRadius: 22))
        .shadow(color: .black.opacity(0.28), radius: 20, y: 8)
    }
}

// MARK: - Overlay share button

private struct OverlayShareButton: View {
    let label: String
    let icon: String
    let bg: Color
    let fg: Color
    var isSystemIcon: Bool = false
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                if isSystemIcon {
                    Image(systemName: "square.and.arrow.up")
                        .font(.system(size: 16, weight: .semibold))
                } else {
                    Text(icon).font(.system(size: 18))
                }
                Text(label)
                    .font(.system(size: 14, weight: .semibold))
            }
            .foregroundStyle(fg)
            .frame(maxWidth: .infinity)
            .frame(height: 50)
            .background(bg)
            .clipShape(RoundedRectangle(cornerRadius: 14))
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Canvas illustrations

private func drawMountains(ctx: GraphicsContext, size: CGSize) {
    let w = size.width, h = size.height
    let white = Color.white

    // Clouds
    for (cx, cy, r) in [(w*0.10, h*0.18, w*0.07), (w*0.20, h*0.12, w*0.05), (w*0.55, h*0.10, w*0.04)] {
        var p = Path(); p.addEllipse(in: CGRect(x: cx-r, y: cy-r, width: r*2, height: r*2))
        ctx.fill(p, with: .color(white.opacity(0.15)))
    }

    // Sun glow + disc
    let sCx = w*0.82, sCy = h*0.22, sR = w*0.09
    var glow = Path(); glow.addEllipse(in: CGRect(x: sCx-sR*1.6, y: sCy-sR*1.6, width: sR*3.2, height: sR*3.2))
    ctx.fill(glow, with: .color(Color(red: 1, green: 0.65, blue: 0.16).opacity(0.30)))
    var disc = Path(); disc.addEllipse(in: CGRect(x: sCx-sR, y: sCy-sR, width: sR*2, height: sR*2))
    ctx.fill(disc, with: .color(Color(red: 1, green: 0.72, blue: 0.16).opacity(0.90)))

    // Mountain 1 (back, lighter)
    var m1 = Path()
    m1.move(to: CGPoint(x: 0, y: h))
    m1.addLine(to: CGPoint(x: w*0.18, y: h*0.38))
    m1.addQuadCurve(to: CGPoint(x: w*0.50, y: h*0.38), control: CGPoint(x: w*0.34, y: h*0.16))
    m1.addLine(to: CGPoint(x: w*0.72, y: h)); m1.closeSubpath()
    ctx.fill(m1, with: .color(white.opacity(0.12)))

    // Mountain 2 (front, more visible)
    var m2 = Path()
    m2.move(to: CGPoint(x: w*0.35, y: h))
    m2.addLine(to: CGPoint(x: w*0.55, y: h*0.32))
    m2.addQuadCurve(to: CGPoint(x: w*0.85, y: h*0.32), control: CGPoint(x: w*0.70, y: h*0.12))
    m2.addLine(to: CGPoint(x: w*1.05, y: h)); m2.closeSubpath()
    ctx.fill(m2, with: .color(white.opacity(0.16)))

    // Snow caps
    for (px, py, lx, rx) in [
        (w*0.34, h*0.16, w*0.28, w*0.40),
        (w*0.70, h*0.12, w*0.63, w*0.77)
    ] {
        var s = Path()
        s.move(to: CGPoint(x: px, y: py))
        s.addLine(to: CGPoint(x: lx, y: py + h*0.10))
        s.addLine(to: CGPoint(x: rx, y: py + h*0.10))
        s.closeSubpath()
        ctx.fill(s, with: .color(white.opacity(0.55)))
    }
}

private func drawLeaves(ctx: GraphicsContext, size: CGSize) {
    let w = size.width, h = size.height

    struct L { let cx, cy, rx, ry, angle: CGFloat; let alpha: Double }
    let leaves: [L] = [
        L(cx: w*0.85, cy: h*0.08, rx: w*0.06, ry: w*0.13, angle: -30, alpha: 0.35),
        L(cx: w*0.95, cy: h*0.22, rx: w*0.05, ry: w*0.10, angle: -65, alpha: 0.28),
        L(cx: w*0.78, cy: h*0.20, rx: w*0.04, ry: w*0.08, angle: -10, alpha: 0.22),
        L(cx: w*0.06, cy: h*0.10, rx: w*0.04, ry: w*0.09, angle:  50, alpha: 0.28),
        L(cx: w*0.14, cy: h*0.02, rx: w*0.03, ry: w*0.07, angle: -20, alpha: 0.22),
        L(cx: w*0.92, cy: h*0.88, rx: w*0.04, ry: w*0.09, angle:  40, alpha: 0.22),
    ]

    for leaf in leaves {
        var lCtx = ctx
        lCtx.translateBy(x: leaf.cx, y: leaf.cy)
        lCtx.rotate(by: Angle.degrees(leaf.angle))
        lCtx.translateBy(x: -leaf.cx, y: -leaf.cy)

        var oval = Path()
        oval.addEllipse(in: CGRect(x: leaf.cx-leaf.rx, y: leaf.cy-leaf.ry, width: leaf.rx*2, height: leaf.ry*2))
        lCtx.fill(oval, with: .color(Color.white.opacity(leaf.alpha)))

        lCtx.stroke(
            Path { p in
                p.move(to: CGPoint(x: leaf.cx, y: leaf.cy - leaf.ry*0.7))
                p.addLine(to: CGPoint(x: leaf.cx, y: leaf.cy + leaf.ry*0.7))
            },
            with: .color(Color.white.opacity(0.12)),
            style: StrokeStyle(lineWidth: leaf.rx*0.18, lineCap: .round)
        )
    }

    // Ground arc at bottom
    var arc = Path()
    arc.addEllipse(in: CGRect(x: w*(-0.1), y: h*0.75, width: w*1.2, height: h*0.50))
    ctx.fill(arc, with: .color(Color.white.opacity(0.08)))
}

private func drawSpace(ctx: GraphicsContext, size: CGSize) {
    let w = size.width, h = size.height

    // Stars
    let stars: [(CGFloat, CGFloat, CGFloat)] = [
        (0.08,0.10,2),(0.22,0.18,3),(0.38,0.06,2),(0.60,0.15,3),(0.78,0.08,2),
        (0.92,0.22,2),(0.15,0.50,2),(0.50,0.35,3),(0.88,0.55,2),(0.30,0.70,2),
        (0.70,0.80,3),(0.45,0.88,2),(0.12,0.82,2),(0.65,0.06,2),(0.85,0.40,2),
    ]
    for (fx, fy, r) in stars {
        var p = Path(); p.addEllipse(in: CGRect(x: w*fx-r, y: h*fy-r, width: r*2, height: r*2))
        ctx.fill(p, with: .color(Color.white.opacity(0.70)))
    }

    // Planet (top-left)
    let pCx = w*0.16, pCy = h*0.22, pR = w*0.09
    var planet = Path(); planet.addEllipse(in: CGRect(x: pCx-pR, y: pCy-pR, width: pR*2, height: pR*2))
    ctx.fill(planet, with: .color(Color.white.opacity(0.22)))
    var ring = Path(); ring.addEllipse(in: CGRect(x: pCx-pR*1.7, y: pCy-pR*0.28, width: pR*3.4, height: pR*0.56))
    ctx.stroke(ring, with: .color(Color.white.opacity(0.30)), style: StrokeStyle(lineWidth: pR*0.18))

    // Rocket (top-right)
    let rCx = w*0.82, rCy = h*0.20, bh = h*0.12, bw = w*0.07
    var rCtx = ctx
    rCtx.translateBy(x: rCx, y: rCy)
    rCtx.rotate(by: Angle.degrees(-38))
    rCtx.translateBy(x: -rCx, y: -rCy)

    var body = Path()
    body.addRoundedRect(in: CGRect(x: rCx-bw/2, y: rCy-bh/2, width: bw, height: bh),
                        cornerSize: CGSize(width: bw*0.38, height: bw*0.38))
    rCtx.fill(body, with: .color(Color.white.opacity(0.90)))

    var nose = Path()
    nose.move(to: CGPoint(x: rCx, y: rCy-bh/2-bw*0.55))
    nose.addLine(to: CGPoint(x: rCx+bw/2, y: rCy-bh/2+bw*0.10))
    nose.addLine(to: CGPoint(x: rCx-bw/2, y: rCy-bh/2+bw*0.10))
    nose.closeSubpath()
    rCtx.fill(nose, with: .color(Color(red: 1, green: 0.34, blue: 0.13).opacity(0.90)))

    var flame = Path()
    flame.move(to: CGPoint(x: rCx-bw*0.32, y: rCy+bh/2))
    flame.addLine(to: CGPoint(x: rCx, y: rCy+bh/2+bw*0.75))
    flame.addLine(to: CGPoint(x: rCx+bw*0.32, y: rCy+bh/2))
    flame.closeSubpath()
    rCtx.fill(flame, with: .color(Color(red: 1, green: 0.60, blue: 0.10).opacity(0.88)))
}
