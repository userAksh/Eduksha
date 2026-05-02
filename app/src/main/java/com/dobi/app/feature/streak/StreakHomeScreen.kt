package com.dobi.app.feature.streak

import android.content.Intent
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dobi.app.core.ui.theme.DobiBackground
import com.dobi.app.core.ui.theme.DobiBorder
import com.dobi.app.core.ui.theme.DobiPrimary
import com.dobi.app.core.ui.theme.DobiPrimarySoft
import com.dobi.app.core.ui.theme.DobiTextPrimary
import com.dobi.app.core.ui.theme.DobiTextSecondary
import com.dobi.app.core.ui.theme.DobiTextTertiary
import com.dobi.app.core.ui.theme.SFProBold
import com.dobi.app.core.ui.theme.SFProSemiBold
import com.dobi.app.domain.model.ClanTier
import com.dobi.app.domain.model.FriendActivityItem
import com.dobi.app.domain.model.FriendSnap

private val FlameRed = Color(0xFFFF4D6D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreakHomeScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToLeaderboard: () -> Unit = {},
    onUploadToday: () -> Unit = {},
    viewModel: StreakHomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val invite: () -> Unit = {
        val msg = "Join me on Dobi and let's keep our study streak going! " +
            "${uiState.childName} is on a ${uiState.streak.days}-day streak."
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, msg)
        }
        context.startActivity(Intent.createChooser(intent, "Invite a friend"))
    }

    Scaffold(
        containerColor = DobiBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🔥", fontSize = 20.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Study Streaks",
                            fontWeight = SFProBold,
                            color = DobiTextPrimary,
                            fontSize = 18.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = DobiTextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = DobiTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DobiBackground)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                StreakHeroCard(
                    days = uiState.streak.days,
                    childName = uiState.childName
                )
            }
            item {
                InviteFriendsCard(
                    subtitle = uiState.inviteSubtitle,
                    onInvite = invite
                )
            }
            item {
                if (uiState.clan.tier == ClanTier.NONE) {
                    JoinClanEmptyState(
                        friendsInvited = uiState.friendsInvited,
                        streakDays = uiState.streak.days,
                        onLearnMore = onNavigateToLeaderboard
                    )
                } else {
                    YourClanCard(
                        tier = uiState.clan.tier,
                        name = uiState.clan.name,
                        rank = uiState.clan.rankInSchool,
                        pointsEarned = uiState.clan.pointsEarned,
                        pointsToNext = uiState.clan.pointsToNextTier,
                        onClick = onNavigateToLeaderboard
                    )
                }
            }
            item {
                if (uiState.hasSharedToday) {
                    FriendSnapsSection(snaps = uiState.friendSnaps)
                } else {
                    TodayTaskCard(
                        completed = uiState.tasksCompletedToday,
                        onUpload = onUploadToday
                    )
                }
            }
            if (uiState.friendsActivity.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Friends Activity",
                        actionLabel = "See all",
                        onAction = {}
                    )
                }
                items(uiState.friendsActivity.size) { idx ->
                    FriendActivityRow(item = uiState.friendsActivity[idx])
                }
            }
        }
    }
}

@Composable
private fun StreakHeroCard(days: Int, childName: String) {
    val gradient = Brush.linearGradient(
        colors = listOf(Color(0xFF2F7DFF), Color(0xFF4A90FF), Color(0xFF6CA8FF))
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 14.dp,
                shape = RoundedCornerShape(22.dp),
                spotColor = Color(0xFF2F7DFF).copy(alpha = 0.30f),
                ambientColor = Color(0xFF2F7DFF).copy(alpha = 0.18f)
            )
            .clip(RoundedCornerShape(22.dp))
            .background(gradient)
    ) {
        Text(
            text = "🔥",
            fontSize = 100.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 14.dp, y = (-14).dp)
                .alpha(0.18f)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp)
        ) {
            val hasStreak = days > 0
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "🔥",
                    fontSize = 28.sp,
                    modifier = Modifier.alpha(if (hasStreak) 1f else 0.55f)
                )
                Spacer(Modifier.width(8.dp))
                if (hasStreak) {
                    Text(
                        text = "$days",
                        fontSize = 44.sp,
                        fontWeight = SFProBold,
                        color = Color.White
                    )
                } else {
                    Text(
                        text = "Start Your Streak!",
                        fontSize = 22.sp,
                        fontWeight = SFProBold,
                        color = Color.White
                    )
                }
            }
            Text(
                text = if (hasStreak) "Day Streak" else "Upload today to light up day 1",
                fontSize = if (hasStreak) 14.sp else 12.sp,
                color = Color.White.copy(alpha = 0.92f),
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = if (hasStreak) "Great going, $childName! 🔥"
                else "Hi $childName, ready to begin? 🌱",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Consistency today,\nExcellence tomorrow!",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun InviteFriendsCard(
    subtitle: String,
    onInvite: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFFFF7E6))
            .border(1.dp, Color(0xFFFEEAB6), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "👫", fontSize = 28.sp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Invite Friends, Earn More!",
                color = DobiTextPrimary,
                fontSize = 14.sp,
                fontWeight = SFProBold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = DobiTextSecondary,
                fontSize = 11.sp
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(DobiPrimary)
                .clickable(onClick = onInvite)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Invite",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = SFProSemiBold
            )
        }
    }
}

