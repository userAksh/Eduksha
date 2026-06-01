import SwiftUI

struct RevisitDetailView: View {
    let groupKey: String
    let childId: String
    var onBack: () -> Void = {}
    var onTakeQuiz: (_ unitId: String, _ conceptId: String, _ unitName: String, _ subject: String) -> Void = { _, _, _, _ in }
    var onViewLastResult: (_ unitId: String, _ conceptId: String, _ unitName: String, _ subject: String) -> Void = { _, _, _, _ in }

    @State private var isLoading = true
    @State private var title = ""
    @State private var subject = ""
    @State private var subjectColor = ""
    @State private var subjectIcon = ""
    @State private var unitId = ""
    @State private var conceptId = ""
    @State private var unitName = ""
    @State private var photoUrls: [String] = []
    @State private var uploadCount = 0
    @State private var latestTimestamp: Int64 = 0
    @State private var isTuition = false
    @State private var mcqAttempt: FirebaseService.QuizAttemptSummary? = nil
    @State private var fullscreenIndex: Int? = nil

    private var canTakeQuiz: Bool { !isTuition && (!conceptId.isEmpty || !unitId.isEmpty) }
    private var hasLastResult: Bool { mcqAttempt != nil }

    private var accent: Color {
        let hex = subjectColor.trimmingCharacters(in: .init(charactersIn: "#"))
        guard !hex.isEmpty, let val = UInt32(hex, radix: 16) else { return Color.dobiPrimary }
        return Color(hex: val)
    }

