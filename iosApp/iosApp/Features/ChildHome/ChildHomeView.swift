import SwiftUI

struct ChildHomeView: View {
    let childId: String
    let childName: String
    var onNavigateToConceptList: () -> Void = {}
    var onNavigateToHomeworkLog: () -> Void = {}
    var onNavigateToClassTestPrep: () -> Void = {}
    var onNavigateToStreakHome: () -> Void = {}
    var onRequestParentAccess: () -> Void = {}

    @State private var selectedTab = 0

    var body: some View {
        ZStack(alignment: .bottom) {
            TabView(selection: $selectedTab) {
                ChildDashboardTab(
                    childName: childName,
                    onConceptList: onNavigateToConceptList,
                    onHomework: onNavigateToHomeworkLog,
                    onClassTest: onNavigateToClassTestPrep,
                    onStreak: onNavigateToStreakHome
                )
                .tag(0)

                MyLearningsView()
                    .tag(1)

                ChildProfileTab(childName: childName)
                    .tag(2)

                ChildSettingsTab(onSignOut: onRequestParentAccess)
                    .tag(3)
            }
            .tabViewStyle(.page(indexDisplayMode: .never))

            ChildBottomNav(selected: $selectedTab)
        }
        .navigationBarHidden(true)
        .ignoresSafeArea(edges: .bottom)
    }
}

// ── Dashboard tab ─────────────────────────────────────────────────────────────

private struct ChildDashboardTab: View {
    let childName: String
    var onConceptList: () -> Void
    var onHomework: () -> Void
    var onClassTest: () -> Void
    var onStreak: () -> Void

    private let actions: [(emoji: String, title: String, subtitle: String, color: Color)] = [
        ("📚", "Start Learning", "Explore concepts & topics", Color.dobiPastelSkyBlue),
        ("✏️", "Log Homework", "Record your daily homework", Color.dobiPastelMint),
        ("📝", "Class Test Prep", "AI-powered test practice", Color.dobiPastelLavender),
        ("🔥", "My Streak", "Keep your streak alive!", Color.dobiPastelYellow),
        ("🔄", "Revisit Topics", "Review what you've learnt", Color.dobiPastelPink),
        ("🏆", "Test Yourself", "Challenge your knowledge", Color.dobiPastelCream)
    ]

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                // Greeting card
                GreetingCard(childName: childName)
                    .padding(.horizontal, 20)

                // Quick actions
                SectionHeader(title: "What do you want to do?")
                    .padding(.horizontal, 20)

                LazyVGrid(columns: [GridItem(.flexible(), spacing: 14), GridItem(.flexible(), spacing: 14)], spacing: 14) {
                    ForEach(actions.indices, id: \.self) { i in
                        let a = actions[i]
                        QuickActionCard(
                            emoji: a.emoji,
                            title: a.title,
                            subtitle: a.subtitle,
                            background: a.color
                        ) {
                            switch i {
                            case 0: onConceptList()
                            case 1: onHomework()
                            case 2: onClassTest()
                            case 3: onStreak()
                            default: break
                            }
                        }
                    }
                }
                .padding(.horizontal, 20)

                Spacer().frame(height: 100)
            }
            .padding(.top, 56)
        }
        .background(Color.dobiPageBg)
    }
}

private struct GreetingCard: View {
    let childName: String

    private var greeting: String {
        let h = Calendar.current.component(.hour, from: Date())
        if h < 12 { return "Good morning" }
        if h < 17 { return "Good afternoon" }
        return "Good evening"
    }

    var body: some View {
        DobiCard(cornerRadius: 20) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text("\(greeting),")
                        .font(.dobiCaption())
                        .foregroundStyle(Color.dobiTextSecondary)
                    Text(childName.components(separatedBy: " ").first ?? childName)
                        .font(.dobiTitle1())
                        .foregroundStyle(Color.dobiTextPrimary)
                    Text("What are we learning today? 🎯")
                        .font(.dobiCaption())
                        .foregroundStyle(Color.dobiTextSecondary)
                }
                Spacer()
                EmojiAvatar(emoji: "🎓", size: 60, background: .dobiPrimaryContainer)
            }
            .padding(20)
        }
    }
}

