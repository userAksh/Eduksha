package com.dobi.app.feature.parentdashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dobi.app.R
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.dobi.app.core.ui.theme.DobiAccentOrange
import com.dobi.app.core.ui.theme.DobiBorderMuted
import com.dobi.app.core.ui.theme.DobiError
import com.dobi.app.core.ui.theme.DobiPastelCream
import com.dobi.app.core.ui.theme.DobiPastelMint
import com.dobi.app.core.ui.theme.DobiPastelPink
import com.dobi.app.core.ui.theme.DobiPastelSkyBlue
import com.dobi.app.core.ui.theme.DobiPrimary
import com.dobi.app.core.ui.theme.DobiSuccess
import com.dobi.app.core.ui.theme.DobiSuccessContainer
import com.dobi.app.core.ui.theme.DobiTextPrimary
import com.dobi.app.core.ui.theme.DobiTextSecondary
import com.dobi.app.data.repository.ActivityLogRepository.ActivityLogItem
import com.dobi.app.data.repository.ActivityLogRepository.DetailRoute
import com.dobi.app.data.repository.ActivityLogRepository.Source

private val ScreenBg = Color(0xFFFDF6F0)
private val CardBorder = Color(0xFFEDF1F8)

@Composable
fun SubjectDetailScreen(
    onNavigateBack: () -> Unit,
    onOpenUploadGroup: (groupKey: String) -> Unit,
    onOpenQuizResult: (unitId: String, conceptId: String) -> Unit,
    onOpenSubjectiveResult: (resultId: String) -> Unit,
    onOpenHomework: (homeworkId: String) -> Unit,
    viewModel: SubjectDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg),
    ) {
        SubjectTopBar(
            subject = state.subject,
            childName = state.childName,
            onNavigateBack = onNavigateBack,
        )

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = DobiPrimary)
            }
            return@Column
        }

        if (state.totalCount == 0) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No activity recorded yet", color = DobiTextSecondary, fontSize = 15.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Once ${state.childName.ifBlank { "your child" }} starts studying ${state.subject}, it'll show up here.",
                        color = DobiTextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (state.quizItems.isNotEmpty()) {
                item(key = "quiz") {
                    ActivitySection(
                        icon = Icons.Default.Quiz,
                        iconTint = DobiPrimary,
                        iconBg = DobiPastelSkyBlue,
                        title = stringResource(R.string.subject_quiz_attempts),
                        badge = "${state.quizItems.size} attempt${if (state.quizItems.size == 1) "" else "s"}",
                        summary = state.avgQuizAccuracy?.let { "Avg accuracy: $it%" },
                        items = state.quizItems,
                        onItemClick = { item ->
                            when (val d = item.detail) {
                                is DetailRoute.QuizMcq -> onOpenQuizResult(d.unitId, d.conceptId)
                                else -> Unit
                            }
                        }
                    )
                }
            }

            if (state.subjectiveItems.isNotEmpty()) {
                item(key = "subjective") {
                    ActivitySection(
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        iconTint = DobiSuccess,
                        iconBg = DobiPastelMint,
                        title = stringResource(R.string.subject_written_answers),
                        badge = "${state.subjectiveItems.size} submission${if (state.subjectiveItems.size == 1) "" else "s"}",
                        summary = null,
                        items = state.subjectiveItems,
                        onItemClick = { item ->
                            when (val d = item.detail) {
                                is DetailRoute.QuizSubjective -> onOpenSubjectiveResult(d.resultId)
                                else -> Unit
                            }
                        }
                    )
                }
            }

            if (state.classWorkItems.isNotEmpty()) {
                item(key = "classwork") {
                    ActivitySection(
                        icon = Icons.Default.School,
                        iconTint = DobiPrimary,
                        iconBg = Color(0xFFDBEAFE),
                        title = stringResource(R.string.subject_class_work),
                        badge = "${state.classWorkItems.size} upload${if (state.classWorkItems.size == 1) "" else "s"}",
                        summary = null,
                        items = state.classWorkItems,
                        onItemClick = { item ->
                            when (val d = item.detail) {
                                is DetailRoute.Upload -> onOpenUploadGroup(d.groupKey)
                                else -> Unit
                            }
                        }
                    )
                }
            }

            if (state.selfLearningItems.isNotEmpty()) {
                item(key = "selflearning") {
                    ActivitySection(
                        icon = Icons.Default.AutoStories,
                        iconTint = DobiError,
                        iconBg = DobiPastelPink,
                        title = stringResource(R.string.subject_self_study),
                        badge = "${state.selfLearningItems.size} upload${if (state.selfLearningItems.size == 1) "" else "s"}",
                        summary = null,
                        items = state.selfLearningItems,
                        onItemClick = { item ->
                            when (val d = item.detail) {
                                is DetailRoute.Upload -> onOpenUploadGroup(d.groupKey)
                                else -> Unit
                            }
                        }
                    )
                }
            }

            if (state.homeworkItems.isNotEmpty()) {
                item(key = "homework") {
                    ActivitySection(
                        icon = Icons.Default.EditNote,
                        iconTint = DobiAccentOrange,
                        iconBg = DobiPastelCream,
                        title = stringResource(R.string.dashboard_legend_homework),
                        badge = "${state.homeworkItems.size} submission${if (state.homeworkItems.size == 1) "" else "s"}",
                        summary = null,
                        items = state.homeworkItems,
                        onItemClick = { item ->
                            when (val d = item.detail) {
                                is DetailRoute.Homework -> onOpenHomework(d.homeworkId)
                                else -> Unit
                            }
                        }
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

// ── Top bar ──────────────────────────────────────────────────────────────────

@Composable
private fun SubjectTopBar(subject: String, childName: String, onNavigateBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ScreenBg)
            .statusBarsPadding()
            .padding(start = 12.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onNavigateBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = DobiTextPrimary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subject,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = DobiTextPrimary,
                )
                if (childName.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.subject_learning_title, childName),
                        fontSize = 12.sp,
                        color = DobiTextSecondary,
                    )
                }
            }
        }
    }
}

