package com.dobi.app.feature.addchild

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dobi.app.domain.model.Child
import com.dobi.app.core.ui.theme.DobiPrimary
import com.dobi.app.core.ui.theme.DobiPrimaryContainer
import com.dobi.app.core.ui.theme.DobiPrimaryLight
import com.dobi.app.core.ui.theme.DobiTextPrimary
import com.dobi.app.core.ui.theme.DobiTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val FieldBorder = Color(0xFFCBD5E1)
private val PageBg = Color(0xFFF5F7FF)

@Composable
fun AddChildScreen(
    onNavigateToParentHome: () -> Unit,
    onNavigateBack: () -> Unit = {},
    viewModel: AddChildViewModel = hiltViewModel()
) {
    val step by viewModel.step.collectAsStateWithLifecycle()
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val addedChildren by viewModel.addedChildren.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navToParentHome.collect { onNavigateToParentHome() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .systemBarsPadding()
    ) {
        // Top app bar
        if (step != AddChildStep.DONE) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (step == AddChildStep.WELCOME) onNavigateBack()
                    else viewModel.goBack()
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = DobiTextPrimary
                    )
                }
                Text(
                    text = "Add Child",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = DobiTextPrimary,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.size(48.dp))
            }
        }

        when (step) {
            AddChildStep.WELCOME -> WelcomeStep(onStart = viewModel::startAdding)
            AddChildStep.DETAILS -> DetailsStep(
                draft = draft,
                error = error,
                onNameChange = viewModel::updateName,
                onDobChange = viewModel::updateDob,
                onGenderChange = viewModel::updateGender,
                onContinue = viewModel::continueToClassBoard
            )
            AddChildStep.CLASS_BOARD -> ClassBoardStep(
                draft = draft,
                isSaving = isSaving,
                error = error,
                onGradeChange = viewModel::updateGrade,
                onBoardChange = viewModel::updateBoard,
                onSave = viewModel::saveChild
            )
            AddChildStep.DONE -> DoneStep(
                addedChildren = addedChildren,
                onAddAnother = viewModel::addAnotherChild,
                onFinish = viewModel::finish
            )
        }
    }
}

// ── Welcome ───────────────────────────────────────────────────────────────────

@Composable
private fun WelcomeStep(onStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))

        // Illustration
        Box(
            modifier = Modifier
                .size(220.dp)
                .clip(CircleShape)
                .background(Color(0xFFEEF4FF)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("👨‍👩‍👦", fontSize = 72.sp)
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Add Your Child",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = DobiPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Add your child to start tracking\ntheir learning journey.",
            style = MaterialTheme.typography.bodyMedium,
            color = DobiTextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(28.dp))

        FeatureRow(
            emoji = "📊",
            bg = Color(0xFFEEF4FF),
            title = "Track progress",
            subtitle = "See what your child is learning every day."
        )
        Spacer(Modifier.height(12.dp))
        FeatureRow(
            emoji = "🏆",
            bg = Color(0xFFFFF7ED),
            title = "Spot what matters",
            subtitle = "Identify strengths and topics that need attention."
        )
        Spacer(Modifier.height(12.dp))
        FeatureRow(
            emoji = "✅",
            bg = Color(0xFFECFDF5),
            title = "Encourage better",
            subtitle = "Motivate your child with goals and rewards."
        )

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DobiPrimary)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text(
                "Add Your First Child",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF1F5F9))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🔒", fontSize = 18.sp)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    "Your child's data is safe and secure",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = DobiTextPrimary
                )
                Text(
                    "We never share your information.",
                    style = MaterialTheme.typography.bodySmall,
                    color = DobiTextSecondary
                )
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun FeatureRow(emoji: String, bg: Color, title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE8EEF8), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(bg),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 22.sp)
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = DobiTextPrimary
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = DobiTextSecondary
            )
        }
    }
}

