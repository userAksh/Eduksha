package com.dobi.app.feature.homework

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.dobi.app.core.ui.theme.DobiBackground
import com.dobi.app.core.ui.theme.DobiPrimary
import com.dobi.app.core.ui.theme.DobiPrimaryContainer
import com.dobi.app.core.ui.theme.DobiTextPrimary
import com.dobi.app.core.ui.theme.DobiTextSecondary

private val subjectPresets = listOf(
    "Science", "Mathematics", "English", "Social Science", "Hindi", "Sanskrit"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeworkLogScreen(
    onNavigateBack: () -> Unit,
    viewModel: HomeworkLogViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onNavigateBack()
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

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        viewModel.onPhotosAdded(uris.map { it.toString() })
    }

    Scaffold(
        containerColor = DobiBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Log Homework",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
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

            // ── Subject ────────────────────────────────────────────────────────
            FieldLabel(text = "Subject")
            Spacer(Modifier.height(10.dp))

            // Quick-select chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                subjectPresets.forEach { preset ->
                    val selected = uiState.subject == preset
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.onSubjectChanged(if (selected) "" else preset) },
                        label = { Text(preset, fontSize = 13.sp) },
                        leadingIcon = if (selected) {
                            { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DobiPrimary,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White,
                            containerColor = Color.White,
                            labelColor = DobiTextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = Color(0xFFE2E8F0),
                            selectedBorderColor = DobiPrimary,
                            borderWidth = 1.dp,
                            selectedBorderWidth = 1.5.dp
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = uiState.subject,
                onValueChange = viewModel::onSubjectChanged,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Or type a custom subject…", color = DobiTextSecondary) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = dobiTextFieldColors()
            )

            Spacer(Modifier.height(24.dp))

            // ── Teacher name ───────────────────────────────────────────────────
            FieldLabel(text = "Teacher's Name")
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = uiState.teacherName,
                onValueChange = { input ->
                    if (input.all { it.isLetter() || it.isWhitespace() || it == '.' }) {
                        viewModel.onTeacherNameChanged(input)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. Mrs. Sharma", color = DobiTextSecondary) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = dobiTextFieldColors()
            )

            Spacer(Modifier.height(24.dp))

            // ── Date ───────────────────────────────────────────────────────────
            FieldLabel(text = "Date")
            Spacer(Modifier.height(10.dp))
            DateRow(
                displayDate = uiState.formattedDate,
                onClick = { showDatePicker = true }
            )

            Spacer(Modifier.height(24.dp))

            // ── Photos ─────────────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                FieldLabel(text = "Photos")
                Spacer(Modifier.width(8.dp))
                Text(
                    "optional",
                    fontSize = 11.sp,
                    color = DobiTextSecondary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Spacer(Modifier.height(10.dp))

            if (uiState.selectedPhotoUris.isNotEmpty()) {
                PhotoGrid(
                    uris = uiState.selectedPhotoUris,
                    onRemove = viewModel::onPhotoRemoved
                )
                Spacer(Modifier.height(12.dp))
            }

            AddPhotosButton(onClick = { photoPicker.launch("image/*") })

            Spacer(Modifier.height(32.dp))

            // ── Save ───────────────────────────────────────────────────────────
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
                } else {
                    Text(
                        "Save Homework",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Section label ──────────────────────────────────────────────────────────────

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = DobiTextPrimary
    )
}

// ── Date row ───────────────────────────────────────────────────────────────────

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
            fontWeight = FontWeight.Medium,
            color = DobiTextPrimary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "Change",
            style = MaterialTheme.typography.bodySmall,
            color = DobiPrimary,
            fontWeight = FontWeight.Medium
        )
    }
}

// ── Photo grid ─────────────────────────────────────────────────────────────────

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

// ── Add photos button ──────────────────────────────────────────────────────────

@Composable
private fun AddPhotosButton(onClick: () -> Unit) {
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
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.AddPhotoAlternate,
            contentDescription = null,
            tint = DobiPrimary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "Add from Gallery",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = DobiPrimary
        )
    }
}

// ── Field colours ──────────────────────────────────────────────────────────────

@Composable
private fun dobiTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = DobiPrimary,
    unfocusedBorderColor = Color(0xFFE2E8F0),
    focusedContainerColor   = Color.White,
    unfocusedContainerColor = Color.White,
    focusedTextColor     = DobiTextPrimary,
    unfocusedTextColor   = DobiTextPrimary,
    cursorColor          = DobiPrimary
)
