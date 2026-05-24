package com.dobi.app.feature.parentdashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dobi.app.R
import com.dobi.app.domain.model.ParentTaskType
import com.dobi.app.core.ui.theme.DobiBorderMuted
import com.dobi.app.core.ui.theme.DobiPastelCream
import com.dobi.app.core.ui.theme.DobiPastelSkyBlue
import com.dobi.app.core.ui.theme.DobiPrimary
import com.dobi.app.core.ui.theme.DobiSuccess
import com.dobi.app.core.ui.theme.DobiSuccessContainer
import com.dobi.app.core.ui.theme.DobiTextPrimary
import com.dobi.app.core.ui.theme.DobiTextSecondary
import com.dobi.app.core.ui.theme.DobiTextTertiary

// ── Local palette (per design brief) ─────────────────────────────────────────
private val ScreenBg = Color(0xFFFAFBFE)
private val CardBorder = Color(0xFFEDF1F8)
private val HeadlineCardBg = Color(0xFFEDF3FF)
private val PositiveGreen = Color(0xFF16A34A)
private val PositiveGreenSoft = Color(0xFFE6F7EC)
private val WarningAmber = Color(0xFFB45309)
private val WarningAmberSoft = Color(0xFFFFEFD9)
private val WarningAmberDeep = Color(0xFFEA580C)
private val CriticalRed = Color(0xFFDC2626)
private val SparklineSoftBg = Color(0xFFD7F2DF)
private val SparklineAmberBg = Color(0xFFFCE4C8)
private val SubjectTrack = Color(0xFFE9ECF4)
private val GridLine = Color(0xFFE3E8F2)

private val BarRevisionColor = Color(0xFF3B7BFF)
private val BarHomeworkColor = Color(0xFF16A34A)
private val BarUploadColor = Color(0xFFBFC6D6)

@Composable
fun InsightsScreen(
    onNavigateBack: () -> Unit,
    onSubjectClick: (subject: String) -> Unit = {},
    viewModel: InsightsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    var showAssignSheet by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.assignEvents.collect { event ->
            snackbarHost.showSnackbar(event.message)
            if (event.ok) showAssignSheet = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(ScreenBg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            TopBar(
                firstName = state.childFirstName,
                grade = state.childGrade,
                board = state.childBoard,
                gender = state.childGender,
                onNavigateBack = onNavigateBack,
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    RangeAndDateRow(
                        selected = state.range,
                        dateRangeLabel = state.dateRangeLabel,
                        onSelect = viewModel::selectRange,
                    )
                }
                item { HeadlineCard(state.headline) }
                item {
                    StatRow(
                        daysActive = state.daysActive,
                        totalAttempts = state.totalAttempts,
                        avgAccuracy = state.avgAccuracy,
                        revisionSessions = state.revisionSessions,
                    )
                }
                item {
                    HighlightsRow(
                        improved = state.mostImproved,
                        attention = state.needsAttention,
                    )
                }
                item { SubjectPerformanceCard(subjects = state.subjectBreakdown, onSubjectClick = onSubjectClick) }
                item {
                    RecommendationCard(
                        text = state.recommendation,
                        actionableTopic = state.actionableTopic,
                        onAssignClick = { showAssignSheet = true },
                    )
                }
                item { PrivacyFooter() }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
        SnackbarHost(
            hostState = snackbarHost,
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        val topic = state.actionableTopic
        if (showAssignSheet && topic != null) {
            AssignPracticeSheet(
                initialTopic = topic,
                curriculum = state.curriculum,
                childFirstName = state.childFirstName,
                isAssigning = state.isAssigning,
                onDismiss = { showAssignSheet = false },
                onAssign = { picked, type, note ->
                    viewModel.assignTask(picked, type, note)
                },
            )
        }
    }
}

// ── Top bar ──────────────────────────────────────────────────────────────────

@Composable
private fun TopBar(
    firstName: String,
    grade: Int,
    board: String,
    gender: String,
    onNavigateBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .size(40.dp)
                .background(Color.White, RoundedCornerShape(12.dp))
                .border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = DobiTextPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Image(
            painter = painterResource(
                id = if (gender.equals("girl", true)) R.drawable.dashboard_avatar_ritika
                else R.drawable.dashboard_avatar_aarav
            ),
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (firstName.isBlank()) stringResource(R.string.insights_learning_report)
                else "${firstName}'s Learning Report",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DobiTextPrimary,
                maxLines = 1,
            )
            if (grade > 0) {
                Spacer(Modifier.height(1.dp))
                Text(
                    text = stringResource(R.string.class_board_label, grade, board),
                    fontSize = 12.sp,
                    color = DobiTextSecondary,
                )
            }
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color.White, RoundedCornerShape(12.dp))
                .border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.CalendarMonth,
                contentDescription = null,
                tint = DobiTextPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// ── Range chips + date range pill ───────────────────────────────────────────

@Composable
private fun RangeAndDateRow(
    selected: InsightsRange,
    dateRangeLabel: String,
    onSelect: (InsightsRange) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                .padding(3.dp),
        ) {
            RangeChip("1 Week", selected == InsightsRange.WEEK) { onSelect(InsightsRange.WEEK) }
            Spacer(Modifier.width(2.dp))
            RangeChip("1 Month", selected == InsightsRange.MONTH) { onSelect(InsightsRange.MONTH) }
        }
        Spacer(Modifier.weight(1f))
        if (dateRangeLabel.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = BorderStroke(1.dp, CardBorder),
            ) {
                Text(
                    text = dateRangeLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = DobiTextPrimary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                )
            }
        }
    }
}

