package com.dobi.app.feature.needrevision

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.dobi.app.domain.model.RevisionMark
import com.dobi.app.domain.model.RevisionScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeedRevisionScreen(
    onNavigateBack: () -> Unit = {},
    onOpenConcept: (String) -> Unit = {},
    onOpenUnit: (String) -> Unit = {},
    viewModel: NeedRevisionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = DobiBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Need Revision",
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
                uiState.isLoading -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                uiState.items.isEmpty() -> EmptyState()

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item { Spacer(Modifier.height(4.dp)) }
                    items(uiState.items, key = { it.id.ifBlank { it.targetId } }) { mark ->
                        RevisionItemCard(
                            mark = mark,
                            onClick = {
                                if (RevisionScope.fromKey(mark.scope) == RevisionScope.UNIT)
                                    onOpenUnit(mark.targetId)
                                else
                                    onOpenConcept(mark.targetId)
                            },
                            onUnmark = { viewModel.onUnmark(mark) }
                        )
                    }
                    item {
                        Spacer(Modifier.height(8.dp))
                        TipCard()
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

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

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🔖", fontSize = 36.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "No topics flagged yet",
            style = MaterialTheme.typography.titleMedium,
            color = DobiTextPrimary
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Open a lesson or concept and tap Mark for Revision. We'll keep them here so you can come back to them later.",
            style = MaterialTheme.typography.bodyMedium,
            color = DobiTextSecondary
        )
    }
}

@Composable
private fun TipCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFEF3C7))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("💡", fontSize = 18.sp)
        Spacer(Modifier.width(10.dp))
        Text(
            text = "Tip: Revisit one or two of these every day instead of cramming them all at once.",
            style = MaterialTheme.typography.bodySmall,
            color = DobiTextPrimary,
            fontSize = 12.sp
        )
    }
}

private fun parseHexColor(hex: String): Color = runCatching {
    if (hex.isBlank()) Color(0xFF94A3B8)
    else Color(android.graphics.Color.parseColor(hex))
}.getOrDefault(Color(0xFF94A3B8))
