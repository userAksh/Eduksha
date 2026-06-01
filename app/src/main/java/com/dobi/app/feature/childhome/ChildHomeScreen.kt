package com.dobi.app.feature.childhome

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Surface
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import com.dobi.app.core.ui.components.DobiBottomNavBar
import com.dobi.app.core.ui.components.childNavItems
import com.dobi.app.core.ui.theme.DobiBackground
import com.dobi.app.core.ui.theme.SFProBold
import com.dobi.app.core.ui.theme.SFProSemiBold
import com.dobi.app.core.ui.theme.SFProMedium
import com.dobi.app.core.ui.theme.DobiPrimary
import com.dobi.app.core.ui.theme.DobiTextPrimary
import com.dobi.app.core.ui.theme.DobiTextSecondary
import com.dobi.app.core.ui.theme.DobiTheme
import com.dobi.app.domain.model.ParentTask
import com.dobi.app.domain.model.ParentTaskStatus
import com.dobi.app.domain.model.ParentTaskType
import com.dobi.app.feature.childprofile.ChildProfileContent
import com.dobi.app.feature.mylearnings.MyLearningsContent

// ── Entry point ────────────────────────────────────────────────────────────────

@Composable
fun ChildHomeScreen(
    onNavigateToConceptList: () -> Unit = {},
    onNavigateToPractiseLesson: () -> Unit = {},
    onNavigateToHomeworkLog: () -> Unit = {},
    onNavigateToClassTestPrep: () -> Unit = {},
    onNavigateToTuitionWork: () -> Unit = {},
    onNavigateToTuitionHomework: () -> Unit = {},
    onNavigateToTuitionProblems: () -> Unit = {},
    onNavigateToRevisitTopics: () -> Unit = {},
    onNavigateToNeedRevision: () -> Unit = {},
    onNavigateToTestYourself: () -> Unit = {},
    onNavigateToStreakDashboard: () -> Unit = {},
    onNavigateToEarnReward: () -> Unit = {},
    onNavigateToParentTask: (taskId: String) -> Unit = {},
    onNavigateToParentTasksList: () -> Unit = {},
    onOpenUnit: (unitId: String) -> Unit = {},
    onOpenConcept: (conceptId: String) -> Unit = {},
    onOpenQuizResult: (unitId: String, conceptId: String) -> Unit = { _, _ -> },
    onOpenSubjectiveResult: (resultId: String) -> Unit = {},
    onOpenUploadGroup: (groupKey: String) -> Unit = {},
    onOpenQuizResultFromProfile: (unitId: String, conceptId: String) -> Unit = { _, _ -> },
    onRequestParentAccess: () -> Unit = {},
    viewModel: ChildHomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var popupModeIndex by remember { mutableStateOf<Int?>(null) }
    val context = LocalContext.current

    // Lock back button — child cannot return to parent view without the PIN gate
    androidx.activity.compose.BackHandler(enabled = true) {
        onRequestParentAccess()
    }

    // Invite Friend: VM asks the server for a fresh code, then we open the
    // share sheet with the Play Store URL embedded with the install referrer.
    LaunchedEffect(viewModel) {
        viewModel.inviteEvents.collectLatest { event ->
            when (event) {
                is ChildHomeViewModel.InviteUiEvent.Share -> {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "Join me on Dobi! Let's keep our study streaks going together. 🔥\n${event.url}"
                        )
                    }
                    context.startActivity(Intent.createChooser(intent, "Invite a friend"))
                }
                is ChildHomeViewModel.InviteUiEvent.CreateFailed -> {
                    Toast.makeText(
                        context,
                        "Couldn't create your invite link. Try again in a moment.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as android.app.Activity).window
            window.statusBarColor = DobiBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    Scaffold(
        containerColor = DobiBackground,
        bottomBar = {
            AnimatedVisibility(
                visible = popupModeIndex == null,
                enter = slideInVertically(tween(200)) { it },
                exit = slideOutVertically(tween(180)) { it },
            ) {
                DobiBottomNavBar(
                    items = childNavItems,
                    selectedIndex = selectedTab,
                    onItemSelected = { selectedTab = it }
                )
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            0 -> ChildHomeContent(
                uiState = uiState,
                onStartLearning = onNavigateToConceptList,
                onNavigateToPractiseLesson = onNavigateToPractiseLesson,
                onNavigateToHomeworkLog = onNavigateToHomeworkLog,
                onNavigateToClassTestPrep = onNavigateToClassTestPrep,
                onNavigateToTuitionWork = onNavigateToTuitionWork,
                onNavigateToTuitionHomework = onNavigateToTuitionHomework,
                onNavigateToTuitionProblems = onNavigateToTuitionProblems,
                onNavigateToRevisitTopics = onNavigateToRevisitTopics,
                onNavigateToNeedRevision = onNavigateToNeedRevision,
                onNavigateToTestYourself = onNavigateToTestYourself,
                onNavigateToStreakDashboard = onNavigateToStreakDashboard,
                onNavigateToInviteFriend = viewModel::onInviteFriendClicked,
                onNavigateToEarnReward = onNavigateToEarnReward,
                onNavigateToParentTasksList = onNavigateToParentTasksList,
                onSaveTutor = viewModel::saveTutor,
                onTaskClick = onNavigateToParentTask,
                onSendReminderToParent = viewModel::sendReminderToParent,
                contentPadding = innerPadding,
                selectedModeIndex = popupModeIndex,
                onModeSelected = { popupModeIndex = it },
            )
            1 -> MyLearningsContent(
                contentPadding = innerPadding,
                onMarkClick = { mark ->
                    if (com.dobi.app.domain.model.RevisionScope.fromKey(mark.scope)
                        == com.dobi.app.domain.model.RevisionScope.UNIT
                    ) {
                        onOpenUnit(mark.targetId)
                    } else {
                        onOpenConcept(mark.targetId)
                    }
                },
                onSeeAllRevision = onNavigateToNeedRevision,
                onTodayItemClick = { entry ->
                    when (val t = entry.navTarget) {
                        is com.dobi.app.feature.mylearnings.TodayNavTarget.QuizResult ->
                            onOpenQuizResult(t.unitId, t.conceptId)
                        is com.dobi.app.feature.mylearnings.TodayNavTarget.Subjective ->
                            onOpenSubjectiveResult(t.resultId)
                        is com.dobi.app.feature.mylearnings.TodayNavTarget.ParentTask ->
                            onNavigateToParentTask(t.taskId)
                        is com.dobi.app.feature.mylearnings.TodayNavTarget.UploadGroup ->
                            onOpenUploadGroup(t.groupKey)
                        null -> Unit
                    }
                }
            )
            2 -> ChildProfileContent(
                contentPadding = innerPadding,
                onInviteFriend = viewModel::onInviteFriendClicked,
                onSwitchProfile = onRequestParentAccess,
                onAccountDeleted = onRequestParentAccess,
                onOpenUploadGroup = onOpenUploadGroup,
                onOpenQuizResult = onOpenQuizResultFromProfile
            )
        }
    }
}

// ── Main scrollable content ────────────────────────────────────────────────────

@Composable
private fun ChildHomeContent(
    uiState: ChildHomeUiState,
    onStartLearning: () -> Unit,
    onNavigateToPractiseLesson: () -> Unit,
    onNavigateToHomeworkLog: () -> Unit,
    onNavigateToClassTestPrep: () -> Unit,
    onNavigateToTuitionWork: () -> Unit,
    onNavigateToTuitionHomework: () -> Unit,
    onNavigateToTuitionProblems: () -> Unit,
    onNavigateToRevisitTopics: () -> Unit,
    onNavigateToNeedRevision: () -> Unit,
    onNavigateToTestYourself: () -> Unit,
    onNavigateToStreakDashboard: () -> Unit,
    onNavigateToInviteFriend: () -> Unit,
    onNavigateToEarnReward: () -> Unit,
    onNavigateToParentTasksList: () -> Unit,
    onSaveTutor: (String, String) -> Unit,
    onTaskClick: (String) -> Unit,
    onSendReminderToParent: (taskId: String) -> Unit = {},
    contentPadding: PaddingValues,
    selectedModeIndex: Int?,
    onModeSelected: (Int?) -> Unit,
) {
    // Keep last valid index so exit animation still renders correct content
    var lastModeIndex by remember { mutableIntStateOf(0) }

    Box(Modifier.fillMaxSize()) {

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(DobiBackground),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = contentPadding.calculateTopPadding() + 28.dp,
                bottom = contentPadding.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Greeting
            item {
                Text(
                    text = "Hi, ${uiState.childName}! 👋",
                    style = MaterialTheme.typography.headlineMedium,
                    color = DobiTextPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Keep up the great learning!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DobiTextSecondary
                )
                Spacer(Modifier.height(20.dp))
            }

            // Pending parent task bar — surfaces tasks the parent assigned
            // (status ASSIGNED). Sits above the streak so it's the first
            // call to action the child sees when there's work waiting.
            val pendingFromParent = uiState.parentTasks.filter {
                it.status == ParentTaskStatus.ASSIGNED
            }
            if (pendingFromParent.isNotEmpty()) {
                item {
                    PendingParentTaskBar(
                        tasks = pendingFromParent,
                        onClick = { taskId -> onTaskClick(taskId) },
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }

            // Streak banner
            item {
                StreakBanner(
                    streakDays = uiState.streakDays,
                    onInviteFriend = onNavigateToInviteFriend,
                    onViewDashboard = onNavigateToStreakDashboard
                )
                Spacer(Modifier.height(20.dp))
            }

            // Friends row — only shows once at least one friend was added via
            // an invite link claim. Read-only chip list for now.
            if (uiState.friends.isNotEmpty()) {
                item {
                    FriendsRow(friends = uiState.friends)
                    Spacer(Modifier.height(20.dp))
                }
            }

            // Revision mode cards
            item {
                RevisionModeRow(onRevisionModeClick = { lastModeIndex = it; onModeSelected(it) })
                Spacer(Modifier.height(20.dp))
            }

            // Start Learning CTA
            item {
                Button(
                    onClick = onStartLearning,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DobiPrimary)
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Start Learning",
                        fontWeight = SFProSemiBold,
                        fontSize = 15.sp
                    )
                }
                Spacer(Modifier.height(28.dp))
            }

            // Tasks from parents — header
            val requestedTasks = uiState.parentTasks.filter { it.status == ParentTaskStatus.REQUESTED }
            // Only tasks that still need the child's attention (excludes ACCEPTED/done tasks).
            // When everything is ACCEPTED, the section reverts to the empty state so the
            // child is prompted to request their next task.
            val pendingTasks = uiState.parentTasks.filter {
                it.status in listOf(
                    ParentTaskStatus.ASSIGNED,
                    ParentTaskStatus.PENDING_REVIEW,
                    ParentTaskStatus.REJECTED
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Tasks from Parents",
                        style = MaterialTheme.typography.titleMedium,
                        color = DobiTextPrimary,
                        fontWeight = SFProSemiBold
                    )
                    if (uiState.parentTasks.isNotEmpty()) {
                        Text(
                            text = "See all",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DobiPrimary,
                            fontWeight = SFProMedium,
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onNavigateToParentTasksList() }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Tasks content — banner only for in-flight tasks; done = prompt next request
            when {
                pendingTasks.isNotEmpty() -> item {
                    ParentTasksSummaryBanner(
                        tasks = pendingTasks,
                        onSeeAll = onNavigateToParentTasksList,
                        onTaskClick = onTaskClick,
                    )
                }
                requestedTasks.isNotEmpty() -> item {
                    ParentTasksSummaryBanner(
                        tasks = requestedTasks,
                        onSeeAll = onNavigateToParentTasksList,
                        onTaskClick = onTaskClick,
                    )
                }
                else -> item {
                    // No tasks, or all tasks ACCEPTED — encourage requesting the next one
                    ParentTasksEmptyCard(onClick = onNavigateToEarnReward)
                }
            }
        }  // end LazyColumn

        // Dim scrim
        AnimatedVisibility(
            visible = selectedModeIndex != null,
            enter = fadeIn(tween(220)),
            exit  = fadeOut(tween(180))
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onModeSelected(null) }
            )
        }

        // Popup card — springs open from the card row (origin y ≈ 0.72)
        AnimatedVisibility(
            visible = selectedModeIndex != null,
            modifier = Modifier.align(Alignment.Center),
            enter = scaleIn(
                initialScale = 0.45f,
                transformOrigin = TransformOrigin(0.5f, 0.72f),
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness    = Spring.StiffnessMediumLow
                )
            ) + fadeIn(tween(160)),
            exit = scaleOut(
                targetScale = 0.55f,
                transformOrigin = TransformOrigin(0.5f, 0.72f),
                animationSpec = tween(160)
            ) + fadeOut(tween(140))
        ) {
            RevisionDialog(
                modeIndex = lastModeIndex,
                onDismiss = { onModeSelected(null) },
                onOptionClick = { optionIndex ->
                    onModeSelected(null)
                    when {
                        lastModeIndex == 0 && optionIndex == 0 -> onNavigateToPractiseLesson()
                        lastModeIndex == 0 && optionIndex == 1 -> onNavigateToHomeworkLog()
                        lastModeIndex == 0 && optionIndex == 2 -> onNavigateToClassTestPrep()
                        lastModeIndex == 1 && optionIndex == 0 -> onNavigateToTuitionWork()
                        lastModeIndex == 1 && optionIndex == 1 -> onNavigateToTuitionHomework()
                        lastModeIndex == 1 && optionIndex == 2 -> onNavigateToTuitionProblems()
                        lastModeIndex == 2 && optionIndex == 0 -> onNavigateToRevisitTopics()
                        lastModeIndex == 2 && optionIndex == 1 -> onNavigateToNeedRevision()
                        lastModeIndex == 2 && optionIndex == 2 -> onNavigateToTestYourself()
                    }
                },
                tutorHonorific = uiState.tutorHonorific,
                tutorName = uiState.tutorName,
                onTutorSave = onSaveTutor
            )
        }

    } // end Box
}

// ── Revision mode row ──────────────────────────────────────────────────────────

private data class RevisionMode(
    val emoji: String,
    val title: String,
    val subtitle: String,
    val cardColor: Color,
    val iconBg: Color,
    val accentColor: Color
)

private val revisionModes = listOf(
    RevisionMode(
        emoji = "🏫",
        title = "School\nWork",
        subtitle = "Revise from class",
        cardColor = Color(0xFFDBEAFE),
        iconBg = Color(0xFFBFD7F7),
        accentColor = Color(0xFF2563EB)
    ),
    RevisionMode(
        emoji = "📐",
        title = "Tuition\nWork",
        subtitle = "Revise from tutor",
        cardColor = Color(0xFFEDE9FE),
        iconBg = Color(0xFFDDD6FE),
        accentColor = Color(0xFF7C3AED)
    ),
    RevisionMode(
        emoji = "✨",
        title = "Self\nRevision",
        subtitle = "Study on your own",
        cardColor = Color(0xFFF0FDF4),
        iconBg = Color(0xFFDCFCE7),
        accentColor = Color(0xFF16A34A)
    )
)

@Composable
private fun RevisionModeRow(onRevisionModeClick: (Int) -> Unit) {
    Column {
        Text(
            text = "What would you like to revise?",
            style = MaterialTheme.typography.titleSmall,
            color = DobiTextPrimary,
            fontWeight = SFProSemiBold
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            revisionModes.forEachIndexed { index, mode ->
                RevisionCard(
                    mode = mode,
                    onClick = { onRevisionModeClick(index) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun RevisionCard(
    mode: RevisionMode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = mode.accentColor.copy(alpha = 0.18f),
                ambientColor = mode.accentColor.copy(alpha = 0.08f)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = mode.cardColor),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(mode.iconBg),
                contentAlignment = Alignment.Center
            ) {
                Text(text = mode.emoji, fontSize = 22.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = mode.title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = SFProBold,
                color = mode.accentColor,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = mode.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = DobiTextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp,
                fontSize = 10.sp
            )
        }
    }
}

// ── Revision bottom sheet ──────────────────────────────────────────────────────

private data class RevisionOption(
    val emoji: String,
    val title: String,
    val subtitle: String
)

private data class RevisionSheet(
    val title: String,
    val emoji: String,
    val options: List<RevisionOption>
)

private val revisionSheetOptions = listOf(
    RevisionSheet(
        title = "School Work",
        emoji = "🏫",
        options = listOf(
            RevisionOption("📖", "Practise a Lesson", "Go through a school lesson step by step"),
            RevisionOption("📝", "Log Homework", "Mark today's homework as completed"),
            RevisionOption("📋", "Prep for a Class Test", "Review and get ready for an upcoming test"),
        )
    ),
    RevisionSheet(
        title = "Tuition Work",
        emoji = "📐",
        options = listOf(
            RevisionOption("📚", "Upload Tuition Work", "Show what you did in today's class"),
            RevisionOption("📝", "Upload Tuition Homework", "Share homework given by your tutor"),
            RevisionOption("🤔", "Tuition Problems", "Share your problems with your parents"),
        )
    ),
    RevisionSheet(
        title = "Self Revision",
        emoji = "✨",
        options = listOf(
            RevisionOption("🔁", "Revisit Past Topics", "Go back to the topics you've completed and strengthen your understanding."),
            RevisionOption("⚠️", "Need Revision", "Topics you might be forgetting. Revise them to stay sharp and confident."),
            RevisionOption("🧠", "Test Yourself", "Practice with quizzes or generate AI questions from your textbook."),
        )
    )
)

@Composable
private fun RevisionDialog(
    modeIndex: Int,
    onDismiss: () -> Unit,
    onOptionClick: (Int) -> Unit = {},
    tutorHonorific: String = "",
    tutorName: String = "",
    onTutorSave: (String, String) -> Unit = { _, _ -> }
) {
    val sheet = revisionSheetOptions[modeIndex]
    val mode = revisionModes[modeIndex]
    val isTuition = modeIndex == 1

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(mode.iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = sheet.emoji, fontSize = 24.sp)
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = sheet.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = SFProBold,
                        color = DobiTextPrimary
                    )
                    Text(
                        text = "What would you like to do?",
                        style = MaterialTheme.typography.bodySmall,
                        color = DobiTextSecondary
                    )
                }
            }

            if (isTuition) {
                Spacer(Modifier.height(14.dp))
                TutorEditor(
                    accent = mode.accentColor,
                    iconBg = mode.iconBg,
                    initialHonorific = tutorHonorific,
                    initialName = tutorName,
                    onSave = onTutorSave
                )
            }

            Spacer(Modifier.height(20.dp))

            sheet.options.forEachIndexed { index, option ->
                RevisionOptionRow(
                    option = option,
                    accentColor = mode.accentColor,
                    iconBg = mode.iconBg,
                    onClick = { onOptionClick(index) }
                )
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Cancel",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
                    .padding(vertical = 10.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = SFProMedium,
                color = DobiTextSecondary
            )
        }
    }
}

@Composable
private fun TutorEditor(
    accent: Color,
    iconBg: Color,
    initialHonorific: String,
    initialName: String,
    onSave: (String, String) -> Unit
) {
    var editing by remember { mutableStateOf(initialName.isBlank()) }
    var honorific by remember(initialHonorific) {
        mutableStateOf(initialHonorific.ifBlank { "Sir" })
    }
    var name by remember(initialName) { mutableStateOf(initialName) }

    val containerBg = iconBg.copy(alpha = 0.6f)

    if (!editing) {
        // Saved state — friendly chip with pencil
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(containerBg)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { editing = true }
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("👨‍🏫", fontSize = 22.sp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Your tutor",
                    fontSize = 11.sp,
                    color = accent,
                    fontWeight = SFProSemiBold
                )
                Text(
                    "$initialHonorific $initialName".trim(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = SFProSemiBold,
                    color = DobiTextPrimary
                )
            }
            Icon(
                Icons.Default.Edit,
                contentDescription = "Edit tutor",
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
        }
        return
    }

    // Editing state — Sir/Madam chips + name input
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(containerBg)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("👨‍🏫", fontSize = 22.sp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (initialName.isBlank())
                        "Who's your tutor?"
                    else "Update your tutor",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = SFProBold,
                    color = DobiTextPrimary
                )
                Text(
                    text = "We'll remember this for you",
                    fontSize = 11.sp,
                    color = DobiTextSecondary
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Sir", "Madam").forEach { h ->
                val selected = honorific == h
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) accent else Color.White)
                        .border(
                            width = 1.dp,
                            color = if (selected) accent else accent.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { honorific = h }
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = h,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = SFProSemiBold,
                        color = if (selected) Color.White else accent
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White)
                .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            BasicTextField(
                value = name,
                onValueChange = { name = it.take(40) },
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    color = DobiTextPrimary,
                    fontWeight = FontWeight.Normal
                ),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (name.isEmpty()) {
                        Text(
                            "Tutor's name (e.g. Sharma)",
                            fontSize = 14.sp,
                            color = DobiTextSecondary
                        )
                    }
                    inner()
                }
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (initialName.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White)
                        .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { editing = false }
                        )
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Cancel",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = SFProSemiBold,
                        color = accent
                    )
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (name.isNotBlank()) accent else accent.copy(alpha = 0.4f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = name.isNotBlank()
                    ) {
                        onSave(honorific, name.trim())
                        editing = false
                    }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Save",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = SFProBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun RevisionOptionRow(
    option: RevisionOption,
    accentColor: Color,
    iconBg: Color,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(18.dp),
                spotColor = accentColor.copy(alpha = 0.10f),
                ambientColor = Color.Black.copy(alpha = 0.04f)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Text(text = option.emoji, fontSize = 22.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = option.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = SFProSemiBold,
                    color = DobiTextPrimary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = option.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = DobiTextSecondary,
                    lineHeight = 16.sp
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// ── Streak banner ──────────────────────────────────────────────────────────────

@Composable
private fun StreakBanner(
    streakDays: Int,
    onInviteFriend: () -> Unit,
    onViewDashboard: () -> Unit
) {
    val skyTop = Color(0xFFE7F1FF)
    val skyBottom = Color(0xFFCFE2FF)
    val accentBlue = Color(0xFF2F7DFF)
    val titleNavy = Color(0xFF0F2A52)
    val subtleBorder = Color(0xFFB9D4FF)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(22.dp),
                spotColor = accentBlue.copy(alpha = 0.20f),
                ambientColor = accentBlue.copy(alpha = 0.10f)
            )
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.verticalGradient(colors = listOf(skyTop, skyBottom))
            )
            .border(1.dp, subtleBorder, RoundedCornerShape(22.dp))
    ) {
        // Soft decorative flame in the top-right corner
        Text(
            text = "🔥",
            fontSize = 130.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 18.dp, y = (-22).dp)
                .alpha(0.10f)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(2.dp, accentBlue.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🔥", fontSize = 22.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    val hasStreak = streakDays > 0
                    Text(
                        text = if (hasStreak) "$streakDays-Day Streak"
                        else "Start Your Streak!",
                        fontSize = if (hasStreak) 22.sp else 20.sp,
                        fontWeight = SFProBold,
                        color = titleNavy
                    )
                    Text(
                        text = if (hasStreak) "You're on fire! Keep it going. 🔥"
                        else "Upload today's chapter to light up day 1 🔥",
                        fontSize = 12.sp,
                        color = titleNavy.copy(alpha = 0.65f)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // 7-day progress dots — anchored at today so the current day
            // is always the rightmost lit dot when there's an active streak.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val labels = listOf("M", "T", "W", "T", "F", "S", "S")
                val activeCount = streakDays.coerceIn(0, 7)
                // 0 = Monday … 6 = Sunday (Calendar.DAY_OF_WEEK: 1=Sun, 2=Mon … 7=Sat)
                val todayIdx = (java.util.Calendar.getInstance()
                    .get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
                labels.forEachIndexed { idx, ch ->
                    val active = idx <= todayIdx && (todayIdx - idx) < activeCount
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(
                                    1.dp,
                                    if (active) Color.Transparent else subtleBorder,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🔥",
                                fontSize = 14.sp,
                                modifier = Modifier.alpha(if (active) 1f else 0.28f)
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = ch,
                            fontSize = 10.sp,
                            color = titleNavy.copy(alpha = if (active) 0.9f else 0.45f),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // CTAs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StreakActionButton(
                    emoji = "👥",
                    label = "Invite Friend",
                    accent = accentBlue,
                    modifier = Modifier.weight(1f),
                    onClick = onInviteFriend
                )
                StreakActionButton(
                    emoji = "📈",
                    label = "Dashboard",
                    accent = accentBlue,
                    modifier = Modifier.weight(1f),
                    onClick = onViewDashboard
                )
            }
        }
    }
}

@Composable
private fun StreakActionButton(
    emoji: String,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(text = emoji, fontSize = 16.sp)
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            color = accent,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ── Friends row ────────────────────────────────────────────────────────────────

@Composable
private fun FriendsRow(friends: List<com.dobi.app.domain.model.Friend>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Your friends",
            style = MaterialTheme.typography.titleMedium,
            color = DobiTextPrimary,
            fontWeight = SFProSemiBold
        )
        Spacer(Modifier.height(10.dp))
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(friends, key = { "${it.parentId}/${it.childId}" }) { friend ->
                FriendChip(friend)
            }
        }
    }
}

