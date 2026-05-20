package com.dobi.app.core.navigation

import android.content.Intent
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.dobi.app.feature.addchild.AddChildScreen
import com.dobi.app.feature.auth.OtpVerificationScreen
import com.dobi.app.feature.auth.PhoneAuthScreen
import com.dobi.app.feature.childhome.ChildHomeScreen
import com.dobi.app.feature.earnreward.EarnRewardPickerScreen
import com.dobi.app.feature.parenttask.DoParentTaskScreen
import com.dobi.app.feature.parenttask.ParentTaskDetailScreen
import com.dobi.app.feature.conceptdetail.ConceptDetailScreen
import com.dobi.app.feature.conceptlist.ConceptListScreen
import com.dobi.app.feature.unitdetail.UnitDetailScreen
import com.dobi.app.feature.unitdetail.UnitDetailViewModel
import com.dobi.app.feature.needrevision.NeedRevisionScreen
import com.dobi.app.feature.revisit.RevisitDetailScreen
import com.dobi.app.feature.revisit.RevisitSubjectsScreen
import com.dobi.app.feature.testyourself.FORMAT_MCQ
import com.dobi.app.feature.testyourself.FORMAT_SUBJECTIVE
import com.dobi.app.feature.testyourself.SOURCE_AI
import com.dobi.app.feature.testyourself.SOURCE_CURATED
import com.dobi.app.feature.testyourself.TestYourselfChooserScreen
import com.dobi.app.feature.testyourself.TestYourselfFormatScreen
import com.dobi.app.feature.tuitionlearning.TuitionLearningScreen
import com.dobi.app.feature.tuitionproblems.TuitionProblemsScreen
import com.dobi.app.feature.uploadtuition.TuitionUploadParent
import com.dobi.app.feature.uploadtuition.UploadTuitionOptionsScreen
import com.dobi.app.feature.uploadwork.UploadWorkScreen
import com.dobi.app.feature.answerquestions.QuestionSourceScreen
import com.dobi.app.feature.quiz.QuizScreen
import com.dobi.app.feature.classtestprep.ClassTestPrepScreen
import com.dobi.app.feature.writeanswer.WriteAnswerScreen
import com.dobi.app.feature.homework.HomeworkLogScreen
import com.dobi.app.feature.childpin.EnterChildPinScreen
import com.dobi.app.feature.pairing.EnterPairingCodeScreen
import com.dobi.app.feature.parentdashboard.ActivityDetailScreen
import com.dobi.app.feature.parentdashboard.HomeworkSubmissionScreen
import com.dobi.app.feature.parentdashboard.InsightsScreen
import com.dobi.app.feature.parenthome.ParentHomeScreen
import com.dobi.app.feature.roleselection.RoleSelectionScreen
import com.dobi.app.feature.selectchild.SelectChildScreen
import com.dobi.app.feature.streak.ClanLeaderboardScreen
import com.dobi.app.feature.streak.StreakHomeScreen
import com.dobi.app.feature.streak.StreakShareOptionsScreen
import com.dobi.app.feature.streak.StreakUploadPickerScreen
import com.dobi.app.feature.studygoals.StudyGoalsScreen
import com.dobi.app.feature.subjectiveresult.SubjectiveResultScreen
import com.dobi.app.feature.webview.WebViewScreen

