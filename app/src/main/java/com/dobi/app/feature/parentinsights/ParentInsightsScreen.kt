package com.dobi.app.feature.parentinsights

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.dobi.app.domain.model.ActivityItem
import com.dobi.app.domain.model.ActivityKind
import com.dobi.app.domain.model.Child
import com.dobi.app.domain.model.ChildInsights
import com.dobi.app.domain.model.InsightsRange
import com.dobi.app.domain.model.SubjectInsight

private val PageBg = Color(0xFFF3F6FD)
private val DividerColor = Color(0xFFF0F2F7)
private val StatPurple = Color(0xFF8B5CF6)
private val StatPurpleBg = Color(0xFFEDE9FE)
private val StatOrange = Color(0xFFF59E0B)
private val StatOrangeBg = Color(0xFFFFF3D6)
private val StatTeal = Color(0xFF14B8A6)
private val StatTealBg = Color(0xFFD1FAF5)

/**
 * Embeddable insights content — no header, no bottom nav. Designed to sit
 * inside the ParentHome shell when the Insights bottom-nav tab is selected.
 */
@Composable
fun ParentInsightsContent(
    onAddChild: () -> Unit,
    viewModel: ParentInsightsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when {
        uiState.isLoading -> LoadingState()
        uiState.children.isEmpty() -> EmptyState(onAddChild = onAddChild)
        else -> Content(
            children = uiState.children,
            selectedChildId = uiState.selectedChildId,
            range = uiState.range,
            insights = uiState.insights,
            onChildSelected = viewModel::onChildSelected,
            onRangeSelected = viewModel::onRangeSelected,
            onAddChild = onAddChild
        )
    }
}

// ── Content (main scroll) ─────────────────────────────────────────────────────

@Composable
private fun Content(
    children: List<Child>,
    selectedChildId: String?,
    range: InsightsRange,
    insights: ChildInsights,
    onChildSelected: (String) -> Unit,
    onRangeSelected: (InsightsRange) -> Unit,
    onAddChild: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 24.dp, bottom = 32.dp)
    ) {
        // Child selector — horizontal scroll
        item {
            ChildPills(
                children = children,
                selectedChildId = selectedChildId,
                onChildSelected = onChildSelected,
                onAddChild = onAddChild
            )
            Spacer(Modifier.height(20.dp))
        }

        // Range filter
        item {
            RangeChips(
                selected = range,
                onSelected = onRangeSelected,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(20.dp))
        }

        // Hero stats — 2x2 grid (built as two rows for simplicity)
        item {
            HeroStatsGrid(
                insights = insights,
                range = range,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(28.dp))
        }

        // Subjects section
        if (insights.subjects.isNotEmpty()) {
            item {
                SectionHeader(title = "Subjects", subtitle = "Where the time is going")
                Spacer(Modifier.height(12.dp))
            }
            items(insights.subjects, key = { it.name }) { s ->
                SubjectRow(s, modifier = Modifier.padding(horizontal = 20.dp))
                Spacer(Modifier.height(8.dp))
            }
            item { Spacer(Modifier.height(24.dp)) }
        } else {
            item {
                EmptySection(
                    title = "Subjects",
                    body = "No subject activity in this range yet."
                )
                Spacer(Modifier.height(24.dp))
            }
        }

        // Recent activity
        if (insights.recentActivities.isNotEmpty()) {
            item {
                SectionHeader(title = "Recent activity", subtitle = "Latest first")
                Spacer(Modifier.height(12.dp))
            }
            items(insights.recentActivities, key = {
                "${it.timestampMillis}-${it.kind}-${it.title}"
            }) { item ->
                ActivityRow(item, modifier = Modifier.padding(horizontal = 20.dp))
                Spacer(Modifier.height(8.dp))
            }
        } else {
            item {
                EmptySection(
                    title = "Recent activity",
                    body = "Once your child starts learning, you'll see it here."
                )
            }
        }
    }
}

// ── Child pills ───────────────────────────────────────────────────────────────

