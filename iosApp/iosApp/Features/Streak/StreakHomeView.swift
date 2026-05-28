import SwiftUI

struct StreakHomeView: View {
    var childName: String = "Aarav"
    var currentStreak: Int = 5
    var onBack: () -> Void = {}
    var onUploadToday: () -> Void = {}

    private let weekDays = ["M", "T", "W", "T", "F", "S", "S"]
    private let completedDays = [true, true, true, true, true, false, false]

    var body: some View {
        ZStack(alignment: .top) {
            Color.dobiPageBg.ignoresSafeArea()

            ScrollView {
                VStack(spacing: 20) {
                    Spacer().frame(height: 80)

                    // Streak hero card
                    DobiCard(cornerRadius: 24) {
                        VStack(spacing: 16) {
                            Text("🔥")
                                .font(.system(size: 64))

                            VStack(spacing: 4) {
                                Text("\(currentStreak) Day Streak!")
                                    .font(.dobiTitle1())
                                    .foregroundStyle(Color.dobiTextPrimary)
                                Text("Keep going, \(childName)! Don't break the chain.")
                                    .font(.dobiBody())
                                    .foregroundStyle(Color.dobiTextSecondary)
                                    .multilineTextAlignment(.center)
                            }

                            // Week calendar
                            HStack(spacing: 0) {
                                ForEach(0..<7, id: \.self) { i in
                                    VStack(spacing: 6) {
                                        Text(weekDays[i])
                                            .font(.dobiSmall())
                                            .foregroundStyle(Color.dobiTextTertiary)
                                        ZStack {
                                            Circle()
                                                .fill(completedDays[i] ? Color.dobiPrimary : Color.dobiSurfaceAlt)
                                                .frame(width: 34, height: 34)
                                            if completedDays[i] {
                                                Image(systemName: "checkmark")
                                                    .font(.system(size: 12, weight: .bold))
                                                    .foregroundStyle(.white)
                                            } else if i == completedDays.lastIndex(where: { $0 }) ?? -1 + 1 {
                                                Text("📚")
                                                    .font(.system(size: 16))
                                            }
                                        }
                                    }
                                    .frame(maxWidth: .infinity)
                                }
                            }
                            .padding(.horizontal, 4)
                        }
                        .padding(24)
                    }
                    .padding(.horizontal, 20)

                    // Today's upload CTA
                    DobiCard(cornerRadius: 20) {
                        HStack(spacing: 16) {
                            ZStack {
                                RoundedRectangle(cornerRadius: 12)
                                    .fill(Color.dobiPastelYellow)
                                    .frame(width: 52, height: 52)
                                Text("📸")
                                    .font(.system(size: 26))
                            }
                            VStack(alignment: .leading, spacing: 4) {
                                Text("Upload Today's Work")
                                    .font(.dobiBodyBold())
                                    .foregroundStyle(Color.dobiTextPrimary)
                                Text("Snap a photo of what you studied today to keep your streak alive!")
                                    .font(.dobiSmall())
                                    .foregroundStyle(Color.dobiTextSecondary)
                                    .lineSpacing(2)
                            }
                            Spacer(minLength: 0)
                        }
                        .padding(16)

                        Button(action: onUploadToday) {
                            HStack {
                                Image(systemName: "camera.fill")
                                    .font(.system(size: 14))
                                Text("Upload Now")
                                    .font(.dobiCaptionBold())
                            }
                            .foregroundStyle(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                            .background(Color.dobiPrimary)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                        }
                        .buttonStyle(.plain)
                        .padding(.horizontal, 16)
                        .padding(.bottom, 16)
                    }
                    .padding(.horizontal, 20)

                    // Milestone cards
                    VStack(alignment: .leading, spacing: 12) {
                        SectionHeader(title: "Milestones")
                            .padding(.horizontal, 20)

                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 12) {
                                MilestoneCard(days: 7, emoji: "⭐️", isUnlocked: currentStreak >= 7)
                                MilestoneCard(days: 14, emoji: "🏅", isUnlocked: currentStreak >= 14)
                                MilestoneCard(days: 30, emoji: "🏆", isUnlocked: currentStreak >= 30)
                                MilestoneCard(days: 60, emoji: "💎", isUnlocked: currentStreak >= 60)
                            }
                            .padding(.horizontal, 20)
                        }
                    }

                    // Stats row
                    HStack(spacing: 12) {
                        StreakStatCard(emoji: "📅", value: "12", label: "Total Days")
                        StreakStatCard(emoji: "🔥", value: "\(currentStreak)", label: "Best Streak")
                        StreakStatCard(emoji: "📚", value: "34", label: "Topics Done")
                    }
                    .padding(.horizontal, 20)

                    Spacer().frame(height: 40)
                }
            }

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
                Text("My Streak")
                    .font(.dobiTitle3())
                    .foregroundStyle(Color.dobiTextPrimary)
                Spacer()
                Color.clear.frame(width: 36, height: 36)
            }
            .padding(.horizontal, 20)
            .padding(.top, 56)
            .background(Color.dobiPageBg)
        }
        .navigationBarHidden(true)
    }
}

private struct MilestoneCard: View {
    let days: Int
    let emoji: String
    let isUnlocked: Bool

    var body: some View {
        VStack(spacing: 8) {
            Text(emoji)
                .font(.system(size: 32))
                .opacity(isUnlocked ? 1 : 0.3)
            Text("\(days) days")
                .font(.dobiCaptionBold())
                .foregroundStyle(isUnlocked ? Color.dobiTextPrimary : Color.dobiTextTertiary)
            Text(isUnlocked ? "Unlocked!" : "Locked")
                .font(.dobiSmall())
                .foregroundStyle(isUnlocked ? Color.dobiSuccess : Color.dobiTextTertiary)
        }
        .frame(width: 90)
        .padding(.vertical, 16)
        .background(isUnlocked ? Color.dobiPastelMint : Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .overlay(RoundedRectangle(cornerRadius: 16).strokeBorder(Color.dobiBorder, lineWidth: 1))
    }
}

private struct StreakStatCard: View {
    let emoji: String
    let value: String
    let label: String

    var body: some View {
        VStack(spacing: 6) {
            Text(emoji).font(.system(size: 20))
            Text(value)
                .font(.system(size: 20, weight: .bold))
                .foregroundStyle(Color.dobiTextPrimary)
            Text(label)
                .font(.dobiSmall())
                .foregroundStyle(Color.dobiTextSecondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 14)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 14))
        .overlay(RoundedRectangle(cornerRadius: 14).strokeBorder(Color.dobiBorder, lineWidth: 1))
    }
}

#Preview {
    StreakHomeView(childName: "Aarav", currentStreak: 5)
}
