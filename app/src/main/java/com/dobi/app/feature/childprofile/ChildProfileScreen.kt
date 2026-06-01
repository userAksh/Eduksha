package com.dobi.app.feature.childprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dobi.app.core.ui.theme.DobiBackground
import com.dobi.app.core.ui.theme.DobiPrimary
import com.dobi.app.core.ui.theme.DobiPrimaryContainer
import com.dobi.app.core.ui.theme.DobiSuccess
import com.dobi.app.core.ui.theme.DobiSuccessContainer
import com.dobi.app.core.ui.theme.DobiTextPrimary
import com.dobi.app.core.ui.theme.DobiTextSecondary
import com.dobi.app.core.ui.theme.DobiWarning
import com.dobi.app.core.ui.theme.DobiWarningContainer
import com.dobi.app.core.ui.theme.SFProBold
import com.dobi.app.core.ui.theme.SFProMedium
import com.dobi.app.core.ui.theme.SFProSemiBold
import com.dobi.app.domain.model.Friend
import androidx.compose.foundation.lazy.items
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildProfileContent(
    contentPadding: PaddingValues,
    onInviteFriend: () -> Unit,
    onSwitchProfile: () -> Unit,
    onAccountDeleted: () -> Unit = {},
    onOpenUploadGroup: (groupKey: String) -> Unit = {},
    onOpenQuizResult: (unitId: String, conceptId: String) -> Unit = { _, _ -> },
    viewModel: ChildProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selectedSubject by remember { mutableStateOf<SubjectStat?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.deletionEvents.collectLatest { event ->
            when (event) {
                is ChildProfileViewModel.DeletionEvent.Completed -> {
                    Toast.makeText(
                        context,
                        "Account deleted. Hope to see you again!",
                        Toast.LENGTH_SHORT
                    ).show()
                    onAccountDeleted()
                }
                is ChildProfileViewModel.DeletionEvent.Failed -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    ChildProfileLayout(
        uiState = uiState,
        contentPadding = contentPadding,
        onInviteFriend = onInviteFriend,
        onSwitchProfile = onSwitchProfile,
        onDeleteAccountClick = viewModel::showDeleteDialog,
        onSubjectClick = { selectedSubject = it }
    )

    if (uiState.deleteDialogVisible) {
        DeleteAccountDialog(
            isDeleting = uiState.isDeleting,
            onConfirm = viewModel::confirmDeleteAccount,
            onDismiss = viewModel::dismissDeleteDialog
        )
    }

    selectedSubject?.let { subject ->
        ModalBottomSheet(
            onDismissRequest = { selectedSubject = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
            containerColor = Color.White
        ) {
            SubjectDetailSheet(
                subject = subject,
                onTopicClick = { topic ->
                    when {
                        topic.groupKey.isNotBlank() -> onOpenUploadGroup(topic.groupKey)
                        topic.quizUnitId.isNotBlank() || topic.quizConceptId.isNotBlank() ->
                            onOpenQuizResult(topic.quizUnitId, topic.quizConceptId)
                    }
                }
            )
        }
    }
}

@Composable
private fun ChildProfileLayout(
    uiState: ChildProfileUiState,
    contentPadding: PaddingValues,
    onInviteFriend: () -> Unit,
    onSwitchProfile: () -> Unit,
    onDeleteAccountClick: () -> Unit = {},
    onSubjectClick: (SubjectStat) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DobiBackground),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = contentPadding.calculateTopPadding() + 24.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { IdentityCard(uiState) }
        item { StatsHero(uiState) }
        item {
            SectionHeader(title = "Subjects you've explored", emoji = "🎓")
        }
        if (uiState.subjects.isEmpty()) {
            item { EmptySubjectsCard() }
        } else {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    uiState.subjects.forEach { subject ->
                        SubjectRow(subject, onClick = { onSubjectClick(subject) })
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = "Studying with friends",
                emoji = "🤝",
                trailing = "Invite",
                onTrailingClick = onInviteFriend
            )
        }
        item { FriendsCard(uiState.friends, onInvite = onInviteFriend) }

        item { Spacer(Modifier.height(4.dp)) }
        item { SwitchProfileCard(onClick = onSwitchProfile) }
        item { DeleteAccountCard(onClick = onDeleteAccountClick) }
    }
}

// ── Identity ───────────────────────────────────────────────────────────────────

@Composable
private fun IdentityCard(uiState: ChildProfileUiState) {
    val accent = Color(0xFF7C8BFF)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = accent.copy(alpha = 0.25f)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFFE8F1FF), Color(0xFFF1E8FF), Color(0xFFFFE8F1))
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.7f))
                )
                Box(
                    modifier = Modifier
                        .shadow(8.dp, CircleShape, spotColor = accent.copy(alpha = 0.4f))
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF7C8BFF), Color(0xFF5B6BFF))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = avatarFor(uiState.gender), fontSize = 38.sp)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = uiState.name.ifBlank { "Hi there!" },
                    fontSize = 22.sp,
                    fontWeight = SFProBold,
                    color = DobiTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (uiState.grade > 0)
                        "Class ${uiState.grade} • ${uiState.board}"
                    else uiState.board,
                    fontSize = 13.sp,
                    color = DobiTextSecondary,
                    fontWeight = SFProMedium
                )
                Spacer(Modifier.height(8.dp))
                Row {
                    MiniBadge(text = "🌟 Learner")
                    Spacer(Modifier.width(6.dp))
                    if (uiState.streakDays > 0) {
                        MiniBadge(text = "🔥 ${uiState.streakDays}-day streak")
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniBadge(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = SFProSemiBold,
        color = DobiTextPrimary,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.85f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

private fun avatarFor(gender: String): String = when (gender.lowercase()) {
    "girl", "female", "f" -> "👧"
    "boy", "male", "m" -> "👦"
    else -> "🧒"
}

// ── Stats hero ─────────────────────────────────────────────────────────────────

@Composable
private fun StatsHero(uiState: ChildProfileUiState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(18.dp),
                spotColor = Color(0x14000000)
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatCell(
                emoji = "🔥",
                value = uiState.streakDays.toString(),
                label = if (uiState.streakDays == 1) "day streak" else "day streak",
                accent = Color(0xFFE67E22)
            )
            StatDivider()
            StatCell(
                emoji = "📚",
                value = uiState.topicsCount.toString(),
                label = "topics",
                accent = DobiPrimary
            )
            StatDivider()
            StatCell(
                emoji = "🧠",
                value = uiState.quizzesCount.toString(),
                label = "quizzes",
                accent = Color(0xFF7C3AED)
            )
            StatDivider()
            StatCell(
                emoji = "🎯",
                value = if (uiState.quizzesCount > 0)
                    "${uiState.averageAccuracyPct}%" else "—",
                label = "accuracy",
                accent = DobiSuccess
            )
        }
    }
}

@Composable
private fun StatCell(emoji: String, value: String, label: String, accent: Color) {
    Column(
        modifier = Modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = emoji, fontSize = 22.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = SFProBold,
            color = accent,
            maxLines = 1
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = DobiTextSecondary,
            fontWeight = SFProMedium,
            maxLines = 1
        )
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(36.dp)
            .background(Color(0xFFEEF1F8))
    )
}

