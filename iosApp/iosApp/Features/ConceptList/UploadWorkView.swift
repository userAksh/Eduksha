import SwiftUI
import PhotosUI
import FirebaseFunctions

struct UploadWorkView: View {
    let kind: String        // "class_work" | "self_learning" | "ai_test_prep"
    let conceptId: String
    let childId: String
    let unitName: String
    let conceptName: String
    let subject: String
    let subjectColor: String
    let subjectIcon: String
    let unitId: String
    let grade: Int
    var scopeText: String = ""
    var initialTeacherHonorific: String = ""
    var initialTeacherName: String = ""
    var initialDate: Date = Date()
    var onBack: () -> Void = {}
    var onSaveComplete: () -> Void = {}
    var onQuizReady: (String, String) -> Void = { _, _ in }

    @State private var teacherHonorific = ""
    @State private var teacherName = ""
    @State private var selectedDate = Date()
    @State private var showDatePicker = false
    @State private var pickerItems: [PhotosPickerItem] = []
    @State private var images: [UIImage] = []
    @State private var showCamera = false
    @State private var cameraImage: UIImage? = nil
    @State private var isSaving = false
    @State private var isValidatingPhotos = false
    @State private var isGeneratingQuiz = false
    @State private var uploadError: String? = nil

    private var isAiTestPrep: Bool { kind == "ai_test_prep" }

    private var kindTitle: String {
        switch kind {
        case "class_work": return "Upload Class Work"
        case "ai_test_prep": return "AI Practice"
        default: return "Upload Self Learning"
        }
    }

    private var ctaLabel: String {
        switch kind {
        case "class_work": return "Save Class Work"
        case "ai_test_prep": return "Generate Quiz"
        default: return "Save Self Learning"
        }
    }

    private var photoHint: String {
        switch kind {
        case "class_work": return "Add photos of the work you did in class"
        case "ai_test_prep": return "Snap pages from the lessons or concepts you picked"
        default: return "Add photos of the notes you made on your own"
        }
    }

    // Android canSave: teacherName.isNotBlank() && photos.isNotEmpty() && !isSaving
    private var canSave: Bool { !teacherName.isEmpty && !images.isEmpty && !isSaving && !isValidatingPhotos && !isGeneratingQuiz }

