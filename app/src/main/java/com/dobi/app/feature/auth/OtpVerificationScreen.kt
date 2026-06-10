package com.dobi.app.feature.auth

import android.app.Activity
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dobi.app.R
import com.dobi.app.core.ui.theme.DobiPrimary
import com.dobi.app.core.ui.theme.DobiPrimaryLight
import com.dobi.app.core.ui.theme.DobiPrimarySoft
import com.dobi.app.core.ui.theme.DobiSuccess
import com.dobi.app.core.ui.theme.DobiTextPrimary
import com.dobi.app.core.ui.theme.DobiTextSecondary
import com.dobi.app.core.ui.theme.DobiTextTertiary
import com.dobi.app.core.ui.theme.SFProBold
import com.dobi.app.core.ui.theme.SFProMedium
import com.dobi.app.core.ui.theme.SFProSemiBold
import kotlinx.coroutines.delay

private val OtpBoxEmpty = Color(0xFFDDE3F0)
private val ErrorRed = Color(0xFFDC2626)

@Composable
fun OtpVerificationScreen(
    onNavigateBack: () -> Unit,
    onNavigateToParentHome: () -> Unit,
    viewModel: OtpVerificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalContext.current as Activity

    LaunchedEffect(Unit) {
        viewModel.navEvent.collect { event ->
            when (event) {
                OtpVerificationViewModel.NavEvent.ToParentHome -> onNavigateToParentHome()
            }
        }
    }

    OtpContent(
        phone = viewModel.phone,
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onVerify = viewModel::verifyOtp,
        onResend = { viewModel.resendOtp(activity) },
        onReferralCodeEntered = viewModel::saveReferralCode
    )
}