// ── Section header ─────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(
    title: String,
    emoji: String,
    trailing: String? = null,
    onTrailingClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = emoji, fontSize = 18.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = DobiTextPrimary,
                fontWeight = SFProSemiBold
            )
        }
        if (trailing != null) {
            Text(
                text = trailing,
                fontSize = 13.sp,
                color = DobiPrimary,
                fontWeight = SFProMedium,
                modifier = Modifier.clickable(onClick = onTrailingClick)
            )
        }
    }
}

// ── Subjects ───────────────────────────────────────────────────────────────────

@Composable
private fun SubjectRow(subject: SubjectStat, onClick: () -> Unit = {}) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(14.dp),
                spotColor = Color(0x10000000)
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF4F6FE)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = subject.emoji, fontSize = 22.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subject.name,
                    fontSize = 14.sp,
                    fontWeight = SFProSemiBold,
                    color = DobiTextPrimary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${subject.topicsCount} topic${if (subject.topicsCount == 1) "" else "s"}",
                    fontSize = 11.sp,
                    color = DobiTextSecondary
                )
            }
            if (subject.accuracyPct != null) {
                val (fg, bg) = when {
                    subject.accuracyPct >= 80 -> DobiSuccess to DobiSuccessContainer
                    subject.accuracyPct >= 50 -> DobiWarning to DobiWarningContainer
                    else -> Color(0xFFEF4444) to Color(0xFFFEE2E2)
                }
                Text(
                    text = "${subject.accuracyPct}%",
                    fontSize = 12.sp,
                    fontWeight = SFProBold,
                    color = fg,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(bg)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

// ── Subject detail bottom sheet ────────────────────────────────────────────────

@Composable
private fun SubjectDetailSheet(
    subject: SubjectStat,
    onTopicClick: (TopicActivityItem) -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Fixed header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 4.dp, bottom = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF4F6FE)),
                contentAlignment = Alignment.Center
            ) {
                Text(subject.emoji, fontSize = 24.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subject.name,
                    fontSize = 17.sp,
                    fontWeight = SFProBold,
                    color = DobiTextPrimary
                )
                Text(
                    text = "${subject.topicsCount} topic${if (subject.topicsCount == 1) "" else "s"} explored",
                    fontSize = 12.sp,
                    color = DobiTextSecondary
                )
            }
            if (subject.accuracyPct != null) {
                val (fg, bg) = when {
                    subject.accuracyPct >= 80 -> DobiSuccess to DobiSuccessContainer
                    subject.accuracyPct >= 50 -> DobiWarning to DobiWarningContainer
                    else -> Color(0xFFEF4444) to Color(0xFFFEE2E2)
                }
                Text(
                    text = "${subject.accuracyPct}% avg",
                    fontSize = 12.sp,
                    fontWeight = SFProBold,
                    color = fg,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(bg)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Scrollable topic list
        if (subject.topics.isEmpty()) {
            Text(
                text = "No topic details available yet.",
                fontSize = 13.sp,
                color = DobiTextSecondary,
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    bottom = 32.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(subject.topics) { topic ->
                    TopicActivityRow(topic, onClick = { onTopicClick(topic) })
                }
            }
        }
    }
}

