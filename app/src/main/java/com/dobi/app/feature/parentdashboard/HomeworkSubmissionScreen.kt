package com.dobi.app.feature.parentdashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.dobi.app.core.ui.theme.DobiAccentOrange
import com.dobi.app.core.ui.theme.DobiBorderMuted
import com.dobi.app.core.ui.theme.DobiPastelCream
import com.dobi.app.core.ui.theme.DobiTextPrimary
import com.dobi.app.core.ui.theme.DobiTextSecondary
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

private val ScreenBg = Color(0xFFFDF6F0)

private data class HomeworkUi(
    val subject: String,
    val teacherName: String,
    val date: String,
    val photoUrls: List<String>,
    val timestampMs: Long,
)

@Composable
fun HomeworkSubmissionScreen(
    homeworkId: String,
    onNavigateBack: () -> Unit,
) {
    var ui by remember(homeworkId) { mutableStateOf<HomeworkUi?>(null) }
    var notFound by remember(homeworkId) { mutableStateOf(false) }
    var fullScreenPhoto by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(homeworkId) {
        if (homeworkId.isBlank()) { notFound = true; return@LaunchedEffect }
        runCatching {
            val snap = FirebaseFirestore.getInstance()
                .collection("homeworks")
                .document(homeworkId)
                .get()
                .await()
            if (!snap.exists()) { notFound = true; return@runCatching }
            ui = HomeworkUi(
                subject = snap.getString("subject").orEmpty(),
                teacherName = snap.getString("teacherName").orEmpty(),
                date = snap.getString("date").orEmpty(),
                photoUrls = (snap.get("photoUrls") as? List<*>)
                    .orEmpty().filterIsInstance<String>(),
                timestampMs = (snap.get("timestamp") as? Timestamp)?.toDate()?.time ?: 0L,
            )
        }.onFailure { notFound = true }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg),
    ) {
        TopBar(
            subject = ui?.subject.orEmpty(),
            teacherName = ui?.teacherName.orEmpty(),
            onNavigateBack = onNavigateBack,
        )
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            color = Color.White,
        ) {
            when {
                notFound -> NotFound(modifier = Modifier.padding(24.dp))
                ui == null -> LoadingIndicator()
                else -> SubmissionBody(
                    ui = ui!!,
                    onPhotoTap = { fullScreenPhoto = it },
                )
            }
        }
    }

    if (fullScreenPhoto != null) {
        FullScreenPhoto(
            url = fullScreenPhoto!!,
            onDismiss = { fullScreenPhoto = null },
        )
    }
}

@Composable
private fun TopBar(
    subject: String,
    teacherName: String,
    onNavigateBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ScreenBg)
            .statusBarsPadding()
            .padding(start = 12.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable(onClick = onNavigateBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = DobiTextPrimary,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(4.dp))
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DobiPastelCream),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.EditNote,
                contentDescription = null,
                tint = DobiAccentOrange,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (subject.isBlank()) "Homework" else "$subject homework",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = DobiTextPrimary,
                maxLines = 1,
            )
            if (teacherName.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = teacherName,
                    fontSize = 12.sp,
                    color = DobiTextSecondary,
                )
            }
        }
    }
}

@Composable
private fun SubmissionBody(
    ui: HomeworkUi,
    onPhotoTap: (String) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
        MetaRow(ui = ui)
        Spacer(Modifier.height(14.dp))
        Text(
            text = "Attachments (${ui.photoUrls.size})",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = DobiTextPrimary,
        )
        Spacer(Modifier.height(10.dp))
        if (ui.photoUrls.isEmpty()) {
            Text(
                text = "No photos attached.",
                fontSize = 13.sp,
                color = DobiTextSecondary,
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(ui.photoUrls) { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DobiBorderMuted)
                            .clickable { onPhotoTap(url) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MetaRow(ui: HomeworkUi) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = DobiPastelCream,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MetaItem(label = "Subject", value = ui.subject.ifBlank { "—" })
            Spacer(Modifier.width(16.dp))
            MetaItem(label = "Date", value = ui.date.ifBlank { "—" })
        }
    }
}

@Composable
private fun MetaItem(label: String, value: String) {
    Column {
        Text(label, fontSize = 11.sp, color = DobiTextSecondary)
        Spacer(Modifier.height(2.dp))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = DobiTextPrimary)
    }
}

@Composable
private fun NotFound(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "This homework could not be loaded.",
            fontSize = 14.sp,
            color = DobiTextSecondary,
        )
    }
}

@Composable
private fun LoadingIndicator() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = DobiAccentOrange, strokeWidth = 3.dp)
    }
}

@Composable
private fun FullScreenPhoto(url: String, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .height(640.dp)
                .padding(24.dp),
        )
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(12.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Close",
                tint = Color.White,
            )
        }
    }
}
