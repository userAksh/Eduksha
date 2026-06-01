package com.dobi.app.feature.parenttask

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dobi.app.domain.model.Child
import com.dobi.app.domain.model.ParentTask
import com.dobi.app.domain.model.ParentTaskStatus
import com.dobi.app.domain.model.ParentTaskType
import com.dobi.app.core.ui.theme.DobiPrimary
import com.dobi.app.core.ui.theme.DobiPrimarySoft
import com.dobi.app.core.ui.theme.DobiTextPrimary
import com.dobi.app.core.ui.theme.DobiTextSecondary
import com.dobi.app.core.ui.theme.SFProBold
import com.dobi.app.core.ui.theme.SFProRegular
import com.dobi.app.core.ui.theme.SFProSemiBold

private val ScreenBg = Color(0xFFF5F7FB)

// ── Change 3: time-ago helper ────────────────────────────────────────────────

fun timeAgo(ms: Long): String {
    if (ms <= 0L) return ""
    val diff = System.currentTimeMillis() - ms
    if (diff < 0L) return "just now"
    val mins  = diff / 60_000L
    val hours = mins  / 60L
    val days  = hours / 24L
    return when {
        days  >= 1L -> if (days  == 1L) "1 day ago"  else "$days days ago"
        hours >= 1L -> if (hours == 1L) "1 hour ago" else "$hours hours ago"
        mins  >= 1L -> if (mins  == 1L) "1 min ago"  else "$mins mins ago"
        else        -> "just now"
    }
}

// ── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun ParentActionableTasksScreen(
    onBack: () -> Unit = {},
    onNavigateToParentTask: (taskId: String) -> Unit = {},
    viewModel: ParentActionableTasksViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Group by child preserving oldest-first sort order
    val grouped: List<Pair<Child?, List<ParentTask>>> = remember(uiState.actionableTasks) {
        val seen = mutableListOf<String>()
        val map = mutableMapOf<String, MutableList<ParentTask>>()
        for (task in uiState.actionableTasks) {
            if (!map.containsKey(task.childId)) seen += task.childId
            map.getOrPut(task.childId) { mutableListOf() } += task
        }
        seen.map { id -> uiState.childById[id] to (map[id] ?: emptyList()) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ScreenBg)
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = DobiTextPrimary
                )
            }
            Text(
                text = "Tasks needing attention",
                fontSize = 17.sp,
                fontWeight = SFProBold,
                color = DobiTextPrimary
            )
        }

        if (uiState.actionableTasks.isEmpty()) {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF22C55E),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "All caught up!",
                    fontSize = 17.sp,
                    fontWeight = SFProBold,
                    color = DobiTextPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "No tasks need your attention right now.",
                    fontSize = 14.sp,
                    fontWeight = SFProRegular,
                    color = DobiTextSecondary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
            ) {
                grouped.forEach { (child, tasks) ->
                    // Child section header
                    item(key = "header-${child?.id ?: tasks.firstOrNull()?.childId}") {
                        ChildSectionHeader(child = child)
                        Spacer(Modifier.height(8.dp))
                    }
                    // Task rows
                    items(tasks, key = { it.id }) { task ->
                        ActionableTaskRow(
                            task = task,
                            onClick = { onNavigateToParentTask(task.id) },
                            onSendReminder = { viewModel.sendReminder(task.id) },
                            isSendingReminder = uiState.sendingReminderForTaskId == task.id
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    item(key = "spacer-${child?.id ?: tasks.firstOrNull()?.childId}") {
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

// ── Child section header ─────────────────────────────────────────────────────

@Composable
private fun ChildSectionHeader(child: Child?) {
    val emoji = when (child?.gender) {
        "girl"  -> "👧"
        "other" -> "🧒"
        else    -> "👦"
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = emoji, fontSize = 20.sp)
        Spacer(Modifier.width(8.dp))
        Text(
            text = child?.name ?: "Unknown",
            fontSize = 14.sp,
            fontWeight = SFProBold,
            color = DobiTextPrimary
        )
    }
}

// ── Task row ─────────────────────────────────────────────────────────────────

@Composable
private fun ActionableTaskRow(
    task: ParentTask,
    onClick: () -> Unit,
    onSendReminder: () -> Unit,
    isSendingReminder: Boolean,
) {
    val isAssigned = task.status == ParentTaskStatus.ASSIGNED
    val (pillBg, pillFg, pillLabel) = when (task.status) {
        ParentTaskStatus.REQUESTED     -> Triple(Color(0xFFFFF4E0), Color(0xFFB45309), "New request")
        ParentTaskStatus.PENDING_REVIEW -> Triple(DobiPrimarySoft, DobiPrimary, "Needs review")
        else                           -> Triple(Color(0xFFFFE4E6), Color(0xFFBE123C), "Remind")
    }

    val relevantTs: Long = when (task.status) {
        ParentTaskStatus.PENDING_REVIEW ->
            if (task.submittedAtMillis > 0L) task.submittedAtMillis else task.assignedAtMillis
        ParentTaskStatus.REQUESTED -> task.requestedAtMillis
        else -> task.assignedAtMillis
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (!isAssigned) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ) else Modifier
            )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Subject icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFF4E0)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = task.subjectIcon.ifBlank { "📚" }, fontSize = 20.sp)
            }

            Spacer(Modifier.width(12.dp))

            // Headline + subtitle
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = actionableHeadline(task),
                    fontSize = 13.sp,
                    fontWeight = SFProSemiBold,
                    color = DobiTextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                val ago = timeAgo(relevantTs)
                val subtitle = buildString {
                    if (task.subject.isNotBlank()) append(task.subject)
                    if (task.subject.isNotBlank() && ago.isNotBlank()) append(" · ")
                    if (ago.isNotBlank()) append(ago)
                }
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        fontWeight = SFProRegular,
                        color = DobiTextSecondary
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            // Pill or Remind button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .background(pillBg)
                    .then(
                        if (isAssigned) Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onSendReminder
                        ) else Modifier
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isAssigned && isSendingReminder) "…" else pillLabel,
                    fontSize = 11.sp,
                    fontWeight = SFProSemiBold,
                    color = pillFg
                )
            }
        }
    }
}

private fun actionableHeadline(task: ParentTask): String {
    val scope = task.displayName.ifBlank { "a topic" }
    return when (task.status) {
        ParentTaskStatus.REQUESTED -> "Wants to study: $scope"
        ParentTaskStatus.PENDING_REVIEW -> when (task.type) {
            ParentTaskType.REVISE_CHAPTER -> "Submitted revision: $scope"
            ParentTaskType.WRITE_QA       -> "Submitted Q&A: $scope"
            ParentTaskType.OUR_QUESTIONS  -> "Quiz done: $scope"
            else                          -> "Submitted: $scope"
        }
        else -> "Not done yet: $scope"
    }
}
