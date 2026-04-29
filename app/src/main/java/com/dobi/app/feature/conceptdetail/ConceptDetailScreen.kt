package com.dobi.app.feature.conceptdetail

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
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.HelpOutline
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dobi.app.core.ui.theme.DobiBackground
import com.dobi.app.core.ui.theme.SFProBold
import com.dobi.app.core.ui.theme.SFProSemiBold
import com.dobi.app.core.ui.theme.SFProMedium
import com.dobi.app.core.ui.theme.SFProRegular
import com.dobi.app.core.ui.theme.SFProLight
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
fun ConceptDetailScreen(
    onNavigateBack: () -> Unit,
    onAnswerQuestions: () -> Unit,
    onUploadWork: (kindKey: String) -> Unit,
    viewModel: ConceptDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Navigate back after saving
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onNavigateBack()
    }

    Scaffold(
        containerColor = DobiBackground,
        topBar = {
            ConceptDetailTopBar(onNavigateBack = onNavigateBack)
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> Box(
                Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = DobiPrimary) }

            uiState.concept == null -> Box(
                Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { Text("Concept not found", color = DobiTextSecondary) }

            else -> ConceptDetailContent(
                uiState = uiState,
                onWorkModeSelected = viewModel::onWorkModeSelected,
                onDifficultySelected = viewModel::onDifficultySelected,
                onAnswerQuestions = onAnswerQuestions,
                onUploadWork = onUploadWork,
                onMarkAsDone = viewModel::onMarkAsDone,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

// ── Top bar ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConceptDetailTopBar(onNavigateBack: () -> Unit) {
    TopAppBar(
        title = {},
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = DobiTextPrimary)
            }
        },
        actions = {
            IconButton(onClick = {}) {
                Icon(Icons.Default.BookmarkBorder, contentDescription = "Bookmark", tint = DobiTextPrimary)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = DobiBackground)
    )
}

// ── Main content ───────────────────────────────────────────────────────────────

@Composable
private fun ConceptDetailContent(
    uiState: ConceptDetailUiState,
    onWorkModeSelected: (WorkMode) -> Unit,
    onDifficultySelected: (Difficulty) -> Unit,
    onAnswerQuestions: () -> Unit,
    onUploadWork: (kindKey: String) -> Unit,
    onMarkAsDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val concept = uiState.concept ?: return

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        // Concept title
        Text(
            text = concept.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = SFProBold,
            color = DobiTextPrimary
        )

        Spacer(Modifier.height(10.dp))

        // Breadcrumb chips
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BreadcrumbChip(label = concept.subject)
            BreadcrumbChip(label = concept.unit)
        }

        Spacer(Modifier.height(16.dp))

        // Description
        Text(
            text = concept.description,
            style = MaterialTheme.typography.bodyMedium,
            color = DobiTextSecondary,
            lineHeight = 22.sp
        )

        Spacer(Modifier.height(28.dp))

        // Section 1 — Work mode
        SectionLabel(number = "1", title = "Show us how you studied this concept")
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
                icon = Icons.Default.HelpOutline,
                title = "Answer Questions",
                subtitle = "I practiced\nwith questions",
                selected = uiState.selectedWorkMode == WorkMode.QUESTIONS,
                onClick = onAnswerQuestions,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(28.dp))

        // Section 2 — Difficulty
        SectionLabel(number = "2", title = "How difficult was it?")
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

        // Mark as Done CTA
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
                    "Mark as Done",
                    fontWeight = SFProSemiBold,
                    fontSize = 15.sp,
                    color = Color.White
                )
            }
        }

        Spacer(Modifier.height(32.dp))
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
