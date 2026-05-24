package com.dobi.app.feature.parentdashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.core.view.WindowCompat
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dobi.app.R
import com.dobi.app.domain.model.Child
import com.dobi.app.domain.model.ChildEngagement
import com.dobi.app.core.ui.theme.DobiAccentOrange
import com.dobi.app.core.ui.theme.DobiBorderMuted
import com.dobi.app.core.ui.theme.DobiError
import com.dobi.app.core.ui.theme.DobiPastelCream
import com.dobi.app.core.ui.theme.DobiPastelPink
import com.dobi.app.core.ui.theme.DobiPastelSkyBlue
import com.dobi.app.core.ui.theme.DobiPastelYellow
import com.dobi.app.core.ui.theme.DobiPrimary
import com.dobi.app.core.ui.theme.DobiSuccess
import com.dobi.app.core.ui.theme.DobiSuccessContainer
import com.dobi.app.core.ui.theme.DobiTextPrimary
import com.dobi.app.core.ui.theme.DobiTextSecondary
import com.dobi.app.core.ui.theme.DobiTextTertiary

// ── Screen-local theme tokens (per Figma) ────────────────────────────────────
private val PrimaryBlue = Color(0xFF3B7BFF)
private val DarkNavy = Color(0xFF16213E)
private val MutedText = Color(0xFF6B7895)
private val DividerBlue = Color(0xFFE3EAF8)
private val SwitcherBorder = Color(0xFFE3ECFF)
private val AddBorder = Color(0xFFB7D2FF)

private data class DashChild(
    val id: String,
    val name: String,
    val firstName: String,
    val grade: Int,
    val board: String,
    val avatarRes: Int,
    val isGirl: Boolean,
    val revisionStreakDays: Int,
    val streakDays: Int = 0,
    val weakSubject: String,
    val weeklyProgressPct: Int,
    val smartInsight: String,
    val watchOut: String,
    val whatYouCanDo: String,
    val weekPattern: List<DayActivity>,
    val insight: LearningInsight,
    val lastRevisedLabel: String = "No activity yet",
)

private val mockPreviewInsight = LearningInsight(
    priority = 6,
    title = "Great learning balance this week",
    body = "Homework and revision are both consistent.",
    tone = InsightTone.POSITIVE,
)

private fun mockChildren(): List<DashChild> = listOf(
    DashChild(
        id = "c1",
        name = "Ritika Sharma",
        firstName = "Ritika",
        grade = 7,
        board = "CBSE",
        avatarRes = R.drawable.dashboard_avatar_ritika,
        isGirl = true,
        revisionStreakDays = 4,
        weakSubject = "Science",
        weeklyProgressPct = 78,
        smartInsight = "",
        watchOut = "",
        whatYouCanDo = "",
        weekPattern = mockWeekPatternPool[0],
        insight = mockPreviewInsight,
    ),
    DashChild(
        id = "c2",
        name = "Aarav Sharma",
        firstName = "Aarav",
        grade = 6,
        board = "CBSE",
        avatarRes = R.drawable.dashboard_avatar_aarav,
        isGirl = false,
        revisionStreakDays = 2,
        weakSubject = "English",
        weeklyProgressPct = 54,
        smartInsight = "",
        watchOut = "",
        whatYouCanDo = "",
        weekPattern = mockWeekPatternPool[1],
        insight = mockPreviewInsight,
    ),
    DashChild(
        id = "c3",
        name = "Vivaan Sharma",
        firstName = "Vivaan",
        grade = 7,
        board = "CBSE",
        avatarRes = R.drawable.dashboard_avatar_vivaan,
        isGirl = false,
        revisionStreakDays = 7,
        weakSubject = "Hindi",
        weeklyProgressPct = 91,
        smartInsight = "",
        watchOut = "",
        whatYouCanDo = "",
        weekPattern = mockWeekPatternPool[2],
        insight = mockPreviewInsight,
    ),
)

private val mockInsightPool = listOf(
    "Science revision consistency improved this week. Keep it up!",
    "Math practice picked up this week — nice momentum.",
    "Strong streak this week — celebrate it!",
)
private val mockWatchOutPool = listOf(
    "Fractions chapter not revised in 8 days",
    "English homework pending for 3 days",
    "Hindi grammar revision missed twice",
)
private val mockActionPool = listOf(
    "Encourage them to revise Fractions today.",
    "Ask them to read one English passage aloud.",
    "Try 10 minutes of Hindi grammar tonight.",
)

private val placeholderInsight = LearningInsight(
    priority = 0,
    title = "Building this week's picture",
    body = "As uploads and quizzes come in, insights will appear here.",
    tone = InsightTone.INFO,
)

