import SwiftUI

// ── Palette (mirrors Android exactly) ─────────────────────────────────────────
private let ScreenBg          = Color(hex: 0xFAFBFE)
private let CardBdr           = Color(hex: 0xEDF1F8)
private let HeadlineBg        = Color(hex: 0xEDF3FF)
private let HeadlineBdr       = Color(hex: 0xDCE5F4)
private let PosGreen          = Color(hex: 0x16A34A)
private let PosGreenSoft      = Color(hex: 0xE6F7EC)
private let PosBadgeBg        = Color(hex: 0xC9EFD3)
private let WarnAmber         = Color(hex: 0xB45309)
private let WarnAmberDeep     = Color(hex: 0xEA580C)
private let WarnAmberSoft     = Color(hex: 0xFFEFD9)
private let WarnBadgeBg       = Color(hex: 0xFAD1A8)
private let CritRed           = Color(hex: 0xDC2626)
private let SparkGreenBg      = Color(hex: 0xD7F2DF)
private let SparkAmberBg      = Color(hex: 0xFCE4C8)
private let SubjectTrack      = Color(hex: 0xE9ECF4)
private let InsPurple         = Color(hex: 0x7C5CF8)
private let InsPurpleBg       = Color(hex: 0xEAE3FF)
private let InsBlue           = Color(hex: 0x3B7BFF)
private let InsBlueSoft       = Color(hex: 0xE0EAFE)
private let InsTextPrim       = Color(hex: 0x1A2B4A)
private let InsTextSec        = Color(hex: 0x6B7A99)
private let InsTextTert       = Color(hex: 0xA0ADC2)

enum InsightsRange: CaseIterable {
    case week, month
    var label: String { self == .week ? "1 Week" : "1 Month" }
    var days: Int     { self == .week ? 7        : 30        }
}

// ── Entry point ────────────────────────────────────────────────────────────────

struct InsightsView: View {
    var childName: String = ""
    var childGrade: String = ""
    var childBoard: String = ""
    var childGender: String = "male"
    var engagement: ChildEngagement = .empty
    var weekKinds: [DayKind] = Array(repeating: .none, count: 7)
    var onBack: () -> Void = {}
    var onStudyGoals: () -> Void = {}
    var onSubjectTap: (String) -> Void = { _ in }

    @State private var range: InsightsRange = .week

    private var firstName: String { childName.components(separatedBy: " ").first ?? childName }

    // ── Derived stats from correctly bucketed week kinds ─────────────────────
    private var revisionSessions: Int {
        weekKinds.filter { $0 == .revision || $0 == .both }.count
    }
    private var totalAttempts: Int {
        weekKinds.filter { $0 != .none }.count
    }
    private var headlineText: String {
        let name = firstName.isEmpty ? "Your child" : firstName
        if let acc = engagement.accuracyPct, acc < 60, let weak = engagement.weakSubject {
            return "\(weak) could use a closer look — accuracy is \(acc)%."
        }
        if engagement.streakDays >= 3, (engagement.accuracyPct ?? 0) >= 60 {
            return "\(engagement.daysActiveThisWeek) active days this week — consistency is paying off."
        }
        if engagement.daysActiveThisWeek == 0 {
            return "No activity this week yet — encourage a short revision session."
        }
        return "Here's \(name)'s learning summary for this period."
    }
    private var headlinePositive: Bool {
        engagement.streakDays >= 3 && (engagement.accuracyPct ?? 0) >= 60
    }
    private var dateRangeLabel: String {
        let cal = Calendar.current
        let today = Date()
        guard let start = cal.date(byAdding: .day, value: -(range.days - 1), to: today) else { return "" }
        let fmt = DateFormatter(); fmt.dateFormat = "d MMM"
        return "\(fmt.string(from: start)) – \(fmt.string(from: today))"
    }
    private var recommendation: String {
        let name = firstName.isEmpty ? "your child" : firstName
        if let weak = engagement.weakSubject {
            return "Encourage \(name) to revise \(weak) twice this week and revisit key concepts."
        }
        if revisionSessions < 2 {
            return "Add 2 short revision sessions this week to improve retention."
        }
        if engagement.daysActiveThisWeek < 3 {
            return "Encourage short 10–15 minute daily study check-ins."
        }
        return "Keep the current rhythm. Regular revision every 2–3 days will help long-term retention."
    }

