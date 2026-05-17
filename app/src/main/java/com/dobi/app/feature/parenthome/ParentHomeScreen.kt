package com.dobi.app.feature.parenthome

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dobi.app.R
import com.dobi.app.domain.model.Child
import com.dobi.app.domain.model.ChildEngagement
import com.dobi.app.domain.model.ParentTask
import com.dobi.app.domain.model.ParentTaskStatus
import com.dobi.app.domain.model.ParentTaskType
import com.dobi.app.core.ui.theme.DobiError
import com.dobi.app.core.ui.theme.DobiPrimary
import com.dobi.app.core.ui.theme.DobiPrimaryContainer
import com.dobi.app.core.ui.theme.DobiPrimarySoft
import com.dobi.app.core.ui.theme.DobiSuccess
import com.dobi.app.core.ui.theme.DobiSuccessContainer
import com.dobi.app.core.ui.theme.DobiTextPrimary
import com.dobi.app.core.ui.theme.DobiTextSecondary
import com.dobi.app.core.ui.theme.SFProBold
import com.dobi.app.core.ui.theme.SFProMedium
import com.dobi.app.core.ui.theme.SFProRegular
import com.dobi.app.core.ui.theme.SFProSemiBold
import com.dobi.app.feature.parentinsights.ParentInsightsContent

private val PageBg = Color(0xFFF3F6FD)
private val StatOrange = Color(0xFFF59E0B)
private val StatOrangeBg = Color(0xFFFFF3D6)
private val StatPink = Color(0xFFEF4444)
private val StatPinkBg = Color(0xFFFFE4E6)
private val DividerColor = Color(0xFFF0F2F7)
private val AvatarPurple = Color(0xFF8B5CF6)
private val AvatarPurpleBg = Color(0xFFEDE9FE)
private val ProgressTrack = Color(0xFFE6EAF5)

private enum class ParentTab { CHILDREN, INSIGHTS, SETTINGS }

@Composable
fun ParentHomeScreen(
    onNavigateToChildHome: (childId: String) -> Unit,
    onNavigateToAddChild: () -> Unit,
    onRedirectToAddChild: () -> Unit,
    onNavigateToRoleSelection: () -> Unit,
    onNavigateToParentTask: (taskId: String) -> Unit = {},
    viewModel: ParentHomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pairingDialog by viewModel.pairingDialog.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(ParentTab.CHILDREN.ordinal) }

    LaunchedEffect(Unit) {
        viewModel.navToRoleSelection.collect { onNavigateToRoleSelection() }
    }

    LaunchedEffect(uiState.isLoading, uiState.children.size) {
        if (!uiState.isLoading && uiState.children.isEmpty()) {
            onRedirectToAddChild()
        }
    }

    if (uiState.children.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().background(PageBg))
        return
    }

    val activeTab = ParentTab.values()[selectedTab.coerceIn(0, ParentTab.values().lastIndex)]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBg)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (activeTab) {
                ParentTab.CHILDREN -> ChildrenTab(
                    parentName = uiState.profile?.name ?: "",
                    children = uiState.children,
                    parentTasks = uiState.parentTasks,
                    engagements = uiState.engagements,
                    onNavigateToChildHome = onNavigateToChildHome,
                    onNavigateToParentTask = onNavigateToParentTask,
                    onAddChild = onNavigateToAddChild,
                    onPairDevice = viewModel::onPairDeviceTapped
                )
                ParentTab.INSIGHTS -> InsightsTabShell(
                    onAddChild = onNavigateToAddChild
                )
                ParentTab.SETTINGS -> SettingsTabShell(
                    onSignOut = viewModel::signOut
                )
            }
        }

        BottomNav(
            selectedTab = activeTab,
            onTabSelected = { selectedTab = it.ordinal }
        )
    }

    PairingDialog(state = pairingDialog, onDismiss = viewModel::dismissPairingDialog)
}

// ── Children tab ─────────────────────────────────────────────────────────────

