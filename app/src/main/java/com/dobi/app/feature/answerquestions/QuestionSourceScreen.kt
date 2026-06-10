package com.dobi.app.feature.answerquestions

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dobi.app.core.ui.theme.DobiBackground
import com.dobi.app.core.ui.theme.DobiPrimary
import com.dobi.app.core.ui.theme.DobiPrimaryContainer
import com.dobi.app.core.ui.theme.DobiTextPrimary
import com.dobi.app.core.ui.theme.DobiTextSecondary
import com.dobi.app.core.ui.theme.SFProBold
import com.dobi.app.core.ui.theme.SFProSemiBold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionSourceScreen(
    onNavigateBack: () -> Unit,
    onPickCuratedSet: () -> Unit,
    onPickUploadAndGenerate: () -> Unit,
    onPickWriteAnswers: () -> Unit,
    onViewLastResult: () -> Unit,
    viewModel: QuestionSourceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navEvents.collect { event ->
            when (event) {
                is QuestionSourceNavEvent.LaunchWriteAnswers -> onPickWriteAnswers()
            }
        }
    }

    Scaffold(
        containerColor = DobiBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Answer Questions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = SFProBold,
                        color = DobiTextPrimary
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
        if (uiState.isLoading) {
            Box(
                Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = DobiPrimary) }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            LessonContextChip(unitName = uiState.unitName, subject = uiState.subject)

            Spacer(Modifier.height(20.dp))

            Text(
                text = "How would you like to practise?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = SFProBold,
                color = DobiTextPrimary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Pick one — both work great for what you've learned.",
                style = MaterialTheme.typography.bodyMedium,
                color = DobiTextSecondary
            )

            Spacer(Modifier.height(24.dp))

            CuratedSetSlot(
                disabled = uiState.hasReachedMax,
                onClick = onPickCuratedSet,
                onViewLastResult = onViewLastResult
            )

            Spacer(Modifier.height(14.dp))

            QuestionSourceCard(
                icon = Icons.Default.AutoAwesome,
                badgeText = "✨ AI-powered",
                title = "Upload Your Pages",
                description = "Snap your textbook questions or chapter pages — Dobi will turn them into questions for you.",
                accent = Color(0xFF8B5CF6),
                onClick = onPickUploadAndGenerate
            )

            Spacer(Modifier.height(14.dp))

            QuestionSourceCard(
                icon = Icons.Default.Edit,
                badgeText = "📝 Write answers",
                title = "Write Answers",
                description = "Dobi generates questions from this lesson — write your answers by hand or type them in.",
                accent = Color(0xFF059669),
                onClick = viewModel::onWriteAnswersClick
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = "You can switch between options anytime.",
                style = MaterialTheme.typography.bodySmall,
                color = DobiTextSecondary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun CuratedSetSlot(
    disabled: Boolean,
    onClick: () -> Unit,
    onViewLastResult: () -> Unit
) {
    Column {
        Box {
            QuestionSourceCard(
                icon = Icons.Default.Quiz,
                badgeText = "Recommended",
                title = "Our Question Set",
                description = "Practise with curated questions made for this lesson by Dobi.",
                accent = DobiPrimary,
                onClick = if (disabled) ({}) else onClick,
                contentAlpha = if (disabled) 0.45f else 1f
            )

            if (disabled) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.55f))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(28.dp)
                        .clip(RoundedCornerShape(50))
                        .background(DobiPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        if (disabled) {
            Spacer(Modifier.height(10.dp))
            MaxAttemptsLink(onViewLastResult = onViewLastResult)
        }
    }
}

@Composable
private fun MaxAttemptsLink(onViewLastResult: () -> Unit) {
    val text = buildAnnotatedString {
        withStyle(SpanStyle(color = DobiTextSecondary)) {
            append("Max attempts reached. ")
        }
        withStyle(
            SpanStyle(
                color = DobiPrimary,
                textDecoration = TextDecoration.Underline,
                fontWeight = SFProSemiBold
            )
        ) {
            append("Check your results")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onViewLastResult
            )
            .padding(horizontal = 4.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Visible,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun LessonContextChip(unitName: String, subject: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(DobiPrimaryContainer)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("📘", fontSize = 14.sp)
        Spacer(Modifier.width(8.dp))
        Text(
            text = unitName.ifBlank { "Lesson" },
            fontSize = 13.sp,
            color = DobiPrimary,
            fontWeight = SFProSemiBold
        )
        if (subject.isNotBlank()) {
            Text(
                text = "  •  $subject",
                fontSize = 12.sp,
                color = DobiPrimary.copy(alpha = 0.75f)
            )
        }
    }
}

@Composable
private fun QuestionSourceCard(
    icon: ImageVector,
    badgeText: String,
    title: String,
    description: String,
    accent: Color,
    onClick: () -> Unit,
    contentAlpha: Float = 1f
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(18.dp), spotColor = Color(0x14000000))
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .alpha(contentAlpha),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE5EBF5)),
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
                    .size(54.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(accent.copy(alpha = 0.14f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 10.sp,
                            color = accent,
                            fontWeight = SFProSemiBold
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = SFProBold,
                    color = DobiTextPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = DobiTextSecondary,
                    lineHeight = 16.sp
                )
            }

            Spacer(Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(50))
                    .background(accent.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