    var body: some View {
        ScreenBg.ignoresSafeArea()
            .overlay(
                ScrollView {
                    VStack(spacing: 14) {
                        RangeAndDateRow(range: $range, dateLabel: dateRangeLabel)
                        HeadlineCard(text: headlineText, positive: headlinePositive)
                        InsStatRow(
                            daysActive: "\(engagement.daysActiveThisWeek)/\(range.days)",
                            daysPos: engagement.daysActiveThisWeek >= (range.days / 2),
                            totalAttempts: "\(totalAttempts)",
                            attemptsPos: totalAttempts > 0,
                            avgAccuracy: engagement.accuracyPct.map { "\($0)%" } ?? "—",
                            accPos: (engagement.accuracyPct ?? 0) >= 60,
                            revisionSessions: "\(revisionSessions)",
                            revPos: revisionSessions >= 2
                        )
                        HighlightsRow(
                            weakSubject: engagement.weakSubject,
                            accuracyPct: engagement.accuracyPct,
                            daysActive: engagement.daysActiveThisWeek
                        )
                        SubjectPerformanceCard(
                            weakSubject: engagement.weakSubject,
                            accuracyPct: engagement.accuracyPct,
                            onSubjectTap: onSubjectTap
                        )
                        InsRecommendationCard(
                            text: recommendation,
                            weakSubject: engagement.weakSubject,
                            onAssign: onStudyGoals
                        )
                        InsPrivacyFooter()
                        Spacer().frame(height: 20)
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 12)
                }
                .safeAreaInset(edge: .top, spacing: 0) {
                    InsightsTopBar(
                        childName: childName,
                        childGrade: childGrade,
                        childBoard: childBoard,
                        childGender: childGender,
                        onBack: onBack
                    )
                }
            )
            .navigationBarHidden(true)
    }
}

// ── Top bar ───────────────────────────────────────────────────────────────────

private struct InsightsTopBar: View {
    let childName: String
    let childGrade: String
    let childBoard: String
    let childGender: String
    let onBack: () -> Void

    private var avatarImage: String {
        childGender.lowercased() == "female" || childGender.lowercased() == "girl"
            ? "dashboard_avatar_ritika" : "dashboard_avatar_aarav"
    }
    private var firstName: String { childName.components(separatedBy: " ").first ?? childName }

