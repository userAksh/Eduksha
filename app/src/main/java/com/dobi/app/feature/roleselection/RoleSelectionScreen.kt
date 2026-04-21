package com.dobi.app.feature.roleselection

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import com.dobi.app.core.ui.theme.DobiBackground
import com.dobi.app.core.ui.theme.DobiPrimary
import com.dobi.app.core.ui.theme.DobiTextPrimary
import com.dobi.app.core.ui.theme.DobiTextSecondary
import com.dobi.app.core.ui.theme.DobiTheme
import com.dobi.app.domain.model.UserRole

// ── Entry point wired to ViewModel ────────────────────────────────────────────

@Composable
fun RoleSelectionScreen(
    onStudentSelected: () -> Unit,
    onParentSelected: () -> Unit,
    viewModel: RoleSelectionViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { role ->
            when (role) {
                UserRole.STUDENT -> onStudentSelected()
                UserRole.PARENT -> onParentSelected()
            }
        }
    }

    RoleSelectionContent(onRoleSelected = viewModel::onRoleSelected)
}

// ── Pure UI (previewable) ──────────────────────────────────────────────────────

@Composable
private fun RoleSelectionContent(onRoleSelected: (UserRole) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DobiBackground)
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(52.dp))

            DobiLogoRow()

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "Who is using the app?",
                style = MaterialTheme.typography.headlineMedium,
                color = DobiTextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Choose your role to get started",
                style = MaterialTheme.typography.bodyMedium,
                color = DobiTextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            RoleCard(
                emoji = "👦",
                avatarBackground = Color(0xFFDBEAFE),
                title = "Student",
                description = "I want to track\nmy learning",
                onClick = { onRoleSelected(UserRole.STUDENT) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            RoleCard(
                emoji = "👩",
                avatarBackground = Color(0xFFDBEAFE),
                title = "Parent",
                description = "I want to track my\nchild's learning",
                onClick = { onRoleSelected(UserRole.PARENT) }
            )

            Spacer(modifier = Modifier.weight(1f))

            DisclaimerRow()

            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

// ── Dobi logo ─────────────────────────────────────────────────────────────────

@Composable
private fun DobiLogoRow() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DobiPrimary),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "😊", fontSize = 26.sp)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "Dobi",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = DobiPrimary
            )
        )
    }
}

// ── Role card ─────────────────────────────────────────────────────────────────

@Composable
private fun RoleCard(
    emoji: String,
    avatarBackground: Color,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(20.dp), ambientColor = Color(0x1A2563EB), spotColor = Color(0x1A2563EB))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Character avatar circle
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(avatarBackground),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 48.sp)
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Role text
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "I am a",
                    style = MaterialTheme.typography.bodySmall,
                    color = DobiTextSecondary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = DobiPrimary
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = DobiTextSecondary,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Arrow CTA button
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(DobiPrimary)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Select $title",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ── Footer disclaimer ─────────────────────────────────────────────────────────

@Composable
private fun DisclaimerRow() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(text = "🔒", fontSize = 13.sp)
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = "This is a temporary login. No sign up required.",
            style = MaterialTheme.typography.bodySmall,
            color = DobiTextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun RoleSelectionPreview() {
    DobiTheme {
        RoleSelectionContent(onRoleSelected = {})
    }
}
