import SwiftUI

// ── Local topic card model ──────────────────────────────────────────────────────

private struct RTTopicData: Identifiable {
    let id: String  // = groupKey
    let groupKey: String
    let title: String
    let subject: String
    let subjectColor: String
    let subjectIcon: String
    let contextLine: String
    let photoCount: Int
    let latestTimestamp: Int64
    let kindLabel: String
    let kindColorHex: UInt32
    let unitId: String
    let conceptId: String
    var mcqLastPct: Int? = nil
    var mcqAttempts: Int = 0

    var parsedSubjectColor: Color {
        let hex = subjectColor.trimmingCharacters(in: .init(charactersIn: "#"))
        guard !hex.isEmpty, let val = UInt32(hex, radix: 16) else { return Color(hex: 0x94A3B8) }
        return Color(hex: val)
    }
}

private struct RTSubjectChipData: Identifiable {
    let id: String
    let name: String
    let color: String
    let icon: String
}

// ── Main view ───────────────────────────────────────────────────────────────────

struct RevisitTopicsView: View {
    let childId: String
    var onBack: () -> Void = {}
    var onTopicClick: (_ groupKey: String, _ title: String, _ subject: String,
                       _ subjectColor: String, _ subjectIcon: String,
                       _ unitId: String, _ conceptId: String) -> Void = { _, _, _, _, _, _, _ in }

    @State private var topics: [RTTopicData] = []
    @State private var subjectChips: [RTSubjectChipData] = []
    @State private var isLoading = true
    @State private var searchQuery = ""
    @State private var selectedSubject = "All"

    private var filteredTopics: [RTTopicData] {
        var result = selectedSubject == "All" ? topics
            : topics.filter { $0.subject.lowercased() == selectedSubject.lowercased() }
        let q = searchQuery.trimmingCharacters(in: .whitespaces)
        guard !q.isEmpty else { return result }
        return result.filter {
            $0.title.localizedCaseInsensitiveContains(q) ||
            $0.contextLine.localizedCaseInsensitiveContains(q)
        }
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
                Text("Revisit Past Topics")
                    .font(.dobiTitle3())
                    .foregroundStyle(Color.dobiTextPrimary)
                Spacer()
                Color.clear.frame(width: 36, height: 36)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(Color.dobiPageBg)

            // Search bar
            HStack(spacing: 10) {
                Image(systemName: "magnifyingglass")
                    .font(.system(size: 15))
                    .foregroundStyle(Color.dobiTextSecondary)
                TextField(text: $searchQuery, prompt: .dobiPlaceholder("Search topics…")) {}
                    .font(.system(size: 15))
                    .foregroundStyle(Color.dobiTextPrimary)
                    .autocorrectionDisabled()
                if !searchQuery.isEmpty {
                    Button { searchQuery = "" } label: {
                        Image(systemName: "xmark.circle.fill")
                            .font(.system(size: 15))
                            .foregroundStyle(Color.dobiTextSecondary)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 10)
            .background(Color(hex: 0xEEF4FF))
            .clipShape(RoundedRectangle(cornerRadius: 14))
            .padding(.horizontal, 16)
            .padding(.top, 8)

            // Subject chips
            if !subjectChips.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        RTSubjectChip(label: "All", color: "", icon: "",
                                      selected: selectedSubject == "All",
                                      onTap: { selectedSubject = "All" })
                        ForEach(subjectChips) { chip in
                            RTSubjectChip(label: chip.name, color: chip.color, icon: chip.icon,
                                          selected: chip.name == selectedSubject,
                                          onTap: { selectedSubject = chip.name })
                        }
                    }
                    .padding(.horizontal, 16)
                }
                .padding(.vertical, 12)
            } else {
                Spacer().frame(height: 8)
            }

            if isLoading {
                Spacer()
                ProgressView().scaleEffect(1.2).tint(Color.dobiPrimary)
                Spacer()
            } else if topics.isEmpty {
                RTEmptyState(
                    title: "Nothing to revisit yet",
                    subtitle: "When you upload class work, self learning, or tuition work, the topics will show up here."
                )
            } else if filteredTopics.isEmpty {
                RTEmptyState(
                    title: "No matches",
                    subtitle: "Try a different subject or clear your search."
                )
            } else {
                ScrollView {
                    LazyVStack(spacing: 10) {
                        ForEach(filteredTopics) { topic in
                            Button {
                                onTopicClick(topic.groupKey, topic.title, topic.subject,
                                             topic.subjectColor, topic.subjectIcon,
                                             topic.unitId, topic.conceptId)
                            } label: {
                                RTTopicCard(topic: topic)
                                    .padding(.horizontal, 16)
                            }
                            .buttonStyle(.plain)
                        }

                        // Tip footer
                        HStack(spacing: 10) {
                            Text("💡").font(.system(size: 18))
                            Text("Tip: Revising little by little each day helps you remember better for a longer time.")
                                .font(.system(size: 12))
                                .foregroundStyle(Color.dobiTextPrimary)
                                .fixedSize(horizontal: false, vertical: true)
                        }
                        .padding(14)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(Color(hex: 0xEEF4FF))
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                        .padding(.horizontal, 16)

                        Spacer().frame(height: 24)
                    }
                    .padding(.top, 4)
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.dobiPageBg.ignoresSafeArea())
        .navigationBarHidden(true)
        .task { await loadData() }
    }

