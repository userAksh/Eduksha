package com.dobi.app.feature.unitdetail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dobi.app.core.ui.theme.DobiBackground
import com.dobi.app.core.ui.theme.SFProBold
import com.dobi.app.core.ui.theme.SFProMedium
import com.dobi.app.core.ui.theme.SFProSemiBold
import com.dobi.app.core.ui.theme.DobiError
import com.dobi.app.core.ui.theme.DobiErrorContainer
import com.dobi.app.core.ui.theme.DobiPrimary
import com.dobi.app.core.ui.theme.DobiPrimaryContainer
import com.dobi.app.core.ui.theme.DobiSuccess
import com.dobi.app.core.ui.theme.DobiSuccessContainer
import com.dobi.app.core.ui.theme.DobiTextPrimary
import com.dobi.app.core.ui.theme.DobiTextSecondary
import com.dobi.app.core.ui.theme.DobiWarning
import com.dobi.app.core.ui.theme.DobiWarningContainer
import com.dobi.app.domain.model.Difficulty
import com.dobi.app.domain.model.WorkMode

// ── Entry point ────────────────────────────────────────────────────────────────

@Composable
fun UnitDetailScreen(
    onNavigateBack: () -> Unit,
    onUploadWork: (kindKey: String) -> Unit,
    onAnswerQuestions: () -> Unit,
    viewModel: UnitDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.toastMessage) {
        val msg = uiState.toastMessage ?: return@LaunchedEffect
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        viewModel.onToastShown()
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onNavigateBack()
    }

    Scaffold(
        containerColor = DobiBackground,
        topBar = { UnitDetailTopBar(onNavigateBack = onNavigateBack) }
    ) { innerPadding ->
        when {
            uiState.isLoading -> Box(
                Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = DobiPrimary) }

            uiState.error != null || uiState.concepts.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { Text(uiState.error ?: "Lesson not found", color = DobiTextSecondary) }

            else -> UnitDetailContent(
                uiState = uiState,
                onWorkModeSelected = viewModel::onWorkModeSelected,
                onDifficultySelected = viewModel::onDifficultySelected,
                onConceptCoverageToggled = viewModel::onConceptCoverageToggled,
                onUploadWork = onUploadWork,
                onAnswerQuestions = onAnswerQuestions,
                onMarkAsDone = viewModel::onMarkAsDone,
                onToggleRevision = viewModel::onToggleRevisionMark,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

// ── Top bar ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitDetailTopBar(onNavigateBack: () -> Unit) {
    TopAppBar(
        title = {},
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = DobiTextPrimary)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = DobiBackground)
    )
}

// ── Main content ───────────────────────────────────────────────────────────────

