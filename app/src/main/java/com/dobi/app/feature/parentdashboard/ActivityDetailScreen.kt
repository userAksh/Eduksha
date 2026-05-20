package com.dobi.app.feature.parentdashboard

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Quiz
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
import androidx.compose.ui.unit.dp
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
import com.dobi.app.core.ui.theme.DobiPastelYellow
import com.dobi.app.core.ui.theme.DobiPrimary
import com.dobi.app.core.ui.theme.DobiSuccess
import com.dobi.app.core.ui.theme.DobiSuccessContainer
import com.dobi.app.core.ui.theme.DobiTextPrimary
import com.dobi.app.core.ui.theme.DobiTextSecondary
import com.dobi.app.data.repository.ActivityLogRepository.ActivityLogItem
import com.dobi.app.data.repository.ActivityLogRepository.Category
import com.dobi.app.data.repository.ActivityLogRepository.DetailRoute
import com.dobi.app.data.repository.ActivityLogRepository.Source

private val ScreenBg = Color(0xFFFDF6F0)

@Composable
fun ActivityDetailScreen(
    onNavigateBack: () -> Unit,
    onOpenUploadGroup: (groupKey: String) -> Unit,
    onOpenQuizResult: (unitId: String, conceptId: String) -> Unit,
    onOpenSubjectiveResult: (resultId: String) -> Unit,
    onOpenHomework: (homeworkId: String) -> Unit,
    viewModel: ActivityDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val (icon, accent, accentBg) = categoryVisuals(state.category)
    val title = categoryTitle(state.category)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg),
    ) {
        TopBar(
            title = title,
            childName = state.childName,
            icon = icon,
            iconTint = accent,
            iconTintBg = accentBg,
            onNavigateBack = onNavigateBack,
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
        ) {
            item {
                FilterChipRow(
                    selected = state.filter,
                    onSelect = viewModel::selectFilter,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Spacer(Modifier.height(14.dp))
                AttemptedSummary(
                    count = state.items.size,
                    filter = state.filter,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Spacer(Modifier.height(12.dp))
            }
            if (state.items.isEmpty()) {
                item {
                    EmptyState(
                        filter = state.filter,
                        category = state.category,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                    )
                }
            } else {
                items(state.items, key = { it.id }) { item ->
                    AttemptedItemCard(
                        item = item,
                        onClick = {
                            when (val d = item.detail) {
                                is DetailRoute.Upload -> onOpenUploadGroup(d.groupKey)
                                is DetailRoute.QuizMcq -> onOpenQuizResult(d.unitId, d.conceptId)
                                is DetailRoute.QuizSubjective -> onOpenSubjectiveResult(d.resultId)
                                is DetailRoute.Homework -> onOpenHomework(d.homeworkId)
                            }
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    title: String,
    childName: String,
    icon: ImageVector,
    iconTint: Color,
    iconTintBg: Color,
    onNavigateBack: () -> Unit,
) {
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
            Spacer(Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconTintBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = DobiTextPrimary,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (childName.isBlank()) "Attempts log" else "$childName's attempts",
                    fontSize = 12.sp,
                    color = DobiTextSecondary,
                )
            }
        }
    }
}

@Composable
private fun FilterChipRow(
    selected: ActivityTimeFilter,
    onSelect: (ActivityTimeFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ActivityTimeFilter.values().forEach { filter ->
            FilterChip(
                label = filter.label,
                selected = selected == filter,
                onClick = { onSelect(filter) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (selected) DobiPrimary else Color.White
    val fg = if (selected) Color.White else DobiTextPrimary
    val borderColor = if (selected) DobiPrimary else DobiBorderMuted
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = bg,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = fg,
            )
        }
    }
}

@Composable
private fun AttemptedSummary(
    count: Int,
    filter: ActivityTimeFilter,
    modifier: Modifier = Modifier,
) {
    val rangeLabel = when (filter) {
        ActivityTimeFilter.TODAY -> "today"
        ActivityTimeFilter.WEEK -> "in the last 7 days"
        ActivityTimeFilter.MONTH -> "in the last 30 days"
    }
    Text(
        text = "$count attempt${if (count == 1) "" else "s"} $rangeLabel",
        fontSize = 13.sp,
        color = DobiTextSecondary,
        modifier = modifier,
    )
}

@Composable
private fun EmptyState(
    filter: ActivityTimeFilter,
    category: Category,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = DobiPastelYellow,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "No ${categoryTitle(category).lowercase()} yet",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = DobiTextPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Nothing recorded ${
                    when (filter) {
                        ActivityTimeFilter.TODAY -> "today"
                        ActivityTimeFilter.WEEK -> "this week"
                        ActivityTimeFilter.MONTH -> "this month"
                    }
                }. Check back after the next learning session.",
                fontSize = 12.sp,
                color = DobiTextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun AttemptedItemCard(
    item: ActivityLogItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visuals = sourceVisuals(item)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        border = BorderStroke(1.dp, DobiBorderMuted),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val thumb = item.photoUrls.firstOrNull()
            if (thumb != null) {
                AsyncImage(
                    model = thumb,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DobiBorderMuted),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(visuals.bg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = visuals.icon,
                        contentDescription = null,
                        tint = visuals.tint,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DobiTextPrimary,
                    lineHeight = 18.sp,
                    maxLines = 2,
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.subject.isNotBlank()) {
                        SubjectPill(
                            subject = item.subject,
                            color = subjectColor(item.subject),
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text = "${visuals.label} • ${formatRelative(item.timestamp)}",
                        fontSize = 11.sp,
                        color = DobiTextSecondary,
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            val score = item.score
            if (score != null && score.total > 0) {
                val isSuccess = score.correct * 2 >= score.total
                StatusBadge(label = "${score.correct}/${score.total}", isSuccess = isSuccess)
            } else if (item.source == Source.UPLOAD) {
                StatusBadge(label = "Submitted", isSuccess = true)
            }
        }
    }
}

@Composable
private fun SubjectPill(subject: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 7.dp, vertical = 2.dp),
    ) {
        Text(
            text = subject,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}

@Composable
private fun StatusBadge(label: String, isSuccess: Boolean) {
    val bg = if (isSuccess) DobiSuccessContainer else Color(0xFFFFF1F1)
    val fg = if (isSuccess) DobiSuccess else DobiError
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isSuccess) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(12.dp),
            )
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = fg,
        )
    }
}

// ── Visual helpers ───────────────────────────────────────────────────────────

private data class SourceVisuals(
    val label: String,
    val icon: ImageVector,
    val tint: Color,
    val bg: Color,
)

private fun sourceVisuals(item: ActivityLogItem): SourceVisuals = when (item.source) {
    Source.UPLOAD -> SourceVisuals(
        label = uploadKindLabel(item.uploadKind),
        icon = Icons.Filled.PhotoLibrary,
        tint = DobiAccentOrange,
        bg = DobiPastelCream,
    )
    Source.QUIZ_MCQ -> SourceVisuals(
        label = "Quiz",
        icon = Icons.Filled.Quiz,
        tint = DobiPrimary,
        bg = DobiPastelSkyBlue,
    )
    Source.QUIZ_SUBJECTIVE -> SourceVisuals(
        label = "Write answers",
        icon = Icons.AutoMirrored.Filled.MenuBook,
        tint = DobiSuccess,
        bg = DobiPastelMint,
    )
    Source.HOMEWORK -> SourceVisuals(
        label = "School homework",
        icon = Icons.Filled.EditNote,
        tint = DobiAccentOrange,
        bg = DobiPastelCream,
    )
}

private fun uploadKindLabel(kind: String): String = when (kind) {
    "class_work" -> "Class work"
    "self_learning" -> "Self learning"
    "ai_test_prep" -> "AI practice"
    "tuition_work_learning" -> "Tuition learning"
    "tuition_work_written" -> "Tuition written"
    "tuition_work_test" -> "Tuition test"
    "tuition_hw_learning" -> "Homework reading"
    "tuition_hw_written" -> "Homework"
    else -> "Upload"
}

private fun categoryTitle(category: Category): String = when (category) {
    Category.SCHOOL_WORK -> "School Work"
    Category.HOME_WORK -> "Home Work"
    Category.SELF_LEARNING -> "Self Learning"
}

private fun categoryVisuals(category: Category): Triple<ImageVector, Color, Color> = when (category) {
    Category.SCHOOL_WORK -> Triple(
        Icons.AutoMirrored.Filled.MenuBook, DobiPrimary, DobiPastelSkyBlue
    )
    Category.HOME_WORK -> Triple(
        Icons.Filled.EditNote, DobiAccentOrange, DobiPastelCream
    )
    Category.SELF_LEARNING -> Triple(
        Icons.Filled.AutoStories, DobiError, DobiPastelPink
    )
}

private fun formatRelative(ts: Long): String {
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

// Subject colour palette (kept local; mirrors the dashboard's pastel scheme).
private fun subjectColor(subject: String): Color = when (subject.lowercase()) {
    "math", "mathematics" -> Color(0xFF7C3AED)
    "science" -> DobiPrimary
    "english" -> DobiError
    "hindi" -> DobiAccentOrange
    "social", "social studies", "social science", "history", "geography" -> Color(0xFF14B8A6)
    else -> DobiTextSecondary
}
