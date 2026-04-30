package com.dobi.app.feature.testyourself

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Quiz
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
import com.dobi.app.core.ui.theme.DobiPrimary
import com.dobi.app.core.ui.theme.DobiTextPrimary
import com.dobi.app.core.ui.theme.DobiTextSecondary
import com.dobi.app.core.ui.theme.SFProBold
import com.dobi.app.core.ui.theme.SFProSemiBold

const val SOURCE_CURATED = "curated"
const val SOURCE_AI = "ai"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestYourselfChooserScreen(
    onNavigateBack: () -> Unit = {},
    onPickSource: (source: String, unitId: String, conceptId: String) -> Unit = { _, _, _ -> },
    viewModel: TestYourselfChooserViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = DobiBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Answer Questions",
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
        when {
            uiState.isLoading -> Box(
                Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = DobiPrimary) }

            uiState.error != null -> Box(
                Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { Text(uiState.error!!, color = DobiTextSecondary) }

            else -> SourcePickerContent(
                state = uiState,
                onPickSource = { source ->
                    onPickSource(source, uiState.unitId, uiState.conceptId)
                },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun SourcePickerContent(
    state: TestYourselfChooserUiState,
    onPickSource: (source: String) -> Unit,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        // Lesson / concept context chip
        LessonContextChip(state = state)

        Spacer(Modifier.height(20.dp))

        Text(
            text = "How would you like to practise?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = SFProBold,
            color = DobiTextPrimary,
            fontSize = 22.sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Pick one — both work great for what you've learned.",
            style = MaterialTheme.typography.bodyMedium,
            color = DobiTextSecondary
        )

        Spacer(Modifier.height(20.dp))

        SourceCard(
            iconBg = Color(0xFFDBEAFE),
            icon = Icons.Default.Quiz,
            iconTint = DobiPrimary,
            badge = "Recommended",
            badgeBg = Color(0xFFDBEAFE),
            badgeFg = DobiPrimary,
            title = "Our Question Set",
            subtitle = "Practise with curated questions made for this lesson by Dobi.",
            onClick = { onPickSource(SOURCE_CURATED) }
        )

        Spacer(Modifier.height(12.dp))

        SourceCard(
            iconBg = Color(0xFFEDE9FE),
            icon = Icons.Default.AutoAwesome,
            iconTint = Color(0xFF7C3AED),
            badge = "✨ AI-powered",
            badgeBg = Color(0xFFEDE9FE),
            badgeFg = Color(0xFF7C3AED),
            title = "Upload Your Pages",
            subtitle = "Snap your textbook questions or chapter pages — Dobi will turn them into questions for you.",
            onClick = { onPickSource(SOURCE_AI) }
        )

        Spacer(Modifier.height(20.dp))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "You can switch between options anytime.",
                style = MaterialTheme.typography.bodySmall,
                color = DobiTextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun LessonContextChip(state: TestYourselfChooserUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFDBEAFE))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = if (state.isUnitScope) "📘" else "🔍", fontSize = 14.sp)
        Spacer(Modifier.width(8.dp))
        Text(
            text = state.title,
            color = DobiPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Text(text = "•", color = DobiPrimary.copy(alpha = 0.6f), fontSize = 14.sp)
        Spacer(Modifier.width(6.dp))
        Text(
            text = state.subject,
            color = DobiPrimary.copy(alpha = 0.8f),
            fontSize = 13.sp
        )
    }
}

@Composable
private fun SourceCard(
    iconBg: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    badge: String,
    badgeBg: Color,
    badgeFg: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(BorderStroke(1.dp, Color(0xFFE5E7EB)), RoundedCornerShape(18.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(badgeBg)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = badge,
                    color = badgeFg,
                    fontSize = 11.sp,
                    fontWeight = SFProSemiBold
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = SFProBold,
                color = DobiTextPrimary,
                fontSize = 17.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = DobiTextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
