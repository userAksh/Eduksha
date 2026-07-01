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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dobi.app.R
import com.dobi.app.core.navigation.Screen
import com.dobi.app.core.ui.theme.DobiPrimary
import com.dobi.app.core.ui.theme.DobiPrimaryDark
import com.dobi.app.core.ui.theme.DobiPrimaryLight
import com.dobi.app.core.ui.theme.DobiSuccess
import com.dobi.app.core.ui.theme.DobiSuccessContainer
import com.dobi.app.core.ui.theme.DobiTextPrimary
import com.dobi.app.core.ui.theme.DobiTextSecondary
import com.dobi.app.core.ui.theme.DobiTextTertiary

// ── Screen-local palette (matches the source design mockup exactly) ──
private val HeroBackground = Color(0xFFF2F6FD)
private val InputBorder = Color(0xFFE3E8F0)
private val DividerLine = Color(0xFFE3E8F0)
private val ErrorRed = Color(0xFFDC2626)

@Composable
fun PhoneAuthScreen(
    onNavigateToOtp: (Screen.OtpVerification) -> Unit,
    onNavigateToParentHome: () -> Unit,
    startInSignUpMode: Boolean = false,
    onStartInSignUpModeConsumed: () -> Unit = {},
    viewModel: PhoneAuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalContext.current as Activity
    var isLogin by remember { mutableStateOf(false) }

    // Switch to Sign Up if OTP screen detected an unregistered number (post-OTP safety net)
    LaunchedEffect(startInSignUpMode) {
        if (startInSignUpMode) {
            isLogin = false
            onStartInSignUpModeConsumed()
        }
    }

    // Switch to Sign Up immediately if Firestore pre-check found the number isn't registered
    LaunchedEffect(uiState) {
        if (uiState is PhoneAuthViewModel.UiState.NotRegistered) {
            isLogin = false
        }
    }

    LaunchedEffect(Unit) {
        viewModel.navEvent.collect { event ->
            when (event) {
                is PhoneAuthViewModel.NavEvent.ToOtpVerification ->
                    onNavigateToOtp(
                        Screen.OtpVerification(event.verificationId, event.name, event.phone)
                    )
                PhoneAuthViewModel.NavEvent.ToParentHome -> onNavigateToParentHome()
            }
        }
    }

    PhoneAuthContent(
        uiState = uiState,
        isLogin = isLogin,
        onToggleMode = { isLogin = !isLogin },
        onSendOtp = { name, phone -> viewModel.sendOtp(name, phone, activity) }
    )
}

@Composable
private fun PhoneAuthContent(
    uiState: PhoneAuthViewModel.UiState,
    isLogin: Boolean,
    onToggleMode: () -> Unit,
    onSendOtp: (name: String, phone: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    val isLoading = uiState is PhoneAuthViewModel.UiState.Loading
    val error = (uiState as? PhoneAuthViewModel.UiState.Error)?.message
        ?: (uiState as? PhoneAuthViewModel.UiState.NotRegistered)?.message

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .imePadding()
        ) {
            HeroSection()
            FormCard(
                name = name,
                onNameChange = { name = it },
                phone = phone,
                onPhoneChange = { phone = it.filter { c -> c.isDigit() }.take(10) },
                isLoading = isLoading,
                error = error,
                isLogin = isLogin,
                onToggleMode = onToggleMode,
                onSendOtp = { onSendOtp(if (isLogin) "" else name.trim(), phone.trim()) }
            )
            FooterDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 18.dp)
            )
            Spacer(modifier = Modifier.navigationBarsPadding())
        }

        // Sticky top band — paints the status-bar strip with the hero colour so
        // scrolling content never sits under the system status bar icons.
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(HeroBackground)
        )
    }
}

// ── Top hero: brand row + headline + subtext on the left, illustration on the right ──
// Mirrors the source mockup: hero region occupies ~38% of screen height; the
// illustration fills the right ~52% of width and runs from just under the status
// bar all the way down to the white card. The text column sits on the left.

@Composable
private fun HeroSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(HeroBackground),
    ) {
        // Left — text column (45% of width)
        Column(
            modifier = Modifier
                .fillMaxWidth(0.45f)
                .padding(start = 22.dp, top = 16.dp, end = 4.dp, bottom = 16.dp)
        ) {
            Text(
                text = "Partner in your\nchild's learning",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 26.sp,
                lineHeight = 32.sp,
                color = DobiTextPrimary
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Get insights, track progress and stay one step ahead.",
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = DobiTextSecondary
            )
        }
        // Right — illustration (55% of width). FillWidth scales to fill the
        // column width and auto-computes height from the image aspect ratio,
        // so the Box (and background) end exactly where the image ends.
        Image(
            painter = painterResource(id = R.drawable.parent_login_illustration),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth(0.55f)
                .align(Alignment.TopEnd)
                .padding(top = 16.dp)
        )
    }
}

