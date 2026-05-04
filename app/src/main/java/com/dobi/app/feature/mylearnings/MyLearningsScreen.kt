package com.dobi.app.feature.mylearnings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dobi.app.core.ui.theme.DobiBackground
import com.dobi.app.core.ui.theme.DobiPrimary
import com.dobi.app.core.ui.theme.DobiPrimaryContainer
import com.dobi.app.core.ui.theme.DobiSuccess
import com.dobi.app.core.ui.theme.DobiSuccessContainer
import com.dobi.app.core.ui.theme.DobiWarning
import com.dobi.app.core.ui.theme.DobiWarningContainer
import com.dobi.app.core.ui.theme.DobiTextPrimary
import com.dobi.app.core.ui.theme.DobiTextSecondary
import com.dobi.app.core.ui.theme.DobiTheme
import com.dobi.app.core.ui.theme.SFProBold
import com.dobi.app.core.ui.theme.SFProMedium
import com.dobi.app.core.ui.theme.SFProSemiBold
import com.dobi.app.domain.model.RevisionMark
import com.dobi.app.domain.model.RevisionScope

// ── Entry point ────────────────────────────────────────────────────────────────

@Composable
fun MyLearningsContent(
    contentPadding: PaddingValues,
    viewModel: MyLearningsViewModel = hiltViewModel(),
    onMarkClick: (RevisionMark) -> Unit = {},
    onSeeAllRevision: () -> Unit = {},
    onTodayItemClick: (TodayLearningEntry) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MyLearningsLayout(
        uiState = uiState,
        contentPadding = contentPadding,
        onMarkClick = onMarkClick,
        onUnmark = viewModel::onUnmark,
        onSeeAllRevision = onSeeAllRevision,
        onTodayItemClick = onTodayItemClick,
        onOpenStory = viewModel::openStory,
        onRetryNews = viewModel::retryLoadNews
    )

    val selected = uiState.selectedStoryIndex
    if (selected != null && uiState.currentAffairs.isNotEmpty()) {
        NewspaperOverlay(
            stories = uiState.currentAffairs,
            startIndex = selected,
            onDismiss = viewModel::closeStory
        )
    }
}

@Composable
private fun MyLearningsLayout(
    uiState: MyLearningsUiState,
    contentPadding: PaddingValues,
    onMarkClick: (RevisionMark) -> Unit,
    onUnmark: (RevisionMark) -> Unit,
    onSeeAllRevision: () -> Unit,
    onTodayItemClick: (TodayLearningEntry) -> Unit,
    onOpenStory: (Int) -> Unit = {},
    onRetryNews: () -> Unit = {}
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
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Title
        item {
            Text(
                text = "My Learnings",
                style = MaterialTheme.typography.headlineMedium,
                color = DobiTextPrimary,
                fontWeight = SFProBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Your daily progress, saved lessons and what's new.",
                style = MaterialTheme.typography.bodyMedium,
                color = DobiTextSecondary
            )
            Spacer(Modifier.height(24.dp))
        }

        // ── Section 1: Today's Learning ──
        item { SectionHeader(title = "Today's Learning", emoji = "✨") }
        item { Spacer(Modifier.height(12.dp)) }

        if (uiState.todaysItems.isEmpty()) {
            item { TodayEmptyStateCard() }
        } else {
            items(uiState.todaysItems, key = { it.id }) { entry ->
                TodayEntryCard(entry = entry, onClick = { onTodayItemClick(entry) })
                Spacer(Modifier.height(10.dp))
            }
        }

        item { Spacer(Modifier.height(28.dp)) }

        // ── Section 2: Revision Hub ──
        // Cap the inline list to the most recent few marks; the "See all"
        // CTA in the section header routes to the full Need Revision screen.
        val revisionPreview = uiState.revisionMarks.take(REVISION_PREVIEW_LIMIT)
        val hasMore = uiState.revisionMarks.size > REVISION_PREVIEW_LIMIT
        item {
            SectionHeader(
                title = "Revision Hub",
                emoji = "📚",
                trailing = if (hasMore) "See all" else null,
                onTrailingClick = onSeeAllRevision
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Topics you've marked for revision.",
                style = MaterialTheme.typography.bodySmall,
                color = DobiTextSecondary
            )
            Spacer(Modifier.height(14.dp))
        }

        if (revisionPreview.isEmpty()) {
            item { RevisionEmptyStateCard() }
        } else {
            items(
                revisionPreview,
                key = { it.id.ifBlank { "${it.scope}/${it.targetId}" } }
            ) { mark ->
                RevisionItemCard(
                    mark = mark,
                    onClick = { onMarkClick(mark) },
                    onUnmark = { onUnmark(mark) }
                )
                Spacer(Modifier.height(10.dp))
            }
        }

        item { Spacer(Modifier.height(28.dp)) }

        // ── Section 3: Do You Know ──
        item {
            val canOpen = uiState.currentAffairs.isNotEmpty()
            SectionHeader(
                title = "Do You Know?",
                emoji = "💡",
                trailing = when {
                    canOpen -> "Open paper"
                    uiState.isLoadingNews -> "Loading…"
                    else -> "Retry"
                },
                onTrailingClick = {
                    if (canOpen) onOpenStory(0) else onRetryNews()
                },
                onHeaderClick = {
                    if (canOpen) onOpenStory(0) else onRetryNews()
                }
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Today's stories — tap any one to open the paper.",
                style = MaterialTheme.typography.bodySmall,
                color = DobiTextSecondary
            )
            Spacer(Modifier.height(14.dp))
        }

        if (uiState.currentAffairs.isEmpty()) {
            item {
                CurrentAffairsEmptyCard(
                    isLoading = uiState.isLoadingNews,
                    error = uiState.newsError,
                    onRetry = onRetryNews
                )
            }
        } else {
            itemsIndexed(uiState.currentAffairs) { index, news ->
                CurrentAffairCard(
                    news = news,
                    onClick = { onOpenStory(index) }
                )
                Spacer(Modifier.height(10.dp))
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

// ── Section header ─────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(
    title: String,
    emoji: String,
    trailing: String? = null,
    onTrailingClick: () -> Unit = {},
    onHeaderClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (onHeaderClick != null) it.clickable(onClick = onHeaderClick) else it },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
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
                style = MaterialTheme.typography.bodyMedium,
                color = DobiPrimary,
                fontWeight = SFProMedium,
                modifier = Modifier.clickable(onClick = onTrailingClick)
            )
        }
    }
}

