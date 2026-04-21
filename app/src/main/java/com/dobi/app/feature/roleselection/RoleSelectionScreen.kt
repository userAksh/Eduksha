package com.dobi.app.feature.roleselection

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dobi.app.core.ui.theme.DobiBackground
import com.dobi.app.core.ui.theme.DobiBorder
import com.dobi.app.core.ui.theme.DobiError
import com.dobi.app.core.ui.theme.DobiErrorContainer
import com.dobi.app.core.ui.theme.DobiPrimary
import com.dobi.app.core.ui.theme.DobiPrimaryContainer
import com.dobi.app.core.ui.theme.DobiPrimaryLight
import com.dobi.app.core.ui.theme.DobiTextPrimary
import com.dobi.app.core.ui.theme.DobiTextSecondary
import com.dobi.app.core.ui.theme.DobiTextTertiary
import com.dobi.app.core.ui.theme.DobiTheme
import com.dobi.app.core.ui.theme.SFProBold
import com.dobi.app.core.ui.theme.SFProMedium
import com.dobi.app.core.ui.theme.SFProRegular
import com.dobi.app.core.ui.theme.SFProSemiBold

@Composable
fun RoleSelectionScreen(
    onNavigateToPhoneAuth: () -> Unit,
    onNavigateToParentHome: () -> Unit,
    onNavigateToSelectChild: () -> Unit,
    onNavigateToPairingCode: () -> Unit,
    viewModel: RoleSelectionViewModel = hiltViewModel()
) {
    val reauth by viewModel.reauth.collectAsStateWithLifecycle()
    var showNoChildrenDialog by remember { mutableStateOf(false) }
    val activity = LocalContext.current as Activity

    LaunchedEffect(Unit) {
        viewModel.navEvent.collect { event ->
            when (event) {
                RoleSelectionViewModel.NavEvent.ToPhoneAuth -> onNavigateToPhoneAuth()
                RoleSelectionViewModel.NavEvent.ToParentHome -> onNavigateToParentHome()
                RoleSelectionViewModel.NavEvent.ToSelectChild -> onNavigateToSelectChild()
                RoleSelectionViewModel.NavEvent.ToPairingCode -> onNavigateToPairingCode()
                RoleSelectionViewModel.NavEvent.ShowNoChildrenDialog -> showNoChildrenDialog = true
            }
        }
    }

    if (showNoChildrenDialog) {
        AlertDialog(
            onDismissRequest = { showNoChildrenDialog = false },
            title = { Text("No Child Profiles") },
            text = { Text("Ask your parent to add your profile from their account.") },
            confirmButton = {
                TextButton(onClick = { showNoChildrenDialog = false }) { Text("Got it") }
            }
        )
    }

    RoleSelectionContent(
        reauth = reauth,
        onParentSelected = viewModel::onParentSelected,
        onChildSelected = viewModel::onChildSelected,
        onSendOtp = { viewModel.sendOtp(activity) },
        onVerifyOtp = viewModel::verifyOtp,
        onDismissReauth = viewModel::dismissReauth
    )
}

