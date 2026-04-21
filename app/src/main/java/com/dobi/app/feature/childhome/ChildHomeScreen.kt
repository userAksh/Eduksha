package com.dobi.app.feature.childhome

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dobi.app.core.ui.components.DobiBottomNavBar
import com.dobi.app.core.ui.components.childNavItems
import com.dobi.app.core.ui.theme.DobiBackground
import com.dobi.app.core.ui.theme.SFProBold
import com.dobi.app.core.ui.theme.SFProSemiBold
import com.dobi.app.core.ui.theme.SFProMedium
import com.dobi.app.core.ui.theme.SFProRegular
import com.dobi.app.core.ui.theme.SFProLight
import com.dobi.app.core.ui.theme.DobiError
import com.dobi.app.core.ui.theme.DobiErrorContainer
import com.dobi.app.core.ui.theme.DobiPrimary
import com.dobi.app.core.ui.theme.DobiPrimaryContainer
import com.dobi.app.core.ui.theme.DobiSuccess
import com.dobi.app.core.ui.theme.DobiSuccessContainer
import com.dobi.app.core.ui.theme.DobiTextPrimary
import com.dobi.app.core.ui.theme.DobiTextSecondary
import com.dobi.app.core.ui.theme.DobiTheme
import com.dobi.app.domain.model.LearningStatus
import com.dobi.app.domain.model.TodayLearningItem

// ── Entry point ────────────────────────────────────────────────────────────────

@Composable
fun ChildHomeScreen(
    onNavigateToConceptList: () -> Unit = {},
    onNavigateToHomeworkLog: () -> Unit = {},
    onRequestParentAccess: () -> Unit = {},
    viewModel: ChildHomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    // Lock back button — child cannot return to parent view without the PIN gate
    androidx.activity.compose.BackHandler(enabled = true) {
        onRequestParentAccess()
    }

    Scaffold(
        containerColor = DobiBackground,
        bottomBar = {
            DobiBottomNavBar(
                items = childNavItems,
                selectedIndex = selectedTab,
                onItemSelected = { selectedTab = it }
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = {},
                    containerColor = DobiPrimary,
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Log session", tint = Color.White)
                }
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            0 -> ChildHomeContent(
                uiState = uiState,
                onStartLearning = onNavigateToConceptList,
                onViewConcepts = onNavigateToConceptList,
                onNavigateToConceptList = onNavigateToConceptList,
                onNavigateToHomeworkLog = onNavigateToHomeworkLog,
                onItemClick = {},
                contentPadding = innerPadding
            )
            1 -> TabPlaceholder("Concepts", innerPadding)
            2 -> TabPlaceholder("Profile", innerPadding)
        }
    }
}

// ── Main scrollable content ────────────────────────────────────────────────────

@Composable
private fun ChildHomeContent(
    uiState: ChildHomeUiState,
    onStartLearning: () -> Unit,
    onViewConcepts: () -> Unit,
    onNavigateToConceptList: () -> Unit,
    onNavigateToHomeworkLog: () -> Unit,
    onItemClick: (String) -> Unit,
    contentPadding: PaddingValues
) {
    var selectedModeIndex by remember { mutableStateOf<Int?>(null) }
    // Keep last valid index so exit animation still renders correct content
    var lastModeIndex by remember { mutableIntStateOf(0) }

    Box(Modifier.fillMaxSize()) {

        LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DobiBackground),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = contentPadding.calculateTopPadding() + 28.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Greeting
        item {
            Text(
                text = "Hi, ${uiState.childName}! \uD83D\uDC4B",
                style = MaterialTheme.typography.headlineMedium,
                color = DobiTextPrimary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Keep up the great learning!",
                style = MaterialTheme.typography.bodyMedium,
                color = DobiTextSecondary
            )
            Spacer(Modifier.height(20.dp))
        }

        // Streak banner
        item {
            StreakBanner(streakDays = uiState.streakDays)
            Spacer(Modifier.height(16.dp))
        }

        // Revision mode cards
        item {
            RevisionModeRow(onRevisionModeClick = { lastModeIndex = it; selectedModeIndex = it })
            Spacer(Modifier.height(20.dp))
        }

        // CTAs
        item {
            Button(
                onClick = onStartLearning,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DobiPrimary)
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Start Learning",
                    fontWeight = SFProSemiBold,
                    fontSize = 15.sp
                )
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onViewConcepts,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, DobiPrimary),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DobiPrimary)
            ) {
                Icon(
                    Icons.Default.MenuBook,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "View Concepts",
                    fontWeight = SFProSemiBold,
                    fontSize = 15.sp
                )
            }
            Spacer(Modifier.height(28.dp))
        }

        // Section header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Today's Learning",
                    style = MaterialTheme.typography.titleMedium,
                    color = DobiTextPrimary
                )
                Text(
                    "See all",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DobiPrimary,
                    fontWeight = SFProMedium
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        // Learning items
        items(uiState.todayItems) { item ->
            LearningItemCard(
                item = item,
                onClick = { onItemClick(item.conceptId) }
            )
            Spacer(Modifier.height(10.dp))
        }
        }  // end LazyColumn

        // Dim scrim
        AnimatedVisibility(
            visible = selectedModeIndex != null,
            enter = fadeIn(tween(220)),
            exit  = fadeOut(tween(180))
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { selectedModeIndex = null }
            )
        }

        // Popup card — springs open from the card row (origin y ≈ 0.72)
        AnimatedVisibility(
            visible = selectedModeIndex != null,
            modifier = Modifier.align(Alignment.Center),
            enter = scaleIn(
                initialScale = 0.45f,
                transformOrigin = TransformOrigin(0.5f, 0.72f),
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness    = Spring.StiffnessMediumLow
                )
            ) + fadeIn(tween(160)),
            exit = scaleOut(
                targetScale = 0.55f,
                transformOrigin = TransformOrigin(0.5f, 0.72f),
                animationSpec = tween(160)
            ) + fadeOut(tween(140))
        ) {
            RevisionDialog(
                modeIndex = lastModeIndex,
                onDismiss = { selectedModeIndex = null },
                onOptionClick = { optionIndex ->
                    selectedModeIndex = null
                    when {
                        lastModeIndex == 0 && optionIndex == 0 -> onNavigateToConceptList()
                        lastModeIndex == 0 && optionIndex == 1 -> onNavigateToHomeworkLog()
                    }
                }
            )
        }

    } // end Box
}

