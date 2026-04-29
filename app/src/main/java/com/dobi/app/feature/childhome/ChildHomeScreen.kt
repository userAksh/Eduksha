package com.dobi.app.feature.childhome

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    onRequestParentAccess: () -> Unit = {},
    viewModel: ChildHomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    // Lock back button — child cannot return to parent view without the PIN gate
    androidx.activity.compose.BackHandler(enabled = true) {
        onRequestParentAccess()
    }

    Scaffold(
        containerColor = DobiBackground,
        bottomBar = {
            DobiBottomNavBar(
                items = childNavItems,
                selectedIndex = selectedTab,
                onItemSelected = { selectedTab = it }
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = {},
                    containerColor = DobiPrimary,
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Log session", tint = Color.White)
                }
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
                onSaveTutor = viewModel::saveTutor,
                onTaskClick = {},
                contentPadding = innerPadding
            )
            1 -> MyLearningsContent(contentPadding = innerPadding)
            2 -> TabPlaceholder("Profile", innerPadding)
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
    onSaveTutor: (String, String) -> Unit,
    onTaskClick: (String) -> Unit,
    contentPadding: PaddingValues
) {
    var selectedModeIndex by remember { mutableStateOf<Int?>(null) }
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

            // Streak banner
            item {
                StreakBanner(streakDays = uiState.streakDays)
                Spacer(Modifier.height(20.dp))
            }

            // Revision mode cards
            item {
                RevisionModeRow(onRevisionModeClick = { lastModeIndex = it; selectedModeIndex = it })
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
                            "See all",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DobiPrimary,
                            fontWeight = SFProMedium
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Tasks content
            if (uiState.parentTasks.isEmpty()) {
                item {
                    ParentTasksEmptyCard()
                }
            } else {
                items(uiState.parentTasks, key = { it.id }) { task ->
                    ParentTaskCard(task = task, onClick = { onTaskClick(task.id) })
                    Spacer(Modifier.height(12.dp))
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
                    ) { selectedModeIndex = null }
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
                onDismiss = { selectedModeIndex = null },
                onOptionClick = { optionIndex ->
                    selectedModeIndex = null
                    when {
                        lastModeIndex == 0 && optionIndex == 0 -> onNavigateToPractiseLesson()
                        lastModeIndex == 0 && optionIndex == 1 -> onNavigateToHomeworkLog()
                        lastModeIndex == 0 && optionIndex == 2 -> onNavigateToClassTestPrep()
                        lastModeIndex == 1 && optionIndex == 0 -> onNavigateToTuitionWork()
                        lastModeIndex == 1 && optionIndex == 1 -> onNavigateToTuitionHomework()
                        lastModeIndex == 1 && optionIndex == 2 -> onNavigateToTuitionProblems()
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
            RevisionOption("🌱", "Explore a New Concept", "Pick something fresh and learn it today"),
            RevisionOption("🔁", "Revisit Past Topics", "Go back and strengthen what you've studied"),
            RevisionOption("🧠", "Test Yourself", "Quiz yourself to see how much you remember"),
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
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
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
private fun StreakBanner(streakDays: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DobiPrimary),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🔥", fontSize = 26.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "$streakDays days",
                        fontSize = 28.sp,
                        fontWeight = SFProBold,
                        color = Color.White
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "You're on a streak!",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "😊", fontSize = 28.sp)
            }
        }
    }
}

// ── Parent tasks — empty state ─────────────────────────────────────────────────

@Composable
private fun ParentTasksEmptyCard() {
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

// ── Parent task card ───────────────────────────────────────────────────────────

@Composable
private fun ParentTaskCard(task: ParentTask, onClick: () -> Unit) {
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
                        text = task.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = SFProSemiBold,
                        color = DobiTextPrimary
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = task.description,
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
                // Reward chip
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFF4E0))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🏆", fontSize = 11.sp)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = task.reward,
                        fontSize = 11.sp,
                        fontWeight = SFProSemiBold,
                        color = accent
                    )
                }

                if (task.dueLabel != null) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "• ${task.dueLabel}",
                        fontSize = 11.sp,
                        color = DobiTextSecondary,
                        fontWeight = SFProMedium
                    )
                }

                Spacer(Modifier.weight(1f))

                ParentTaskStatusBadge(status = task.status)
            }
        }
    }
}

@Composable
private fun ParentTaskStatusBadge(status: ParentTaskStatus) {
    val (bg, fg, label) = when (status) {
        ParentTaskStatus.PENDING    -> Triple(Color(0xFFF1F5F9), DobiTextSecondary, "New")
        ParentTaskStatus.IN_PROGRESS -> Triple(Color(0xFFDBEAFE), DobiPrimary, "In Progress")
        ParentTaskStatus.SUBMITTED  -> Triple(Color(0xFFFEF3C7), Color(0xFFB45309), "Submitted")
        ParentTaskStatus.COMPLETED  -> Triple(Color(0xFFDCFCE7), Color(0xFF15803D), "✓  Done")
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
            onSaveTutor = { _, _ -> },
            onTaskClick = {},
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
                        title = "Solve 20 word problems on fractions",
                        description = "Use Chapter 4 examples. Show your working.",
                        reward = "₹100",
                        dueLabel = "Due Sunday",
                        status = ParentTaskStatus.PENDING
                    ),
                    ParentTask(
                        id = "2",
                        title = "Write a paragraph on the water cycle",
                        description = "150 words, in your own language.",
                        reward = "30 min screen time",
                        dueLabel = "Due today",
                        status = ParentTaskStatus.IN_PROGRESS
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
            onSaveTutor = { _, _ -> },
            onTaskClick = {},
            contentPadding = PaddingValues(0.dp)
        )
    }
}
