package com.dobi.app.feature.parenthome

import android.app.Activity
import android.content.ContextWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.dobi.app.R
import com.dobi.app.data.local.LanguagePreference
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

@OptIn(ExperimentalMaterial3Api::class)
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
    onInviteParents: () -> Unit = {},
    referralRewards: List<com.dobi.app.domain.model.ReferralReward> = emptyList(),
    onScratchReward: (rewardId: String, onRevealed: (String) -> Unit) -> Unit = { _, _ -> },
    onLanguage: () -> Unit = {},
    onPrivacyData: () -> Unit = {},
    onFaq: () -> Unit = {},
    onHelpSupport: () -> Unit = {},
    onSignOut: () -> Unit = {},
) {
    val context = LocalContext.current
    var weeklyReportsOn by rememberSaveable { mutableStateOf(true) }
    var whatsappReportsOn by rememberSaveable { mutableStateOf(true) }
    var selectedChildId by rememberSaveable(children) {
        mutableStateOf(children.firstOrNull()?.id ?: "")
    }
    var currentLang by rememberSaveable {
        mutableStateOf(LanguagePreference.get(context))
    }
    var showLanguagePicker by remember { mutableStateOf(false) }
    val langSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

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
                SectionHeader(title = stringResource(R.string.settings_section_children))
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
                SectionHeader(title = stringResource(R.string.settings_section_learning))
                Spacer(Modifier.height(8.dp))
                GroupCard {
                    SettingsRow(
                        icon = Icons.Filled.Schedule,
                        iconTint = PositiveGreen,
                        iconBg = PositiveSoft,
                        title = stringResource(R.string.settings_weekly_reports),
                        subtitle = stringResource(R.string.settings_weekly_reports_desc),
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
                        title = stringResource(R.string.settings_whatsapp_reports),
                        subtitle = stringResource(R.string.settings_whatsapp_reports_desc),
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
                        title = stringResource(R.string.settings_study_goals),
                        subtitle = stringResource(R.string.settings_study_goals_desc),
                        trailing = { ChevronTrailing() },
                        onClick = { onStudyGoals(selectedChildId) },
                    )
                    RowDivider()
                    val langLabel = if (currentLang == LanguagePreference.LANG_HI)
                        stringResource(R.string.settings_language_hindi)
                    else
                        stringResource(R.string.settings_language_english)
                    SettingsRow(
                        icon = Icons.Filled.Language,
                        iconTint = PrimaryBlue,
                        iconBg = SoftBlueBg,
                        title = stringResource(R.string.settings_language),
                        subtitle = stringResource(R.string.settings_language_desc),
                        trailing = {
                            Text(
                                text = langLabel,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryBlue,
                            )
                            Spacer(Modifier.width(6.dp))
                            ChevronTrailing()
                        },
                        onClick = { showLanguagePicker = true },
                    )
                }
            }

            item {
                SectionHeader(title = "Invite & Rewards")
                Spacer(Modifier.height(8.dp))
                GroupCard {
                    SettingsRow(
                        icon = Icons.Filled.CardGiftcard,
                        iconTint = AmberDeep,
                        iconBg = AmberSoft,
                        title = "Invite a parent",
                        subtitle = "Share Eduksha and earn rewards when they join",
                        trailing = { ChevronTrailing() },
                        onClick = onInviteParents,
                    )
                }
            }

            if (referralRewards.isNotEmpty()) {
                item {
                    SectionHeader(title = "Your Rewards")
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        referralRewards.forEach { reward ->
                            ScratchCard(
                                reward = reward,
                                onScratch = { onScratchReward(reward.id) { } },
                            )
                        }
                    }
                }
            }

            item {
                SectionHeader(title = stringResource(R.string.settings_section_account))
                Spacer(Modifier.height(8.dp))
                GroupCard {
                    SettingsRow(
                        icon = Icons.Filled.Lock,
                        iconTint = PositiveGreen,
                        iconBg = PositiveSoft,
                        title = stringResource(R.string.settings_privacy),
                        subtitle = stringResource(R.string.settings_privacy_desc),
                        trailing = { ChevronTrailing() },
                        onClick = onPrivacyData,
                    )
                    RowDivider()
                    SettingsRow(
                        icon = Icons.AutoMirrored.Filled.HelpOutline,
                        iconTint = PrimaryBlue,
                        iconBg = SoftBlueBg,
                        title = stringResource(R.string.settings_faq),
                        subtitle = stringResource(R.string.settings_faq_desc),
                        trailing = { ChevronTrailing() },
                        onClick = onFaq,
                    )
                    RowDivider()
                    SettingsRow(
                        icon = Icons.Filled.SupportAgent,
                        iconTint = AmberDeep,
                        iconBg = AmberSoft,
                        title = stringResource(R.string.settings_help),
                        subtitle = stringResource(R.string.settings_help_desc),
                        trailing = { ChevronTrailing() },
                        onClick = onHelpSupport,
                    )
                    RowDivider()
                    SettingsRow(
                        icon = Icons.AutoMirrored.Filled.Logout,
                        iconTint = DangerText,
                        iconBg = DangerSoft,
                        title = stringResource(R.string.settings_logout),
                        titleColor = DangerText,
                        subtitle = stringResource(R.string.settings_logout_desc),
                        trailing = { ChevronTrailing() },
                        onClick = onSignOut,
                    )
                }
            }

            item { TrustFooter() }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }

    if (showLanguagePicker) {
        ModalBottomSheet(
            onDismissRequest = { showLanguagePicker = false },
            sheetState = langSheetState,
            containerColor = Color.White,
        ) {
            LanguagePickerSheet(
                currentLang = currentLang,
                onSelect = { lang ->
                    LanguagePreference.set(context, lang)
                    currentLang = lang
                    coroutineScope.launch {
                        langSheetState.hide()
                        showLanguagePicker = false
                        var ctx = context
                        while (ctx is ContextWrapper && ctx !is Activity) ctx = ctx.baseContext
                        (ctx as? Activity)?.recreate()
                    }
                },
            )
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
                text = if (verified) stringResource(R.string.settings_parent_account) else stringResource(R.string.settings_complete_profile),
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
                text = stringResource(R.string.settings_add_child_btn),
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

// ── Language picker sheet ─────────────────────────────────────────────────────

@Composable
private fun LanguagePickerSheet(
    currentLang: String,
    onSelect: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Text(
            text = stringResource(R.string.lang_picker_title),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryText,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        LanguageOption(
            label = stringResource(R.string.lang_picker_english),
            selected = currentLang == LanguagePreference.LANG_EN,
            onClick = { onSelect(LanguagePreference.LANG_EN) },
        )
        HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 4.dp))
        LanguageOption(
            label = stringResource(R.string.lang_picker_hindi),
            selected = currentLang == LanguagePreference.LANG_HI,
            onClick = { onSelect(LanguagePreference.LANG_HI) },
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun LanguageOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) PrimaryBlue else PrimaryText,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = PrimaryBlue,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// ── Trust footer ─────────────────────────────────────────────────────────────

// ── Scratch card ─────────────────────────────────────────────────────────────

@Composable
private fun ScratchCard(
    reward: com.dobi.app.domain.model.ReferralReward,
    onScratch: () -> Unit,
) {
    val scratchedLines = remember { mutableListOf<Pair<Offset, Offset>>() }
    var scratchProgress by remember { mutableFloatStateOf(if (reward.isScratched) 1f else 0f) }
    var revealed by remember { mutableStateOf(reward.isScratched) }
    var lastPos by remember { mutableStateOf<Offset?>(null) }

    val cardBg = Color(0xFFFFF8E6)
    val scratchBg = Color(0xFFFFD700)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = BorderStroke(1.dp, Color(0xFFE8D090)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🎁", fontSize = 22.sp)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (reward.referredParentName.isNotBlank())
                            "${reward.referredParentName} joined Eduksha!"
                        else "Referral reward unlocked!",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberDeep,
                    )
                    Text(
                        text = reward.rewardLabel,
                        fontSize = 11.sp,
                        color = SecondaryText,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // The scratch area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                // Revealed content beneath
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color(0xFFEFF9F0)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (revealed) reward.rewardValue else "🎉",
                        fontSize = if (revealed) 18.sp else 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF15803D),
                        textAlign = TextAlign.Center,
                    )
                }

                if (!revealed) {
                    // Scratchable overlay
                    Canvas(
                        modifier = Modifier
                            .matchParentSize()
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { pos -> lastPos = pos },
                                    onDrag = { change, _ ->
                                        val cur = change.position
                                        lastPos?.let { prev -> scratchedLines.add(prev to cur) }
                                        lastPos = cur
                                        // Rough progress: count lines relative to card area.
                                        scratchProgress =
                                            (scratchedLines.size / 80f).coerceAtMost(1f)
                                        if (scratchProgress >= 0.5f && !revealed) {
                                            revealed = true
                                            onScratch()
                                        }
                                    },
                                    onDragEnd = { lastPos = null },
                                )
                            }
                    ) {
                        drawRect(color = scratchBg)
                        val erasePaint = Paint().apply {
                            blendMode = BlendMode.Clear
                            strokeWidth = 40f
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        }
                        drawIntoCanvas { canvas ->
                            scratchedLines.forEach { (a, b) ->
                                canvas.drawLine(a, b, erasePaint)
                            }
                        }
                    }

                    Text(
                        text = "← Scratch to reveal →",
                        fontSize = 11.sp,
                        color = Color(0xFF5C4500),
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }

            if (revealed && reward.rewardValue.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Show this code at checkout or apply in-app",
                    fontSize = 11.sp,
                    color = SecondaryText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────

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
            text = stringResource(R.string.settings_trust_footer),
            fontSize = 11.sp,
            color = SecondaryText,
        )
    }
}
