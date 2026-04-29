package com.dobi.app.feature.tuitionproblems

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dobi.app.core.ui.theme.DobiBackground
import com.dobi.app.core.ui.theme.DobiBorder
import com.dobi.app.core.ui.theme.DobiPrimaryLight
import com.dobi.app.core.ui.theme.DobiTextPrimary
import com.dobi.app.core.ui.theme.DobiTextSecondary
import com.dobi.app.core.ui.theme.SFProBold
import com.dobi.app.core.ui.theme.SFProSemiBold
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TuitionProblemsScreen(
    onNavigateBack: () -> Unit,
    viewModel: TuitionProblemsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val accent = Color(0xFF7C3AED) // tuition purple

    LaunchedEffect(uiState.sent) {
        if (uiState.sent) {
            delay(1400)
            onNavigateBack()
        }
    }

    Scaffold(
        containerColor = DobiBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Tuition Problems",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = SFProBold,
                        color = DobiTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = DobiTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DobiBackground)
            )
        }
    ) { innerPadding ->
        if (uiState.sent) {
            SentState(modifier = Modifier.fillMaxSize().padding(innerPadding), accent = accent)
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Friendly header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = 0.10f))
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🤔", fontSize = 28.sp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "What's bothering you?",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = SFProBold,
                        color = DobiTextPrimary
                    )
                    Text(
                        "Tap whatever feels true. Your parents will see this.",
                        fontSize = 12.sp,
                        color = DobiTextSecondary
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            TuitionProblems.OPTIONS.forEach { option ->
                ProblemRow(
                    label = option,
                    selected = option in uiState.selected,
                    accent = accent,
                    onToggle = { viewModel.onToggle(option) }
                )
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "Anything else? (optional)",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = SFProSemiBold,
                color = DobiTextPrimary
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "Tell your parents in your own words if your problem isn't above.",
                fontSize = 12.sp,
                color = DobiTextSecondary
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.otherText,
                onValueChange = viewModel::onOtherTextChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp),
                placeholder = {
                    Text(
                        "Write what's on your mind…",
                        color = DobiTextSecondary
                    )
                },
                singleLine = false,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accent,
                    unfocusedBorderColor = DobiBorder,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedTextColor = DobiTextPrimary,
                    unfocusedTextColor = DobiTextPrimary,
                    cursorColor = accent
                )
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${uiState.otherText.length}/280",
                fontSize = 11.sp,
                color = DobiTextSecondary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = viewModel::onSend,
                enabled = uiState.canSend,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    disabledContainerColor = DobiPrimaryLight.copy(alpha = 0.4f)
                )
            ) {
                if (uiState.isSending) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Text(
                        "Tell your parents",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = SFProBold,
                        color = Color.White
                    )
                }
            }

            if (!uiState.canSend && !uiState.isSending) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Pick at least one — or write your own — to continue.",
                    fontSize = 12.sp,
                    color = DobiTextSecondary
                )
            }

            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun ProblemRow(
    label: String,
    selected: Boolean,
    accent: Color,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) accent.copy(alpha = 0.08f) else Color.White)
            .border(
                width = 1.5.dp,
                color = if (selected) accent else DobiBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggle
            )
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(if (selected) accent else Color.White)
                .border(
                    width = 1.5.dp,
                    color = if (selected) accent else DobiBorder,
                    shape = RoundedCornerShape(7.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) SFProSemiBold else SFProSemiBold,
            color = DobiTextPrimary
        )
    }
}

@Composable
private fun SentState(modifier: Modifier, accent: Color) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text("🎉", fontSize = 48.sp)
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "Sent to your parents",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = SFProBold,
            color = DobiTextPrimary
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "They'll see this in their app and can talk to your tutor or you.",
            style = MaterialTheme.typography.bodyMedium,
            color = DobiTextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
