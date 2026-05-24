package com.dobi.app.feature.revisit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.dobi.app.core.ui.theme.DobiBackground
import com.dobi.app.core.ui.theme.DobiPrimary
import com.dobi.app.core.ui.theme.DobiTextPrimary
import com.dobi.app.core.ui.theme.DobiTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RevisitDetailScreen(
    onNavigateBack: () -> Unit = {},
    onTakeQuiz: (unitId: String, conceptId: String) -> Unit = { _, _ -> },
    onViewLastResult: (unitId: String, conceptId: String) -> Unit = { _, _ -> },
    onViewSubjectiveResult: (resultId: String) -> Unit = {},
    isParentView: Boolean = false,
    viewModel: RevisitDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var fullscreenIndex by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        containerColor = DobiBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.title.ifBlank { "Topic" },
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
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                uiState.photoUrls.isEmpty() -> EmptyState(
                    title = "Nothing here yet",
                    subtitle = "Photos uploaded for this topic will appear here."
                )
                else -> Column(modifier = Modifier.fillMaxSize()) {
                    HeaderRow(state = uiState)
                    Spacer(Modifier.height(8.dp))

                    if (uiState.canTakeQuiz && !isParentView) {
                        ActionRow(
                            onTakeQuiz = { onTakeQuiz(uiState.quizUnitId, uiState.quizConceptId) },
                            onViewLastResult = { onViewLastResult(uiState.quizUnitId, uiState.quizConceptId) },
                            hasLastResult = uiState.hasLastResult
                        )
                        Spacer(Modifier.height(12.dp))
                    }

                    if (uiState.attempts.isNotEmpty()) {
                        AttemptsSection(
                            attempts = uiState.attempts,
                            onMcqClick = {
                                onViewLastResult(uiState.quizUnitId, uiState.quizConceptId)
                            },
                            onSubjectiveClick = onViewSubjectiveResult
                        )
                        Spacer(Modifier.height(12.dp))
                    }

                    Text(
                        text = "Photos",
                        style = MaterialTheme.typography.titleSmall,
                        color = DobiTextPrimary,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(8.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.photoUrls.size) { index ->
                            val url = uiState.photoUrls[index]
                            PhotoTile(url = url, onClick = { fullscreenIndex = index })
                        }
                    }
                }
            }
        }
    }

    fullscreenIndex?.let { index ->
        FullscreenPhoto(
            url = uiState.photoUrls.getOrNull(index).orEmpty(),
            onDismiss = { fullscreenIndex = null }
        )
    }
}

@Composable
private fun HeaderRow(state: RevisitDetailUiState) {
    val accent = parseHexColor(state.subjectColor)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = state.subjectIcon.ifBlank { "📘" }, fontSize = 18.sp)
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = state.subject.ifBlank { "—" },
                style = MaterialTheme.typography.bodyMedium,
                color = DobiTextPrimary
            )
            Text(
                text = "${state.uploadCount} ${if (state.uploadCount == 1) "upload" else "uploads"} • ${formatDate(state.latestTimestamp)}",
                style = MaterialTheme.typography.bodySmall,
                color = DobiTextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun ActionRow(onTakeQuiz: () -> Unit, onViewLastResult: () -> Unit, hasLastResult: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = onTakeQuiz,
            modifier = Modifier.weight(1f).height(46.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DobiPrimary)
        ) {
            Text("Take Quiz", fontSize = 14.sp)
        }
        if (hasLastResult) {
            OutlinedButton(
                onClick = onViewLastResult,
                modifier = Modifier.weight(1f).height(46.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("View Last Result", fontSize = 14.sp, color = DobiTextPrimary)
            }
        }
    }
}

@Composable
private fun AttemptsSection(
    attempts: List<RevisitAttemptItem>,
    onMcqClick: () -> Unit,
    onSubjectiveClick: (String) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "Attempts",
            style = MaterialTheme.typography.titleSmall,
            color = DobiTextPrimary
        )
        Spacer(Modifier.height(8.dp))
        attempts.forEach { item ->
            when (item) {
                is RevisitAttemptItem.McqLast -> AttemptCard(
                    emoji = "📊",
                    title = "MCQ Quiz",
                    subtitle = "${item.correct}/${item.total} correct" +
                        if (item.attempts > 1) " · ${item.attempts} tries" else "",
                    accuracyPct = item.accuracyPct,
                    timestamp = item.timestamp,
                    accent = Color(0xFF2563EB),
                    bg = Color(0xFFDBEAFE),
                    onClick = onMcqClick
                )
                is RevisitAttemptItem.Subjective -> AttemptCard(
                    emoji = "✍️",
                    title = "Written Answers",
                    subtitle = "${item.correct}/${item.attempted} marked correct",
                    accuracyPct = item.accuracyPct,
                    timestamp = item.timestamp,
                    accent = Color(0xFFD97706),
                    bg = Color(0xFFFEF3C7),
                    onClick = { onSubjectiveClick(item.resultId) }
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun AttemptCard(
    emoji: String,
    title: String,
    subtitle: String,
    accuracyPct: Int,
    timestamp: Long,
    accent: Color,
    bg: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg.copy(alpha = 0.5f))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(bg),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji, fontSize = 18.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = DobiTextPrimary,
                fontSize = 14.sp
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = DobiTextSecondary,
                fontSize = 12.sp
            )
            Text(
                text = formatRelative(timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = DobiTextSecondary,
                fontSize = 11.sp
            )
        }
        Text(
            text = "$accuracyPct%",
            color = accent,
            fontSize = 18.sp,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun PhotoTile(url: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF1F5F9))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun FullscreenPhoto(url: String, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White
            )
        }
    }
}

private fun formatDate(millis: Long): String {
    if (millis <= 0) return ""
    return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(millis))
}

private fun formatRelative(millis: Long): String {
    if (millis <= 0) return ""
    val diff = System.currentTimeMillis() - millis
    val mins = diff / 60_000
    val hours = mins / 60
    val days = hours / 24
    return when {
        mins < 1 -> "Just now"
        mins < 60 -> "${mins}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> formatDate(millis)
    }
}