@Composable
fun DobiNavGraph(
    navController: NavHostController,
    startDestination: Screen
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            slideIntoContainer(SlideDirection.Start, animationSpec = tween(280)) +
                fadeIn(tween(180))
        },
        exitTransition = {
            slideOutOfContainer(SlideDirection.Start, animationSpec = tween(280)) +
                fadeOut(tween(180))
        },
        popEnterTransition = {
            slideIntoContainer(SlideDirection.End, animationSpec = tween(280)) +
                fadeIn(tween(180))
        },
        popExitTransition = {
            slideOutOfContainer(SlideDirection.End, animationSpec = tween(280)) +
                fadeOut(tween(180))
        }
    ) {
        composable<Screen.RoleSelection> {
            RoleSelectionScreen(
                onNavigateToPhoneAuth = { navController.navigate(Screen.PhoneAuth) },
                onNavigateToParentHome = {
                    navController.navigate(Screen.ParentHome) {
                        popUpTo<Screen.RoleSelection> { inclusive = true }
                    }
                },
                onNavigateToSelectChild = { navController.navigate(Screen.SelectChild) },
                onNavigateToPairingCode = { navController.navigate(Screen.EnterPairingCode) }
            )
        }

        composable<Screen.EnterPairingCode> {
            EnterPairingCodeScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToChildHome = { childId ->
                    navController.navigate(Screen.ChildHome(childId)) {
                        popUpTo<Screen.RoleSelection> { inclusive = true }
                    }
                }
            )
        }

        composable<Screen.PhoneAuth> {
            PhoneAuthScreen(
                onNavigateToOtp = { otpScreen -> navController.navigate(otpScreen) },
                onNavigateToParentHome = {
                    navController.navigate(Screen.ParentHome) {
                        popUpTo<Screen.RoleSelection> { inclusive = true }
                    }
                }
            )
        }

        composable<Screen.OtpVerification> {
            OtpVerificationScreen(
                onNavigateToParentHome = {
                    navController.navigate(Screen.ParentHome) {
                        popUpTo<Screen.RoleSelection> { inclusive = true }
                    }
                }
            )
        }

        composable<Screen.EnterChildPin> {
            EnterChildPinScreen(
                onBack = { navController.popBackStack() },
                onNavigateToChildHome = { childId ->
                    navController.navigate(Screen.ChildHome(childId)) {
                        popUpTo<Screen.EnterChildPin> { inclusive = true }
                    }
                },
                onForgotPin = { navController.navigate(Screen.PinRecovery) }
            )
        }

        composable<Screen.PinRecovery> {
            com.dobi.app.feature.childpin.PinRecoveryScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable<Screen.AddChild> {
            AddChildScreen(
                onNavigateToParentHome = {
                    navController.navigate(Screen.ParentHome) {
                        popUpTo<Screen.AddChild> { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<Screen.SelectChild> {
            SelectChildScreen(
                onChildSelected = { childId ->
                    navController.navigate(Screen.EnterChildPin(childId))
                }
            )
        }

        composable<Screen.ChildHome> {
            ChildHomeScreen(
                onNavigateToConceptList = {
                    navController.navigate(Screen.ConceptList(ConceptListMode.DEFAULT))
                },
                onNavigateToPractiseLesson = {
                    navController.navigate(Screen.ConceptList(ConceptListMode.PRACTISE_LESSON))
                },
                onNavigateToHomeworkLog = { navController.navigate(Screen.HomeworkLog) },
                onNavigateToClassTestPrep = { navController.navigate(Screen.ClassTestPrep) },
                onNavigateToTuitionWork = {
                    navController.navigate(Screen.UploadTuitionOptions(TuitionUploadParent.WORK.key))
                },
                onNavigateToTuitionHomework = {
                    navController.navigate(Screen.UploadTuitionOptions(TuitionUploadParent.HOMEWORK.key))
                },
                onNavigateToTuitionProblems = {
                    navController.navigate(Screen.TuitionProblems)
                },
                onNavigateToRevisitTopics = {
                    navController.navigate(Screen.RevisitSubjects)
                },
                onNavigateToNeedRevision = {
                    navController.navigate(Screen.NeedRevision)
                },
                onNavigateToTestYourself = {
                    navController.navigate(Screen.ConceptList(ConceptListMode.TEST_YOURSELF))
                },
                onNavigateToStreakDashboard = {
                    navController.navigate(Screen.StreakHome)
                },
                onNavigateToEarnReward = {
                    navController.navigate(Screen.EarnRewardPicker)
                },
                onNavigateToParentTask = { taskId ->
                    navController.navigate(Screen.DoParentTask(taskId))
                },
                onOpenUnit = { unitId ->
                    navController.navigate(Screen.UnitDetail(unitId))
                },
                onOpenConcept = { conceptId ->
                    navController.navigate(Screen.ConceptDetail(conceptId))
                },
                onOpenQuizResult = { unitId, conceptId ->
                    navController.navigate(
                        Screen.Quiz(
                            unitId = unitId,
                            conceptId = conceptId,
                            source = "curated",
                            viewResult = true
                        )
                    )
                },
                onOpenSubjectiveResult = { resultId ->
                    navController.navigate(Screen.SubjectiveResult(resultId))
                },
                onOpenUploadGroup = { groupKey ->
                    navController.navigate(Screen.RevisitDetail(groupKey))
                },
                onRequestParentAccess = {
                    // Pop back to RoleSelection using the pop animation (slide back)
                    val popped = navController.popBackStack(
                        route = Screen.RoleSelection,
                        inclusive = false
                    )
                    if (!popped) {
                        // Fallback: RoleSelection not in the stack (e.g. paired child device launched straight into ChildHome)
                        navController.navigate(Screen.RoleSelection) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable<Screen.ParentHome> {
            ParentHomeScreen(
                onNavigateToChildHome = { childId ->
                    navController.navigate(Screen.EnterChildPin(childId))
                },
                onNavigateToAddChild = {
                    navController.navigate(Screen.AddChild)
                },
                onRedirectToAddChild = {
                    navController.navigate(Screen.AddChild) {
                        popUpTo<Screen.ParentHome> { inclusive = true }
                    }
                },
                onNavigateToRoleSelection = {
                    navController.navigate(Screen.RoleSelection) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToParentTask = { taskId ->
                    navController.navigate(Screen.ParentTaskDetail(taskId))
                },
                onNavigateToActivityDetail = { categoryKey, childId ->
                    navController.navigate(Screen.ActivityDetail(categoryKey, childId))
                },
                onNavigateToInsights = { childId ->
                    navController.navigate(Screen.Insights(childId))
                },
                onNavigateToUploadGroup = { groupKey ->
                    navController.navigate(Screen.RevisitDetail(groupKey))
                },
                onNavigateToQuizResult = { unitId, conceptId, childId ->
                    navController.navigate(
                        Screen.Quiz(
                            unitId = unitId,
                            conceptId = conceptId,
                            source = "curated",
                            viewResult = true,
                            childId = childId,
                        )
                    )
                },
                onNavigateToSubjectiveResult = { resultId ->
                    navController.navigate(Screen.SubjectiveResult(resultId))
                },
                onNavigateToHomework = { homeworkId ->
                    navController.navigate(Screen.HomeworkSubmission(homeworkId))
                },
                onNavigateToWebView = { url, title ->
                    navController.navigate(Screen.WebView(url, title))
                },
                onNavigateToStudyGoals = { childId ->
                    navController.navigate(Screen.StudyGoals(childId))
                },
            )
        }

        composable<Screen.StudyGoals> {
            StudyGoalsScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable<Screen.WebView> { entry ->
            val route = entry.toRoute<Screen.WebView>()
            WebViewScreen(
                url = route.url,
                title = route.title,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable<Screen.Insights> {
            InsightsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<Screen.ActivityDetail> { entry ->
            val route = entry.toRoute<Screen.ActivityDetail>()
            ActivityDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onOpenUploadGroup = { groupKey ->
                    navController.navigate(Screen.RevisitDetail(groupKey))
                },
                onOpenQuizResult = { unitId, conceptId ->
                    navController.navigate(
                        Screen.Quiz(
                            unitId = unitId,
                            conceptId = conceptId,
                            source = "curated",
                            viewResult = true,
                            childId = route.childId
                        )
                    )
                },
                onOpenSubjectiveResult = { resultId ->
                    navController.navigate(Screen.SubjectiveResult(resultId))
                },
                onOpenHomework = { homeworkId ->
                    navController.navigate(Screen.HomeworkSubmission(homeworkId))
                }
            )
        }

        composable<Screen.HomeworkSubmission> { entry ->
            val route = entry.toRoute<Screen.HomeworkSubmission>()
            HomeworkSubmissionScreen(
                homeworkId = route.homeworkId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable<Screen.ConceptList> { entry ->
            val route = entry.toRoute<Screen.ConceptList>()
            ConceptListScreen(
                mode = route.mode,
                onNavigateBack = { navController.popBackStack() },
                onConceptClick = { conceptId ->
                    if (route.mode == ConceptListMode.TEST_YOURSELF) {
                        navController.navigate(Screen.TestYourselfChooser(conceptId = conceptId))
                    } else {
                        navController.navigate(Screen.ConceptDetail(conceptId))
                    }
                },
                onUnitClick = { unitId ->
                    if (route.mode == ConceptListMode.TEST_YOURSELF) {
                        navController.navigate(Screen.TestYourselfChooser(unitId = unitId))
                    } else {
                        navController.navigate(Screen.UnitDetail(unitId))
                    }
                }
            )
        }

        composable<Screen.ConceptDetail> { entry ->
            val route = entry.toRoute<Screen.ConceptDetail>()
            ConceptDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onAnswerQuestions = {
                    navController.navigate(
                        Screen.AnswerQuestionsSource(conceptId = route.conceptId)
                    )
                },
                onUploadWork = { kindKey ->
                    navController.navigate(
                        Screen.UploadWork(kind = kindKey, conceptId = route.conceptId)
                    )
                }
            )
        }

        composable<Screen.UnitDetail> { entry ->
            val route = entry.toRoute<Screen.UnitDetail>()
            UnitDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onUploadWork = { kindKey ->
                    navController.navigate(Screen.UploadWork(route.unitId, kindKey))
                },
                onAnswerQuestions = {
                    navController.navigate(Screen.AnswerQuestionsSource(route.unitId))
                }
            )
        }

        composable<Screen.UploadWork> { entry ->
            val route = entry.toRoute<Screen.UploadWork>()
            UploadWorkScreen(
                onNavigateBack = { navController.popBackStack() },
                onUploadComplete = { kindKey, quizCacheKey, displayName ->
                    when (kindKey) {
                        "self_learning" -> navController.navigate(
                            Screen.Quiz(
                                unitId = route.unitId,
                                conceptId = route.conceptId,
                                source = "ai"
                            )
                        ) { popUpTo<Screen.UploadWork> { inclusive = true } }

                        "ai_test_prep" -> navController.navigate(
                            Screen.Quiz(
                                unitId = quizCacheKey,
                                source = "ai",
                                displayName = displayName
                            )
                        ) { popUpTo<Screen.UploadWork> { inclusive = true } }

                        else -> {
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set(UnitDetailViewModel.KEY_UPLOAD_RESULT, kindKey)
                            navController.popBackStack()
                        }
                    }
                }
            )
        }

        composable<Screen.AnswerQuestionsSource> { entry ->
            val route = entry.toRoute<Screen.AnswerQuestionsSource>()
            QuestionSourceScreen(
                onNavigateBack = { navController.popBackStack() },
                onPickCuratedSet = {
                    navController.navigate(
                        Screen.Quiz(unitId = route.unitId, conceptId = route.conceptId)
                    )
                },
                onPickUploadAndGenerate = {
                    navController.navigate(
                        Screen.UploadWork(
                            unitId = route.unitId,
                            conceptId = route.conceptId,
                            kind = "self_learning"
                        )
                    )
                },
                onViewLastResult = {
                    navController.navigate(
                        Screen.Quiz(
                            unitId = route.unitId,
                            conceptId = route.conceptId,
                            viewResult = true
                        )
                    )
                }
            )
        }

        composable<Screen.Quiz> {
            val quizContext = LocalContext.current
            QuizScreen(
                onNavigateBack = { navController.popBackStack() },
                onSnapSend = { displayName ->
                    val text = "🔥 Today's streak unlocked! I just nailed a quiz on " +
                        "\"$displayName\" on Eduksha. Your turn!"
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    quizContext.startActivity(
                        Intent.createChooser(intent, "Share your streak")
                    )
                    val popped = navController.popBackStack(
                        route = Screen.StreakHome,
                        inclusive = false
                    )
                    if (!popped) {
                        // Quiz launched from a flow that didn't go through
                        // StreakHome (e.g. Practise Lesson) — navigate there
                        // and clear the back stack down to ChildHome.
                        navController.navigate(Screen.StreakHome) {
                            popUpTo<Screen.ChildHome>()
                        }
                    }
                }
            )
        }

        composable<Screen.HomeworkLog> {
            HomeworkLogScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<Screen.ClassTestPrep> {
            ClassTestPrepScreen(
                onNavigateBack = { navController.popBackStack() },
                onLaunchMcqQuiz = { key, displayName ->
                    navController.navigate(
                        Screen.Quiz(
                            unitId = key,
                            source = "ai",
                            displayName = displayName
                        )
                    )
                },
                onLaunchWriteAnswers = {
                    navController.navigate(Screen.WriteAnswer)
                },
                onLaunchAiPracticeUpload = {
                    navController.navigate(
                        Screen.UploadWork(kind = "ai_test_prep")
                    )
                }
            )
        }

        composable<Screen.UploadTuitionOptions> { entry ->
            val route = entry.toRoute<Screen.UploadTuitionOptions>()
            UploadTuitionOptionsScreen(
                parent = TuitionUploadParent.fromKey(route.parent),
                onNavigateBack = { navController.popBackStack() },
                onSelectKind = { kind ->
                    // All five tuition kinds use the rich multi-subject + topic flow.
                    navController.navigate(Screen.TuitionLearning(kindKey = kind.key))
                }
            )
        }

        composable<Screen.TuitionLearning> {
            TuitionLearningScreen(
                onNavigateBack = { navController.popBackStack() },
                onSavedNavigateBack = { navController.popBackStack() }
            )
        }

        composable<Screen.TuitionProblems> {
            TuitionProblemsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<Screen.RevisitSubjects> {
            RevisitSubjectsScreen(
                onNavigateBack = { navController.popBackStack() },
                onTopicClick = { groupKey ->
                    navController.navigate(Screen.RevisitDetail(groupKey))
                }
            )
        }

        composable<Screen.RevisitDetail> {
            RevisitDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onTakeQuiz = { unitId, conceptId ->
                    navController.navigate(
                        Screen.Quiz(unitId = unitId, conceptId = conceptId)
                    )
                },
                onViewLastResult = { unitId, conceptId ->
                    navController.navigate(
                        Screen.Quiz(unitId = unitId, conceptId = conceptId, viewResult = true)
                    )
                },
                onViewSubjectiveResult = { resultId ->
                    navController.navigate(Screen.SubjectiveResult(resultId))
                }
            )
        }

        composable<Screen.SubjectiveResult> {
            SubjectiveResultScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<Screen.StreakHome> {
            StreakHomeScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLeaderboard = {
                    navController.navigate(Screen.ClanLeaderboard)
                },
                onUploadToday = {
                    navController.navigate(Screen.StreakUploadPicker)
                }
            )
        }

        composable<Screen.ClanLeaderboard> {
            ClanLeaderboardScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<Screen.EarnRewardPicker> {
            EarnRewardPickerScreen(
                onNavigateBack = { navController.popBackStack() },
                onRequestSent = { navController.popBackStack() }
            )
        }

        composable<Screen.ParentTaskDetail> {
            ParentTaskDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onFinished = { navController.popBackStack() }
            )
        }

        composable<Screen.DoParentTask> { entry ->
            val route = entry.toRoute<Screen.DoParentTask>()
            DoParentTaskScreen(
                onNavigateBack = { navController.popBackStack() },
                onFinished = { navController.popBackStack() },
                onRequestNextTask = {
                    navController.navigate(Screen.EarnRewardPicker) {
                        popUpTo<Screen.DoParentTask> { inclusive = true }
                    }
                },
                onLaunchQuiz = { cacheKey, displayName ->
                    navController.navigate(
                        Screen.Quiz(
                            unitId = cacheKey,
                            source = "ai",
                            displayName = displayName,
                            parentTaskId = route.taskId
                        )
                    ) {
                        // Remove the DoParentTask from the back-stack so the
                        // child returns to home after the quiz, not back to
                        // a stale "Start quiz" screen.
                        popUpTo<Screen.DoParentTask> { inclusive = true }
                    }
                }
            )
        }

        composable<Screen.StreakUploadPicker> {
            StreakUploadPickerScreen(
                onNavigateBack = { navController.popBackStack() },
                onContinue = { resolved ->
                    navController.navigate(
                        Screen.StreakShareOptions(
                            unitId = if (resolved.isUnitScope) resolved.unitId else "",
                            conceptId = if (resolved.isUnitScope) "" else resolved.conceptId,
                            displayName = resolved.displayName,
                            subject = resolved.subject
                        )
                    )
                }
            )
        }

        composable<Screen.StreakShareOptions> {
            StreakShareOptionsScreen(
                onNavigateBack = { navController.popBackStack() },
                onLaunchQuiz = { cacheKey, displayName ->
                    navController.navigate(
                        Screen.Quiz(
                            unitId = cacheKey,
                            source = "ai",
                            displayName = displayName
                        )
                    ) {
                        // Pop the picker + share-options off so back from
                        // the quiz lands on the streak dashboard.
                        popUpTo<Screen.StreakHome>()
                    }
                },
                onFinishedShareOnly = {
                    navController.popBackStack(
                        route = Screen.StreakHome,
                        inclusive = false
                    )
                }
            )
        }

        composable<Screen.NeedRevision> {
            NeedRevisionScreen(
                onNavigateBack = { navController.popBackStack() },
                onOpenConcept = { conceptId ->
                    navController.navigate(Screen.ConceptDetail(conceptId))
                },
                onOpenUnit = { unitId ->
                    navController.navigate(Screen.UnitDetail(unitId))
                }
            )
        }

        composable<Screen.TestYourselfChooser> {
            TestYourselfChooserScreen(
                onNavigateBack = { navController.popBackStack() },
                onPickSource = { source, unitId, conceptId ->
                    navController.navigate(
                        Screen.TestYourselfFormat(
                            source = source,
                            unitId = unitId,
                            conceptId = conceptId
                        )
                    )
                }
            )
        }

        composable<Screen.TestYourselfFormat> {
            TestYourselfFormatScreen(
                onNavigateBack = { navController.popBackStack() },
                onPickFormat = { source, format, unitId, conceptId ->
                    when {
                        source == SOURCE_CURATED && format == FORMAT_MCQ -> {
                            navController.navigate(
                                Screen.Quiz(
                                    unitId = unitId,
                                    conceptId = conceptId,
                                    source = "curated"
                                )
                            )
                        }
                        source == SOURCE_AI && format == FORMAT_MCQ -> {
                            navController.navigate(
                                Screen.UploadWork(
                                    unitId = unitId,
                                    conceptId = conceptId,
                                    kind = "self_learning"
                                )
                            )
                        }
                        format == FORMAT_SUBJECTIVE -> {
                            // Both curated and AI subjective reuse the
                            // Class Test Prep WriteAnswer pipeline. The
                            // VM has already populated AiPracticeContextHolder
                            // with the lesson/concept scope.
                            navController.navigate(Screen.WriteAnswer)
                        }
                    }
                }
            )
        }

        composable<Screen.WriteAnswer> {
            WriteAnswerScreen(
                onBack = { navController.popBackStack() },
                onLaunchAntiCheatQuiz = {
                    navController.navigate(
                        Screen.Quiz(
                            unitId = com.dobi.app.feature.writeanswer.WriteAnswerViewModel.ANTI_CHEAT_KEY,
                            source = "ai",
                            displayName = "Quick check"
                        )
                    ) { popUpTo<Screen.WriteAnswer> { inclusive = true } }
                }
            )
        }
    }
}