@Composable
private fun ChildrenTab(
    parentName: String,
    children: List<Child>,
    parentTasks: List<ParentTask>,
    engagements: Map<String, ChildEngagement>,
    onNavigateToChildHome: (String) -> Unit,
    onNavigateToParentTask: (String) -> Unit,
    onAddChild: () -> Unit,
    onPairDevice: (Child) -> Unit
) {
    val actionableTasks = parentTasks.filter {
        it.status == ParentTaskStatus.REQUESTED || it.status == ParentTaskStatus.PENDING_REVIEW
    }
    val childById: Map<String, Child> = remember(children) {
        children.associateBy { it.id }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        item {
            HeaderRow(parentName = parentName)
            Spacer(Modifier.height(4.dp))
        }

        if (actionableTasks.isNotEmpty()) {
            item {
                TasksCard(
                    actionableTasks = actionableTasks,
                    childById = childById,
                    onNavigateToParentTask = onNavigateToParentTask,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(Modifier.height(24.dp))
            }
        } else {
            item { Spacer(Modifier.height(8.dp)) }
        }

        item {
            ChildrenSectionHeader(
                childrenCount = children.size,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(14.dp))
        }

        items(children, key = { it.id }) { child ->
            val activeTasksCount = parentTasks.count { task ->
                task.childId == child.id && task.status in setOf(
                    ParentTaskStatus.REQUESTED,
                    ParentTaskStatus.ASSIGNED,
                    ParentTaskStatus.PENDING_REVIEW
                )
            }
            val engagement = engagements[child.id]
            ChildStatCard(
                child = child,
                activeTasksCount = activeTasksCount,
                weeklyDaysActive = engagement?.daysActiveThisWeek ?: 0,
                isActiveToday = engagement?.isActiveToday ?: false,
                accuracyPct = engagement?.accuracyPct,
                onClick = { onNavigateToChildHome(child.id) },
                onLongPress = { onPairDevice(child) },
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(14.dp))
        }

        item {
            Spacer(Modifier.height(4.dp))
            AddMoreChildrenCard(
                onAddChild = onAddChild,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
    }
}

// ── Header row (avatar + greeting + bell) ────────────────────────────────────

@Composable
private fun HeaderRow(parentName: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 52.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(AvatarPurpleBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = parentName.firstOrNull()?.uppercaseChar()?.toString() ?: "P",
                    fontSize = 22.sp,
                    fontWeight = SFProBold,
                    color = AvatarPurple
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Hello, ${parentName.substringBefore(" ").ifBlank { "there" }}!",
                        fontSize = 20.sp,
                        fontWeight = SFProBold,
                        color = DobiTextPrimary,
                        maxLines = 1
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("👋", fontSize = 20.sp)
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Track your children's learning",
                    fontSize = 12.sp,
                    fontWeight = SFProRegular,
                    color = DobiTextSecondary,
                    maxLines = 2
                )
            }

            Spacer(Modifier.width(8.dp))

            Image(
                painter = painterResource(R.drawable.parent_home_illustration),
                contentDescription = null,
                modifier = Modifier.height(96.dp)
            )
        }

        BellButton(
            modifier = Modifier.align(Alignment.TopEnd)
        )
    }
}

@Composable
private fun BellButton(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(44.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Notifications",
                tint = DobiTextPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-6).dp, y = 6.dp)
                .size(9.dp)
                .clip(CircleShape)
                .background(DobiError)
        )
    }
}

// ── Tasks card (with "View all") ─────────────────────────────────────────────

@Composable
private fun TasksCard(
    actionableTasks: List<ParentTask>,
    childById: Map<String, Child>,
    onNavigateToParentTask: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val firstTask = actionableTasks.first()
    val moreCount = (actionableTasks.size - 1).coerceAtLeast(0)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Tasks needing your attention",
                    fontSize = 15.sp,
                    fontWeight = SFProBold,
                    color = DobiTextPrimary,
                    modifier = Modifier.weight(1f)
                )
                if (moreCount > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onNavigateToParentTask(firstTask.id) }
                            )
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "View all",
                            fontSize = 13.sp,
                            fontWeight = SFProSemiBold,
                            color = DobiPrimary
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = DobiPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            ParentActionableTaskRow(
                task = firstTask,
                childName = childById[firstTask.childId]?.name.orEmpty(),
                onClick = { onNavigateToParentTask(firstTask.id) }
            )
        }
    }
}

// ── Children section header ──────────────────────────────────────────────────

