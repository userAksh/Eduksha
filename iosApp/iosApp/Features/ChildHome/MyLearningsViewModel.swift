import SwiftUI
import FirebaseFirestore
import FirebaseAuth

@MainActor
final class MyLearningsViewModel: ObservableObject {
    let childId: String

    @Published var todayItems: [TodayLearningEntry] = []
    @Published var revisionMarks: [RevisionMarkItem] = []
    @Published var currentAffairs: [CurrentAffairItem] = []
    @Published var isLoadingNews = false
    @Published var newsError: String? = nil
    @Published var selectedStoryIndex: Int? = nil
    @Published var subjectiveSheet: ADSubjectiveResult? = nil
    @Published var isLoadingSubjective = false

    private let firebase = FirebaseService.shared
    private var marksListener: ListenerRegistration? = nil
    private var quizListeners: [ListenerRegistration] = []
    private var uploadListener: ListenerRegistration? = nil
    private var subjectiveListener: ListenerRegistration? = nil
    private var lastParentTasks: [ParentTaskItem] = []
    private var newsCache: [CurrentAffairItem]? = nil

    init(childId: String) {
        self.childId = childId
    }

    func onAppear(parentTasks: [ParentTaskItem]) {
        lastParentTasks = parentTasks
        startMarksListener()
        startQuizListener()
        startUploadListener()
        startSubjectiveListener()
        Task { await loadTodayItems(parentTasks: parentTasks) }
        if newsCache == nil { Task { await loadDailyNews() } }
    }

    func refreshTodayItems(parentTasks: [ParentTaskItem]) {
        lastParentTasks = parentTasks
        Task { await loadTodayItems(parentTasks: parentTasks) }
    }

    private func startQuizListener() {
        guard quizListeners.isEmpty else { return }
        quizListeners = firebase.listenQuizAttempts(childId: childId) { [weak self] in
            guard let self else { return }
            Task { await self.loadTodayItems(parentTasks: self.lastParentTasks) }
        }
    }

    private func startUploadListener() {
        guard uploadListener == nil else { return }
        uploadListener = firebase.listenUploads(childId: childId) { [weak self] in
            guard let self else { return }
            Task { await self.loadTodayItems(parentTasks: self.lastParentTasks) }
        }
    }

    private func startSubjectiveListener() {
        guard subjectiveListener == nil else { return }
        subjectiveListener = firebase.listenSubjectiveResults(childId: childId) { [weak self] in
            guard let self else { return }
            Task { await self.loadTodayItems(parentTasks: self.lastParentTasks) }
        }
    }

    func onUnmark(_ mark: RevisionMarkItem) {
        firebase.unmarkRevision(childId: childId, scope: mark.scope, targetId: mark.targetId)
    }

    func openStory(at index: Int) {
        guard !currentAffairs.isEmpty else {
            if !isLoadingNews { Task { await loadDailyNews() } }
            return
        }
        selectedStoryIndex = min(max(index, 0), currentAffairs.count - 1)
    }

    func closeStory() { selectedStoryIndex = nil }

    func openSubjectiveResult(_ resultId: String) {
        guard !isLoadingSubjective else { return }
        isLoadingSubjective = true
        Task {
            subjectiveSheet = await firebase.fetchSubjectiveResult(resultId: resultId)
            isLoadingSubjective = false
        }
    }

    func closeSubjectiveSheet() { subjectiveSheet = nil }

    func retryLoadNews() {
        guard !isLoadingNews else { return }
        newsCache = nil
        Task { await loadDailyNews() }
    }

    func stop() {
        marksListener?.remove()
        quizListeners.forEach { $0.remove() }
        quizListeners = []
        uploadListener?.remove()
        uploadListener = nil
        subjectiveListener?.remove()
        subjectiveListener = nil
    }

    private func startMarksListener() {
        guard marksListener == nil else { return }
        Task { @MainActor in
            if Auth.auth().currentUser == nil {
                try? await Auth.auth().signInAnonymously()
            }
            guard self.marksListener == nil else { return }
            self.marksListener = self.firebase.listenRevisionMarks(childId: self.childId) { [weak self] marks in
                self?.revisionMarks = marks
            }
        }
    }

    private func loadTodayItems(parentTasks: [ParentTaskItem]) async {
        let myTasks = parentTasks.filter { $0.childId == childId }
        todayItems = await firebase.fetchTodayItems(childId: childId, parentTasks: myTasks)
    }

    private func loadDailyNews() async {
        isLoadingNews = true
        newsError = nil
        let result = await firebase.fetchDailyNews()
        switch result {
        case .success(let stories):
            newsCache = stories
            currentAffairs = stories
            newsError = nil
        case .failure(let error):
            currentAffairs = []
            newsError = error.localizedDescription
                .replacingOccurrences(of: "The operation couldn\u{2019}t be completed. ", with: "")
        }
        isLoadingNews = false
    }
}
