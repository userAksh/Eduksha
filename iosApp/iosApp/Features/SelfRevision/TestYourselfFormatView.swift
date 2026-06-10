import SwiftUI

struct TestYourselfFormatView: View {
    let source: String       // "curated" or "ai"
    let conceptId: String
    let unitId: String
    let unitName: String
    let conceptName: String
    let subject: String
    let subjectColor: String
    let childId: String
    let grade: Int
    var onBack: () -> Void = {}
    var onPickMcq: () -> Void = {}
    var onPickSubjective: () -> Void = {}

    private var isAi: Bool { source == "ai" }
    private var isUnitScope: Bool { conceptId.isEmpty }

    @State private var mcqAttemptCount: Int = 0

    private var displayTitle: String {
        isUnitScope ? unitName : (conceptName.isEmpty ? unitName : conceptName)
    }

    private var accent: Color {
        let hex = subjectColor.trimmingCharacters(in: .init(charactersIn: "#"))
        guard !hex.isEmpty, let val = UInt32(hex, radix: 16) else { return Color.dobiPrimary }
        return Color(hex: val)
    }

    var body: some View {
        VStack(spacing: 0) {
            // Nav bar
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
                Text("Pick a format")
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
                    // Lesson chip
                    TYFLessonChip(title: displayTitle, subject: subject, accent: accent)

                    Text(isAi ? "Pick a question type" : "Pick how you'd like to practise")
                        .font(.system(size: 22, weight: .bold))
                        .foregroundStyle(Color.dobiTextPrimary)
                        .padding(.top, 20)

                    Text(isAi
                         ? "Snap your textbook pages and Dobi will generate the questions you pick."
                         : "We'll show you 10 questions with answers — practise as long as you'd like.")
                        .font(.system(size: 14))
                        .foregroundStyle(Color.dobiTextSecondary)
                        .padding(.top, 6)

                    // MCQ card
                    let mcqDisabled = !isAi && mcqAttemptCount >= 2
                    TYFFormatCard(
                        iconSystemName: "list.bullet.clipboard",
                        iconBg: Color(hex: 0xDBEAFE),
                        iconTint: Color.dobiPrimary,
                        title: "MCQ",
                        subtitle: "Multiple-choice questions with one correct answer.",
                        ctaLabel: isAi ? "Snap & Generate MCQs" : "Start MCQ Quiz",
                        disabled: mcqDisabled,
                        onClick: onPickMcq
                    )
                    .padding(.top, 20)

                    if mcqDisabled {
                        Text("Max attempts reached for this topic.")
                            .font(.system(size: 12))
                            .foregroundStyle(Color.dobiTextSecondary)
                            .frame(maxWidth: .infinity, alignment: .center)
                            .padding(.top, 6)
                    }

                    // Subjective card
                    TYFFormatCard(
                        iconSystemName: "pencil.and.list.clipboard",
                        iconBg: Color(hex: 0xFEF3C7),
                        iconTint: Color(hex: 0xF59E0B),
                        title: "Subjective",
                        subtitle: "Write short answers and have Dobi grade them with feedback.",
                        ctaLabel: isAi ? "Snap & Write" : "Start Subjective",
                        onClick: onPickSubjective
                    )
                    .padding(.top, 12)

                    // Info footer
                    HStack(spacing: 10) {
                        Text(isAi ? "✨" : "💡").font(.system(size: 18))
                        Text(isAi
                             ? "Dobi will check your photos match this lesson before generating questions."
                             : "Tip: try MCQs for quick checks, Subjective when you're ready to write full answers.")
                            .font(.system(size: 12))
                            .foregroundStyle(Color.dobiTextPrimary)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                    .padding(14)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color(hex: 0xEEF4FF))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                    .padding(.top, 20)

                    Spacer().frame(height: 40)
                }
                .padding(.horizontal, 20)
                .padding(.top, 8)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.dobiPageBg.ignoresSafeArea())
        .navigationBarHidden(true)
        .task {
            guard !isAi && !childId.isEmpty else { return }
            let id = isUnitScope ? unitId : conceptId
            mcqAttemptCount = await FirebaseService.shared.fetchAttemptCount(
                childId: childId, id: id, isUnit: isUnitScope
            )
        }
    }
}

// ── Lesson chip ─────────────────────────────────────────────────────────────────

private struct TYFLessonChip: View {
    let title: String
    let subject: String
    let accent: Color

    var body: some View {
        HStack(spacing: 8) {
            Text("📘").font(.system(size: 14))
            Text(title.isEmpty ? "Lesson" : title)
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(accent)
                .frame(maxWidth: .infinity, alignment: .leading)
            if !subject.isEmpty {
                Text("•")
                    .foregroundStyle(accent.opacity(0.6))
                    .font(.system(size: 14))
                Text(subject)
                    .font(.system(size: 13))
                    .foregroundStyle(accent.opacity(0.85))
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(accent.opacity(0.12))
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }
}

// ── Format card ─────────────────────────────────────────────────────────────────

private struct TYFFormatCard: View {
    let iconSystemName: String
    let iconBg: Color
    let iconTint: Color
    let title: String
    let subtitle: String
    let ctaLabel: String
    var disabled: Bool = false
    let onClick: () -> Void

    var body: some View {
        Button(action: disabled ? {} : onClick) {
            VStack(spacing: 0) {
                HStack(spacing: 14) {
                    ZStack {
                        RoundedRectangle(cornerRadius: 14)
                            .fill(iconBg)
                            .frame(width: 52, height: 52)
                        Image(systemName: iconSystemName)
                            .font(.system(size: 26))
                            .foregroundStyle(iconTint)
                    }

                    VStack(alignment: .leading, spacing: 4) {
                        Text(title)
                            .font(.system(size: 17, weight: .bold))
                            .foregroundStyle(Color.dobiTextPrimary)
                        Text(subtitle)
                            .font(.system(size: 12))
                            .foregroundStyle(Color.dobiTextSecondary)
                            .lineSpacing(2)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)

                    ZStack {
                        Circle()
                            .fill(iconBg)
                            .frame(width: 32, height: 32)
                        Image(systemName: disabled ? "lock.fill" : "chevron.right")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundStyle(iconTint)
                    }
                }

                // CTA button
                Text(ctaLabel)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(Color.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 46)
                    .background(iconTint)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                    .padding(.top, 14)
            }
            .padding(16)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 18))
            .overlay(
                RoundedRectangle(cornerRadius: 18)
                    .stroke(iconTint.opacity(0.5), lineWidth: 1)
            )
            .opacity(disabled ? 0.45 : 1.0)
        }
        .buttonStyle(.plain)
    }
}