@Composable
private fun YourClanCard(
    tier: ClanTier,
    name: String,
    rank: Int,
    pointsEarned: Int,
    pointsToNext: Int,
    onClick: () -> Unit
) {
    val progress = if (pointsToNext == 0) 0f
    else (pointsEarned.toFloat() / pointsToNext.toFloat()).coerceIn(0f, 1f)
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Your Clan",
            fontSize = 13.sp,
            color = DobiTextSecondary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(DobiPrimarySoft)
                .clickable(onClick = onClick)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val iconRes = tier.iconRes()
            if (iconRes != null) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = name,
                    modifier = Modifier.size(56.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF7C3AED).copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🛡️", fontSize = 24.sp)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    color = DobiTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = SFProBold
                )
                Text(
                    text = "Rank #$rank in your school",
                    color = DobiTextSecondary,
                    fontSize = 11.sp
                )
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = DobiPrimary,
                    trackColor = Color.White,
                    strokeCap = ProgressIndicatorDefaults.LinearStrokeCap
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "$pointsEarned / $pointsToNext pts",
                    color = DobiTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = DobiTextTertiary
            )
        }
    }
}

@Composable
private fun JoinClanEmptyState(
    friendsInvited: Int,
    streakDays: Int,
    onLearnMore: () -> Unit
) {
    val tiers = listOf(
        ClanTierVisual(
            tier = ClanTier.STREAK_WARRIOR,
            tagline = "Entry tier",
            startColor = Color(0xFFFFE0E0),
            endColor = Color(0xFFFFB7B7),
            ribbonColor = Color(0xFFC62828)
        ),
        ClanTierVisual(
            tier = ClanTier.RISING_LEGENDS,
            tagline = "Mid tier",
            startColor = Color(0xFFD9E5FF),
            endColor = Color(0xFF9CB7F0),
            ribbonColor = Color(0xFF1E40AF)
        ),
        ClanTierVisual(
            tier = ClanTier.NEXT_GEN_CHAMPS,
            tagline = "Top tier",
            startColor = Color(0xFFE8DBFF),
            endColor = Color(0xFFB89BF5),
            ribbonColor = Color(0xFF6B21A8)
        )
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Join Your First Clan!",
                    color = DobiTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = SFProBold
                )
                Text(
                    text = "Invite friends and keep your streak going to unlock clans.",
                    color = DobiTextSecondary,
                    fontSize = 11.sp
                )
            }
            Text(
                text = "Learn more",
                color = DobiPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onLearnMore)
            )
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(tiers) { visual ->
                ClanTierUnlockCard(
                    visual = visual,
                    friendsInvited = friendsInvited,
                    streakDays = streakDays
                )
            }
        }
    }
}

private data class ClanTierVisual(
    val tier: ClanTier,
    val tagline: String,
    val startColor: Color,
    val endColor: Color,
    val ribbonColor: Color
)

@Composable
private fun ClanTierUnlockCard(
    visual: ClanTierVisual,
    friendsInvited: Int,
    streakDays: Int
) {
    val friendsProgress = friendsInvited.coerceAtMost(visual.tier.friendThreshold)
    val streakProgress = streakDays.coerceAtMost(visual.tier.streakThreshold)
    val unlocked = friendsInvited >= visual.tier.friendThreshold &&
        streakDays >= visual.tier.streakThreshold

    Column(
        modifier = Modifier
            .width(196.dp)
            .height(316.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        visual.startColor,
                        Color.White
                    ),
                    startY = 0f,
                    endY = 360f
                )
            )
            .border(1.dp, visual.endColor.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
    ) {
        // Badge area — transparent badge sits on the pastel halo from the
        // card's gradient backdrop, so no harsh white block behind it.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            contentAlignment = Alignment.Center
        ) {
            // Soft circular glow behind the badge
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(visual.endColor.copy(alpha = 0.22f))
            )
            visual.tier.iconRes()?.let { iconRes ->
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = visual.tier.displayLabel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentScale = ContentScale.Fit
                )
            }
            // Tier-position chip (entry / mid / top)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(visual.ribbonColor)
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    text = visual.tagline,
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 14.dp, vertical = 4.dp)
        ) {
            Text(
                text = visual.tier.displayLabel,
                color = DobiTextPrimary,
                fontSize = 14.sp,
                fontWeight = SFProBold
            )
            Spacer(Modifier.height(8.dp))
            UnlockRequirement(
                emoji = "👥",
                label = "Friends",
                current = friendsProgress,
                target = visual.tier.friendThreshold
            )
            Spacer(Modifier.height(6.dp))
            UnlockRequirement(
                emoji = "🔥",
                label = "Day streak",
                current = streakProgress,
                target = visual.tier.streakThreshold
            )
            Spacer(Modifier.weight(1f))
            // Bonus / value-prop line — keeps all 3 cards visually consistent.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🎁", fontSize = 12.sp)
                Spacer(Modifier.width(4.dp))
                Text(
                    text = if (visual.tier.pointsReward > 0)
                        "+${visual.tier.pointsReward} pts on join"
                    else "Free entry tier",
                    color = visual.ribbonColor,
                    fontSize = 11.sp,
                    fontWeight = SFProSemiBold
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .padding(bottom = 12.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(
                    if (unlocked) visual.ribbonColor.copy(alpha = 0.14f)
                    else Color.White
                )
                .border(
                    1.dp,
                    if (unlocked) visual.ribbonColor.copy(alpha = 0.4f)
                    else Color(0xFFE5EBF5),
                    RoundedCornerShape(9.dp)
                )
                .padding(vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (unlocked) "Ready to join" else "🔒 Locked",
                color = if (unlocked) visual.ribbonColor else DobiTextSecondary,
                fontSize = 11.sp,
                fontWeight = SFProSemiBold
            )
        }
    }
}