@Composable
private fun FriendChip(friend: com.dobi.app.domain.model.Friend) {
    val emoji = if (friend.gender.equals("girl", ignoreCase = true)) "👧" else "🧑"
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(64.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFFEFF6FF)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji, fontSize = 24.sp)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = friend.name.ifBlank { "Friend" },
            style = MaterialTheme.typography.bodySmall,
            color = DobiTextPrimary,
            fontWeight = SFProMedium,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

// ── Parent tasks — empty state ─────────────────────────────────────────────────

@Composable
private fun ParentTasksEmptyCard(onClick: () -> Unit = {}) {
    val warmTop = Color(0xFFFFF4E0)   // soft peach
    val warmMid = Color(0xFFFFE8F1)   // soft pink
    val warmEnd = Color(0xFFEEE8FF)   // soft lavender
    val ctaAccent = Color(0xFFE67E22) // warm reward orange

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = ctaAccent.copy(alpha = 0.18f),
                ambientColor = Color(0xFFB48BFF).copy(alpha = 0.08f)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(warmTop, warmMid, warmEnd)
                )
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.6f),
                shape = RoundedCornerShape(24.dp)
            )
            .clickable(onClick = onClick)
    ) {
        // Decorative sparkles scattered around
        Text(
            text = "✨",
            fontSize = 18.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 18.dp, top = 14.dp)
        )
        Text(
            text = "⭐",
            fontSize = 14.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 22.dp, top = 28.dp)
        )
        Text(
            text = "✨",
            fontSize = 14.sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 26.dp, bottom = 22.dp)
        )
        Text(
            text = "💫",
            fontSize = 16.sp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 14.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Gift-icon bubble with ring
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.55f))
                )
                Box(
                    modifier = Modifier
                        .shadow(
                            elevation = 10.dp,
                            shape = CircleShape,
                            spotColor = ctaAccent.copy(alpha = 0.45f),
                            ambientColor = Color(0xFFFFB27A).copy(alpha = 0.3f)
                        )
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFFFD08A), Color(0xFFFF9F6B))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🎁", fontSize = 38.sp)
                }
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = "Confident with your studies?",
                fontSize = 17.sp,
                fontWeight = SFProBold,
                color = DobiTextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Ask your parents to give you a task and win an assured reward upon submission.",
                fontSize = 13.sp,
                color = DobiTextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp
            )

            Spacer(Modifier.height(18.dp))

            // Reward pill
            Row(
                modifier = Modifier
                    .shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(22.dp),
                        spotColor = ctaAccent.copy(alpha = 0.28f)
                    )
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White)
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "🏆", fontSize = 14.sp)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Earn your first reward",
                    fontSize = 12.sp,
                    fontWeight = SFProSemiBold,
                    color = ctaAccent
                )
            }
        }
    }
}

