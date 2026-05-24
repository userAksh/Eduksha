package com.dobi.app.feature.quiz

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dobi.app.core.ui.theme.DobiBackground
import com.dobi.app.core.ui.theme.DobiError
import com.dobi.app.core.ui.theme.DobiErrorContainer
import com.dobi.app.core.ui.theme.DobiPrimary
import com.dobi.app.core.ui.theme.DobiPrimaryContainer
import com.dobi.app.core.ui.theme.DobiSuccess
import com.dobi.app.core.ui.theme.DobiSuccessContainer
import com.dobi.app.core.ui.theme.DobiTextPrimary
import com.dobi.app.core.ui.theme.DobiWarning
import com.dobi.app.core.ui.theme.DobiTextSecondary
import com.dobi.app.core.ui.theme.SFProBold
import com.dobi.app.core.ui.theme.SFProMedium
import com.dobi.app.core.ui.theme.SFProSemiBold
import com.dobi.app.domain.model.McqQuestion

// ── Entry point ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    onNavigateBack: () -> Unit,
    onSnapSend: (displayName: String) -> Unit = {},
    viewModel: QuizViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = DobiBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (uiState.phase) {
                            QuizPhase.INTRO -> uiState.unitName.ifBlank { "Quick Quiz" }
                            QuizPhase.QUESTION -> "Quick Quiz"
                            QuizPhase.DONE -> "Quiz Result"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = SFProBold,
                        color = DobiTextPrimary,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = DobiTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DobiBackground)
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading || uiState.isGeneratingCurated -> Box(
                Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = DobiPrimary)
                    if (uiState.isGeneratingCurated) {
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = "Preparing your quiz…",
                            color = DobiTextPrimary,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Dobi is making 10 questions for this lesson.",
                            color = DobiTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            uiState.error != null && uiState.questions.isEmpty() -> Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("😕", fontSize = 36.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = uiState.error!!,
                    color = DobiTextPrimary,
                    fontSize = 14.sp
                )
            }

            uiState.questions.isEmpty() && !uiState.isScoreOnly -> EmptyBankState(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                onBack = onNavigateBack
            )

            uiState.phase == QuizPhase.INTRO -> QuizIntro(
                state = uiState,
                onStart = viewModel::startQuiz,
                modifier = Modifier.padding(innerPadding)
            )

            uiState.phase == QuizPhase.QUESTION -> QuizQuestion(
                state = uiState,
                onSelect = viewModel::selectOption,
                onNext = viewModel::next,
                onPrevious = viewModel::previous,
                modifier = Modifier.padding(innerPadding)
            )

            else -> QuizDone(
                state = uiState,
                onRetry = viewModel::retry,
                onDone = onNavigateBack,
                onAssignQuiz = viewModel::assignQuiz,
                onSnapSend = if (uiState.showSnapAndSend) {
                    {
                        viewModel.markSnapSent()
                        onSnapSend(uiState.unitName)
                    }
                } else null,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

// ── Intro phase ────────────────────────────────────────────────────────────────

@Composable
private fun QuizIntro(
    state: QuizUiState,
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))

            QuizTimePill()

            Spacer(Modifier.height(20.dp))

            Text(
                text = "You've learned the basics of",
                style = MaterialTheme.typography.bodyMedium,
                color = DobiTextSecondary,
                textAlign = TextAlign.Center
            )
            Text(
                text = state.unitName.ifBlank { "this lesson" },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = SFProBold,
                color = DobiTextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Let's see how much you remember!",
                style = MaterialTheme.typography.bodyMedium,
                color = DobiTextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(20.dp))

            StatChip(
                icon = Icons.Default.HelpOutline,
                label = "${state.totalQuestions} Questions"
            )

            Spacer(Modifier.height(28.dp))

            QuizRulesCard(totalQuestions = state.totalQuestions)

            Spacer(Modifier.height(24.dp))
        }

        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DobiPrimary)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "Start the Quiz",
                fontWeight = SFProSemiBold,
                fontSize = 15.sp,
                color = Color.White
            )
        }
    }
}

