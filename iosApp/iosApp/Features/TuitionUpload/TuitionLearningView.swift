import SwiftUI
import PhotosUI

// MARK: - Local models

private struct TuitionSubjectData: Equatable {
    let name: String
    let color: String   // hex e.g. "#22C55E"
    let icon: String
}

private struct TuitionEntry: Identifiable {
    let id: String      // = subject.name
    var subject: TuitionSubjectData
    var topic: String = ""
    var images: [UIImage] = []
    init(subject: TuitionSubjectData) { self.id = subject.name; self.subject = subject }
}

// MARK: - Subject palettes (mirrors Android DEFAULT_TUITION_SUBJECTS / HOMEWORK_TUITION_SUBJECTS)

private let tuitionWorkSubjects: [TuitionSubjectData] = [
    .init(name: "Science",        color: "#22C55E", icon: "🔬"),
    .init(name: "Mathematics",    color: "#3B82F6", icon: "🔢"),
    .init(name: "English",        color: "#EC4899", icon: "📖"),
    .init(name: "Social Science", color: "#F59E0B", icon: "🌍"),
    .init(name: "Hindi",          color: "#EF4444", icon: "✍️"),
    .init(name: "Sanskrit",       color: "#8B5CF6", icon: "📜"),
]

private let tuitionHwSubjects: [TuitionSubjectData] = [
    .init(name: "Science",        color: "#0EA5E9", icon: "🔬"),
    .init(name: "Mathematics",    color: "#6366F1", icon: "🔢"),
    .init(name: "English",        color: "#D946EF", icon: "📖"),
    .init(name: "Social Science", color: "#F97316", icon: "🌍"),
    .init(name: "Hindi",          color: "#DC2626", icon: "✍️"),
    .init(name: "Sanskrit",       color: "#7C3AED", icon: "📜"),
]

// MARK: - Kind-specific copy

private struct KindCopy {
    let subtitle: String
    let topicPlaceholder: String
    let photoHint: String

    static func forKind(_ kind: String) -> KindCopy {
        switch kind {
        case "tuition_work_learning":
            return .init(
                subtitle: "What did you cover in today's tuition class?",
                topicPlaceholder: "e.g. Photosynthesis",
                photoHint: "Snap pages from the notes or lessons your tutor covered."
            )
        case "tuition_work_written":
            return .init(
                subtitle: "What written work did you do in today's tuition?",
                topicPlaceholder: "e.g. Fractions exercise",
                photoHint: "Snap the pages where you wrote or solved questions."
            )
        case "tuition_work_test":
            return .init(
                subtitle: "What test did your tutor give you today?",
                topicPlaceholder: "e.g. Algebra unit test",
                photoHint: "Snap the test paper(s) — questions and your answers."
            )
        case "tuition_hw_learning":
            return .init(
                subtitle: "What learning did your tutor assign for home?",
                topicPlaceholder: "e.g. Read about cells",
                photoHint: "Snap the pages or notes you went through at home."
            )
        case "tuition_hw_written":
            return .init(
                subtitle: "What written homework did your tutor give you?",
                topicPlaceholder: "e.g. Linear equations Q1–Q10",
                photoHint: "Snap the homework you finished at home."
            )
        default:
            return .init(
                subtitle: "Tell us what you covered today.",
                topicPlaceholder: "Topic",
                photoHint: "Add photos of your work."
            )
        }
    }
}

private func kindTitle(_ kind: String) -> String {
    switch kind {
    case "tuition_work_learning": return "Today's Learning"
    case "tuition_work_written":  return "Written Work"
    case "tuition_work_test":     return "Today's Test"
    case "tuition_hw_learning":   return "Homework — Today's Learning"
    case "tuition_hw_written":    return "Homework — Written Work"
    default:                      return "Tuition Work"
    }
}

private func saveLabel(_ kind: String) -> String {
    switch kind {
    case "tuition_work_learning": return "Save Today's Learning"
    case "tuition_work_written":  return "Save Written Work"
    case "tuition_work_test":     return "Save Today's Test"
    case "tuition_hw_learning":   return "Save Homework Learning"
    case "tuition_hw_written":    return "Save Homework"
    default:                      return "Save Tuition Work"
    }
}