private fun toDashChild(
    child: Child,
    index: Int,
    engagement: ChildEngagement?,
    weekPattern: List<DayActivity>?,
    insight: LearningInsight?,
): DashChild {
    val avatarRes = when (index % 3) {
        0 -> R.drawable.dashboard_avatar_ritika
        1 -> R.drawable.dashboard_avatar_aarav
        else -> R.drawable.dashboard_avatar_vivaan
    }
    val isGirl = child.gender.equals("girl", ignoreCase = true)
    return DashChild(
        id = child.id,
        name = child.name,
        firstName = child.name.substringBefore(" ").ifBlank { child.name },
        grade = child.grade,
        board = child.board,
        avatarRes = avatarRes,
        isGirl = isGirl,
        // Streak comes from days active this week (Mon→today). Weekly progress
        // uses pooled quiz/subjective accuracy as a proxy. Weak subject is
        // still a placeholder — per-subject accuracy isn't aggregated yet.
        revisionStreakDays = engagement?.daysActiveThisWeek ?: 0,
        streakDays = engagement?.streakDays ?: 0,
        weakSubject = engagement?.weakSubject?.ifBlank { "—" } ?: "—",
        weeklyProgressPct = engagement?.accuracyPct ?: 0,
        smartInsight = "",
        watchOut = run {
            val weakPct = engagement?.weakSubjectAccuracyPct
            if (weakPct != null) return@run "$weakPct% accuracy"
            val lastMs = engagement?.lastActivityMs ?: 0L
            if (lastMs == 0L) return@run "No activity yet"
            val daysAgo = ((System.currentTimeMillis() - lastMs) / (24L * 60 * 60 * 1000)).toInt()
            when {
                daysAgo == 0 -> "Active today"
                daysAgo == 1 -> "Active yesterday"
                else -> "Last active $daysAgo days ago"
            }
        },
        whatYouCanDo = "",
        weekPattern = weekPattern ?: EMPTY_WEEK,
        insight = insight ?: placeholderInsight,
        lastRevisedLabel = run {
            val lastMs = engagement?.lastActivityMs ?: 0L
            if (lastMs == 0L) return@run "No activity yet"
            val daysAgo = ((System.currentTimeMillis() - lastMs) / (24L * 60 * 60 * 1000)).toInt()
            when {
                daysAgo == 0 -> "Today"
                daysAgo == 1 -> "Yesterday"
                else -> "$daysAgo days ago"
            }
        },
    )
}

// Mock weekly data with revision + homework lists per day. Indexed Mon → Sun.
// Real data should come from ActivityLogRepository aggregated by local-day —
// each item's route field comes straight from ActivityLogItem.detail.
private fun mockQuiz(title: String, subject: String) = DayItem(
    title = title,
    subject = subject,
    route = com.dobi.app.data.repository.ActivityLogRepository.DetailRoute.QuizMcq(
        unitId = "mock-unit", conceptId = "",
    ),
)

private fun mockWriteAnswer(title: String, subject: String) = DayItem(
    title = title,
    subject = subject,
    route = com.dobi.app.data.repository.ActivityLogRepository.DetailRoute.QuizSubjective(
        resultId = "mock-result",
    ),
)

private fun mockUpload(title: String, subject: String) = DayItem(
    title = title,
    subject = subject,
    route = com.dobi.app.data.repository.ActivityLogRepository.DetailRoute.Upload(
        groupKey = "mock-group", photoUrls = emptyList(),
    ),
)

private fun mockHomework(title: String, subject: String) = DayItem(
    title = title,
    subject = subject,
    route = com.dobi.app.data.repository.ActivityLogRepository.DetailRoute.Homework(
        homeworkId = "mock-homework",
    ),
)

private val mockWeekPatternPool: List<List<DayActivity>> = listOf(
    listOf(
        DayActivity(revisions = listOf(
            mockQuiz("Fractions Quiz", "Math"),
            mockWriteAnswer("Verbs — write answer", "English"),
        )),
        DayActivity(
            revisions = listOf(mockUpload("Photosynthesis notes", "Science")),
            homeworks = listOf(mockHomework("Math homework — Page 24", "Math")),
        ),
        DayActivity(revisions = listOf(mockUpload("Algebra practice", "Math"))),
        DayActivity(),
        DayActivity(),
        DayActivity(homeworks = listOf(mockHomework("Science homework — Diagram", "Science"))),
        DayActivity(),
    ),
    listOf(
        DayActivity(homeworks = listOf(mockHomework("English homework — Essay", "English"))),
        DayActivity(homeworks = listOf(mockHomework("Math homework — Page 18", "Math"))),
        DayActivity(),
        DayActivity(
            revisions = listOf(mockQuiz("Comprehension quiz", "English")),
            homeworks = listOf(mockHomework("Science homework — Worksheet", "Science")),
        ),
        DayActivity(homeworks = listOf(mockHomework("Math homework — Page 19", "Math"))),
        DayActivity(),
        DayActivity(),
    ),
    listOf(
        DayActivity(revisions = listOf(
            mockQuiz("Trigonometry quiz", "Math"),
            mockWriteAnswer("Hindi grammar — write answer", "Hindi"),
        )),
        DayActivity(revisions = listOf(mockUpload("Force & motion concept", "Science"))),
        DayActivity(
            revisions = listOf(mockQuiz("Mensuration quiz", "Math")),
            homeworks = listOf(mockHomework("Math homework — Page 33", "Math")),
        ),
        DayActivity(revisions = listOf(mockUpload("Civics notes", "Social Science"))),
        DayActivity(revisions = listOf(
            mockQuiz("Algebra quiz", "Math"),
            mockUpload("Light & shadow concept", "Science"),
        )),
        DayActivity(homeworks = listOf(mockHomework("Hindi homework — Vyakaran", "Hindi"))),
        DayActivity(revisions = listOf(mockUpload("Chapter recap — Geography", "Social Science"))),
    ),
)

// Data model (DayKind / DayItem / DayActivity) lives in WeekPattern.kt so the
// ViewModel can construct them when bucketing real ActivityLogRepository data.

