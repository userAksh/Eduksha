package com.dobi.app.feature.uploadtuition

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dobi.app.core.ui.theme.DobiBackground
import com.dobi.app.core.ui.theme.DobiBorder
import com.dobi.app.core.ui.theme.DobiTextPrimary
import com.dobi.app.core.ui.theme.DobiTextSecondary
import com.dobi.app.core.ui.theme.SFProBold
import com.dobi.app.core.ui.theme.SFProRegular
import com.dobi.app.core.ui.theme.SFProSemiBold
import com.dobi.app.feature.uploadwork.WorkUploadKind

enum class TuitionUploadParent(val key: String, val title: String, val subtitle: String) {
    WORK(
        key = "work",
        title = "Upload Tuition Work",
        subtitle = "What did you do in today's tuition class?"
    ),
    HOMEWORK(
        key = "homework",
        title = "Upload Tuition Homework",
        subtitle = "What homework did your tutor give you?"
    );

    companion object {
        fun fromKey(key: String): TuitionUploadParent =
            values().firstOrNull { it.key == key } ?: WORK
    }
}

private data class TuitionOption(
    val emoji: String,
    val title: String,
    val subtitle: String,
    val kind: WorkUploadKind
)

private fun optionsFor(parent: TuitionUploadParent): List<TuitionOption> = when (parent) {
    TuitionUploadParent.WORK -> listOf(
        TuitionOption(
            emoji = "📖",
            title = "Today's Learning / Reading",
            subtitle = "Notes or pages your tutor went through with you",
            kind = WorkUploadKind.TUITION_WORK_LEARNING
        ),
        TuitionOption(
            emoji = "✍️",
            title = "Written Work",
            subtitle = "Anything you wrote down or solved during tuition",
            kind = WorkUploadKind.TUITION_WORK_WRITTEN
        ),
        TuitionOption(
            emoji = "🧪",
            title = "Today's Test",
            subtitle = "A quick test or assessment your tutor gave today",
            kind = WorkUploadKind.TUITION_WORK_TEST
        )
    )

    TuitionUploadParent.HOMEWORK -> listOf(
        TuitionOption(
            emoji = "📖",
            title = "Today's Learning / Reading",
            subtitle = "Reading or notes your tutor asked you to go through",
            kind = WorkUploadKind.TUITION_HW_LEARNING
        ),
        TuitionOption(
            emoji = "✍️",
            title = "Written Work",
            subtitle = "Written homework you finished at home",
            kind = WorkUploadKind.TUITION_HW_WRITTEN
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadTuitionOptionsScreen(
    parent: TuitionUploadParent,
    onNavigateBack: () -> Unit,
    onSelectKind: (WorkUploadKind) -> Unit
) {
    val options = remember(parent) { optionsFor(parent) }

    Scaffold(
        containerColor = DobiBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = parent.title,
                        fontWeight = SFProBold,
                        color = DobiTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = DobiTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DobiBackground)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 4.dp)
        ) {
            Text(
                text = parent.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = DobiTextSecondary
            )
            Spacer(Modifier.height(20.dp))
            options.forEach { option ->
                OptionCard(
                    emoji = option.emoji,
                    title = option.title,
                    subtitle = option.subtitle,
                    onClick = { onSelectKind(option.kind) }
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun OptionCard(
    emoji: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFEDE9FE)),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 24.sp)
            }
            Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = SFProSemiBold,
                    color = DobiTextPrimary,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    fontWeight = SFProRegular,
                    color = DobiTextSecondary,
                    lineHeight = 16.sp
                )
            }
            Spacer(Modifier.size(8.dp))
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = DobiBorder,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