@Composable
private fun UnitDetailContent(
    uiState: UnitDetailUiState,
    onWorkModeSelected: (WorkMode) -> Unit,
    onDifficultySelected: (Difficulty) -> Unit,
    onConceptCoverageToggled: (String) -> Unit,
    onUploadWork: (kindKey: String) -> Unit,
    onAnswerQuestions: () -> Unit,
    onMarkAsDone: () -> Unit,
    onToggleRevision: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        // "Lesson" badge to distinguish from concept-level detail
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(DobiPrimaryContainer)
                .padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "📘", fontSize = 12.sp)
            Spacer(Modifier.size(4.dp))
            Text(
                text = "Lesson",
                fontSize = 11.sp,
                color = DobiPrimary,
                fontWeight = SFProSemiBold
            )
        }

        Spacer(Modifier.height(10.dp))

        // Unit title
        Text(
            text = uiState.unitName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = SFProBold,
            color = DobiTextPrimary
        )

        Spacer(Modifier.height(10.dp))

        // Subject breadcrumb
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BreadcrumbChip(label = uiState.subject)
            BreadcrumbChip(label = "${uiState.concepts.size} concepts")
        }

        Spacer(Modifier.height(16.dp))

        com.dobi.app.feature.conceptdetail.RevisionToggle(
            isMarked = uiState.isMarkedForRevision,
            isBusy = uiState.isTogglingRevision,
            onClick = onToggleRevision
        )

        Spacer(Modifier.height(20.dp))

        // Concepts covered in this lesson
        ConceptsInLessonCard(
            concepts = uiState.concepts,
            coveredConceptIds = uiState.coveredConceptIds,
            onConceptToggled = onConceptCoverageToggled
        )

        Spacer(Modifier.height(28.dp))

        // Section 1 — Work mode
        SectionLabel(number = "1", title = "Show us how you studied this lesson")
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WorkModeCard(
                icon = Icons.Default.School,
                title = "Upload Class Work",
                subtitle = "Notes I made\nin class",
                selected = uiState.selectedWorkMode == WorkMode.CLASS_WORK,
                onClick = { onUploadWork("class_work") },
                modifier = Modifier.weight(1f)
            )
            WorkModeCard(
                icon = Icons.Default.AutoStories,
                title = "Upload Self Learning",
                subtitle = "Notes I made\non my own",
                selected = uiState.selectedWorkMode == WorkMode.SELF_LEARNING,
                onClick = { onUploadWork("self_learning") },
                modifier = Modifier.weight(1f)
            )
            WorkModeCard(
                icon = Icons.AutoMirrored.Filled.HelpOutline,
                title = "Answer Questions",
                subtitle = "I practiced\nwith questions",
                selected = uiState.selectedWorkMode == WorkMode.QUESTIONS,
                onClick = onAnswerQuestions,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(28.dp))

        // Section 2 — Difficulty
        SectionLabel(number = "2", title = "How difficult was the whole lesson?")
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DifficultyCard(
                emoji = "😊",
                label = "Easy",
                description = "I understood\nit well",
                selected = uiState.selectedDifficulty == Difficulty.EASY,
                selectedColor = DobiSuccess,
                selectedContainerColor = DobiSuccessContainer,
                onClick = { onDifficultySelected(Difficulty.EASY) },
                modifier = Modifier.weight(1f)
            )
            DifficultyCard(
                emoji = "😐",
                label = "OK",
                description = "It was okay,\nsome doubts",
                selected = uiState.selectedDifficulty == Difficulty.OK,
                selectedColor = DobiWarning,
                selectedContainerColor = DobiWarningContainer,
                onClick = { onDifficultySelected(Difficulty.OK) },
                modifier = Modifier.weight(1f)
            )
            DifficultyCard(
                emoji = "😓",
                label = "Hard",
                description = "I found it\ndifficult",
                selected = uiState.selectedDifficulty == Difficulty.HARD,
                selectedColor = DobiError,
                selectedContainerColor = DobiErrorContainer,
                onClick = { onDifficultySelected(Difficulty.HARD) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onMarkAsDone,
            enabled = uiState.canMarkAsDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = DobiPrimary,
                disabledContainerColor = Color(0xFFBFD7F7)
            )
        ) {
            if (uiState.isSaving) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(
                    "Mark Lesson as Done",
                    fontWeight = SFProSemiBold,
                    fontSize = 15.sp,
                    color = Color.White
                )
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

// ── Concepts in lesson card ────────────────────────────────────────────────────

@Composable
private fun ConceptsInLessonCard(
    concepts: List<com.dobi.app.domain.model.Concept>,
    coveredConceptIds: Set<String>,
    onConceptToggled: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFF)),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, Color(0xFFE5EBF5))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "What's covered",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = SFProSemiBold,
                color = DobiTextPrimary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Toggle the concepts you actually studied.",
                style = MaterialTheme.typography.bodySmall,
                color = DobiTextSecondary
            )
            Spacer(Modifier.height(12.dp))

            val showCoachMark = coveredConceptIds.isEmpty() && concepts.isNotEmpty()
            AnimatedVisibility(
                visible = showCoachMark,
                enter = fadeIn(),
                exit = fadeOut()
            ) { CoachTooltip() }

            concepts.forEachIndexed { index, concept ->
                val checked = concept.id in coveredConceptIds
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onConceptToggled(concept.id) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DobiToggle(checked = checked, pulse = showCoachMark && index == 0)
                    Spacer(Modifier.size(12.dp))
                    Text(
                        text = concept.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (checked) DobiTextPrimary else DobiTextSecondary
                    )
                }
            }
        }
    }
}

