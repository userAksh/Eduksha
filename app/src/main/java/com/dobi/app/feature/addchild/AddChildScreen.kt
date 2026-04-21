package com.dobi.app.feature.addchild

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dobi.app.core.ui.theme.DobiPrimary
import com.dobi.app.core.ui.theme.DobiPrimaryLight
import com.dobi.app.core.ui.theme.DobiTextPrimary
import com.dobi.app.core.ui.theme.DobiTextSecondary

private val grades = (2..9).toList()
private val boards = listOf("CBSE", "ICSE")

private data class CardTheme(val bg: Color, val accent: Color, val emoji: String)

private val cardThemes = listOf(
    CardTheme(Color(0xFFDBEAFE), Color(0xFF1D4ED8), "👦"),
    CardTheme(Color(0xFFFCE7F3), Color(0xFFBE185D), "👧"),
    CardTheme(Color(0xFFD1FAE5), Color(0xFF047857), "🧒"),
    CardTheme(Color(0xFFFEF3C7), Color(0xFFB45309), "👦"),
    CardTheme(Color(0xFFEDE9FE), Color(0xFF6D28D9), "👧"),
)

private val FieldBorder = Color(0xFFCBD5E1)
private val ErrorRed = Color(0xFFDC2626)

@Composable
fun AddChildScreen(
    onNavigateToParentHome: () -> Unit,
    viewModel: AddChildViewModel = hiltViewModel()
) {
    val forms by viewModel.forms.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navToParentHome.collect { onNavigateToParentHome() }
    }

    val isSaving = uiState is AddChildViewModel.UiState.Saving
    val error = (uiState as? AddChildViewModel.UiState.Error)?.message
    val canSave = forms.any { it.name.isNotBlank() } && !isSaving

    Box(modifier = Modifier.fillMaxSize()) {
        // Blue header band
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .background(DobiPrimary)
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .systemBarsPadding()
                    .padding(top = 28.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🧒", fontSize = 26.sp)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Add your children",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "Fill in all profiles, then save at once",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Scrollable content card
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = Color(0xFFF0F4FF),
                shadowElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                        .padding(horizontal = 18.dp, vertical = 24.dp)
                ) {
                    forms.forEachIndexed { index, form ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + slideInVertically { it / 2 },
                            exit = fadeOut()
                        ) {
                            ChildFormCard(
                                form = form,
                                index = index,
                                showRemove = forms.size > 1,
                                theme = cardThemes[index % cardThemes.size],
                                onNameChange = { viewModel.updateName(form.id, it) },
                                onGradeChange = { viewModel.updateGrade(form.id, it) },
                                onBoardChange = { viewModel.updateBoard(form.id, it) },
                                onRemove = { viewModel.removeForm(form.id) }
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                    }

                    // Add another child card
                    if (forms.size < 5) {
                        AddAnotherCard(onClick = viewModel::addForm)
                        Spacer(Modifier.height(24.dp))
                    }

                    // Error
                    if (error != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFEE2E2))
                                .padding(14.dp)
                        ) {
                            Text("⚠️  $error", style = MaterialTheme.typography.bodySmall, color = ErrorRed)
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    // Save button
                    Button(
                        onClick = viewModel::saveAll,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = canSave,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DobiPrimary,
                            disabledContainerColor = DobiPrimaryLight.copy(alpha = 0.35f)
                        )
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(22.dp)
                            )
                        } else {
                            val count = forms.count { it.name.isNotBlank() }
                            Text(
                                text = if (count > 1) "Save $count Children & Continue" else "Save & Continue",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }

                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChildFormCard(
    form: ChildForm,
    index: Int,
    showRemove: Boolean,
    theme: CardTheme,
    onNameChange: (String) -> Unit,
    onGradeChange: (Int) -> Unit,
    onBoardChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    var gradeExpanded by remember { mutableStateOf(false) }
    var boardExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 3.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Card header strip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = theme.bg,
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(theme.accent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Child ${index + 1}  ${theme.emoji}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = theme.accent,
                        modifier = Modifier.weight(1f)
                    )
                    if (showRemove) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(theme.accent.copy(alpha = 0.12f))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onRemove
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = theme.accent,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            // Form fields
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Student Name",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = DobiTextPrimary
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = form.name,
                    onValueChange = onNameChange,
                    placeholder = { Text("e.g. Aarav Sharma", color = DobiTextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    colors = fieldColors(theme.accent)
                )

                Spacer(Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Class",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = DobiTextPrimary
                        )
                        Spacer(Modifier.height(6.dp))
                        ExposedDropdownMenuBox(
                            expanded = gradeExpanded,
                            onExpandedChange = { gradeExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = "Class ${form.grade}",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(gradeExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                shape = RoundedCornerShape(12.dp),
                                colors = fieldColors(theme.accent)
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
                        Text(
                            text = "Board",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = DobiTextPrimary
                        )
                        Spacer(Modifier.height(6.dp))
                        ExposedDropdownMenuBox(
                            expanded = boardExpanded,
                            onExpandedChange = { boardExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = form.board,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(boardExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                shape = RoundedCornerShape(12.dp),
                                colors = fieldColors(theme.accent)
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
            }
        }
    }
}

@Composable
private fun AddAnotherCard(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.5.dp,
                color = DobiPrimary.copy(alpha = 0.35f),
                shape = RoundedCornerShape(16.dp)
            )
            .background(Color.White)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(DobiPrimary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = DobiPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Add another child",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = DobiPrimary
            )
        }
    }
}

@Composable
private fun fieldColors(accentColor: Color) = OutlinedTextFieldDefaults.colors(
    focusedTextColor = DobiTextPrimary,
    unfocusedTextColor = DobiTextPrimary,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color(0xFFFAFBFF),
    focusedBorderColor = accentColor,
    unfocusedBorderColor = FieldBorder,
    cursorColor = accentColor
)