private struct QuickActionCard: View {
    let emoji: String
    let title: String
    let subtitle: String
    let background: Color
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 10) {
                Text(emoji).font(.system(size: 30))
                VStack(alignment: .leading, spacing: 3) {
                    Text(title)
                        .font(.dobiCaptionBold())
                        .foregroundStyle(Color.dobiTextPrimary)
                    Text(subtitle)
                        .font(.dobiSmall())
                        .foregroundStyle(Color.dobiTextSecondary)
                        .lineLimit(2)
                }
            }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(background)
            .clipShape(RoundedRectangle(cornerRadius: 16))
        }
        .buttonStyle(.plain)
    }
}

// ── My Learnings tab ──────────────────────────────────────────────────────────

struct MyLearningsView: View {
    var body: some View {
        VStack(spacing: 20) {
            Spacer().frame(height: 60)
            EmojiAvatar(emoji: "📖", size: 80, background: .dobiPastelSkyBlue)
            Text("My Learnings")
                .font(.dobiTitle2())
                .foregroundStyle(Color.dobiTextPrimary)
            Text("Your quiz results, uploads\nand learning history will appear here.")
                .font(.dobiBody())
                .foregroundStyle(Color.dobiTextSecondary)
                .multilineTextAlignment(.center)
            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.dobiPageBg)
    }
}

// ── Profile tab ───────────────────────────────────────────────────────────────

private struct ChildProfileTab: View {
    let childName: String
    var body: some View {
        VStack(spacing: 20) {
            Spacer().frame(height: 60)
            EmojiAvatar(emoji: "👦", size: 80, background: .dobiPrimaryContainer)
            Text(childName)
                .font(.dobiTitle2())
                .foregroundStyle(Color.dobiTextPrimary)
            Text("Profile & preferences")
                .font(.dobiBody())
                .foregroundStyle(Color.dobiTextSecondary)
            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.dobiPageBg)
    }
}

// ── Settings tab ──────────────────────────────────────────────────────────────

private struct ChildSettingsTab: View {
    var onSignOut: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            Spacer().frame(height: 60)
            List {
                Section {
                    SettingsRow(icon: "lock.shield", title: "Privacy & Data", color: .dobiPrimary, action: {})
                    SettingsRow(icon: "questionmark.circle", title: "FAQ", color: .dobiWarning, action: {})
                    SettingsRow(icon: "headphones", title: "Help & Support", color: .dobiSuccess, action: {})
                }
                Section {
                    SettingsRow(icon: "rectangle.portrait.and.arrow.right", title: "Switch to Parent", color: .dobiError, action: onSignOut)
                }
            }
            .scrollContentBackground(.hidden)
            .background(Color.dobiPageBg)
        }
        .background(Color.dobiPageBg)
    }
}

struct SettingsRow: View {
    let icon: String
    let title: String
    let color: Color
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 14) {
                ZStack {
                    RoundedRectangle(cornerRadius: 8)
                        .fill(color.opacity(0.12))
                        .frame(width: 34, height: 34)
                    Image(systemName: icon)
                        .font(.system(size: 15, weight: .medium))
                        .foregroundStyle(color)
                }
                Text(title)
                    .font(.dobiBody())
                    .foregroundStyle(Color.dobiTextPrimary)
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(Color.dobiTextTertiary)
            }
        }
        .listRowBackground(Color.white)
    }
}

// ── Bottom nav ────────────────────────────────────────────────────────────────

private struct ChildBottomNav: View {
    @Binding var selected: Int
    private let items: [(icon: String, label: String)] = [
        ("house.fill", "Home"),
        ("book.fill", "Learnings"),
        ("person.fill", "Profile"),
        ("gearshape.fill", "Settings")
    ]

    var body: some View {
        HStack(spacing: 0) {
            ForEach(items.indices, id: \.self) { i in
                Button {
                    withAnimation(.spring(duration: 0.3)) { selected = i }
                } label: {
                    VStack(spacing: 4) {
                        Image(systemName: items[i].icon)
                            .font(.system(size: 20, weight: selected == i ? .semibold : .regular))
                            .foregroundStyle(selected == i ? Color.dobiPrimary : Color.dobiTextTertiary)
                            .scaleEffect(selected == i ? 1.1 : 1)
                            .animation(.spring(duration: 0.2), value: selected)
                        Text(items[i].label)
                            .font(.dobiSmall())
                            .foregroundStyle(selected == i ? Color.dobiPrimary : Color.dobiTextTertiary)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 10)
                }
                .buttonStyle(.plain)
            }
        }
        .background(
            Color.white
                .shadow(color: .black.opacity(0.08), radius: 12, x: 0, y: -4)
        )
        .ignoresSafeArea(edges: .bottom)
    }
}

#Preview {
    ChildHomeView(childId: "1", childName: "Aarav Sharma")
}