private func tuitionColorFromHex(_ hex: String) -> Color {
    let clean = hex.hasPrefix("#") ? String(hex.dropFirst()) : hex
    guard let value = UInt32(clean, radix: 16) else { return Color(hex: 0x7C3AED) }
    return Color(hex: value)
}

// MARK: - Main view

struct TuitionLearningView: View {
    let kind: String
    let childId: String
    let grade: Int
    var onBack: () -> Void = {}
    var onSaved: () -> Void = {}

    @State private var tutorHonorific: String = UserDefaults.standard.string(forKey: "tutor_honorific") ?? ""
    @State private var tutorName: String = UserDefaults.standard.string(forKey: "tutor_name") ?? ""
    @State private var selectedDate = Date()
    @State private var showDatePicker = false
    @State private var entries: [TuitionEntry] = []
    @State private var isSaving = false
    @State private var isValidatingPhotos = false
    @State private var saveError: String? = nil

    private var isHomework: Bool { kind.hasPrefix("tuition_hw") }
    private var availableSubjects: [TuitionSubjectData] { isHomework ? tuitionHwSubjects : tuitionWorkSubjects }
    private var selectedNames: Set<String> { Set(entries.map { $0.subject.name }) }
    private var copy: KindCopy { KindCopy.forKind(kind) }

    private var formattedDate: String {
        let fmt = DateFormatter()
        fmt.dateFormat = "dd MMM yyyy"
        return fmt.string(from: selectedDate)
    }

    private var canSave: Bool {
        !tutorName.trimmingCharacters(in: .whitespaces).isEmpty &&
        !entries.isEmpty &&
        entries.allSatisfy { !$0.topic.trimmingCharacters(in: .whitespaces).isEmpty && !$0.images.isEmpty } &&
        !isSaving && !isValidatingPhotos
    }