private const val REVISION_PREVIEW_LIMIT = 3

// ── Today: empty state ─────────────────────────────────────────────────────────

@Composable
private fun TodayEmptyStateCard() {
    val topColor = Color(0xFFE8F1FF)
    val midColor = Color(0xFFF1E8FF)
    val bottomColor = Color(0xFFFFE8F1)
    val accent = Color(0xFF5B6BFF)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = accent.copy(alpha = 0.18f)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.verticalGradient(listOf(topColor, midColor, bottomColor)))
            .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
    ) {
        // Decorative sparkles
        Text("✨", fontSize = 18.sp, modifier = Modifier.align(Alignment.TopStart).padding(start = 18.dp, top = 14.dp))
        Text("⭐", fontSize = 14.sp, modifier = Modifier.align(Alignment.TopEnd).padding(end = 20.dp, top = 26.dp))
        Text("💫", fontSize = 16.sp, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 18.dp, bottom = 14.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.55f))
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
                    Text("🚀", fontSize = 36.sp)
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(
                "Let's get started!",
                fontSize = 17.sp,
                fontWeight = SFProBold,
                color = DobiTextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "You're yet to begin today's tasks. Start now and share your progress with friends and parents.",
                fontSize = 13.sp,
                color = DobiTextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp
            )
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier
                    .shadow(4.dp, RoundedCornerShape(22.dp), spotColor = accent.copy(alpha = 0.3f))
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White)
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🎯", fontSize = 14.sp)
                Spacer(Modifier.width(6.dp))
                Text(
                    "Begin today's learning",
                    fontSize = 12.sp,
                    fontWeight = SFProSemiBold,
                    color = accent
                )
            }
        }
    }
}

// ── Today: entry card ──────────────────────────────────────────────────────────

private data class TodayStyle(
    val emoji: String,
    val label: String,
    val accent: Color,
    val tint: Color
)

private fun styleFor(kind: TodayKind): TodayStyle = when (kind) {
    TodayKind.HOMEWORK -> TodayStyle("📝", "Homework", Color(0xFFE67E22), Color(0xFFFFF4E0))
    TodayKind.TASK     -> TodayStyle("🎯", "Task",     Color(0xFF7C3AED), Color(0xFFEDE9FE))
    TodayKind.QUIZ     -> TodayStyle("🧠", "Quiz",     Color(0xFF16A34A), Color(0xFFDCFCE7))
}