// ── White form card ──

@Composable
private fun FormCard(
    name: String,
    onNameChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    isLoading: Boolean,
    error: String?,
    isLogin: Boolean,
    onToggleMode: () -> Unit,
    onSendOtp: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = Color.White,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 28.dp, bottom = 20.dp)
        ) {
            // ── Sign Up / Login toggle ────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(HeroBackground)
                    .padding(4.dp),
            ) {
                listOf("Sign Up" to false, "Login" to true).forEach { (label, loginMode) ->
                    val selected = isLogin == loginMode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) DobiPrimary else Color.Transparent)
                            .clickable(enabled = !selected) { onToggleMode() }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            color = if (selected) Color.White else DobiTextSecondary,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 14.sp,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (!isLogin) {
                FieldLabel(icon = Icons.Default.Person, label = "Your Name")
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    placeholder = {
                        Text(
                            "e.g. Rahul Sharma",
                            color = DobiTextTertiary,
                            fontWeight = FontWeight.Normal,
                            fontSize = 15.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    colors = textFieldColors()
                )
                Spacer(modifier = Modifier.height(22.dp))
            }

            FieldLabel(icon = Icons.Default.Phone, label = "Phone Number")
            Spacer(modifier = Modifier.height(10.dp))
            PhoneField(value = phone, onValueChange = onPhoneChange)

            if (error != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = error,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = ErrorRed
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            SendOtpButton(
                enabled = (isLogin || name.isNotBlank()) && phone.length == 10 && !isLoading,
                isLoading = isLoading,
                onClick = { focusManager.clearFocus(); onSendOtp() }
            )

            Spacer(modifier = Modifier.height(20.dp))

            TrustStrip()
        }
    }
}

@Composable
private fun FieldLabel(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = DobiPrimary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = DobiTextPrimary
        )
    }
}

@Composable
private fun PhoneField(
    value: String,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(width = 1.dp, color = InputBorder, shape = RoundedCornerShape(14.dp))
            .background(Color.White),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Country prefix
        Row(
            modifier = Modifier.padding(start = 18.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "+91",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = DobiPrimary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "▾",
                fontSize = 14.sp,
                color = DobiPrimary
            )
        }
        // Divider
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(28.dp)
                .background(DividerLine)
        )
        // Number input (unstyled OutlinedTextField with no border, sits inside the parent row)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    "10-digit mobile number",
                    color = DobiTextTertiary,
                    fontWeight = FontWeight.Normal,
                    fontSize = 15.sp
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(0.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                disabledBorderColor = Color.Transparent,
                focusedTextColor = DobiTextPrimary,
                unfocusedTextColor = DobiTextPrimary,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                cursorColor = DobiPrimary
            )
        )
    }
}

@Composable
private fun SendOtpButton(
    enabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    val gradient = Brush.horizontalGradient(
        colors = if (enabled) listOf(DobiPrimary, DobiPrimaryDark)
        else listOf(Color(0xFF9BC2FF), Color(0xFF9BC2FF))
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .then(
                if (enabled) Modifier.shadow(
                    elevation = 10.dp,
                    shape = RoundedCornerShape(20.dp),
                    spotColor = DobiPrimary.copy(alpha = 0.35f)
                ) else Modifier
            )
            .clip(RoundedCornerShape(20.dp))
            .background(gradient)
            .then(if (enabled && !isLoading) Modifier.clickable { onClick() } else Modifier),
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
                text = "Send OTP",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = Color.White
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 14.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun TrustStrip() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(DobiSuccessContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = DobiSuccess,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = "Your number is safe with us.",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = DobiTextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "An OTP will be sent to verify your number.",
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                color = DobiTextSecondary
            )
        }
    }
}

@Composable
private fun FooterDivider(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(1.dp)
                .background(DividerLine)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = "Secure  •  Private  •  Trusted",
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = DobiTextTertiary,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.width(14.dp))
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(1.dp)
                .background(DividerLine)
        )
    }
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = DobiTextPrimary,
    unfocusedTextColor = DobiTextPrimary,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    focusedBorderColor = DobiPrimary,
    unfocusedBorderColor = InputBorder,
    cursorColor = DobiPrimary
)