@Composable
private fun TopicActivityRow(topic: TopicActivityItem, onClick: () -> Unit = {}) {
    val isNavigable = topic.uploadKinds.isNotEmpty() || topic.quizUnitId.isNotBlank() || topic.quizConceptId.isNotBlank()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF8FAFF))
            .then(if (isNavigable) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = topic.name,
                fontSize = 13.sp,
                fontWeight = SFProSemiBold,
                color = DobiTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(5.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                topic.uploadKinds
                    .map { uploadKindPill(it) }
                    .distinct()
                    .forEach { (emoji, label) -> ActivityPill(emoji = emoji, label = label) }
                if (topic.hasQuiz) ActivityPill(emoji = "🧠", label = "Quiz")
                if (topic.hasSubjective) ActivityPill(emoji = "✍️", label = "Written")
            }
            val timeStr = formatActivityTime(topic.lastActivityMs)
            if (timeStr.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = timeStr,
                    fontSize = 10.sp,
                    color = DobiTextSecondary
                )
            }
        }
        if (topic.accuracyPct != null) {
            Spacer(Modifier.width(10.dp))
            val (fg, bg) = when {
                topic.accuracyPct >= 80 -> DobiSuccess to DobiSuccessContainer
                topic.accuracyPct >= 50 -> DobiWarning to DobiWarningContainer
                else -> Color(0xFFEF4444) to Color(0xFFFEE2E2)
            }
            Text(
                text = "${topic.accuracyPct}%",
                fontSize = 12.sp,
                fontWeight = SFProBold,
                color = fg,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(bg)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        if (isNavigable) {
            Spacer(Modifier.width(8.dp))
            Text("›", fontSize = 18.sp, color = DobiTextSecondary)
        }
    }
}

@Composable
private fun ActivityPill(emoji: String, label: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE5EBF5), RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(emoji, fontSize = 11.sp)
        Text(label, fontSize = 11.sp, color = DobiTextSecondary, fontWeight = SFProMedium)
    }
}

private fun uploadKindPill(kind: String): Pair<String, String> = when {
    kind == "class_work"             -> "📝" to "Class Work"
    kind == "self_learning"          -> "📖" to "Self Study"
    kind == "homework"               -> "📓" to "Homework"
    kind == "ai_test_prep"           -> "🤖" to "AI Practice"
    kind.startsWith("tuition_hw")    -> "🎒" to "Tuition HW"
    kind.startsWith("tuition_")      -> "🎒" to "Tuition Work"
    else                             -> "📤" to "Work"
}

private fun formatActivityTime(ms: Long): String {
    if (ms <= 0L) return ""
    val diff = System.currentTimeMillis() - ms
    if (diff < 0L) return "just now"
    val mins = diff / 60_000L
    val hours = mins / 60L
    return when {
        diff < 60_000L -> "just now"
        mins < 60L -> if (mins == 1L) "1 min ago" else "$mins mins ago"
        hours < 24L -> if (hours == 1L) "1 hour ago" else "$hours hours ago"
        else -> SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(ms))
    }
}

