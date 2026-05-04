package com.dobi.app.feature.parenttask

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.dobi.app.core.ui.theme.DobiBackground
import com.dobi.app.core.ui.theme.DobiPrimary
import com.dobi.app.core.ui.theme.DobiTextPrimary
import com.dobi.app.core.ui.theme.DobiTextSecondary
import com.dobi.app.core.ui.theme.SFProBold
import com.dobi.app.core.ui.theme.SFProSemiBold
import com.dobi.app.domain.model.ParentTask
import com.dobi.app.domain.model.ParentTaskStatus
import com.dobi.app.domain.model.ParentTaskType

@Composable
fun ParentTaskDetailScreen(
    onNavigateBack: () -> Unit = {},
    onFinished: () -> Unit = {},
    viewModel: ParentTaskDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.finished) {
        if (uiState.finished) onFinished()
    }

    Scaffold(
        containerColor = DobiBackground,
        topBar = { DetailTopBar(onNavigateBack = onNavigateBack) }
    ) { padding ->
        when {
            uiState.isLoading -> Loading(Modifier.padding(padding))
            uiState.task == null -> ErrorState(
                Modifier.padding(padding),
                uiState.error ?: "Task not found"
            )
            else -> when (uiState.task!!.status) {
                ParentTaskStatus.REQUESTED -> AssignBody(
                    state = uiState,
                    onTypeSelected = viewModel::onTypeSelected,
                    onCustomNoteChanged = viewModel::onCustomNoteChanged,
                    onMcqCountChanged = viewModel::onMcqCountChanged,
                    onAssign = viewModel::assignTask,
                    modifier = Modifier.padding(padding)
                )
                ParentTaskStatus.PENDING_REVIEW -> ReviewBody(
                    state = uiState,
                    onReviewNoteChanged = viewModel::onReviewNoteChanged,
                    onAccept = viewModel::acceptSubmission,
                    onReject = viewModel::rejectSubmission,
                    modifier = Modifier.padding(padding)
                )
                else -> AlreadyHandledBody(
                    task = uiState.task!!,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailTopBar(onNavigateBack: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = "Task from your child",
                style = MaterialTheme.typography.titleLarge,
                color = DobiTextPrimary
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = DobiTextPrimary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = DobiBackground)
    )
}

@Composable
private fun TaskHeader(task: ParentTask) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE5EBF5), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFF4E0)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = task.subjectIcon.ifBlank { "📚" },
                fontSize = 22.sp
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.displayName,
                color = DobiTextPrimary,
                fontSize = 15.sp,
                fontWeight = SFProBold
            )
            Text(
                text = buildString {
                    append(if (task.isUnitScope) "Lesson" else "Concept")
                    if (task.subject.isNotBlank()) {
                        append(" · ")
                        append(task.subject)
                    }
                },
                color = DobiTextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

// ── Assign body ────────────────────────────────────────────────────────────────

@Composable
private fun AssignBody(
    state: ParentTaskDetailUiState,
    onTypeSelected: (ParentTaskType) -> Unit,
    onCustomNoteChanged: (String) -> Unit,
    onMcqCountChanged: (Int) -> Unit,
    onAssign: () -> Unit,
    modifier: Modifier = Modifier
) {
    val task = state.task ?: return
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Your child wants to study this",
            color = DobiTextSecondary,
            fontSize = 12.sp
        )
        TaskHeader(task)

        Text(
            text = "Pick a task",
            color = DobiTextPrimary,
            fontSize = 14.sp,
            fontWeight = SFProBold,
            modifier = Modifier.padding(top = 8.dp, start = 4.dp)
        )

        TaskTypeOption(
            emoji = "📖",
            title = "Revise the chapter",
            subtitle = "Child uploads photos of revision notes.",
            isSelected = state.selectedType == ParentTaskType.REVISE_CHAPTER,
            onClick = { onTypeSelected(ParentTaskType.REVISE_CHAPTER) }
        )
        TaskTypeOption(
            emoji = "✍️",
            title = "Write Q&A from textbook",
            subtitle = "Child writes questions + answers and uploads the photo.",
            isSelected = state.selectedType == ParentTaskType.WRITE_QA,
            onClick = { onTypeSelected(ParentTaskType.WRITE_QA) }
        )
        TaskTypeOption(
            emoji = "🎯",
            title = "Our set of questions",
            subtitle = "${state.mcqCount} AI-generated MCQs on this " +
                "${if (task.isUnitScope) "lesson" else "concept"}.",
            isSelected = state.selectedType == ParentTaskType.OUR_QUESTIONS,
            onClick = { onTypeSelected(ParentTaskType.OUR_QUESTIONS) }
        ) {
            // Inline MCQ count toggle. Default = 10 for lesson scope, 5 for concept.
            McqCountSelector(
                count = state.mcqCount,
                onChange = onMcqCountChanged
            )
        }
        TaskTypeOption(
            emoji = "📝",
            title = "Custom task",
            subtitle = "Type any task — child uploads a photo as proof.",
            isSelected = state.selectedType == ParentTaskType.CUSTOM,
            onClick = { onTypeSelected(ParentTaskType.CUSTOM) }
        ) {
            TextField(
                value = state.customNote,
                onValueChange = onCustomNoteChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                placeholder = {
                    Text(
                        text = "What do you want them to do?",
                        color = DobiTextSecondary,
                        fontSize = 13.sp
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF8FAFF),
                    unfocusedContainerColor = Color(0xFFF8FAFF),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = DobiTextPrimary,
                    unfocusedTextColor = DobiTextPrimary,
                    cursorColor = DobiPrimary
                ),
                shape = RoundedCornerShape(10.dp),
                maxLines = 4
            )
        }

        Spacer(Modifier.height(8.dp))

        state.error?.let { msg ->
            Text(
                text = msg,
                color = Color(0xFFB91C1C),
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        PrimaryActionButton(
            label = "Give this task",
            isLoading = state.isSubmitting,
            enabled = state.selectedType != null && !state.isSubmitting &&
                (state.selectedType != ParentTaskType.CUSTOM || state.customNote.isNotBlank()),
            onClick = onAssign
        )
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun TaskTypeOption(
    emoji: String,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    extra: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) Color(0xFFEFF6FF) else Color.White)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) DobiPrimary else Color(0xFFE5EBF5),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = emoji, fontSize = 22.sp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = DobiTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = SFProBold
                )
                Text(
                    text = subtitle,
                    color = DobiTextSecondary,
                    fontSize = 12.sp
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = DobiPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        if (isSelected && extra != null) extra()
    }
}

