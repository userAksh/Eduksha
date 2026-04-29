package com.dobi.app.feature.tuitionlearning

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.dobi.app.core.ui.theme.DobiBackground
import com.dobi.app.core.ui.theme.DobiBorder
import com.dobi.app.core.ui.theme.DobiPrimary
import com.dobi.app.core.ui.theme.DobiPrimaryContainer
import com.dobi.app.core.ui.theme.DobiPrimaryLight
import com.dobi.app.core.ui.theme.DobiTextPrimary
import com.dobi.app.core.ui.theme.DobiTextSecondary
import com.dobi.app.core.ui.theme.SFProBold
import com.dobi.app.core.ui.theme.SFProSemiBold
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TuitionLearningScreen(
    onNavigateBack: () -> Unit,
    onSavedNavigateBack: () -> Unit,
    viewModel: TuitionLearningViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onSavedNavigateBack()
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

    // Active subject for photo capture — set when user taps Camera/Gallery for a subject
    var activeSubjectName by remember { mutableStateOf<String?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        val target = activeSubjectName
        if (target != null && uris.isNotEmpty()) {
            viewModel.onPhotosAdded(target, uris.map { it.toString() })
        }
        activeSubjectName = null
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val target = activeSubjectName
        if (success && target != null) {
            pendingCameraUri?.toString()?.let { viewModel.onPhotosAdded(target, listOf(it)) }
        }
        pendingCameraUri = null
        activeSubjectName = null
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = createCameraImageUri(context)
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            activeSubjectName = null
        }
    }

    fun launchCamera(subjectName: String) {
        activeSubjectName = subjectName
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

    fun launchGallery(subjectName: String) {
        activeSubjectName = subjectName
        galleryPicker.launch("image/*")
    }

    val title = uiState.kind.title
    val copy = remember(uiState.kind) { TuitionLearningCopy.forKind(uiState.kind) }
    val subtitle = copy.subtitle
    val saveLabel = uiState.kind.ctaLabel

    Scaffold(
        containerColor = DobiBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        title,
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
                .padding(horizontal = 20.dp, vertical = 4.dp)
        ) {
            Text(
                subtitle,
                fontSize = 13.sp,
                color = DobiTextSecondary
            )

            Spacer(Modifier.height(16.dp))

            // Read-only tutor strip
            TutorReadOnlyStrip(
                tutorDisplay = uiState.tutorDisplay
            )

            Spacer(Modifier.height(16.dp))

            // Date row
            FieldLabel(text = "Date")
            Spacer(Modifier.height(8.dp))
            DateRow(displayDate = uiState.formattedDate, onClick = { showDatePicker = true })

            Spacer(Modifier.height(20.dp))

            // Subjects horizontal scroll
            FieldLabel(text = "Subjects covered today")
            Spacer(Modifier.height(4.dp))
            Text(
                "Tap any subject you studied today. Pick more than one if you covered many.",
                fontSize = 12.sp,
                color = DobiTextSecondary
            )
            Spacer(Modifier.height(12.dp))
            SubjectChipsRow(
                subjects = uiState.availableSubjects,
                selectedNames = uiState.entries.map { it.subject.name }.toSet(),
                onSubjectToggled = viewModel::onSubjectToggled
            )

            // Per-subject expandable cards
            if (uiState.entries.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                FieldLabel(text = "Topic + photos for each subject")
                Spacer(Modifier.height(4.dp))
                Text(
                    "Tell us the topic and add a few photos for each subject.",
                    fontSize = 12.sp,
                    color = DobiTextSecondary
                )
                Spacer(Modifier.height(12.dp))
                uiState.entries.forEach { entry ->
                    SubjectEntryCard(
                        entry = entry,
                        topicPlaceholder = copy.topicPlaceholder,
                        photoHint = copy.photoHint,
                        onTopicChange = { viewModel.onTopicChanged(entry.subject.name, it) },
                        onAddCamera = { launchCamera(entry.subject.name) },
                        onAddGallery = { launchGallery(entry.subject.name) },
                        onRemovePhoto = { uri ->
                            viewModel.onPhotoRemoved(entry.subject.name, uri)
                        },
                        onRemoveSubject = { viewModel.onSubjectToggled(entry.subject) }
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }

            Spacer(Modifier.height(28.dp))

            // Save button
            Button(
                onClick = viewModel::onSave,
                enabled = uiState.canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DobiPrimary,
                    disabledContainerColor = DobiPrimaryLight.copy(alpha = 0.4f)
                )
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Text(
                        saveLabel,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = SFProBold,
                        color = Color.White
                    )
                }
            }

            // Validation hint
            if (!uiState.canSave && uiState.entries.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                val firstEmptyTopic = uiState.entries.firstOrNull { it.topic.isBlank() }
                val firstEmptyPhotos = uiState.entries.firstOrNull { it.photoUris.isEmpty() }
                val hint = when {
                    firstEmptyTopic != null ->
                        "Add a topic for ${firstEmptyTopic.subject.name} to continue."
                    firstEmptyPhotos != null ->
                        "Add at least one photo for ${firstEmptyPhotos.subject.name}."
                    uiState.tutorName.isBlank() ->
                        "Set your tutor's name from the home screen first."
                    else -> null
                }
                if (hint != null) {
                    Text(
                        text = hint,
                        fontSize = 12.sp,
                        color = DobiTextSecondary
                    )
                }
            }

            Spacer(Modifier.height(28.dp))
        }
    }
}

