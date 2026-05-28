import SwiftUI

// Full-screen quiz result — mirrors Android QuizDone composable exactly.

struct QuizResultView: View {
    let result: ADQuizResult
    var onBack: () -> Void = {}

    private var percentInt: Int {
        result.total > 0 ? Int(Double(result.correct) / Double(result.total) * 100) : 0
    }
    private var scoreColor: Color {
        let pct = Double(percentInt) / 100.0
        if pct >= 0.7 { return Color.dobiSuccess }     // DobiSuccess
        if pct >= 0.4 { return Color(hex: 0xF59E0B) } // DobiWarning
        return Color.dobiError
    }
    private var attempted: Int { result.selections.count }

    var body: some View {
        VStack(spacing: 0) {
            // TopAppBar — "Quiz Result" with back arrow (matches Android Scaffold topBar)
            QRTopBar(onBack: onBack)

            // Scrollable content — everything padded 20pt horizontally (matches Modifier.padding(horizontal=20.dp))
            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 0) {
                    Spacer().frame(height: 8)

                    QRResultBanner(unitName: result.displayName)

                    Spacer().frame(height: 14)

                    QRAttemptedPill(attempted: attempted, total: result.total)
                        .frame(maxWidth: .infinity)

                    Spacer().frame(height: 20)

                    QRResultDivider()

                    Spacer().frame(height: 16)

                    QRScoreCard(
                        percentInt: percentInt,
                        correct: result.correct,
                        total: result.total,
                        scoreColor: scoreColor
                    )

                    Spacer().frame(height: 20)

                    ForEach(Array(result.questions.enumerated()), id: \.offset) { idx, q in
                        QRQuestionCard(
                            number: idx + 1,
                            question: q,
                            userPick: result.selections[idx]
                        )
                        Spacer().frame(height: 12)
                    }

                    Spacer().frame(height: 8)
                }
                .padding(.horizontal, 20)

                Spacer().frame(height: 100)
            }

            QRResultActionBar(onDone: onBack, onAssign: {})
        }
        .background(Color.white.ignoresSafeArea())
        .navigationBarHidden(true)
    }
}

// MARK: - TopBar  (Android: TopAppBar, containerColor = DobiBackground = white)

private struct QRTopBar: View {
    let onBack: () -> Void

    var body: some View {
        HStack(spacing: 4) {
            Button(action: onBack) {
                Image(systemName: "arrow.left")
                    .font(.system(size: 20, weight: .medium))
                    .foregroundStyle(Color(hex: 0x1A2B4A))
                    .frame(width: 44, height: 44)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)

            Text("Quiz Result")
                .font(.system(size: 17, weight: .bold))
                .foregroundStyle(Color(hex: 0x1A2B4A))

            Spacer()
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 4)
        .background(Color.white)
        .overlay(Divider(), alignment: .bottom)
    }
}

// MARK: - ResultBanner  (Android: Box with gradient rounded 20dp + subtitle below)

private struct QRResultBanner: View {
    let unitName: String

    private let gradColors: [Color] = [
        Color(hex: 0x7AB8F5), Color(hex: 0xB78CF0),
        Color(hex: 0xF59CA1), Color(hex: 0xF5B36A)
    ]

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            // Gradient card
            HStack(spacing: 10) {
                Text("👍")
                    .font(.system(size: 22))
                Text("Thank you for taking the Quiz")
                    .font(.system(size: 17, weight: .bold))
                    .foregroundStyle(.white)
                    .lineLimit(2)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 28)
            .frame(maxWidth: .infinity, alignment: .center)
            .background(
                LinearGradient(colors: gradColors, startPoint: .leading, endPoint: .trailing)
            )
            .clipShape(RoundedRectangle(cornerRadius: 20))

            // Subtitle below the card
            if !unitName.isEmpty {
                Spacer().frame(height: 12)
                Text("You've practised the fundamentals of \(unitName). Here's how you did.")
                    .font(.system(size: 14))
                    .foregroundStyle(Color(hex: 0x6B7A99))
                    .multilineTextAlignment(.center)
                    .lineSpacing(4)
                    .frame(maxWidth: .infinity)
            }
        }
    }
}