@Composable
fun ParentDashboardScreen(
    onAddChild: () -> Unit = {},
    onViewSmartInsightDetails: () -> Unit = {},
    onOpenWatchOut: () -> Unit = {},
    onTakeAction: () -> Unit = {},
    onOpenActivity: (categoryKey: String, childId: String) -> Unit = { _, _ -> },
    onOpenInsights: (childId: String) -> Unit = {},
    onOpenUploadGroup: (groupKey: String) -> Unit = {},
    onOpenQuizResult: (unitId: String, conceptId: String, childId: String) -> Unit = { _, _, _ -> },
    onOpenSubjectiveResult: (resultId: String) -> Unit = {},
    onOpenHomework: (homeworkId: String) -> Unit = {},
    viewModel: ParentDashboardViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
) {
    val vmState by viewModel.uiState.collectAsStateWithLifecycle()
    val children = remember(vmState.children, vmState.engagements, vmState.weekPatterns, vmState.insights) {
        if (vmState.children.isEmpty()) mockChildren()
        else vmState.children.mapIndexed { idx, c ->
            toDashChild(
                child = c,
                index = idx,
                engagement = vmState.engagements[c.id],
                weekPattern = vmState.weekPatterns[c.id],
                insight = vmState.insights[c.id],
            )
        }
    }
    if (children.isEmpty()) return
    var selectedChildId by rememberSaveable(children) { mutableStateOf(children.first().id) }
    val selected = children.firstOrNull { it.id == selectedChildId } ?: children.first()

    val headerBg = Color(0xFFFDF6F0)

    // Paint the system status bar in the same cream as the header so the
    // top edge has no visible seam. Light-on-dark icons → dark-on-light.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as android.app.Activity).window
            window.statusBarColor = headerBg.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                ParentHomeTopBar(
                    parentName = vmState.parentName,
                    children = children,
                    selectedChildId = selectedChildId,
                    onChildClick = { selectedChildId = it.id },
                    onAddChildClick = onAddChild,
                )
            }
            item {
                // Blue → white gradient section. Starts right below the
                // top bar (where the lamp bg ends) and scrolls together
                // with everything else as one piece.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFD6E6F7).copy(alpha = 0.55f),
                                    Color.White,
                                )
                            )
                        )
                        .padding(top = 12.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PaddedBlock {
                        ChildSummaryCard(
                            child = selected,
                            onViewReport = { onOpenInsights(selected.id) },
                        )
                    }
                    PaddedBlock {
                        ActivityCategoriesCard(
                            onOpenActivity = { key -> onOpenActivity(key, selected.id) }
                        )
                    }
                    PaddedBlock {
                        WeeklyLearningPattern(
                            week = selected.weekPattern,
                            firstName = selected.firstName,
                            onOpenItem = { item ->
                                when (val route = item.route) {
                                    is com.dobi.app.data.repository.ActivityLogRepository.DetailRoute.Upload ->
                                        onOpenUploadGroup(route.groupKey)
                                    is com.dobi.app.data.repository.ActivityLogRepository.DetailRoute.QuizMcq ->
                                        onOpenQuizResult(route.unitId, route.conceptId, selected.id)
                                    is com.dobi.app.data.repository.ActivityLogRepository.DetailRoute.QuizSubjective ->
                                        onOpenSubjectiveResult(route.resultId)
                                    is com.dobi.app.data.repository.ActivityLogRepository.DetailRoute.Homework ->
                                        onOpenHomework(route.homeworkId)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaddedBlock(content: @Composable () -> Unit) {
    Box(modifier = Modifier.padding(horizontal = 16.dp)) { content() }
}

@Composable
private fun ParentHomeTopBar(
    parentName: String,
    children: List<DashChild>,
    selectedChildId: String,
    onChildClick: (DashChild) -> Unit,
    onAddChildClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            // Hard cream → blue transition at y=176 — same y where the
            // lamp image's wavy cream cloud ends, so both finish together.
            // Blue color matches item 1's gradient top, so the y=260 seam
            // is also invisible.
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to Color(0xFFFDF6F0),
                        (176f / 260f) to Color(0xFFFDF6F0),
                        (176f / 260f) to Color(0xFFD6E6F7).copy(alpha = 0.55f),
                        1f to Color(0xFFD6E6F7).copy(alpha = 0.55f),
                    )
                )
            )
    ) {
        // Lamp + cream wave illustration anchored bottom-right.
        // offset(-84) puts the image's bottom edge at y=176 — same y as
        // the cream→blue seam, so the painted wavy edge and the flat
        // cream backdrop end together at the same line.
        Image(
            painter = painterResource(id = R.drawable.dashboard_header_bg),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(y = (-84).dp)
                .size(width = 240.dp, height = 220.dp)
        )

        // Greeting text
        Column(
            modifier = Modifier
                .padding(start = 24.dp)
                .offset(y = 40.dp)
        ) {
            Text(
                text = if (parentName.isNotBlank()) "Hello, $parentName! 👋" else "Hello! 👋",
                fontSize = 22.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Bold,
                color = DarkNavy
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Let's support your child's\nlearning journey.",
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = MutedText
            )
        }

        // Floating child switcher card — offset(-20) lifts the card 20dp
        // above the top bar's 260dp bottom edge.
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-20).dp)
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .height(94.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(28.dp),
                    spotColor = Color(0x1A3B7BFF)
                )
                .background(Color.White, RoundedCornerShape(28.dp))
                .border(
                    width = 1.dp,
                    color = SwitcherBorder,
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            children.forEach { child ->
                ChildSwitcherItem(
                    child = child,
                    selected = child.id == selectedChildId,
                    onClick = { onChildClick(child) },
                    modifier = Modifier.weight(1f)
                )
            }
            VerticalDivider(
                modifier = Modifier.height(48.dp),
                thickness = 1.dp,
                color = DividerBlue
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onAddChildClick)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .border(
                            width = 1.5.dp,
                            color = AddBorder,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add child",
                        tint = PrimaryBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Add",
                    fontSize = 12.sp,
                    color = DarkNavy,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ChildSwitcherItem(
    child: DashChild,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Image(
            painter = painterResource(id = child.avatarRes),
            contentDescription = child.firstName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .then(
                    if (selected) Modifier.border(2.dp, PrimaryBlue, CircleShape)
                    else Modifier.border(1.dp, Color(0xFFE6EAF1), CircleShape)
                )
        )
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = child.firstName,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) PrimaryBlue else DarkNavy,
            maxLines = 1
        )
        if (selected) {
            Box(
                modifier = Modifier
                    .padding(top = 3.dp)
                    .width(28.dp)
                    .height(3.dp)
                    .background(PrimaryBlue, RoundedCornerShape(50))
            )
        } else {
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

// ── Colors specific to the new student summary card ──────────────────────────
private val StudiedTint = DobiSuccess
private val StudiedTintBg = DobiSuccessContainer
private val StudiedCardBg = Color(0xFFEFFAF1)

private val RevisedTint = Color(0xFF7C5CF8)
private val RevisedTintBg = Color(0xFFE8E4FF)
private val RevisedCardBg = Color(0xFFF1EEFF)

private val AttentionTint = Color(0xFFF59E0B)
private val AttentionTintBg = Color(0xFFFFE7B5)
private val AttentionCardBg = Color(0xFFFFF1DA)

private val InsightCardBg = Color(0xFFEEF3FF)
private val InsightDivider = Color(0xFFD6DEF2)

private data class StatusPillSpec(val text: String, val tint: Color, val bg: Color)

private fun statusFor(child: DashChild): StatusPillSpec = when {
    child.streakDays >= 3 -> StatusPillSpec(
        text = "Learning Well",
        tint = DobiSuccess,
        bg = DobiSuccessContainer,
    )
    child.streakDays > 0 -> StatusPillSpec(
        text = "Needs revision consistency",
        tint = DobiAccentOrange,
        bg = Color(0xFFFFEFCC),
    )
    else -> StatusPillSpec(
        text = "Needs more attention",
        tint = DobiError,
        bg = DobiPastelPink,
    )
}

@Composable
private fun ChildSummaryCard(
    child: DashChild,
    onViewReport: () -> Unit,
) {
    val status = statusFor(child)
    val daysStudied = child.revisionStreakDays.coerceIn(0, 7)
    val studiedHint = when {
        daysStudied >= 5 -> "Great consistency!"
        daysStudied >= 3 -> "Keep building the habit."
        else -> "Aim for daily revision."
    }
    val attentionDetail = child.watchOut.ifBlank { "—" }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, AddBorder),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            StudentHeader(
                name = child.firstName,
                grade = child.grade,
                board = child.board,
                status = status,
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val tileModifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                InsightStatTile(
                    title = "${daysStudied}/7 Days",
                    subtitle = "Studied this week",
                    hint = studiedHint,
                    icon = Icons.Filled.EventAvailable,
                    tint = StudiedTint,
                    tintBg = StudiedTintBg,
                    cardBg = StudiedCardBg,
                    titleColor = StudiedTint,
                    modifier = tileModifier,
                )
                InsightStatTile(
                    title = "Last active",
                    subtitle = child.lastRevisedLabel,
                    hint = if (child.lastRevisedLabel == "No activity yet") "Start revising to build the habit." else "Keep the streak going!",
                    icon = Icons.Filled.Autorenew,
                    tint = RevisedTint,
                    tintBg = RevisedTintBg,
                    cardBg = RevisedCardBg,
                    titleColor = RevisedTint,
                    modifier = tileModifier,
                )
                InsightStatTile(
                    title = "Needs attention",
                    subtitle = child.weakSubject,
                    hint = attentionDetail,
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    tint = AttentionTint,
                    tintBg = AttentionTintBg,
                    cardBg = AttentionCardBg,
                    titleColor = AttentionTint,
                    modifier = tileModifier,
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            ParentInsightBlock(insight = child.insight)

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(DobiBorderMuted),
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onViewReport)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(DobiPastelSkyBlue),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.BarChart,
                        contentDescription = null,
                        tint = DobiPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "View Detailed Learning Report",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DobiPrimary,
                    modifier = Modifier.weight(1f),
                )
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

@Composable
private fun StudentHeader(
    name: String,
    grade: Int,
    board: String,
    status: StatusPillSpec,
) {
    Row(verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = DobiTextPrimary,
                lineHeight = 32.sp,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Class $grade • $board",
                fontSize = 14.sp,
                color = DobiTextSecondary,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = status.bg,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(status.tint),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = status.text,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = status.tint,
                    )
                }
            }
        }
        Image(
            painter = painterResource(id = R.drawable.dashboard_hero),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(130.dp),
        )
    }
}