@Composable
private fun TodayEntryCard(entry: TodayLearningEntry, onClick: () -> Unit) {
    val style = styleFor(entry.kind)
    val accentColor = if (entry.status == TodayStatus.PENDING) DobiWarning else style.accent
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color(0x14000000)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Accent strip — switches to warning amber while still pending
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(accentColor)
            )
            Spacer(Modifier.width(12.dp))

            // Emoji bubble
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(style.tint),
                contentAlignment = Alignment.Center
            ) {
                Text(text = style.emoji, fontSize = 22.sp)
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = style.label,
                        fontSize = 10.sp,
                        fontWeight = SFProSemiBold,
                        color = style.accent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(style.tint)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    StatusPill(entry.status)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = SFProSemiBold,
                    color = DobiTextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = entry.subject,
                    style = MaterialTheme.typography.bodySmall,
                    color = DobiTextSecondary
                )
            }

            Spacer(Modifier.width(10.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = entry.timeLabel,
                    fontSize = 11.sp,
                    color = DobiTextSecondary,
                    maxLines = 1
                )
                Spacer(Modifier.height(4.dp))
                when {
                    entry.scoreLabel != null -> {
                        Text(
                            text = entry.scoreLabel,
                            fontSize = 15.sp,
                            fontWeight = SFProBold,
                            color = style.accent
                        )
                        Text(
                            text = "score",
                            fontSize = 10.sp,
                            color = DobiTextSecondary
                        )
                    }
                    entry.status == TodayStatus.COMPLETED -> {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(DobiSuccessContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Done",
                                tint = DobiSuccess,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    else -> {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = DobiTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(status: TodayStatus) {
    val (label, fg, bg) = when (status) {
        TodayStatus.COMPLETED -> Triple("Done", DobiSuccess, DobiSuccessContainer)
        TodayStatus.PENDING -> Triple("Pending", DobiWarning, DobiWarningContainer)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(fg)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = SFProSemiBold,
            color = fg
        )
    }
}

// ── Revision Hub: item card (mirrors NeedRevisionScreen.RevisionItemCard) ──────

@Composable
private fun RevisionItemCard(
    mark: RevisionMark,
    onClick: () -> Unit,
    onUnmark: () -> Unit
) {
    val accent = parseHexColor(mark.subjectColor)
    val isUnit = RevisionScope.fromKey(mark.scope) == RevisionScope.UNIT
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.06f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = mark.subjectIcon.ifBlank { "📘" }, fontSize = 24.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (isUnit) "Lesson" else "Concept",
                        color = Color(0xFFF59E0B),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = mark.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = DobiTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                Spacer(Modifier.height(2.dp))
                val secondary = if (!isUnit && mark.unitName.isNotBlank())
                    "${mark.subject} • ${mark.unitName}"
                else mark.subject.ifBlank { "Topic" }
                Text(
                    text = secondary,
                    style = MaterialTheme.typography.bodySmall,
                    color = DobiTextSecondary,
                    fontSize = 12.sp
                )
            }
            IconButton(onClick = onUnmark) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = DobiTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun parseHexColor(hex: String): Color = runCatching {
    if (hex.isBlank()) Color(0xFF94A3B8)
    else Color(android.graphics.Color.parseColor(hex))
}.getOrDefault(Color(0xFF94A3B8))

@Composable
private fun RevisionEmptyStateCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                Color(0xFFE5EBF5),
                RoundedCornerShape(16.dp)
            ),
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
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(DobiPrimaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text("🔖", fontSize = 22.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "No topics flagged yet",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = SFProSemiBold,
                    color = DobiTextPrimary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Open a lesson or concept and tap Mark for Revision. We'll keep them here so you can come back to them later.",
                    style = MaterialTheme.typography.bodySmall,
                    color = DobiTextSecondary,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

// ── Do You Know: current affair card ───────────────────────────────────────────

private fun categoryColor(category: String): Pair<Color, Color> = when (category.lowercase()) {
    "sports"     -> Color(0xFF16A34A) to Color(0xFFDCFCE7)
    "science"    -> Color(0xFF2563EB) to Color(0xFFDBEAFE)
    "space"      -> Color(0xFF7C3AED) to Color(0xFFEDE9FE)
    "technology" -> Color(0xFF0891B2) to Color(0xFFCFFAFE)
    "environment", "nature" -> Color(0xFF15803D) to Color(0xFFBBF7D0)
    "world"      -> Color(0xFFDC2626) to Color(0xFFFEE2E2)
    "fun fact"   -> Color(0xFFE67E22) to Color(0xFFFFF4E0)
    else         -> Color(0xFF6B7A99) to Color(0xFFF1F5F9)
}

@Composable
private fun CurrentAffairCard(news: CurrentAffair, onClick: () -> Unit = {}) {
    val (accent, tint) = categoryColor(news.category)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color(0x14000000)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(16.dp),
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
                    .width(4.dp)
                    .height(56.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(accent)
            )
            Spacer(Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(tint),
                contentAlignment = Alignment.Center
            ) {
                Text(text = news.emoji, fontSize = 24.sp)
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = news.category,
                        fontSize = 10.sp,
                        fontWeight = SFProSemiBold,
                        color = accent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(tint)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = news.timeAgo,
                        fontSize = 11.sp,
                        color = DobiTextSecondary
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = news.headline,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = SFProSemiBold,
                    color = DobiTextPrimary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 19.sp
                )
            }
        }
    }
}

@Composable
private fun CurrentAffairsEmptyCard(
    isLoading: Boolean = false,
    error: String? = null,
    onRetry: () -> Unit = {}
) {
    val title = when {
        isLoading -> "Putting today's paper together…"
        error != null -> "Couldn't load today's paper"
        else -> "Fresh stories coming soon"
    }
    val subtitle = when {
        isLoading -> "Hand-picked, kid-safe stories. Just a few seconds…"
        error != null -> error
        else -> "We'll pull the day's interesting news for you every 24 hours."
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE5EBF5), RoundedCornerShape(16.dp))
            .let { if (!isLoading) it.clickable(onClick = onRetry) else it },
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
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (error != null) Color(0xFFFEE2E2) else DobiSuccessContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(if (error != null) "📵" else "📰", fontSize = 22.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = SFProSemiBold,
                    color = DobiTextPrimary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = DobiTextSecondary,
                    lineHeight = 17.sp
                )
                if (error != null && !isLoading) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Tap to retry",
                        style = MaterialTheme.typography.bodySmall,
                        color = DobiPrimary,
                        fontWeight = SFProMedium
                    )
                }
            }
        }
    }
}

