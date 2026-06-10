package com.dobi.app.feature.streak

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.dobi.app.core.ui.theme.DobiPrimary
import com.dobi.app.core.ui.theme.DobiTextPrimary
import com.dobi.app.core.ui.theme.DobiTextSecondary
import com.dobi.app.core.ui.theme.SFProBold
import com.dobi.app.core.ui.theme.SFProSemiBold
import com.dobi.app.domain.model.Friend

// ── Template definitions ───────────────────────────────────────────────────────

private val BlueAccent   = Color(0xFF2F5DFF)
private val GreenAccent  = Color(0xFF2D9B4B)
private val PurpleAccent = Color(0xFF7B4FFF)
private val DarkPurpleBg = Color(0xFF1E1060)

private enum class CardTheme { BLUE, GREEN, SPACE }

private data class SnapTemplate(
    val bgColors: List<Color>,
    val accentColor: Color,
    val textColor: Color,
    val isDark: Boolean = false,
    val theme: CardTheme,
    val headline: String,
    val subtitle: String,
    val heroEmoji: String,
    val streakEmoji: String,
    val shareMessage: (Int, String) -> String
)

private val templates = listOf(
    SnapTemplate(
        bgColors     = listOf(Color(0xFFD6E4FF), Color(0xFFECF3FF)),
        accentColor  = BlueAccent,
        textColor    = Color(0xFF1A3A8F),
        theme        = CardTheme.BLUE,
        headline     = "Small steps today,\nbig progress tomorrow!",
        subtitle     = "Stay consistent and unlock exciting rewards.",
        heroEmoji    = "🏆",
        streakEmoji  = "🔥",
        shareMessage = { days, name ->
            "🏆 Small steps today, big progress tomorrow! $name is on a $days-day streak on Eduksha. " +
                "Are you keeping up? 💪 #Eduksha #StudyStreak"
        }
    ),
    SnapTemplate(
        bgColors     = listOf(Color(0xFFDDF0DD), Color(0xFFECF8EC)),
        accentColor  = GreenAccent,
        textColor    = Color(0xFF1A5C2A),
        theme        = CardTheme.GREEN,
        headline     = "You're doing great!",
        subtitle     = "Keep going and make your streak stronger every day.",
        heroEmoji    = "🎁",
        streakEmoji  = "🔥",
        shareMessage = { days, name ->
            "🌿 You're doing great! $name has been learning consistently for $days days on Eduksha. " +
                "Why not challenge yourself too? 🎁 #Eduksha #StudyStreak"
        }
    ),
    SnapTemplate(
        bgColors     = listOf(DarkPurpleBg, Color(0xFF2D1A80)),
        accentColor  = PurpleAccent,
        textColor    = Color.White,
        isDark       = true,
        theme        = CardTheme.SPACE,
        headline     = "Every day counts!",
        subtitle     = "Complete your tasks and reach for your next reward.",
        heroEmoji    = "🏅",
        streakEmoji  = "⚡",
        shareMessage = { days, name ->
            "🚀 Every day counts! $name is building a $days-day streak on Eduksha. " +
                "Let's reach for the stars together! ⚡ #Eduksha #StudyStreak"
        }
    )
)

// ── Main dialog ────────────────────────────────────────────────────────────────

