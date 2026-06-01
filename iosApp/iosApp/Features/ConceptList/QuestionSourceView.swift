import SwiftUI

struct QuestionSourceView: View {
    let conceptId: String
    let childId: String
    let unitName: String
    let conceptName: String
    let subject: String
    var unitId: String = ""
    var onBack: () -> Void = {}
    var onPickCuratedSet: () -> Void = {}
    var onPickUploadAndGenerate: () -> Void = {}

    var body: some View {
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
                Text("Answer Questions")
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
                    QSLessonChip(unitName: unitName, conceptName: conceptName, subject: subject)

                    Text("How would you like to practise?")
                        .font(.system(size: 20, weight: .bold))
                        .foregroundStyle(Color.dobiTextPrimary)
                        .padding(.top, 20)

                    Text("Pick one — both work great for what you've learned.")
                        .font(.system(size: 14))
                        .foregroundStyle(Color.dobiTextSecondary)
                        .padding(.top, 6)

                    QSCard(
                        icon: "checklist",
                        badgeText: "Recommended",
                        title: "Our Question Set",
                        description: "Practise with curated questions made for this lesson by Dobi.",
                        accent: Color.dobiPrimary,
                        onClick: onPickCuratedSet
                    )
                    .padding(.top, 24)

                    QSCard(
                        icon: "sparkles",
                        badgeText: "✨ AI-powered",
                        title: "Upload Your Pages",
                        description: "Snap your textbook questions or chapter pages — Dobi will turn them into questions for you.",
                        accent: Color(hex: 0x8B5CF6),
                        onClick: onPickUploadAndGenerate
                    )
                    .padding(.top, 14)

                    Text("You can switch between options anytime.")
                        .font(.system(size: 12))
                        .foregroundStyle(Color.dobiTextSecondary)
                        .frame(maxWidth: .infinity, alignment: .center)
                        .padding(.top, 20)
                        .padding(.bottom, 40)
                }
                .padding(.horizontal, 20)
                .padding(.top, 8)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.dobiPageBg.ignoresSafeArea())
        .navigationBarHidden(true)
    }
}

// MARK: - Subviews

private struct QSLessonChip: View {
    let unitName: String
    let conceptName: String
    let subject: String

    var body: some View {
        let isConceptScope = !conceptName.isEmpty
        HStack(spacing: 8) {
            Text(isConceptScope ? "🔍" : "📘").font(.system(size: 14))
            Text(isConceptScope ? conceptName : (unitName.isEmpty ? "Lesson" : unitName))
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(Color.dobiPrimary)
            if !subject.isEmpty {
                Text("  •  \(subject)")
                    .font(.system(size: 12))
                    .foregroundStyle(Color.dobiPrimary.opacity(0.75))
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(Color.dobiPrimaryContainer)
        .clipShape(RoundedRectangle(cornerRadius: 10))
    }
}

private struct QSCard: View {
    let icon: String
    let badgeText: String
    let title: String
    let description: String
    let accent: Color
    let onClick: () -> Void

    var body: some View {
        Button(action: onClick) {
            HStack(spacing: 14) {
                ZStack {
                    RoundedRectangle(cornerRadius: 14)
                        .fill(accent.opacity(0.12))
                        .frame(width: 54, height: 54)
                    Image(systemName: icon)
                        .font(.system(size: 24))
                        .foregroundStyle(accent)
                }

                VStack(alignment: .leading, spacing: 0) {
                    Text(badgeText)
                        .font(.system(size: 10, weight: .semibold))
                        .foregroundStyle(accent)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 3)
                        .background(accent.opacity(0.14))
                        .clipShape(RoundedRectangle(cornerRadius: 6))

                    Text(title)
                        .font(.system(size: 15, weight: .bold))
                        .foregroundStyle(Color.dobiTextPrimary)
                        .padding(.top, 6)

                    Text(description)
                        .font(.system(size: 12))
                        .foregroundStyle(Color.dobiTextSecondary)
                        .lineSpacing(2)
                        .fixedSize(horizontal: false, vertical: true)
                        .padding(.top, 4)
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                ZStack {
                    Circle()
                        .fill(accent.opacity(0.10))
                        .frame(width: 32, height: 32)
                    Image(systemName: "chevron.right")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(accent)
                }
            }
            .padding(16)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 18))
            .overlay(
                RoundedRectangle(cornerRadius: 18)
                    .stroke(Color(hex: 0xE5EBF5), lineWidth: 1)
            )
            .shadow(color: .black.opacity(0.05), radius: 4, x: 0, y: 2)
        }
        .buttonStyle(.plain)
    }
}
