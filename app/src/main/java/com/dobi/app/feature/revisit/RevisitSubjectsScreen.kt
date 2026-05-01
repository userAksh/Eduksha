package com.dobi.app.feature.revisit

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dobi.app.core.ui.theme.DobiBackground
import com.dobi.app.core.ui.theme.DobiPrimary
import com.dobi.app.core.ui.theme.DobiTextPrimary
import com.dobi.app.core.ui.theme.DobiTextSecondary
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RevisitSubjectsScreen(
    onNavigateBack: () -> Unit = {},
    onTopicClick: (String) -> Unit = {},
    viewModel: RevisitSubjectsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = DobiBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Revisit Past Topics",
                        style = MaterialTheme.typography.titleLarge,
                        color = DobiTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = DobiTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DobiBackground)
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            SearchBar(
                query = uiState.searchQuery,
                onQueryChanged = viewModel::onSearchQueryChanged
            )
            Spacer(Modifier.height(8.dp))
            if (uiState.subjectChips.isNotEmpty()) {
                SubjectChipsRow(
                    chips = uiState.subjectChips,
                    selected = uiState.selectedSubject,
                    onSelect = viewModel::onSubjectSelected
                )
                Spacer(Modifier.height(12.dp))
            }
            Box(modifier = Modifier.weight(1f)) {
                when {
                    uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    uiState.totalTopicCount == 0 -> EmptyState(
                        title = "Nothing to revisit yet",
                        subtitle = "When you upload class work, self learning, or tuition work, the topics will show up here."
                    )
                    uiState.visibleTopics.isEmpty() -> EmptyState(
                        title = "No matches",
                        subtitle = "Try a different subject or clear your search."
                    )
                    else -> TopicList(
                        topics = uiState.visibleTopics,
                        onTopicClick = onTopicClick
                    )
                }
            }
        }
    }
}

// ── Search bar ─────────────────────────────────────────────────────────────────

@Composable
private fun SearchBar(query: String, onQueryChanged: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onQueryChanged,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(14.dp)),
        placeholder = {
            Text("Search topics…", color = DobiTextSecondary, fontSize = 14.sp)
        },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = DobiTextSecondary,
                modifier = Modifier.size(20.dp)
            )
        },
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color(0xFFEEF4FF),
            unfocusedContainerColor = Color(0xFFEEF4FF),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedTextColor = DobiTextPrimary,
            unfocusedTextColor = DobiTextPrimary,
            cursorColor = DobiPrimary
        ),
        shape = RoundedCornerShape(14.dp)
    )
}

// ── Subject filter chips ───────────────────────────────────────────────────────

@Composable
private fun SubjectChipsRow(
    chips: List<RevisitSubjectChip>,
    selected: String,
    onSelect: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(chips, key = { it.name }) { chip ->
            SubjectChip(
                chip = chip,
                isSelected = chip.name.equals(selected, ignoreCase = true),
                onClick = { onSelect(chip.name) }
            )
        }
    }
}

