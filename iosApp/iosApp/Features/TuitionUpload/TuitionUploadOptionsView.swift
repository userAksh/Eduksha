import SwiftUI

private struct TuitionOption {
    let kind: String
    let emoji: String
    let title: String
    let subtitle: String
}

private func optionsFor(mode: String) -> [TuitionOption] {
    if mode == "work" {
        return [
            TuitionOption(kind: "tuition_work_learning", emoji: "📖",
                          title: "Today's Learning / Reading",
                          subtitle: "Notes or pages your tutor went through with you"),
            TuitionOption(kind: "tuition_work_written",  emoji: "✍️",
                          title: "Written Work",
                          subtitle: "Anything you wrote down or solved during tuition"),
            TuitionOption(kind: "tuition_work_test",     emoji: "🧪",
                          title: "Today's Test",
                          subtitle: "A quick test or assessment your tutor gave today"),
        ]
    } else {
        return [
            TuitionOption(kind: "tuition_hw_learning", emoji: "📖",
                          title: "Today's Learning / Reading",
                          subtitle: "Reading or notes your tutor asked you to go through"),
            TuitionOption(kind: "tuition_hw_written",  emoji: "✍️",
                          title: "Written Work",
                          subtitle: "Written homework you finished at home"),
        ]
    }
}

struct TuitionUploadOptionsView: View {
    let mode: String   // "work" or "homework"
    let childId: String
    let grade: Int
    var onBack: () -> Void = {}
    var onSelectKind: (String) -> Void = { _ in }

    private var options: [TuitionOption] { optionsFor(mode: mode) }

    private var pageTitle: String { mode == "work" ? "Upload Tuition Work" : "Upload Tuition Homework" }
    private var pageSubtitle: String {
        mode == "work"
            ? "What did you do in today's tuition class?"
            : "What homework did your tutor give you?"
    }

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
                Text(pageTitle)
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
                    Text(pageSubtitle)
                        .font(.dobiBody())
                        .foregroundStyle(Color.dobiTextSecondary)
                        .padding(.top, 12)

                    VStack(spacing: 12) {
                        ForEach(options, id: \.kind) { opt in
                            TOOptionCard(option: opt) { onSelectKind(opt.kind) }
                        }
                    }
                    .padding(.top, 20)
                }
                .padding(.horizontal, 20)
                .padding(.top, 4)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.dobiPageBg.ignoresSafeArea())
        .navigationBarHidden(true)
    }
}

private struct TOOptionCard: View {
    let option: TuitionOption
    let onClick: () -> Void

    var body: some View {
        Button(action: onClick) {
            HStack(spacing: 14) {
                ZStack {
                    RoundedRectangle(cornerRadius: 14)
                        .fill(Color(hex: 0xEDE9FE))
                        .frame(width: 48, height: 48)
                    Text(option.emoji).font(.system(size: 24))
                }
                VStack(alignment: .leading, spacing: 3) {
                    Text(option.title)
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(Color.dobiTextPrimary)
                        .multilineTextAlignment(.leading)
                    Text(option.subtitle)
                        .font(.system(size: 12))
                        .foregroundStyle(Color.dobiTextSecondary)
                        .multilineTextAlignment(.leading)
                        .lineLimit(2)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                Image(systemName: "chevron.right")
                    .font(.system(size: 14, weight: .medium))
                    .foregroundStyle(Color(hex: 0xE2E8F0))
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 16)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 18))
            .shadow(color: .black.opacity(0.05), radius: 4, x: 0, y: 2)
        }
        .buttonStyle(.plain)
    }
}
