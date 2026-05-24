package com.dobi.app.feature.auth

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
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
    viewModel: PhoneAuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalContext.current as Activity

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
        onSendOtp = { name, phone -> viewModel.sendOtp(name, phone, activity) }
    )
}

@Composable
private fun PhoneAuthContent(
    uiState: PhoneAuthViewModel.UiState,
    onSendOtp: (name: String, phone: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    val isLoading = uiState is PhoneAuthViewModel.UiState.Loading
    val error = (uiState as? PhoneAuthViewModel.UiState.Error)?.message

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // imePadding sits OUTSIDE the scroll so the whole screen (hero image
        // included) is pushed up by the keyboard; the focused TextField is then
        // brought into view by OutlinedTextField's built-in scroll-into-view.
        // statusBarsPadding keeps scrolling content from sliding under the
        // system status bar — the sticky top band below paints that strip.
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
                onSendOtp = { onSendOtp(name.trim(), phone.trim()) }
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(HeroBackground),
        verticalAlignment = Alignment.Top
    ) {
        // Left — text column (45% of width)
        Column(
            modifier = Modifier
                .weight(0.45f)
                .fillMaxHeight()
                .padding(start = 22.dp, top = 16.dp, end = 4.dp)
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
        // Right — illustration (55% of width). Top inset matches the left column.
        Image(
            painter = painterResource(id = R.drawable.parent_login_illustration),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            alignment = Alignment.TopEnd,
            modifier = Modifier
                .weight(0.55f)
                .fillMaxHeight()
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
    onSendOtp: () -> Unit
) {
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
                enabled = name.isNotBlank() && phone.length == 10 && !isLoading,
                isLoading = isLoading,
                onClick = onSendOtp
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