    // MARK: - Data loading

    private func loadData() async {
        let uploads = await FirebaseService.shared.fetchUploads(childId: childId)
        let schoolUploads = uploads.filter { !$0.isTuition }

        // Group by groupKey
        var grouped: [String: [UploadItem]] = [:]
        for item in schoolUploads {
            grouped[item.groupKey, default: []].append(item)
        }

        let built: [RTTopicData] = grouped.compactMap { (key, items) -> RTTopicData? in
            guard let latest = items.max(by: { $0.timestamp < $1.timestamp }) else { return nil }
            return RTTopicData(
                id: key, groupKey: key,
                title: latest.displayTitle,
                subject: latest.subject,
                subjectColor: latest.subjectColor,
                subjectIcon: latest.subjectIcon,
                contextLine: contextLine(for: latest),
                photoCount: items.reduce(0) { $0 + $1.photoUrls.count },
                latestTimestamp: latest.timestamp,
                kindLabel: latest.kindLabel,
                kindColorHex: latest.kindColorHex,
                unitId: latest.unitId,
                conceptId: latest.conceptId
            )
        }.sorted { $0.latestTimestamp > $1.latestTimestamp }

        // Build subject chips from unique subjects (preserving order by first appearance)
        var seenSubjects: [String] = []
        var subjectMeta: [String: (color: String, icon: String)] = [:]
        for item in schoolUploads where !item.subject.isEmpty {
            if subjectMeta[item.subject] == nil {
                seenSubjects.append(item.subject)
                subjectMeta[item.subject] = (item.subjectColor, item.subjectIcon)
            }
        }
        let chips = seenSubjects.sorted().map { name -> RTSubjectChipData in
            let meta = subjectMeta[name] ?? ("", "")
            return RTSubjectChipData(id: name, name: name, color: meta.color, icon: meta.icon)
        }

        await MainActor.run {
            self.topics = built
            self.subjectChips = chips
            self.isLoading = false
        }

        // Fetch quiz attempt summaries in parallel
        await withTaskGroup(of: (String, FirebaseService.QuizAttemptSummary?).self) { group in
            for topic in built {
                let gk = topic.groupKey
                let cid = topic.conceptId
                let uid = topic.unitId
                group.addTask {
                    let summary = await FirebaseService.shared.fetchQuizAttemptSummary(
                        childId: childId, conceptId: cid, unitId: uid
                    )
                    return (gk, summary)
                }
            }
            for await (gk, summary) in group {
                if let s = summary {
                    await MainActor.run {
                        if let i = self.topics.firstIndex(where: { $0.groupKey == gk }) {
                            self.topics[i].mcqLastPct = s.accuracyPct
                            self.topics[i].mcqAttempts = s.attempts
                        }
                    }
                }
            }
        }
    }

    private func contextLine(for item: UploadItem) -> String {
        if !item.conceptId.isEmpty && !item.unitName.isEmpty {
            return "\(item.subject) • \(item.unitName)"
        }
        if !item.unitName.isEmpty { return item.subject }
        return item.subject
    }
}

// ── Subject chip ────────────────────────────────────────────────────────────────

private struct RTSubjectChip: View {
    let label: String
    let color: String
    let icon: String
    let selected: Bool
    let onTap: () -> Void

    private var accent: Color {
        guard !color.isEmpty else { return Color.dobiPrimary }
        let hex = color.trimmingCharacters(in: .init(charactersIn: "#"))
        guard let val = UInt32(hex, radix: 16) else { return Color.dobiPrimary }
        return Color(hex: val)
    }

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 6) {
                if !icon.isEmpty && label != "All" {
                    Text(icon).font(.system(size: 14))
                }
                Text(label)
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(selected ? Color.white : Color.dobiTextPrimary)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 8)
            .background(selected ? accent : Color(hex: 0xF1F5F9))
            .clipShape(RoundedRectangle(cornerRadius: 20))
        }
        .buttonStyle(.plain)
    }
}

// ── Topic card ──────────────────────────────────────────────────────────────────

