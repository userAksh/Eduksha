package com.dobi.app.feature.testyourself

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dobi.app.core.ui.theme.DobiBackground
import com.dobi.app.core.ui.theme.DobiPrimary
import com.dobi.app.core.ui.theme.DobiTextPrimary
import com.dobi.app.core.ui.theme.DobiTextSecondary
import com.dobi.app.core.ui.theme.SFProBold
import com.dobi.app.core.ui.theme.SFProSemiBold

const val FORMAT_MCQ = "mcq"
const val FORMAT_SUBJECTIVE = "subjective"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestYourselfFormatScreen(
    onNavigateBack: () -> Unit = {},
    onPickFormat: (source: String, format: String, unitId: String, conceptId: String) -> Unit =
        { _, _, _, _ -> },
    viewModel: TestYourselfFormatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        containerColor = DobiBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Pick a format",
                        style = MaterialTheme.typography.titleLarge,
                        color = DobiTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
            uiState.isLoading -> Box(
                Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = DobiPrimary) }

            uiState.error != null -> Box(
                Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { Text(uiState.error!!, color = DobiTextSecondary) }

            else -> Content(
                state = uiState,
                onPickFormat = { fmt ->
                    if (fmt == FORMAT_SUBJECTIVE) {
                        // Subjective reuses the WriteAnswer pipeline from
                        // Class Test Prep, which keys off AiPracticeContextHolder.
                        // Populate it with the picked lesson/concept first.
                        coroutineScope.launch {
                            val ok = viewModel.prepareSubjectiveContext()
                            if (ok) onPickFormat(
                                uiState.source, fmt, uiState.unitId, uiState.conceptId
                            )
                        }
                    } else {
                        onPickFormat(uiState.source, fmt, uiState.unitId, uiState.conceptId)
                    }
                },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun Content(
    state: TestYourselfFormatUiState,
    onPickFormat: (format: String) -> Unit,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        LessonChip(state)
        Spacer(Modifier.height(20.dp))

        Text(
            text = if (state.isAi) "Pick a question type" else "Pick how you'd like to practise",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = SFProBold,
            color = DobiTextPrimary,
            fontSize = 22.sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (state.isAi)
                "Snap your textbook pages and Dobi will generate the questions you pick."
            else
                "We'll show you 10 questions with answers — practise as long as you'd like.",
            style = MaterialTheme.typography.bodyMedium,
            color = DobiTextSecondary
        )

        Spacer(Modifier.height(20.dp))

        FormatCard(
            iconBg = Color(0xFFDBEAFE),
            iconTint = DobiPrimary,
            icon = Icons.Default.Quiz,
            title = "MCQ",
            subtitle = "Multiple-choice questions with one correct answer.",
            ctaLabel = if (state.isAi) "Snap & Generate MCQs" else "Start MCQ Quiz",
            comingSoon = false,
            onClick = { onPickFormat(FORMAT_MCQ) }
        )

        Spacer(Modifier.height(12.dp))

        FormatCard(
            iconBg = Color(0xFFFEF3C7),
            iconTint = Color(0xFFF59E0B),
            icon = Icons.Default.Edit,
            title = "Subjective",
            subtitle = "Write short answers and have Dobi grade them with feedback.",
            ctaLabel = if (state.isAi) "Snap & Write" else "Start Subjective",
            comingSoon = false,
            onClick = { onPickFormat(FORMAT_SUBJECTIVE) }
        )

        Spacer(Modifier.height(20.dp))

        if (state.isAi) {
            InfoFooter(
                "✨",
                "Dobi will check your photos match this lesson before generating questions."
            )
        } else {
            InfoFooter(
                "💡",
                "Tip: try MCQs for quick checks, Subjective when you're ready to write full answers."
            )
        }
    }
}

@Composable
private fun LessonChip(state: TestYourselfFormatUiState) {
    val accent = parseHexColor(state.subjectColor)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = 0.12f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = if (state.isUnitScope) "📘" else "🔍", fontSize = 14.sp)
        Spacer(Modifier.width(8.dp))
        Text(
            text = state.title,
            color = accent,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Text(text = "•", color = accent.copy(alpha = 0.6f), fontSize = 14.sp)
        Spacer(Modifier.width(6.dp))
        Text(
            text = state.subject,
            color = accent.copy(alpha = 0.85f),
            fontSize = 13.sp
        )
    }
}

@Composable
private fun FormatCard(
    iconBg: Color,
    iconTint: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    ctaLabel: String,
    comingSoon: Boolean,
    onClick: () -> Unit
) {
    val border = if (comingSoon) Color(0xFFE5E7EB) else iconTint.copy(alpha = 0.5f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(BorderStroke(1.dp, border), RoundedCornerShape(18.dp))
            .background(if (comingSoon) Color(0xFFF8FAFF) else Color.White)
            .clickable(enabled = !comingSoon, onClick = onClick)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) { Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(26.dp)) }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = SFProBold,
                        color = DobiTextPrimary,
                        fontSize = 17.sp
                    )
                    if (comingSoon) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF59E0B).copy(alpha = 0.18f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Soon",
                                color = Color(0xFFF59E0B),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = DobiTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
            Spacer(Modifier.width(8.dp))
            if (!comingSoon) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        if (!comingSoon) {
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconTint)
                    .clickable(onClick = onClick)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = ctaLabel,
                    color = Color.White,
                    fontWeight = SFProSemiBold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun InfoFooter(emoji: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFEEF4FF))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 18.sp)
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = DobiTextPrimary,
            fontSize = 12.sp
        )
    }
}

private fun parseHexColor(hex: String): Color = runCatching {
    if (hex.isBlank()) Color(0xFF94A3B8)
    else Color(android.graphics.Color.parseColor(hex))
}.getOrDefault(Color(0xFF94A3B8))