// ── Revision mode row ──────────────────────────────────────────────────────────

private data class RevisionMode(
    val emoji: String,
    val title: String,
    val subtitle: String,
    val cardColor: Color,
    val iconBg: Color,
    val accentColor: Color
)

private val revisionModes = listOf(
    RevisionMode(
        emoji = "🏫",
        title = "School\nWork",
        subtitle = "Revise from class",
        cardColor = Color(0xFFDBEAFE),
        iconBg = Color(0xFFBFD7F7),
        accentColor = Color(0xFF2563EB)
    ),
    RevisionMode(
        emoji = "📐",
        title = "Tuition\nWork",
        subtitle = "Revise from tutor",
        cardColor = Color(0xFFEDE9FE),
        iconBg = Color(0xFFDDD6FE),
        accentColor = Color(0xFF7C3AED)
    ),
    RevisionMode(
        emoji = "✨",
        title = "Self\nRevision",
        subtitle = "Study on your own",
        cardColor = Color(0xFFF0FDF4),
        iconBg = Color(0xFFDCFCE7),
        accentColor = Color(0xFF16A34A)
    )
)

@Composable
private fun RevisionModeRow(onRevisionModeClick: (Int) -> Unit) {
    Column {
        Text(
            text = "What would you like to revise?",
            style = MaterialTheme.typography.titleSmall,
            color = DobiTextPrimary,
            fontWeight = SFProSemiBold
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            revisionModes.forEachIndexed { index, mode ->
                RevisionCard(
                    mode = mode,
                    onClick = { onRevisionModeClick(index) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun RevisionCard(
    mode: RevisionMode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = mode.accentColor.copy(alpha = 0.18f),
                ambientColor = mode.accentColor.copy(alpha = 0.08f)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = mode.cardColor),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(mode.iconBg),
                contentAlignment = Alignment.Center
            ) {
                Text(text = mode.emoji, fontSize = 22.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = mode.title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = SFProBold,
                color = mode.accentColor,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = mode.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = DobiTextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp,
                fontSize = 10.sp
            )
        }
    }
}

// ── Revision bottom sheet ──────────────────────────────────────────────────────

private data class RevisionOption(
    val emoji: String,
    val title: String,
    val subtitle: String
)

private data class RevisionSheet(
    val title: String,
    val emoji: String,
    val options: List<RevisionOption>
)

private val revisionSheetOptions = listOf(
    RevisionSheet(
        title = "School Work",
        emoji = "🏫",
        options = listOf(
            RevisionOption("📖", "Practise a Lesson", "Go through a school lesson step by step"),
            RevisionOption("📝", "Log Homework", "Mark today's homework as completed"),
            RevisionOption("📋", "Prep for a Class Test", "Review and get ready for an upcoming test"),
        )
    ),
    RevisionSheet(
        title = "Tuition Work",
        emoji = "📐",
        options = listOf(
            RevisionOption("🎯", "Cover a Tuition Topic", "Work through a lesson from your tutor"),
            RevisionOption("✅", "Complete an Assignment", "Finish work given by your tutor"),
            RevisionOption("🔄", "Revise Before My Session", "Quick review before your next tuition class"),
        )
    ),
    RevisionSheet(
        title = "Self Revision",
        emoji = "✨",
        options = listOf(
            RevisionOption("🌱", "Explore a New Concept", "Pick something fresh and learn it today"),
            RevisionOption("🔁", "Revisit Past Topics", "Go back and strengthen what you've studied"),
            RevisionOption("🧠", "Test Yourself", "Quiz yourself to see how much you remember"),
        )
    )
)

@Composable
private fun RevisionDialog(
    modeIndex: Int,
    onDismiss: () -> Unit,
    onOptionClick: (Int) -> Unit = {}
) {
    val sheet = revisionSheetOptions[modeIndex]
    val mode = revisionModes[modeIndex]

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(mode.iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = sheet.emoji, fontSize = 24.sp)
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = sheet.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = SFProBold,
                        color = DobiTextPrimary
                    )
                    Text(
                        text = "What would you like to do?",
                        style = MaterialTheme.typography.bodySmall,
                        color = DobiTextSecondary
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            sheet.options.forEachIndexed { index, option ->
                RevisionOptionRow(
                    option = option,
                    accentColor = mode.accentColor,
                    iconBg = mode.iconBg,
                    onClick = { onOptionClick(index) }
                )
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Cancel",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
                    .padding(vertical = 10.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = SFProMedium,
                color = DobiTextSecondary
            )
        }
    }
}

@Composable
private fun RevisionOptionRow(
    option: RevisionOption,
    accentColor: Color,
    iconBg: Color,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(18.dp),
                spotColor = accentColor.copy(alpha = 0.10f),
                ambientColor = Color.Black.copy(alpha = 0.04f)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Text(text = option.emoji, fontSize = 22.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = option.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = SFProSemiBold,
                    color = DobiTextPrimary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = option.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = DobiTextSecondary,
                    lineHeight = 16.sp
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// ── Streak banner ──────────────────────────────────────────────────────────────

@Composable
private fun StreakBanner(streakDays: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DobiPrimary),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "\uD83D\uDD25", fontSize = 26.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "$streakDays days",
                        fontSize = 28.sp,
                        fontWeight = SFProBold,
                        color = Color.White
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "You're on a streak!",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "\uD83D\uDE0A", fontSize = 28.sp)
            }
        }
    }
}

