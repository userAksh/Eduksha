import SwiftUI
import PhotosUI

private let subjectPresets = ["Science", "Mathematics", "English", "Social Science", "Hindi", "Sanskrit"]

struct HomeworkLogView: View {
    let childId: String
    var onBack: () -> Void = {}
    var onSaved: () -> Void = {}

    @State private var subject = ""
    @State private var teacherHonorific = ""
    @State private var teacherName = ""
    @State private var chapterName = ""
    @State private var selectedDate = Date()
    @State private var showDatePicker = false
    @State private var pickerItems: [PhotosPickerItem] = []
    @State private var images: [UIImage] = []
    @State private var showCamera = false
    @State private var cameraImage: UIImage? = nil
    @State private var isSaving = false
    @State private var isValidatingPhotos = false
    @State private var saveError: String? = nil

    // Android canSave: subject.isNotBlank() && teacherName.isNotBlank() && photos.isNotEmpty() && !isSaving
    private var canSave: Bool {
        !subject.trimmingCharacters(in: .whitespaces).isEmpty &&
        !teacherName.trimmingCharacters(in: .whitespaces).isEmpty &&
        !images.isEmpty &&
        !isSaving && !isValidatingPhotos
    }

    private var formattedDate: String {
        let fmt = DateFormatter()
        fmt.dateFormat = "dd MMMM yyyy"  // matches Android "dd MMMM yyyy"
        return fmt.string(from: selectedDate)
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
                Text("Log Homework")
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

                    // ── Subject ────────────────────────────────────────────────
                    HLFieldLabel(text: "Subject")
                        .padding(.top, 20)

                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(subjectPresets, id: \.self) { preset in
                                HLSubjectChip(
                                    label: preset,
                                    selected: subject == preset,
                                    onTap: { subject = (subject == preset) ? "" : preset }
                                )
                            }
                        }
                    }
                    .padding(.top, 10)

                    TextField(text: $subject, prompt: .dobiPlaceholder("Or type a custom subject…")) {}
                        .font(.system(size: 15))
                        .foregroundStyle(Color.dobiTextPrimary)
                        .autocorrectionDisabled()
                        .padding(.horizontal, 14)
                        .padding(.vertical, 13)
                        .background(Color.white)
                        .overlay(
                            RoundedRectangle(cornerRadius: 14)
                                .stroke(subject.isEmpty ? Color(hex: 0xE2E8F0) : Color.dobiPrimary, lineWidth: subject.isEmpty ? 1 : 1.5)
                        )
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                        .padding(.top, 10)

                    // ── Teacher's Name ─────────────────────────────────────────
                    HLFieldLabel(text: "Teacher's Name")
                        .padding(.top, 24)

                    HStack(spacing: 8) {
                        ForEach(["Sir", "Madam"], id: \.self) { h in hlHonorificChip(h) }
                    }
                    .padding(.top, 10)

                    TextField(text: $teacherName, prompt: .dobiPlaceholder("e.g. Sharma")) {}
                        .font(.system(size: 15))
                        .foregroundStyle(Color.dobiTextPrimary)
                        .autocorrectionDisabled()
                        .padding(.horizontal, 14)
                        .padding(.vertical, 13)
                        .background(Color.white)
                        .overlay(
                            RoundedRectangle(cornerRadius: 14)
                                .stroke(teacherName.isEmpty ? Color(hex: 0xE2E8F0) : Color.dobiPrimary, lineWidth: teacherName.isEmpty ? 1 : 1.5)
                        )
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                        .onChange(of: teacherName) { val in
                            teacherName = String(val.filter { $0.isLetter || $0.isWhitespace || $0 == "." })
                        }
                        .padding(.top, 10)

                    // ── Chapter / Topic Name ───────────────────────────────────
                    HLFieldLabel(text: "Chapter / Topic Name")
                        .padding(.top, 24)

                    TextField(text: $chapterName, prompt: .dobiPlaceholder("e.g. Conductors and Insulators")) {}
                        .font(.system(size: 15))
                        .foregroundStyle(Color.dobiTextPrimary)
                        .autocorrectionDisabled()
                        .padding(.horizontal, 14)
                        .padding(.vertical, 13)
                        .background(Color.white)
                        .overlay(
                            RoundedRectangle(cornerRadius: 14)
                                .stroke(Color(hex: 0xE2E8F0), lineWidth: 1)
                        )
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                        .padding(.top, 10)

                    // ── Date ──────────────────────────────────────────────────
                    HLFieldLabel(text: "Date")
                        .padding(.top, 24)

                    HLDateRow(displayDate: formattedDate) { showDatePicker = true }
                        .padding(.top, 10)

                    // ── Photos ────────────────────────────────────────────────
                    HLFieldLabel(text: "Photos")
                        .padding(.top, 24)

                    if !images.isEmpty {
                        HLPhotoGrid(images: images) { idx in images.remove(at: idx); saveError = nil }
                            .padding(.top, 12)
                    }

                    HStack(spacing: 12) {
                        Button { showCamera = true } label: {
                            HLPhotoSourceButton(icon: "camera.fill", label: "Camera")
                        }
                        .buttonStyle(.plain)

                        PhotosPicker(selection: $pickerItems, maxSelectionCount: 10, matching: .images) {
                            HLPhotoSourceButton(icon: "photo.on.rectangle", label: "Gallery")
                                .frame(maxWidth: .infinity)
                        }
                    }
                    .padding(.top, 12)

                    // ── Error ─────────────────────────────────────────────────
                    if let err = saveError {
                        Text(err)
                            .font(.system(size: 12))
                            .foregroundStyle(Color(hex: 0xEF4444))
                            .padding(.top, 8)
                    }

                    // ── Save button ───────────────────────────────────────────
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
                                Text("Save Homework")
                                    .font(.system(size: 15, weight: .semibold))
                                    .foregroundStyle(Color.white)
                            }
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                        .background(canSave ? Color.dobiPrimary : Color(hex: 0xBFD7F7))
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
                    .tint(Color.dobiPrimary)
                    .padding()
                Button("Done") { showDatePicker = false }
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(Color.dobiPrimary)
                    .padding(.bottom, 20)
            }
            .presentationDetents([.medium])
        }
        .fullScreenCover(isPresented: $showCamera, onDismiss: addCameraImage) {
            UWCameraView(image: $cameraImage).ignoresSafeArea()
        }
        .onChange(of: pickerItems) { items in
            Task {
                var loaded: [UIImage] = []
                for item in items {
                    if let data = try? await item.loadTransferable(type: Data.self),
                       let img = UIImage(data: data) {
                        loaded.append(img)
                    }
                }
                await MainActor.run { images.append(contentsOf: loaded); pickerItems = [] }
            }
        }
    }

    private func addCameraImage() {
        if let img = cameraImage { images.append(img); cameraImage = nil }
    }

    @ViewBuilder
    private func hlHonorificChip(_ label: String) -> some View {
        let selected = teacherHonorific == label
        HStack(spacing: 6) {
            if selected {
                Image(systemName: "checkmark")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(Color.white)
            }
            Text(label)
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(selected ? Color.white : Color.dobiTextSecondary)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 8)
        .background(selected ? Color.dobiPrimary : Color.white)
        .clipShape(Capsule())
        .overlay(Capsule().stroke(selected ? Color.dobiPrimary : Color(hex: 0xE2E8F0), lineWidth: 1.5))
        .onTapGesture { teacherHonorific = selected ? "" : label }
        .animation(.easeInOut(duration: 0.15), value: selected)
    }

    private func handleSave() {
        guard canSave else { return }
        saveError = nil
        Task { @MainActor in
            // AI scope validation before saving (mirrors Android HomeworkLogViewModel exactly)
            isValidatingPhotos = true
            isSaving = true
            let topic = chapterName.isEmpty ? subject : chapterName
            let validationError = await ImageScopeValidator.shared.validate(
                subject: subject,
                grade: 6,   // grade not available in homework flow; defaults to 6 (same as Android uses linked child grade)
                topic: topic,
                lessonContext: chapterName.isEmpty ? "" : chapterName,
                artefactLabel: "homework",
                images: images
            )
            isValidatingPhotos = false
            if let err = validationError {
                isSaving = false
                saveError = err
                return
            }

            let dateMillis = Int64(selectedDate.timeIntervalSince1970 * 1000)
            let fullTeacherName = teacherHonorific.isEmpty ? teacherName : "\(teacherHonorific) \(teacherName)"
            let error = await FirebaseService.shared.saveHomework(
                childId: childId,
                subject: subject,
                teacherName: fullTeacherName,
                chapterName: chapterName,
                dateMillis: dateMillis,
                images: images
            )
            isSaving = false
            if let error {
                saveError = error
            } else {
                onSaved()
            }
        }
    }
}