@Composable
private fun RangeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) DobiPastelSkyBlue else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.CalendarMonth,
            contentDescription = null,
            tint = if (selected) DobiPrimary else DobiTextSecondary,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) DobiPrimary else DobiTextSecondary,
        )
    }
}

// ── Headline card ───────────────────────────────────────────────────────────

@Composable
private fun HeadlineCard(headline: HeadlineInsight) {
    val accent = if (headline.tone == HeadlineTone.POSITIVE) DobiPrimary else DobiPrimary
    val bg = if (headline.tone == HeadlineTone.POSITIVE) HeadlineCardBg else HeadlineCardBg
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = bg,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, Color(0xFFDCE5F4)),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, Color(0xFFDCE5F4), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = headline.text.ifBlank { stringResource(R.string.insights_building_picture) },
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = DobiTextPrimary,
                lineHeight = 19.sp,
                modifier = Modifier.weight(1f),
            )
            if (headline.tone == HeadlineTone.POSITIVE) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Filled.EmojiEvents,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(38.dp),
                )
            }
        }
    }
}

// ── Stat row (4 tiles) ──────────────────────────────────────────────────────

private data class StatVisual(
    val iconBg: Color,
    val iconTint: Color,
    val icon: ImageVector,
    val valueColor: Color,
)

@Composable
private fun StatRow(
    daysActive: StatTileData,
    totalAttempts: StatTileData,
    avgAccuracy: StatTileData,
    revisionSessions: StatTileData,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val tileModifier = Modifier
            .weight(1f)
            .fillMaxHeight()
        StatTile(
            modifier = tileModifier,
            label = stringResource(R.string.insights_days_active),
            data = daysActive,
            visual = StatVisual(
                iconBg = Color(0xFFE6F7EC),
                iconTint = PositiveGreen,
                icon = Icons.Filled.EventAvailable,
                valueColor = PositiveGreen,
            ),
        )
        StatTile(
            modifier = tileModifier,
            label = stringResource(R.string.insights_total_attempts),
            data = totalAttempts,
            visual = StatVisual(
                iconBg = DobiPastelSkyBlue,
                iconTint = DobiPrimary,
                icon = Icons.Filled.Edit,
                valueColor = DobiPrimary,
            ),
        )
        StatTile(
            modifier = tileModifier,
            label = stringResource(R.string.insights_avg_accuracy),
            data = avgAccuracy,
            visual = StatVisual(
                iconBg = Color(0xFFEAE3FF),
                iconTint = Color(0xFF7C5CF8),
                icon = Icons.Filled.GpsFixed,
                valueColor = Color(0xFF7C5CF8),
            ),
        )
        StatTile(
            modifier = tileModifier,
            label = stringResource(R.string.insights_revision_sessions),
            data = revisionSessions,
            visual = StatVisual(
                iconBg = WarningAmberSoft,
                iconTint = WarningAmberDeep,
                icon = Icons.Filled.Refresh,
                valueColor = WarningAmberDeep,
            ),
        )
    }
}

