package com.dobi.app.feature.parenthome

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dobi.app.domain.model.Child
import com.dobi.app.core.ui.theme.DobiPrimary
import com.dobi.app.core.ui.theme.DobiPrimaryContainer
import com.dobi.app.core.ui.theme.DobiPrimarySoft
import com.dobi.app.core.ui.theme.DobiSuccess
import com.dobi.app.core.ui.theme.DobiSuccessContainer
import com.dobi.app.core.ui.theme.DobiTextPrimary
import com.dobi.app.core.ui.theme.DobiTextSecondary
import com.dobi.app.core.ui.theme.SFProBold
import com.dobi.app.core.ui.theme.SFProMedium
import com.dobi.app.core.ui.theme.SFProRegular
import com.dobi.app.core.ui.theme.SFProSemiBold

private val PageBg = Color(0xFFF3F6FD)
private val StatOrange = Color(0xFFF59E0B)
private val StatOrangeBg = Color(0xFFFFF3D6)
private val StatPink = Color(0xFFEF4444)
private val StatPinkBg = Color(0xFFFFE4E6)
private val DividerColor = Color(0xFFF0F2F7)

@Composable
fun ParentHomeScreen(
    onNavigateToChildHome: (childId: String) -> Unit,
    onNavigateToAddChild: () -> Unit,
    onNavigateToRoleSelection: () -> Unit,
    viewModel: ParentHomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navToRoleSelection.collect { onNavigateToRoleSelection() }
    }

    LaunchedEffect(uiState.isLoading, uiState.children.size) {
        if (!uiState.isLoading && uiState.children.isEmpty()) {
            onNavigateToAddChild()
        }
    }

    if (uiState.children.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().background(DobiPrimary))
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(PageBg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            HeaderBlock(
                parentName = uiState.profile?.name ?: "",
                onSignOut = viewModel::signOut
            )

            ContentBlock(
                children = uiState.children,
                onNavigateToChildHome = onNavigateToChildHome,
                onAddChild = onNavigateToAddChild,
                modifier = Modifier
                    .weight(1f)
                    .offset(y = (-24).dp)
            )

            BottomNav()
        }

        FloatingActionButton(
            onClick = onNavigateToAddChild,
            containerColor = DobiPrimary,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 88.dp)
                .size(56.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add child", modifier = Modifier.size(24.dp))
        }
    }
}

// ── Header (blue) ─────────────────────────────────────────────────────────────

@Composable
private fun HeaderBlock(parentName: String, onSignOut: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DobiPrimary)
            .systemBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 48.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = parentName.firstOrNull()?.uppercaseChar()?.toString() ?: "P",
                    fontSize = 24.sp,
                    fontWeight = SFProBold,
                    color = Color.White
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Hello, ${parentName.substringBefore(" ").ifBlank { "there" }}!",
                        fontSize = 20.sp,
                        fontWeight = SFProBold,
                        color = Color.White
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("👋", fontSize = 20.sp)
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Track your children's learning",
                    fontSize = 14.sp,
                    fontWeight = SFProRegular,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }

            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.15f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onSignOut
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Sign out",
                    fontSize = 11.sp,
                    fontWeight = SFProMedium,
                    color = Color.White
                )
            }
        }
    }
}

// ── Content card ──────────────────────────────────────────────────────────────

