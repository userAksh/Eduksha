package com.dobi.app.feature.classtestprep

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dobi.app.core.ui.theme.DobiBackground
import com.dobi.app.core.ui.theme.DobiPrimary
import com.dobi.app.core.ui.theme.DobiPrimaryContainer
import com.dobi.app.core.ui.theme.DobiTextPrimary
import com.dobi.app.core.ui.theme.DobiTextSecondary
import com.dobi.app.core.ui.theme.DobiWarning
import com.dobi.app.core.ui.theme.DobiWarningContainer
import com.dobi.app.core.ui.theme.SFProBold
import com.dobi.app.core.ui.theme.SFProMedium
import com.dobi.app.core.ui.theme.SFProSemiBold
import kotlinx.coroutines.delay

// ── Entry point ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassTestPrepScreen(
    onNavigateBack: () -> Unit,
    onLaunchMcqQuiz: (key: String, displayName: String) -> Unit,
    onLaunchWriteAnswers: () -> Unit,
    onLaunchAiPracticeUpload: () -> Unit,
    viewModel: ClassTestPrepViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navEvents.collect { event ->
            when (event) {
                is TestPrepNavEvent.StartMcqQuiz ->
                    onLaunchMcqQuiz(event.key, event.displayName)
                TestPrepNavEvent.StartAiPracticeUpload ->
                    onLaunchAiPracticeUpload()
                TestPrepNavEvent.StartWriteAnswers ->
                    onLaunchWriteAnswers()
            }
        }
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

    Scaffold(
        containerColor = DobiBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Class Test Prep",
                        style = MaterialTheme.typography.titleMedium,
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
        when {
            uiState.isLoading -> Box(
                Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = DobiPrimary) }

            else -> Box(Modifier.fillMaxSize()) {
                Content(
                    state = uiState,
                    contentPadding = innerPadding,
                    onSubjectSelected = viewModel::onSubjectSelected,
                    onTeacherHonorificChanged = viewModel::onTeacherHonorificChanged,
                    onTeacherNameChanged = viewModel::onTeacherNameChanged,
                    onDateClick = { showDatePicker = true },
                    onItemToggled = viewModel::onItemToggled,
                    onStartPractise = viewModel::onStartPractise
                )

                LimitInfoSnack(
                    visible = uiState.showLimitInfo,
                    message = uiState.limitMessage
                        ?: "You can pick at most 2 items. Deselect one to add another.",
                    onDismiss = viewModel::dismissLimitInfo,
                    bottomPadding = innerPadding.calculateBottomPadding() + 80.dp
                )

                ModePopup(
                    visible = uiState.showModePopup,
                    selectionCount = uiState.selectedItems.size,
                    onDismiss = viewModel::onModePopupDismissed,
                    onMcq = viewModel::onPractiseMcq,
                    onSubjective = {
                        viewModel.onPractiseSubjective()
                    },
                    onAi = {
                        viewModel.onPractiseAi()
                    }
                )
            }
        }
    }
}

@Composable
private fun Content(
    state: ClassTestPrepUiState,
    contentPadding: PaddingValues,
    onSubjectSelected: (String) -> Unit,
    onTeacherHonorificChanged: (String) -> Unit,
    onTeacherNameChanged: (String) -> Unit,
    onDateClick: () -> Unit,
    onItemToggled: (TestPrepSelection) -> Unit,
    onStartPractise: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // Hero
            Text(
                text = "Let's Prepare 🚀",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = SFProBold,
                color = DobiTextPrimary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Tell us what your test is about and we'll set you up.",
                style = MaterialTheme.typography.bodyMedium,
                color = DobiTextSecondary,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(22.dp))

            FieldLabel("Subject")
            Spacer(Modifier.height(10.dp))
            SubjectChipsRow(
                subjects = state.subjects,
                selected = state.selectedSubject,
                onSelected = onSubjectSelected
            )

            Spacer(Modifier.height(22.dp))

            FieldLabel("Teacher's Name")
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Sir", "Madam").forEach { honorific ->
                    HonorificChip(
                        label = honorific,
                        selected = state.teacherHonorific == honorific,
                        onClick = { onTeacherHonorificChanged(honorific) }
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = state.teacherName,
                onValueChange = { input ->
                    if (input.all { it.isLetter() || it.isWhitespace() || it == '.' }) {
                        onTeacherNameChanged(input)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. Sharma", color = DobiTextSecondary) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = dobiTextFieldColors()
            )

            Spacer(Modifier.height(22.dp))

            FieldLabel("Test Date")
            Spacer(Modifier.height(10.dp))
            DateRow(displayDate = state.formattedDate, onClick = onDateClick)

            Spacer(Modifier.height(28.dp))

            // Selectable list
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FieldLabel("What's on the test?")
                Text(
                    text = "${state.selectedItems.size}/${ClassTestPrepUiState.MAX_SELECTIONS} picked",
                    fontSize = 12.sp,
                    color = DobiPrimary,
                    fontWeight = SFProSemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(DobiPrimaryContainer)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Pick up to 2 lessons or concepts you want to revise.",
                style = MaterialTheme.typography.bodySmall,
                color = DobiTextSecondary
            )

            Spacer(Modifier.height(12.dp))

            UnitConceptList(
                units = state.units,
                selectedIds = state.selectedItems.map { it.id }.toSet(),
                onToggle = { selection -> onItemToggled(selection) },
                subjectName = state.selectedSubject.orEmpty()
            )

            Spacer(Modifier.height(20.dp))
        }

        // Sticky CTA
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DobiBackground)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Button(
                onClick = onStartPractise,
                enabled = state.canStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DobiPrimary,
                    disabledContainerColor = Color(0xFFBFD7F7)
                )
            ) {
                Text(
                    "Start Practice",
                    color = Color.White,
                    fontWeight = SFProSemiBold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

// ── Field label ────────────────────────────────────────────────────────────────

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = SFProSemiBold,
        color = DobiTextPrimary
    )
}