@Composable
fun SnapShareDialog(
    streakDays: Int,
    childName: String,
    friends: List<Friend>,
    onDismiss: () -> Unit,
    onShared: () -> Unit
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(initialPage = 1) { templates.size }

    fun shareText() = templates[pagerState.currentPage].shareMessage(streakDays, childName)

    fun launchIntent(intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            val fallback = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText())
            }
            context.startActivity(Intent.createChooser(fallback, "Share"))
        }
        onShared()
    }

    fun shareVia(pkg: String?) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText())
            if (pkg != null) setPackage(pkg)
        }
        launchIntent(intent)
    }

    fun shareWithFriend(friend: Friend) {
        val text = "Hey ${friend.name}! ${shareText()}"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        launchIntent(intent)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(bottom = 20.dp)) {

                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Share Your Streak 🔥",
                        fontWeight = SFProBold,
                        fontSize = 17.sp,
                        color = DobiTextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = DobiTextSecondary)
                    }
                }

                // Template carousel
                HorizontalPager(
                    state = pagerState,
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    pageSpacing = 12.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp)
                ) { page ->
                    SnapTemplateCard(
                        template   = templates[page],
                        streakDays = streakDays,
                        childName  = childName
                    )
                }

                // Page dots
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(templates.size) { i ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(if (i == pagerState.currentPage) 20.dp else 6.dp, 6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (i == pagerState.currentPage)
                                        templates[i].accentColor
                                    else
                                        Color(0xFFCBD5E1)
                                )
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(1.dp)
                        .background(Color(0xFFE8EDF3))
                )

                // Friends section
                if (friends.isNotEmpty()) {
                    Text(
                        text = "Send to a friend",
                        fontWeight = SFProSemiBold,
                        fontSize = 13.sp,
                        color = DobiTextSecondary,
                        modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 8.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(friends, key = { it.childId }) { friend ->
                            FriendChip(friend = friend, onClick = { shareWithFriend(friend) })
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                            .height(1.dp)
                            .background(Color(0xFFE8EDF3))
                    )
                } else {
                    Spacer(Modifier.height(14.dp))
                }

                // Social share buttons
                Text(
                    text = "Share on",
                    fontWeight = SFProSemiBold,
                    fontSize = 13.sp,
                    color = DobiTextSecondary,
                    modifier = Modifier.padding(start = 20.dp, bottom = 10.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SocialButton(
                        emoji = "💬", label = "WhatsApp", color = Color(0xFF25D366),
                        modifier = Modifier.weight(1f), onClick = { shareVia("com.whatsapp") }
                    )
                    SocialButton(
                        emoji = "📸", label = "Instagram", color = Color(0xFFE1306C),
                        modifier = Modifier.weight(1f), onClick = { shareVia("com.instagram.android") }
                    )
                    SocialButton(
                        emoji = "👍", label = "Facebook", color = Color(0xFF1877F2),
                        modifier = Modifier.weight(1f), onClick = { shareVia("com.facebook.katana") }
                    )
                }
            }
        }
    }
}

// ── Template card ──────────────────────────────────────────────────────────────