// ── Components ────────────────────────────────────────────────────────────────

@Composable
private fun TutorReadOnlyStrip(tutorDisplay: String) {
    val display = tutorDisplay.ifBlank { "Add tutor's name from home screen" }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFEDE9FE))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("👨‍🏫", fontSize = 22.sp)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Tutor",
                fontSize = 11.sp,
                color = Color(0xFF6D28D9),
                fontWeight = SFProSemiBold
            )
            Text(
                display,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = SFProSemiBold,
                color = if (tutorDisplay.isNotBlank()) DobiTextPrimary else DobiTextSecondary
            )
            if (tutorDisplay.isNotBlank()) {
                Text(
                    "Edit on home screen",
                    fontSize = 11.sp,
                    color = DobiTextSecondary
                )
            }
        }
    }
}

@Composable
private fun DateRow(displayDate: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.CalendarToday,
            contentDescription = null,
            tint = DobiTextSecondary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            displayDate,
            style = MaterialTheme.typography.bodyMedium,
            color = DobiTextPrimary
        )
    }
}

@Composable
private fun SubjectChipsRow(
    subjects: List<TuitionSubject>,
    selectedNames: Set<String>,
    onSubjectToggled: (TuitionSubject) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        subjects.forEach { subject ->
            SubjectChip(
                subject = subject,
                selected = selectedNames.contains(subject.name),
                onClick = { onSubjectToggled(subject) }
            )
        }
    }
}

@Composable
private fun SubjectChip(
    subject: TuitionSubject,
    selected: Boolean,
    onClick: () -> Unit
) {
    val color = runCatching { Color(android.graphics.Color.parseColor(subject.color)) }
        .getOrDefault(DobiPrimary)

    val bg = if (selected) color else Color.White
    val fg = if (selected) Color.White else DobiTextPrimary

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(
                width = 1.dp,
                color = if (selected) color else DobiBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(subject.icon, fontSize = 18.sp)
        Spacer(Modifier.width(8.dp))
        Text(
            subject.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = SFProSemiBold,
            color = fg
        )
        if (selected) {
            Spacer(Modifier.width(6.dp))
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubjectEntryCard(
    entry: TuitionEntry,
    topicPlaceholder: String,
    photoHint: String,
    onTopicChange: (String) -> Unit,
    onAddCamera: () -> Unit,
    onAddGallery: () -> Unit,
    onRemovePhoto: (String) -> Unit,
    onRemoveSubject: () -> Unit
) {
    val color = runCatching { Color(android.graphics.Color.parseColor(entry.subject.color)) }
        .getOrDefault(DobiPrimary)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(1.dp, DobiBorder, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(entry.subject.icon, fontSize = 20.sp)
            }
            Spacer(Modifier.width(10.dp))
            Text(
                entry.subject.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = SFProBold,
                color = DobiTextPrimary
            )
            IconButton(onClick = onRemoveSubject, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove subject",
                    tint = DobiTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            "Topic *",
            fontSize = 12.sp,
            fontWeight = SFProSemiBold,
            color = DobiTextPrimary
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = entry.topic,
            onValueChange = onTopicChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    topicPlaceholder,
                    color = DobiTextSecondary
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = color,
                unfocusedBorderColor = DobiBorder,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedTextColor = DobiTextPrimary,
                unfocusedTextColor = DobiTextPrimary,
                cursorColor = color
            )
        )

        Spacer(Modifier.height(14.dp))

        Text(
            "Photos *",
            fontSize = 12.sp,
            fontWeight = SFProSemiBold,
            color = DobiTextPrimary
        )
        Spacer(Modifier.height(2.dp))
        Text(
            photoHint,
            fontSize = 11.sp,
            color = DobiTextSecondary
        )
        Spacer(Modifier.height(8.dp))

        if (entry.photoUris.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                entry.photoUris.forEach { uri ->
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DobiPrimaryContainer)
                    ) {
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
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
                                    indication = null,
                                    onClick = { onRemovePhoto(uri) }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove photo",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PhotoSourceButton(
                icon = Icons.Default.CameraAlt,
                label = "Camera",
                onClick = onAddCamera,
                color = color,
                modifier = Modifier.weight(1f)
            )
            PhotoSourceButton(
                icon = Icons.Default.AddPhotoAlternate,
                label = "Gallery",
                onClick = onAddGallery,
                color = color,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PhotoSourceButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.10f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = SFProSemiBold,
            color = color
        )
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