// MARK: - AttemptedPill  (Android: Row with HelpOutline icon + text, white bg, primary border alpha 0.3)

private struct QRAttemptedPill: View {
    let attempted: Int
    let total: Int

    var body: some View {
        HStack(spacing: 8) {
            Image(systemName: "questionmark.circle")
                .font(.system(size: 16))
                .foregroundStyle(Color.dobiPrimary)
            Text("Attempted Questions: \(attempted) / \(total)")
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(Color(hex: 0x1A2B4A))
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .background(Color.white)
        .clipShape(Capsule())
        .overlay(Capsule().strokeBorder(Color.dobiPrimary.opacity(0.3), lineWidth: 1.5))
    }
}

// MARK: - ResultDivider  (Android: DividerLine() | "RESULT" | DividerLine())

private struct QRResultDivider: View {
    var body: some View {
        HStack(spacing: 12) {
            Rectangle()
                .fill(Color(hex: 0xE5EBF5))
                .frame(height: 1)
            Text("RESULT")
                .font(.system(size: 11, weight: .semibold))
                .foregroundStyle(Color(hex: 0x6B7A99))
                .fixedSize()
            Rectangle()
                .fill(Color(hex: 0xE5EBF5))
                .frame(height: 1)
        }
    }
}

// MARK: - ScoreCard  (Android: Card, Row — left column: SCORE label / big % / "X/Y correct" + right: ScoreRing 78dp)

private struct QRScoreCard: View {
    let percentInt: Int
    let correct: Int
    let total: Int
    let scoreColor: Color

    var body: some View {
        HStack(alignment: .center, spacing: 0) {
            VStack(alignment: .leading, spacing: 0) {
                Text("SCORE")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(Color(hex: 0x6B7A99))
                Spacer().frame(height: 4)
                Text("\(percentInt)%")
                    .font(.system(size: 36, weight: .bold))
                    .foregroundStyle(Color(hex: 0x1A2B4A))
                Spacer().frame(height: 2)
                Text("\(correct) / \(total) correct")
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(Color(hex: 0x6B7A99))
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            QRScoreRing(percent: Double(percentInt) / 100.0, color: scoreColor, label: "\(percentInt)%")
                .frame(width: 78, height: 78)
        }
        .padding(20)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .overlay(RoundedRectangle(cornerRadius: 20).strokeBorder(Color(hex: 0xE5EBF5), lineWidth: 1))
    }
}

// MARK: - ScoreRing  (Android: Canvas drawArc, 8dp stroke, text inside = "$percentInt%" DobiTextPrimary 13sp)

private struct QRScoreRing: View {
    let percent: Double
    let color: Color
    let label: String

    var body: some View {
        ZStack {
            Canvas { ctx, size in
                let lw: CGFloat = 8
                let r = (min(size.width, size.height) - lw) / 2
                let center = CGPoint(x: size.width / 2, y: size.height / 2)

                // Track (full 360°)
                var track = Path()
                track.addArc(center: center, radius: r,
                             startAngle: .degrees(-90), endAngle: .degrees(270), clockwise: false)
                ctx.stroke(track, with: .color(Color(hex: 0xE5EBF5)),
                           style: StrokeStyle(lineWidth: lw, lineCap: .round))

                // Progress arc
                guard percent > 0 else { return }
                let sweep = min(percent, 1.0) * 360.0
                var arc = Path()
                arc.addArc(center: center, radius: r,
                           startAngle: .degrees(-90), endAngle: .degrees(-90 + sweep), clockwise: false)
                ctx.stroke(arc, with: .color(color),
                           style: StrokeStyle(lineWidth: lw, lineCap: .round))
            }

            // Label inside ring: "$percentInt%" DobiTextPrimary 13sp bold
            Text(label)
                .font(.system(size: 13, weight: .bold))
                .foregroundStyle(Color(hex: 0x1A2B4A))
        }
    }
}

// MARK: - QuestionReviewCard  (Android: white Card 18dp corners, shows correct pill + user's wrong pill)

private struct QRQuestionCard: View {
    let number: Int
    let question: ADMcqQuestion
    let userPick: Int?