    var body: some View {
        ZStack {
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
                    Text(title.isEmpty ? "Topic" : title)
                        .font(.dobiTitle3())
                        .foregroundStyle(Color.dobiTextPrimary)
                        .lineLimit(1)
                    Spacer()
                    Color.clear.frame(width: 36, height: 36)
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .background(Color.dobiPageBg)

                if isLoading {
                    Spacer()
                    ProgressView().scaleEffect(1.2).tint(Color.dobiPrimary)
                    Spacer()
                } else if photoUrls.isEmpty {
                    VStack(spacing: 8) {
                        Spacer()
                        Text("Nothing here yet")
                            .font(.system(size: 17, weight: .bold))
                            .foregroundStyle(Color.dobiTextPrimary)
                        Text("Photos uploaded for this topic will appear here.")
                            .font(.system(size: 14))
                            .foregroundStyle(Color.dobiTextSecondary)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 32)
                        Spacer()
                    }
                } else {
                    ScrollView {
                        VStack(alignment: .leading, spacing: 0) {
                            // Header row
                            RDHeaderRow(
                                subject: subject, subjectColor: subjectColor,
                                subjectIcon: subjectIcon, uploadCount: uploadCount,
                                latestTimestamp: latestTimestamp
                            )
                            .padding(.horizontal, 16)
                            .padding(.vertical, 4)

                            // Action buttons
                            if canTakeQuiz {
                                HStack(spacing: 10) {
                                    Button(action: {
                                        onTakeQuiz(unitId, conceptId, unitName, subject)
                                    }) {
                                        Text("Take Quiz")
                                            .font(.system(size: 14, weight: .semibold))
                                            .foregroundStyle(Color.white)
                                            .frame(maxWidth: .infinity)
                                            .frame(height: 46)
                                            .background(Color.dobiPrimary)
                                            .clipShape(RoundedRectangle(cornerRadius: 12))
                                    }
                                    .buttonStyle(.plain)

                                    if hasLastResult {
                                        Button(action: {
                                            onViewLastResult(unitId, conceptId, unitName, subject)
                                        }) {
                                            Text("View Last Result")
                                                .font(.system(size: 14, weight: .semibold))
                                                .foregroundStyle(Color.dobiTextPrimary)
                                                .frame(maxWidth: .infinity)
                                                .frame(height: 46)
                                                .overlay(
                                                    RoundedRectangle(cornerRadius: 12)
                                                        .stroke(Color(hex: 0xE2E8F0), lineWidth: 1)
                                                )
                                        }
                                        .buttonStyle(.plain)
                                    }
                                }
                                .padding(.horizontal, 16)
                                .padding(.top, 8)
                            }

                            // Attempts section
                            if let mcq = mcqAttempt {
                                VStack(alignment: .leading, spacing: 8) {
                                    Text("Attempts")
                                        .font(.system(size: 15, weight: .semibold))
                                        .foregroundStyle(Color.dobiTextPrimary)

                                    RDAttemptCard(
                                        emoji: "📊",
                                        title: "MCQ Quiz",
                                        subtitle: "\(mcq.lastCorrect)/\(mcq.lastTotal) correct" +
                                            (mcq.attempts > 1 ? " · \(mcq.attempts) tries" : ""),
                                        accuracyPct: mcq.accuracyPct,
                                        timestamp: mcq.lastUpdated,
                                        accent: Color(hex: 0x2563EB),
                                        bg: Color(hex: 0xDBEAFE),
                                        onClick: { onViewLastResult(unitId, conceptId, unitName, subject) }
                                    )
                                }
                                .padding(.horizontal, 16)
                                .padding(.top, 12)
                            }

                            // Photos section
                            Text("Photos")
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundStyle(Color.dobiTextPrimary)
                                .padding(.horizontal, 16)
                                .padding(.top, 12)
                                .padding(.bottom, 8)

                            LazyVGrid(
                                columns: [GridItem(.flexible(), spacing: 10), GridItem(.flexible(), spacing: 10)],
                                spacing: 10
                            ) {
                                ForEach(Array(photoUrls.enumerated()), id: \.offset) { idx, url in
                                    RDPhotoTile(url: url, onClick: { fullscreenIndex = idx })
                                }
                            }
                            .padding(.horizontal, 16)

                            Spacer().frame(height: 24)
                        }
                    }
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Color.dobiPageBg.ignoresSafeArea())
            .navigationBarHidden(true)

            // Fullscreen photo overlay
            if let idx = fullscreenIndex, idx < photoUrls.count {
                RDFullscreenPhoto(url: photoUrls[idx], onDismiss: { fullscreenIndex = nil })
                    .ignoresSafeArea()
            }
        }
        .task { await loadData() }
    }

    // MARK: - Data loading

    private func loadData() async {
        let uploads = await FirebaseService.shared.fetchUploads(childId: childId)
        let matching = uploads.filter { $0.groupKey == groupKey }
        let sorted = matching.sorted { $0.timestamp > $1.timestamp }

        guard let latest = sorted.first else {
            await MainActor.run { isLoading = false }
            return
        }

        let photos = sorted.flatMap { $0.photoUrls }
        let summary = await FirebaseService.shared.fetchQuizAttemptSummary(
            childId: childId, conceptId: latest.conceptId, unitId: latest.unitId
        )

        await MainActor.run {
            self.title = latest.displayTitle
            self.subject = latest.subject
            self.subjectColor = latest.subjectColor
            self.subjectIcon = latest.subjectIcon
            self.unitId = latest.unitId
            self.unitName = latest.unitName
            self.conceptId = latest.conceptId
            self.isTuition = latest.isTuition
            self.photoUrls = photos
            self.uploadCount = matching.count
            self.latestTimestamp = latest.timestamp
            self.mcqAttempt = summary
            self.isLoading = false
        }
    }
}

// ── Header row ──────────────────────────────────────────────────────────────────

private struct RDHeaderRow: View {
    let subject: String
    let subjectColor: String
    let subjectIcon: String
    let uploadCount: Int
    let latestTimestamp: Int64

    private var accent: Color {
        let hex = subjectColor.trimmingCharacters(in: .init(charactersIn: "#"))
        guard !hex.isEmpty, let val = UInt32(hex, radix: 16) else { return Color.dobiPrimary }
        return Color(hex: val)
    }

    private var dateLabel: String {
        guard latestTimestamp > 0 else { return "" }
        let fmt = DateFormatter(); fmt.dateFormat = "dd MMM yyyy"
        return fmt.string(from: Date(timeIntervalSince1970: Double(latestTimestamp) / 1000))
    }

