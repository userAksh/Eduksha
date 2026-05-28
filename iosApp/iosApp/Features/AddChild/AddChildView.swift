import SwiftUI

// MARK: - Private types

private enum ACGender: String, CaseIterable {
    case boy, girl, other
    var display: String { switch self { case .boy: "Boy"; case .girl: "Girl"; case .other: "Other" } }
    var emoji: String   { switch self { case .boy: "👦"; case .girl: "👧"; case .other: "🧒" } }
}

private struct ACDraft {
    var name: String = ""
    var gender: ACGender = .boy
    var grade: Int = 6
    var board: String = "CBSE"
    var pin: String = ""

    static let supportedGrades: [Int] = [6, 7]
    static let upcomingGrades: [Int] = (1...12).filter { ![6, 7].contains($0) }
    static let supportedBoards: [String] = ["CBSE"]
    static let upcomingBoards: [String] = ["ICSE", "State Board"]

    var isValid: Bool { !name.trimmingCharacters(in: .whitespaces).isEmpty && pin.count == 4 }
}

private enum ACStep { case welcome, details, done }

// MARK: - ViewModel

@MainActor
private final class AddChildViewModel: ObservableObject {
    @Published var step: ACStep = .welcome
    @Published var draft = ACDraft()
    @Published var isSaving = false
    @Published var error: String? = nil
    @Published var addedChildren: [ChildProfile] = []

    private(set) var editingChildId: String? = nil

    var isEditing: Bool { editingChildId != nil }

    func startAdding() {
        editingChildId = nil
        draft = ACDraft()
        step = .details
    }

    func goBack() {
        switch step {
        case .details:
            editingChildId = nil
            draft = ACDraft()
            step = .welcome
        case .done:
            if let last = addedChildren.last {
                editingChildId = last.id
                draft = ACDraft(
                    name: last.name,
                    gender: ACGender(rawValue: last.gender) ?? .boy,
                    grade: Int(last.grade) ?? 6,
                    board: last.board,
                    pin: ""
                )
            }
            step = .details
        case .welcome:
            break
        }
    }

    func addAnother() {
        draft = ACDraft()
        editingChildId = nil
        step = .details
    }

    func save(parentUID: String) async {
        guard draft.isValid, !parentUID.isEmpty else { return }
        isSaving = true
        error = nil
        do {
            let id = try await FirebaseService.shared.saveChild(
                parentUID: parentUID,
                childId: editingChildId,
                name: draft.name.trimmingCharacters(in: .whitespaces),
                gender: draft.gender.rawValue,
                grade: draft.grade,
                board: draft.board,
                pin: draft.pin
            )
            let profile = ChildProfile(
                id: id,
                name: draft.name.trimmingCharacters(in: .whitespaces),
                grade: "\(draft.grade)",
                board: draft.board,
                gender: draft.gender.rawValue
            )
            if let eid = editingChildId {
                addedChildren = addedChildren.map { $0.id == eid ? profile : $0 }
            } else {
                addedChildren.append(profile)
            }
            draft = ACDraft()
            editingChildId = nil
            step = .done
        } catch {
            self.error = error.localizedDescription
        }
        isSaving = false
    }
}

// MARK: - Root view

struct AddChildView: View {
    var onDone: () -> Void = {}
    var onBack: () -> Void = {}

    @EnvironmentObject private var appState: AppState
    @StateObject private var vm = AddChildViewModel()