// ── Pending request card ───────────────────────────────────────────────────────
// Shown when the child has requested a task but the parent hasn't assigned
// it yet. After 12 h, a "Remind Parent" button becomes available.

private const val REMIND_THRESHOLD_MS = 12L * 60L * 60L * 1000L

@Composable
internal fun PendingRequestCard(
    task: ParentTask,
    isSendingReminder: Boolean,
    reminderSent: Boolean,
    onSendReminder: () -> Unit,
) {
    val elapsedMs = System.currentTimeMillis() - task.requestedAtMillis
    val canRemind = elapsedMs >= REMIND_THRESHOLD_MS

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFF)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFDBEAFE)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = task.subjectIcon.ifBlank { "📚" }, fontSize = 20.sp)
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Task requested to parent",
                        style = MaterialTheme.typography.titleSmall,
                        color = DobiTextPrimary,
                        fontWeight = SFProSemiBold
                    )
                    Text(
                        text = task.displayName.ifBlank { task.subject },
                        style = MaterialTheme.typography.bodySmall,
                        color = DobiTextSecondary
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "We've notified your parent. They'll assign a task for you soon.",
                style = MaterialTheme.typography.bodySmall,
                color = DobiTextSecondary,
                lineHeight = 18.sp
            )
            if (canRemind) {
                Spacer(Modifier.height(14.dp))
                if (reminderSent) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFDCFCE7))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "Reminder sent to parent!",
                            fontSize = 13.sp,
                            color = Color(0xFF15803D),
                            fontWeight = SFProSemiBold
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = onSendReminder,
                        enabled = !isSendingReminder,
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, DobiPrimary)
                    ) {
                        if (isSendingReminder) {
                            CircularProgressIndicator(
                                color = DobiPrimary,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = DobiPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Remind Parent",
                                color = DobiPrimary,
                                fontWeight = SFProSemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Pending parent-task bar ────────────────────────────────────────────────────
// Surfaced above the streak banner whenever the child has at least one
// ASSIGNED task from their parent. Tapping opens the most recent task; if
// multiple tasks are waiting, the bar shows the count and still opens the
// most recent (the child sees the others on the task screen).

@Composable
private fun PendingParentTaskBar(
    tasks: List<ParentTask>,
    onClick: (taskId: String) -> Unit,
) {
    if (tasks.isEmpty()) return
    val mostRecent = tasks.maxByOrNull {
        it.assignedAtMillis.takeIf { ts -> ts > 0L } ?: it.requestedAtMillis
    } ?: tasks.first()
    val moreCount = (tasks.size - 1).coerceAtLeast(0)

    val accentBg = Color(0xFFFFF4E0)
    val accentFg = Color(0xFFB45309)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 2.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Tasks needing your attention",
                    fontSize = 15.sp,
                    fontWeight = SFProBold,
                    color = DobiTextPrimary,
                    modifier = Modifier.weight(1f),
                )
                if (moreCount > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onClick(mostRecent.id) },
                            )
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = "View all",
                            fontSize = 13.sp,
                            fontWeight = SFProSemiBold,
                            color = DobiPrimary,
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = DobiPrimary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onClick(mostRecent.id) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(accentBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = mostRecent.subjectIcon.ifBlank { "📚" },
                        fontSize = 24.sp,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = parentTaskTitle(mostRecent),
                        color = DobiTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = SFProBold,
                        lineHeight = 18.sp,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = buildString {
                            append(if (mostRecent.isUnitScope) "Lesson" else "Concept")
                            if (mostRecent.subject.isNotBlank()) append(" · ${mostRecent.subject}")
                        },
                        color = DobiTextSecondary,
                        fontSize = 12.sp,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentBg)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = "New task",
                        fontSize = 11.sp,
                        fontWeight = SFProSemiBold,
                        color = accentFg,
                    )
                }
            }
        }
    }
}

