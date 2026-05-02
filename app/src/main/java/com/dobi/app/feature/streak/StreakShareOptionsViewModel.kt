package com.dobi.app.feature.streak

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.dobi.app.core.navigation.Screen
import com.dobi.app.data.repository.GeneratedQuizCache
import com.dobi.app.data.repository.McqGenerationRepository
import com.dobi.app.domain.model.Concept
import com.dobi.app.domain.repository.UserRepository
import com.dobi.app.domain.usecase.GetConceptsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StreakShareOptionsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getConceptsUseCase: GetConceptsUseCase,
    private val mcqGenerationRepository: McqGenerationRepository,
    private val generatedQuizCache: GeneratedQuizCache,
    private val userRepository: UserRepository
) : ViewModel() {

    private val route = savedStateHandle.toRoute<Screen.StreakShareOptions>()
    private val isUnitScope: Boolean = route.conceptId.isBlank()
    private val targetId: String = if (isUnitScope) route.unitId else route.conceptId

    private val _uiState = MutableStateFlow(
        StreakShareOptionsUiState(
            displayName = route.displayName,
            subject = route.subject,
            isUnitScope = isUnitScope,
            unitId = route.unitId,
            conceptId = route.conceptId
        )
    )
    val uiState = _uiState.asStateFlow()

    fun onShareWithoutQuiz() {
        _uiState.update { it.copy(pendingShareText = buildShareText(includeQuizMention = false)) }
    }

    fun onTakeQuizAndShare() {
        if (_uiState.value.isGeneratingQuiz) return
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingQuiz = true, error = null) }
            val grade = userRepository.linkedChildGrade.first()
            val concepts = getConceptsUseCase(grade = grade, board = "CBSE").first()

            val (subject, unitName, conceptName, description) = if (isUnitScope) {
                val unitConcepts = concepts.filter { it.unitId == route.unitId }
                val first = unitConcepts.firstOrNull()
                LessonScope(
                    subject = first?.subject.orEmpty(),
                    unitName = first?.unit.orEmpty(),
                    conceptName = "",
                    description = unitConcepts.joinToString("\n\n") { c ->
                        "• ${c.name}: ${c.description}"
                    }
                )
            } else {
                val concept = concepts.firstOrNull { it.id == route.conceptId }
                LessonScope(
                    subject = concept?.subject.orEmpty(),
                    unitName = concept?.unit.orEmpty(),
                    conceptName = concept?.name.orEmpty(),
                    description = concept?.description.orEmpty()
                )
            }

            val result = mcqGenerationRepository.generateFromLesson(
                subject = subject,
                grade = grade,
                unitName = unitName,
                conceptName = conceptName,
                conceptDescription = description,
                targetCount = 5
            )
            result.fold(
                onSuccess = { questions ->
                    val cacheKey = "streak_quiz_$targetId"
                    generatedQuizCache.put(cacheKey, questions)
                    _uiState.update {
                        it.copy(
                            isGeneratingQuiz = false,
                            launchQuiz = LaunchQuizArgs(
                                cacheKey = cacheKey,
                                displayName = it.displayName
                            )
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isGeneratingQuiz = false,
                            error = friendlyError(e.message)
                        )
                    }
                }
            )
        }
    }

    fun onShareConsumed() {
        _uiState.update { it.copy(pendingShareText = null) }
    }

    fun onSharedWithoutQuizCompleted() {
        _uiState.update { it.copy(finishedShareOnly = true) }
    }

    fun onQuizLaunchConsumed() {
        _uiState.update { it.copy(launchQuiz = null) }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    private data class LessonScope(
        val subject: String,
        val unitName: String,
        val conceptName: String,
        val description: String
    )

    private fun buildShareText(includeQuizMention: Boolean): String {
        val state = _uiState.value
        val what = if (state.isUnitScope) "lesson" else "concept"
        val quizPart = if (includeQuizMention) " 🎯 Taking a quick 5-question quiz too!" else ""
        return "🔥 Today's streak unlocked! Studying \"${state.displayName}\" " +
            "($what) on Dobi.$quizPart"
    }

    private fun friendlyError(raw: String?): String {
        val msg = raw.orEmpty().trim().lowercase()
        return when {
            "unavailable" in msg || "overloaded" in msg || "503" in msg ->
                "Dobi's brain is a little busy right now. Please try again in a minute."
            "quota" in msg || ("rate" in msg && "limit" in msg) ->
                "We've hit today's quiz limit. Try again later."
            "network" in msg || "timeout" in msg || "unable to" in msg ->
                "No internet right now. Check your connection and try again."
            else -> "Couldn't prepare the quiz. Please try again."
        }
    }
}
