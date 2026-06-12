package com.dobi.app.feature.paywall

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CurrencyRupee
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dobi.app.R
import com.razorpay.Checkout
import org.json.JSONObject

private val Purple        = Color(0xFF6C3EE8)
private val PurpleLight   = Color(0xFF9B6FF5)
private val PurpleDark    = Color(0xFF4A1DB5)
private val Pink          = Color(0xFFE8367A)
private val TextPrimary   = Color(0xFF1A0A3B)
private val TextSecondary = Color(0xFF7B6FA0)

@Composable
fun PaywallScreen(
    onNavigateToParentHome: () -> Unit,
    viewModel: PaywallViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.navigateToHome) {
        if (uiState.navigateToHome) {
            viewModel.onNavigationConsumed()
            onNavigateToParentHome()
        }
    }

    LaunchedEffect(uiState.checkoutData) {
        val data = uiState.checkoutData ?: return@LaunchedEffect
        viewModel.onCheckoutConsumed()

        val activity = context.findActivity() ?: return@LaunchedEffect
        val checkout = Checkout()
        checkout.setKeyID(data.keyId)
        val options = JSONObject().apply {
            put("name", "Eduksha")
            put("description", "AI-powered learning for your child")
            put("subscription_id", data.subscriptionId)
            put("prefill", JSONObject().apply {
                put("email", data.userEmail ?: "")
                put("contact", data.userPhone ?: "")
            })
            // Hides contact/email fields so Razorpay skips the contact form
            // and lands directly on the UPI/card selection screen.
            put("hidden", JSONObject().apply {
                put("contact", 1)
                put("email", 1)
            })
            put("theme", JSONObject().apply {
                put("color", "#6C3EE8")
            })
        }
        try {
            checkout.open(activity, options)
        } catch (e: Exception) {
            viewModel.onCheckoutError(e.message ?: "Could not open payment screen")
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.paywall_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        if (uiState.isCheckingSubscription) {
            // Hold the card until we know the user isn't already subscribed,
            // preventing a visible paywall flash before routing to parent home.
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.align(Alignment.Center),
            )
            return@Box
        }

        if (uiState.paymentFailed) {
            PaymentFailedOverlay(
                reason = uiState.paymentFailureReason,
                onRetry = { viewModel.retryPayment() },
                onChangePlan = { viewModel.dismissPaymentFailed() },
            )
            return@Box
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // ── Hero: video plays muted on loop over the background image ─────
            PaywallVideoPlayer(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.32f),
            )

            // ── White card: bottom 53% ─────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.68f)
                    .navigationBarsPadding()
                    .padding(start = 8.dp, end = 8.dp, bottom = 10.dp),
            ) {
                Card(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 14.dp, end = 14.dp, bottom = 16.dp),
            ) {
                PlanTabRow(
                    selected = uiState.selectedTab,
                    onSelect = viewModel::selectTab,
                )

                Spacer(Modifier.height(16.dp))

                when (uiState.selectedTab) {
                    PaywallTab.TRIAL -> TrialCard()
                    else -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            listOf(
                                PaywallPlan.MONTHLY,
                                PaywallPlan.QUARTERLY,
                                PaywallPlan.HALFYEARLY,
                            ).forEach { plan ->
                                PlanCard(
                                    plan = plan,
                                    selected = uiState.selectedPlan == plan,
                                    onSelect = { viewModel.selectPlan(plan) },
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                ReferralCodeSection(
                    code = uiState.referralCode,
                    applied = uiState.referralApplied,
                    error = uiState.referralError,
                    onCodeChanged = viewModel::onReferralCodeChanged,
                    onApply = viewModel::applyReferralCode,
                    onRemove = viewModel::removeReferralCode,
                )

                Spacer(Modifier.height(16.dp))

                if (uiState.error != null) {
                    Text(
                        text = uiState.error!!,
                        color = Color(0xFFEF4444),
                        fontSize = 13.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                    )
                }

                SubscribeButton(
                    plan = if (uiState.selectedTab == PaywallTab.TRIAL) PaywallPlan.TRIAL
                           else uiState.selectedPlan,
                    isLoading = uiState.isLoading,
                    onClick = {
                        if (uiState.selectedTab == PaywallTab.TRIAL) {
                            viewModel.selectPlan(PaywallPlan.TRIAL)
                        }
                        viewModel.startCheckout()
                    },
                )

                Spacer(Modifier.height(12.dp))

                MoneyBackBanner()

                Spacer(Modifier.height(12.dp))

                SecurePaymentsFooter()

                Spacer(Modifier.height(8.dp))
            }
                } // Card
            } // Box
        }
    }
}