@Composable
private fun ChildPills(
    children: List<Child>,
    selectedChildId: String?,
    onChildSelected: (String) -> Unit,
    onAddChild: () -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(children, key = { it.id }) { child ->
            ChildPill(
                child = child,
                isSelected = child.id == selectedChildId,
                onClick = { onChildSelected(child.id) }
            )
        }
        item {
            AddChildPill(onClick = onAddChild)
        }
    }
}

@Composable
private fun ChildPill(child: Child, isSelected: Boolean, onClick: () -> Unit) {
    val avatarEmoji = when (child.gender) {
        "girl" -> "👧"
        "other" -> "🧒"
        else -> "👦"
    }
    val bg = if (isSelected) DobiPrimary else Color.White
    val textColor = if (isSelected) Color.White else DobiTextPrimary

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .border(
                width = if (isSelected) 0.dp else 1.dp,
                color = DividerColor,
                shape = RoundedCornerShape(999.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(start = 6.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) Color.White.copy(alpha = 0.22f)
                    else DobiPrimarySoft
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(avatarEmoji, fontSize = 18.sp)
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = child.name,
            fontSize = 14.sp,
            fontWeight = SFProSemiBold,
            color = textColor,
            maxLines = 1
        )
    }
}

@Composable
private fun AddChildPill(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(DobiPrimaryContainer)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            tint = DobiPrimary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "Add",
            fontSize = 13.sp,
            fontWeight = SFProSemiBold,
            color = DobiPrimary
        )
    }
}

// ── Range chips ───────────────────────────────────────────────────────────────

@Composable
private fun RangeChips(
    selected: InsightsRange,
    onSelected: (InsightsRange) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            InsightsRange.values().forEach { r ->
                RangeChip(
                    label = r.label,
                    isSelected = r == selected,
                    onClick = { onSelected(r) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun RangeChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) DobiPrimary else Color.Transparent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (isSelected) SFProBold else SFProMedium,
            color = if (isSelected) Color.White else DobiTextSecondary
        )
    }
}

// ── Hero stats 2x2 ────────────────────────────────────────────────────────────

@Composable
private fun HeroStatsGrid(
    insights: ChildInsights,
    range: InsightsRange,
    modifier: Modifier = Modifier
) {
    val totalDays = range.days
    val accuracyLabel = insights.accuracyPct?.let { "$it%" } ?: "—"

    Column(modifier = modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HeroStatTile(
                icon = Icons.Default.LocalFireDepartment,
                accent = StatOrange,
                accentBg = StatOrangeBg,
                value = if (range == InsightsRange.TODAY)
                    if (insights.daysActive > 0) "Active" else "Not yet"
                else "${insights.daysActive} / $totalDays",
                label = "Days active",
                modifier = Modifier.weight(1f)
            )
            HeroStatTile(
                icon = Icons.Default.AutoAwesome,
                accent = StatPurple,
                accentBg = StatPurpleBg,
                value = insights.activitiesCompleted.toString(),
                label = "Activities",
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HeroStatTile(
                icon = Icons.Default.Check,
                accent = DobiSuccess,
                accentBg = DobiSuccessContainer,
                value = accuracyLabel,
                label = "Accuracy",
                modifier = Modifier.weight(1f)
            )
            HeroStatTile(
                icon = Icons.Default.Check,
                accent = StatTeal,
                accentBg = StatTealBg,
                value = insights.tasksDone.toString(),
                label = "Tasks done",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun HeroStatTile(
    icon: ImageVector,
    accent: Color,
    accentBg: Color,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(accentBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = SFProBold,
                color = DobiTextPrimary,
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = SFProRegular,
                color = DobiTextSecondary,
                maxLines = 1
            )
        }
    }
}

// ── Section header ────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, subtitle: String? = null) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = SFProBold,
            color = DobiTextPrimary
        )
        if (subtitle != null) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                fontWeight = SFProRegular,
                color = DobiTextSecondary
            )
        }
    }
}

// ── Subject row ───────────────────────────────────────────────────────────────