@Composable
private fun InsightStatTile(
    title: String,
    subtitle: String,
    hint: String,
    icon: ImageVector,
    tint: Color,
    tintBg: Color,
    cardBg: Color,
    titleColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(tintBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = titleColor,
                lineHeight = 16.sp,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = DobiTextPrimary,
                lineHeight = 15.sp,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = hint,
                fontSize = 11.sp,
                color = DobiTextSecondary,
                lineHeight = 14.sp,
            )
        }
    }
}

// ── Tone palette for dynamic LearningInsight rendering ──────────────────────
// Soft pastel bg + a single accent color per tone. Even SOFT_RED stays calm
// (warm coral rather than aggressive red), per the design brief.
private data class InsightPalette(
    val bg: Color,
    val accent: Color,
    val dividerColor: Color,
    val iconCircleBg: Color,
)

private fun InsightTone.palette(): InsightPalette = when (this) {
    InsightTone.POSITIVE -> InsightPalette(
        bg = Color(0xFFE6F8EC),
        accent = Color(0xFF16A34A),
        dividerColor = Color(0xFFCDEED7),
        iconCircleBg = Color(0xFFD4F5DF),
    )
    InsightTone.INFO -> InsightPalette(
        bg = InsightCardBg,
        accent = DobiPrimary,
        dividerColor = InsightDivider,
        iconCircleBg = DobiPastelSkyBlue,
    )
    InsightTone.AMBER -> InsightPalette(
        bg = Color(0xFFFFF4DD),
        accent = Color(0xFFB45309),
        dividerColor = Color(0xFFF1E1BD),
        iconCircleBg = Color(0xFFFCE7B5),
    )
    InsightTone.ORANGE -> InsightPalette(
        bg = Color(0xFFFFE9D6),
        accent = Color(0xFFC2410C),
        dividerColor = Color(0xFFF1CFB1),
        iconCircleBg = Color(0xFFFCD3B0),
    )
    InsightTone.SOFT_RED -> InsightPalette(
        bg = Color(0xFFFFE6E1),
        accent = Color(0xFFB91C1C),
        dividerColor = Color(0xFFF1C7C0),
        iconCircleBg = Color(0xFFFCCFC7),
    )
}