@Composable
private fun SnapTemplateCard(template: SnapTemplate, streakDays: Int, childName: String) {
    val filledDots = minOf(streakDays, 7)
    val daysLeft   = (7 - filledDots).coerceAtLeast(0)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(template.bgColors))
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            when (template.theme) {
                CardTheme.BLUE  -> drawMountainScene()
                CardTheme.GREEN -> drawLeafScene()
                CardTheme.SPACE -> drawSpaceScene()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            // Headline — centered over illustration
            Text(
                text      = template.headline,
                fontWeight = SFProBold,
                fontSize  = 14.sp,
                color     = template.textColor,
                lineHeight = 19.sp,
                textAlign = TextAlign.Center,
                modifier  = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text      = template.subtitle,
                fontSize  = 10.sp,
                color     = template.textColor.copy(alpha = 0.75f),
                lineHeight = 13.sp,
                textAlign = TextAlign.Center,
                modifier  = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            // CTA banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(template.accentColor)
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✓", color = template.accentColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text       = "I have completed\nmy today's first task",
                    color      = Color.White,
                    fontSize   = 11.sp,
                    fontWeight = SFProSemiBold,
                    lineHeight = 15.sp,
                    modifier   = Modifier.weight(1f)
                )
                Text("›", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(7.dp))

            // Reward progress card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = template.heroEmoji, fontSize = 36.sp)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = buildAnnotatedString {
                                append("I am ")
                                withStyle(SpanStyle(color = template.accentColor, fontWeight = SFProBold)) {
                                    append("$daysLeft days")
                                }
                                append(" away\nto get rewards!")
                            },
                            fontSize   = 11.sp,
                            color      = Color(0xFF1A202C),
                            lineHeight = 15.sp
                        )
                        Spacer(Modifier.height(5.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            repeat(7) { i ->
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (i < filledDots) template.accentColor
                                            else Color(0xFFE2E8F0)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (i < filledDots) {
                                        Text(
                                            text       = "✓",
                                            color      = Color.White,
                                            fontSize   = 7.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text          = "$daysLeft DAYS LEFT",
                            fontSize      = 8.sp,
                            color         = Color(0xFF718096),
                            fontWeight    = SFProSemiBold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
            Spacer(Modifier.height(7.dp))

            // Theme-specific middle section
            when (template.theme) {
                CardTheme.BLUE  -> StreakOverviewSection(streakDays, filledDots, template.accentColor)
                CardTheme.GREEN -> MotivationCard(
                    icon  = "🌱",
                    title = "Why Streaks Matter?",
                    body  = "Daily consistency helps you build better habits and achieve bigger goals in your learning journey.",
                    isDark = false
                )
                CardTheme.SPACE -> MotivationCard(
                    icon  = "📊",
                    title = "Keep the momentum!",
                    body  = "You're building something amazing with your consistency.",
                    isDark = true
                )
            }
            Spacer(Modifier.height(7.dp))

            // Bottom row (blue gets a unique "Consistency" row)
            when (template.theme) {
                CardTheme.BLUE -> ConsistencyRow(template.accentColor)
                else           -> StreakCountRow(
                    streakEmoji = template.streakEmoji,
                    streakDays  = streakDays,
                    accentColor = template.accentColor,
                    isDark      = template.isDark
                )
            }

            Spacer(Modifier.weight(1f))

            // Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("🔒", fontSize = 9.sp, modifier = Modifier.alpha(0.6f))
                Spacer(Modifier.width(3.dp))
                Text(
                    text      = "Your streak will be shared with your parent",
                    fontSize  = 8.sp,
                    color     = if (template.isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF718096),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ── Blue card: Streak Overview (Mon–Sun day circles) ──────────────────────────

@Composable
private fun StreakOverviewSection(streakDays: Int, filledDots: Int, accentColor: Color) {
    val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Column {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    "Your Streak Overview",
                    fontWeight = SFProBold,
                    fontSize   = 9.sp,
                    color      = Color(0xFF1A202C)
                )
                Text(
                    "🔥 $streakDays Day Streak",
                    fontSize   = 9.sp,
                    fontWeight = SFProSemiBold,
                    color      = Color(0xFFFF6B35)
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                dayLabels.forEachIndexed { i, label ->
                    val active = i < filledDots
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(if (active) accentColor else Color(0xFFECEFF1)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "✓",
                                color      = if (active) Color.White else Color(0xFFCFD8DC),
                                fontSize   = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(label, fontSize = 7.sp, color = Color(0xFF718096))
                    }
                }
            }
        }
    }
}

// ── Green / Space: motivation card ────────────────────────────────────────────

@Composable
private fun MotivationCard(icon: String, title: String, body: String, isDark: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE8EDF3), RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 24.sp)
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = SFProBold, fontSize = 10.sp, color = Color(0xFF1A202C))
            Text(body, fontSize = 9.sp, lineHeight = 12.sp, color = Color(0xFF4A5568))
        }
    }
}

// ── Blue card bottom: Consistency row ─────────────────────────────────────────

@Composable
private fun ConsistencyRow(accentColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text("⭐", fontSize = 14.sp)
        }
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Consistency is the key.",
                fontWeight = SFProBold,
                fontSize   = 10.sp,
                color      = Color(0xFF1A202C)
            )
            Text(
                "Keep your streak alive!",
                fontSize = 9.sp,
                color    = Color(0xFF4A5568)
            )
        }
        Text("›", fontSize = 16.sp, color = Color(0xFF718096))
    }
}

// ── Green / Space bottom: streak count row ────────────────────────────────────

@Composable
private fun StreakCountRow(streakEmoji: String, streakDays: Int, accentColor: Color, isDark: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE8EDF3), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(streakEmoji, fontSize = 14.sp)
        Spacer(Modifier.width(6.dp))
        Text(
            "Current Streak",
            fontSize   = 10.sp,
            fontWeight = SFProSemiBold,
            color      = Color(0xFF1A202C),
            modifier   = Modifier.weight(1f)
        )
        Text("$streakDays Days", fontSize = 10.sp, fontWeight = SFProBold, color = accentColor)
    }
}

// ── Canvas illustrations ───────────────────────────────────────────────────────