    private var validationHint: String? {
        guard !canSave && !entries.isEmpty else { return nil }
        if let entry = entries.first(where: { $0.topic.trimmingCharacters(in: .whitespaces).isEmpty }) {
            return "Add a topic for \(entry.subject.name) to continue."
        }
        if let entry = entries.first(where: { $0.images.isEmpty }) {
            return "Add at least one photo for \(entry.subject.name)."
        }
        if tutorName.trimmingCharacters(in: .whitespaces).isEmpty {
            return "Add your tutor's name above."
        }
        return nil
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
                Text(kindTitle(kind))
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

                    // Subtitle
                    Text(copy.subtitle)
                        .font(.system(size: 13))
                        .foregroundStyle(Color.dobiTextSecondary)
                        .padding(.top, 4)

                    // Tutor strip
                    TLTutorStrip(honorific: $tutorHonorific, name: $tutorName)
                        .padding(.top, 16)

                    // Date
                    TLFieldLabel(text: "Date")
                        .padding(.top, 20)
                    TLDateRow(displayDate: formattedDate) { showDatePicker = true }
                        .padding(.top, 8)

                    // Subject chips
                    TLFieldLabel(text: "Subjects covered today")
                        .padding(.top, 20)
                    Text("Tap any subject you studied today. Pick more than one if you covered many.")
                        .font(.system(size: 12))
                        .foregroundStyle(Color.dobiTextSecondary)
                        .padding(.top, 4)
                    TLSubjectChipsRow(
                        subjects: availableSubjects,
                        selectedNames: selectedNames,
                        onToggle: toggleSubject
                    )
                    .padding(.top, 12)

                    // Per-subject entry cards
                    if !entries.isEmpty {
                        TLFieldLabel(text: "Topic + photos for each subject")
                            .padding(.top, 20)
                        Text("Tell us the topic and add a few photos for each subject.")
                            .font(.system(size: 12))
                            .foregroundStyle(Color.dobiTextSecondary)
                            .padding(.top, 4)
                        ForEach($entries) { $entry in
                            SubjectEntryCard(
                                entry: $entry,
                                topicPlaceholder: copy.topicPlaceholder,
                                photoHint: copy.photoHint,
                                onRemove: { entries.removeAll { $0.id == entry.id } }
                            )
                            .padding(.top, 12)
                        }
                    }

                    // Validation hint
                    if let hint = validationHint {
                        Text(hint)
                            .font(.system(size: 12))
                            .foregroundStyle(Color.dobiTextSecondary)
                            .padding(.top, 12)
                    }

                    // Save error
                    if let err = saveError {
                        Text(err)
                            .font(.system(size: 12))
                            .foregroundStyle(Color(hex: 0xEF4444))
                            .padding(.top, 8)
                    }

                    // Save button
                    Button(action: handleSave) {
                        ZStack {
                            if isValidatingPhotos {
                                HStack(spacing: 8) {
                                    ProgressView().scaleEffect(0.9).tint(Color.white)
                                    Text("Checking photos…")
                                        .font(.system(size: 15, weight: .semibold))
                                        .foregroundStyle(Color.white)
                                }
                            } else if isSaving {
                                ProgressView().scaleEffect(0.9).tint(Color.white)
                            } else {
                                Text(saveLabel(kind))
                                    .font(.system(size: 15, weight: .semibold))
                                    .foregroundStyle(Color.white)
                            }
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                        .background(canSave ? Color(hex: 0x7C3AED) : Color(hex: 0xDDD6FE))
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                    }
                    .buttonStyle(.plain)
                    .disabled(!canSave)
                    .padding(.top, 32)
                    .padding(.bottom, 40)
                }
                .padding(.horizontal, 20)
                .padding(.top, 8)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.dobiPageBg.ignoresSafeArea())
        .navigationBarHidden(true)
        .sheet(isPresented: $showDatePicker) {
            VStack {
                DatePicker("", selection: $selectedDate, displayedComponents: .date)
                    .datePickerStyle(.graphical)
                    .tint(Color(hex: 0x7C3AED))
                    .padding()
                Button("Done") { showDatePicker = false }
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(Color(hex: 0x7C3AED))
                    .padding(.bottom, 20)
            }
            .presentationDetents([.medium])
        }
    }

    private func toggleSubject(_ subject: TuitionSubjectData) {
        if let idx = entries.firstIndex(where: { $0.subject.name == subject.name }) {
            entries.remove(at: idx)
        } else {
            entries.append(TuitionEntry(subject: subject))
        }
    }

    private func handleSave() {
        guard canSave else { return }
        saveError = nil
        let capturedEntries = entries
        let dateMillis = Int64(selectedDate.timeIntervalSince1970 * 1000)
        let artefactLabel = isHomework ? "tuition homework" : "tuition class work"
        let savedTutorName = tutorName.trimmingCharacters(in: .whitespaces)
        let savedTutorHonorific = tutorHonorific.trimmingCharacters(in: .whitespaces)

        Task { @MainActor in
            isSaving = true
            isValidatingPhotos = true

            // Validate all entries upfront — fail fast on the first mismatch
            for entry in capturedEntries {
                let err = await ImageScopeValidator.shared.validate(
                    subject: entry.subject.name,
                    grade: grade,
                    topic: entry.topic,
                    artefactLabel: artefactLabel,
                    images: entry.images
                )
                if let err {
                    isValidatingPhotos = false
                    isSaving = false
                    saveError = "\(entry.subject.name): \(err)"
                    return
                }
            }
            isValidatingPhotos = false

            // Save each subject entry separately (one Firestore doc per entry)
            var anyFailed = false
            for entry in capturedEntries {
                let ok = await FirebaseService.shared.uploadConceptWork(
                    childId: childId,
                    conceptId: "",
                    kind: kind,
                    unitId: "",
                    unitName: "",
                    conceptName: "",
                    subject: entry.subject.name,
                    subjectColor: entry.subject.color,
                    subjectIcon: entry.subject.icon,
                    topic: entry.topic,
                    dateMillis: dateMillis,
                    images: entry.images
                )
                if !ok { anyFailed = true }
            }

            isSaving = false
            if !anyFailed {
                UserDefaults.standard.set(savedTutorName, forKey: "tutor_name")
                UserDefaults.standard.set(savedTutorHonorific, forKey: "tutor_honorific")
                onSaved()
            } else {
                saveError = "Some uploads failed. Please try again."
            }
        }
    }
}