// ── Subject chips ──────────────────────────────────────────────────────────────

@Composable
private fun SubjectChipsRow(
    subjects: List<TestPrepSubject>,
    selected: String?,
    onSelected: (String) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(subjects.size) { i ->
            val s = subjects[i]
            SubjectChip(
                subject = s,
                selected = s.name == selected,
                onClick = { onSelected(s.name) }
            )
        }
    }
}

@Composable
private fun SubjectChip(
    subject: TestPrepSubject,
    selected: Boolean,
    onClick: () -> Unit
) {
    val accent = parseHex(subject.color, fallback = DobiPrimary)
    val bg by animateColorAsState(
        targetValue = if (selected) accent else Color.White,
        label = "sbg"
    )
    val border by animateColorAsState(
        targetValue = if (selected) accent else Color(0xFFE5EBF5),
        label = "sbd"
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) Color.White else DobiTextPrimary,
        label = "sft"
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
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(subject.icon, fontSize = 16.sp)
        Spacer(Modifier.width(8.dp))
        Text(
            subject.name,
            color = labelColor,
            fontWeight = SFProSemiBold,
            fontSize = 13.sp
        )
    }
}

// ── Honorific chip ─────────────────────────────────────────────────────────────

@Composable
private fun HonorificChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(
        targetValue = if (selected) DobiPrimary else Color.White, label = "hbg"
    )
    val border by animateColorAsState(
        targetValue = if (selected) DobiPrimary else Color(0xFFE2E8F0), label = "hbd"
    )
    val content by animateColorAsState(
        targetValue = if (selected) Color.White else DobiTextSecondary, label = "hft"
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
        Text(label, fontSize = 13.sp, fontWeight = SFProSemiBold, color = content)
    }
}

// ── Date row ───────────────────────────────────────────────────────────────────

@Composable
private fun DateRow(displayDate: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 16.dp),
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
            "Change",
            style = MaterialTheme.typography.bodySmall,
            color = DobiPrimary,
            fontWeight = SFProMedium
        )
    }
}

// ── Unit + concept list ────────────────────────────────────────────────────────

@Composable
private fun UnitConceptList(
    units: List<TestPrepUnit>,
    selectedIds: Set<String>,
    onToggle: (TestPrepSelection) -> Unit,
    subjectName: String
) {
    if (units.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFE5EBF5), RoundedCornerShape(14.dp))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Pick a subject above to see lessons.",
                style = MaterialTheme.typography.bodyMedium,
                color = DobiTextSecondary
            )
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        units.forEach { unit ->
            UnitGroupCard(
                unit = unit,
                selectedIds = selectedIds,
                onToggle = onToggle,
                subjectName = subjectName
            )
        }
    }
}

@Composable
private fun UnitGroupCard(
    unit: TestPrepUnit,
    selectedIds: Set<String>,
    onToggle: (TestPrepSelection) -> Unit,
    subjectName: String
) {
    val lessonSelected = unit.unitId in selectedIds
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE5EBF5), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        SelectableRow(
            id = unit.unitId,
            title = unit.unitName,
            subtitle = "Whole lesson — ${unit.concepts.size} concepts",
            selected = lessonSelected,
            isLesson = true,
            onClick = {
                onToggle(
                    TestPrepSelection(
                        id = unit.unitId,
                        title = unit.unitName,
                        isLesson = true,
                        subject = subjectName,
                        unitId = unit.unitId,
                        unitName = unit.unitName
                    )
                )
            }
        )
        if (unit.concepts.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            unit.concepts.forEach { concept ->
                SelectableRow(
                    id = concept.id,
                    title = concept.name,
                    subtitle = if (lessonSelected) "Included with the lesson" else null,
                    selected = lessonSelected || concept.id in selectedIds,
                    isLesson = false,
                    inset = true,
                    onClick = {
                        onToggle(
                            TestPrepSelection(
                                id = concept.id,
                                title = concept.name,
                                isLesson = false,
                                subject = subjectName,
                                unitId = unit.unitId,
                                unitName = unit.unitName
                            )
                        )
                    }
                )
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun SelectableRow(
    id: String,
    title: String,
    subtitle: String?,
    selected: Boolean,
    isLesson: Boolean,
    onClick: () -> Unit,
    inset: Boolean = false
) {
    val border by animateColorAsState(
        targetValue = if (selected) DobiPrimary else Color(0xFFEEF2F7),
        label = "rbd"
    )
    val bg by animateColorAsState(
        targetValue = if (selected) DobiPrimaryContainer else Color(0xFFFAFBFD),
        label = "rbg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (inset) 8.dp else 0.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.5.dp, border, RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SelectionBox(selected = selected)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isLesson) SFProSemiBold else SFProMedium,
                color = DobiTextPrimary
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = DobiTextSecondary,
                    fontSize = 11.sp
                )
            }
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(
                    if (isLesson) DobiPrimaryContainer
                    else Color(0xFFF1F5F9)
                )
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                if (isLesson) "Lesson" else "Concept",
                fontSize = 10.sp,
                color = if (isLesson) DobiPrimary else DobiTextSecondary,
                fontWeight = SFProSemiBold
            )
        }
    }
}