    var body: some View {
        VStack(spacing: 0) {
            ACTopBar {
                if vm.step == .welcome { onBack() } else { vm.goBack() }
            }

            if vm.step != .welcome {
                ACStepperBar(currentStep: vm.step == .details ? 1 : 2)
                    .transition(.opacity)
            }

            Group {
                switch vm.step {
                case .welcome:
                    ACWelcomeStep(onStart: vm.startAdding)
                case .details:
                    ACDetailsStep(vm: vm, parentUID: appState.parentUID ?? "")
                case .done:
                    ACDoneStep(children: vm.addedChildren, onAddAnother: vm.addAnother, onFinish: onDone)
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .background(Color.dobiPageBg.ignoresSafeArea())
        .navigationBarHidden(true)
        .animation(.easeInOut(duration: 0.22), value: vm.step == .welcome)
    }
}

// MARK: - Top bar

private struct ACTopBar: View {
    let onBack: () -> Void

    var body: some View {
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
            .buttonStyle(.plain)

            Spacer()

            Text("Add Child")
                .font(.system(size: 17, weight: .bold))
                .foregroundStyle(Color.dobiTextPrimary)

            Spacer()

            Color.clear.frame(width: 36, height: 36)
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 12)
    }
}

// MARK: - Stepper bar

private struct ACStepperBar: View {
    let currentStep: Int
    private let labels = ["Details", "Done"]

    var body: some View {
        HStack(alignment: .top, spacing: 0) {
            ForEach(Array(labels.enumerated()), id: \.offset) { index, label in
                let num = index + 1
                let isDone = num < currentStep
                let isActive = num == currentStep

                Column(label: label, number: num, isDone: isDone, isActive: isActive)

                if index < labels.count - 1 {
                    Rectangle()
                        .fill(currentStep > num ? Color.dobiPrimary : Color.dobiPrimaryContainer)
                        .frame(maxWidth: .infinity)
                        .frame(height: 2)
                        .padding(.top, 15)
                }
            }
        }
        .padding(.horizontal, 24)
        .padding(.vertical, 14)
        .background(Color.white)
    }

    private struct Column: View {
        let label: String
        let number: Int
        let isDone: Bool
        let isActive: Bool

        var body: some View {
            VStack(spacing: 4) {
                ZStack {
                    Circle()
                        .fill(isDone || isActive ? Color.dobiPrimary : Color.dobiPrimaryContainer)
                        .frame(width: 32, height: 32)
                    if isDone {
                        Image(systemName: "checkmark")
                            .font(.system(size: 13, weight: .bold))
                            .foregroundStyle(.white)
                    } else {
                        Text("\(number)")
                            .font(.system(size: 13, weight: .bold))
                            .foregroundStyle(isActive ? .white : Color.dobiPrimary)
                    }
                }
                Text(label)
                    .font(.system(size: 11, weight: isActive ? .semibold : .regular))
                    .foregroundStyle(isActive ? Color.dobiPrimary : Color.dobiTextTertiary)
            }
        }
    }
}

// MARK: - Welcome step

private struct ACWelcomeStep: View {
    let onStart: () -> Void

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                Spacer().frame(height: 12)

                // Illustration
                ZStack {
                    Circle()
                        .fill(Color.dobiPrimaryContainer)
                        .frame(width: 200, height: 200)
                    Text("👨‍👩‍👧")
                        .font(.system(size: 88))
                }

                Spacer().frame(height: 16)

                Text("Add Your Child")
                    .font(.system(size: 26, weight: .bold))
                    .foregroundStyle(Color.dobiPrimary)

                Spacer().frame(height: 8)

                Text("Add your child to start tracking\ntheir learning journey.")
                    .font(.system(size: 14))
                    .foregroundStyle(Color.dobiTextSecondary)
                    .multilineTextAlignment(.center)

                Spacer().frame(height: 24)

                VStack(spacing: 14) {
                    ACFeatureRow(emoji: "📊", bg: Color.dobiPrimaryContainer,
                                 title: "Track progress",
                                 subtitle: "See what your child is learning every day.")
                    ACFeatureRow(emoji: "🏅", bg: Color(hex: 0xFCE7F3),
                                 title: "Spot what matters",
                                 subtitle: "Identify strengths and topics that need attention.")
                    ACFeatureRow(emoji: "🛡️", bg: Color.dobiSuccessContainer,
                                 title: "Encourage better",
                                 subtitle: "Motivate your child with goals and rewards.")
                }
                .padding(.horizontal, 24)

                Spacer().frame(height: 28)

                Button(action: onStart) {
                    HStack(spacing: 8) {
                        Image(systemName: "plus")
                            .font(.system(size: 14, weight: .bold))
                        Text("Add Your First Child")
                            .font(.system(size: 15, weight: .bold))
                    }
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 56)
                    .background(Color.dobiPrimary)
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                }
                .buttonStyle(.plain)
                .padding(.horizontal, 24)

                Spacer().frame(height: 14)

                HStack(alignment: .top, spacing: 10) {
                    Text("🔒")
                        .font(.system(size: 18))
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Your child's data is safe and secure")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundStyle(Color.dobiTextPrimary)
                        Text("We never share your information.")
                            .font(.system(size: 12))
                            .foregroundStyle(Color.dobiTextSecondary)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color.dobiPrimarySoft)
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .padding(.horizontal, 24)

                Spacer().frame(height: 40)
            }
        }
    }
}

private struct ACFeatureRow: View {
    let emoji: String
    let bg: Color
    let title: String
    let subtitle: String

