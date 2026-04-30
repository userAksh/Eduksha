package com.dobi.app.feature.uploadwork

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.dobi.app.core.ui.theme.DobiBackground
import com.dobi.app.core.ui.theme.DobiPrimary
import com.dobi.app.core.ui.theme.DobiPrimaryContainer
import com.dobi.app.core.ui.theme.DobiTextPrimary
import com.dobi.app.core.ui.theme.DobiTextSecondary
import com.dobi.app.core.ui.theme.SFProBold
import com.dobi.app.core.ui.theme.SFProMedium
import com.dobi.app.core.ui.theme.SFProSemiBold
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadWorkScreen(
    onNavigateBack: () -> Unit,
    onUploadComplete: (kindKey: String, quizCacheKey: String, displayName: String) -> Unit,
    viewModel: UploadWorkViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onUploadComplete(
            uiState.kind.key,
            uiState.generatedQuizCacheKey,
            uiState.generatedQuizDisplayName
        )
    }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = uiState.dateMillis)

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.onDateChanged(it) }
                    showDatePicker = false
                }) { Text("OK", color = DobiPrimary) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = DobiTextSecondary)
                }
            }
        ) { DatePicker(state = datePickerState) }
    }

    val galleryPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        viewModel.onPhotosAdded(uris.map { it.toString() })
    }

    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            pendingCameraUri?.toString()?.let { viewModel.onPhotosAdded(listOf(it)) }
        }
        pendingCameraUri = null
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = createCameraImageUri(context)
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        }
    }

    fun launchCamera() {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            val uri = createCameraImageUri(context)
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        containerColor = DobiBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        uiState.kind.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = SFProBold,
                        color = DobiTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = DobiTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DobiBackground)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            if (uiState.kind.isTuition) {
                TuitionContextCard(title = uiState.kind.title)
            } else {
                LessonContextCard(unitName = uiState.unitName, subject = uiState.subject)
            }
            Spacer(Modifier.height(20.dp))

            FieldLabel(text = if (uiState.kind.isTuition) "Tutor's Name" else "Teacher's Name")
            Spacer(Modifier.height(10.dp))
            if (uiState.kind.isTuition) {
                // Tutor name is set once on the home Tuition popup and reused — read only here.
                val tutorDisplay = listOf(uiState.teacherHonorific, uiState.teacherName)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                    .ifBlank { "Add tutor's name from home screen" }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFEDE9FE))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("👨‍🏫", fontSize = 20.sp)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            tutorDisplay,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = SFProSemiBold,
                            color = if (uiState.teacherName.isNotBlank()) DobiTextPrimary else DobiTextSecondary
                        )
                        if (uiState.teacherName.isNotBlank()) {
                            Text(
                                "Edit on home screen",
                                fontSize = 11.sp,
                                color = DobiTextSecondary
                            )
                        }
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Sir", "Madam").forEach { honorific ->
                        HonorificChip(
                            label = honorific,
                            selected = uiState.teacherHonorific == honorific,
                            onClick = { viewModel.onTeacherHonorificChanged(honorific) }
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = uiState.teacherName,
                    onValueChange = { input ->
                        if (input.all { it.isLetter() || it.isWhitespace() || it == '.' }) {
                            viewModel.onTeacherNameChanged(input)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. Sharma", color = DobiTextSecondary) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = dobiTextFieldColors()
                )
            }

            Spacer(Modifier.height(24.dp))

            FieldLabel(text = "Date")
            Spacer(Modifier.height(10.dp))
            DateRow(displayDate = uiState.formattedDate, onClick = { showDatePicker = true })

            Spacer(Modifier.height(24.dp))

            FieldLabel(text = "Photos")
            Spacer(Modifier.height(6.dp))
            Text(
                uiState.kind.emptyHint,
                fontSize = 12.sp,
                color = DobiTextSecondary
            )
            Spacer(Modifier.height(12.dp))

            if (uiState.selectedPhotoUris.isNotEmpty()) {
                PhotoGrid(uris = uiState.selectedPhotoUris, onRemove = viewModel::onPhotoRemoved)
                Spacer(Modifier.height(12.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PhotoSourceButton(
                    icon = Icons.Default.CameraAlt,
                    label = "Camera",
                    onClick = ::launchCamera,
                    modifier = Modifier.weight(1f)
                )
                PhotoSourceButton(
                    icon = Icons.Default.AddPhotoAlternate,
                    label = "Gallery",
                    onClick = { galleryPicker.launch("image/*") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(32.dp))

            uiState.error?.let { errorMsg ->
                Text(
                    text = errorMsg,
                    color = Color(0xFFEF4444),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Button(
                onClick = viewModel::onSave,
                enabled = uiState.canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DobiPrimary,
                    disabledContainerColor = Color(0xFFBFD7F7)
                )
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    val busyLabel = when {
                        uiState.isValidatingPhotos -> "Checking your photos…"
                        uiState.isGeneratingQuiz -> "Reading your pages…"
                        else -> ""
                    }
                    if (busyLabel.isNotBlank()) {
                        Spacer(Modifier.width(10.dp))
                        Text(
                            busyLabel,
                            fontWeight = SFProSemiBold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                } else {
                    Text(
                        uiState.kind.ctaLabel,
                        fontWeight = SFProSemiBold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TuitionContextCard(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DobiPrimaryContainer)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("📐", fontSize = 22.sp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Tuition",
                fontSize = 11.sp,
                color = DobiPrimary,
                fontWeight = SFProSemiBold
            )
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = SFProSemiBold,
                color = DobiTextPrimary
            )
        }
    }
}

@Composable
private fun LessonContextCard(unitName: String, subject: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DobiPrimaryContainer)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("📘", fontSize = 22.sp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Lesson",
                fontSize = 11.sp,
                color = DobiPrimary,
                fontWeight = SFProSemiBold
            )
            Text(
                unitName.ifBlank { "Loading…" },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = SFProSemiBold,
                color = DobiTextPrimary
            )
            if (subject.isNotBlank()) {
                Text(
                    subject,
                    style = MaterialTheme.typography.bodySmall,
                    color = DobiTextSecondary
                )
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = SFProSemiBold,
        color = DobiTextPrimary
    )
}

@Composable
private fun DateRow(displayDate: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(14.dp), spotColor = Color(0x0D000000))
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.CalendarToday,
            contentDescription = null,
            tint = DobiPrimary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = displayDate,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = SFProMedium,
            color = DobiTextPrimary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "Change",
            style = MaterialTheme.typography.bodySmall,
            color = DobiPrimary,
            fontWeight = SFProMedium
        )
    }
}

@Composable
private fun PhotoSourceButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .shadow(3.dp, RoundedCornerShape(14.dp), spotColor = Color(0x0D000000))
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = DobiPrimary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = SFProSemiBold,
            color = DobiPrimary
        )
    }
}

@Composable
private fun PhotoGrid(uris: List<String>, onRemove: (String) -> Unit) {
    val rows = uris.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { rowUris ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowUris.forEach { uri ->
                    Box(modifier = Modifier.size(96.dp)) {
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onRemove(uri) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HonorificChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        targetValue = if (selected) DobiPrimary else Color.White,
        label = "chip-bg"
    )
    val border by animateColorAsState(
        targetValue = if (selected) DobiPrimary else Color(0xFFE2E8F0),
        label = "chip-border"
    )
    val content by animateColorAsState(
        targetValue = if (selected) Color.White else DobiTextSecondary,
        label = "chip-fg"
    )
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(1.5.dp, border, RoundedCornerShape(50))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selected) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = SFProSemiBold,
            color = content
        )
    }
}

@Composable
private fun dobiTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = DobiPrimary,
    unfocusedBorderColor = Color(0xFFE2E8F0),
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    focusedTextColor = DobiTextPrimary,
    unfocusedTextColor = DobiTextPrimary,
    cursorColor = DobiPrimary
)

private fun createCameraImageUri(context: Context): Uri {
    val file = File.createTempFile(
        "dobi_${System.currentTimeMillis()}_",
        ".jpg",
        context.cacheDir
    )
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
}
