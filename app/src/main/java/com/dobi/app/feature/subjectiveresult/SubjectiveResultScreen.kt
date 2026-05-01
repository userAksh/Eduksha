package com.dobi.app.feature.subjectiveresult

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.dobi.app.core.ui.theme.DobiTextPrimary
import com.dobi.app.core.ui.theme.DobiTextSecondary
import com.dobi.app.domain.model.SubjectiveResult
import com.dobi.app.domain.model.SubjectiveResultItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectiveResultScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: SubjectiveResultViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = DobiBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.result?.displayName?.ifBlank { "Written Answers" } ?: "Written Answers",
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
                uiState.error != null || uiState.result == null -> Box(
                    Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.error ?: "Result not found",
                        color = DobiTextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                else -> ResultBody(result = uiState.result!!)
            }
        }
    }
}

@Composable
private fun ResultBody(result: SubjectiveResult) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { SummaryCard(result) }
        items(result.items) { item -> AnswerCard(item) }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun SummaryCard(result: SubjectiveResult) {
    val accent = when {
        result.accuracyPct >= 80 -> Color(0xFF22C55E)
        result.accuracyPct >= 50 -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(accent.copy(alpha = 0.08f))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "✍️", fontSize = 26.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${result.correctCount}/${result.attemptedCount} correct",
                    color = DobiTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = formatDate(result.timestamp),
                    color = DobiTextSecondary,
                    fontSize = 12.sp
                )
            }
            Text(
                text = "${result.accuracyPct}%",
                color = accent,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AnswerCard(item: SubjectiveResultItem) {
    val verdictAccent = when {
        !item.isAttempted -> Color(0xFF94A3B8)
        item.isCorrect -> Color(0xFF22C55E)
        else -> Color(0xFFEF4444)
    }
    val verdictLabel = when {
        !item.isAttempted -> "Skipped"
        item.isCorrect -> "Correct"
        else -> "Needs work"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(verdictAccent.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = verdictLabel,
                    color = verdictAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (item.score > 0f) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Score: ${formatScore(item.score)}",
                    color = DobiTextSecondary,
                    fontSize = 11.sp
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Q: ${item.questionText}",
            color = DobiTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        if (item.answerSeen.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Your answer",
                color = DobiTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = item.answerSeen,
                color = DobiTextPrimary,
                fontSize = 13.sp
            )
        }
        if (item.softVerdict.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Dobi says",
                color = DobiTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = item.softVerdict,
                color = DobiTextPrimary,
                fontSize = 13.sp
            )
        }
        if (item.strictFeedback.isNotBlank() && item.strictFeedback != item.softVerdict) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = item.strictFeedback,
                color = DobiTextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

private fun formatDate(millis: Long): String {
    if (millis <= 0) return ""
    return SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault()).format(Date(millis))
}

private fun formatScore(score: Float): String {
    return if (score == score.toInt().toFloat()) "${score.toInt()}"
    else String.format(Locale.US, "%.1f", score)
}