    var body: some View {
        HStack(spacing: 10) {
            ZStack {
                RoundedRectangle(cornerRadius: 8)
                    .fill(accent.opacity(0.15))
                    .frame(width: 36, height: 36)
                Text(subjectIcon.isEmpty ? "📘" : subjectIcon)
                    .font(.system(size: 18))
            }
            VStack(alignment: .leading, spacing: 1) {
                Text(subject.isEmpty ? "—" : subject)
                    .font(.system(size: 14))
                    .foregroundStyle(Color.dobiTextPrimary)
                Text("\(uploadCount) \(uploadCount == 1 ? "upload" : "uploads")\(dateLabel.isEmpty ? "" : " • \(dateLabel)")")
                    .font(.system(size: 12))
                    .foregroundStyle(Color.dobiTextSecondary)
            }
            Spacer()
        }
    }
}

// ── Attempt card ────────────────────────────────────────────────────────────────

private struct RDAttemptCard: View {
    let emoji: String
    let title: String
    let subtitle: String
    let accuracyPct: Int
    let timestamp: Int64
    let accent: Color
    let bg: Color
    let onClick: () -> Void

    private var timeLabel: String {
        guard timestamp > 0 else { return "" }
        let diff = Int64(Date().timeIntervalSince1970 * 1000) - timestamp
        let mins = max(0, diff) / 60_000
        let hours = mins / 60
        let days = hours / 24
        if mins < 1 { return "Just now" }
        if mins < 60 { return "\(mins)m ago" }
        if hours < 24 { return "\(hours)h ago" }
        if days < 7 { return "\(days)d ago" }
        let fmt = DateFormatter(); fmt.dateFormat = "dd MMM yyyy"
        return fmt.string(from: Date(timeIntervalSince1970: Double(timestamp) / 1000))
    }

    var body: some View {
        Button(action: onClick) {
            HStack(spacing: 12) {
                ZStack {
                    RoundedRectangle(cornerRadius: 10)
                        .fill(bg)
                        .frame(width: 40, height: 40)
                    Text(emoji).font(.system(size: 18))
                }
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.system(size: 14))
                        .foregroundStyle(Color.dobiTextPrimary)
                    Text(subtitle)
                        .font(.system(size: 12))
                        .foregroundStyle(Color.dobiTextSecondary)
                    if !timeLabel.isEmpty {
                        Text(timeLabel)
                            .font(.system(size: 11))
                            .foregroundStyle(Color.dobiTextSecondary)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                Text("\(accuracyPct)%")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundStyle(accent)
            }
            .padding(12)
            .background(bg.opacity(0.5))
            .clipShape(RoundedRectangle(cornerRadius: 14))
        }
        .buttonStyle(.plain)
    }
}

// ── Photo tile ──────────────────────────────────────────────────────────────────

private struct RDPhotoTile: View {
    let url: String
    let onClick: () -> Void

    var body: some View {
        Button(action: onClick) {
            GeometryReader { geo in
                ZStack {
                    Color(hex: 0xF1F5F9)
                    AsyncImage(url: URL(string: url)) { phase in
                        switch phase {
                        case .success(let img):
                            img.resizable()
                                .scaledToFill()
                                .frame(width: geo.size.width, height: geo.size.width)
                                .clipped()
                        case .empty:
                            ProgressView().tint(Color.dobiPrimary)
                        default:
                            EmptyView()
                        }
                    }
                }
            }
            .aspectRatio(1, contentMode: .fit)
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
        .buttonStyle(.plain)
    }
}

// ── Fullscreen photo overlay ────────────────────────────────────────────────────

private struct RDFullscreenPhoto: View {
    let url: String
    let onDismiss: () -> Void

    var body: some View {
        ZStack(alignment: .topTrailing) {
            Color.black.opacity(0.92)
                .onTapGesture(perform: onDismiss)

            AsyncImage(url: URL(string: url)) { phase in
                if case .success(let img) = phase {
                    img.resizable().scaledToFit()
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    ProgressView().tint(.white)
                }
            }
            .allowsHitTesting(false)

            Button(action: onDismiss) {
                Image(systemName: "xmark")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(Color.white)
                    .frame(width: 36, height: 36)
                    .background(Color.black.opacity(0.5))
                    .clipShape(Circle())
            }
            .padding(.top, 60)
            .padding(.trailing, 16)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