    var body: some View {
        HStack(spacing: 16) {
            ZStack {
                RoundedRectangle(cornerRadius: 14)
                    .fill(bg)
                    .frame(width: 60, height: 60)
                Text(emoji).font(.system(size: 28))
            }
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(Color.dobiTextPrimary)
                Text(subtitle)
                    .font(.system(size: 12))
                    .foregroundStyle(Color.dobiTextSecondary)
                    .lineSpacing(3)
            }
            Spacer()
        }
    }
}

// MARK: - Details step

private struct ACDetailsStep: View {
    @ObservedObject var vm: AddChildViewModel
    let parentUID: String

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                Spacer().frame(height: 20)

                VStack(alignment: .leading, spacing: 4) {
                    Text("Child Details")
                        .font(.system(size: 22, weight: .bold))
                        .foregroundStyle(Color.dobiTextPrimary)
                    Text("Tell us about your child.")
                        .font(.system(size: 14))
                        .foregroundStyle(Color.dobiTextSecondary)
                }
                .padding(.horizontal, 24)

                Spacer().frame(height: 24)

                // Avatar — updates with gender
                HStack {
                    Spacer()
                    ZStack {
                        Circle()
                            .fill(Color.white)
                            .frame(width: 96, height: 96)
                            .overlay(Circle().strokeBorder(Color.dobiBorder, lineWidth: 2))
                        Text(vm.draft.gender.emoji)
                            .font(.system(size: 52))
                    }
                    Spacer()
                }

                Spacer().frame(height: 24)

                // Name
                ACFieldLabel("Child's Name").padding(.horizontal, 24)
                Spacer().frame(height: 8)
                ACOutlinedField {
                    HStack(spacing: 10) {
                        Image(systemName: "person.fill")
                            .foregroundStyle(Color.dobiTextSecondary)
                            .frame(width: 20)
                        TextField("Enter child's name", text: $vm.draft.name)
                            .font(.system(size: 15))
                            .foregroundStyle(Color.dobiTextPrimary)
                            .autocorrectionDisabled()
                            .textContentType(.name)
                    }
                    .padding(.horizontal, 14)
                    .padding(.vertical, 14)
                }
                .padding(.horizontal, 24)

                Spacer().frame(height: 18)

                // Gender
                ACFieldLabel("Gender").padding(.horizontal, 24)
                Spacer().frame(height: 10)
                HStack(spacing: 10) {
                    ForEach(ACGender.allCases, id: \.self) { g in
                        let sel = vm.draft.gender == g
                        Button { vm.draft.gender = g } label: {
                            HStack(spacing: 5) {
                                Text(g.emoji).font(.system(size: 16))
                                Text(g.display)
                                    .font(.system(size: 14, weight: .semibold))
                                    .foregroundStyle(sel ? .white : Color.dobiTextSecondary)
                            }
                            .frame(maxWidth: .infinity)
                            .frame(height: 48)
                            .background(sel ? Color.dobiPrimary : Color.white)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                            .overlay(RoundedRectangle(cornerRadius: 12)
                                .strokeBorder(sel ? Color.dobiPrimary : Color.dobiBorder, lineWidth: sel ? 0 : 1.5))
                        }
                        .buttonStyle(.plain)
                        .animation(.spring(duration: 0.2), value: sel)
                    }
                }
                .padding(.horizontal, 24)

                Spacer().frame(height: 18)

                // Class + Board side by side
                HStack(alignment: .top, spacing: 12) {
                    // Class
                    VStack(alignment: .leading, spacing: 8) {
                        ACFieldLabel("Class")
                        ACDropdownButton(
                            label: "Class \(vm.draft.grade)",
                            supportedItems: ACDraft.supportedGrades.map { "Class \($0)" },
                            upcomingItems: ACDraft.upcomingGrades.map { "Class \($0)" },
                            selectedLabel: "Class \(vm.draft.grade)"
                        ) { label in
                            if let g = Int(label.replacingOccurrences(of: "Class ", with: "")) {
                                vm.draft.grade = g
                            }
                        }
                    }

                    // Board
                    VStack(alignment: .leading, spacing: 8) {
                        ACFieldLabel("Board")
                        ACDropdownButton(
                            label: vm.draft.board,
                            supportedItems: ACDraft.supportedBoards,
                            upcomingItems: ACDraft.upcomingBoards,
                            selectedLabel: vm.draft.board
                        ) { label in
                            vm.draft.board = label
                        }
                    }
                }
                .padding(.horizontal, 24)

                Spacer().frame(height: 22)