    private var formattedDate: String {
        let fmt = DateFormatter()
        fmt.dateFormat = "d MMM yyyy"
        return fmt.string(from: selectedDate)
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
                Text(kindTitle)
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
                    UWLessonCard(unitName: unitName, subject: subject)

                    Text("Teacher's Name")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(Color.dobiTextPrimary)
                        .padding(.top, 20)

                    HStack(spacing: 8) {
                        ForEach(["Sir", "Madam"], id: \.self) { h in
                            UWHonorificChip(label: h, selected: teacherHonorific == h) {
                                teacherHonorific = h
                            }
                        }
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
                                .stroke(Color(hex: 0xE2E8F0), lineWidth: 1)
                        )
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                        .onChange(of: teacherName) { val in
                            teacherName = String(val.filter { $0.isLetter || $0.isWhitespace || $0 == "." })
                        }
                        .padding(.top, 10)

                    Text("Date")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(Color.dobiTextPrimary)
                        .padding(.top, 24)

                    UWDateRow(displayDate: formattedDate) { showDatePicker = true }
                        .padding(.top, 10)

                    Text("Photos")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(Color.dobiTextPrimary)
                        .padding(.top, 24)

                    Text(photoHint)
                        .font(.system(size: 12))
                        .foregroundStyle(Color.dobiTextSecondary)
                        .padding(.top, 4)

                    if !images.isEmpty {
                        UWPhotoGrid(images: images) { idx in images.remove(at: idx); uploadError = nil }
                            .padding(.top, 12)
                    }

                    HStack(spacing: 12) {
                        Button { showCamera = true } label: {
                            UWPhotoSourceButton(icon: "camera.fill", label: "Camera")
                        }
                        .buttonStyle(.plain)

                        PhotosPicker(selection: $pickerItems, maxSelectionCount: 10, matching: .images) {
                            UWPhotoSourceButton(icon: "photo.on.rectangle", label: "Gallery")
                                .frame(maxWidth: .infinity)
                        }
                    }
                    .padding(.top, 12)

                    if let err = uploadError {
                        Text(err)
                            .font(.system(size: 12))
                            .foregroundStyle(Color(hex: 0xEF4444))
                            .padding(.top, 8)
                    }

                    Button(action: handleSave) {
                        ZStack {
                            if isGeneratingQuiz {
                                HStack(spacing: 8) {
                                    ProgressView().scaleEffect(0.9).tint(Color.white)
                                    Text("Reading your pages…")
                                        .font(.system(size: 15, weight: .semibold))
                                        .foregroundStyle(Color.white)
                                }
                            } else if isValidatingPhotos {
                                HStack(spacing: 8) {
                                    ProgressView().scaleEffect(0.9).tint(Color.white)
                                    Text("Checking photos…")
                                        .font(.system(size: 15, weight: .semibold))
                                        .foregroundStyle(Color.white)
                                }
                            } else if isSaving {
                                ProgressView().scaleEffect(0.9).tint(Color.white)
                            } else {
                                Text(ctaLabel)
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
        .task {
            if isAiTestPrep {
                if teacherHonorific.isEmpty { teacherHonorific = initialTeacherHonorific }
                if teacherName.isEmpty { teacherName = initialTeacherName }
                selectedDate = initialDate
            }
        }
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
            UWCameraView(image: $cameraImage)
                .ignoresSafeArea()
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
                await MainActor.run {
                    images.append(contentsOf: loaded)
                    pickerItems = []
                }
            }
        }
    }

    private func addCameraImage() {
        if let img = cameraImage { images.append(img); cameraImage = nil }
    }

    private func handleSave() {
        guard canSave else { return }
        uploadError = nil
        if isAiTestPrep {
            handleAiTestPrepSave()
        } else {
            handleRegularSave()
        }
    }

    private func handleAiTestPrepSave() {
        Task { @MainActor in
            isSaving = true
            isGeneratingQuiz = true
            uploadError = nil

            // Downsample to 1280px (matches Android MAX_IMAGE_SIDE), jpeg 0.75, base64.
            let b64Images: [String] = images.compactMap { img in
                let size = img.size
                let scale = min(1280.0 / max(size.width, size.height), 1.0)
                let newSize = scale < 1.0
                    ? CGSize(width: size.width * scale, height: size.height * scale)
                    : size
                let downsampled = UIGraphicsImageRenderer(size: newSize)
                    .image { _ in img.draw(in: CGRect(origin: .zero, size: newSize)) }
                return downsampled.jpegData(compressionQuality: 0.75)?.base64EncodedString()
            }

            let cacheKey = "ai_prep_\(conceptId)_\(Int(Date().timeIntervalSince1970))"
            let effectiveScopeText = scopeText.isEmpty
                ? (conceptName.isEmpty ? "Lesson: \(unitName)" : "Lesson: \(unitName)\nConcept (focus narrowly on this): \(conceptName)")
                : scopeText

            do {
                let fn = Functions.functions(region: "us-central1").httpsCallable("generateMcqFromImage")
                let payload: [String: Any] = [
                    "scopeText": effectiveScopeText,
                    "subject": subject,
                    "grade": grade,
                    "images": b64Images,
                    "targetCount": 8,
                    "bloomMix": ["recall": 2, "understand": 2, "apply": 2, "analyze": 2]
                ]
                let result = try await fn.call(payload)
                guard let dict = result.data as? [String: Any],
                      let rawQs = dict["questions"] as? [[String: Any]] else {
                    throw NSError(domain: "AiPractice", code: -1,
                                  userInfo: [NSLocalizedDescriptionKey: "Invalid response from server."])
                }
                let qs: [McqQuestion] = rawQs.compactMap { m in
                    guard let text = m["text"] as? String,
                          let options = m["options"] as? [String], options.count == 4 else { return nil }
                    let ci = m["correct_index"] as? Int ?? 0
                    return McqQuestion(
                        id: UUID().uuidString,
                        text: text,
                        options: options,
                        correctIndex: ci,
                        explanation: m["explanation"] as? String ?? ""
                    )
                }
                guard !qs.isEmpty else {
                    throw NSError(domain: "AiPractice", code: -2,
                                  userInfo: [NSLocalizedDescriptionKey: "No valid questions generated."])
                }
                GeneratedQuizCache.shared.put(cacheKey, qs)
                isSaving = false
                isGeneratingQuiz = false
                onQuizReady(cacheKey, unitName)
            } catch let err as NSError {
                isSaving = false
                isGeneratingQuiz = false
                // Show the model's kid-friendly refusal (e.g. "This looks like a mixed paper…")
                // or a generic message for network/server errors.
                let msg = err.localizedDescription
                let lower = msg.lowercased()
                if lower.contains("unavailable") || lower.contains("internal") || lower.contains("503") {
                    uploadError = "Dobi's brain is a little busy right now. Please try again in a minute."
                } else if lower.contains("internet") || lower.contains("network") || lower.contains("offline") {
                    uploadError = "No internet right now. Check your connection and try again."
                } else {
                    uploadError = msg
                }
            }
        }
    }

    private func handleRegularSave() {
        Task { @MainActor in
            // Validate photos with Gemini before uploading (both class_work and self_learning)
            isValidatingPhotos = true
            isSaving = true
            let artefactLabel = kind == "class_work" ? "class work" : "self learning"
            let error = await ImageScopeValidator.shared.validate(
                subject: subject,
                grade: grade,
                topic: unitName,
                lessonContext: unitName,
                artefactLabel: artefactLabel,
                images: images
            )
            isValidatingPhotos = false
            if let error {
                isSaving = false
                uploadError = error
                return
            }

            let ok = await FirebaseService.shared.uploadConceptWork(
                childId: childId,
                conceptId: conceptId,
                kind: kind,
                unitId: unitId,
                unitName: unitName,
                conceptName: conceptName,
                subject: subject,
                subjectColor: subjectColor,
                subjectIcon: subjectIcon,
                images: images
            )
            isSaving = false
            if ok { onSaveComplete() }
            else { uploadError = "Upload failed. Please try again." }
        }
    }
}

// MARK: - Lesson card

private struct UWLessonCard: View {
    let unitName: String
    let subject: String