@Composable
private fun QuizTimePill() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(DobiPrimaryContainer)
            .border(2.dp, DobiPrimary, RoundedCornerShape(50))
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.PlaylistAddCheck,
            contentDescription = null,
            tint = DobiPrimary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "QUIZ TIME!",
            color = DobiPrimary,
            fontSize = 14.sp,
            fontWeight = SFProBold
        )
    }
}

@Composable
private fun StatChip(icon: ImageVector, label: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White)
            .border(1.5.dp, DobiPrimary, RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = DobiPrimary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            color = DobiPrimary,
            fontSize = 13.sp,
            fontWeight = SFProSemiBold
        )
    }
}

@Composable
private fun QuizRulesCard(totalQuestions: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, Color(0xFFE5EBF5))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DividerLine()
                Spacer(Modifier.width(12.dp))
                Text(
                    "QUIZ RULES",
                    color = DobiTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = SFProSemiBold
                )
                Spacer(Modifier.width(12.dp))
                DividerLine()
            }

            Spacer(Modifier.height(14.dp))

            val rules = listOf(
                "This quiz has $totalQuestions questions with 4 options each.",
                "Only one option is correct per question.",
                "You need at least 70% to pass.",
                "No time limit — take your time and think.",
                "If you don't pass, revisit the lesson and try again.",
                "Your best score will be recorded."
            )
            rules.forEach { rule ->
                RuleRow(text = rule)
            }
        }
    }
}

@Composable
private fun RuleRow(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 5.dp)
                .size(8.dp)
                .background(DobiPrimary, shape = DiamondShape)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = DobiTextPrimary,
            lineHeight = 20.sp
        )
    }
}

private val DiamondShape = androidx.compose.ui.graphics.RectangleShape
    .let {
        androidx.compose.foundation.shape.GenericShape { size, _ ->
            moveTo(size.width / 2f, 0f)
            lineTo(size.width, size.height / 2f)
            lineTo(size.width / 2f, size.height)
            lineTo(0f, size.height / 2f)
            close()
        }
    }

@Composable
private fun RowScope.DividerLine() {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(1.dp)
            .background(Color(0xFFE5EBF5))
    )
}

// ── Question phase ─────────────────────────────────────────────────────────────

@Composable
private fun QuizQuestion(
    state: QuizUiState,
    onSelect: (Int) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier
) {
    val q = state.currentQuestion ?: return

    Column(modifier = modifier.fillMaxSize()) {
        // Progress
        LinearProgressIndicator(
            progress = { (state.currentIndex + 1f) / state.totalQuestions },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = DobiPrimary,
            trackColor = Color(0xFFE5EBF5)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(20.dp))

            // Question header
            Text(
                text = "Question",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = SFProBold,
                color = DobiTextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = q.text,
                style = MaterialTheme.typography.bodyLarge,
                color = DobiTextPrimary,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "SELECT THE MOST APPROPRIATE ANSWER",
                style = MaterialTheme.typography.labelSmall,
                color = DobiTextSecondary,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))

            q.options.forEachIndexed { index, optionText ->
                OptionRow(
                    text = optionText,
                    selectionState = optionState(
                        index = index,
                        selectedIndex = state.currentSelection,
                        correctIndex = q.correctIndex,
                        revealed = state.isCurrentRevealed
                    ),
                    onClick = { onSelect(index) }
                )
                Spacer(Modifier.height(10.dp))
            }

            // Explanation only when revealed AND user answered wrong
            if (state.isCurrentRevealed && state.currentSelection != q.correctIndex) {
                Spacer(Modifier.height(8.dp))
                ExplanationCard(text = q.explanation)
            }

            Spacer(Modifier.height(20.dp))
        }

        QuizNavBar(
            counter = "${state.currentIndex + 1}/${state.totalQuestions}",
            showPrevious = state.canGoPrevious,
            nextEnabled = state.currentSelection != null,
            isLastReveal = state.isCurrentRevealed && state.isLastQuestion,
            onPrevious = onPrevious,
            onNext = onNext
        )
    }
}

private enum class OptionState { DEFAULT, SELECTED, CORRECT, WRONG }

private fun optionState(
    index: Int,
    selectedIndex: Int?,
    correctIndex: Int,
    revealed: Boolean
): OptionState = when {
    !revealed && index == selectedIndex -> OptionState.SELECTED
    revealed && index == correctIndex -> OptionState.CORRECT
    revealed && index == selectedIndex -> OptionState.WRONG
    else -> OptionState.DEFAULT
}