@Composable
private fun UnlockRequirement(
    emoji: String,
    label: String,
    current: Int,
    target: Int
) {
    val pct = if (target == 0) 0f else (current.toFloat() / target.toFloat()).coerceIn(0f, 1f)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = emoji, fontSize = 11.sp)
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                color = DobiTextSecondary,
                fontSize = 10.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "$current / $target",
                color = DobiTextPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFFEEF1F6))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(pct)
                    .height(4.dp)
                    .background(DobiPrimary)
            )
        }
    }
}

@Composable
private fun TodayTaskCard(
    completed: Boolean,
    onUpload: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFFFF8E6))
            .border(1.dp, Color(0xFFFEE7A6), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "⭐", fontSize = 22.sp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Today's Task",
                color = DobiTextPrimary,
                fontSize = 14.sp,
                fontWeight = SFProBold
            )
            Text(
                text = if (completed) "Done — share to keep streak alive."
                else "Upload the chapter you studied today",
                color = DobiTextSecondary,
                fontSize = 11.sp
            )
        }
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(DobiPrimary)
                .clickable(onClick = onUpload)
                .padding(horizontal = 14.dp, vertical = 9.dp)
        ) {
            Text(
                text = if (completed) "Share" else "Upload Now",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = SFProSemiBold
            )
        }
    }
}

@Composable
private fun FriendSnapsSection(snaps: List<FriendSnap>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "📸", fontSize = 18.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Snaps from your friends",
                color = DobiTextPrimary,
                fontSize = 14.sp,
                fontWeight = SFProBold
            )
        }
        Spacer(Modifier.height(8.dp))
        if (snaps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE5EBF5), RoundedCornerShape(14.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Your friends haven't shared any snap with you yet.",
                    color = DobiTextSecondary,
                    fontSize = 12.sp
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                snaps.forEach { snap -> FriendSnapRow(snap = snap) }
            }
        }
    }
}

@Composable
private fun FriendSnapRow(snap: FriendSnap) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE5EBF5), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(DobiPrimarySoft),
            contentAlignment = Alignment.Center
        ) {
            Text(text = snap.friendAvatarEmoji, fontSize = 18.sp)
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = snap.friendName,
                color = DobiTextPrimary,
                fontSize = 13.sp,
                fontWeight = SFProSemiBold
            )
            Text(
                text = "Studied \"${snap.lessonName}\"",
                color = DobiTextSecondary,
                fontSize = 11.sp
            )
        }
        Text(
            text = formatRelative(snap.sharedAtMillis),
            color = DobiTextTertiary,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, start = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = DobiTextPrimary,
            fontSize = 14.sp,
            fontWeight = SFProBold,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = actionLabel,
            color = DobiPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable(onClick = onAction)
        )
    }
}

@Composable
private fun FriendActivityRow(item: FriendActivityItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(DobiPrimarySoft),
            contentAlignment = Alignment.Center
        ) {
            Text(text = item.avatarEmoji, fontSize = 18.sp)
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                color = DobiTextPrimary,
                fontSize = 13.sp,
                fontWeight = SFProSemiBold
            )
            Text(
                text = item.action,
                color = DobiTextSecondary,
                fontSize = 11.sp
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "🔥", fontSize = 12.sp)
            Spacer(Modifier.width(2.dp))
            Text(
                text = "${item.streakDays}",
                color = FlameRed,
                fontSize = 12.sp,
                fontWeight = SFProBold
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = formatRelative(item.timestampMillis),
            color = DobiTextTertiary,
            fontSize = 10.sp
        )
    }
}

private fun formatRelative(millis: Long): String {
    if (millis <= 0) return ""
    val diff = System.currentTimeMillis() - millis
    val mins = diff / 60_000
    val hours = mins / 60
    return when {
        mins < 1 -> "just now"
        mins < 60 -> "${mins}m ago"
        hours < 24 -> "${hours}h ago"
        else -> "${hours / 24}d ago"
    }
}