    var body: some View {
        HStack(spacing: 10) {
            // Back button — rounded rect, not circle (matches Android)
            Button(action: onBack) {
                Image(systemName: "arrow.left")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundStyle(InsTextPrim)
                    .frame(width: 40, height: 40)
                    .background(Color.white)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                    .overlay(RoundedRectangle(cornerRadius: 12).strokeBorder(CardBdr, lineWidth: 1))
            }
            .buttonStyle(.plain)

            // Avatar
            Image(avatarImage)
                .resizable()
                .scaledToFill()
                .frame(width: 40, height: 40)
                .clipShape(Circle())

            // Name + class
            VStack(alignment: .leading, spacing: 1) {
                Text(firstName.isEmpty ? "Learning Report" : "\(firstName)'s Learning Report")
                    .font(.system(size: 16, weight: .bold))
                    .foregroundStyle(InsTextPrim)
                    .lineLimit(1)
                if !childGrade.isEmpty {
                    Text("Class \(childGrade) · \(childBoard)")
                        .font(.system(size: 12))
                        .foregroundStyle(InsTextSec)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            // Calendar icon button (right side)
            ZStack {
                RoundedRectangle(cornerRadius: 12)
                    .fill(Color.white)
                    .overlay(RoundedRectangle(cornerRadius: 12).strokeBorder(CardBdr, lineWidth: 1))
                Image(systemName: "calendar")
                    .font(.system(size: 16, weight: .medium))
                    .foregroundStyle(InsTextPrim)
            }
            .frame(width: 40, height: 40)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 10)
        .background(ScreenBg.ignoresSafeArea(edges: .top))
    }
}

// ── Range selector + date label ───────────────────────────────────────────────

private struct RangeAndDateRow: View {
    @Binding var range: InsightsRange
    let dateLabel: String

    var body: some View {
        HStack {
            // Toggle pill
            HStack(spacing: 2) {
                ForEach(InsightsRange.allCases, id: \.label) { r in
                    RangeChip(label: r.label, selected: range == r) { range = r }
                }
            }
            .padding(3)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .overlay(RoundedRectangle(cornerRadius: 12).strokeBorder(CardBdr, lineWidth: 1))

            Spacer()

            if !dateLabel.isEmpty {
                Text(dateLabel)
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(InsTextPrim)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 7)
                    .background(Color.white)
                    .clipShape(Capsule())
                    .overlay(Capsule().strokeBorder(CardBdr, lineWidth: 1))
            }
        }
    }
}

private struct RangeChip: View {
    let label: String
    let selected: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 6) {
                Image(systemName: "calendar")
                    .font(.system(size: 12))
                    .foregroundStyle(selected ? InsBlue : InsTextSec)
                Text(label)
                    .font(.system(size: 13, weight: selected ? .bold : .medium))
                    .foregroundStyle(selected ? InsBlue : InsTextSec)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(selected ? InsBlueSoft : Color.clear)
            .clipShape(RoundedRectangle(cornerRadius: 10))
        }
        .buttonStyle(.plain)
    }
}

// ── Headline card ──────────────────────────────────────────────────────────────

private struct HeadlineCard: View {
    let text: String
    let positive: Bool

    var body: some View {
        HStack(spacing: 12) {
            ZStack {
                Circle().fill(Color.white)
                    .overlay(Circle().strokeBorder(HeadlineBdr, lineWidth: 1))
                    .frame(width: 46, height: 46)
                Image(systemName: "chart.line.uptrend.xyaxis")
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundStyle(InsBlue)
            }
            Text(text.isEmpty ? "Building this week's picture…" : text)
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(InsTextPrim)
                .lineSpacing(3)
                .frame(maxWidth: .infinity, alignment: .leading)
            if positive {
                Image(systemName: "trophy.fill")
                    .font(.system(size: 34))
                    .foregroundStyle(InsBlue)
            }
        }
        .padding(14)
        .background(HeadlineBg)
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .overlay(RoundedRectangle(cornerRadius: 20).strokeBorder(HeadlineBdr, lineWidth: 1))
    }
}

// ── Stat row (4 tiles) ────────────────────────────────────────────────────────

private struct InsStatRow: View {
    let daysActive: String;      let daysPos: Bool
    let totalAttempts: String;   let attemptsPos: Bool
    let avgAccuracy: String;     let accPos: Bool
    let revisionSessions: String; let revPos: Bool

    var body: some View {
        HStack(spacing: 8) {
            StatTile(
                icon: "calendar.badge.checkmark",
                iconBg: Color(hex: 0xE6F7EC), iconTint: PosGreen,
                value: daysActive, valueColor: PosGreen,
                label: "Days Active"
            )
            StatTile(
                icon: "pencil",
                iconBg: InsBlueSoft, iconTint: InsBlue,
                value: totalAttempts, valueColor: InsBlue,
                label: "Total Attempts"
            )
            StatTile(
                icon: "scope",
                iconBg: InsPurpleBg, iconTint: InsPurple,
                value: avgAccuracy, valueColor: InsPurple,
                label: "Avg Accuracy"
            )
            StatTile(
                icon: "arrow.clockwise",
                iconBg: WarnAmberSoft, iconTint: WarnAmberDeep,
                value: revisionSessions, valueColor: WarnAmberDeep,
                label: "Revision Sessions"
            )
        }
    }
}