@Composable
private fun OptionRow(
    text: String,
    selectionState: OptionState,
    onClick: () -> Unit
) {
    val targetBg = when (selectionState) {
        OptionState.DEFAULT -> Color.White
        OptionState.SELECTED -> DobiPrimaryContainer
        OptionState.CORRECT -> DobiSuccessContainer
        OptionState.WRONG -> DobiErrorContainer
    }
    val targetBorder = when (selectionState) {
        OptionState.DEFAULT -> Color(0xFFE5EBF5)
        OptionState.SELECTED -> DobiPrimary
        OptionState.CORRECT -> DobiSuccess
        OptionState.WRONG -> DobiError
    }
    val targetTextColor = when (selectionState) {
        OptionState.DEFAULT -> DobiTextPrimary
        OptionState.SELECTED -> DobiTextPrimary
        OptionState.CORRECT -> DobiSuccess
        OptionState.WRONG -> DobiError
    }
    val bg by animateColorAsState(targetBg, label = "opt-bg")
    val border by animateColorAsState(targetBorder, label = "opt-border")
    val txt by animateColorAsState(targetTextColor, label = "opt-text")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.5.dp, border, RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OptionIndicator(state = selectionState, accent = border)
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selectionState == OptionState.DEFAULT) SFProMedium else SFProSemiBold,
            color = txt,
            lineHeight = 20.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun OptionIndicator(state: OptionState, accent: Color) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (state == OptionState.DEFAULT) Color.White else Color.White)
            .border(1.5.dp, accent, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        when (state) {
            OptionState.DEFAULT -> Unit
            OptionState.SELECTED, OptionState.CORRECT -> Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(16.dp)
            )
            OptionState.WRONG -> Icon(
                Icons.Default.Close,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun ExplanationCard(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DobiSuccessContainer)
            .border(1.dp, DobiSuccess.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(50))
                .background(DobiSuccess.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Lightbulb,
                contentDescription = null,
                tint = DobiSuccess,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = text.ifBlank { "Take a moment to think about why the correct answer fits best." },
            style = MaterialTheme.typography.bodySmall,
            color = DobiSuccess.copy(red = 0.05f, green = 0.45f, blue = 0.20f, alpha = 1f),
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuizNavBar(
    counter: String,
    showPrevious: Boolean,
    nextEnabled: Boolean,
    isLastReveal: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showPrevious) {
            NavArrowButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                enabled = true,
                onClick = onPrevious,
                modifier = Modifier.weight(1f)
            )
        }

        // Counter pill
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .border(1.5.dp, DobiPrimary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = counter,
                fontSize = 15.sp,
                fontWeight = SFProSemiBold,
                color = DobiTextPrimary
            )
        }

        NavArrowButton(
            icon = Icons.AutoMirrored.Filled.ArrowForward,
            enabled = nextEnabled,
            isFinal = isLastReveal,
            onClick = onNext,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun NavArrowButton(
    icon: ImageVector,
    enabled: Boolean,
    isFinal: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg by animateColorAsState(
        targetValue = when {
            !enabled -> Color(0xFFE5EBF5)
            isFinal -> DobiSuccess
            else -> DobiPrimary
        },
        label = "nav-bg"
    )
    val fg = if (enabled) Color.White else DobiTextSecondary

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isFinal) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Finish", color = fg, fontWeight = SFProSemiBold, fontSize = 14.sp)
                Spacer(Modifier.width(6.dp))
                Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
            }
        } else {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
        }
    }
}

// ── Done phase ─────────────────────────────────────────────────────────────────

