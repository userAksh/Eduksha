package com.dobi.app.feature.pin

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
import androidx.compose.material3.CircularProgressIndicator
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
import com.dobi.app.core.ui.theme.DobiTextPrimary
import com.dobi.app.core.ui.theme.DobiTextSecondary
import com.dobi.app.core.ui.theme.DobiTextTertiary
import com.dobi.app.core.ui.theme.SFProBold
import com.dobi.app.core.ui.theme.SFProMedium

@Composable
fun SetPinScreen(
    onDone: () -> Unit,
    viewModel: SetPinViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var pin by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.navDone.collect { onDone() }
    }

    // Clear pin when moving between entering/confirming
    LaunchedEffect(uiState) {
        if (uiState is SetPinViewModel.UiState.Confirming ||
            uiState is SetPinViewModel.UiState.Error) {
            pin = ""
        }
    }

    val title = when (uiState) {
        is SetPinViewModel.UiState.Entering -> "Create your PIN"
        is SetPinViewModel.UiState.Confirming -> "Confirm your PIN"
        is SetPinViewModel.UiState.Saving -> "Saving…"
        is SetPinViewModel.UiState.Error -> "Create your PIN"
    }
    val subtitle = when (uiState) {
        is SetPinViewModel.UiState.Entering -> "Set a 4-digit PIN to protect parent access."
        is SetPinViewModel.UiState.Confirming -> "Enter the same 4 digits again to confirm."
        is SetPinViewModel.UiState.Saving -> "Securing your profile…"
        is SetPinViewModel.UiState.Error -> "Set a 4-digit PIN to protect parent access."
    }
    val isSaving = uiState is SetPinViewModel.UiState.Saving
    val error = (uiState as? SetPinViewModel.UiState.Error)?.message

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DobiBackground)
            .systemBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(48.dp))

        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(DobiPrimaryContainer)
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center
        ) {
            Text("🔒", fontSize = 44.sp)
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = title,
            fontSize = 22.sp,
            fontWeight = SFProBold,
            color = DobiTextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = subtitle,
            fontSize = 14.sp,
            color = DobiTextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(32.dp))

        BasicTextField(
            value = pin,
            onValueChange = { new ->
                if (new.length <= 4 && !isSaving) {
                    val digits = new.filter { it.isDigit() }
                    pin = digits
                    if (digits.length == 4) viewModel.onPinEntered(digits)
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            decorationBox = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    repeat(4) { index ->
                        val char = pin.getOrNull(index)
                        val isCurrent = pin.length == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp)
                                .border(
                                    width = if (isCurrent) 2.dp else 1.5.dp,
                                    color = if (isCurrent || char != null) DobiPrimary else DobiBorder,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .background(
                                    color = if (char != null) DobiPrimaryContainer else Color.White,
                                    shape = RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (char != null) "●" else if (isCurrent) "·" else "",
                                fontSize = if (char != null) 24.sp else 28.sp,
                                fontWeight = SFProBold,
                                color = if (char != null) DobiPrimary else DobiTextTertiary
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

        if (isSaving) {
            Spacer(Modifier.height(20.dp))
            CircularProgressIndicator(
                color = DobiPrimary,
                strokeWidth = 2.dp,
                modifier = Modifier.size(24.dp).align(Alignment.CenterHorizontally)
            )
        }
    }
}