// MARK: - Tutor strip  (mirrors Android TutorEditor: saved chip + editing form with Sir/Madam chips)

private let tutorHonorifics = ["Sir", "Madam"]

private struct TLTutorStrip: View {
    @Binding var honorific: String
    @Binding var name: String

    @State private var editing: Bool = false
    @State private var draftHonorific: String = "Sir"
    @State private var draftName: String = ""

    private let accent = Color(hex: 0x7C3AED)
    private let containerBg = Color(hex: 0xEDE9FE)

    var body: some View {
        Group {
            if editing {
                editingBody
            } else {
                savedBody
            }
        }
        .onAppear {
            editing = name.isEmpty
            draftHonorific = honorific.isEmpty ? "Sir" : honorific
            draftName = name
        }
    }

    // Saved state — friendly chip with pencil icon
    private var savedBody: some View {
        Button(action: { withAnimation(.spring(duration: 0.2)) { editing = true } }) {
            HStack(spacing: 10) {
                Text("👨‍🏫").font(.system(size: 22))
                VStack(alignment: .leading, spacing: 2) {
                    Text("Your tutor")
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundStyle(accent)
                    Text(name.isEmpty ? "Add tutor's name" : "\(honorific) \(name)".trimmingCharacters(in: .whitespaces))
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(name.isEmpty ? Color.dobiTextSecondary : Color.dobiTextPrimary)
                }
                Spacer()
                Image(systemName: "pencil")
                    .font(.system(size: 16))
                    .foregroundStyle(accent)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .frame(maxWidth: .infinity)
            .background(containerBg)
            .clipShape(RoundedRectangle(cornerRadius: 14))
        }
        .buttonStyle(.plain)
    }

    // Editing state — honorific chips + name field + save/cancel
    private var editingBody: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: 10) {
                Text("👨‍🏫").font(.system(size: 22))
                VStack(alignment: .leading, spacing: 2) {
                    Text(name.isEmpty ? "Who's your tutor?" : "Update your tutor")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundStyle(Color.dobiTextPrimary)
                    Text("We'll remember this for you")
                        .font(.system(size: 11))
                        .foregroundStyle(Color.dobiTextSecondary)
                }
                Spacer()
            }

            // Honorific chips
            HStack(spacing: 8) {
                ForEach(tutorHonorifics, id: \.self) { h in
                    let selected = draftHonorific == h
                    Button(action: { draftHonorific = h }) {
                        Text(h)
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundStyle(selected ? .white : accent)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 8)
                            .background(selected ? accent : Color.white)
                            .overlay(
                                RoundedRectangle(cornerRadius: 10)
                                    .stroke(accent.opacity(selected ? 1 : 0.3), lineWidth: 1)
                            )
                            .clipShape(RoundedRectangle(cornerRadius: 10))
                    }
                    .buttonStyle(.plain)
                    .animation(.spring(duration: 0.2), value: selected)
                }
            }
            .padding(.top, 12)

            // Name field
            TextField(text: $draftName, prompt: .dobiPlaceholder("Tutor's name (e.g. Sharma)")) {}
                .font(.system(size: 14))
                .foregroundStyle(Color.dobiTextPrimary)
                .autocorrectionDisabled()
                .onChange(of: draftName) { val in if val.count > 40 { draftName = String(val.prefix(40)) } }
                .padding(.horizontal, 14)
                .padding(.vertical, 12)
                .background(Color.white)
                .overlay(
                    RoundedRectangle(cornerRadius: 10)
                        .stroke(accent.opacity(0.3), lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: 10))
                .padding(.top, 10)

            // Buttons
            HStack(spacing: 10) {
                if !name.isEmpty {
                    Button(action: { withAnimation(.spring(duration: 0.2)) { editing = false } }) {
                        Text("Cancel")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundStyle(accent)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 10)
                            .background(Color.white)
                            .overlay(
                                RoundedRectangle(cornerRadius: 10)
                                    .stroke(accent.opacity(0.3), lineWidth: 1)
                            )
                            .clipShape(RoundedRectangle(cornerRadius: 10))
                    }
                    .buttonStyle(.plain)
                }
                Button(action: {
                    guard !draftName.trimmingCharacters(in: .whitespaces).isEmpty else { return }
                    honorific = draftHonorific
                    name = draftName.trimmingCharacters(in: .whitespaces)
                    withAnimation(.spring(duration: 0.2)) { editing = false }
                }) {
                    HStack(spacing: 6) {
                        Image(systemName: "checkmark")
                            .font(.system(size: 12, weight: .bold))
                        Text("Save tutor")
                            .font(.system(size: 13, weight: .semibold))
                    }
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 10)
                    .background(draftName.trimmingCharacters(in: .whitespaces).isEmpty ? accent.opacity(0.4) : accent)
                    .clipShape(RoundedRectangle(cornerRadius: 10))
                }
                .buttonStyle(.plain)
                .disabled(draftName.trimmingCharacters(in: .whitespaces).isEmpty)
            }
            .padding(.top, 10)
        }
        .padding(14)
        .frame(maxWidth: .infinity)
        .background(containerBg)
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }
}