@Composable
private fun ChildrenSectionHeader(
    childrenCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Your Children",
                fontSize = 20.sp,
                fontWeight = SFProBold,
                color = DobiTextPrimary
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "View progress, insights and help them grow",
                fontSize = 13.sp,
                fontWeight = SFProRegular,
                color = DobiTextSecondary
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(DobiPrimaryContainer)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.People,
                contentDescription = null,
                tint = DobiPrimary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "$childrenCount profile${if (childrenCount > 1) "s" else ""}",
                fontSize = 12.sp,
                fontWeight = SFProSemiBold,
                color = DobiPrimary
            )
        }
    }
}

// ── Child stat card (with progress ring) ─────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChildStatCard(
    child: Child,
    activeTasksCount: Int,
    weeklyDaysActive: Int,
    isActiveToday: Boolean,
    accuracyPct: Int?,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val avatarEmoji = when (child.gender) {
        "girl" -> "👧"
        "other" -> "🧒"
        else -> "👦"
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = onLongPress
            ),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 3.dp,
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(DobiPrimarySoft),
                    contentAlignment = Alignment.Center
                ) {
                    Text(avatarEmoji, fontSize = 36.sp)
                }

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = child.name,
                        fontSize = 17.sp,
                        fontWeight = SFProBold,
                        color = DobiTextPrimary,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(6.dp))
                    Row {
                        InfoChip(text = "Class ${child.grade}")
                        Spacer(Modifier.width(6.dp))
                        InfoChip(text = child.board)
                    }
                }

                Spacer(Modifier.width(8.dp))

                ProgressRing(percent = accuracyPct)

                Spacer(Modifier.width(4.dp))

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = DobiPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = DividerColor, thickness = 1.dp)
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                TasksTile(
                    activeCount = activeTasksCount,
                    modifier = Modifier.weight(1f)
                )
                VerticalSeparator()
                WeeklyStreakTile(
                    daysActive = weeklyDaysActive,
                    modifier = Modifier.weight(1f)
                )
                VerticalSeparator()
                TodayLearningTile(
                    isActiveToday = isActiveToday,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ProgressRing(percent: Int?) {
    val pct = (percent ?: 0).coerceIn(0, 100)
    val fraction = pct / 100f
    Box(
        modifier = Modifier.size(54.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(54.dp)) {
            val strokeWidthPx = 5.dp.toPx()
            val inset = strokeWidthPx / 2f
            val arcSize = androidx.compose.ui.geometry.Size(
                size.width - strokeWidthPx,
                size.height - strokeWidthPx
            )
            val arcTopLeft = androidx.compose.ui.geometry.Offset(inset, inset)
            // Track
            drawArc(
                color = ProgressTrack,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )
            // Progress
            if (fraction > 0f) {
                drawArc(
                    color = DobiPrimary,
                    startAngle = -90f,
                    sweepAngle = 360f * fraction,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )
            }
        }
        Text(
            text = if (percent != null) "$pct%" else "—",
            fontSize = 12.sp,
            fontWeight = SFProBold,
            color = DobiTextPrimary
        )
    }
}

@Composable
private fun InfoChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DobiPrimaryContainer)
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = SFProSemiBold,
            color = DobiPrimary
        )
    }
}

// ── Stat tile: Tasks by parents ──────────────────────────────────────────────

@Composable
private fun TasksTile(activeCount: Int, modifier: Modifier = Modifier) {
    val hasPending = activeCount > 0
    val accent = if (hasPending) StatPink else DobiSuccess
    val accentBg = if (hasPending) StatPinkBg else DobiSuccessContainer

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(accentBg),
                contentAlignment = Alignment.Center
            ) {
                if (hasPending) {
                    Text(
                        text = activeCount.toString(),
                        fontSize = 11.sp,
                        fontWeight = SFProBold,
                        color = accent
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(Modifier.width(6.dp))
            Text(
                text = if (hasPending) "Active" else "All done",
                fontSize = 13.sp,
                fontWeight = SFProSemiBold,
                color = DobiTextPrimary
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Tasks by parents",
            fontSize = 11.sp,
            fontWeight = SFProRegular,
            color = DobiTextSecondary,
            maxLines = 2
        )
    }
}

// ── Stat tile: Weekly streak ─────────────────────────────────────────────────

@Composable
private fun WeeklyStreakTile(daysActive: Int, modifier: Modifier = Modifier) {
    val clamped = daysActive.coerceIn(0, 7)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(7) { idx ->
                val active = idx < clamped
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (active) DobiPrimary else DividerColor)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "$clamped of 7",
            fontSize = 13.sp,
            fontWeight = SFProSemiBold,
            color = DobiTextPrimary
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "Streaks this week",
            fontSize = 11.sp,
            fontWeight = SFProRegular,
            color = DobiTextSecondary,
            maxLines = 2
        )
    }
}