private fun headerIconFor(tone: InsightTone): ImageVector = when (tone) {
    InsightTone.POSITIVE -> Icons.Filled.AutoAwesome
    InsightTone.INFO -> Icons.Filled.Psychology
    InsightTone.AMBER, InsightTone.ORANGE, InsightTone.SOFT_RED -> Icons.Outlined.Info
}

@Composable
private fun ParentInsightBlock(insight: LearningInsight) {
    val palette = insight.tone.palette()
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = palette.bg,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(palette.iconCircleBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = headerIconFor(insight.tone),
                        contentDescription = null,
                        tint = palette.accent,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = insight.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.accent,
                        lineHeight = 19.sp,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = insight.body,
                        fontSize = 13.sp,
                        color = DobiTextPrimary,
                        lineHeight = 18.sp,
                    )
                }
            }

            if (insight.helpHint != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(palette.dividerColor),
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(start = 8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(palette.iconCircleBg),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lightbulb,
                            contentDescription = null,
                            tint = palette.accent,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = buildHelpText(insight.helpHint, palette.accent),
                        fontSize = 13.sp,
                        color = DobiTextPrimary,
                        lineHeight = 18.sp,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun buildHelpText(
    action: String,
    accent: Color,
): androidx.compose.ui.text.AnnotatedString {
    return androidx.compose.ui.text.buildAnnotatedString {
        pushStyle(
            androidx.compose.ui.text.SpanStyle(
                color = accent,
                fontWeight = FontWeight.Bold,
            )
        )
        append("What can help: ")
        pop()
        append(action)
    }
}

// ── Activity categories (entry points to ActivityDetailScreen) ───────────────
const val ACTIVITY_CATEGORY_SCHOOL_WORK = "school_work"
const val ACTIVITY_CATEGORY_HOME_WORK = "home_work"
const val ACTIVITY_CATEGORY_SELF_LEARNING = "self_learning"

private data class ActivityCategoryTile(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val tint: Color,
    val tintBg: Color,
)

@Composable
private fun ActivityCategoriesCard(
    onOpenActivity: (categoryKey: String) -> Unit,
) {
    val tiles = listOf(
        ActivityCategoryTile(
            key = ACTIVITY_CATEGORY_SCHOOL_WORK,
            label = "School Work",
            icon = Icons.Filled.School,
            tint = DobiPrimary,
            tintBg = DobiPastelSkyBlue,
        ),
        ActivityCategoryTile(
            key = ACTIVITY_CATEGORY_HOME_WORK,
            label = "Home Work",
            icon = Icons.Filled.EditNote,
            tint = DobiAccentOrange,
            tintBg = DobiPastelYellow,
        ),
        ActivityCategoryTile(
            key = ACTIVITY_CATEGORY_SELF_LEARNING,
            label = "Self Learning",
            icon = Icons.Filled.Psychology,
            tint = DobiSuccess,
            tintBg = DobiSuccessContainer,
        ),
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, DobiBorderMuted),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Explore activity",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = DobiTextPrimary,
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "See what your child has attempted",
                fontSize = 12.sp,
                color = DobiTextSecondary,
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                tiles.forEach { tile ->
                    ActivityCategoryButton(
                        tile = tile,
                        onClick = { onOpenActivity(tile.key) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityCategoryButton(
    tile: ActivityCategoryTile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = tile.tintBg,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = tile.icon,
                    contentDescription = null,
                    tint = tile.tint,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = tile.label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = DobiTextPrimary,
                lineHeight = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

// ── This Week's Learning Pattern ─────────────────────────────────────────────
// Header (title + info + per-child subtitle), 7-day strip with day labels +
// dates + connector + tappable nodes + kind pill, a parent insight banner,
// and a 4-cell legend reference card. Each day node opens a detail sheet
// listing the items the child did that day.

private val PatternLineColor = Color(0xFFC8D1E1)
private val PatternSkippedBg = Color(0xFFE5E7EB)
private val PatternSkippedTint = Color(0xFF9CA3AF)
private val InsightBannerBg = Color(0xFFEFF3FF)
private val InsightBannerBorder = Color(0xFFDCE4F7)

private val WeekDayShortLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

@Composable
private fun WeeklyLearningPattern(
    week: List<DayActivity>,
    firstName: String,
    onOpenItem: (DayItem) -> Unit,
) {
    var selectedDay by remember { mutableStateOf<Int?>(null) }
    val dates = remember { computeWeekDates() }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, DobiBorderMuted),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            WeeklyPatternHeader(firstName = firstName)
            Spacer(modifier = Modifier.height(18.dp))
            DayStrip(
                week = week,
                dates = dates,
                onDayClick = { selectedDay = it },
            )
            Spacer(modifier = Modifier.height(18.dp))
            WeeklyInsightBanner(week = week)
            Spacer(modifier = Modifier.height(12.dp))
            WeeklyLegendCard()
        }
    }

    selectedDay?.let { index ->
        DayDetailSheet(
            dayIndex = index,
            activity = week.getOrElse(index) { DayActivity() },
            onDismiss = { selectedDay = null },
            onItemClick = { item ->
                selectedDay = null
                onOpenItem(item)
            },
        )
    }
}

@Composable
private fun WeeklyPatternHeader(firstName: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "This Week's Learning Pattern",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = DobiTextPrimary,
        )
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = DobiTextTertiary,
            modifier = Modifier.size(16.dp),
        )
    }
    Spacer(modifier = Modifier.height(2.dp))
    val subjectName = firstName.ifBlank { "your child" }
    Text(
        text = "See how $subjectName learned this week",
        fontSize = 12.sp,
        color = DobiTextSecondary,
    )
}

// Zigzag offset between even and odd node rows.
private val ZigzagOffset = 22.dp
private val DayNodeSize = 48.dp

@Composable
private fun DayStrip(
    week: List<DayActivity>,
    dates: List<String>,
    onDayClick: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // ── Day name + date headers ──────────────────────────────────────────
        Row(modifier = Modifier.fillMaxWidth()) {
            WeekDayShortLabels.forEachIndexed { i, name ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = DobiTextPrimary,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = dates.getOrElse(i) { "" },
                        fontSize = 11.sp,
                        color = DobiTextSecondary,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        // ── Zigzag node strip with connector ─────────────────────────────────
        ZigzagNodeStrip(week = week, onDayClick = onDayClick)
    }
}

@Composable
private fun ZigzagNodeStrip(
    week: List<DayActivity>,
    onDayClick: (Int) -> Unit,
) {
    val stripHeight = DayNodeSize + ZigzagOffset
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(stripHeight),
    ) {
        // Connector — cubic Bezier through node centers. Even columns sit at
        // the top of the strip (their centers at y = node-radius), odd
        // columns sit at the bottom (centers at y = stripHeight - node-radius).
        Canvas(modifier = Modifier.matchParentSize()) {
            val nodeRadiusPx = with(density) { (DayNodeSize / 2).toPx() }
            val evenY = nodeRadiusPx
            val oddY = size.height - nodeRadiusPx
            val slot = size.width / WeekDayShortLabels.size.toFloat()
            val centers = WeekDayShortLabels.indices.map { i ->
                Offset(
                    x = (i + 0.5f) * slot,
                    y = if (i % 2 == 0) evenY else oddY,
                )
            }
            val path = Path()
            centers.forEachIndexed { i, p ->
                if (i == 0) {
                    path.moveTo(p.x, p.y)
                } else {
                    val prev = centers[i - 1]
                    val midX = (prev.x + p.x) / 2f
                    path.cubicTo(midX, prev.y, midX, p.y, p.x, p.y)
                }
            }
            drawPath(
                path = path,
                color = PatternLineColor,
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        intervals = floatArrayOf(3.dp.toPx(), 5.dp.toPx()),
                        phase = 0f,
                    ),
                    cap = StrokeCap.Round,
                ),
            )
        }

        Row(modifier = Modifier.matchParentSize()) {
            week.forEachIndexed { i, day ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onDayClick(i) },
                    verticalArrangement = if (i % 2 == 0) Arrangement.Top else Arrangement.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    DayNodeLarge(day.kind)
                }
            }
        }
    }
}