// ── Learning item card ─────────────────────────────────────────────────────────

@Composable
private fun LearningItemCard(item: TodayLearningItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(12.dp), spotColor = Color(0x0D000000))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Subject icon
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(subjectColor(item.subject).copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = subjectEmoji(item.subject), fontSize = 20.sp)
            }

            Spacer(Modifier.width(12.dp))

            // Name + breadcrumb + status
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.conceptName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = SFProSemiBold,
                    color = DobiTextPrimary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${item.subject} \u2022 ${item.unit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = DobiTextSecondary
                )
                Spacer(Modifier.height(6.dp))
                StatusBadge(item.status)
            }

            Spacer(Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = DobiTextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ── Status badge ───────────────────────────────────────────────────────────────

@Composable
private fun StatusBadge(status: LearningStatus) {
    val (bg, fg, label) = when (status) {
        LearningStatus.COMPLETED -> Triple(DobiSuccessContainer, DobiSuccess, "✓  Completed")
        LearningStatus.IN_PROGRESS -> Triple(DobiPrimaryContainer, DobiPrimary, "●  In Progress")
        LearningStatus.NOT_STARTED -> Triple(Color(0xFFF1F5F9), DobiTextSecondary, "Not Started")
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = fg,
            fontWeight = SFProMedium
        )
    }
}

// ── Helpers ────────────────────────────────────────────────────────────────────

private fun subjectColor(subject: String): Color = when (subject) {
    "Mathematics" -> Color(0xFF2563EB)
    "Science"     -> Color(0xFF22C55E)
    "English"     -> Color(0xFF7C3AED)
    "Social Studies" -> Color(0xFFF59E0B)
    else          -> Color(0xFF64748B)
}

private fun subjectEmoji(subject: String): String = when (subject) {
    "Mathematics" -> "➕"
    "Science"     -> "\uD83D\uDD2C"   // 🔬
    "English"     -> "\uD83D\uDCD6"   // 📖
    "Social Studies" -> "\uD83C\uDF0D" // 🌍
    else          -> "\uD83D\uDCDA"   // 📚
}

// ── Tab placeholder ────────────────────────────────────────────────────────────

@Composable
private fun TabPlaceholder(label: String, padding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DobiBackground)
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$label — Coming Soon",
            style = MaterialTheme.typography.bodyLarge,
            color = DobiTextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

// ── Preview ────────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ChildHomePreview() {
    DobiTheme {
        ChildHomeContent(
            uiState = ChildHomeUiState(
                childName = "Emma",
                streakDays = 3,
                todayItems = listOf(
                    TodayLearningItem("1", "Addition of Fractions", "Mathematics", "Fractions", LearningStatus.COMPLETED),
                    TodayLearningItem("2", "Subtraction of Fractions", "Mathematics", "Fractions", LearningStatus.COMPLETED),
                    TodayLearningItem("3", "Water Cycle", "Science", "Our Environment", LearningStatus.IN_PROGRESS),
                )
            ),
            onStartLearning = {},
            onViewConcepts = {},
            onNavigateToConceptList = {},
            onNavigateToHomeworkLog = {},
            onItemClick = {},
            contentPadding = PaddingValues(0.dp)
        )
    }
}
