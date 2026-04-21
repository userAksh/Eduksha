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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dobi.app.core.ui.theme.DobiPrimary
import com.dobi.app.core.ui.theme.SFProBold
import com.dobi.app.core.ui.theme.SFProSemiBold
import com.dobi.app.core.ui.theme.SFProMedium
import com.dobi.app.core.ui.theme.SFProRegular
import com.dobi.app.core.ui.theme.SFProLight
import com.dobi.app.core.ui.theme.DobiPrimaryContainer
import com.dobi.app.core.ui.theme.DobiPrimaryLight
import com.dobi.app.core.ui.theme.DobiTextPrimary
import com.dobi.app.core.ui.theme.DobiTextSecondary

private val ErrorRed = Color(0xFFDC2626)

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

    Box(modifier = Modifier.fillMaxSize()) {
        // Coloured top band
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(DobiPrimary)
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .systemBarsPadding()
                    .padding(top = 32.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "📱", fontSize = 28.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Verify your number",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = SFProBold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Enter the 6-digit code sent to +91 $phone",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding(),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = Color(0xFFF5F7FF),
                shadowElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 36.dp)
                ) {
                    Text(
                        text = "Enter OTP",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = SFProSemiBold),
                        color = DobiTextPrimary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    BasicTextField(
                        value = otp,
                        onValueChange = { if (it.length <= 6) otp = it.filter { c -> c.isDigit() } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        decorationBox = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                repeat(6) { index ->
                                    val char = otp.getOrNull(index)
                                    val isCurrent = otp.length == index
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
                                                    Modifier.border(1.5.dp, Color(0xFFCBD5E1), RoundedCornerShape(14.dp))
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
                                            color = if (char != null) DobiPrimary else Color(0xFFCBD5E1),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    )

                    if (error != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFFEE2E2))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "⚠️  $error",
                                style = MaterialTheme.typography.bodySmall,
                                color = ErrorRed
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = { onVerify(otp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = otp.length == 6 && !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DobiPrimary,
                            disabledContainerColor = DobiPrimaryLight.copy(alpha = 0.4f)
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(22.dp)
                            )
                        } else {
                            Text(
                                "Verify & Continue",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = SFProBold),
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Didn't receive the code? Check your messages or try again.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DobiTextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
