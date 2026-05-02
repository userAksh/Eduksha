package com.dobi.app.feature.streak

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dobi.app.core.ui.theme.DobiBackground
import com.dobi.app.core.ui.theme.DobiPrimary
import com.dobi.app.core.ui.theme.DobiPrimarySoft
import com.dobi.app.core.ui.theme.DobiTextPrimary
import com.dobi.app.core.ui.theme.DobiTextSecondary
import com.dobi.app.core.ui.theme.SFProBold
import com.dobi.app.core.ui.theme.SFProSemiBold

@Composable
fun StreakShareOptionsScreen(
    onNavigateBack: () -> Unit = {},
    onLaunchQuiz: (cacheKey: String, displayName: String) -> Unit = { _, _ -> },
    onFinishedShareOnly: () -> Unit = {},
    viewModel: StreakShareOptionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Take Quiz path: jump straight into the quiz. Share happens later from
    // the Snap & Send button on the result screen.
    val pendingLaunchQuiz = uiState.launchQuiz
    LaunchedEffect(pendingLaunchQuiz) {
        val launch = pendingLaunchQuiz ?: return@LaunchedEffect
        viewModel.onQuizLaunchConsumed()
        onLaunchQuiz(launch.cacheKey, launch.displayName)
    }

    // Share-without-quiz path: fire share intent, then pop back.
    val pendingShareText = uiState.pendingShareText
    LaunchedEffect(pendingShareText) {
        if (pendingShareText == null) return@LaunchedEffect
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, pendingShareText)
        }
        context.startActivity(Intent.createChooser(intent, "Share your streak"))
        viewModel.onShareConsumed()
        viewModel.onSharedWithoutQuizCompleted()
    }

    LaunchedEffect(uiState.finishedShareOnly) {
        if (uiState.finishedShareOnly) onFinishedShareOnly()
    }

    Scaffold(
        containerColor = DobiBackground,
        topBar = {
            ShareOptionsTopBar(onNavigateBack = onNavigateBack)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SelectionCard(
                displayName = uiState.displayName,
                isUnitScope = uiState.isUnitScope,
                subject = uiState.subject
            )
            Text(
                text = "How do you want to share?",
                color = DobiTextPrimary,
                fontSize = 14.sp,
                fontWeight = SFProBold,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
            QuizAndShareCard(
                isLoading = uiState.isGeneratingQuiz,
                onClick = viewModel::onTakeQuizAndShare
            )
            ShareOnlyCard(onClick = viewModel::onShareWithoutQuiz)

            uiState.error?.let { msg ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFEECEC))
                        .border(1.dp, Color(0xFFF5BFBF), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = msg,
                        color = Color(0xFF8A1F1F),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShareOptionsTopBar(onNavigateBack: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = "Share Today's Streak",
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

@Composable
private fun SelectionCard(
    displayName: String,
    isUnitScope: Boolean,
    subject: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DobiPrimarySoft)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "📚", fontSize = 20.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                color = DobiTextPrimary,
                fontSize = 14.sp,
                fontWeight = SFProBold
            )
            Text(
                text = buildString {
                    append(if (isUnitScope) "Lesson" else "Concept")
                    if (subject.isNotBlank()) {
                        append(" · ")
                        append(subject)
                    }
                },
                color = DobiTextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun QuizAndShareCard(isLoading: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFFFF7E6))
            .border(1.dp, Color(0xFFFEEAB6), RoundedCornerShape(16.dp))
            .clickable(enabled = !isLoading, onClick = onClick)
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🎯", fontSize = 24.sp)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Take Quiz & Share",
                        color = DobiTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = SFProBold
                    )
                    Text(
                        text = "5 quick questions on your topic, then share a streak snap with friends",
                        color = DobiTextSecondary,
                        fontSize = 12.sp
                    )
                }
                if (isLoading) {
                    CircularProgressIndicator(
                        color = DobiPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = DobiTextSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Don't worry — your marks won't be shared 🤫",
                    color = DobiTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            if (isLoading) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Dobi is preparing your 5 questions…",
                    color = DobiPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ShareOnlyCard(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE5EBF5), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "📤", fontSize = 24.sp)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Share Without Quiz",
                color = DobiTextPrimary,
                fontSize = 15.sp,
                fontWeight = SFProBold
            )
            Text(
                text = "Just send a quick streak snap to your friends",
                color = DobiTextSecondary,
                fontSize = 12.sp
            )
        }
    }
}