private struct StatTile: View {
    let icon: String
    let iconBg: Color
    let iconTint: Color
    let value: String
    let valueColor: Color
    let label: String

    var body: some View {
        VStack(spacing: 0) {
            ZStack {
                Circle().fill(iconBg).frame(width: 32, height: 32)
                Image(systemName: icon).font(.system(size: 15)).foregroundStyle(iconTint)
            }
            Spacer().frame(height: 8)
            Text(value)
                .font(.system(size: 18, weight: .bold))
                .foregroundStyle(valueColor)
                .lineLimit(1)
            Spacer().frame(height: 2)
            Text(label)
                .font(.system(size: 11))
                .foregroundStyle(InsTextSec)
                .multilineTextAlignment(.center)
                .lineLimit(2)
                .fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(.horizontal, 8)
        .padding(.vertical, 12)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .overlay(RoundedRectangle(cornerRadius: 16).strokeBorder(CardBdr, lineWidth: 1))
    }
}

// ── Highlights row: Most Improved + Needs Attention ───────────────────────────

private struct HighlightsRow: View {
    let weakSubject: String?
    let accuracyPct: Int?
    let daysActive: Int

    var body: some View {
        HStack(spacing: 10) {
            HighlightCard(
                tone: .positive,
                header: "Most Improved",
                headerIcon: "chart.line.uptrend.xyaxis",
                title: daysActive >= 3 ? "Consistent Learning" : "Building Habit",
                badge: nil,
                bodyText: daysActive >= 3
                    ? "Active \(daysActive) days this week — revision is becoming a habit."
                    : "Start revising daily to build learning momentum.",
                sparklineAscending: true
            )
            HighlightCard(
                tone: .warning,
                header: "Needs Attention",
                headerIcon: "exclamationmark.triangle.fill",
                title: weakSubject ?? "No major concern",
                badge: weakSubject != nil ? accuracyPct.map { "\($0)%" } : nil,
                bodyText: weakSubject != nil
                    ? "Lowest accuracy this week.\nReview key concepts."
                    : "Learning is on track this period.",
                sparklineAscending: false
            )
        }
    }
}

private enum HighlightTone { case positive, warning }

private struct HighlightCard: View {
    let tone: HighlightTone
    let header: String
    let headerIcon: String
    let title: String
    let badge: String?
    let bodyText: String
    let sparklineAscending: Bool

    private var accent: Color    { tone == .positive ? PosGreen    : WarnAmberDeep }
    private var cardBg: Color    { tone == .positive ? PosGreenSoft : WarnAmberSoft }
    private var badgeBg: Color   { tone == .positive ? PosBadgeBg  : WarnBadgeBg   }
    private var chartBg: Color   { tone == .positive ? SparkGreenBg : SparkAmberBg }

    private var sparkline: [Float] {
        let n = 14
        if sparklineAscending {
            return (0..<n).map { 0.25 + 0.55 * (Float($0) / Float(n - 1)) }
        } else {
            return (0..<n).map { 0.65 - 0.35 * (Float($0) / Float(n - 1)) }
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            // Header row
            HStack(spacing: 6) {
                Image(systemName: headerIcon)
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(accent)
                Text(header)
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(accent)
            }
            Spacer().frame(height: 6)

            // Title + badge
            HStack(alignment: .center, spacing: 6) {
                Text(title)
                    .font(.system(size: 20, weight: .bold))
                    .foregroundStyle(accent)
                    .lineLimit(1)
                    .minimumScaleFactor(0.7)
                if let badge {
                    Text(badge)
                        .font(.system(size: 11, weight: .bold))
                        .foregroundStyle(accent)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 3)
                        .background(badgeBg)
                        .clipShape(Capsule())
                }
            }
            Spacer().frame(height: 8)

            Text(bodyText)
                .font(.system(size: 11))
                .foregroundStyle(InsTextPrim)
                .lineSpacing(2)
                .lineLimit(3)
                .fixedSize(horizontal: false, vertical: true)

            Spacer().frame(height: 8)

            SparklineLine(points: sparkline, lineColor: accent, fillBg: chartBg)
                .frame(maxWidth: .infinity)
                .frame(height: 48)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(14)
        .background(cardBg)
        .clipShape(RoundedRectangle(cornerRadius: 18))
    }
}

private struct SparklineLine: View {
    let points: [Float]
    let lineColor: Color
    let fillBg: Color

