package com.dobi.app.feature.parenthome

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.dobi.app.R
import com.dobi.app.domain.model.Child
import com.dobi.app.domain.model.ParentProfile

// ── Local palette (per design brief) ─────────────────────────────────────────
private val ScreenBg = Color(0xFFFFF8EE)
private val CardBg = Color.White
private val CardBorder = Color(0xFFEDEAE0)
private val SectionLabelColor = Color(0xFF16213E)
private val PrimaryBlue = Color(0xFF3B7BFF)
private val SoftBlueBg = Color(0xFFEEF5FF)
private val PrimaryText = Color(0xFF16213E)
private val SecondaryText = Color(0xFF6B7895)
private val TertiaryText = Color(0xFFA0ADC2)
private val DividerColor = Color(0xFFF1EEE2)

private val PositiveGreen = Color(0xFF16A34A)
private val PositiveSoft = Color(0xFFE6F7EC)
private val AmberDeep = Color(0xFFEA580C)
private val AmberSoft = Color(0xFFFFEFD9)
private val PurpleDeep = Color(0xFF7C5CF8)
private val PurpleSoft = Color(0xFFEAE3FF)
private val DangerSoft = Color(0xFFFEE2E2)
private val DangerText = Color(0xFFDC2626)
private val ProfileAvatarBg = Color(0xFFFFE7B5)

@Composable
fun ParentSettingsTab(
    profile: ParentProfile?,
    children: List<Child>,
    onBack: () -> Unit = {},
    onEditProfile: () -> Unit = {},
    onChildClick: (childId: String) -> Unit = {},
    onManageChildren: () -> Unit = {},
    onAddChild: () -> Unit = {},
    onStudyGoals: (childId: String) -> Unit = {},
    onLanguage: () -> Unit = {},
    onPrivacyData: () -> Unit = {},
    onFaq: () -> Unit = {},
    onHelpSupport: () -> Unit = {},
    onSignOut: () -> Unit = {},
) {
    var weeklyReportsOn by rememberSaveable { mutableStateOf(true) }
    var whatsappReportsOn by rememberSaveable { mutableStateOf(true) }
    var selectedChildId by rememberSaveable(children) {
        mutableStateOf(children.firstOrNull()?.id ?: "")
    }

    // Paint the system status bar in ScreenBg so the OS bar (network/battery)
    // sits on the page background. Mirrors ParentDashboardScreen.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as android.app.Activity).window
            window.statusBarColor = ScreenBg.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                ProfileCard(
                    name = profile?.name?.takeIf { it.isNotBlank() } ?: "Your name",
                    contact = profile?.phone?.takeIf { it.isNotBlank() } ?: "Add your phone or email",
                    verified = !profile?.name.isNullOrBlank(),
                    onClick = onEditProfile,
                )
            }

            item {
                SectionHeader(title = "Children")
                Spacer(Modifier.height(8.dp))
                ChildrenSwitcherCard(
                    children = children,
                    selectedChildId = selectedChildId,
                    onChildClick = {
                        selectedChildId = it.id
                        onChildClick(it.id)
                    },
                    onAddChild = onAddChild,
                )
            }

            item {
                SectionHeader(title = "Learning & Activity")
                Spacer(Modifier.height(8.dp))
                GroupCard {
                    SettingsRow(
                        icon = Icons.Filled.Schedule,
                        iconTint = PositiveGreen,
                        iconBg = PositiveSoft,
                        title = "Weekly Reports",
                        subtitle = "Receive progress reports every week",
                        trailing = {
                            BrandedSwitch(
                                checked = weeklyReportsOn,
                                onCheckedChange = { weeklyReportsOn = it },
                            )
                        },
                        onClick = { weeklyReportsOn = !weeklyReportsOn },
                    )
                    RowDivider()
                    SettingsRow(
                        icon = Icons.Filled.Phone,
                        iconTint = PositiveGreen,
                        iconBg = PositiveSoft,
                        title = "WhatsApp Reports",
                        subtitle = "Get reports on WhatsApp",
                        trailing = {
                            BrandedSwitch(
                                checked = whatsappReportsOn,
                                onCheckedChange = { whatsappReportsOn = it },
                            )
                        },
                        onClick = { whatsappReportsOn = !whatsappReportsOn },
                    )
                    RowDivider()
                    SettingsRow(
                        icon = Icons.Filled.GpsFixed,
                        iconTint = PurpleDeep,
                        iconBg = PurpleSoft,
                        title = "Study Goals",
                        subtitle = "Assign a task to your child",
                        trailing = { ChevronTrailing() },
                        onClick = { onStudyGoals(selectedChildId) },
                    )
                    RowDivider()
                    SettingsRow(
                        icon = Icons.Filled.Language,
                        iconTint = PrimaryBlue,
                        iconBg = SoftBlueBg,
                        title = "Language",
                        subtitle = "Choose your preferred language",
                        trailing = {
                            Text(
                                text = "English",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryBlue,
                            )
                            Spacer(Modifier.width(6.dp))
                            ChevronTrailing()
                        },
                        onClick = onLanguage,
                    )
                }
            }

            item {
                SectionHeader(title = "Account & Privacy")
                Spacer(Modifier.height(8.dp))
                GroupCard {
                    SettingsRow(
                        icon = Icons.Filled.Lock,
                        iconTint = PositiveGreen,
                        iconBg = PositiveSoft,
                        title = "Privacy & Data",
                        subtitle = "Manage your data and privacy",
                        trailing = { ChevronTrailing() },
                        onClick = onPrivacyData,
                    )
                    RowDivider()
                    SettingsRow(
                        icon = Icons.AutoMirrored.Filled.HelpOutline,
                        iconTint = PrimaryBlue,
                        iconBg = SoftBlueBg,
                        title = "FAQ",
                        subtitle = "Answers to common questions",
                        trailing = { ChevronTrailing() },
                        onClick = onFaq,
                    )
                    RowDivider()
                    SettingsRow(
                        icon = Icons.Filled.SupportAgent,
                        iconTint = AmberDeep,
                        iconBg = AmberSoft,
                        title = "Help & Support",
                        subtitle = "Contact us and report a problem",
                        trailing = { ChevronTrailing() },
                        onClick = onHelpSupport,
                    )
                    RowDivider()
                    SettingsRow(
                        icon = Icons.AutoMirrored.Filled.Logout,
                        iconTint = DangerText,
                        iconBg = DangerSoft,
                        title = "Log Out",
                        titleColor = DangerText,
                        subtitle = "Sign out from your account",
                        trailing = { ChevronTrailing() },
                        onClick = onSignOut,
                    )
                }
            }

            item { TrustFooter() }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}


