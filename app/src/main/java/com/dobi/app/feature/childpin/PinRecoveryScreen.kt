package com.dobi.app.feature.childpin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dobi.app.core.ui.theme.DobiBackground
import com.dobi.app.core.ui.theme.DobiPrimary
import com.dobi.app.core.ui.theme.DobiPrimaryContainer
import com.dobi.app.core.ui.theme.DobiPrimarySoft
import com.dobi.app.core.ui.theme.DobiTextPrimary
import com.dobi.app.core.ui.theme.DobiTextSecondary
import com.dobi.app.core.ui.theme.SFProBold
import com.dobi.app.core.ui.theme.SFProRegular
import com.dobi.app.core.ui.theme.SFProSemiBold

@Composable
fun PinRecoveryScreen(
    onBack: () -> Unit,
    viewModel: PinRecoveryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DobiBackground)
            .systemBarsPadding()
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
                text = "PIN Recovery",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = SFProBold),
                color = DobiTextPrimary,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.size(48.dp))
        }

        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = DobiPrimary)
            }
            !uiState.signedIn -> Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(40.dp))
                Text("🔒", fontSize = 48.sp)
                Spacer(Modifier.height(16.dp))
                Text(
                    "Parent sign-in required",
                    fontSize = 18.sp,
                    fontWeight = SFProBold,
                    color = DobiTextPrimary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Sign in as the parent on this device to view child PINs.",
                    fontSize = 14.sp,
                    color = DobiTextSecondary,
                    textAlign = TextAlign.Center
                )
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp)
            ) {
                item {
                    Text(
                        text = "Child PINs",
                        fontSize = 20.sp,
                        fontWeight = SFProBold,
                        color = DobiTextPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Showing PINs for all children on your account.",
                        fontSize = 13.sp,
                        color = DobiTextSecondary
                    )
                    Spacer(Modifier.height(16.dp))
                }
                items(uiState.children) { child ->
                    val avatarEmoji = when (child.gender) {
                        "girl" -> "👧"
                        "other" -> "🧒"
                        else -> "👦"
                    }
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(DobiPrimarySoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(avatarEmoji, fontSize = 26.sp)
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = child.name,
                                    fontSize = 16.sp,
                                    fontWeight = SFProBold,
                                    color = DobiTextPrimary
                                )
                                Text(
                                    text = "Class ${child.grade} · ${child.board}",
                                    fontSize = 12.sp,
                                    fontWeight = SFProRegular,
                                    color = DobiTextSecondary
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(DobiPrimaryContainer)
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = child.pin.ifBlank { "—" },
                                    fontSize = 18.sp,
                                    fontWeight = SFProBold,
                                    color = DobiPrimary,
                                    letterSpacing = 3.sp
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}