// ── Step 1 — Details ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailsStep(
    draft: ChildDraft,
    error: String?,
    onNameChange: (String) -> Unit,
    onDobChange: (String) -> Unit,
    onGenderChange: (Gender) -> Unit,
    onContinue: () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    datePickerState.selectedDateMillis?.let { millis ->
                        val fmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        onDobChange(fmt.format(Date(millis)))
                    }
                }) { Text("OK", color = DobiPrimary) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = DobiTextSecondary)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBg)
            .verticalScroll(rememberScrollState())
            .imePadding()
    ) {
        StepperBar(currentStep = 1)

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(20.dp))

            Text(
                "Child Details",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = DobiTextPrimary
            )
            Text(
                "Tell us about your child.",
                style = MaterialTheme.typography.bodyMedium,
                color = DobiTextSecondary
            )

            Spacer(Modifier.height(24.dp))

            // Avatar
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color(0xFFCBD5E1), CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = draft.gender.emoji, fontSize = 52.sp)
                }
            }

            Spacer(Modifier.height(24.dp))

            FieldLabel("Child's Name")
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = draft.name,
                onValueChange = onNameChange,
                placeholder = { Text("Enter child's name", color = DobiTextSecondary) },
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = null, tint = DobiTextSecondary)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                colors = fieldColors()
            )

            Spacer(Modifier.height(18.dp))

            FieldLabel("Date of Birth")
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = draft.dateOfBirth,
                onValueChange = {},
                placeholder = { Text("Select date of birth", color = DobiTextSecondary) },
                leadingIcon = {
                    Icon(Icons.Default.DateRange, contentDescription = null, tint = DobiTextSecondary)
                },
                trailingIcon = {
                    Icon(Icons.Default.DateRange, contentDescription = null, tint = DobiPrimary)
                },
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showDatePicker = true },
                shape = RoundedCornerShape(14.dp),
                colors = fieldColors()
            )

            Spacer(Modifier.height(18.dp))

            FieldLabel("Gender")
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Gender.entries.forEach { gender ->
                    val selected = draft.gender == gender
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) DobiPrimary else Color.White)
                            .border(
                                1.5.dp,
                                if (selected) DobiPrimary else FieldBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onGenderChange(gender) },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(gender.emoji, fontSize = 16.sp)
                            Spacer(Modifier.width(5.dp))
                            Text(
                                gender.display,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = if (selected) Color.White else DobiTextSecondary
                            )
                        }
                    }
                }
            }

            if (error != null) {
                Spacer(Modifier.height(12.dp))
                ErrorBox(error)
            }

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = draft.name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DobiPrimary,
                    disabledContainerColor = DobiPrimaryLight.copy(alpha = 0.35f)
                )
            ) {
                Text(
                    "Continue",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Step 2 — Class & Board ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClassBoardStep(
    draft: ChildDraft,
    isSaving: Boolean,
    error: String?,
    onGradeChange: (Int) -> Unit,
    onBoardChange: (String) -> Unit,
    onSave: () -> Unit
) {
    var gradeExpanded by remember { mutableStateOf(false) }
    var boardExpanded by remember { mutableStateOf(false) }
    val grades = (2..9).toList()
    val boards = listOf("CBSE", "ICSE")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBg)
            .verticalScroll(rememberScrollState())
            .imePadding()
    ) {
        StepperBar(currentStep = 2)

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(20.dp))

            Text(
                "Class & Board",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = DobiTextPrimary
            )
            Text(
                "Select your child's class and board.",
                style = MaterialTheme.typography.bodyMedium,
                color = DobiTextSecondary
            )

            Spacer(Modifier.height(24.dp))

            // Preview card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DobiPrimaryContainer)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text(draft.gender.emoji, fontSize = 28.sp)
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        draft.name.ifBlank { "Your child" },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = DobiPrimary
                    )
                    if (draft.dateOfBirth.isNotBlank()) {
                        Text(
                            "DOB: ${draft.dateOfBirth}",
                            style = MaterialTheme.typography.bodySmall,
                            color = DobiTextSecondary
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            FieldLabel("Class")
            Spacer(Modifier.height(8.dp))
            ExposedDropdownMenuBox(expanded = gradeExpanded, onExpandedChange = { gradeExpanded = it }) {
                OutlinedTextField(
                    value = "Class ${draft.grade}",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(gradeExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors()
                )
                ExposedDropdownMenu(
                    expanded = gradeExpanded,
                    onDismissRequest = { gradeExpanded = false },
                    containerColor = Color.White
                ) {
                    grades.forEach { grade ->
                        DropdownMenuItem(
                            text = { Text("Class $grade", color = DobiTextPrimary) },
                            onClick = { onGradeChange(grade); gradeExpanded = false }
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            FieldLabel("Board")
            Spacer(Modifier.height(8.dp))
            ExposedDropdownMenuBox(expanded = boardExpanded, onExpandedChange = { boardExpanded = it }) {
                OutlinedTextField(
                    value = draft.board,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(boardExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors()
                )
                ExposedDropdownMenu(
                    expanded = boardExpanded,
                    onDismissRequest = { boardExpanded = false },
                    containerColor = Color.White
                ) {
                    boards.forEach { board ->
                        DropdownMenuItem(
                            text = { Text(board, color = DobiTextPrimary) },
                            onClick = { onBoardChange(board); boardExpanded = false }
                        )
                    }
                }
            }

            if (error != null) {
                Spacer(Modifier.height(12.dp))
                ErrorBox(error)
            }

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = DobiPrimary)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                } else {
                    Text(
                        "Save Child",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Step 3 — Done ─────────────────────────────────────────────────────────────

private val childCardColors = listOf(
    Pair(Color(0xFFDBEAFE), Color(0xFF1D4ED8)),
    Pair(Color(0xFFFCE7F3), Color(0xFFBE185D)),
    Pair(Color(0xFFD1FAE5), Color(0xFF047857)),
    Pair(Color(0xFFFEF3C7), Color(0xFFB45309)),
    Pair(Color(0xFFEDE9FE), Color(0xFF6D28D9)),
)

private fun genderEmoji(gender: String) = when (gender) {
    "girl" -> "👧"
    "other" -> "🧒"
    else -> "👦"
}

@Composable
private fun DoneStep(
    addedChildren: List<Child>,
    onAddAnother: () -> Unit,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBg)
    ) {
        StepperBar(currentStep = 3)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            Text(
                "Added Children",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = DobiTextPrimary
            )
            Text(
                "You can manage and switch between profiles anytime.",
                style = MaterialTheme.typography.bodyMedium,
                color = DobiTextSecondary
            )

            Spacer(Modifier.height(20.dp))

            addedChildren.forEachIndexed { index, child ->
                val (bg, accent) = childCardColors[index % childCardColors.size]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE8EEF8), RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(bg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(genderEmoji(child.gender), fontSize = 28.sp)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            child.name,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = DobiTextPrimary
                        )
                        Text(
                            "Class ${child.grade} · ${child.board}",
                            style = MaterialTheme.typography.bodySmall,
                            color = DobiTextSecondary
                        )
                    }
                    if (index == 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFDCFCE7))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "Primary",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF15803D)
                            )
                        }
                    } else {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(8.dp))

            // Add another
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.5.dp, DobiPrimary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onAddAnother
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, DobiPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = DobiPrimary, modifier = Modifier.size(14.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Add Another Child",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = DobiPrimary
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onFinish,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DobiPrimary)
            ) {
                Text(
                    "Continue to Home",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Shared components ─────────────────────────────────────────────────────────

@Composable
private fun StepperBar(currentStep: Int) {
    val steps = listOf("Details", "Class & Board", "Done")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, label ->
            val stepNum = index + 1
            val isDone = stepNum < currentStep
            val isActive = stepNum == currentStep

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isDone || isActive -> DobiPrimary
                                else -> Color(0xFFE8EEF8)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDone) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Text(
                            "$stepNum",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isActive) Color.White else Color(0xFF94A3B8)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal),
                    color = if (isActive) DobiPrimary else Color(0xFF94A3B8)
                )
            }

            if (index < steps.size - 1) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .padding(bottom = 16.dp)
                        .background(if (currentStep > stepNum) DobiPrimary else Color(0xFFE8EEF8))
                )
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = DobiTextPrimary
    )
}

@Composable
private fun ErrorBox(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFFEE2E2))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text("⚠️  $message", style = MaterialTheme.typography.bodySmall, color = Color(0xFFDC2626))
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = DobiTextPrimary,
    unfocusedTextColor = DobiTextPrimary,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    focusedBorderColor = DobiPrimary,
    unfocusedBorderColor = FieldBorder,
    cursorColor = DobiPrimary
)