                // PIN
                ACFieldLabel("4-digit PIN for this child").padding(.horizontal, 24)
                Spacer().frame(height: 6)
                Text("Parent or child will enter this PIN to open this profile.")
                    .font(.system(size: 12))
                    .foregroundStyle(Color.dobiTextSecondary)
                    .padding(.horizontal, 24)
                Spacer().frame(height: 10)
                ACPinInput(pin: $vm.draft.pin)
                    .padding(.horizontal, 24)

                // Error
                if let err = vm.error {
                    Spacer().frame(height: 12)
                    HStack(spacing: 8) {
                        Text("⚠️")
                        Text(err)
                            .font(.system(size: 13))
                            .foregroundStyle(Color.dobiError)
                    }
                    .padding(.horizontal, 14)
                    .padding(.vertical, 10)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color.dobiErrorContainer)
                    .clipShape(RoundedRectangle(cornerRadius: 10))
                    .padding(.horizontal, 24)
                }

                Spacer().frame(height: 28)

                // Save button
                Button {
                    Task { await vm.save(parentUID: parentUID) }
                } label: {
                    ZStack {
                        if vm.isSaving {
                            ProgressView().tint(.white)
                        } else {
                            HStack(spacing: 8) {
                                Text(vm.isEditing ? "Update Child" : "Save Child")
                                    .font(.system(size: 15, weight: .bold))
                                    .foregroundStyle(.white)
                                Image(systemName: "arrow.right")
                                    .font(.system(size: 14, weight: .bold))
                                    .foregroundStyle(.white)
                            }
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 56)
                    .background(vm.draft.isValid ? Color.dobiPrimary : Color.dobiPrimaryLight.opacity(0.35))
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                }
                .buttonStyle(.plain)
                .disabled(!vm.draft.isValid || vm.isSaving)
                .padding(.horizontal, 24)

                Spacer().frame(height: 40)
            }
        }
    }
}

// MARK: - Dropdown button (class / board)

private struct ACDropdownButton: View {
    let label: String
    let supportedItems: [String]
    let upcomingItems: [String]
    let selectedLabel: String
    let onSelect: (String) -> Void

    @State private var expanded = false

    var body: some View {
        Menu {
            ForEach(supportedItems, id: \.self) { item in
                Button(item) { onSelect(item) }
            }
            if !upcomingItems.isEmpty {
                Divider()
                ForEach(upcomingItems, id: \.self) { item in
                    Button("\(item) — Coming soon") {}
                        .disabled(true)
                }
            }
        } label: {
            ACOutlinedField {
                HStack {
                    Text(label)
                        .font(.system(size: 14))
                        .foregroundStyle(Color.dobiTextPrimary)
                    Spacer()
                    Image(systemName: "chevron.down")
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(Color.dobiTextSecondary)
                }
                .padding(.horizontal, 14)
                .padding(.vertical, 14)
            }
        }
        .menuStyle(.automatic)
    }
}

// MARK: - PIN input

private struct ACPinInput: View {
    @Binding var pin: String
    @FocusState private var focused: Bool

    var body: some View {
        ZStack {
            TextField("", text: $pin)
                .keyboardType(.numberPad)
                .focused($focused)
                .opacity(0)
                .frame(width: 1, height: 1)
                .onChange(of: pin) { new in
                    let digits = new.filter(\.isNumber)
                    if digits.count <= 4 { pin = digits }
                }

            HStack(spacing: 10) {
                ForEach(0..<4, id: \.self) { i in
                    let char: Character? = i < pin.count ? pin[pin.index(pin.startIndex, offsetBy: i)] : nil
                    let isCurrent = pin.count == i

                    ZStack {
                        RoundedRectangle(cornerRadius: 12)
                            .fill(char != nil ? Color.dobiPrimaryContainer : Color.white)
                        RoundedRectangle(cornerRadius: 12)
                            .strokeBorder(
                                (isCurrent || char != nil) ? Color.dobiPrimary : Color.dobiBorder,
                                lineWidth: isCurrent ? 2 : 1.5
                            )
                        Text(char != nil ? "●" : (isCurrent ? "·" : ""))
                            .font(char != nil ? .system(size: 20, weight: .bold) : .system(size: 24))
                            .foregroundStyle(char != nil ? Color.dobiPrimary : Color.dobiTextTertiary)
                    }
                    .frame(height: 52)
                }
            }
        }
        .onTapGesture { focused = true }
    }
}

// MARK: - Done step