@Composable
private fun SubjectRow(s: SubjectInsight, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DobiPrimarySoft),
                contentAlignment = Alignment.Center
            ) {
                Text(s.emoji, fontSize = 22.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = s.name,
                    fontSize = 14.sp,
                    fontWeight = SFProSemiBold,
                    color = DobiTextPrimary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${s.topicsCount} topic${if (s.topicsCount == 1) "" else "s"} · ${s.activitiesCount} activit${if (s.activitiesCount == 1) "y" else "ies"}",
                    fontSize = 12.sp,
                    fontWeight = SFProRegular,
                    color = DobiTextSecondary
                )
            }
            if (s.accuracyPct != null) {
                AccuracyPill(s.accuracyPct!!)
            }
        }
    }
}

@Composable
private fun AccuracyPill(pct: Int) {
    val (fg, bg) = when {
        pct >= 80 -> DobiSuccess to DobiSuccessContainer
        pct >= 60 -> StatOrange to StatOrangeBg
        else -> Color(0xFFEF4444) to Color(0xFFFFE4E6)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = "$pct%",
            fontSize = 12.sp,
            fontWeight = SFProBold,
            color = fg
        )
    }
}

// ── Activity row ──────────────────────────────────────────────────────────────

@Composable
private fun ActivityRow(item: ActivityItem, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(DobiPrimarySoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (item.kind) {
                        ActivityKind.QUIZ -> Icons.Default.AutoAwesome
                        ActivityKind.UPLOAD -> Icons.Default.PhotoCamera
                        ActivityKind.WRITTEN -> Icons.Default.Edit
                    },
                    contentDescription = null,
                    tint = DobiPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = 14.sp,
                    fontWeight = SFProSemiBold,
                    color = DobiTextPrimary,
                    maxLines = 1
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = listOfNotNull(
                        kindLabel(item.kind),
                        item.subject.takeIf { it.isNotBlank() },
                        relativeTime(item.timestampMillis)
                    ).joinToString(" · "),
                    fontSize = 12.sp,
                    fontWeight = SFProRegular,
                    color = DobiTextSecondary,
                    maxLines = 1
                )
            }
            if (item.accuracyPct != null) {
                AccuracyPill(item.accuracyPct!!)
            }
        }
    }
}

private fun kindLabel(kind: ActivityKind): String = when (kind) {
    ActivityKind.QUIZ -> "Quiz"
    ActivityKind.UPLOAD -> "Upload"
    ActivityKind.WRITTEN -> "Written"
}

private fun relativeTime(ts: Long): String {
    if (ts <= 0) return ""
    val diff = System.currentTimeMillis() - ts
    val mins = diff / 60_000
    val hrs = mins / 60
    val days = hrs / 24
    return when {
        mins < 1 -> "Just now"
        mins < 60 -> "${mins}m ago"
        hrs < 24 -> "${hrs}h ago"
        days < 7 -> "${days}d ago"
        else -> "${days / 7}w ago"
    }
}

// ── Empty/loading states ──────────────────────────────────────────────────────

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Loading insights…",
            fontSize = 14.sp,
            fontWeight = SFProRegular,
            color = DobiTextSecondary
        )
    }
}

@Composable
private fun EmptyState(onAddChild: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("📊", fontSize = 56.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            "No children yet",
            fontSize = 18.sp,
            fontWeight = SFProBold,
            color = DobiTextPrimary
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Add a child to see their learning insights here.",
            fontSize = 14.sp,
            fontWeight = SFProRegular,
            color = DobiTextSecondary
        )
        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(DobiPrimary)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onAddChild
                )
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "Add a child",
                fontSize = 13.sp,
                fontWeight = SFProSemiBold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun EmptySection(title: String, body: String) {
    Column {
        SectionHeader(title = title)
        Spacer(Modifier.height(12.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(14.dp),
            color = Color.White,
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = DobiTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = body,
                    fontSize = 13.sp,
                    fontWeight = SFProRegular,
                    color = DobiTextSecondary
                )
            }
        }
    }
}