// ── Stat tile: Today's learning ──────────────────────────────────────────────

@Composable
private fun TodayLearningTile(isActiveToday: Boolean, modifier: Modifier = Modifier) {
    val accent = if (isActiveToday) DobiSuccess else StatOrange
    val accentBg = if (isActiveToday) DobiSuccessContainer else StatOrangeBg
    val icon = if (isActiveToday) Icons.Default.Check else Icons.Default.Schedule

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(accentBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(Modifier.width(6.dp))
            Text(
                text = if (isActiveToday) "Done" else "Not yet",
                fontSize = 13.sp,
                fontWeight = SFProSemiBold,
                color = DobiTextPrimary
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Today's learning",
            fontSize = 11.sp,
            fontWeight = SFProRegular,
            color = DobiTextSecondary,
            maxLines = 2
        )
    }
}

@Composable
private fun VerticalSeparator() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(50.dp)
            .background(DividerColor)
    )
}

// ── Add more children card ───────────────────────────────────────────────────

@Composable
private fun AddMoreChildrenCard(
    onAddChild: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = DobiPrimaryContainer,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(DobiPrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Add more children",
                    fontSize = 15.sp,
                    fontWeight = SFProBold,
                    color = DobiTextPrimary
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "Track each child's learning and get personalised insights.",
                    fontSize = 12.sp,
                    fontWeight = SFProRegular,
                    color = DobiTextSecondary,
                    lineHeight = 16.sp
                )
            }

            Spacer(Modifier.width(10.dp))

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(DobiPrimary)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onAddChild
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Add Child",
                    fontSize = 12.sp,
                    fontWeight = SFProSemiBold,
                    color = Color.White
                )
            }
        }
    }
}

// ── Insights / Settings / Calendar tab shells ────────────────────────────────

@Composable
private fun InsightsTabShell(onAddChild: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        SimpleTabHeader(title = "Insights", subtitle = "How your children are learning")
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .offset(y = (-24).dp),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = PageBg,
            shadowElevation = 0.dp
        ) {
            ParentInsightsContent(onAddChild = onAddChild)
        }
    }
}

@Composable
private fun SettingsTabShell(onSignOut: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        SimpleTabHeader(title = "Settings", subtitle = "Account and preferences")
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .offset(y = (-24).dp),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = PageBg,
            shadowElevation = 0.dp
        ) {
            SettingsPlaceholder(onSignOut = onSignOut)
        }
    }
}