// MARK: - Subject chips row

private struct TLSubjectChipsRow: View {
    let subjects: [TuitionSubjectData]
    let selectedNames: Set<String>
    let onToggle: (TuitionSubjectData) -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 10) {
                ForEach(subjects, id: \.name) { subject in
                    TLSubjectChip(
                        subject: subject,
                        selected: selectedNames.contains(subject.name),
                        onTap: { onToggle(subject) }
                    )
                }
            }
        }
    }
}

private struct TLSubjectChip: View {
    let subject: TuitionSubjectData
    let selected: Bool
    let onTap: () -> Void

    private var color: Color { tuitionColorFromHex(subject.color) }

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 6) {
                Text(subject.icon)
                    .font(.system(size: 16))
                Text(subject.name)
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(selected ? Color.white : Color.dobiTextPrimary)
                if selected {
                    Image(systemName: "checkmark")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundStyle(Color.white)
                }
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 10)
            .background(selected ? color : Color.white)
            .overlay(
                RoundedRectangle(cornerRadius: 14)
                    .stroke(selected ? color : Color(hex: 0xE2E8F0), lineWidth: selected ? 1.5 : 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 14))
        }
        .buttonStyle(.plain)
        .animation(.spring(duration: 0.2), value: selected)
    }
}

// MARK: - Per-subject entry card

private struct SubjectEntryCard: View {
    @Binding var entry: TuitionEntry
    let topicPlaceholder: String
    let photoHint: String
    let onRemove: () -> Void

    @State private var pickerItems: [PhotosPickerItem] = []
    @State private var showCamera = false
    @State private var cameraImage: UIImage? = nil