// MARK: - Sub-components

private struct HLFieldLabel: View {
    let text: String
    var body: some View {
        Text(text)
            .font(.system(size: 14, weight: .semibold))
            .foregroundStyle(Color.dobiTextPrimary)
    }
}

private struct HLSubjectChip: View {
    let label: String
    let selected: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 4) {
                if selected {
                    Image(systemName: "checkmark")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundStyle(Color.white)
                }
                Text(label)
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(selected ? Color.white : Color.dobiTextSecondary)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 8)
            .background(selected ? Color.dobiPrimary : Color.white)
            .overlay(
                RoundedRectangle(cornerRadius: 10)
                    .stroke(selected ? Color.dobiPrimary : Color(hex: 0xE2E8F0), lineWidth: selected ? 1.5 : 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 10))
        }
        .buttonStyle(.plain)
        .animation(.spring(duration: 0.2), value: selected)
    }
}

private struct HLDateRow: View {
    let displayDate: String
    let onClick: () -> Void

    var body: some View {
        Button(action: onClick) {
            HStack(spacing: 12) {
                Image(systemName: "calendar")
                    .font(.system(size: 18))
                    .foregroundStyle(Color.dobiPrimary)
                Text(displayDate)
                    .font(.system(size: 14, weight: .medium))
                    .foregroundStyle(Color.dobiTextPrimary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                Text("Change")
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(Color.dobiPrimary)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 14))
            .shadow(color: .black.opacity(0.05), radius: 3, x: 0, y: 1)
        }
        .buttonStyle(.plain)
    }
}

private struct HLPhotoSourceButton: View {
    let icon: String
    let label: String

    var body: some View {
        HStack(spacing: 8) {
            Image(systemName: icon)
                .font(.system(size: 18))
                .foregroundStyle(Color.dobiPrimary)
            Text(label)
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(Color.dobiPrimary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 14)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 14))
        .shadow(color: .black.opacity(0.05), radius: 3, x: 0, y: 1)
    }
}

private struct HLPhotoGrid: View {
    let images: [UIImage]
    let onRemove: (Int) -> Void

    private let columns = [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())]

    var body: some View {
        LazyVGrid(columns: columns, spacing: 8) {
            ForEach(images.indices, id: \.self) { i in
                ZStack(alignment: .topTrailing) {
                    Image(uiImage: images[i])
                        .resizable()
                        .scaledToFill()
                        .frame(width: 96, height: 96)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                        .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color(hex: 0xE2E8F0), lineWidth: 1))
                    Button { onRemove(i) } label: {
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
}
