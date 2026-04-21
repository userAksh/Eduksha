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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import com.dobi.app.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dobi.app.domain.model.Child
import com.dobi.app.core.ui.theme.DobiPrimary
import com.dobi.app.core.ui.theme.SFProBold
import com.dobi.app.core.ui.theme.SFProSemiBold
import com.dobi.app.core.ui.theme.SFProMedium
import com.dobi.app.core.ui.theme.SFProRegular
import com.dobi.app.core.ui.theme.SFProLight
import com.dobi.app.core.ui.theme.DobiPrimaryContainer
import com.dobi.app.core.ui.theme.DobiPrimaryLight
import com.dobi.app.core.ui.theme.DobiPrimarySoft
import com.dobi.app.core.ui.theme.DobiTextPrimary
import com.dobi.app.core.ui.theme.DobiTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.dobi.app.core.ui.theme.DobiBackground
import com.dobi.app.core.ui.theme.DobiBorder
import com.dobi.app.core.ui.theme.DobiSurface
import com.dobi.app.core.ui.theme.DobiSurfaceAlt
import com.dobi.app.core.ui.theme.DobiTextTertiary
import com.dobi.app.core.ui.theme.DobiAccentOrange
import com.dobi.app.core.ui.theme.DobiAccentOrangeBg
import com.dobi.app.core.ui.theme.DobiSuccess
import com.dobi.app.core.ui.theme.DobiSuccessContainer
import com.dobi.app.core.ui.theme.DobiError
import com.dobi.app.core.ui.theme.DobiErrorContainer

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
    val editingChildId by viewModel.editingChildId.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navToParentHome.collect { onNavigateToParentHome() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DobiBackground)
            .systemBarsPadding()
    ) {
        // Top app bar
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
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = SFProBold),
                color = DobiTextPrimary,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.size(48.dp))
        }

        // Stepper — stays fixed when moving between Details and Done
        AnimatedVisibility(
            visible = step != AddChildStep.WELCOME,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200))
        ) {
            StepperBar(
                currentStep = when (step) {
                    AddChildStep.DONE -> 2
                    else -> 1
                }
            )
        }

        // Step body — animated swap
        Crossfade(
            targetState = step,
            animationSpec = tween(250),
            label = "add-child-step",
            modifier = Modifier.weight(1f)
        ) { currentStep ->
            when (currentStep) {
                AddChildStep.WELCOME -> WelcomeStep(onStart = viewModel::startAdding)
                AddChildStep.DETAILS -> DetailsStep(
                    draft = draft,
                    isSaving = isSaving,
                    error = error,
                    isEditing = editingChildId != null,
                    onNameChange = viewModel::updateName,
                    onGenderChange = viewModel::updateGender,
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

        Box(
            modifier = Modifier
                .size(220.dp)
                .clip(CircleShape)
                .background(DobiPrimaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_parents_child),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(Modifier.height(14.dp))

        Text(
            text = "Add Your Child",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = SFProBold),
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

        Spacer(Modifier.height(18.dp))

        FeatureRow(
            emoji = "📊",
            bg = DobiPrimaryContainer,
            title = "Track progress",
            subtitle = "See what your child is learning every day."
        )
        Spacer(Modifier.height(14.dp))
        FeatureRow(
            emoji = "🏅",
            bg = Color(0xFFFCE7F3),
            title = "Spot what matters",
            subtitle = "Identify strengths and topics that need attention."
        )
        Spacer(Modifier.height(14.dp))
        FeatureRow(
            emoji = "🛡️",
            bg = DobiSuccessContainer,
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
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = SFProBold),
                color = Color.White
            )
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DobiPrimarySoft)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🔒", fontSize = 18.sp)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    "Your child's data is safe and secure",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = SFProSemiBold),
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
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(bg),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 30.sp)
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = SFProSemiBold,
                lineHeight = 22.sp,
                color = DobiTextPrimary
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                fontWeight = SFProRegular,
                lineHeight = 20.sp,
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
    isSaving: Boolean,
    error: String?,
    isEditing: Boolean,
    onNameChange: (String) -> Unit,
    onGenderChange: (Gender) -> Unit,
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
            .background(DobiBackground)
            .verticalScroll(rememberScrollState())
            .imePadding()
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(20.dp))

            Text(
                "Child Details",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = SFProBold),
                color = DobiTextPrimary
            )
            Spacer(Modifier.height(4.dp))
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
                        .border(2.dp, DobiBorder, CircleShape)
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
                                if (selected) DobiPrimary else DobiBorder,
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
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = SFProSemiBold),
                                color = if (selected) Color.White else DobiTextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
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
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
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
                enabled = draft.name.isNotBlank() && !isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DobiPrimary,
                    disabledContainerColor = DobiPrimaryLight.copy(alpha = 0.35f)
                )
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                } else {
                    Text(
                        if (isEditing) "Update Child" else "Save Child",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = SFProBold),
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

// ── Step 2 — Done ─────────────────────────────────────────────────────────────

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
            .background(DobiBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            Text(
                "Added Children",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = SFProBold),
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
                        .border(1.dp, DobiBorder, RoundedCornerShape(16.dp))
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
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = SFProBold),
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
                                .background(DobiSuccessContainer)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "Primary",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = SFProBold),
                                color = DobiSuccess
                            )
                        }
                    } else {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = DobiBorder,
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
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = SFProSemiBold),
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
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = SFProBold),
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
    val steps = listOf("Details", "Done")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        steps.forEachIndexed { index, label ->
            val stepNum = index + 1
            val isDone = stepNum < currentStep
            val isActive = stepNum == currentStep

            val circleBg by animateColorAsState(
                targetValue = if (isDone || isActive) DobiPrimary else DobiPrimaryContainer,
                animationSpec = tween(300),
                label = "circleBg"
            )
            val numberColor by animateColorAsState(
                targetValue = if (isActive || isDone) Color.White else DobiPrimary,
                animationSpec = tween(300),
                label = "numberColor"
            )
            val labelColor by animateColorAsState(
                targetValue = if (isActive) DobiPrimary else DobiTextTertiary,
                animationSpec = tween(300),
                label = "labelColor"
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(circleBg),
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
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = SFProBold),
                            color = numberColor
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (isActive) SFProSemiBold else SFProRegular),
                    color = labelColor
                )
            }

            if (index < steps.size - 1) {
                val lineColor by animateColorAsState(
                    targetValue = if (currentStep > stepNum) DobiPrimary else DobiPrimaryContainer,
                    animationSpec = tween(400),
                    label = "lineColor"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 15.dp)
                        .height(2.dp)
                        .background(lineColor)
                )
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = SFProSemiBold),
        color = DobiTextPrimary
    )
}

@Composable
private fun ErrorBox(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DobiErrorContainer)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text("⚠️  $message", style = MaterialTheme.typography.bodySmall, color = DobiError)
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = DobiTextPrimary,
    unfocusedTextColor = DobiTextPrimary,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    focusedBorderColor = DobiPrimary,
    unfocusedBorderColor = DobiBorder,
    cursorColor = DobiPrimary
)