@Composable
private fun QuizDone(
    state: QuizUiState,
    onRetry: () -> Unit,
    onDone: () -> Unit,
    onAssignQuiz: () -> Unit = {},
    onSnapSend: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val attempted = if (state.isScoreOnly) state.scoreOnlyTotal else state.selections.size
    val total = state.totalQuestions
    val percent = if (total == 0) 0f else state.score.toFloat() / total
    val percentInt = (percent * 100).toInt()
    val scoreColor = when {
        percent >= 0.7f -> DobiSuccess
        percent >= 0.4f -> DobiWarning
        else -> DobiError
    }

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            ResultBanner(unitName = state.unitName)

            Spacer(Modifier.height(14.dp))

            AttemptedPill(attempted = attempted, total = total)

            Spacer(Modifier.height(20.dp))

            ResultDivider()

            Spacer(Modifier.height(16.dp))

            ScoreCard(
                percentInt = percentInt,
                correct = state.score,
                total = total,
                scoreColor = scoreColor
            )

            Spacer(Modifier.height(20.dp))

            if (state.isScoreOnly) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Question-by-question breakdown is only available on the student's device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = DobiTextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                )
            } else {
                state.questions.forEachIndexed { index, question ->
                    QuestionReviewCard(
                        number = index + 1,
                        question = question,
                        userPick = state.selections[index]
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }

            Spacer(Modifier.height(8.dp))
        }

        ResultActionBar(
            canRetry = state.canRetry,
            isParentView = state.isParentView,
            isAssigningQuiz = state.isAssigningQuiz,
            quizAssigned = state.quizAssigned,
            quizAlreadyAssigned = state.quizAlreadyAssigned,
            onRetry = onRetry,
            onDone = onDone,
            onAssignQuiz = onAssignQuiz,
            onSnapSend = onSnapSend
        )
    }
}

@Composable
private fun ResultBanner(unitName: String) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF7AB8F5),
                            Color(0xFFB78CF0),
                            Color(0xFFF59CA1),
                            Color(0xFFF5B36A)
                        )
                    )
                )
                .padding(horizontal = 20.dp, vertical = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("👍", fontSize = 22.sp)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Thank you for taking the Quiz",
                    color = Color.White,
                    fontWeight = SFProBold,
                    fontSize = 17.sp
                )
            }
        }

        if (unitName.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "You've practised the fundamentals of $unitName. Here's how you did.",
                style = MaterialTheme.typography.bodyMedium,
                color = DobiTextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun AttemptedPill(attempted: Int, total: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color.White)
                .border(1.5.dp, DobiPrimary.copy(alpha = 0.3f), RoundedCornerShape(50))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.HelpOutline,
                contentDescription = null,
                tint = DobiPrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Attempted Questions: $attempted / $total",
                color = DobiTextPrimary,
                fontWeight = SFProSemiBold,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun ResultDivider() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DividerLine()
        Spacer(Modifier.width(12.dp))
        Text(
            "RESULT",
            color = DobiTextSecondary,
            fontSize = 11.sp,
            fontWeight = SFProSemiBold
        )
        Spacer(Modifier.width(12.dp))
        DividerLine()
    }
}

@Composable
private fun ScoreCard(
    percentInt: Int,
    correct: Int,
    total: Int,
    scoreColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, Color(0xFFE5EBF5))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "SCORE",
                    color = DobiTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = SFProSemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "$percentInt%",
                    color = DobiTextPrimary,
                    fontSize = 36.sp,
                    fontWeight = SFProBold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "$correct / $total correct",
                    color = DobiTextSecondary,
                    fontSize = 13.sp,
                    fontWeight = SFProMedium
                )
            }

            ScoreRing(
                percent = percentInt / 100f,
                color = scoreColor,
                label = "$percentInt%"
            )
        }
    }
}

@Composable
private fun ScoreRing(percent: Float, color: Color, label: String) {
    Box(
        modifier = Modifier.size(78.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 8.dp.toPx()
            drawArc(
                color = Color(0xFFE5EBF5),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * percent.coerceIn(0f, 1f),
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Text(
            text = label,
            color = DobiTextPrimary,
            fontSize = 13.sp,
            fontWeight = SFProBold
        )
    }
}

@Composable
private fun QuestionReviewCard(
    number: Int,
    question: McqQuestion,
    userPick: Int?
) {
    val isCorrect = userPick != null && userPick == question.correctIndex

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, Color(0xFFE5EBF5))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Q$number. ${question.text}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = SFProSemiBold,
                color = DobiTextPrimary,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(12.dp))

            // Always show the correct option
            ReviewPill(
                optionText = question.options[question.correctIndex],
                label = "Correct",
                accent = DobiSuccess,
                muted = false
            )

            // If wrong, also show the user's pick
            if (!isCorrect && userPick != null) {
                Spacer(Modifier.height(8.dp))
                ReviewPill(
                    optionText = question.options[userPick],
                    label = "Your answer",
                    accent = DobiError,
                    muted = true
                )
            }

            // If skipped (shouldn't happen, but safety)
            if (userPick == null) {
                Spacer(Modifier.height(8.dp))
                ReviewPill(
                    optionText = "Not attempted",
                    label = "Your answer",
                    accent = Color(0xFF94A3B8),
                    muted = true
                )
            }
        }
    }
}