@Composable
private fun SimpleTabHeader(title: String, subtitle: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DobiPrimary)
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 48.dp)
    ) {
        Column {
            Text(
                text = title,
                fontSize = 22.sp,
                fontWeight = SFProBold,
                color = Color.White
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                fontWeight = SFProRegular,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun SettingsPlaceholder(onSignOut: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onSignOut
                ),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFE4E6)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = null,
                        tint = DobiError,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sign out",
                        fontSize = 15.sp,
                        fontWeight = SFProSemiBold,
                        color = DobiTextPrimary
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "You'll need to sign in again to access this parent dashboard.",
                        fontSize = 12.sp,
                        fontWeight = SFProRegular,
                        color = DobiTextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

// ── Bottom navigation ────────────────────────────────────────────────────────

@Composable
private fun BottomNav(
    selectedTab: ParentTab,
    onTabSelected: (ParentTab) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 10.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavTab(
                icon = Icons.Default.People,
                label = "Children",
                active = selectedTab == ParentTab.CHILDREN,
                onClick = { onTabSelected(ParentTab.CHILDREN) },
                modifier = Modifier.weight(1f)
            )
            NavTab(
                icon = Icons.Default.BarChart,
                label = "Insights",
                active = selectedTab == ParentTab.INSIGHTS,
                onClick = { onTabSelected(ParentTab.INSIGHTS) },
                modifier = Modifier.weight(1f)
            )
            NavTab(
                icon = Icons.Default.Settings,
                label = "Settings",
                active = selectedTab == ParentTab.SETTINGS,
                onClick = { onTabSelected(ParentTab.SETTINGS) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun NavTab(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (active) DobiPrimaryContainer else Color.Transparent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (active) DobiPrimary else DobiTextSecondary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (active) SFProSemiBold else SFProMedium,
            color = if (active) DobiPrimary else DobiTextSecondary
        )
    }
}

// ── Pairing dialog ───────────────────────────────────────────────────────────

@Composable
private fun PairingDialog(
    state: ParentHomeViewModel.PairingDialogState,
    onDismiss: () -> Unit
) {
    if (state is ParentHomeViewModel.PairingDialogState.Hidden) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            val name = when (state) {
                is ParentHomeViewModel.PairingDialogState.Loading -> state.child.name
                is ParentHomeViewModel.PairingDialogState.Shown -> state.child.name
                is ParentHomeViewModel.PairingDialogState.Error -> state.child.name
                else -> ""
            }
            Text(
                text = "Pair device for $name",
                fontSize = 18.sp,
                fontWeight = SFProBold,
                color = DobiTextPrimary
            )
        },
        text = {
            Column {
                when (state) {
                    is ParentHomeViewModel.PairingDialogState.Loading -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(color = DobiPrimary, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("Generating code…", fontSize = 14.sp, color = DobiTextSecondary)
                        }
                    }
                    is ParentHomeViewModel.PairingDialogState.Shown -> {
                        Text(
                            "Enter this 6-digit code on the child's device. It expires in 10 minutes.",
                            fontSize = 14.sp,
                            color = DobiTextSecondary,
                            lineHeight = 20.sp
                        )
                        Spacer(Modifier.height(20.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(DobiPrimaryContainer)
                                .padding(vertical = 18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = state.code.chunked(3).joinToString("  "),
                                fontSize = 32.sp,
                                fontWeight = SFProBold,
                                color = DobiPrimary,
                                letterSpacing = 4.sp
                            )
                        }
                    }
                    is ParentHomeViewModel.PairingDialogState.Error -> {
                        Text(state.message, fontSize = 14.sp, color = DobiError)
                    }
                    else -> {}
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = DobiPrimary, fontWeight = SFProSemiBold)
            }
        },
        containerColor = Color.White
    )
}

// ── Parent task row (inside Tasks card) ──────────────────────────────────────

@Composable
private fun ParentActionableTaskRow(
    task: ParentTask,
    childName: String,
    onClick: () -> Unit
) {
    val (accentBg, accentFg, label) = when (task.status) {
        ParentTaskStatus.REQUESTED -> Triple(
            Color(0xFFFFF4E0), Color(0xFFB45309), "New request"
        )
        ParentTaskStatus.PENDING_REVIEW -> Triple(
            Color(0xFFEFF6FF), DobiPrimary, "Needs review"
        )
        else -> Triple(Color(0xFFF1F5F9), DobiTextSecondary, "")
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(accentBg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = task.subjectIcon.ifBlank { "📚" },
                fontSize = 24.sp
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = parentTaskHeadline(task),
                color = DobiTextPrimary,
                fontSize = 14.sp,
                fontWeight = SFProBold,
                lineHeight = 18.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = buildString {
                    if (childName.isNotBlank()) append("$childName · ")
                    append(if (task.isUnitScope) "Lesson" else "Concept")
                    if (task.subject.isNotBlank()) append(" · ${task.subject}")
                },
                color = DobiTextSecondary,
                fontSize = 12.sp
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(accentBg)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = SFProSemiBold,
                color = accentFg
            )
        }
    }
}

private fun parentTaskHeadline(task: ParentTask): String {
    val scope = task.displayName.ifBlank { "a topic" }
    return when (task.status) {
        ParentTaskStatus.REQUESTED -> "Wants to study: $scope"
        ParentTaskStatus.PENDING_REVIEW -> when (task.type) {
            ParentTaskType.REVISE_CHAPTER -> "Submitted revision: $scope"
            ParentTaskType.WRITE_QA -> "Submitted Q&A: $scope"
            ParentTaskType.OUR_QUESTIONS -> "Quiz done: $scope"
            ParentTaskType.CUSTOM -> "Submitted: $scope"
            null -> "Submitted: $scope"
        }
        else -> scope
    }
}