@Composable
private fun StatTile(
    modifier: Modifier,
    label: String,
    data: StatTileData,
    visual: StatVisual,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        modifier = modifier,
        border = BorderStroke(1.dp, CardBorder),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(visual.iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = visual.icon,
                    contentDescription = null,
                    tint = visual.iconTint,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = data.value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = visual.valueColor,
                maxLines = 1,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = DobiTextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 13.sp,
            )
            if (data.delta != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = data.delta,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (data.deltaPositive) PositiveGreen else CriticalRed,
                    textAlign = TextAlign.Center,
                    lineHeight = 12.sp,
                )
            }
        }
    }
}

// ── Highlights row (Most Improved + Needs Attention) ────────────────────────

@Composable
private fun HighlightsRow(
    improved: ImprovedHighlight?,
    attention: AttentionHighlight?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        HighlightCard(
            modifier = Modifier.weight(1f),
            tone = HighlightTone.POSITIVE,
            header = stringResource(R.string.insights_most_improved),
            headerIcon = Icons.AutoMirrored.Filled.TrendingUp,
            title = improved?.subject ?: "Consistent Learning",
            badge = improved?.let { "+${it.deltaPct}%" },
            body = improved?.let {
                "Accuracy improved from ${it.prevAccuracy}% to ${it.currAccuracy}% this week."
            } ?: stringResource(R.string.insights_default_rhythm),
            sparkline = improved?.sparkline ?: stylizedSparkline(ascending = true),
        )
        HighlightCard(
            modifier = Modifier.weight(1f),
            tone = HighlightTone.WARNING,
            header = stringResource(R.string.insights_needs_attention_section),
            headerIcon = Icons.Filled.WarningAmber,
            title = attention?.subject ?: "No major concern",
            badge = attention?.accuracyPct?.let { "$it%" },
            body = attention?.let { "${it.tagline}\nReview key concepts" }
                ?: stringResource(R.string.insights_default_steady),
            sparkline = attention?.sparkline ?: stylizedSparkline(ascending = false),
        )
    }
}

private enum class HighlightTone { POSITIVE, WARNING }

@Composable
private fun HighlightCard(
    modifier: Modifier,
    tone: HighlightTone,
    header: String,
    headerIcon: ImageVector,
    title: String,
    badge: String?,
    body: String,
    sparkline: List<Float>,
) {
    val accent = if (tone == HighlightTone.POSITIVE) PositiveGreen else WarningAmberDeep
    val cardBg = if (tone == HighlightTone.POSITIVE) PositiveGreenSoft else WarningAmberSoft
    val badgeBg = if (tone == HighlightTone.POSITIVE) Color(0xFFC9EFD3) else Color(0xFFFAD1A8)
    val chartBg = if (tone == HighlightTone.POSITIVE) SparklineSoftBg else SparklineAmberBg

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = cardBg,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = headerIcon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = header,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = accent,
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                )
                if (badge != null) {
                    Spacer(Modifier.width(6.dp))
                    Surface(shape = RoundedCornerShape(50), color = badgeBg) {
                        Text(
                            text = badge,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = accent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = body,
                fontSize = 11.sp,
                color = DobiTextPrimary,
                lineHeight = 14.sp,
            )
            Spacer(Modifier.height(8.dp))
            SparklineLine(
                points = sparkline,
                lineColor = accent,
                fillBg = chartBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            )
        }
    }
}

