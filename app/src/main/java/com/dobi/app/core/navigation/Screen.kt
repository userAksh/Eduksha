package com.dobi.app.core.navigation

import kotlinx.serialization.Serializable

@Serializable
enum class ConceptListMode { DEFAULT, PRACTISE_LESSON, TEST_YOURSELF }

@Serializable sealed class Screen {
    @Serializable data object RoleSelection : Screen()
    @Serializable data object PhoneAuth : Screen()
    @Serializable data class OtpVerification(
        val verificationId: String,
        val name: String,
        val phone: String
    ) : Screen()
    @Serializable data object AddChild : Screen()
    @Serializable data object SelectChild : Screen()
    @Serializable data object EnterPairingCode : Screen()
    @Serializable data class EnterChildPin(val childId: String) : Screen()
    @Serializable data object PinRecovery : Screen()
    @Serializable data class ChildHome(val childId: String) : Screen()
    @Serializable data object ParentHome : Screen()
    @Serializable data class ConceptList(
        val mode: ConceptListMode = ConceptListMode.DEFAULT
    ) : Screen()
    @Serializable data class ConceptDetail(val conceptId: String) : Screen()
    @Serializable data class UnitDetail(val unitId: String) : Screen()
    @Serializable data class UploadWork(
        val unitId: String = "",
        val kind: String,
        val conceptId: String = ""
    ) : Screen()
    @Serializable data class AnswerQuestionsSource(
        val unitId: String = "",
        val conceptId: String = ""
    ) : Screen()
    @Serializable data class Quiz(
        val unitId: String = "",
        val conceptId: String = "",
        val viewResult: Boolean = false,
        val source: String = "curated",
        val displayName: String = "",
        val parentTaskId: String = ""
    ) : Screen()
    @Serializable data object HomeworkLog : Screen()
    @Serializable data object ClassTestPrep : Screen()
    @Serializable data object WriteAnswer : Screen()
    @Serializable data class UploadTuitionOptions(val parent: String) : Screen()
    @Serializable data class TuitionLearning(val kindKey: String) : Screen()
    @Serializable data object TuitionProblems : Screen()
    @Serializable data object RevisitSubjects : Screen()
    @Serializable data class RevisitDetail(val groupKey: String) : Screen()
    @Serializable data object NeedRevision : Screen()
    @Serializable data class TestYourselfChooser(
        val unitId: String = "",
        val conceptId: String = ""
    ) : Screen()
    @Serializable data class TestYourselfFormat(
        val source: String,
        val unitId: String = "",
        val conceptId: String = ""
    ) : Screen()
    @Serializable data class SubjectiveResult(val resultId: String) : Screen()
    @Serializable data object EarnRewardPicker : Screen()
    @Serializable data class ParentTaskDetail(val taskId: String) : Screen()
    @Serializable data class DoParentTask(val taskId: String) : Screen()
    @Serializable data object StreakHome : Screen()
    @Serializable data object StreakUploadPicker : Screen()
    @Serializable data class StreakShareOptions(
        val unitId: String = "",
        val conceptId: String = "",
        val displayName: String,
        val subject: String = ""
    ) : Screen()
    @Serializable data object ClanLeaderboard : Screen()
}