private let acChildCardColors: [(bg: Color, accent: Color)] = [
    (Color(hex: 0xDBEAFE), Color(hex: 0x1D4ED8)),
    (Color(hex: 0xFCE7F3), Color(hex: 0xBE185D)),
    (Color(hex: 0xD1FAE5), Color(hex: 0x047857)),
    (Color(hex: 0xFEF3C7), Color(hex: 0xB45309)),
    (Color(hex: 0xEDE9FE), Color(hex: 0x6D28D9)),
]

private struct ACDoneStep: View {
    let children: [ChildProfile]
    let onAddAnother: () -> Void
    let onFinish: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                Spacer().frame(height: 16)

                Text("Added Children")
                    .font(.system(size: 22, weight: .bold))
                    .foregroundStyle(Color.dobiTextPrimary)
                    .padding(.horizontal, 24)
                Spacer().frame(height: 4)
                Text("You can manage and switch between profiles anytime.")
                    .font(.system(size: 14))
                    .foregroundStyle(Color.dobiTextSecondary)
                    .padding(.horizontal, 24)

                Spacer().frame(height: 20)

                ForEach(Array(children.enumerated()), id: \.element.id) { index, child in
                    let colors = acChildCardColors[index % acChildCardColors.count]
                    let genderEmoji: String = {
                        switch child.gender {
                        case "girl":  return "👧"
                        case "other": return "🧒"
                        default:      return "👦"
                        }
                    }()

                    HStack(spacing: 14) {
                        ZStack {
                            Circle().fill(colors.bg).frame(width: 52, height: 52)
                            Text(genderEmoji).font(.system(size: 26))
                        }

                        VStack(alignment: .leading, spacing: 2) {
                            Text(child.name)
                                .font(.system(size: 14, weight: .bold))
                                .foregroundStyle(Color.dobiTextPrimary)
                            Text("Class \(child.grade) · \(child.board)")
                                .font(.system(size: 12))
                                .foregroundStyle(Color.dobiTextSecondary)
                        }

                        Spacer()

                        if index == 0 {
                            Text("Primary")
                                .font(.system(size: 11, weight: .bold))
                                .foregroundStyle(Color.dobiSuccess)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 4)
                                .background(Color.dobiSuccessContainer)
                                .clipShape(RoundedRectangle(cornerRadius: 6))
                        } else {
                            Image(systemName: "arrow.right")
                                .font(.system(size: 14))
                                .foregroundStyle(Color.dobiBorder)
                        }
                    }
                    .padding(14)
                    .background(Color.white)
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                    .overlay(RoundedRectangle(cornerRadius: 16).strokeBorder(Color.dobiBorder, lineWidth: 1))
                    .padding(.horizontal, 24)

                    Spacer().frame(height: 10)
                }

                Spacer().frame(height: 8)

                // Add another
                Button(action: onAddAnother) {
                    HStack(spacing: 10) {
                        ZStack {
                            Circle()
                                .strokeBorder(Color.dobiPrimary, lineWidth: 1.5)
                                .frame(width: 24, height: 24)
                            Image(systemName: "plus")
                                .font(.system(size: 12, weight: .bold))
                                .foregroundStyle(Color.dobiPrimary)
                        }
                        Text("Add Another Child")
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundStyle(Color.dobiPrimary)
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 56)
                    .background(Color.white)
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                    .overlay(RoundedRectangle(cornerRadius: 16).strokeBorder(Color.dobiPrimary.opacity(0.4), lineWidth: 1.5))
                }
                .buttonStyle(.plain)
                .padding(.horizontal, 24)

                Spacer().frame(height: 12)

                // Continue to Home
                Button(action: onFinish) {
                    Text("Continue to Home")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 56)
                        .background(Color.dobiPrimary)
                        .clipShape(RoundedRectangle(cornerRadius: 16))
                }
                .buttonStyle(.plain)
                .padding(.horizontal, 24)

                Spacer().frame(height: 40)
            }
        }
    }
}

// MARK: - Shared helpers

private struct ACFieldLabel: View {
    let text: String
    init(_ text: String) { self.text = text }

    var body: some View {
        Text(text)
            .font(.system(size: 13, weight: .semibold))
            .foregroundStyle(Color.dobiTextPrimary)
    }
}

private struct ACOutlinedField<Content: View>: View {
    @ViewBuilder let content: Content

    var body: some View {
        content
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 14))
            .overlay(RoundedRectangle(cornerRadius: 14).strokeBorder(Color.dobiBorder, lineWidth: 1.5))
    }
}

#Preview {
    AddChildView()
        .environmentObject(AppState())
}