@Composable
private fun ContentBlock(
    children: List<Child>,
    onNavigateToChildHome: (String) -> Unit,
    onAddChild: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = PageBg,
        shadowElevation = 0.dp
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 20.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Your Children",
                            fontSize = 20.sp,
                            fontWeight = SFProBold,
                            color = DobiTextPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "View progress, insights and help them grow",
                            fontSize = 13.sp,
                            fontWeight = SFProRegular,
                            color = DobiTextSecondary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(DobiPrimaryContainer)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "${children.size} profile${if (children.size > 1) "s" else ""}",
                            fontSize = 12.sp,
                            fontWeight = SFProSemiBold,
                            color = DobiPrimary
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            items(children) { child ->
                ChildStatCard(child = child, onClick = { onNavigateToChildHome(child.id) })
                Spacer(Modifier.height(14.dp))
            }

            item {
                Spacer(Modifier.height(4.dp))
                AddMoreChildrenCard(onAddChild = onAddChild)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// ── Child stat card ───────────────────────────────────────────────────────────

@Composable
private fun ChildStatCard(child: Child, onClick: () -> Unit) {
    val avatarEmoji = when (child.gender) {
        "girl" -> "👧"
        "other" -> "🧒"
        else -> "👦"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(DobiPrimarySoft),
                    contentAlignment = Alignment.Center
                ) {
                    Text(avatarEmoji, fontSize = 40.sp)
                }

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = child.name,
                        fontSize = 20.sp,
                        fontWeight = SFProBold,
                        color = DobiTextPrimary
                    )
                    Spacer(Modifier.height(6.dp))
                    Row {
                        InfoChip(text = "Class ${child.grade}")
                        Spacer(Modifier.width(8.dp))
                        InfoChip(text = child.board)
                    }
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(DobiPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = DividerColor, thickness = 1.dp)
            Spacer(Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                StatItem(
                    icon = Icons.Default.Check,
                    iconTint = DobiSuccess,
                    iconBg = DobiSuccessContainer,
                    value = "12",
                    label = "Concepts Covered",
                    modifier = Modifier.weight(1f)
                )
                VerticalSeparator()
                StatItem(
                    icon = Icons.Default.BarChart,
                    iconTint = StatOrange,
                    iconBg = StatOrangeBg,
                    value = "3",
                    label = "This Week",
                    modifier = Modifier.weight(1f)
                )
                VerticalSeparator()
                StatItem(
                    icon = Icons.Default.LocalFireDepartment,
                    iconTint = StatPink,
                    iconBg = StatPinkBg,
                    value = "5",
                    label = "Day Streak",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun InfoChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DobiPrimaryContainer)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = SFProSemiBold,
            color = DobiPrimary
        )
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.width(6.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = SFProBold,
                color = DobiTextPrimary
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = SFProRegular,
            color = DobiTextSecondary
        )
    }
}

@Composable
private fun VerticalSeparator() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(40.dp)
            .background(DividerColor)
    )
}

// ── Add more children card ────────────────────────────────────────────────────

@Composable
private fun AddMoreChildrenCard(onAddChild: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = DobiPrimaryContainer,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(DobiPrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Add more children",
                    fontSize = 15.sp,
                    fontWeight = SFProBold,
                    color = DobiTextPrimary
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "Track each child's learning and get personalised insights.",
                    fontSize = 12.sp,
                    fontWeight = SFProRegular,
                    color = DobiTextSecondary,
                    lineHeight = 16.sp
                )
            }

            Spacer(Modifier.width(10.dp))

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(DobiPrimary)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onAddChild
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Add Child",
                    fontSize = 12.sp,
                    fontWeight = SFProSemiBold,
                    color = Color.White
                )
            }
        }
    }
}

// ── Bottom navigation ─────────────────────────────────────────────────────────

@Composable
private fun BottomNav() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavTab(
                icon = Icons.Default.People,
                label = "Children",
                active = true,
                modifier = Modifier.weight(1f)
            )
            NavTab(
                icon = Icons.Default.BarChart,
                label = "Insights",
                active = false,
                modifier = Modifier.weight(1f)
            )
            NavTab(
                icon = Icons.Default.CalendarMonth,
                label = "Calendar",
                active = false,
                modifier = Modifier.weight(1f)
            )
            NavTab(
                icon = Icons.Default.Settings,
                label = "Settings",
                active = false,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun NavTab(
    icon: ImageVector,
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (active) DobiPrimaryContainer else Color.Transparent)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (active) DobiPrimary else DobiTextSecondary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (active) SFProSemiBold else SFProMedium,
            color = if (active) DobiPrimary else DobiTextSecondary
        )
    }
}