private struct RTTopicCard: View {
    let topic: RTTopicData

    var body: some View {
        let accent = topic.parsedSubjectColor
        let kindAccent = Color(hex: topic.kindColorHex)
        let revised = revisedLabel(timestamp: topic.latestTimestamp)

        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .top, spacing: 12) {
                ZStack {
                    RoundedRectangle(cornerRadius: 14)
                        .fill(accent.opacity(0.18))
                        .frame(width: 52, height: 52)
                    Text(topic.subjectIcon.isEmpty ? "📘" : topic.subjectIcon)
                        .font(.system(size: 26))
                }

                VStack(alignment: .leading, spacing: 2) {
                    Text(topic.title)
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundStyle(Color.dobiTextPrimary)
                        .multilineTextAlignment(.leading)
                    if !topic.contextLine.isEmpty {
                        Text(topic.contextLine)
                            .font(.system(size: 12))
                            .foregroundStyle(Color.dobiTextSecondary)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                // Kind tag
                Text(topic.kindLabel)
                    .font(.system(size: 10, weight: .semibold))
                    .foregroundStyle(kindAccent)
                    .padding(.horizontal, 8).padding(.vertical, 3)
                    .background(kindAccent.opacity(0.12))
                    .clipShape(RoundedRectangle(cornerRadius: 6))
            }

            Spacer().frame(height: 10)

            // Meta row
            HStack(spacing: 6) {
                Circle().fill(revised.1).frame(width: 7, height: 7)
                Text(revised.0)
                    .font(.system(size: 11, weight: .medium))
                    .foregroundStyle(revised.1)
                Spacer()
                Text("📷 \(topic.photoCount) \(topic.photoCount == 1 ? "photo" : "photos")")
                    .font(.system(size: 11, weight: .medium))
                    .foregroundStyle(Color.dobiTextSecondary)
            }

            // Quiz attempt pills
            if topic.mcqLastPct != nil {
                Spacer().frame(height: 8)
                HStack(spacing: 6) {
                    if let pct = topic.mcqLastPct {
                        RTResultPill(
                            emoji: "📊",
                            text: "\(pct)% MCQ" + (topic.mcqAttempts > 1 ? " · \(topic.mcqAttempts) tries" : ""),
                            bg: Color(hex: 0xDBEAFE),
                            fg: Color(hex: 0x2563EB)
                        )
                    }
                }
            }
        }
        .padding(14)
        .background(accent.opacity(0.06))
        .clipShape(RoundedRectangle(cornerRadius: 18))
        .overlay(RoundedRectangle(cornerRadius: 18).stroke(Color(hex: 0xE2E8F0), lineWidth: 1))
    }
}

private struct RTResultPill: View {
    let emoji: String
    let text: String
    let bg: Color
    let fg: Color

    var body: some View {
        HStack(spacing: 4) {
            Text(emoji).font(.system(size: 11))
            Text(text)
                .font(.system(size: 10, weight: .semibold))
                .foregroundStyle(fg)
        }
        .padding(.horizontal, 8).padding(.vertical, 3)
        .background(bg)
        .clipShape(RoundedRectangle(cornerRadius: 8))
    }
}

// ── Empty state ─────────────────────────────────────────────────────────────────

private struct RTEmptyState: View {
    let title: String
    let subtitle: String

    var body: some View {
        VStack(spacing: 6) {
            Spacer()
            Text(title)
                .font(.system(size: 17, weight: .bold))
                .foregroundStyle(Color.dobiTextPrimary)
            Text(subtitle)
                .font(.system(size: 14))
                .foregroundStyle(Color.dobiTextSecondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)
            Spacer()
        }
    }
}

// ── Last-revised label helper ───────────────────────────────────────────────────

private func revisedLabel(timestamp: Int64) -> (String, Color) {
    guard timestamp > 0 else { return ("Not revised yet", Color(hex: 0x94A3B8)) }
    let now = Int64(Date().timeIntervalSince1970 * 1000)
    let diff = max(0, now - timestamp)
    let hours = diff / 3_600_000
    let days = diff / 86_400_000
    let (text, bucket): (String, Int) = {
        if days == 0 && hours <= 1 { return ("Last revised just now", 0) }
        if days == 0 { return ("Last revised today", 0) }
        if days == 1 { return ("Last revised yesterday", 0) }
        if days < 7 { return ("Last revised \(days) days ago", 0) }
        if days < 14 { return ("Last revised \(days) days ago", 1) }
        return ("Last revised \(days) days ago", 2)
    }()
    let color: Color = bucket == 0 ? Color(hex: 0x22C55E) : bucket == 1 ? Color(hex: 0xF59E0B) : Color(hex: 0xEF4444)
    return (text, color)
}