@Composable
private fun SparklineLine(
    points: List<Float>,
    lineColor: Color,
    fillBg: Color,
    modifier: Modifier,
) {
    if (points.isEmpty()) return
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val padY = h * 0.15f
        val usable = h - padY * 2f
        val n = points.size
        val step = if (n > 1) w / (n - 1) else w
        val ys = points.map { p -> padY + (1f - p.coerceIn(0f, 1f)) * usable }
        val path = Path().apply {
            moveTo(0f, ys[0])
            for (i in 1 until n) {
                val px = step * i
                val py = ys[i]
                val midX = step * (i - 0.5f)
                cubicTo(midX, ys[i - 1], midX, py, px, py)
            }
        }
        // Soft area fill below the line.
        val fillPath = Path().apply {
            addPath(path)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(path = fillPath, color = fillBg)
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
        )
        // Small node dots at each data point.
        for (i in 0 until n) {
            drawCircle(
                color = lineColor,
                radius = 1.6.dp.toPx(),
                center = Offset(step * i, ys[i]),
            )
        }
    }
}

private fun stylizedSparkline(ascending: Boolean): List<Float> {
    val n = 14
    return if (ascending) (0 until n).map { 0.25f + 0.6f * (it / (n - 1f)) }
    else (0 until n).map { 0.6f - 0.25f * (it / (n - 1f)) }
}

// ── Subject performance card ────────────────────────────────────────────────

@Composable
private fun SubjectPerformanceCard(subjects: List<SubjectStat>, onSubjectClick: (String) -> Unit = {}) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, CardBorder),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.insights_subject_performance),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = DobiTextPrimary,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = DobiTextTertiary,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.insights_sorted_attempts),
                    fontSize = 11.sp,
                    color = DobiTextSecondary,
                )
            }

            if (subjects.isEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.insights_no_subject_activity),
                    fontSize = 13.sp,
                    color = DobiTextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                return@Column
            }

            Spacer(Modifier.height(12.dp))
            SubjectHeaderRow()
            Spacer(Modifier.height(4.dp))
            subjects.forEach { stat ->
                SubjectRow(stat, onClick = { onSubjectClick(stat.subject) })
            }

            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 6.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = DobiTextTertiary,
                    modifier = Modifier.size(12.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.insights_accuracy_note),
                    fontSize = 10.sp,
                    color = DobiTextSecondary,
                    lineHeight = 13.sp,
                )
            }
        }
    }
}

@Composable
private fun SubjectHeaderRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(40.dp))
        Text(
            text = stringResource(R.string.insights_subject_header),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = DobiTextTertiary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(R.string.insights_accuracy_header),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = DobiTextTertiary,
            textAlign = TextAlign.End,
            modifier = Modifier.width(76.dp),
        )
        Spacer(Modifier.width(22.dp))
    }
}

@Composable
private fun SubjectRow(stat: SubjectStat, onClick: () -> Unit = {}) {
    val visual = subjectVisual(stat.subject)
    val accuracyColor = when {
        stat.accuracyPct == null -> DobiTextTertiary
        stat.accuracyPct >= 80 -> PositiveGreen
        stat.accuracyPct >= 60 -> DobiPrimary
        else -> WarningAmberDeep
    }
    val lastRevisedColor = when {
        stat.lastRevisedDays == null -> DobiTextTertiary
        stat.lastRevisedDays >= 5 -> CriticalRed
        else -> DobiTextSecondary
    }
    val subtitle = androidx.compose.ui.text.buildAnnotatedString {
        pushStyle(androidx.compose.ui.text.SpanStyle(color = DobiTextSecondary))
        append("${stat.attempts} ${if (stat.attempts == 1) "attempt" else "attempts"}")
        pop()
        append("  •  ")
        pushStyle(androidx.compose.ui.text.SpanStyle(color = lastRevisedColor))
        append(stat.lastRevisedLabel)
        pop()
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(visual.bg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = visual.icon,
                contentDescription = null,
                tint = visual.tint,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stat.subject,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = DobiTextPrimary,
                maxLines = 1,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                maxLines = 1,
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(
            modifier = Modifier.width(76.dp),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                text = stat.accuracyPct?.let { "$it%" } ?: "—",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = accuracyColor,
            )
            Spacer(Modifier.height(3.dp))
            AccuracyBar(
                accuracyPct = stat.accuracyPct,
                color = accuracyColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
            )
        }
        Spacer(Modifier.width(6.dp))
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = DobiTextTertiary,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun AccuracyBar(accuracyPct: Int?, color: Color, modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(SubjectTrack),
    ) {
        if (accuracyPct != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(accuracyPct.coerceIn(0, 100) / 100f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color),
            )
        }
    }
}

