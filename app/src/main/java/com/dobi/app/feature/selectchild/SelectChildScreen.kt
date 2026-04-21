package com.dobi.app.feature.selectchild

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dobi.app.domain.model.Child
import com.dobi.app.core.ui.theme.DobiPrimary
import com.dobi.app.core.ui.theme.DobiTextPrimary
import com.dobi.app.core.ui.theme.DobiTextSecondary

private val cardPalette = listOf(
    Triple(Color(0xFFDBEAFE), Color(0xFF1D4ED8), "👦"),
    Triple(Color(0xFFFCE7F3), Color(0xFFBE185D), "👧"),
    Triple(Color(0xFFD1FAE5), Color(0xFF047857), "🧒"),
    Triple(Color(0xFFFEF3C7), Color(0xFFB45309), "👦"),
    Triple(Color(0xFFEDE9FE), Color(0xFF6D28D9), "👧"),
)

@Composable
fun SelectChildScreen(
    onChildSelected: (childId: String) -> Unit,
    viewModel: SelectChildViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(DobiPrimary)
        )

        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .systemBarsPadding()
                    .padding(top = 32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "👨‍👩‍👧‍👦", fontSize = 26.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Select Profile",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Who is using the app right now?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = Color(0xFFF5F7FF),
                shadowElevation = 0.dp
            ) {
                when (val state = uiState) {
                    is SelectChildViewModel.UiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = DobiPrimary)
                        }
                    }
                    is SelectChildViewModel.UiState.Ready -> {
                        LazyColumn(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)
                        ) {
                            itemsIndexed(state.children) { index, child ->
                                ChildProfileCard(
                                    child = child,
                                    colorIndex = index,
                                    onClick = { onChildSelected(child.id) }
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                    is SelectChildViewModel.UiState.Error -> {
                        Box(modifier = Modifier.padding(24.dp)) {
                            Text(state.message, color = Color(0xFFDC2626))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChildProfileCard(child: Child, colorIndex: Int, onClick: () -> Unit) {
    val (bg, accent, emoji) = cardPalette[colorIndex % cardPalette.size]

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(bg),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 32.sp)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = child.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = DobiTextPrimary
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Chip(text = "Class ${child.grade}", bg = bg, accent = accent)
                    Spacer(modifier = Modifier.width(6.dp))
                    Chip(text = child.board, bg = bg, accent = accent)
                }
            }

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun Chip(text: String, bg: Color, accent: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = accent
        )
    }
}