@Composable
private fun RoleSelectionContent(
    reauth: RoleSelectionViewModel.ReauthState,
    onParentSelected: () -> Unit,
    onChildSelected: () -> Unit,
    onSendOtp: () -> Unit,
    onVerifyOtp: (String) -> Unit,
    onDismissReauth: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DobiBackground)
            .systemBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
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
                avatarBackground = DobiPrimaryContainer,
                title = "Child",
                description = "I want to track\nmy learning",
                onClick = {
                    if (reauth.visible) onDismissReauth()
                    onChildSelected()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            RoleCard(
                emoji = "👩",
                avatarBackground = DobiPrimaryContainer,
                title = "Parent",
                description = "I want to track my\nchild's learning",
                onClick = onParentSelected
            )

            // Inline re-auth dropdown (only for returning parents)
            AnimatedVisibility(
                visible = reauth.visible,
                enter = fadeIn(tween(200)) + expandVertically(tween(250)),
                exit = fadeOut(tween(150)) + shrinkVertically(tween(200))
            ) {
                ReauthDropdown(
                    reauth = reauth,
                    onSendOtp = onSendOtp,
                    onVerifyOtp = onVerifyOtp,
                    onDismiss = onDismissReauth
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
            DisclaimerRow()
            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

// ── Re-auth dropdown ──────────────────────────────────────────────────────────

@Composable
private fun ReauthDropdown(
    reauth: RoleSelectionViewModel.ReauthState,
    onSendOtp: () -> Unit,
    onVerifyOtp: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var otp by remember { mutableStateOf("") }
    LaunchedEffect(reauth.error) { if (reauth.error != null) otp = "" }

    Column(modifier = Modifier.padding(top = 12.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Welcome back, ${reauth.name.substringBefore(" ").ifBlank { "there" }}",
                        fontSize = 16.sp,
                        fontWeight = SFProBold,
                        color = DobiTextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Not you?",
                        fontSize = 12.sp,
                        fontWeight = SFProSemiBold,
                        color = DobiTextSecondary,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismiss
                        )
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "We'll send an OTP to verify it's you.",
                    fontSize = 13.sp,
                    color = DobiTextSecondary
                )

                Spacer(Modifier.height(14.dp))

                // Phone tile (read-only)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DobiPrimaryContainer)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📱", fontSize = 18.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = reauth.displayPhone,
                        fontSize = 15.sp,
                        fontWeight = SFProSemiBold,
                        color = DobiPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(14.dp))

                if (reauth.verificationId == null) {
                    // Step 1: Send OTP
                    Button(
                        onClick = onSendOtp,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        enabled = !reauth.isSending,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DobiPrimary,
                            disabledContainerColor = DobiPrimaryLight.copy(alpha = 0.4f)
                        )
                    ) {
                        if (reauth.isSending) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        } else {
                            Text("Send OTP", fontSize = 15.sp, fontWeight = SFProBold, color = Color.White)
                        }
                    }
                } else {
                    // Step 2: OTP input
                    Text(
                        text = "Enter 6-digit code",
                        fontSize = 13.sp,
                        fontWeight = SFProMedium,
                        color = DobiTextPrimary
                    )
                    Spacer(Modifier.height(8.dp))
                    BasicTextField(
                        value = otp,
                        onValueChange = { new ->
                            if (!reauth.isVerifying && new.length <= 6) {
                                val digits = new.filter { it.isDigit() }
                                otp = digits
                                if (digits.length == 6) onVerifyOtp(digits)
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        decorationBox = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                repeat(6) { index ->
                                    val char = otp.getOrNull(index)
                                    val isCurrent = otp.length == index
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(52.dp)
                                            .border(
                                                width = if (isCurrent) 2.dp else 1.5.dp,
                                                color = if (isCurrent || char != null) DobiPrimary else DobiBorder,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .background(
                                                color = if (char != null) DobiPrimaryContainer else Color.White,
                                                shape = RoundedCornerShape(12.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = char?.toString() ?: if (isCurrent) "·" else "",
                                            fontSize = if (char != null) 18.sp else 20.sp,
                                            fontWeight = SFProBold,
                                            color = if (char != null) DobiPrimary else DobiTextTertiary
                                        )
                                    }
                                }
                            }
                        }
                    )
                    if (reauth.isVerifying) {
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(color = DobiPrimary, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Verifying…", fontSize = 13.sp, color = DobiTextSecondary)
                        }
                    }
                }

                if (reauth.error != null) {
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DobiErrorContainer)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text("⚠️  ${reauth.error}", fontSize = 12.sp, fontWeight = SFProMedium, color = DobiError)
                    }
                }
            }
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
                fontWeight = SFProBold,
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
                        fontWeight = SFProBold,
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

@Composable
private fun DisclaimerRow() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(text = "🔒", fontSize = 13.sp)
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = "Parent login required. Children are added by parents.",
            style = MaterialTheme.typography.bodySmall,
            color = DobiTextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun RoleSelectionPreview() {
    DobiTheme {
        RoleSelectionContent(
            reauth = RoleSelectionViewModel.ReauthState(),
            onParentSelected = {},
            onChildSelected = {},
            onSendOtp = {},
            onVerifyOtp = {},
            onDismissReauth = {}
        )
    }
}