@Composable
private fun SubjectChip(chip: RevisitSubjectChip, isSelected: Boolean, onClick: () -> Unit) {
    val accent = if (chip.color.isBlank()) DobiPrimary else parseHexColor(chip.color)
    val bg = if (isSelected) accent else Color(0xFFF1F5F9)
    val fg = if (isSelected) Color.White else DobiTextPrimary
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (chip.icon.isNotBlank()) {
            Text(text = chip.icon, fontSize = 14.sp)
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = chip.name,
            color = fg,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ── Topic list ─────────────────────────────────────────────────────────────────

@Composable
private fun TopicList(topics: List<RevisitTopicCard>, onTopicClick: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(topics, key = { it.groupKey }) { topic ->
            TopicCard(topic = topic, onClick = { onTopicClick(topic.groupKey) })
        }
        item {
            Spacer(Modifier.height(8.dp))
            TipFooter()
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TopicCard(topic: RevisitTopicCard, onClick: () -> Unit) {
    val accent = parseHexColor(topic.subjectColor)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.06f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(accent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = topic.subjectIcon.ifBlank { "📘" }, fontSize = 26.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = topic.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = DobiTextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    if (topic.context.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = topic.context,
                            style = MaterialTheme.typography.bodySmall,
                            color = DobiTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                KindTag(label = topic.kindLabel, hex = topic.kindColor)
            }
            Spacer(Modifier.height(10.dp))
            MetaRow(topic = topic)
        }
    }
}

@Composable
private fun MetaRow(topic: RevisitTopicCard) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val (label, color) = revisedLabel(topic.latestTimestamp)
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "📷 ${topic.photoCount} ${if (topic.photoCount == 1) "photo" else "photos"}",
                color = DobiTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
        if (topic.mcqLastPct != null || topic.subjectiveAttempts > 0) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (topic.mcqLastPct != null) {
                    ResultPill(
                        emoji = "📊",
                        text = "${topic.mcqLastPct}% MCQ" +
                            if (topic.mcqAttempts > 1) " · ${topic.mcqAttempts} tries" else "",
                        bg = Color(0xFFDBEAFE),
                        fg = Color(0xFF2563EB)
                    )
                    Spacer(Modifier.width(6.dp))
                }
                if (topic.subjectiveAttempts > 0) {
                    ResultPill(
                        emoji = "✍️",
                        text = topic.subjectiveLastPct?.let { "${it}% Sub" }
                            ?: "Sub" +
                            if (topic.subjectiveAttempts > 1) " · ${topic.subjectiveAttempts} tries" else "",
                        bg = Color(0xFFFEF3C7),
                        fg = Color(0xFFD97706)
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultPill(emoji: String, text: String, bg: Color, fg: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = emoji, fontSize = 11.sp)
        Spacer(Modifier.width(4.dp))
        Text(
            text = text,
            color = fg,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun KindTag(label: String, hex: String) {
    val accent = parseHexColor(hex)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(accent.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            color = accent,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ── Tip footer ─────────────────────────────────────────────────────────────────

@Composable
private fun TipFooter() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFEEF4FF))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("💡", fontSize = 18.sp)
        Spacer(Modifier.width(10.dp))
        Text(
            text = "Tip: Revising little by little each day helps you remember better for a longer time.",
            style = MaterialTheme.typography.bodySmall,
            color = DobiTextPrimary,
            fontSize = 12.sp
        )
    }
}

// ── Helpers ────────────────────────────────────────────────────────────────────

@Composable
internal fun EmptyState(title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = DobiTextPrimary)
        Spacer(Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = DobiTextSecondary
        )
    }
}

internal fun parseHexColor(hex: String): Color = runCatching {
    if (hex.isBlank()) Color(0xFF94A3B8)
    else Color(android.graphics.Color.parseColor(hex))
}.getOrDefault(Color(0xFF94A3B8))

private fun revisedLabel(timestamp: Long): Pair<String, Color> {
    if (timestamp <= 0) return "Not revised yet" to Color(0xFF94A3B8)
    val now = System.currentTimeMillis()
    val days = TimeUnit.MILLISECONDS.toDays(now - timestamp).coerceAtLeast(0)
    val hours = TimeUnit.MILLISECONDS.toHours(now - timestamp).coerceAtLeast(0)
    val (text, ageBucket) = when {
        days == 0L && hours <= 1L -> "Last revised just now" to 0
        days == 0L -> "Last revised today" to 0
        days == 1L -> "Last revised yesterday" to 0
        days < 7 -> "Last revised $days days ago" to 0
        days < 14 -> "Last revised $days days ago" to 1
        else -> "Last revised $days days ago" to 2
    }
    val color = when (ageBucket) {
        0 -> Color(0xFF22C55E)
        1 -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }
    return text to color
}
