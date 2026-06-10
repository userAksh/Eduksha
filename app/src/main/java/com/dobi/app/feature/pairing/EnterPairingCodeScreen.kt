package com.dobi.app.feature.pairing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
import com.dobi.app.core.ui.theme.SFProBold
import com.dobi.app.core.ui.theme.SFProMedium

@Composable
fun EnterPairingCodeScreen(
    onNavigateBack: () -> Unit,
    onNavigateToChildHome: (String) -> Unit,
    viewModel: EnterPairingCodeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navToChildHome.collect { childId ->
            onNavigateToChildHome(childId)
        }
    }

    Content(
        uiState = uiState,
        onBack = onNavigateBack,
        onVerify = viewModel::verifyCode,
        onClearError = viewModel::clearError
    )
}

@Composable
private fun Content(
    uiState: EnterPairingCodeViewModel.UiState,
    onBack: () -> Unit,
    onVerify: (String) -> Unit,
    onClearError: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    var code by remember { mutableStateOf("") }
    val isVerifying = uiState is EnterPairingCodeViewModel.UiState.Verifying
    val error = (uiState as? EnterPairingCodeViewModel.UiState.Error)?.message

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DobiBackground)
            .systemBarsPadding()
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = DobiTextPrimary
                )
            }
            Text(
                text = "Enter Pairing Code",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = SFProBold),
                color = DobiTextPrimary,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.size(48.dp))
        }

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(DobiPrimaryContainer)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                Text("🔗", fontSize = 44.sp)
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Pair with a parent",
                fontSize = 22.sp,
                fontWeight = SFProBold,
                color = DobiTextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Ask your parent for a 6-digit pairing code from their Dobi app.",
                fontSize = 14.sp,
                color = DobiTextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(32.dp))

            BasicTextField(
                value = code,
                onValueChange = {
                    if (it.length <= 6) code = it.filter { c -> c.isDigit() }
                    onClearError()
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                decorationBox = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        repeat(6) { index ->
                            val char = code.getOrNull(index)
                            val isCurrent = code.length == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .then(
                                        if (isCurrent)
                                            Modifier.border(2.dp, DobiPrimary, RoundedCornerShape(14.dp))
                                        else if (char != null)
                                            Modifier.border(2.dp, DobiPrimary.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                        else
                                            Modifier.border(1.5.dp, DobiBorder, RoundedCornerShape(14.dp))
                                    )
                                    .background(
                                        color = if (char != null) DobiPrimaryContainer else Color.White,
                                        shape = RoundedCornerShape(14.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = char?.toString() ?: if (isCurrent) "·" else "",
                                    fontSize = if (char != null) 22.sp else 28.sp,
                                    fontWeight = SFProBold,
                                    color = if (char != null) DobiPrimary else DobiTextTertiary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            )

            if (error != null) {
                Spacer(Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DobiErrorContainer)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "⚠️  $error",
                        fontSize = 13.sp,
                        fontWeight = SFProMedium,
                        color = DobiError
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = { focusManager.clearFocus(); onVerify(code) },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = code.length == 6 && !isVerifying,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DobiPrimary,
                    disabledContainerColor = DobiPrimaryLight.copy(alpha = 0.4f)
                )
            ) {
                if (isVerifying) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Text(
                        "Verify & Continue",
                        fontSize = 16.sp,
                        fontWeight = SFProBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