@Composable
private fun DayNodeLarge(kind: DayKind) {
    when (kind) {
        DayKind.REVISION -> NodeFilled(
            size = 48.dp,
            bg = DobiPrimary,
            icon = Icons.Filled.Edit,
            iconSize = 22.dp,
        )
        DayKind.HOMEWORK -> NodeFilled(
            size = 48.dp,
            bg = DobiSuccess,
            icon = Icons.AutoMirrored.Filled.MenuBook,
            iconSize = 22.dp,
        )
        DayKind.NONE -> NodeFilled(
            size = 48.dp,
            bg = PatternSkippedBg,
            icon = Icons.Filled.Remove,
            iconTint = PatternSkippedTint,
            iconSize = 22.dp,
        )
        DayKind.BOTH -> NodeRinged(
            size = 48.dp,
            ringColor = DobiPrimary,
            innerBg = DobiSuccess,
            icon = Icons.AutoMirrored.Filled.MenuBook,
            iconSize = 18.dp,
        )
    }
}

@Composable
private fun NodeFilled(
    size: Dp,
    bg: Color,
    icon: ImageVector,
    iconTint: Color = Color.White,
    iconSize: Dp = 22.dp,
) {
    Box(
        modifier = Modifier
            .size(size)
            .shadow(
                elevation = 3.dp,
                shape = CircleShape,
                ambientColor = bg,
                spotColor = bg,
            )
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
private fun NodeRinged(
    size: Dp,
    ringColor: Color,
    innerBg: Color,
    icon: ImageVector,
    iconSize: Dp = 18.dp,
) {
    Box(
        modifier = Modifier
            .size(size)
            .shadow(
                elevation = 3.dp,
                shape = CircleShape,
                ambientColor = ringColor,
                spotColor = ringColor,
            )
            .border(width = 2.5.dp, color = ringColor, shape = CircleShape)
            .padding(4.dp)
            .clip(CircleShape)
            .background(innerBg),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
private fun WeeklyInsightBanner(week: List<DayActivity>) {
    val (title, body) = remember(week) { derivePatternInsight(week) }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = InsightBannerBg,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, InsightBannerBorder),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, InsightBannerBorder, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = DobiPrimary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = DobiTextPrimary,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = body,
                    fontSize = 12.sp,
                    color = DobiTextSecondary,
                    lineHeight = 16.sp,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Filled.AssignmentTurnedIn,
                contentDescription = null,
                tint = DobiPrimary,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

@Composable
private fun WeeklyLegendCard() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, DobiBorderMuted),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LegendCell(
                modifier = Modifier.weight(1f),
                kind = DayKind.REVISION,
                title = "Revision",
                description = "Old topics revised and practiced",
            )
            LegendCell(
                modifier = Modifier.weight(1f),
                kind = DayKind.HOMEWORK,
                title = "Homework",
                description = "New learning or homework done",
            )
            LegendCell(
                modifier = Modifier.weight(1f),
                kind = DayKind.BOTH,
                title = "Both",
                description = "Both learning and revision completed",
            )
            LegendCell(
                modifier = Modifier.weight(1f),
                kind = DayKind.NONE,
                title = "No Activity",
                description = "No learning activity recorded",
            )
        }
    }
}