// ── Activity section card ─────────────────────────────────────────────────────

@Composable
private fun ActivitySection(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    badge: String,
    summary: String?,
    items: List<ActivityLogItem>,
    onItemClick: (ActivityLogItem) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        modifier = Modifier.fillMaxWidth(),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Section header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = DobiTextPrimary,
                    )
                    if (summary != null) {
                        Text(
                            text = summary,
                            fontSize = 11.sp,
                            color = DobiTextSecondary,
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = badge,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = iconTint,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Individual items
            items.forEachIndexed { index, item ->
                if (index > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(CardBorder)
                    )
                }
                ActivityItemRow(item = item, onClick = { onItemClick(item) })
            }
        }
    }
}

// ── Activity item row ─────────────────────────────────────────────────────────

@Composable
private fun ActivityItemRow(item: ActivityLogItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Thumbnail or icon
        val thumb = item.photoUrls.firstOrNull()
        if (thumb != null) {
            AsyncImage(
                model = thumb,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DobiBorderMuted),
            )
        } else {
            val (icon, tint, bg) = activityItemVisuals(item.source)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title.ifBlank { "Untitled" },
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = DobiTextPrimary,
                maxLines = 2,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = sdFormatRelative(item.timestamp),
                fontSize = 11.sp,
                color = DobiTextSecondary,
            )
        }

        Spacer(Modifier.width(8.dp))

        val score = item.score
        when {
            score != null && score.total > 0 -> {
                val ok = score.correct * 2 >= score.total
                ScoreBadge(label = "${score.correct}/${score.total}", isSuccess = ok)
            }
            item.source == Source.UPLOAD || item.source == Source.HOMEWORK -> {
                ScoreBadge(label = "Submitted", isSuccess = true)
            }
            item.source == Source.QUIZ_MCQ -> {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = DobiTextSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun ScoreBadge(label: String, isSuccess: Boolean) {
    val bg = if (isSuccess) DobiSuccessContainer else Color(0xFFFFF1F1)
    val fg = if (isSuccess) DobiSuccess else DobiError
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isSuccess) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(11.dp),
            )
            Spacer(Modifier.width(3.dp))
        }
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = fg)
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

private data class ItemVisuals(val icon: ImageVector, val tint: Color, val bg: Color)

private fun activityItemVisuals(source: Source): ItemVisuals = when (source) {
    Source.QUIZ_MCQ -> ItemVisuals(Icons.Default.Quiz, DobiPrimary, DobiPastelSkyBlue)
    Source.QUIZ_SUBJECTIVE -> ItemVisuals(Icons.AutoMirrored.Filled.MenuBook, DobiSuccess, DobiPastelMint)
    Source.UPLOAD -> ItemVisuals(Icons.Default.PhotoLibrary, DobiAccentOrange, DobiPastelCream)
    Source.HOMEWORK -> ItemVisuals(Icons.Default.EditNote, DobiAccentOrange, DobiPastelCream)
}

private fun sdFormatRelative(ts: Long): String {
    if (ts <= 0L) return "—"
    val now = System.currentTimeMillis()
    val deltaMin = (now - ts).coerceAtLeast(0) / 60_000L
    return when {
        deltaMin < 1 -> "Just now"
        deltaMin < 60 -> "${deltaMin}m ago"
        deltaMin < 24 * 60 -> "${deltaMin / 60}h ago"
        deltaMin < 2 * 24 * 60 -> "Yesterday"
        deltaMin < 30 * 24 * 60 -> "${deltaMin / (24 * 60)}d ago"
        else -> "${deltaMin / (30 * 24 * 60)}mo ago"
    }
}