    var body: some View {
        Canvas { ctx, size in
            guard points.count > 1 else { return }
            let w = size.width; let h = size.height
            let padY = h * 0.15
            let usable = h - padY * 2
            let n = points.count
            let step = w / CGFloat(n - 1)
            let ys = points.map { p in padY + (1 - CGFloat(p).clamped(0, 1)) * usable }

            var linePath = Path()
            linePath.move(to: CGPoint(x: 0, y: ys[0]))
            for i in 1..<n {
                let px = step * CGFloat(i); let py = ys[i]
                let midX = step * CGFloat(i) - step / 2
                linePath.addCurve(to: CGPoint(x: px, y: py),
                    control1: CGPoint(x: midX, y: ys[i-1]),
                    control2: CGPoint(x: midX, y: py))
            }
            var fillPath = linePath
            fillPath.addLine(to: CGPoint(x: w, y: h))
            fillPath.addLine(to: CGPoint(x: 0, y: h))
            fillPath.closeSubpath()

            ctx.fill(fillPath, with: .color(fillBg))
            ctx.stroke(linePath, with: .color(lineColor),
                       style: StrokeStyle(lineWidth: 2, lineCap: .round))
            for i in 0..<n {
                let center = CGPoint(x: step * CGFloat(i), y: ys[i])
                ctx.fill(Path(ellipseIn: CGRect(x: center.x - 2.2, y: center.y - 2.2, width: 4.4, height: 4.4)),
                         with: .color(lineColor))
            }
        }
    }
}

// ── Subject performance card ───────────────────────────────────────────────────

private struct SubjectPerformanceCard: View {
    let weakSubject: String?
    let accuracyPct: Int?
    let onSubjectTap: (String) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                Text("Subject Performance")
                    .font(.system(size: 15, weight: .bold))
                    .foregroundStyle(InsTextPrim)
                Spacer()
                Image(systemName: "info.circle")
                    .font(.system(size: 13))
                    .foregroundStyle(InsTextTert)
                Spacer().frame(width: 4)
                Text("sorted by attempts")
                    .font(.system(size: 11))
                    .foregroundStyle(InsTextSec)
            }

            if let subject = weakSubject {
                Spacer().frame(height: 12)
                SubjectHeaderRow()
                Spacer().frame(height: 4)
                SubjectRow(
                    subject: subject,
                    attempts: 3,
                    accuracyPct: accuracyPct,
                    lastRevised: "This week",
                    lastRevisedDays: 0,
                    onTap: { onSubjectTap(subject) }
                )
                Spacer().frame(height: 8)
                HStack(spacing: 6) {
                    Image(systemName: "info.circle")
                        .font(.system(size: 11))
                        .foregroundStyle(InsTextTert)
                    Text("Accuracy = correct answers ÷ total answered × 100")
                        .font(.system(size: 10))
                        .foregroundStyle(InsTextSec)
                        .lineSpacing(2)
                }
                .padding(.top, 6)
            } else {
                Spacer().frame(height: 20)
                Text("No subject activity recorded yet. Quiz attempts and uploads will appear here.")
                    .font(.system(size: 13))
                    .foregroundStyle(InsTextSec)
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: .infinity)
                Spacer().frame(height: 12)
            }
        }
        .padding(16)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 18))
        .overlay(RoundedRectangle(cornerRadius: 18).strokeBorder(CardBdr, lineWidth: 1))
    }
}

private struct SubjectHeaderRow: View {
    var body: some View {
        HStack {
            Spacer().frame(width: 40)
            Text("Subject")
                .font(.system(size: 10, weight: .medium))
                .foregroundStyle(InsTextTert)
                .frame(maxWidth: .infinity, alignment: .leading)
            Text("Accuracy")
                .font(.system(size: 10, weight: .medium))
                .foregroundStyle(InsTextTert)
                .frame(width: 76, alignment: .trailing)
            Spacer().frame(width: 22)
        }
    }
}