// ── Parent tasks — summary banner (collapsed default) ─────────────────────────
// Shows the most-recent active task with a "+N more" badge. Tapping the card
// opens that task; the "See all" link in the section header expands the full list.

@Composable
private fun ParentTasksSummaryBanner(
    tasks: List<ParentTask>,
    onSeeAll: () -> Unit,
    onTaskClick: (String) -> Unit,
) {
    val mostRecent = tasks.first()
    val moreCount = tasks.size - 1

    // Count tasks still needing action
    val actionNeeded = tasks.count {
        it.status == ParentTaskStatus.ASSIGNED || it.status == ParentTaskStatus.REJECTED
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color(0x14000000)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onTaskClick(mostRecent.id) }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFF1E0)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mostRecent.subjectIcon.ifBlank { "🎯" },
                        fontSize = 20.sp
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = parentTaskTitle(mostRecent),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = SFProSemiBold,
                        color = DobiTextPrimary,
                        maxLines = 2
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = mostRecent.subject.ifBlank { "Task" },
                        style = MaterialTheme.typography.bodySmall,
                        color = DobiTextSecondary
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    ParentTaskStatusBadge(status = mostRecent.status)
                    if (moreCount > 0) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "+$moreCount more",
                            fontSize = 10.sp,
                            fontWeight = SFProSemiBold,
                            color = DobiPrimary
                        )
                    }
                }
            }

            if (tasks.size > 1) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (actionNeeded > 0) Color(0xFFFFF7E6) else Color(0xFFF8FAFF))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onSeeAll
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (actionNeeded > 0)
                            "⚡ $actionNeeded task${if (actionNeeded > 1) "s" else ""} need${if (actionNeeded == 1) "s" else ""} your attention"
                        else
                            "✓ All ${tasks.size} tasks completed",
                        fontSize = 12.sp,
                        fontWeight = SFProSemiBold,
                        color = if (actionNeeded > 0) Color(0xFFB45309) else Color(0xFF15803D),
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = if (actionNeeded > 0) Color(0xFFB45309) else Color(0xFF15803D),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ── Parent task card ───────────────────────────────────────────────────────────