// ── Previews ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true, name = "My Learnings — empty")
@Composable
private fun MyLearningsEmptyPreview() {
    DobiTheme {
        MyLearningsLayout(
            uiState = MyLearningsUiState(),
            contentPadding = PaddingValues(0.dp),
            onMarkClick = {},
            onUnmark = {},
            onSeeAllRevision = {},
            onTodayItemClick = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "My Learnings — populated")
@Composable
private fun MyLearningsPopulatedPreview() {
    DobiTheme {
        MyLearningsLayout(
            uiState = MyLearningsUiState(
                todaysItems = listOf(
                    TodayLearningEntry(
                        id = "t1",
                        kind = TodayKind.HOMEWORK,
                        title = "Fractions worksheet — 10 questions",
                        subject = "Mathematics",
                        timeLabel = "25 min ago"
                    ),
                    TodayLearningEntry(
                        id = "t2",
                        kind = TodayKind.QUIZ,
                        title = "Water Cycle Quick Quiz",
                        subject = "Science",
                        timeLabel = "1 hr ago",
                        scoreLabel = "8/10"
                    ),
                    TodayLearningEntry(
                        id = "t3",
                        kind = TodayKind.TASK,
                        title = "Write a paragraph on rain",
                        subject = "English",
                        timeLabel = "2 hr ago"
                    )
                ),
                revisionMarks = listOf(
                    RevisionMark(
                        id = "r1",
                        scope = "unit",
                        targetId = "u1",
                        name = "Food: Where Does It Come From?",
                        subject = "Science",
                        subjectColor = "#2563EB",
                        subjectIcon = "🔬"
                    ),
                    RevisionMark(
                        id = "r2",
                        scope = "concept",
                        targetId = "c1",
                        name = "Addition of Fractions",
                        subject = "Mathematics",
                        subjectColor = "#7C3AED",
                        subjectIcon = "➕",
                        unitName = "Fractions"
                    ),
                    RevisionMark(
                        id = "r3",
                        scope = "unit",
                        targetId = "u2",
                        name = "Our Environment",
                        subject = "Social Studies",
                        subjectColor = "#16A34A",
                        subjectIcon = "🌍"
                    )
                ),
                currentAffairs = listOf(
                    CurrentAffair(
                        id = "n1",
                        headline = "ISRO successfully tests reusable rocket stage in Sriharikota",
                        category = "Space",
                        timeAgo = "4h ago",
                        emoji = "🚀"
                    ),
                    CurrentAffair(
                        id = "n2",
                        headline = "India wins first gold at the Asian Junior Athletics Championship",
                        category = "Sports",
                        timeAgo = "9h ago",
                        emoji = "🏅"
                    ),
                    CurrentAffair(
                        id = "n3",
                        headline = "Scientists discover a new species of frog in the Western Ghats",
                        category = "Nature",
                        timeAgo = "18h ago",
                        emoji = "🐸"
                    )
                )
            ),
            contentPadding = PaddingValues(0.dp),
            onMarkClick = {},
            onUnmark = {},
            onSeeAllRevision = {},
            onTodayItemClick = {}
        )
    }
}