@Composable
private fun McqCountSelector(count: Int, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.padding(top = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Number of questions",
            color = DobiTextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f)
        )
        StepperButton(
            icon = Icons.Default.Remove,
            enabled = count > 5,
            onClick = { if (count > 5) onChange(5) }
        )
        Text(
            text = count.toString(),
            color = DobiTextPrimary,
            fontSize = 14.sp,
            fontWeight = SFProBold,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        StepperButton(
            icon = Icons.Default.Add,
            enabled = count < 10,
            onClick = { if (count < 10) onChange(10) }
        )
    }
}

@Composable
private fun StepperButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (enabled) DobiPrimary else Color(0xFFCBD5E1))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp)
        )
    }
}

// ── Review body ────────────────────────────────────────────────────────────────

@Composable
private fun ReviewBody(
    state: ParentTaskDetailUiState,
    onReviewNoteChanged: (String) -> Unit,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier
) {
    val task = state.task ?: return
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Your child submitted this task",
            color = DobiTextSecondary,
            fontSize = 12.sp
        )
        TaskHeader(task)

        if (task.type == ParentTaskType.OUR_QUESTIONS && task.submissionTotal != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFEFF6FF))
                    .border(1.dp, Color(0xFFCFE0FF), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        text = "Quiz score",
                        color = DobiTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${task.submissionScore ?: 0} / ${task.submissionTotal} correct",
                        color = DobiTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = SFProBold
                    )
                }
            }
        }

        if (task.submissionPhotoUrls.isNotEmpty()) {
            Text(
                text = "Photos uploaded",
                color = DobiTextPrimary,
                fontSize = 13.sp,
                fontWeight = SFProBold,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(task.submissionPhotoUrls) { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        modifier = Modifier
                            .size(140.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE5EBF5))
                    )
                }
            }
        }

        Text(
            text = "Note for your child (optional for accept, required for redo)",
            color = DobiTextPrimary,
            fontSize = 13.sp,
            fontWeight = SFProBold,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
        )
        TextField(
            value = state.reviewNote,
            onValueChange = onReviewNoteChanged,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = "Add a quick comment…",
                    color = DobiTextSecondary,
                    fontSize = 13.sp
                )
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFF8FAFF),
                unfocusedContainerColor = Color(0xFFF8FAFF),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = DobiTextPrimary,
                unfocusedTextColor = DobiTextPrimary,
                cursorColor = DobiPrimary
            ),
            shape = RoundedCornerShape(10.dp),
            maxLines = 4
        )

        state.error?.let { msg ->
            Text(text = msg, color = Color(0xFFB91C1C), fontSize = 12.sp)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SecondaryActionButton(
                label = "Ask to redo",
                icon = Icons.Default.Close,
                isLoading = state.isSubmitting,
                onClick = onReject,
                modifier = Modifier.weight(1f)
            )
            PrimaryActionButton(
                label = "Accept",
                isLoading = state.isSubmitting,
                enabled = !state.isSubmitting,
                onClick = onAccept,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun AlreadyHandledBody(task: ParentTask, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        TaskHeader(task)
        val msg = when (task.status) {
            ParentTaskStatus.ASSIGNED -> "Task assigned. Waiting for your child to submit."
            ParentTaskStatus.ACCEPTED -> "You've already accepted this submission."
            ParentTaskStatus.REJECTED -> "You've asked your child to redo this."
            else -> ""
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFF8FAFF))
                .padding(16.dp)
        ) {
            Text(text = msg, color = DobiTextSecondary, fontSize = 13.sp)
        }
    }
}

// ── Shared bits ────────────────────────────────────────────────────────────────

@Composable
private fun PrimaryActionButton(
    label: String,
    isLoading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) DobiPrimary else Color(0xFFCBD5E1))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.dp,
                modifier = Modifier.size(18.dp)
            )
        } else {
            Text(
                text = label,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SecondaryActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.5.dp, Color(0xFFCBD5E1), RoundedCornerShape(14.dp))
            .clickable(enabled = !isLoading, onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = DobiTextPrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                color = DobiTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun Loading(modifier: Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = DobiPrimary)
    }
}

@Composable
private fun ErrorState(modifier: Modifier, msg: String) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = msg, color = MaterialTheme.colorScheme.error)
    }
}