// Subject icon mapping — Material icons by keyword.
private data class SubjectVisual(val icon: ImageVector, val tint: Color, val bg: Color)

private fun subjectVisual(name: String): SubjectVisual {
    val n = name.trim().lowercase()
    return when {
        n.contains("math") -> SubjectVisual(Icons.Filled.Functions, Color(0xFF16A34A), Color(0xFFE6F7EC))
        n.contains("physics") -> SubjectVisual(Icons.Filled.Science, Color(0xFF2563EB), Color(0xFFE0EAFE))
        n.contains("chemistry") -> SubjectVisual(Icons.Filled.Science, Color(0xFFCA8A04), Color(0xFFFEF3C7))
        n.contains("biology") -> SubjectVisual(Icons.Filled.Science, Color(0xFF16A34A), Color(0xFFDCFCE7))
        n.contains("science") -> SubjectVisual(Icons.Filled.Science, Color(0xFF2563EB), Color(0xFFE0EAFE))
        n.contains("english") -> SubjectVisual(Icons.AutoMirrored.Filled.MenuBook, Color(0xFFDC2626), Color(0xFFFEE2E2))
        n.contains("hindi") -> SubjectVisual(Icons.Filled.Language, Color(0xFFEA580C), Color(0xFFFEE7D6))
        n.contains("social") -> SubjectVisual(Icons.Filled.Public, Color(0xFFEA580C), Color(0xFFFEE7D6))
        n.contains("computer") -> SubjectVisual(Icons.Filled.Calculate, Color(0xFF7C5CF8), Color(0xFFEAE3FF))
        else -> SubjectVisual(Icons.AutoMirrored.Filled.MenuBook, DobiPrimary, DobiPastelSkyBlue)
    }
}

// ── Parent Recommendation card ──────────────────────────────────────────────

