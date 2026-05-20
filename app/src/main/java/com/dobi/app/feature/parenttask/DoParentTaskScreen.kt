package com.dobi.app.feature.parenttask

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
fun DoParentTaskScreen(
    onNavigateBack: () -> Unit = {},
    onLaunchQuiz: (cacheKey: String, displayName: String) -> Unit = { _, _ -> },
    onFinished: () -> Unit = {},
    onRequestNextTask: () -> Unit = {},
    viewModel: DoParentTaskViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.finished) {
        if (uiState.finished) onFinished()
    }
    LaunchedEffect(uiState.launchQuizCacheKey) {
        val key = uiState.launchQuizCacheKey ?: return@LaunchedEffect
        viewModel.onQuizLaunchConsumed()
        onLaunchQuiz(key, uiState.task?.displayName.orEmpty())
    }

    Scaffold(
        containerColor = DobiBackground,
        topBar = { TopBar(onNavigateBack) }
    ) { padding ->
        when {
            uiState.isLoading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = DobiPrimary) }
            uiState.task == null -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.error ?: "Task not found",
                    color = MaterialTheme.colorScheme.error
                )
            }
            else -> {
                val task = uiState.task!!
                when {
                    task.status == ParentTaskStatus.REQUESTED || task.type == null ->
                        StatusBody(
                            task = task,
                            emoji = "⏳",
                            title = "Waiting for your parent",
                            subtitle = "Your parent will pick a task and send it back. We'll let you know.",
                            modifier = Modifier.padding(padding)
                        )
                    task.status == ParentTaskStatus.PENDING_REVIEW ->
                        StatusBody(
                            task = task,
                            emoji = "📬",
                            title = "Submitted!",
                            subtitle = "Your parent will review this soon. Hang tight.",
                            modifier = Modifier.padding(padding)
                        )
                    task.status == ParentTaskStatus.ACCEPTED ->
                        StatusBody(
                            task = task,
                            emoji = "🎉",
                            title = "Great job!",
                            subtitle = task.parentReviewNote.ifBlank {
                                "Your parent accepted your task."
                            },
                            modifier = Modifier.padding(padding)
                        )
                    task.type == ParentTaskType.OUR_QUESTIONS -> QuizBody(
                        task = task,
                        isPreparingQuiz = uiState.isPreparingQuiz,
                        error = uiState.error,
                        onStartQuiz = viewModel::startQuiz,
                        modifier = Modifier.padding(padding)
                    )
                    else -> UploadBody(
                        state = uiState,
                        onPhotosAdded = viewModel::onPhotosAdded,
                        onPhotoRemoved = viewModel::onPhotoRemoved,
                        onSubmit = viewModel::submitPhotos,
                        modifier = Modifier.padding(padding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(onNavigateBack: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = "Your Task",
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
            Text(text = task.subjectIcon.ifBlank { "📚" }, fontSize = 22.sp)
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

// ── Quiz body (OUR_QUESTIONS) ──────────────────────────────────────────────────

@Composable
private fun QuizBody(
    task: ParentTask,
    isPreparingQuiz: Boolean,
    error: String?,
    onStartQuiz: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Your parent picked this task",
            color = DobiTextSecondary,
            fontSize = 12.sp
        )
        TaskHeader(task)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFFFF7E6))
                .border(1.dp, Color(0xFFFEEAB6), RoundedCornerShape(14.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🎯", fontSize = 22.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Answer ${task.mcqCount} questions",
                        color = DobiTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = SFProBold
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Your score will be sent to your parent for review.",
                    color = DobiTextSecondary,
                    fontSize = 12.sp
                )
            }
        }
        error?.let {
            Text(text = it, color = Color(0xFFB91C1C), fontSize = 12.sp)
        }
        PrimaryActionButton(
            label = "Start quiz",
            isLoading = isPreparingQuiz,
            enabled = !isPreparingQuiz,
            onClick = onStartQuiz
        )
    }
}