private fun DrawScope.drawMountainScene() {
    val w = size.width
    val h = size.height

    // White clouds (drawn first so mountains overlap them)
    val cloudColor = Color.White.copy(alpha = 0.82f)
    for ((cx, cy, r) in listOf(
        Triple(w * 0.07f, h * 0.09f, w * 0.062f),
        Triple(w * 0.16f, h * 0.07f, w * 0.048f),
        Triple(w * 0.25f, h * 0.10f, w * 0.044f),
    )) { drawCircle(cloudColor, r, Offset(cx, cy)) }
    for ((cx, cy, r) in listOf(
        Triple(w * 0.58f, h * 0.05f, w * 0.036f),
        Triple(w * 0.65f, h * 0.04f, w * 0.026f),
    )) { drawCircle(cloudColor, r, Offset(cx, cy)) }

    // Sun behind the rightmost mountain
    val sunCx = w * 0.80f
    val sunCy = h * 0.22f
    val sunR  = w * 0.10f
    drawCircle(Color(0xFFFFA726).copy(alpha = 0.92f), sunR * 1.50f, Offset(sunCx, sunCy)) // glow
    drawCircle(Color(0xFFFFA726), sunR, Offset(sunCx, sunCy))

    // Mountain 1 — back, rounded peak, lightest
    drawPath(
        path = Path().apply {
            moveTo(w * -0.05f, h * 0.58f)
            lineTo(w * 0.14f,  h * 0.34f)
            quadraticBezierTo(w * 0.28f, h * 0.16f, w * 0.42f, h * 0.34f)
            lineTo(w * 0.70f,  h * 0.58f)
            close()
        },
        color = Color(0xFF8BAAD4).copy(alpha = 0.60f)
    )
    // Mountain 2 — mid, rounded
    drawPath(
        path = Path().apply {
            moveTo(w * 0.20f, h * 0.60f)
            lineTo(w * 0.38f, h * 0.36f)
            quadraticBezierTo(w * 0.52f, h * 0.21f, w * 0.66f, h * 0.36f)
            lineTo(w * 0.88f, h * 0.60f)
            close()
        },
        color = Color(0xFF5B87C5).copy(alpha = 0.75f)
    )
    // Mountain 3 — foreground, overlaps sun
    drawPath(
        path = Path().apply {
            moveTo(w * 0.48f, h * 0.61f)
            lineTo(w * 0.66f, h * 0.35f)
            quadraticBezierTo(w * 0.80f, h * 0.18f, w * 0.94f, h * 0.35f)
            lineTo(w * 1.10f, h * 0.61f)
            close()
        },
        color = Color(0xFF4A7BB5).copy(alpha = 0.88f)
    )

    // Snow caps
    val snow = Color.White.copy(alpha = 0.92f)
    drawPath(path = Path().apply {
        moveTo(w * 0.28f, h * 0.16f)
        lineTo(w * 0.21f, h * 0.28f)
        lineTo(w * 0.35f, h * 0.28f)
        close()
    }, color = snow)
    drawPath(path = Path().apply {
        moveTo(w * 0.52f, h * 0.21f)
        lineTo(w * 0.45f, h * 0.33f)
        lineTo(w * 0.59f, h * 0.33f)
        close()
    }, color = snow)
    drawPath(path = Path().apply {
        moveTo(w * 0.80f, h * 0.18f)
        lineTo(w * 0.74f, h * 0.29f)
        lineTo(w * 0.86f, h * 0.29f)
        close()
    }, color = snow)

    // Pine trees at mountain base
    val treeColor = Color(0xFF2E7D32).copy(alpha = 0.72f)
    for ((cx, baseY, hw) in listOf(
        Triple(w * 0.08f, h * 0.54f, w * 0.024f),
        Triple(w * 0.18f, h * 0.55f, w * 0.019f),
        Triple(w * 0.36f, h * 0.56f, w * 0.022f),
        Triple(w * 0.45f, h * 0.55f, w * 0.017f),
        Triple(w * 0.62f, h * 0.57f, w * 0.023f),
        Triple(w * 0.70f, h * 0.56f, w * 0.018f),
    )) {
        drawPath(path = Path().apply {
            moveTo(cx, baseY - hw * 2.4f)
            lineTo(cx + hw, baseY)
            lineTo(cx - hw, baseY)
            close()
        }, color = treeColor)
    }
}