    var body: some View {
        HStack(spacing: 12) {
            Text("📘").font(.system(size: 22))
            VStack(alignment: .leading, spacing: 2) {
                Text("Lesson")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(Color.dobiPrimary)
                Text(unitName.isEmpty ? "Loading…" : unitName)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(Color.dobiTextPrimary)
                if !subject.isEmpty {
                    Text(subject)
                        .font(.system(size: 12))
                        .foregroundStyle(Color.dobiTextSecondary)
                }
            }
            Spacer()
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .background(Color.dobiPrimaryContainer)
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }
}

// MARK: - Honorific chip

private struct UWHonorificChip: View {
    let label: String
    let selected: Bool
    let onClick: () -> Void

    var body: some View {
        Button(action: onClick) {
            HStack(spacing: 6) {
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
            .overlay(Capsule().stroke(selected ? Color.dobiPrimary : Color(hex: 0xE2E8F0), lineWidth: 1.5))
            .clipShape(Capsule())
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Date row

private struct UWDateRow: View {
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

// MARK: - Photo source button

private struct UWPhotoSourceButton: View {
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

// MARK: - Photo grid

private struct UWPhotoGrid: View {
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
                        .overlay(
                            RoundedRectangle(cornerRadius: 12)
                                .stroke(Color(hex: 0xE2E8F0), lineWidth: 1)
                        )
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

// MARK: - Camera picker

struct UWCameraView: UIViewControllerRepresentable {
    @Binding var image: UIImage?
    @Environment(\.dismiss) private var dismiss

    func makeUIViewController(context: Context) -> UIImagePickerController {
        let picker = UIImagePickerController()
        picker.sourceType = .camera
        picker.delegate = context.coordinator
        return picker
    }

    func updateUIViewController(_ vc: UIImagePickerController, context: Context) {}

    func makeCoordinator() -> Coordinator { Coordinator(self) }

    final class Coordinator: NSObject, UIImagePickerControllerDelegate, UINavigationControllerDelegate {
        let parent: UWCameraView
        init(_ parent: UWCameraView) { self.parent = parent }

        func imagePickerController(_ picker: UIImagePickerController,
                                   didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]) {
            parent.image = info[.originalImage] as? UIImage
            parent.dismiss()
        }

        func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
            parent.dismiss()
        }
    }
}