@Composable
internal fun ParentTaskCard(task: ParentTask, onClick: () -> Unit) {
    val accent = Color(0xFFE67E22)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color(0x14000000)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFF1E0)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🎯", fontSize = 20.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = parentTaskTitle(task),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = SFProSemiBold,
                        color = DobiTextPrimary
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = parentTaskDescription(task),
                        style = MaterialTheme.typography.bodySmall,
                        color = DobiTextSecondary,
                        lineHeight = 16.sp,
                        maxLines = 2
                    )
                }
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = DobiTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (task.subject.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFFF4E0))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (task.subjectIcon.isNotBlank()) {
                            Text(text = task.subjectIcon, fontSize = 11.sp)
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            text = task.subject,
                            fontSize = 11.sp,
                            fontWeight = SFProSemiBold,
                            color = accent
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                ParentTaskStatusBadge(status = task.status)
            }
        }
    }
}

@Composable
internal fun RequestNextTaskRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFFFF7E6))
            .border(1.dp, Color(0xFFFEEAB6), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "🎁", fontSize = 18.sp)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Request your next task",
                color = DobiTextPrimary,
                fontSize = 13.sp,
                fontWeight = SFProSemiBold
            )
            Text(
                text = "Pick another lesson and earn another reward.",
                color = DobiTextSecondary,
                fontSize = 11.sp
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFFE67E22),
            modifier = Modifier.size(20.dp)
        )
    }
}