// ── Profile card ─────────────────────────────────────────────────────────────

@Composable
private fun ProfileCard(
    name: String,
    contact: String,
    verified: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = CardBg,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        border = BorderStroke(1.dp, CardBorder),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(76.dp), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(ProfileAvatarBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = Color(0xFFB45309),
                        modifier = Modifier.size(48.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(SoftBlueBg)
                        .border(2.dp, CardBg, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit profile",
                        tint = PrimaryBlue,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText,
                    maxLines = 1,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = contact,
                    fontSize = 12.sp,
                    color = SecondaryText,
                    maxLines = 1,
                )
                Spacer(Modifier.height(8.dp))
                ParentAccountBadge(verified = verified)
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = TertiaryText,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun ParentAccountBadge(verified: Boolean) {
    val bg = if (verified) PositiveSoft else AmberSoft
    val fg = if (verified) PositiveGreen else AmberDeep
    Surface(shape = RoundedCornerShape(50), color = bg) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (verified) Icons.Filled.Shield else Icons.Filled.Person,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(12.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = if (verified) "Parent Account" else "Complete Profile",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = fg,
            )
        }
    }
}

// ── Children switcher card ───────────────────────────────────────────────────

private val SwitcherBorderColor = Color(0xFFE6EAF1)
private val SwitcherDividerColor = Color(0xFFE0E8F2)
private val AddBorderColor = Color(0xFFB8C5DA)

@Composable
private fun ChildrenSwitcherCard(
    children: List<Child>,
    selectedChildId: String,
    onChildClick: (Child) -> Unit,
    onAddChild: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(94.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(CardBg)
            .border(1.dp, SwitcherBorderColor, RoundedCornerShape(28.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        children.forEachIndexed { index, child ->
            ChildSwitcherItem(
                child = child,
                index = index,
                selected = child.id == selectedChildId,
                onClick = { onChildClick(child) },
                modifier = Modifier.weight(1f),
            )
        }
        if (children.isNotEmpty()) {
            VerticalDivider(
                modifier = Modifier.height(48.dp),
                thickness = 1.dp,
                color = SwitcherDividerColor,
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onAddChild),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .border(1.5.dp, AddBorderColor, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add child",
                    tint = PrimaryBlue,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Add",
                fontSize = 12.sp,
                color = PrimaryText,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ChildSwitcherItem(
    child: Child,
    index: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val avatarRes = when (index % 3) {
        0 -> R.drawable.dashboard_avatar_ritika
        1 -> R.drawable.dashboard_avatar_aarav
        else -> R.drawable.dashboard_avatar_vivaan
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Image(
            painter = painterResource(id = avatarRes),
            contentDescription = child.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .then(
                    if (selected) Modifier.border(2.dp, PrimaryBlue, CircleShape)
                    else Modifier.border(1.dp, SwitcherBorderColor, CircleShape)
                ),
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = child.name.substringBefore(" ").ifBlank { "Child" },
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) PrimaryBlue else PrimaryText,
            maxLines = 1,
        )
        if (selected) {
            Box(
                modifier = Modifier
                    .padding(top = 3.dp)
                    .width(28.dp)
                    .height(3.dp)
                    .background(PrimaryBlue, RoundedCornerShape(50)),
            )
        } else {
            Spacer(Modifier.height(6.dp))
        }
    }
}

// ── Section header ───────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = SectionLabelColor,
        modifier = Modifier.padding(start = 4.dp, top = 6.dp),
    )
}

// ── Group card + rows ────────────────────────────────────────────────────────

@Composable
private fun GroupCard(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = CardBg,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, CardBorder),
    ) {
        Column { content() }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit,
    titleColor: Color = PrimaryText,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = titleColor,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = SecondaryText,
                lineHeight = 16.sp,
            )
        }
        Spacer(Modifier.width(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) { trailing() }
    }
}

@Composable
private fun RowDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 64.dp)
            .height(1.dp)
            .background(DividerColor),
    )
}

@Composable
private fun ChevronTrailing() {
    Icon(
        imageVector = Icons.Outlined.ChevronRight,
        contentDescription = null,
        tint = TertiaryText,
        modifier = Modifier.size(20.dp),
    )
}

@Composable
private fun BrandedSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = PositiveGreen,
            checkedBorderColor = PositiveGreen,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = Color(0xFFD8DCE5),
            uncheckedBorderColor = Color(0xFFD8DCE5),
        ),
    )
}

// ── Trust footer ─────────────────────────────────────────────────────────────

@Composable
private fun TrustFooter() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = TertiaryText,
            modifier = Modifier.size(12.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "Your data is safe with us and is never shared.",
            fontSize = 11.sp,
            color = SecondaryText,
        )
    }
}
