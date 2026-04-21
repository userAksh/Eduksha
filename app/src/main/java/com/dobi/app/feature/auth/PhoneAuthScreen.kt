package com.dobi.app.feature.auth

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dobi.app.core.navigation.Screen
import com.dobi.app.core.ui.theme.DobiBackground
import com.dobi.app.core.ui.theme.DobiPrimary
import com.dobi.app.core.ui.theme.DobiTextPrimary
import com.dobi.app.core.ui.theme.DobiTextSecondary

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
            text = "Welcome to Dobi 👋",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = DobiTextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Create your parent account to get started",
            style = MaterialTheme.typography.bodyMedium,
            color = DobiTextSecondary
        )

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "Your Name",
            style = MaterialTheme.typography.labelLarge,
            color = DobiTextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = { Text("e.g. Rahul Sharma", color = DobiTextSecondary) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DobiPrimary,
                unfocusedBorderColor = Color(0xFFE2E8F0)
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Phone Number",
            style = MaterialTheme.typography.labelLarge,
            color = DobiTextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = phone,
            onValueChange = { if (it.length <= 10) phone = it.filter { c -> c.isDigit() } },
            placeholder = { Text("10-digit mobile number", color = DobiTextSecondary) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            prefix = { Text("+91  ", color = DobiTextPrimary, fontWeight = FontWeight.Medium) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DobiPrimary,
                unfocusedBorderColor = Color(0xFFE2E8F0)
            )
        )

        if (error != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = error, style = MaterialTheme.typography.bodySmall, color = Color(0xFFEF4444))
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { onSendOtp(name.trim(), phone.trim()) },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            enabled = name.isNotBlank() && phone.length == 10 && !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = DobiPrimary)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.height(20.dp))
            } else {
                Text("Send OTP", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "An OTP will be sent to verify your number",
            style = MaterialTheme.typography.bodySmall,
            color = DobiTextSecondary,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
