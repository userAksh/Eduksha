package com.dobi.app.feature.addchild

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dobi.app.core.ui.theme.DobiBackground
import com.dobi.app.core.ui.theme.DobiPrimary
import com.dobi.app.core.ui.theme.DobiSuccess
import com.dobi.app.core.ui.theme.DobiSuccessContainer
import com.dobi.app.core.ui.theme.DobiTextPrimary
import com.dobi.app.core.ui.theme.DobiTextSecondary

private val grades = (2..9).map { it }
private val boards = listOf("CBSE", "ICSE")

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DobiBackground)
            .systemBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Add your child",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = DobiTextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "You can add multiple children and switch between them",
            style = MaterialTheme.typography.bodyMedium,
            color = DobiTextSecondary
        )

        if (addedCount > 0) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "✓ $addedCount child${if (addedCount > 1) "ren" else ""} added",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = DobiSuccess,
                modifier = Modifier
                    .background(DobiSuccessContainer, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(text = "Student Name", style = MaterialTheme.typography.labelLarge, color = DobiTextPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = { Text("e.g. Aarav Sharma", color = DobiTextSecondary) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DobiPrimary,
                unfocusedBorderColor = Color(0xFFE2E8F0)
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Class", style = MaterialTheme.typography.labelLarge, color = DobiTextPrimary)
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DobiPrimary,
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = gradeExpanded,
                        onDismissRequest = { gradeExpanded = false }
                    ) {
                        grades.forEach { grade ->
                            DropdownMenuItem(
                                text = { Text("Class $grade") },
                                onClick = { selectedGrade = grade; gradeExpanded = false }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Board", style = MaterialTheme.typography.labelLarge, color = DobiTextPrimary)
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DobiPrimary,
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = boardExpanded,
                        onDismissRequest = { boardExpanded = false }
                    ) {
                        boards.forEach { board ->
                            DropdownMenuItem(
                                text = { Text(board) },
                                onClick = { selectedBoard = board; boardExpanded = false }
                            )
                        }
                    }
                }
            }
        }

        if (error != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = error, style = MaterialTheme.typography.bodySmall, color = Color(0xFFEF4444))
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { onAddChild(name.trim(), selectedGrade, selectedBoard); onResetState() },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            enabled = name.isNotBlank() && !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = DobiPrimary)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.height(20.dp))
            } else {
                Text("Add Child", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
            }
        }

        if (addedCount > 0) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DobiPrimary)
            ) {
                Text("Done — Go to Home", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold))
            }
        }
    }
}
