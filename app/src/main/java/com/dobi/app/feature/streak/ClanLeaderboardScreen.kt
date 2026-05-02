package com.dobi.app.feature.streak

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.dobi.app.domain.model.LeaderboardEntry
import com.dobi.app.domain.model.StreakReward

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClanLeaderboardScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: ClanLeaderboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = DobiBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Clan Leaderboard",
                        fontWeight = SFProBold,
                        color = DobiTextPrimary,
                        fontSize = 18.sp
                    )
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
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = DobiTextTertiary
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
                top = padding.calculateTopPadding() + 4.dp,
                bottom = padding.calculateBottomPadding() + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                WindowToggle(
                    selected = uiState.window,
                    onSelect = viewModel::onWindowChange
                )
            }
            if (uiState.topThree.isNotEmpty()) {
                item { PodiumRow(top = uiState.topThree) }
            }
            items(uiState.rest.size) { idx ->
                LeaderboardRow(entry = uiState.rest[idx])
            }
            if (uiState.rewards.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, start = 4.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Your Rewards",
                            color = DobiTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = SFProBold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "See all",
                            color = DobiPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable {}
                        )
                    }
                }
                item { RewardsRow(rewards = uiState.rewards) }
            }
        }
    }
}

@Composable
private fun WindowToggle(
    selected: LeaderboardWindow,
    onSelect: (LeaderboardWindow) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DobiPrimarySoft)
            .padding(4.dp)
    ) {
        ToggleButton(
            label = "This Week",
            isActive = selected == LeaderboardWindow.THIS_WEEK,
            modifier = Modifier.weight(1f),
            onClick = { onSelect(LeaderboardWindow.THIS_WEEK) }
        )
        ToggleButton(
            label = "All Time",
            isActive = selected == LeaderboardWindow.ALL_TIME,
            modifier = Modifier.weight(1f),
            onClick = { onSelect(LeaderboardWindow.ALL_TIME) }
        )
    }
}

@Composable
private fun ToggleButton(
    label: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .background(if (isActive) DobiPrimary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isActive) Color.White else DobiTextSecondary,
            fontSize = 13.sp,
            fontWeight = SFProSemiBold
        )
    }
}

@Composable
private fun PodiumRow(top: List<LeaderboardEntry>) {
    val first = top.getOrNull(0)
    val second = top.getOrNull(1)
    val third = top.getOrNull(2)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        if (second != null) {
            PodiumCard(
                entry = second,
                rankColor = Color(0xFFA0A0A0),
                shieldGradientStart = Color(0xFFC68BFF),
                shieldGradientEnd = Color(0xFF7C3AED),
                modifier = Modifier.weight(1f)
            )
        }
        if (first != null) {
            PodiumCard(
                entry = first,
                rankColor = Color(0xFFE9A73D),
                shieldGradientStart = Color(0xFFFFD56B),
                shieldGradientEnd = Color(0xFFF59E0B),
                isFirst = true,
                modifier = Modifier.weight(1f)
            )
        }
        if (third != null) {
            PodiumCard(
                entry = third,
                rankColor = Color(0xFFCD7F32),
                shieldGradientStart = Color(0xFF6B7280),
                shieldGradientEnd = Color(0xFF374151),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PodiumCard(
    entry: LeaderboardEntry,
    rankColor: Color,
    shieldGradientStart: Color,
    shieldGradientEnd: Color,
    isFirst: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(rankColor.copy(alpha = 0.2f))
                .border(2.dp, rankColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${entry.rank}",
                color = rankColor,
                fontSize = 13.sp,
                fontWeight = SFProBold
            )
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isFirst) 130.dp else 110.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(14.dp),
                    spotColor = shieldGradientEnd.copy(alpha = 0.3f)
                )
                .clip(RoundedCornerShape(14.dp))
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(shieldGradientStart, shieldGradientEnd)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            val iconRes = entry.tier?.iconRes()
            if (iconRes != null) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = entry.clanName,
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text(text = entry.emoji, fontSize = if (isFirst) 44.sp else 36.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = entry.clanName,
            color = DobiTextPrimary,
            fontSize = 12.sp,
            fontWeight = SFProSemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Text(
            text = "${entry.points} pts",
            color = DobiTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun LeaderboardRow(entry: LeaderboardEntry) {
    val highlight = entry.isCurrentClan
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (highlight) DobiPrimarySoft else Color.Transparent)
            .border(
                if (highlight) 1.dp else 0.dp,
                if (highlight) DobiPrimary.copy(alpha = 0.4f) else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${entry.rank}",
            color = DobiTextSecondary,
            fontSize = 13.sp,
            fontWeight = SFProSemiBold,
            modifier = Modifier.width(24.dp)
        )
        Spacer(Modifier.width(6.dp))
        val iconRes = entry.tier?.iconRes()
        if (iconRes != null) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = entry.clanName,
                modifier = Modifier.size(40.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(DobiPrimarySoft),
                contentAlignment = Alignment.Center
            ) {
                Text(text = entry.emoji, fontSize = 18.sp)
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = entry.clanName,
            color = DobiTextPrimary,
            fontSize = 13.sp,
            fontWeight = SFProSemiBold,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${entry.points} pts",
            color = DobiTextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun RewardsRow(rewards: List<StreakReward>) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(rewards.size) { idx ->
            RewardCard(reward = rewards[idx])
        }
    }
}

@Composable
private fun RewardCard(reward: StreakReward) {
    val bg = if (reward.unlocked) Color(0xFFFFF7E6) else Color(0xFFF1F2F6)
    val border = if (reward.unlocked) Color(0xFFFEEAB6) else DobiBorder
    Column(
        modifier = Modifier
            .width(130.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(14.dp))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Text(text = reward.emoji, fontSize = 30.sp)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = reward.title,
            color = DobiTextPrimary,
            fontSize = 13.sp,
            fontWeight = SFProBold
        )
        Text(
            text = reward.subtitle,
            color = DobiTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