@Composable
private fun ReviewPill(
    optionText: String,
    label: String,
    accent: Color,
    muted: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (muted) Color(0xFFF8FAFC) else accent.copy(alpha = 0.06f))
            .border(1.5.dp, accent.copy(alpha = if (muted) 0.4f else 0.7f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = optionText,
            style = MaterialTheme.typography.bodyMedium,
            color = DobiTextPrimary,
            modifier = Modifier.weight(1f),
            lineHeight = 18.sp
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            color = accent,
            fontWeight = SFProSemiBold,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun ResultActionBar(
    canRetry: Boolean,
    isParentView: Boolean,
    isAssigningQuiz: Boolean,
    quizAssigned: Boolean,
    quizAlreadyAssigned: Boolean = false,
    onRetry: () -> Unit,
    onDone: () -> Unit,
    onAssignQuiz: () -> Unit,
    onSnapSend: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (isParentView) {
            when {
                quizAssigned -> {
                    // Success banner — parent just assigned in this session
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFDCFCE7))
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.TaskAlt,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                "Quiz assigned!",
                                fontWeight = SFProSemiBold,
                                fontSize = 14.sp,
                                color = Color(0xFF15803D)
                            )
                            Text(
                                "Your child will see it in their tasks.",
                                fontSize = 12.sp,
                                color = Color(0xFF166534)
                            )
                        }
                    }
                }
                quizAlreadyAssigned -> {
                    // Disabled — quiz already pending/in-progress for this child
                    OutlinedButton(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.5.dp, DobiTextSecondary.copy(alpha = 0.4f))
                    ) {
                        Icon(
                            Icons.Default.AssignmentTurnedIn,
                            contentDescription = null,
                            tint = DobiTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Already assigned this quiz to child",
                            color = DobiTextSecondary,
                            fontWeight = SFProSemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
                else -> {
                    OutlinedButton(
                        onClick = onAssignQuiz,
                        enabled = !isAssigningQuiz,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.5.dp, DobiPrimary)
                    ) {
                        if (isAssigningQuiz) {
                            CircularProgressIndicator(
                                color = DobiPrimary,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.AssignmentTurnedIn,
                                contentDescription = null,
                                tint = DobiPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Assign This Quiz to Child",
                                color = DobiPrimary,
                                fontWeight = SFProSemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        } else {
            if (canRetry) {
                OutlinedButton(
                    onClick = onRetry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, DobiPrimary)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        tint = DobiPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Retake Quiz", color = DobiPrimary, fontWeight = SFProSemiBold, fontSize = 14.sp)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
        Button(
            onClick = onSnapSend ?: onDone,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DobiPrimary)
        ) {
            Text(
                text = if (onSnapSend != null) "Snap & Send" else "Done",
                color = Color.White,
                fontWeight = SFProSemiBold,
                fontSize = 15.sp
            )
        }
        }
    }
}

// ── Empty state (no curated set yet) ───────────────────────────────────────────

@Composable
private fun EmptyBankState(modifier: Modifier = Modifier, onBack: () -> Unit) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(50))
                .background(DobiPrimaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.PlaylistAddCheck,
                contentDescription = null,
                tint = DobiPrimary,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            "Curated set coming soon",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = SFProBold,
            color = DobiTextPrimary
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "We're still preparing questions for this lesson. Try the Upload Your Pages option in the meantime!",
            style = MaterialTheme.typography.bodyMedium,
            color = DobiTextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(Modifier.height(20.dp))
        OutlinedButton(
            onClick = onBack,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.5.dp, DobiPrimary)
        ) {
            Text("Go back", color = DobiPrimary, fontWeight = SFProSemiBold)
        }
    }
}

