package com.dobi.app.feature.auth

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dobi.app.core.ui.theme.DobiBackground
import com.dobi.app.core.ui.theme.DobiPrimary
import com.dobi.app.core.ui.theme.DobiPrimaryContainer
import com.dobi.app.core.ui.theme.DobiTextPrimary
import com.dobi.app.core.ui.theme.DobiTextSecondary

@Composable
fun OtpVerificationScreen(
    onNavigateToAddChild: () -> Unit,
    onNavigateToParentHome: () -> Unit,
    viewModel: OtpVerificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navEvent.collect { event ->
            when (event) {
                OtpVerificationViewModel.NavEvent.ToAddChild -> onNavigateToAddChild()
                OtpVerificationViewModel.NavEvent.ToParentHome -> onNavigateToParentHome()
            }
        }
    }

    OtpContent(
        phone = viewModel.phone,
        uiState = uiState,
        onVerify = viewModel::verifyOtp
    )
}

@Composable
private fun OtpContent(
    phone: String,
    uiState: OtpVerificationViewModel.UiState,
    onVerify: (String) -> Unit
) {
    var otp by remember { mutableStateOf("") }

    val isLoading = uiState is OtpVerificationViewModel.UiState.Loading
    val error = (uiState as? OtpVerificationViewModel.UiState.Error)?.message

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DobiBackground)
            .systemBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Verify your number",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = DobiTextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Enter the 6-digit code sent to +91 $phone",
            style = MaterialTheme.typography.bodyMedium,
            color = DobiTextSecondary
        )

        Spacer(modifier = Modifier.height(48.dp))

        BasicTextField(
            value = otp,
            onValueChange = { if (it.length <= 6) otp = it.filter { c -> c.isDigit() } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            decorationBox = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    repeat(6) { index ->
                        val char = otp.getOrNull(index)
                        val isFocused = otp.length == index
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .border(
                                    width = if (isFocused) 2.dp else 1.dp,
                                    color = if (isFocused) DobiPrimary else Color(0xFFE2E8F0),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .background(
                                    if (char != null) DobiPrimaryContainer else Color.White,
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = char?.toString() ?: "",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = DobiTextPrimary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        )

        if (error != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = error, style = MaterialTheme.typography.bodySmall, color = Color(0xFFEF4444))
        }

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = { onVerify(otp) },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            enabled = otp.length == 6 && !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = DobiPrimary)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.height(20.dp))
            } else {
                Text("Verify OTP", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
            }
        }
    }
}