@Composable
private fun LegendCell(
    modifier: Modifier,
    kind: DayKind,
    title: String,
    description: String,
) {
    Column(modifier = modifier) {
        when (kind) {
            DayKind.REVISION -> NodeFilled(
                size = 28.dp,
                bg = DobiPrimary,
                icon = Icons.Filled.Edit,
                iconSize = 14.dp,
            )
            DayKind.HOMEWORK -> NodeFilled(
                size = 28.dp,
                bg = DobiSuccess,
                icon = Icons.AutoMirrored.Filled.MenuBook,
                iconSize = 14.dp,
            )
            DayKind.NONE -> NodeFilled(
                size = 28.dp,
                bg = PatternSkippedBg,
                icon = Icons.Filled.Remove,
                iconTint = PatternSkippedTint,
                iconSize = 14.dp,
            )
            DayKind.BOTH -> NodeRinged(
                size = 28.dp,
                ringColor = DobiPrimary,
                innerBg = DobiSuccess,
                icon = Icons.AutoMirrored.Filled.MenuBook,
                iconSize = 10.dp,
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = DobiTextPrimary,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = description,
            fontSize = 10.sp,
            color = DobiTextSecondary,
            lineHeight = 13.sp,
        )
    }
}

private fun computeWeekDates(): List<String> {
    val cal = java.util.Calendar.getInstance()
    // Calendar.DAY_OF_WEEK: SUNDAY=1, MONDAY=2, ..., SATURDAY=7.
    // We want offsetFromMonday in 0..6 (Mon..Sun).
    val offsetFromMonday = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
    cal.add(java.util.Calendar.DAY_OF_MONTH, -offsetFromMonday)
    val fmt = java.text.SimpleDateFormat("d MMM", java.util.Locale.getDefault())
    return (0 until 7).map {
        val s = fmt.format(cal.time)
        cal.add(java.util.Calendar.DAY_OF_MONTH, 1)
        s
    }
}

private fun derivePatternInsight(week: List<DayActivity>): Pair<String, String> {
    if (week.isEmpty()) return "Nothing yet" to "No activity recorded this week."
    val bothDays = week.indices.filter { week[it].kind == DayKind.BOTH }
    val revOnly = week.indices.filter { week[it].kind == DayKind.REVISION }
    val hwOnly = week.indices.filter { week[it].kind == DayKind.HOMEWORK }
    val skipped = week.indices.filter { week[it].kind == DayKind.NONE }

    return when {
        bothDays.size >= 2 -> {
            val names = bothDays.joinToString(" & ") {
                WeekDayShortLabels.getOrElse(it) { "" }
            }
            "Great balance this week!" to
                "${bothDays.size} balanced learning days ($names). Keep up the good work!"
        }
        bothDays.size == 1 && revOnly.size + hwOnly.size >= 3 -> {
            val name = WeekDayShortLabels.getOrElse(bothDays[0]) { "one day" }
            "Strong week" to "Balanced learning on $name with steady activity around it."
        }
        revOnly.size >= 4 -> "Strong revision habit" to
            "Revision happened on ${revOnly.size} days — try adding a homework day."
        hwOnly.size >= 4 && revOnly.isEmpty() && bothDays.isEmpty() ->
            "Homework on track" to
                "Most days had homework. Add a short revision to balance learning."
        skipped.size >= 4 -> "Several skipped days" to
            "${skipped.size} days had no learning activity. Try one small revision daily."
        else -> "Mixed week" to "Keep building daily habits — even short sessions help."
    }
}

// ── Day detail bottom sheet ──────────────────────────────────────────────────
private val WeekDayFullLabels = listOf(
    "Monday", "Tuesday", "Wednesday", "Thursday",
    "Friday", "Saturday", "Sunday",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayDetailSheet(
    dayIndex: Int,
    activity: DayActivity,
    onDismiss: () -> Unit,
    onItemClick: (DayItem) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp),
        ) {
            Text(
                text = WeekDayFullLabels.getOrElse(dayIndex) { "Day" },
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = DobiTextPrimary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = daySummaryText(activity),
                fontSize = 13.sp,
                color = DobiTextSecondary,
            )
            Spacer(modifier = Modifier.height(20.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                when {
                    activity.revisions.isEmpty() && activity.homeworks.isEmpty() ->
                        DayEmptyState()
                    else -> {
                        if (activity.revisions.isNotEmpty()) {
                            DaySection(
                                title = "Revision",
                                accent = DobiPrimary,
                                accentBg = DobiPastelSkyBlue,
                                icon = Icons.Filled.Edit,
                                items = activity.revisions,
                                onItemClick = onItemClick,
                            )
                        }
                        if (activity.revisions.isNotEmpty() && activity.homeworks.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(18.dp))
                        }
                        if (activity.homeworks.isNotEmpty()) {
                            DaySection(
                                title = "Homework",
                                accent = DobiSuccess,
                                accentBg = DobiSuccessContainer,
                                icon = Icons.AutoMirrored.Filled.MenuBook,
                                items = activity.homeworks,
                                onItemClick = onItemClick,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

private fun daySummaryText(activity: DayActivity): String {
    val rev = activity.revisions.size
    val hw = activity.homeworks.size
    return when {
        rev == 0 && hw == 0 -> "No activity recorded."
        rev > 0 && hw > 0 -> "$rev ${pluralize("revision", rev)} • $hw ${pluralize("homework", hw)}"
        rev > 0 -> "$rev ${pluralize("revision", rev)} done"
        else -> "$hw ${pluralize("homework", hw)} done"
    }
}

private fun pluralize(word: String, count: Int): String =
    if (count == 1) word else "${word}s"

@Composable
private fun DaySection(
    title: String,
    accent: Color,
    accentBg: Color,
    icon: ImageVector,
    items: List<DayItem>,
    onItemClick: (DayItem) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(accentBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = accent,
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        items.forEachIndexed { i, item ->
            DayItemRow(item = item, onClick = { onItemClick(item) })
            if (i < items.lastIndex) Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun DayItemRow(item: DayItem, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFAFBFD),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        border = BorderStroke(1.dp, DobiBorderMuted),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DobiTextPrimary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (item.subject.isNotBlank()) {
                        Text(
                            text = item.subject,
                            fontSize = 12.sp,
                            color = DobiTextSecondary,
                        )
                    }
                    if (item.typeTag.isNotBlank()) {
                        DayItemTypeTag(item.typeTag)
                    }
                }
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = DobiTextTertiary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun DayItemTypeTag(tag: String) {
    val (tint, bg) = when (tag) {
        "Quiz"        -> Color(0xFF3B7BFF) to Color(0xFFEAF0FF)
        "Write Answer"-> Color(0xFF7C3AED) to Color(0xFFF3EEFF)
        "Class Work"  -> Color(0xFF0891B2) to Color(0xFFE0F7FA)
        "Self Study"  -> Color(0xFF059669) to Color(0xFFE6F7F1)
        "Homework"    -> Color(0xFFD97706) to Color(0xFFFFF3E0)
        else          -> DobiTextSecondary to Color(0xFFF3F4F6)
    }
    Box(
        modifier = androidx.compose.ui.Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 7.dp, vertical = 2.dp),
    ) {
        Text(
            text = tag,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = tint,
        )
    }
}

@Composable
private fun DayEmptyState() {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFFAFBFD),
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, DobiBorderMuted),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Filled.Remove,
                contentDescription = null,
                tint = PatternSkippedTint,
                modifier = Modifier.size(28.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No activity this day",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = DobiTextPrimary,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Nothing was uploaded or attempted.",
                fontSize = 12.sp,
                color = DobiTextSecondary,
            )
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, fontSize = 13.sp, color = DobiTextSecondary)
    }
}