@Composable
private fun RecommendationCard(
    text: String,
    actionableTopic: ActionableTopic?,
    onAssignClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFFFF6E6),
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, Color(0xFFF1E0BD)),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFCE7B5)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = null,
                        tint = WarningAmberDeep,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.insights_parent_rec),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = WarningAmberDeep,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = text.ifBlank { stringResource(R.string.insights_default_rhythm) },
                        fontSize = 12.sp,
                        color = DobiTextPrimary,
                        lineHeight = 16.sp,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Filled.AssignmentTurnedIn,
                    contentDescription = null,
                    tint = WarningAmberDeep,
                    modifier = Modifier.size(36.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, Color(0xFFF1E0BD)),
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.insights_suggested_practice),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = WarningAmberDeep,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = if (actionableTopic != null) {
                                "${actionableTopic.subject} • ${actionableTopic.topicName}"
                            } else {
                                stringResource(R.string.insights_finding_topic)
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DobiTextPrimary,
                            maxLines = 1,
                        )
                        Spacer(Modifier.height(1.dp))
                        Text(
                            text = actionableTopic?.reason
                                ?: stringResource(R.string.insights_just_moment),
                            fontSize = 11.sp,
                            color = DobiTextSecondary,
                            lineHeight = 14.sp,
                            maxLines = 2,
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    val enabled = actionableTopic != null
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (enabled) WarningAmberDeep else DobiTextTertiary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(enabled = enabled, onClick = onAssignClick),
                    ) {
                        Text(
                            text = stringResource(R.string.insights_assign_btn),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssignPracticeSheet(
    initialTopic: ActionableTopic,
    curriculum: CurriculumIndex,
    childFirstName: String,
    isAssigning: Boolean,
    onDismiss: () -> Unit,
    onAssign: (ActionableTopic, ParentTaskType, String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val childLabel = childFirstName.ifBlank { "your child" }
    var topic by remember(initialTopic) { mutableStateOf(initialTopic) }
    var selectedType by remember { mutableStateOf(ParentTaskType.OUR_QUESTIONS) }
    var customNote by remember { mutableStateOf("") }
    var showPicker by remember { mutableStateOf(false) }
    val canSubmit = when {
        isAssigning -> false
        selectedType == ParentTaskType.CUSTOM -> customNote.trim().isNotEmpty()
        else -> true
    }
    val canChangeTopic = curriculum.isLoaded && curriculum.subjects.isNotEmpty()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 4.dp),
        ) {
            Text(
                text = stringResource(R.string.insights_assign_practice),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = DobiTextPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.insights_assign_desc, childLabel),
                fontSize = 13.sp,
                color = DobiTextSecondary,
                lineHeight = 17.sp,
            )

            // ── Topic preview + Change link ─────────────────────────────────
            Spacer(Modifier.height(14.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFFFF6E6),
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, Color(0xFFF1E0BD)),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = topic.subject.uppercase().ifBlank { "RECOMMENDED" },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = WarningAmberDeep,
                            modifier = Modifier.weight(1f),
                        )
                        if (canChangeTopic) {
                            Text(
                                text = stringResource(R.string.insights_change_btn),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = DobiPrimary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(enabled = !isAssigning) { showPicker = true }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = topic.topicName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DobiTextPrimary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = topic.reason,
                        fontSize = 12.sp,
                        color = DobiTextSecondary,
                        lineHeight = 16.sp,
                    )
                }
            }

            // ── Task type cards ─────────────────────────────────────────────
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.pick_task),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = DobiTextPrimary,
            )
            Spacer(Modifier.height(8.dp))
            TaskTypeCard(
                selected = selectedType == ParentTaskType.REVISE_CHAPTER,
                title = stringResource(R.string.task_type_revise),
                subtitle = stringResource(R.string.task_type_revise_desc),
                icon = Icons.AutoMirrored.Filled.MenuBook,
                tint = Color(0xFF7C5CF8),
                tintBg = Color(0xFFEAE3FF),
                onClick = { selectedType = ParentTaskType.REVISE_CHAPTER },
            )
            Spacer(Modifier.height(8.dp))
            TaskTypeCard(
                selected = selectedType == ParentTaskType.WRITE_QA,
                title = stringResource(R.string.task_type_qa),
                subtitle = stringResource(R.string.task_type_qa_desc),
                icon = Icons.Filled.Edit,
                tint = Color(0xFFCA8A04),
                tintBg = Color(0xFFFEF3C7),
                onClick = { selectedType = ParentTaskType.WRITE_QA },
            )
            Spacer(Modifier.height(8.dp))
            TaskTypeCard(
                selected = selectedType == ParentTaskType.OUR_QUESTIONS,
                title = stringResource(R.string.task_type_mcq),
                subtitle = if (topic.isUnitScope)
                    "10 AI-generated MCQs on this chapter."
                else "5 AI-generated MCQs on this concept.",
                icon = Icons.Filled.GpsFixed,
                tint = DobiPrimary,
                tintBg = DobiPastelSkyBlue,
                onClick = { selectedType = ParentTaskType.OUR_QUESTIONS },
            )
            Spacer(Modifier.height(8.dp))
            TaskTypeCard(
                selected = selectedType == ParentTaskType.CUSTOM,
                title = stringResource(R.string.task_type_custom),
                subtitle = stringResource(R.string.task_type_custom_desc_write),
                icon = Icons.Filled.Edit,
                tint = PositiveGreen,
                tintBg = PositiveGreenSoft,
                onClick = { selectedType = ParentTaskType.CUSTOM },
            )

            if (selectedType == ParentTaskType.CUSTOM) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = customNote,
                    onValueChange = { customNote = it },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.task_custom_hint),
                            fontSize = 13.sp,
                        )
                    },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DobiPrimary,
                        unfocusedBorderColor = CardBorder,
                    ),
                    shape = RoundedCornerShape(12.dp),
                )
            }

            // ── Action row ──────────────────────────────────────────────────
            Spacer(Modifier.height(20.dp))
            Row {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, CardBorder),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(enabled = !isAssigning, onClick = onDismiss),
                ) {
                    Text(
                        text = stringResource(R.string.cancel_btn),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DobiTextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (canSubmit) DobiPrimary else DobiTextTertiary,
                    modifier = Modifier
                        .weight(2f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(enabled = canSubmit) {
                            onAssign(topic, selectedType, customNote)
                        },
                ) {
                    Text(
                        text = if (isAssigning) stringResource(R.string.assigning_btn) else stringResource(R.string.give_task_btn),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showPicker) {
        TopicPickerSheet(
            curriculum = curriculum,
            initialSubject = topic.subject,
            onDismiss = { showPicker = false },
            onPicked = { unit ->
                topic = unit.toActionable("Picked from ${unit.subject} chapters.")
                showPicker = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopicPickerSheet(
    curriculum: CurriculumIndex,
    initialSubject: String,
    onDismiss: () -> Unit,
    onPicked: (UnitOption) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val defaultSubject = curriculum.subjects
        .firstOrNull { it.name.equals(initialSubject, true) }?.name
        ?: curriculum.subjects.firstOrNull()?.name
        ?: ""
    var selectedSubject by remember(defaultSubject) { mutableStateOf(defaultSubject) }
    val units = curriculum.unitsFor(selectedSubject)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
        ) {
            Text(
                text = stringResource(R.string.insights_choose_topic),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = DobiTextPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.insights_choose_topic_desc),
                fontSize = 13.sp,
                color = DobiTextSecondary,
            )

            Spacer(Modifier.height(14.dp))
            // Subject chips — horizontally scrollable.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                curriculum.subjects.forEach { s ->
                    SubjectChip(
                        name = s.name,
                        selected = s.name.equals(selectedSubject, true),
                        onClick = { selectedSubject = s.name },
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.insights_chapters),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = DobiTextTertiary,
            )
            Spacer(Modifier.height(6.dp))

            if (units.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.insights_no_chapters),
                        fontSize = 13.sp,
                        color = DobiTextSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                // Cap height so the sheet stays usable when there are many chapters.
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(units) { unit ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, CardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onPicked(unit) },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = unit.unitName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DobiTextPrimary,
                                    modifier = Modifier.weight(1f),
                                )
                                Icon(
                                    imageVector = Icons.Filled.ChevronRight,
                                    contentDescription = null,
                                    tint = DobiTextTertiary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SubjectChip(name: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) DobiPrimary else Color.White,
        border = BorderStroke(1.dp, if (selected) DobiPrimary else CardBorder),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick),
    ) {
        Text(
            text = name,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else DobiTextPrimary,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun TaskTypeCard(
    selected: Boolean,
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    tintBg: Color,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected) DobiPastelSkyBlue else Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) DobiPrimary else CardBorder,
        ),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(tintBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = DobiTextPrimary,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = DobiTextSecondary,
                    lineHeight = 15.sp,
                )
            }
            if (selected) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = DobiPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

// ── Privacy footer ──────────────────────────────────────────────────────────

@Composable
private fun PrivacyFooter() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            tint = DobiTextTertiary,
            modifier = Modifier.size(12.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.insights_private_note),
            fontSize = 11.sp,
            color = DobiTextSecondary,
        )
    }
}