@Composable
private fun OtpContent(
    phone: String,
    uiState: OtpVerificationViewModel.UiState,
    onNavigateBack: () -> Unit,
    onVerify: (String) -> Unit,
    onResend: () -> Unit,
    onReferralCodeEntered: (String) -> Unit = {},
) {
    val focusManager = LocalFocusManager.current
    var otp by remember { mutableStateOf("") }
    var resendTick by remember { mutableStateOf(0) }
    var secondsRemaining by remember { mutableStateOf(30) }

    LaunchedEffect(resendTick) {
        secondsRemaining = 30
        while (secondsRemaining > 0) {
            delay(1000L)
            secondsRemaining -= 1
        }
    }

    val isLoading = uiState is OtpVerificationViewModel.UiState.Loading
    val error = (uiState as? OtpVerificationViewModel.UiState.Error)?.message
    val canResend = secondsRemaining == 0 && !isLoading

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F9FC))
            .statusBarsPadding()
    ) {
        // ── Fixed back button header — never scrolls ──────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, top = 16.dp, bottom = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .shadow(4.dp, CircleShape, spotColor = Color(0x20000000))
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable { onNavigateBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = DobiTextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // ── Scrollable content below the fixed header ─────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
        ) {

        // ── Hero: title left, illustration right ─────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .padding(start = 24.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(0.55f),
                verticalArrangement = Arrangement.Top
            ) {
                Text(
                    text = "Verify your\nnumber",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 36.sp,
                    color = DobiTextPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Enter the 6-digit code sent to",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = DobiTextSecondary,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "+91 $phone",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DobiPrimary
                )
            }

            Image(
                painter = painterResource(id = R.drawable.otp_illustration),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                alignment = Alignment.TopEnd,
                modifier = Modifier
                    .weight(0.45f)
                    .fillMaxSize()
            )
        }

        // ── OTP card ─────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(24.dp),
                    spotColor = DobiPrimary.copy(alpha = 0.12f),
                    ambientColor = Color(0x18000000)
                )
                .background(Color.White, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header: shield icon + title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DobiPrimarySoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = DobiPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Enter OTP",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = DobiTextPrimary
                        )
                        Text(
                            text = "6-digit code sent via SMS",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            color = DobiTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // 6-digit OTP boxes
                BasicTextField(
                    value = otp,
                    onValueChange = { new -> otp = new.filter { c -> c.isDigit() }.take(6) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    decorationBox = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            repeat(6) { index ->
                                val char = otp.getOrNull(index)
                                val isActive = otp.length == index
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(54.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (char != null) DobiPrimarySoft else Color.White
                                        )
                                        .then(
                                            when {
                                                isActive -> Modifier.border(2.dp, DobiPrimary, RoundedCornerShape(12.dp))
                                                char != null -> Modifier.border(1.5.dp, DobiPrimary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                                else -> Modifier.border(1.5.dp, OtpBoxEmpty, RoundedCornerShape(12.dp))
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = when {
                                            char != null -> char.toString()
                                            isActive -> "|"
                                            else -> ""
                                        },
                                        fontSize = if (char != null) 22.sp else 20.sp,
                                        fontWeight = SFProBold,
                                        color = if (char != null) DobiPrimary else Color(0xFFBBC5D6),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                )

                if (error != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFEE2E2))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "⚠️  $error",
                            fontSize = 13.sp,
                            color = ErrorRed
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Hint box
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(DobiPrimarySoft)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = DobiPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Didn't receive the code? Check your SMS spam folder or request a new one.",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = DobiPrimary,
                        lineHeight = 18.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Resend timer / button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (canResend) Modifier.clickable {
                                onResend()
                                otp = ""
                                resendTick += 1
                            } else Modifier
                        )
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = DobiPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (canResend) "Resend OTP"
                               else "Resend OTP in 00:${"%02d".format(secondsRemaining)}",
                        fontSize = 14.sp,
                        fontWeight = if (canResend) FontWeight.Bold else FontWeight.Medium,
                        color = DobiPrimary
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Verify & Continue button
                val buttonEnabled = otp.length == 6 && !isLoading
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (buttonEnabled) DobiPrimary
                            else DobiPrimaryLight.copy(alpha = 0.45f)
                        )
                        .then(
                            if (buttonEnabled) Modifier.clickable { focusManager.clearFocus(); onVerify(otp) } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            text = "Verify & Continue",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 14.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.25f)),
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
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Referral code (optional) ──────────────────────────────────────────
        ReferralCodeField(onCodeEntered = onReferralCodeEntered)

        Spacer(modifier = Modifier.height(16.dp))

        // ── Trust footer ──────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = DobiSuccess,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Your number is safe with us",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = DobiTextPrimary
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(Color(0xFFD1D9E6))
                )
                Text(
                    text = "  Secure  •  Private  •  Trusted  ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = DobiTextTertiary,
                    letterSpacing = 0.3.sp
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(Color(0xFFD1D9E6))
                )
            }
        }
        } // end scrollable Column
    } // end outer Column
}

// ── Referral code field ────────────────────────────────────────────────────────

@Composable
private fun ReferralCodeField(onCodeEntered: (String) -> Unit) {
    val focusManager = LocalFocusManager.current
    var expanded by remember { mutableStateOf(false) }
    var code by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (saved) "✅ Referral code saved" else "Have a referral code?",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (saved) DobiSuccess else DobiTextSecondary
            )
            if (!saved) {
                Spacer(Modifier.width(4.dp))
                Text(
                    text = if (expanded) "▲" else "▼",
                    fontSize = 11.sp,
                    color = DobiTextSecondary
                )
            }
        }

        if (expanded && !saved) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = code,
                    onValueChange = { code = it.trim().lowercase() },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = DobiTextPrimary,
                        letterSpacing = 1.sp
                    ),
                    decorationBox = { inner ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, if (code.isNotBlank()) DobiPrimary else Color(0xFFDDE3F0), RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            if (code.isEmpty()) {
                                Text("Enter referral code", fontSize = 14.sp, color = DobiTextSecondary)
                            }
                            inner()
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (code.length >= 6) DobiPrimary else DobiPrimarySoft)
                        .then(if (code.length >= 6) Modifier.clickable {
                            focusManager.clearFocus()
                            onCodeEntered(code)
                            saved = true
                            expanded = false
                        } else Modifier)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Apply",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (code.length >= 6) Color.White else DobiTextSecondary
                    )
                }
            }
        }
    }
}