private fun DrawScope.drawLeafScene() {
    val w = size.width
    val h = size.height

    data class Leaf(val cx: Float, val cy: Float, val rx: Float, val ry: Float, val angle: Float, val color: Color)

    // Prominent cluster top-right, small accent top-left, tiny bottom-right
    val leaves = listOf(
        Leaf(w*0.82f, h*0.04f, w*0.052f, w*0.108f, -28f, Color(0xFF388E3C)),
        Leaf(w*0.92f, h*0.02f, w*0.040f, w*0.082f, -58f, Color(0xFF4CAF50)),
        Leaf(w*0.90f, h*0.13f, w*0.036f, w*0.074f,  12f, Color(0xFF66BB6A)),
        Leaf(w*0.98f, h*0.07f, w*0.030f, w*0.062f, -80f, Color(0xFF81C784)),
        Leaf(w*0.75f, h*0.07f, w*0.028f, w*0.058f, -14f, Color(0xFF4CAF50)),
        // top-left accent
        Leaf(w*0.05f, h*0.08f, w*0.028f, w*0.058f,  48f, Color(0xFF4CAF50)),
        Leaf(w*0.13f, h*0.03f, w*0.022f, w*0.046f, -22f, Color(0xFF66BB6A)),
        // bottom-right corner
        Leaf(w*0.92f, h*0.90f, w*0.028f, w*0.058f,  42f, Color(0xFF388E3C)),
        Leaf(w*0.99f, h*0.82f, w*0.022f, w*0.046f, -52f, Color(0xFF4CAF50)),
    )

    for (leaf in leaves) {
        withTransform({ rotate(leaf.angle, Offset(leaf.cx, leaf.cy)) }) {
            drawOval(
                color   = leaf.color,
                topLeft = Offset(leaf.cx - leaf.rx, leaf.cy - leaf.ry),
                size    = Size(leaf.rx * 2f, leaf.ry * 2f)
            )
            // centre vein
            drawLine(
                color       = Color.White.copy(alpha = 0.30f),
                start       = Offset(leaf.cx, leaf.cy - leaf.ry * 0.72f),
                end         = Offset(leaf.cx, leaf.cy + leaf.ry * 0.72f),
                strokeWidth = leaf.rx * 0.18f,
                cap         = StrokeCap.Round
            )
        }
    }
}