// ── Paywall hero video ────────────────────────────────────────────────────────

private const val PAYWALL_VIDEO_URL =
    "https://firebasestorage.googleapis.com/v0/b/dobi-ac085.firebasestorage.app/o/paywall_video.mp4?alt=media&token=0e1eb69c-88de-4efa-b661-62bd1d7d8d29"

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun PaywallVideoPlayer(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(PAYWALL_VIDEO_URL))
            volume = 0f
            repeatMode = Player.REPEAT_MODE_ONE
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(Unit) { onDispose { player.release() } }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player = player
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            }
        },
        modifier = modifier,
    )
}

// ── Tab row ──────────────────────────────────────────────────────────────────

@Composable
private fun PlanTabRow(selected: PaywallTab, onSelect: (PaywallTab) -> Unit) {
    val tabs = listOf(
        PaywallTab.AUTOPAY  to "AUTO\nPAY",
        PaywallTab.TRIAL    to "TRIAL\n@ ₹1",
        PaywallTab.PAY_ONCE to "PAY\nONCE",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF0EBFF))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        tabs.forEach { (tab, label) ->
            val isSelected = selected == tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (!isSelected) Modifier.border(1.dp, Color(0xFFD8D0F0), RoundedCornerShape(10.dp))
                        else Modifier
                    )
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) Purple else Color.White)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = { onSelect(tab) },
                    )
                    .padding(vertical = 9.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = if (isSelected) Color.White else TextPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 14.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

// ── Plan card ─────────────────────────────────────────────────────────────────

@Composable
private fun PlanCard(
    plan: PaywallPlan,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val icon: ImageVector = when (plan) {
        PaywallPlan.MONTHLY    -> Icons.Outlined.CurrencyRupee
        PaywallPlan.QUARTERLY  -> Icons.Outlined.BarChart
        PaywallPlan.HALFYEARLY -> Icons.Outlined.Star
        PaywallPlan.TRIAL      -> Icons.Outlined.CurrencyRupee
    }

    Card(
        modifier = modifier
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onSelect,
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Color(0xFFF3EEFF) else Color.White,
        ),
        border = if (selected) BorderStroke(2.dp, Purple)
                 else BorderStroke(1.dp, Color(0xFFE0D9F7)),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Icon circle
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) Purple else Color(0xFFF0EBFF)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) Color.White else Purple,
                    modifier = Modifier.size(16.dp),
                )
            }

            Spacer(Modifier.height(6.dp))

            // POPULAR badge above label — fixed height slot on all cards
            if (plan.isMostPopular) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Purple)
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = "POPULAR",
                        color = Color.White,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            } else {
                Spacer(Modifier.height(15.dp))
            }

            Spacer(Modifier.height(4.dp))

            // Plan label — centered, below badge slot
            Text(
                text = plan.label,
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )

            Spacer(Modifier.height(4.dp))

            // Strikethrough original price — fixed height slot on all cards
            if (plan.originalPriceRs != null) {
                Text(
                    text = "₹${plan.originalPriceRs}",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    style = TextStyle(textDecoration = TextDecoration.LineThrough),
                    textAlign = TextAlign.Center,
                )
            } else {
                Spacer(Modifier.height(14.dp))
            }

            Text(
                text = "₹${plan.priceRs}",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )

            Text(
                text = plan.durationLabel,
                color = TextSecondary,
                fontSize = 9.sp,
                textAlign = TextAlign.Center,
                lineHeight = 12.sp,
            )

            Spacer(Modifier.height(6.dp))

            // Saving badge — fixed height slot on all cards
            if (plan.savingLabel != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (selected) Purple else Color(0xFFEDE8FF))
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = plan.savingLabel,
                        color = if (selected) Color.White else Purple,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                Spacer(Modifier.height(17.dp))
            }

            // Push radio button to bottom
            Spacer(Modifier.weight(1f))

            RadioButton(
                selected = selected,
                onClick = onSelect,
                colors = RadioButtonDefaults.colors(
                    selectedColor = Purple,
                    unselectedColor = Color(0xFFCCC0EE),
                ),
            )
        }
    }
}