    private var entryColor: Color { tuitionColorFromHex(entry.subject.color) }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {

            // Header: colored icon box + name + X
            HStack(spacing: 10) {
                ZStack {
                    RoundedRectangle(cornerRadius: 12)
                        .fill(entryColor.opacity(0.15))
                        .frame(width: 38, height: 38)
                    Text(entry.subject.icon)
                        .font(.system(size: 20))
                }
                Text(entry.subject.name)
                    .font(.system(size: 15, weight: .bold))
                    .foregroundStyle(Color.dobiTextPrimary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                Button(action: onRemove) {
                    Image(systemName: "xmark")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(Color.dobiTextSecondary)
                        .frame(width: 28, height: 28)
                        .background(Color(hex: 0xF1F5F9))
                        .clipShape(Circle())
                }
                .buttonStyle(.plain)
            }

            // Topic
            Text("Topic *")
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(Color.dobiTextPrimary)
                .padding(.top, 14)
            TextField(text: $entry.topic, prompt: Text(topicPlaceholder).foregroundColor(Color.dobiTextSecondary)) {}
                .font(.system(size: 14))
                .foregroundStyle(Color.dobiTextPrimary)
                .autocorrectionDisabled()
                .padding(.horizontal, 12)
                .padding(.vertical, 11)
                .background(Color(hex: 0xF8FAFF))
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(entry.topic.isEmpty ? Color(hex: 0xE2E8F0) : entryColor,
                                lineWidth: entry.topic.isEmpty ? 1 : 1.5)
                )
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .padding(.top, 6)
                .onChange(of: entry.topic) { val in
                    if val.count > 80 { entry.topic = String(val.prefix(80)) }
                }

            // Photos
            Text("Photos *")
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(Color.dobiTextPrimary)
                .padding(.top, 16)
            Text(photoHint)
                .font(.system(size: 11))
                .foregroundStyle(Color.dobiTextSecondary)
                .padding(.top, 2)

            if !entry.images.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(entry.images.indices, id: \.self) { i in
                            ZStack(alignment: .topTrailing) {
                                Image(uiImage: entry.images[i])
                                    .resizable()
                                    .scaledToFill()
                                    .frame(width: 72, height: 72)
                                    .clipShape(RoundedRectangle(cornerRadius: 12))
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 12)
                                            .stroke(Color(hex: 0xE2E8F0), lineWidth: 1)
                                    )
                                Button { entry.images.remove(at: i) } label: {
                                    Image(systemName: "xmark")
                                        .font(.system(size: 10, weight: .bold))
                                        .foregroundStyle(Color.white)
                                        .frame(width: 20, height: 20)
                                        .background(Color.black.opacity(0.55))
                                        .clipShape(Circle())
                                }
                                .buttonStyle(.plain)
                                .padding(4)
                            }
                        }
                    }
                }
                .padding(.top, 8)
            }

            HStack(spacing: 10) {
                Button { showCamera = true } label: {
                    TLPhotoSourceButton(icon: "camera.fill", label: "Camera", color: entryColor)
                }
                .buttonStyle(.plain)

                PhotosPicker(selection: $pickerItems, maxSelectionCount: 10, matching: .images) {
                    TLPhotoSourceButton(icon: "photo.on.rectangle", label: "Gallery", color: entryColor)
                        .frame(maxWidth: .infinity)
                }
            }
            .padding(.top, 10)
        }
        .padding(16)
        .background(Color.white)
        .overlay(
            RoundedRectangle(cornerRadius: 18)
                .stroke(Color(hex: 0xE2E8F0), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 18))
        .fullScreenCover(isPresented: $showCamera, onDismiss: {
            if let img = cameraImage { entry.images.append(img); cameraImage = nil }
        }) {
            UWCameraView(image: $cameraImage).ignoresSafeArea()
        }
        .onChange(of: pickerItems) { items in
            Task {
                var loaded: [UIImage] = []
                for item in items {
                    if let data = try? await item.loadTransferable(type: Data.self),
                       let img = UIImage(data: data) { loaded.append(img) }
                }
                await MainActor.run { entry.images.append(contentsOf: loaded); pickerItems = [] }
            }
        }
    }
}

// MARK: - Sub-components

private struct TLFieldLabel: View {
    let text: String
    var body: some View {
        Text(text)
            .font(.system(size: 14, weight: .semibold))
            .foregroundStyle(Color.dobiTextPrimary)
    }
}

private struct TLDateRow: View {
    let displayDate: String
    let onClick: () -> Void
    var body: some View {
        Button(action: onClick) {
            HStack(spacing: 12) {
                Image(systemName: "calendar")
                    .font(.system(size: 18))
                    .foregroundStyle(Color(hex: 0x7C3AED))
                Text(displayDate)
                    .font(.system(size: 14, weight: .medium))
                    .foregroundStyle(Color.dobiTextPrimary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                Text("Change")
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(Color(hex: 0x7C3AED))
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .background(Color.white)
            .overlay(
                RoundedRectangle(cornerRadius: 14)
                    .stroke(Color(hex: 0xE2E8F0), lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 14))
        }
        .buttonStyle(.plain)
    }
}

private struct TLPhotoSourceButton: View {
    let icon: String
    let label: String
    var color: Color = Color(hex: 0x7C3AED)
    var body: some View {
        HStack(spacing: 8) {
            Image(systemName: icon)
                .font(.system(size: 18))
                .foregroundStyle(color)
            Text(label)
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(color)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 12)
        .background(color.opacity(0.10))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}