private fun DrawScope.drawSpaceScene() {
    val w = size.width
    val h = size.height

    // Star dots
    val starPos = listOf(
        0.08f to 0.05f, 0.22f to 0.12f, 0.40f to 0.04f, 0.60f to 0.08f,
        0.75f to 0.03f, 0.92f to 0.10f, 0.15f to 0.22f, 0.50f to 0.20f,
        0.85f to 0.28f, 0.30f to 0.35f, 0.65f to 0.40f, 0.10f to 0.50f,
        0.90f to 0.55f, 0.45f to 0.60f, 0.70f to 0.68f, 0.20f to 0.75f,
        0.55f to 0.80f, 0.88f to 0.82f, 0.35f to 0.90f, 0.78f to 0.94f,
    )
    val starR = listOf(3f, 2f, 4f, 2f, 3f, 2f, 4f, 2f, 3f, 2f, 4f, 3f, 2f, 3f, 2f, 4f, 2f, 3f, 2f, 4f)
    starPos.forEachIndexed { i, (fx, fy) ->
        drawCircle(Color.White.copy(alpha = 0.72f), starR[i % starR.size], Offset(w * fx, h * fy))
    }

    // 4-pointed sparkles
    fun DrawScope.drawSparkle(cx: Float, cy: Float, r: Float) {
        val c = Color(0xFFFFEB3B).copy(alpha = 0.92f)
        drawPath(path = Path().apply {
            moveTo(cx, cy - r); lineTo(cx + r * 0.22f, cy)
            lineTo(cx, cy + r); lineTo(cx - r * 0.22f, cy); close()
        }, color = c)
        drawPath(path = Path().apply {
            moveTo(cx - r, cy); lineTo(cx, cy - r * 0.22f)
            lineTo(cx + r, cy); lineTo(cx, cy + r * 0.22f); close()
        }, color = c)
    }
    drawSparkle(w * 0.58f, h * 0.06f, w * 0.028f)
    drawSparkle(w * 0.18f, h * 0.15f, w * 0.018f)
    drawSparkle(w * 0.88f, h * 0.36f, w * 0.016f)
    drawSparkle(w * 0.42f, h * 0.65f, w * 0.018f)

    // Planet — top-left
    val pCx = w * 0.14f
    val pCy = h * 0.13f
    val pR  = w * 0.082f
    drawCircle(Color(0xFF9C27B0).copy(alpha = 0.92f), pR, Offset(pCx, pCy))
    drawCircle(Color(0xFFCE93D8).copy(alpha = 0.50f), pR * 0.55f, Offset(pCx, pCy))
    drawOval(
        color    = Color(0xFFCE93D8).copy(alpha = 0.72f),
        topLeft  = Offset(pCx - pR * 1.65f, pCy - pR * 0.28f),
        size     = Size(pR * 3.30f, pR * 0.56f),
        style    = Stroke(width = pR * 0.20f)
    )

    // Purple nebula clouds at bottom (transition to card content area)
    val nebula = Color(0xFF7B4FFF)
    drawOval(nebula.copy(alpha = 0.20f), Offset(w * -0.12f, h * 0.70f), Size(w * 0.68f, h * 0.22f))
    drawOval(nebula.copy(alpha = 0.18f), Offset(w * 0.33f,  h * 0.72f), Size(w * 0.78f, h * 0.20f))
    drawOval(nebula.copy(alpha = 0.12f), Offset(w * 0.10f,  h * 0.80f), Size(w * 0.80f, h * 0.18f))

    // Rocket — top-right, angled ~42° up-right
    val rCx = w * 0.80f
    val rCy = h * 0.13f
    withTransform({ rotate(-42f, Offset(rCx, rCy)) }) {
        val bh = h * 0.11f
        val bw = w * 0.072f
        // body
        drawRoundRect(Color.White, Offset(rCx - bw / 2f, rCy - bh / 2f), Size(bw, bh), CornerRadius(bw * 0.38f))
        // nose cone
        drawPath(path = Path().apply {
            moveTo(rCx, rCy - bh / 2f - bw * 0.58f)
            lineTo(rCx + bw / 2f, rCy - bh / 2f + bw * 0.08f)
            lineTo(rCx - bw / 2f, rCy - bh / 2f + bw * 0.08f)
            close()
        }, color = Color(0xFFFF5722))
        // fins
        drawPath(path = Path().apply {
            moveTo(rCx - bw / 2f, rCy + bh * 0.22f)
            lineTo(rCx - bw * 0.88f, rCy + bh * 0.48f)
            lineTo(rCx - bw / 2f,   rCy + bh * 0.44f)
            close()
        }, color = Color(0xFFFF5722).copy(alpha = 0.85f))
        drawPath(path = Path().apply {
            moveTo(rCx + bw / 2f, rCy + bh * 0.22f)
            lineTo(rCx + bw * 0.88f, rCy + bh * 0.48f)
            lineTo(rCx + bw / 2f,   rCy + bh * 0.44f)
            close()
        }, color = Color(0xFFFF5722).copy(alpha = 0.85f))
        // outer flame
        drawPath(path = Path().apply {
            moveTo(rCx - bw * 0.34f, rCy + bh / 2f)
            lineTo(rCx, rCy + bh / 2f + bw * 0.72f)
            lineTo(rCx + bw * 0.34f, rCy + bh / 2f)
            close()
        }, color = Color(0xFFFF9800).copy(alpha = 0.92f))
        // inner flame
        drawPath(path = Path().apply {
            moveTo(rCx - bw * 0.18f, rCy + bh / 2f)
            lineTo(rCx, rCy + bh / 2f + bw * 0.46f)
            lineTo(rCx + bw * 0.18f, rCy + bh / 2f)
            close()
        }, color = Color(0xFFFFEB3B))
        // window
        drawCircle(Color(0xFFB3E5FC), bw * 0.22f, Offset(rCx, rCy - bh * 0.06f))
    }
}

// ── Friend chip ────────────────────────────────────────────────────────────────

@Composable
private fun FriendChip(friend: Friend, onClick: () -> Unit) {
    val avatarEmoji = if (friend.gender.lowercase() == "girl") "👧" else "👦"
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFF1F5F9))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(DobiPrimary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(avatarEmoji, fontSize = 14.sp)
        }
        Spacer(Modifier.width(6.dp))
        Column {
            Text(
                text       = friend.name.split(" ").firstOrNull() ?: friend.name,
                fontWeight = SFProSemiBold,
                fontSize   = 12.sp,
                color      = DobiTextPrimary
            )
            Text("Tap to share", fontSize = 9.sp, color = DobiTextSecondary)
        }
    }
}

// ── Social share button ────────────────────────────────────────────────────────

@Composable
private fun SocialButton(
    emoji: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 22.sp)
        Spacer(Modifier.height(4.dp))
        Text(text = label, fontSize = 10.sp, fontWeight = SFProSemiBold, color = color)
    }
}