    private var isCorrect: Bool { userPick != nil && userPick == question.correctIndex }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("Q\(number). \(question.text)")
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(Color(hex: 0x1A2B4A))
                .lineSpacing(4)
                .fixedSize(horizontal: false, vertical: true)

            Spacer().frame(height: 12)

            // Always show the correct answer
            QRReviewPill(
                optionText: question.options[safe: question.correctIndex] ?? "",
                label: "Correct",
                accent: Color.dobiSuccess,
                muted: false
            )

            // If wrong: show user's pick below
            if !isCorrect, let pick = userPick, pick >= 0 {
                Spacer().frame(height: 8)
                QRReviewPill(
                    optionText: question.options[safe: pick] ?? "",
                    label: "Your answer",
                    accent: Color.dobiError,
                    muted: true
                )
            }

            // If not attempted (userPick == nil)
            if userPick == nil {
                Spacer().frame(height: 8)
                QRReviewPill(
                    optionText: "Not attempted",
                    label: "Your answer",
                    accent: Color(hex: 0x94A3B8),
                    muted: true
                )
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 18))
        .overlay(RoundedRectangle(cornerRadius: 18).strokeBorder(Color(hex: 0xE5EBF5), lineWidth: 1))
    }
}

// MARK: - ReviewPill  (Android: Row, bg = muted?0xFFF8FAFC:accent.06, border 1.5dp accent.alpha, optionText left + label right)

private struct QRReviewPill: View {
    let optionText: String
    let label: String
    let accent: Color
    let muted: Bool

    var body: some View {
        HStack(alignment: .center, spacing: 10) {
            Text(optionText)
                .font(.system(size: 14))
                .foregroundStyle(Color(hex: 0x1A2B4A))
                .lineSpacing(3)
                .fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: .infinity, alignment: .leading)

            Text(label)
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(accent)
                .fixedSize()
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(muted ? Color(hex: 0xF8FAFC) : accent.opacity(0.06))
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .strokeBorder(accent.opacity(muted ? 0.4 : 0.7), lineWidth: 1.5)
        )
    }
}

// MARK: - ResultActionBar  (Android: isParentView → outlined "Assign" + filled "Done")

private struct QRResultActionBar: View {
    let onDone: () -> Void
    let onAssign: () -> Void

    var body: some View {
        VStack(spacing: 12) {
            // Outlined "Assign This Quiz to Child"
            Button(action: onAssign) {
                HStack(spacing: 8) {
                    Image(systemName: "checkmark.rectangle.portrait")
                        .font(.system(size: 18))
                        .foregroundStyle(Color.dobiPrimary)
                    Text("Assign This Quiz to Child")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(Color.dobiPrimary)
                }
                .frame(maxWidth: .infinity)
                .frame(height: 52)
                .background(Color.white)
                .clipShape(RoundedRectangle(cornerRadius: 16))
                .overlay(RoundedRectangle(cornerRadius: 16)
                    .strokeBorder(Color.dobiPrimary, lineWidth: 1.5))
            }
            .buttonStyle(.plain)

            // Filled "Done"
            Button(action: onDone) {
                Text("Done")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 52)
                    .background(Color.dobiPrimary)
                    .clipShape(RoundedRectangle(cornerRadius: 16))
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 14)
        .background(
            Color.white
                .shadow(color: .black.opacity(0.07), radius: 8, y: -3)
                .ignoresSafeArea(edges: .bottom)
        )
    }
}

// MARK: - Safe subscript

private extension Array {
    subscript(safe index: Int) -> Element? {
        indices.contains(index) ? self[index] : nil
    }
}