// ── Upload body (REVISE / WRITE_QA / CUSTOM) ───────────────────────────────────

@Composable
private fun UploadBody(
    state: DoParentTaskUiState,
    onPhotosAdded: (List<String>) -> Unit,
    onPhotoRemoved: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val task = state.task ?: return
    val galleryPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris -> onPhotosAdded(uris.map { it.toString() }) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Your parent picked this task",
            color = DobiTextSecondary,
            fontSize = 12.sp
        )
        TaskHeader(task)

        InstructionCard(task)

        if (task.assignedSubjectiveQs.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Questions from your parent",
                    color = DobiTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                task.assignedSubjectiveQs.forEachIndexed { index, q ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "${index + 1}. ${q.text}",
                            color = DobiTextPrimary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                        )
                    }
                }
            }
        }

        if (task.customNote.isNotBlank()) {
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
                        text = "Note from your parent",
                        color = DobiTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(text = task.customNote, color = DobiTextPrimary, fontSize = 14.sp)
                }
            }
        }

        Text(
            text = "Add photos",
            color = DobiTextPrimary,
            fontSize = 14.sp,
            fontWeight = SFProBold,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
        )

        if (state.selectedPhotoUris.isEmpty()) {
            AddPhotoCta(onClick = { galleryPicker.launch("image/*") })
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.selectedPhotoUris) { uri ->
                    Box {
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            modifier = Modifier
                                .size(110.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFE5EBF5))
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                                .clickable { onPhotoRemoved(uri) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
                item {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEFF6FF))
                            .border(
                                1.dp, Color(0xFFCFE0FF), RoundedCornerShape(12.dp)
                            )
                            .clickable { galleryPicker.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add more",
                            tint = DobiPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }

        state.error?.let {
            Text(text = it, color = Color(0xFFB91C1C), fontSize = 12.sp)
        }

        PrimaryActionButton(
            label = "Submit to parent",
            isLoading = state.isSubmitting,
            enabled = state.canSubmitPhotos,
            onClick = onSubmit
        )
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun InstructionCard(task: ParentTask) {
    val msg = when (task.type) {
        ParentTaskType.REVISE_CHAPTER ->
            "Revise the lesson, then take photos of your revision notes and upload them."
        ParentTaskType.WRITE_QA ->
            if (task.assignedSubjectiveQs.isNotEmpty())
                "Answer these questions on paper, then upload a photo of your answers."
            else
                "Write important Q&A from your textbook, then upload a photo."
        ParentTaskType.CUSTOM ->
            "Read your parent's note above. When done, upload a photo as proof."
        else -> ""
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFFFF7E6))
            .border(1.dp, Color(0xFFFEEAB6), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Text(text = msg, color = DobiTextPrimary, fontSize = 13.sp)
    }
}

@Composable
private fun AddPhotoCta(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFEFF6FF))
            .border(1.5.dp, Color(0xFFCFE0FF), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = DobiPrimary,
                modifier = Modifier.size(34.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Tap to add photos",
                color = DobiPrimary,
                fontSize = 13.sp,
                fontWeight = SFProSemiBold
            )
        }
    }
}

@Composable
private fun StatusBody(
    task: ParentTask,
    emoji: String,
    title: String,
    subtitle: String,
    primaryActionLabel: String? = null,
    onPrimaryAction: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        TaskHeader(task)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFEFF6FF))
                .border(1.dp, Color(0xFFCFE0FF), RoundedCornerShape(16.dp))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = emoji, fontSize = 36.sp)
                Spacer(Modifier.height(10.dp))
                Text(
                    text = title,
                    color = DobiTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = SFProBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    color = DobiTextSecondary,
                    fontSize = 13.sp
                )
            }
        }
        if (primaryActionLabel != null) {
            PrimaryActionButton(
                label = primaryActionLabel,
                isLoading = false,
                enabled = true,
                onClick = onPrimaryAction
            )
        }
    }
}

@Composable
private fun PrimaryActionButton(
    label: String,
    isLoading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
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