private struct SubjectRow: View {
    let subject: String
    let attempts: Int
    let accuracyPct: Int?
    let lastRevised: String
    let lastRevisedDays: Int?
    let onTap: () -> Void

    private var visual: (icon: String, tint: Color, bg: Color) { subjectVisual(subject) }
    private var accuracyColor: Color {
        guard let acc = accuracyPct else { return InsTextTert }
        if acc >= 80 { return PosGreen }
        if acc >= 60 { return InsBlue }
        return WarnAmberDeep
    }
    private var lastRevisedColor: Color {
        guard let d = lastRevisedDays, d >= 5 else { return InsTextSec }
        return CritRed
    }

    var body: some View {
        Button(action: onTap) {
            HStack {
                ZStack {
                    Circle().fill(visual.bg).frame(width: 32, height: 32)
                    Image(systemName: visual.icon).font(.system(size: 15)).foregroundStyle(visual.tint)
                }
                Spacer().frame(width: 10)
                VStack(alignment: .leading, spacing: 2) {
                    Text(subject)
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(InsTextPrim)
                        .lineLimit(1)
                    HStack(spacing: 0) {
                        Text("\(attempts) attempt\(attempts == 1 ? "" : "s")")
                            .font(.system(size: 11))
                            .foregroundStyle(InsTextSec)
                        Text("  •  ")
                            .font(.system(size: 11))
                            .foregroundStyle(InsTextSec)
                        Text(lastRevised)
                            .font(.system(size: 11))
                            .foregroundStyle(lastRevisedColor)
                    }
                    .lineLimit(1)
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                Spacer().frame(width: 8)

                VStack(alignment: .trailing, spacing: 3) {
                    Text(accuracyPct.map { "\($0)%" } ?? "—")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundStyle(accuracyColor)
                    AccuracyBar(pct: accuracyPct, color: accuracyColor)
                        .frame(width: 76, height: 4)
                }

                Spacer().frame(width: 6)
                Image(systemName: "chevron.right")
                    .font(.system(size: 13))
                    .foregroundStyle(InsTextTert)
            }
            .padding(.vertical, 8)
        }
        .buttonStyle(.plain)
    }
}

private struct AccuracyBar: View {
    let pct: Int?
    let color: Color