@Composable
private fun EmptySubjectsCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE5EBF5), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFF)),
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
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(DobiPrimaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text("🎓", fontSize = 20.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Your subjects will show up here",
                    fontSize = 13.sp,
                    fontWeight = SFProSemiBold,
                    color = DobiTextPrimary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Take a quiz or upload some work to start tracking your progress.",
                    fontSize = 12.sp,
                    color = DobiTextSecondary,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

// ── Friends ────────────────────────────────────────────────────────────────────

@Composable
private fun FriendsCard(friends: List<Friend>, onInvite: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color(0x14000000)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            if (friends.isEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(DobiSuccessContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🚀", fontSize = 22.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "No friends here yet",
                            fontSize = 13.sp,
                            fontWeight = SFProSemiBold,
                            color = DobiTextPrimary
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Invite a friend so you can keep streaks together.",
                            fontSize = 12.sp,
                            color = DobiTextSecondary,
                            lineHeight = 17.sp
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    InvitePill(onClick = onInvite)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(modifier = Modifier.weight(1f)) {
                        friends.take(4).forEachIndexed { index, friend ->
                            FriendBubble(friend = friend, overlap = index > 0)
                        }
                        if (friends.size > 4) {
                            Spacer(Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF1F5F9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+${friends.size - 4}",
                                    fontSize = 11.sp,
                                    fontWeight = SFProSemiBold,
                                    color = DobiTextSecondary
                                )
                            }
                        }
                    }
                    InvitePill(onClick = onInvite)
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = friends.take(3).joinToString(", ") {
                        it.name.ifBlank { "A friend" }
                    } + if (friends.size > 3) " +${friends.size - 3} more" else "",
                    fontSize = 12.sp,
                    color = DobiTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun FriendBubble(friend: Friend, overlap: Boolean) {
    Box(
        modifier = Modifier
            .padding(start = if (overlap) (-8).dp else 0.dp)
            .size(36.dp)
            .clip(CircleShape)
            .background(bubbleColorFor(friend.name))
            .border(2.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = avatarFor(friend.gender),
            fontSize = 18.sp
        )
    }
}

private fun bubbleColorFor(seed: String): Color {
    val palette = listOf(
        Color(0xFFFFE0E6), Color(0xFFE0F0FF), Color(0xFFE6FFE0),
        Color(0xFFFFF4E0), Color(0xFFEDE9FE), Color(0xFFE0FFFB)
    )
    val idx = (seed.hashCode().toUInt() % palette.size.toUInt()).toInt()
    return palette[idx]
}

@Composable
private fun InvitePill(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(DobiPrimary)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.PersonAdd,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "Invite",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = SFProSemiBold
        )
    }
}

// ── Switch profile / logout ────────────────────────────────────────────────────

@Composable
private fun DeleteAccountCard(onClick: () -> Unit) {
    val danger = Color(0xFFEF4444)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(1.dp, Color(0xFFFECACA), RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF5F5)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFEE2E2)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = danger,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Delete account",
                    fontSize = 14.sp,
                    fontWeight = SFProSemiBold,
                    color = danger
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Permanently remove your account, every child profile, all uploads, and friend connections.",
                    fontSize = 12.sp,
                    color = DobiTextSecondary,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}

@Composable
private fun DeleteAccountDialog(
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val danger = Color(0xFFEF4444)
    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        title = {
            Text(
                text = "Delete account?",
                fontWeight = SFProBold,
                color = DobiTextPrimary
            )
        },
        text = {
            Column {
                Text(
                    text = "This permanently deletes:",
                    color = DobiTextPrimary,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "•  Every child profile under your account\n" +
                        "•  All uploaded photos, quizzes, and written answers\n" +
                        "•  Streaks, parent tasks, and friend connections\n" +
                        "•  Your sign-in record",
                    color = DobiTextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "This cannot be undone.",
                    color = danger,
                    fontSize = 13.sp,
                    fontWeight = SFProSemiBold
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isDeleting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = danger,
                    contentColor = Color.White
                )
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Deleting…")
                } else {
                    Text("Delete forever")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isDeleting) {
                Text("Cancel", color = DobiTextSecondary)
            }
        },
        containerColor = Color.White
    )
}

@Composable
private fun SwitchProfileCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(1.dp, Color(0xFFE5EBF5), RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = DobiTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Switch profile",
                    fontSize = 14.sp,
                    fontWeight = SFProSemiBold,
                    color = DobiTextPrimary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Hand the phone back to a parent or pick a different child.",
                    fontSize = 12.sp,
                    color = DobiTextSecondary,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}
