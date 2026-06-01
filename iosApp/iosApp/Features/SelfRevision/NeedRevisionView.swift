import SwiftUI

struct NeedRevisionView: View {
    let childId: String
    var onBack: () -> Void = {}
    var onOpenItem: (RevisionMarkItem) -> Void = { _ in }

    @State private var marks: [RevisionMarkItem] = []
    @State private var isLoading = true

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
                Text("Need Revision")
                    .font(.dobiTitle3())
                    .foregroundStyle(Color.dobiTextPrimary)
                Spacer()
                Color.clear.frame(width: 36, height: 36)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(Color.dobiPageBg)

            if isLoading {
                Spacer()
                ProgressView()
                Spacer()
            } else if marks.isEmpty {
                NREmptyState()
            } else {
                ScrollView {
                    LazyVStack(spacing: 10) {
                        ForEach(marks) { mark in
                            NRRevisionItemCard(
                                mark: mark,
                                onTap: { onOpenItem(mark) },
                                onUnmark: { unmark(mark) }
                            )
                            .padding(.horizontal, 16)
                        }
                        NRTipCard()
                            .padding(.horizontal, 16)
                        Spacer().frame(height: 24)
                    }
                    .padding(.top, 8)
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.dobiPageBg.ignoresSafeArea())
        .navigationBarHidden(true)
        .task {
            marks = await FirebaseService.shared.fetchRevisionMarksOnce(childId: childId)
            isLoading = false
        }
    }

    private func unmark(_ mark: RevisionMarkItem) {
        FirebaseService.shared.unmarkRevision(childId: childId, scope: mark.scope, targetId: mark.targetId)
        withAnimation { marks.removeAll { $0.id == mark.id } }
    }
}

private struct NRRevisionItemCard: View {
    let mark: RevisionMarkItem
    let onTap: () -> Void
    let onUnmark: () -> Void

    var body: some View {
        let accent = mark.parsedColor
        Button(action: onTap) {
            HStack(spacing: 12) {
                ZStack {
                    RoundedRectangle(cornerRadius: 14)
                        .fill(accent.opacity(0.18))
                        .frame(width: 48, height: 48)
                    Text(mark.subjectIcon.isEmpty ? "📘" : mark.subjectIcon)
                        .font(.system(size: 24))
                }

                VStack(alignment: .leading, spacing: 3) {
                    HStack(spacing: 4) {
                        Image(systemName: "bookmark.fill")
                            .font(.system(size: 10, weight: .medium))
                            .foregroundStyle(Color(hex: 0xF59E0B))
                        Text(mark.isUnit ? "Lesson" : "Concept")
                            .font(.system(size: 10, weight: .semibold))
                            .foregroundStyle(Color(hex: 0xF59E0B))
                    }
                    Text(mark.name)
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(Color.dobiTextPrimary)
                        .lineLimit(2)
                    Text(mark.secondaryLabel)
                        .font(.system(size: 12))
                        .foregroundStyle(Color.dobiTextSecondary)
                }

                Spacer()

                Button(action: onUnmark) {
                    Image(systemName: "xmark")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(Color.dobiTextSecondary)
                        .frame(width: 32, height: 32)
                        .background(Color(hex: 0xF1F5F9))
                        .clipShape(Circle())
                }
                .buttonStyle(.plain)
            }
            .padding(14)
            .background(accent.opacity(0.06))
            .clipShape(RoundedRectangle(cornerRadius: 18))
            .overlay(RoundedRectangle(cornerRadius: 18).stroke(Color(hex: 0xE2E8F0), lineWidth: 1))
        }
        .buttonStyle(.plain)
    }
}

private struct NREmptyState: View {
    var body: some View {
        VStack(spacing: 16) {
            Spacer()
            Text("🔖").font(.system(size: 48))
            Text("No topics flagged yet")
                .font(.system(size: 17, weight: .bold))
                .foregroundStyle(Color.dobiTextPrimary)
            Text("Open a lesson or concept and tap Mark for Revision. We'll keep them here so you can come back to them later.")
                .font(.system(size: 14))
                .foregroundStyle(Color.dobiTextSecondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)
            Spacer()
        }
    }
}

private struct NRTipCard: View {
    var body: some View {
        HStack(spacing: 12) {
            Text("💡").font(.system(size: 20))
            Text("Tip: Revisit one or two of these every day instead of cramming them all at once.")
                .font(.system(size: 12))
                .foregroundStyle(Color.dobiTextPrimary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(14)
        .frame(maxWidth: .infinity)
        .background(Color(hex: 0xFEF3C7))
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .padding(.top, 4)
    }
}