private fun parentTaskTitle(task: ParentTask): String {
    val scope = task.displayName.ifBlank { "your topic" }
    return when (task.status) {
        ParentTaskStatus.REQUESTED -> "Waiting for parent: $scope"
        else -> when (task.type) {
            ParentTaskType.REVISE_CHAPTER -> "Revise: $scope"
            ParentTaskType.WRITE_QA -> "Write Q&A: $scope"
            ParentTaskType.OUR_QUESTIONS -> "${task.mcqCount} questions: $scope"
            ParentTaskType.CUSTOM -> "Task on: $scope"
            null -> scope
        }
    }
}

private fun parentTaskDescription(task: ParentTask): String = when (task.status) {
    ParentTaskStatus.REQUESTED -> "We've sent your topic to your parents. Hang tight!"
    ParentTaskStatus.PENDING_REVIEW -> "Submitted. Waiting for parent to review."
    ParentTaskStatus.ACCEPTED -> task.parentReviewNote.ifBlank { "Great job — your parent accepted it." }
    ParentTaskStatus.REJECTED -> task.parentReviewNote.ifBlank {
        "Your parent asked you to redo this. Try again."
    }
    ParentTaskStatus.ASSIGNED -> when (task.type) {
        ParentTaskType.REVISE_CHAPTER -> "Revise the chapter and upload your revision photos."
        ParentTaskType.WRITE_QA -> "Write Q&A from your textbook and upload the photo."
        ParentTaskType.OUR_QUESTIONS -> "Answer ${task.mcqCount} quick questions on this topic."
        ParentTaskType.CUSTOM -> task.customNote.ifBlank { "Open to see what your parent wrote." }
        null -> "Open to start"
    }
}