@Composable
private fun SelectionBox(selected: Boolean) {
    val border by animateColorAsState(
        targetValue = if (selected) DobiPrimary else Color(0xFFCBD5E1),
        label = "sbox-bd"
    )
    val bg by animateColorAsState(
        targetValue = if (selected) DobiPrimary else Color.White,
        label = "sbox-bg"
    )
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(bg)
            .border(1.5.dp, border, RoundedCornerShape(7.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

// ── Limit info snack ───────────────────────────────────────────────────────────

@Composable
private fun LimitInfoSnack(
    visible: Boolean,
    message: String,
    onDismiss: () -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp
) {
    LaunchedEffect(visible, message) {
        if (visible) {
            delay(2800)
            onDismiss()
        }
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.9f),
        exit = fadeOut() + scaleOut(targetScale = 0.95f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = bottomPadding, start = 24.dp, end = 24.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(DobiWarningContainer)
                    .border(1.dp, DobiWarning, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("⚠️", fontSize = 16.sp)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = message,
                    color = DobiTextPrimary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = SFProMedium
                )
            }
        }
    }
}

// ── Mode popup ─────────────────────────────────────────────────────────────────

@Composable
private fun ModePopup(
    visible: Boolean,
    selectionCount: Int,
    onDismiss: () -> Unit,
    onMcq: () -> Unit,
    onSubjective: () -> Unit,
    onAi: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(220)),
        exit = fadeOut(tween(180))
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = scaleIn(
                initialScale = 0.45f,
                transformOrigin = TransformOrigin(0.5f, 0.78f),
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(tween(160)),
            exit = scaleOut(
                targetScale = 0.55f,
                transformOrigin = TransformOrigin(0.5f, 0.78f),
                animationSpec = tween(160)
            ) + fadeOut(tween(140))
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 20.dp)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(DobiPrimaryContainer),
                            contentAlignment = Alignment.Center
                        ) { Text("🎯", fontSize = 22.sp) }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "How would you like to practise?",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = SFProBold,
                                color = DobiTextPrimary
                            )
                            Text(
                                "$selectionCount item" +
                                    (if (selectionCount == 1) "" else "s") +
                                    " ready",
                                fontSize = 12.sp,
                                color = DobiTextSecondary
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp)
                    ) {
                        items(3) { idx ->
                            when (idx) {
                                0 -> ModeCard(
                                    emoji = "📝",
                                    title = "Quick MCQs",
                                    subtitle = "Multiple-choice questions",
                                    accent = DobiPrimary,
                                    onClick = onMcq
                                )
                                1 -> ModeCard(
                                    emoji = "✍️",
                                    title = "Write Answers",
                                    subtitle = "Short & long answers",
                                    accent = Color(0xFF7C3AED),
                                    onClick = onSubjective
                                )
                                else -> ModeCard(
                                    emoji = "✨",
                                    title = "AI Practice",
                                    subtitle = "Fresh questions by Dobi",
                                    accent = Color(0xFFD946EF),
                                    onClick = onAi
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        "Swipe to see all 3 options →",
                        fontSize = 11.sp,
                        color = DobiTextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeCard(
    emoji: String,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(118.dp)
            .height(146.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFFAFBFD))
            .border(1.5.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) { Text(emoji, fontSize = 18.sp) }

        Spacer(Modifier.height(8.dp))

        Text(
            title,
            fontSize = 13.sp,
            fontWeight = SFProBold,
            color = DobiTextPrimary,
            lineHeight = 16.sp
        )
        Spacer(Modifier.height(2.dp))
        Text(
            subtitle,
            fontSize = 10.sp,
            color = DobiTextSecondary,
            lineHeight = 13.sp,
            maxLines = 2
        )

        Spacer(Modifier.weight(1f))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Start",
                fontSize = 11.sp,
                fontWeight = SFProSemiBold,
                color = accent
            )
            Spacer(Modifier.width(5.dp))
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
    }
}

// ── Helpers ────────────────────────────────────────────────────────────────────

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

private fun parseHex(hex: String, fallback: Color): Color = runCatching {
    Color(android.graphics.Color.parseColor(hex))
}.getOrDefault(fallback)