// ── Coach mark tooltip ─────────────────────────────────────────────────────────

@Composable
private fun CoachTooltip() {
    val infinite = rememberInfiniteTransition(label = "coach")
    val bob by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bob"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = bob.dp)
            .padding(bottom = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(DobiPrimary)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = "✨ Tap a toggle to mark what you covered",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = SFProSemiBold
            )
        }
        Box(
            modifier = Modifier
                .padding(start = 14.dp)
                .offset(y = (-5).dp)
                .size(10.dp)
                .rotate(45f)
                .background(DobiPrimary)
        )
    }
}

// ── Custom themed toggle ───────────────────────────────────────────────────────

@Composable
private fun DobiToggle(checked: Boolean, pulse: Boolean = false) {
    val trackWidth = 36.dp
    val trackHeight = 22.dp
    val thumbSize = 18.dp
    val padding = 2.dp

    val trackColor by animateColorAsState(
        targetValue = if (checked) DobiPrimary else Color(0xFFD8DFEA),
        label = "track"
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) trackWidth - thumbSize - padding else padding,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "thumb"
    )

    val infinite = rememberInfiniteTransition(label = "pulse")
    val haloScale by infinite.animateFloat(
        initialValue = 1f,
        targetValue = if (pulse) 1.45f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "halo-scale"
    )
    val haloAlpha by infinite.animateFloat(
        initialValue = if (pulse) 0.55f else 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "halo-alpha"
    )

    Box(
        modifier = Modifier.size(width = trackWidth, height = trackHeight),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .scale(haloScale)
                .clip(RoundedCornerShape(50))
                .background(DobiPrimary.copy(alpha = haloAlpha))
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(50))
                .background(trackColor),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset)
                    .size(thumbSize)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White)
            )
        }
    }
}

// ── Section label ──────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(number: String, title: String) {
    Text(
        text = "$number. $title",
        style = MaterialTheme.typography.titleMedium,
        color = DobiTextPrimary
    )
    Text(
        text = "Choose one",
        style = MaterialTheme.typography.bodySmall,
        color = DobiTextSecondary
    )
}

// ── Breadcrumb chip ────────────────────────────────────────────────────────────

@Composable
private fun BreadcrumbChip(label: String) {
    SuggestionChip(
        onClick = {},
        label = { Text(label, fontSize = 12.sp, fontWeight = SFProMedium) },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = DobiPrimaryContainer,
            labelColor = DobiPrimary
        ),
        border = SuggestionChipDefaults.suggestionChipBorder(
            enabled = true,
            borderColor = Color.Transparent
        ),
        shape = RoundedCornerShape(8.dp)
    )
}

// ── Work mode card ─────────────────────────────────────────────────────────────

@Composable
private fun WorkModeCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val border = if (selected) BorderStroke(2.dp, DobiPrimary) else BorderStroke(1.dp, Color(0xFFE2E8F0))
    val bg = if (selected) DobiPrimaryContainer else Color.White

    Card(
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        border = border,
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) DobiPrimary.copy(0.12f) else Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) DobiPrimary else DobiTextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = SFProSemiBold,
                color = if (selected) DobiPrimary else DobiTextPrimary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = DobiTextSecondary,
                lineHeight = 16.sp
            )
        }
    }
}

// ── Difficulty card ────────────────────────────────────────────────────────────

@Composable
private fun DifficultyCard(
    emoji: String,
    label: String,
    description: String,
    selected: Boolean,
    selectedColor: Color,
    selectedContainerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val border = if (selected) BorderStroke(2.dp, selectedColor) else BorderStroke(1.dp, Color(0xFFE2E8F0))
    val bg = if (selected) selectedContainerColor else Color.White

    Card(
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        border = border,
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = emoji, fontSize = 28.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = SFProSemiBold,
                color = if (selected) selectedColor else DobiTextPrimary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = DobiTextSecondary,
                lineHeight = 15.sp
            )
        }
    }
}