    var body: some View {
        GeometryReader { geo in
            ZStack(alignment: .leading) {
                RoundedRectangle(cornerRadius: 50).fill(SubjectTrack)
                if let p = pct {
                    RoundedRectangle(cornerRadius: 50).fill(color)
                        .frame(width: geo.size.width * CGFloat(p.clamped(0, 100)) / 100)
                }
            }
        }
    }
}

private func subjectVisual(_ name: String) -> (icon: String, tint: Color, bg: Color) {
    let n = name.lowercased()
    if n.contains("math")    { return ("function",          PosGreen,     Color(hex: 0xE6F7EC)) }
    if n.contains("physics") { return ("atom",              InsBlue,      Color(hex: 0xE0EAFE)) }
    if n.contains("chemistry"){ return ("flask.fill",       Color(hex: 0xCA8A04), Color(hex: 0xFEF3C7)) }
    if n.contains("biology") { return ("leaf.fill",         PosGreen,     Color(hex: 0xDCFCE7)) }
    if n.contains("science") { return ("atom",              InsBlue,      Color(hex: 0xE0EAFE)) }
    if n.contains("english") { return ("book.fill",         CritRed,      Color(hex: 0xFEE2E2)) }
    if n.contains("hindi")   { return ("textformat.alt",    WarnAmberDeep,Color(hex: 0xFEE7D6)) }
    if n.contains("social")  { return ("globe",             WarnAmberDeep,Color(hex: 0xFEE7D6)) }
    if n.contains("computer"){ return ("desktopcomputer",   InsPurple,    InsPurpleBg) }
    return ("book.fill", InsBlue, InsBlueSoft)
}

// ── Recommendation card ────────────────────────────────────────────────────────

private struct InsRecommendationCard: View {
    let text: String
    let weakSubject: String?
    let onAssign: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            // Header row
            HStack(spacing: 12) {
                ZStack {
                    Circle().fill(Color(hex: 0xFCE7B5)).frame(width: 40, height: 40)
                    Image(systemName: "trophy.fill")
                        .font(.system(size: 18))
                        .foregroundStyle(WarnAmberDeep)
                }
                VStack(alignment: .leading, spacing: 2) {
                    Text("Parent Recommendation")
                        .font(.system(size: 13, weight: .bold))
                        .foregroundStyle(WarnAmberDeep)
                    Text(text.isEmpty ? "Keep the current rhythm — regular revision helps." : text)
                        .font(.system(size: 12))
                        .foregroundStyle(InsTextPrim)
                        .lineSpacing(3)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                Image(systemName: "checkmark.seal.fill")
                    .font(.system(size: 32))
                    .foregroundStyle(WarnAmberDeep)
            }

            Spacer().frame(height: 12)

            // Assign practice sub-card
            HStack(spacing: 10) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("SUGGESTED PRACTICE")
                        .font(.system(size: 10, weight: .bold))
                        .foregroundStyle(WarnAmberDeep)
                    Text(weakSubject != nil ? "\(weakSubject!) • Daily revision" : "Daily revision practice")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(InsTextPrim)
                        .lineLimit(1)
                    Text(weakSubject != nil
                         ? "Low accuracy — a short practice will help."
                         : "Keep the study habit going.")
                        .font(.system(size: 11))
                        .foregroundStyle(InsTextSec)
                        .lineLimit(2)
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                Button(action: onAssign) {
                    Text("Assign")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundStyle(.white)
                        .padding(.horizontal, 14)
                        .padding(.vertical, 8)
                        .background(WarnAmberDeep)
                        .clipShape(Capsule())
                }
                .buttonStyle(.plain)
            }
            .padding(10)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .overlay(RoundedRectangle(cornerRadius: 12).strokeBorder(Color(hex: 0xF1E0BD), lineWidth: 1))
        }
        .padding(14)
        .background(Color(hex: 0xFFF6E6))
        .clipShape(RoundedRectangle(cornerRadius: 18))
        .overlay(RoundedRectangle(cornerRadius: 18).strokeBorder(Color(hex: 0xF1E0BD), lineWidth: 1))
    }
}

// ── Privacy footer ─────────────────────────────────────────────────────────────

private struct InsPrivacyFooter: View {
    var body: some View {
        HStack(spacing: 6) {
            Image(systemName: "lock.fill")
                .font(.system(size: 11))
                .foregroundStyle(InsTextTert)
            Text("Visible only to you — your child's data is private.")
                .font(.system(size: 11))
                .foregroundStyle(InsTextSec)
        }
        .frame(maxWidth: .infinity, alignment: .center)
    }
}

// ── Helpers ────────────────────────────────────────────────────────────────────

private extension CGFloat {
    func clamped(_ lo: CGFloat, _ hi: CGFloat) -> CGFloat { Swift.min(Swift.max(self, lo), hi) }
}

private extension Int {
    func clamped(_ lo: Int, _ hi: Int) -> Int { Swift.min(Swift.max(self, lo), hi) }
}

#Preview {
    InsightsView(
        childName: "Ritika Sharma",
        childGrade: "7",
        childBoard: "CBSE",
        childGender: "female",
        engagement: ChildEngagement(
            childId: "c1",
            daysActiveThisWeek: 4,
            isActiveToday: true,
            streakDays: 4,
            accuracyPct: 58,
            weakSubject: "Science",
            weakSubjectAccuracyPct: 45,
            lastActivityMs: Int64(Date().timeIntervalSince1970 * 1000),
            weekDayKinds: [.revision, .both, .none, .revision, .homework, .none, .none]
        )
    )
}
