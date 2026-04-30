package com.dobi.app.feature.uploadwork

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import android.util.Log
import com.dobi.app.core.navigation.Screen
import com.dobi.app.data.repository.AiPracticeContextHolder
import com.dobi.app.data.repository.GeneratedQuizCache
import com.dobi.app.data.repository.ImageScopeRequest
import com.dobi.app.data.repository.ImageScopeValidator
import com.dobi.app.data.repository.UploadEntryRepository
import com.dobi.app.domain.model.UploadEntry
import com.dobi.app.domain.repository.UserRepository
import com.dobi.app.domain.usecase.GenerateMcqFromImageUseCase
import com.dobi.app.domain.usecase.GenerateMcqRequest
import com.dobi.app.domain.usecase.GetConceptsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class UploadWorkViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getConceptsUseCase: GetConceptsUseCase,
    private val generateMcqFromImageUseCase: GenerateMcqFromImageUseCase,
    private val generatedQuizCache: GeneratedQuizCache,
    private val aiPracticeContextHolder: AiPracticeContextHolder,
    private val userRepository: UserRepository,
    private val uploadEntryRepository: UploadEntryRepository,
    private val imageScopeValidator: ImageScopeValidator
) : ViewModel() {

    private val route = savedStateHandle.toRoute<Screen.UploadWork>()
    private val kind: WorkUploadKind = WorkUploadKind.fromKey(route.kind)
    private val isAiTestPrep: Boolean = kind == WorkUploadKind.AI_TEST_PREP
    private val isTuition: Boolean = kind.isTuition
    private val isConceptScope: Boolean = !isAiTestPrep && !isTuition && route.conceptId.isNotBlank()
    private val targetId: String = if (isConceptScope) route.conceptId else route.unitId
    private var resolvedUnitId: String = route.unitId

    private val _uiState = MutableStateFlow(UploadWorkUiState(kind = kind))
    val uiState = _uiState.asStateFlow()

    init { loadContext() }

    private fun loadContext() {
        if (isTuition) {
            viewModelScope.launch {
                val honorific = userRepository.tutorHonorific.first()
                val name = userRepository.tutorName.first()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        teacherHonorific = honorific,
                        teacherName = name
                    )
                }
            }
            return
        }
        if (isAiTestPrep) {
            val ctx = aiPracticeContextHolder.peek()
            if (ctx == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Selection lost. Please go back and pick topics again."
                    )
                }
                return
            }
            _uiState.update {
                it.copy(
                    unitName = ctx.displayName,
                    subject = ctx.subject,
                    teacherHonorific = ctx.teacherHonorific,
                    teacherName = ctx.teacherName,
                    dateMillis = ctx.dateMillis,
                    isLoading = false
                )
            }
            return
        }

        viewModelScope.launch {
            userRepository.linkedChildGrade
                .flatMapLatest { grade -> getConceptsUseCase(grade = grade, board = "CBSE") }
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { allConcepts ->
                    val unitName: String
                    val conceptName: String
                    val subject: String
                    val subjectColor: String
                    val subjectIcon: String
                    if (isConceptScope) {
                        val concept = allConcepts.firstOrNull { it.id == targetId }
                        conceptName = concept?.name.orEmpty()
                        unitName = concept?.unit.orEmpty()
                        subject = concept?.subject.orEmpty()
                        subjectColor = concept?.subjectColor.orEmpty()
                        subjectIcon = concept?.subjectIcon.orEmpty()
                        resolvedUnitId = concept?.unitId.orEmpty()
                    } else {
                        val first = allConcepts.firstOrNull { it.unitId == targetId }
                        conceptName = ""
                        unitName = first?.unit.orEmpty()
                        subject = first?.subject.orEmpty()
                        subjectColor = first?.subjectColor.orEmpty()
                        subjectIcon = first?.subjectIcon.orEmpty()
                    }
                    _uiState.update {
                        it.copy(
                            unitId = resolvedUnitId,
                            unitName = unitName,
                            conceptId = if (isConceptScope) targetId else "",
                            conceptName = conceptName,
                            subject = subject,
                            subjectColor = subjectColor,
                            subjectIcon = subjectIcon,
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun onTeacherHonorificChanged(honorific: String) =
        _uiState.update {
            it.copy(teacherHonorific = if (it.teacherHonorific == honorific) "" else honorific)
        }

    fun onTeacherNameChanged(name: String) =
        _uiState.update { it.copy(teacherName = name) }

    fun onDateChanged(millis: Long) = _uiState.update { it.copy(dateMillis = millis) }

    fun onPhotosAdded(uris: List<String>) =
        _uiState.update { it.copy(selectedPhotoUris = it.selectedPhotoUris + uris) }

    fun onPhotoRemoved(uri: String) =
        _uiState.update { it.copy(selectedPhotoUris = it.selectedPhotoUris - uri) }

    private fun friendlyError(raw: String?): String {
        val msg = raw.orEmpty().trim()
        val lower = msg.lowercase()
        return when {
            // Infrastructure / transport — replace with kid-friendly copy.
            "unavailable" in lower || "overloaded" in lower || "high demand" in lower || "503" in lower ->
                "Dobi's brain is a little busy right now. Please wait a minute and try again."
            "resource_exhausted" in lower || "quota" in lower ||
                ("rate" in lower && "limit" in lower) ->
                "We've hit today's quiz limit. Try again in a bit."
            "unable to resolve" in lower || "unable to connect" in lower ||
                "no internet" in lower || "network is unreachable" in lower ||
                ("network" in lower && "error" in lower) ||
                ("timeout" in lower && "google" in lower) ->
                "No internet right now. Check your connection and try again."

            // Local validation thrown by the repository itself.
            "no photos provided" in lower -> "Please add at least one photo first."
            "couldn't read uploaded photos" in lower ->
                "We couldn't open those photos. Please re-add them."
            "empty response from gemini" in lower || "model returned no questions" in lower ->
                "Couldn't generate questions. Please try again."

            // Anything else is Gemini's own refusal text — show it verbatim so the
            // student sees the rich explanation (e.g. "this looks like a purse",
            // "this is a mixed sample paper", "these are blurry — please retake").
            msg.isNotBlank() -> msg

            else -> "Couldn't generate questions. Please try again."
        }
    }

    fun onSave() {
        if (!_uiState.value.canSave) return
        val state = _uiState.value
        when {
            kind == WorkUploadKind.SELF_LEARNING -> {
                // Generate quiz AND persist photos so they show on Revisit.
                viewModelScope.launch {
                    persistUpload(state)
                    generateQuiz(state)
                }
            }
            kind == WorkUploadKind.AI_TEST_PREP -> generateQuiz(state)
            kind == WorkUploadKind.CLASS_WORK -> {
                viewModelScope.launch {
                    _uiState.update {
                        it.copy(isSaving = true, isValidatingPhotos = true, error = null)
                    }
                    val grade = userRepository.linkedChildGrade.first()
                    val topic = state.conceptName.ifBlank { state.unitName }
                    val validation = imageScopeValidator.validate(
                        ImageScopeRequest(
                            subject = state.subject,
                            grade = grade,
                            topic = topic,
                            lessonContext = state.unitName,
                            artefactLabel = "class work",
                            photoUris = state.selectedPhotoUris
                        )
                    )
                    if (validation.isFailure) {
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                isValidatingPhotos = false,
                                error = friendlyError(validation.exceptionOrNull()?.message)
                            )
                        }
                        return@launch
                    }
                    _uiState.update { it.copy(isValidatingPhotos = false) }
                    val result = uploadEntryRepository.save(
                        entryFor(state),
                        state.selectedPhotoUris
                    )
                    result.fold(
                        onSuccess = {
                            _uiState.update { it.copy(isSaving = false, isSaved = true) }
                        },
                        onFailure = { e ->
                            Log.w("UploadWork", "Persist failed", e)
                            _uiState.update {
                                it.copy(
                                    isSaving = false,
                                    error = "Couldn't save: ${e.message ?: e::class.simpleName}"
                                )
                            }
                        }
                    )
                }
            }
            else -> {
                // Tuition kinds reach this code path only if a parent navigated
                // directly to UploadWork (legacy paths). The richer Tuition
                // flow handles persistence on its own.
                _uiState.update { it.copy(isSaving = true, error = null, isSaved = true) }
            }
        }
    }

    /** Stores the uploaded photos + metadata so they appear on Revisit Past Topics. */
    private suspend fun persistUpload(state: UploadWorkUiState): Boolean {
        if (state.selectedPhotoUris.isEmpty()) return true
        val result = uploadEntryRepository.save(entryFor(state), state.selectedPhotoUris)
        result.onFailure { Log.w("UploadWork", "Persist failed", it) }
        return result.isSuccess
    }

    private fun entryFor(state: UploadWorkUiState): UploadEntry = UploadEntry(
        kind = kind.key,
        subject = state.subject,
        subjectColor = state.subjectColor,
        subjectIcon = state.subjectIcon,
        unitId = state.unitId,
        unitName = state.unitName,
        conceptId = state.conceptId,
        conceptName = state.conceptName
    )

    private fun generateQuiz(state: UploadWorkUiState) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, isGeneratingQuiz = true, error = null) }
            val testPrepCtx = if (isAiTestPrep) aiPracticeContextHolder.peek() else null
            val cacheKey = testPrepCtx?.selectionKey ?: targetId

            val request = if (testPrepCtx != null) {
                GenerateMcqRequest(
                    unitId = "",
                    unitName = testPrepCtx.displayName,
                    subject = testPrepCtx.subject,
                    grade = testPrepCtx.grade,
                    photoUris = state.selectedPhotoUris,
                    customScopeText = testPrepCtx.scopeText,
                    scopeDisplayName = testPrepCtx.displayName
                )
            } else {
                GenerateMcqRequest(
                    unitId = resolvedUnitId,
                    unitName = state.unitName,
                    conceptId = if (isConceptScope) targetId else "",
                    conceptName = state.conceptName,
                    subject = state.subject,
                    grade = userRepository.linkedChildGrade.first(),
                    photoUris = state.selectedPhotoUris
                )
            }
            val result = generateMcqFromImageUseCase(request)
            result.fold(
                onSuccess = { mcqs ->
                    generatedQuizCache.put(cacheKey, mcqs)
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            isGeneratingQuiz = false,
                            generatedQuiz = mcqs,
                            generatedQuizCacheKey = cacheKey,
                            generatedQuizDisplayName = testPrepCtx?.displayName.orEmpty(),
                            isSaved = true
                        )
                    }
                },
                onFailure = { e ->
                    Log.e("McqGen", "Generation failed", e)
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            isGeneratingQuiz = false,
                            error = friendlyError(e.message)
                        )
                    }
                }
            )
        }
    }
}