// ── Trial card ────────────────────────────────────────────────────────────────

@Composable
private fun TrialCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EEFF)),
        border = BorderStroke(2.dp, Purple),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Start with just",
                color = TextSecondary,
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "₹1",
                color = Purple,
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = "then ₹149/month",
                color = TextSecondary,
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(16.dp))
            listOf(
                "Auto-pay setup with first charge",
                "Cancel anytime before renewal",
                "Full access during trial",
            ).forEach { feature ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Purple),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(text = feature, color = TextPrimary, fontSize = 13.sp)
                }
            }
        }
    }
}

// ── Referral code section ─────────────────────────────────────────────────────

@Composable
private fun ReferralCodeSection(
    code: String,
    applied: Boolean,
    error: String?,
    onCodeChanged: (String) -> Unit,
    onApply: () -> Unit,
    onRemove: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Have a referral code?",
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = code,
                onValueChange = onCodeChanged,
                enabled = !applied,
                placeholder = {
                    Text(
                        text = "Enter referral code",
                        fontSize = 13.sp,
                        color = TextSecondary,
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Purple,
                    unfocusedBorderColor = Color(0xFFE0D9F7),
                    disabledBorderColor = Color(0xFFE0D9F7),
                    focusedLabelColor = Purple,
                ),
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = if (applied) onRemove else onApply,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (applied) Color(0xFFEF4444) else Purple,
                ),
            ) {
                Text(
                    text = if (applied) "Remove" else "Apply",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        if (error != null) {
            Text(
                text = error,
                color = Color(0xFFEF4444),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (applied) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF22C55E),
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Referral code applied successfully!",
                    color = Color(0xFF22C55E),
                    fontSize = 12.sp,
                )
            }
        }
    }
}

// ── Subscribe CTA button ──────────────────────────────────────────────────────

@Composable
private fun SubscribeButton(
    plan: PaywallPlan,
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(listOf(PurpleDark, Purple, Pink))
            )
            .clickable(enabled = !isLoading, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
            )
        } else if (plan == PaywallPlan.TRIAL) {
            Text(
                text = "⚡ START TRIAL FOR ₹1",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        } else {
            val ctaText = buildAnnotatedString {
                append("⚡ SUBSCRIBE NOW FOR ")
                if (plan.ctaOriginalLabel != null) {
                    withStyle(
                        SpanStyle(
                            textDecoration = TextDecoration.LineThrough,
                            color = Color.White.copy(alpha = 0.65f),
                        )
                    ) {
                        append(plan.ctaOriginalLabel)
                    }
                    append(" ")
                }
                withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) {
                    append(plan.ctaDiscountedLabel)
                }
            }
            Text(
                text = ctaText,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

// ── Money back banner ─────────────────────────────────────────────────────────

@Composable
private fun MoneyBackBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF0EBFF))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            tint = Purple,
            modifier = Modifier.size(28.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = "30-Day Money Back Guarantee",
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Not satisfied? Get a full refund — no questions asked.",
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 15.sp,
            )
        }
    }
}

// ── Secure payments footer ────────────────────────────────────────────────────

@Composable
private fun SecurePaymentsFooter() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(12.dp),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = "Secure payments powered by ",
            color = TextSecondary,
            fontSize = 11.sp,
        )
        Text(
            text = "Razorpay",
            color = Purple,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// ── Payment failed overlay ────────────────────────────────────────────────────

@Composable
private fun PaymentFailedOverlay(
    reason: String,
    onRetry: () -> Unit,
    onChangePlan: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White)
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // Error icon
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFFEE2E2)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Cancel,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(40.dp),
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Payment Failed",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
                Text(
                    text = reason,
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Try Again — primary gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(listOf(PurpleDark, Purple, Pink))
                        )
                        .clickable(onClick = onRetry),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Try Again", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                // Change Plan — outlined
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.5.dp, Purple, RoundedCornerShape(14.dp))
                        .clickable(onClick = onChangePlan),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Change Plan", color = Purple, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }
        }
    }
}

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
