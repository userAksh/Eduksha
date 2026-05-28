import SwiftUI

private let subjectPresets = ["Science", "Mathematics", "English", "Social Science", "Hindi", "Sanskrit"]

struct HomeworkLogView: View {
    var onBack: () -> Void = {}
    var onSaved: () -> Void = {}

    @State private var selectedSubject = ""
    @State private var customSubject = ""
    @State private var notes = ""
    @State private var selectedDate = Date()
    @State private var showDatePicker = false
    @State private var isSaving = false
    @State private var showSuccess = false

    private var effectiveSubject: String {
        selectedSubject == "Other" ? customSubject : selectedSubject
    }

    private var canSave: Bool {
        !effectiveSubject.trimmingCharacters(in: .whitespaces).isEmpty
    }

    var body: some View {
        ZStack(alignment: .top) {
            Color.dobiPageBg.ignoresSafeArea()

            ScrollView {
                VStack(spacing: 20) {
                    Spacer().frame(height: 72)

                    // Subject picker
                    DobiCard(cornerRadius: 16) {
                        VStack(alignment: .leading, spacing: 12) {
                            Label("Subject", systemImage: "book.fill")
                                .font(.dobiCaptionBold())
                                .foregroundStyle(Color.dobiTextSecondary)

                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(spacing: 8) {
                                    ForEach(subjectPresets + ["Other"], id: \.self) { sub in
                                        SubjectChip(
                                            label: sub,
                                            isSelected: selectedSubject == sub,
                                            onTap: { selectedSubject = sub }
                                        )
                                    }
                                }
                            }

                            if selectedSubject == "Other" {
                                TextField("Enter subject name", text: $customSubject)
                                    .font(.dobiBody())
                                    .foregroundStyle(Color.dobiTextPrimary)
                                    .padding(.horizontal, 12)
                                    .padding(.vertical, 10)
                                    .background(Color.dobiSurfaceAlt)
                                    .clipShape(RoundedRectangle(cornerRadius: 10))
                                    .overlay(RoundedRectangle(cornerRadius: 10).strokeBorder(Color.dobiPrimary, lineWidth: 1.5))
                            }
                        }
                        .padding(16)
                    }
                    .padding(.horizontal, 20)

                    // Date
                    DobiCard(cornerRadius: 16) {
                        Button { showDatePicker.toggle() } label: {
                            HStack {
                                Image(systemName: "calendar")
                                    .font(.system(size: 15))
                                    .foregroundStyle(Color.dobiPrimary)
                                VStack(alignment: .leading, spacing: 2) {
                                    Text("Date")
                                        .font(.dobiSmall())
                                        .foregroundStyle(Color.dobiTextTertiary)
                                    Text(selectedDate.formatted(date: .long, time: .omitted))
                                        .font(.dobiBodyMedium())
                                        .foregroundStyle(Color.dobiTextPrimary)
                                }
                                Spacer()
                                Image(systemName: "chevron.down")
                                    .font(.system(size: 13))
                                    .foregroundStyle(Color.dobiTextTertiary)
                            }
                        }
                        .buttonStyle(.plain)
                        .padding(16)

                        if showDatePicker {
                            DatePicker("", selection: $selectedDate, displayedComponents: .date)
                                .datePickerStyle(.graphical)
                                .tint(Color.dobiPrimary)
                                .padding(.horizontal, 8)
                                .padding(.bottom, 8)
                        }
                    }
                    .padding(.horizontal, 20)

                    // Notes
                    DobiCard(cornerRadius: 16) {
                        VStack(alignment: .leading, spacing: 10) {
                            Label("Notes (optional)", systemImage: "note.text")
                                .font(.dobiCaptionBold())
                                .foregroundStyle(Color.dobiTextSecondary)

                            ZStack(alignment: .topLeading) {
                                if notes.isEmpty {
                                    Text("What was assigned today?")
                                        .font(.dobiBody())
                                        .foregroundStyle(Color.dobiTextTertiary)
                                        .padding(.top, 8)
                                        .padding(.leading, 4)
                                }
                                TextEditor(text: $notes)
                                    .font(.dobiBody())
                                    .foregroundStyle(Color.dobiTextPrimary)
                                    .frame(minHeight: 100)
                                    .scrollContentBackground(.hidden)
                            }
                        }
                        .padding(16)
                    }
                    .padding(.horizontal, 20)

                    // Save button
                    DobiPrimaryButton(
                        title: showSuccess ? "Saved!" : "Save Homework",
                        isLoading: isSaving,
                        isEnabled: canSave
                    ) {
                        guard canSave && !isSaving else { return }
                        isSaving = true
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.8) {
                            isSaving = false
                            showSuccess = true
                            DispatchQueue.main.asyncAfter(deadline: .now() + 0.6) { onSaved() }
                        }
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
                Text("Log Homework")
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

private struct SubjectChip: View {
    let label: String
    let isSelected: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            Text(label)
                .font(.dobiCaptionMedium())
                .foregroundStyle(isSelected ? .white : Color.dobiTextSecondary)
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .background(isSelected ? Color.dobiPrimary : Color.white)
                .clipShape(Capsule())
                .overlay(Capsule().strokeBorder(isSelected ? Color.clear : Color.dobiBorder, lineWidth: 1))
        }
        .buttonStyle(.plain)
        .animation(.spring(duration: 0.2), value: isSelected)
    }
}

#Preview {
    HomeworkLogView()
}