@Composable
private fun ParentTaskStatusBadge(status: ParentTaskStatus) {
    val (bg, fg, label) = when (status) {
        ParentTaskStatus.REQUESTED      -> Triple(Color(0xFFF1F5F9), DobiTextSecondary, "Waiting…")
        ParentTaskStatus.ASSIGNED       -> Triple(Color(0xFFDBEAFE), DobiPrimary, "To do")
        ParentTaskStatus.PENDING_REVIEW -> Triple(Color(0xFFFEF3C7), Color(0xFFB45309), "Submitted")
        ParentTaskStatus.ACCEPTED       -> Triple(Color(0xFFDCFCE7), Color(0xFF15803D), "✓  Done")
        ParentTaskStatus.REJECTED       -> Triple(Color(0xFFFEE2E2), Color(0xFFB91C1C), "Redo")
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = fg,
            fontWeight = SFProMedium
        )
    }
}

// ── Tab placeholder ────────────────────────────────────────────────────────────

@Composable
private fun TabPlaceholder(label: String, padding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DobiBackground)
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$label — Coming Soon",
            style = MaterialTheme.typography.bodyLarge,
            color = DobiTextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

// ── Preview ────────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true, name = "Empty tasks")
@Composable
private fun ChildHomeEmptyPreview() {
    DobiTheme {
        ChildHomeContent(
            uiState = ChildHomeUiState(
                childName = "Emma",
                streakDays = 3,
                parentTasks = emptyList()
            ),
            onStartLearning = {},
            onNavigateToPractiseLesson = {},
            onNavigateToHomeworkLog = {},
            onNavigateToClassTestPrep = {},
            onNavigateToTuitionWork = {},
            onNavigateToTuitionHomework = {},
            onNavigateToTuitionProblems = {},
            onNavigateToRevisitTopics = {},
            onNavigateToNeedRevision = {},
            onNavigateToTestYourself = {},
            onNavigateToStreakDashboard = {},
            onNavigateToInviteFriend = {},
            onNavigateToEarnReward = {},
            onNavigateToParentTasksList = {},
            onSaveTutor = { _, _ -> },
            onTaskClick = {},
            selectedModeIndex = null,
            onModeSelected = {},
            contentPadding = PaddingValues(0.dp)
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "With tasks")
@Composable
private fun ChildHomeWithTasksPreview() {
    DobiTheme {
        ChildHomeContent(
            uiState = ChildHomeUiState(
                childName = "Emma",
                streakDays = 3,
                parentTasks = listOf(
                    ParentTask(
                        id = "1",
                        unitName = "Light, Shadows and Reflections",
                        isUnitScope = true,
                        subject = "Science",
                        subjectIcon = "🔬",
                        type = ParentTaskType.OUR_QUESTIONS,
                        mcqCount = 10,
                        status = ParentTaskStatus.ASSIGNED
                    ),
                    ParentTask(
                        id = "2",
                        unitName = "Fractions",
                        conceptName = "Equivalent Fractions",
                        isUnitScope = false,
                        subject = "Math",
                        subjectIcon = "🧮",
                        type = ParentTaskType.REVISE_CHAPTER,
                        status = ParentTaskStatus.PENDING_REVIEW
                    )
                )
            ),
            onStartLearning = {},
            onNavigateToPractiseLesson = {},
            onNavigateToHomeworkLog = {},
            onNavigateToClassTestPrep = {},
            onNavigateToTuitionWork = {},
            onNavigateToTuitionHomework = {},
            onNavigateToTuitionProblems = {},
            onNavigateToRevisitTopics = {},
            onNavigateToNeedRevision = {},
            onNavigateToTestYourself = {},
            onNavigateToStreakDashboard = {},
            onNavigateToInviteFriend = {},
            onNavigateToEarnReward = {},
            onNavigateToParentTasksList = {},
            onSaveTutor = { _, _ -> },
            onTaskClick = {},
            selectedModeIndex = null,
            onModeSelected = {},
            contentPadding = PaddingValues(0.dp)
        )
    }
}
