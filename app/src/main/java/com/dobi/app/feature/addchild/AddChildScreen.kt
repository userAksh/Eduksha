package com.dobi.app.feature.addchild

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.dobi.app.core.ui.theme.DobiPrimaryContainer
import com.dobi.app.core.ui.theme.DobiPrimaryLight
import com.dobi.app.core.ui.theme.DobiSuccess
import com.dobi.app.core.ui.theme.DobiSuccessContainer
import com.dobi.app.core.ui.theme.DobiTextPrimary
import com.dobi.app.core.ui.theme.DobiTextSecondary

private val grades = (2..9).toList()
private val boards = listOf("CBSE", "ICSE")
private val FieldBorder = Color(0xFFCBD5E1)
private val ErrorRed = Color(0xFFDC2626)

@Composable
fun AddChildScreen(
    onNavigateToParentHome: () -> Unit,
    viewModel: AddChildViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val addedCount by viewModel.addedCount.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navToParentHome.collect { onNavigateToParentHome() }
    }

    AddChildContent(
        uiState = uiState,
        addedCount = addedCount,
        onAddChild = viewModel::addChild,
        onResetState = viewModel::resetState,
        onDone = viewModel::goToParentHome
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddChildContent(
    uiState: AddChildViewModel.UiState,
    addedCount: Int,
    onAddChild: (name: String, grade: Int, board: String) -> Unit,
    onResetState: () -> Unit,
    onDone: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedGrade by remember { mutableIntStateOf(5) }
    var selectedBoard by remember { mutableStateOf("CBSE") }
    var gradeExpanded by remember { mutableStateOf(false) }
    var boardExpanded by remember { mutableStateOf(false) }

    val isLoading = uiState is AddChildViewModel.UiState.Loading
    val isSuccess = uiState is AddChildViewModel.UiState.Success
    val error = (uiState as? AddChildViewModel.UiState.Error)?.message

    LaunchedEffect(isSuccess) {
        if (isSuccess) {
            name = ""
            selectedGrade = 5
            selectedBoard = "CBSE"
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(DobiPrimary)
        )

        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .systemBarsPadding()
                    .padding(top = 32.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🧒", fontSize = 28.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Add your child",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "You can add multiple children and switch between them",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                modifier = Modifier.fillMaxSize().imePadding(),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = Color(0xFFF5F7FF),
                shadowElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 28.dp)
                ) {
                    if (addedCount > 0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DobiSuccessContainer)
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = DobiSuccess,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$addedCount child${if (addedCount > 1) "ren" else ""} added successfully",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = DobiSuccess
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    FieldLabel("Student Name")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text("e.g. Aarav Sharma", color = DobiTextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        colors = textFieldColors()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            FieldLabel("Class")
                            Spacer(modifier = Modifier.height(8.dp))
                            ExposedDropdownMenuBox(
                                expanded = gradeExpanded,
                                onExpandedChange = { gradeExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = "Class $selectedGrade",
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(gradeExpanded) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = textFieldColors()
                                )
                                ExposedDropdownMenu(
                                    expanded = gradeExpanded,
                                    onDismissRequest = { gradeExpanded = false },
                                    containerColor = Color.White
                                ) {
                                    grades.forEach { grade ->
                                        DropdownMenuItem(
                                            text = { Text("Class $grade", color = DobiTextPrimary) },
                                            onClick = { selectedGrade = grade; gradeExpanded = false }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            FieldLabel("Board")
                            Spacer(modifier = Modifier.height(8.dp))
                            ExposedDropdownMenuBox(
                                expanded = boardExpanded,
                                onExpandedChange = { boardExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = selectedBoard,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(boardExpanded) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = textFieldColors()
                                )
                                ExposedDropdownMenu(
                                    expanded = boardExpanded,
                                    onDismissRequest = { boardExpanded = false },
                                    containerColor = Color.White
                                ) {
                                    boards.forEach { board ->
                                        DropdownMenuItem(
                                            text = { Text(board, color = DobiTextPrimary) },
                                            onClick = { selectedBoard = board; boardExpanded = false }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (error != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFFEE2E2))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text("⚠️  $error", style = MaterialTheme.typography.bodySmall, color = ErrorRed)
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Button(
                        onClick = { onAddChild(name.trim(), selectedGrade, selectedBoard); onResetState() },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = name.isNotBlank() && !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DobiPrimary,
                            disabledContainerColor = DobiPrimaryLight.copy(alpha = 0.4f)
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                        } else {
                            Text(
                                "Add Child",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }

                    if (addedCount > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = onDone,
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = DobiPrimary),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, DobiPrimary)
                        ) {
                            Text(
                                "Done — Go to Home",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = DobiTextPrimary
    )
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = DobiTextPrimary,
    unfocusedTextColor = DobiTextPrimary,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    focusedBorderColor = DobiPrimary,
    unfocusedBorderColor = FieldBorder,
    cursorColor = DobiPrimary
)
