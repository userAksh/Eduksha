package com.dobi.app.feature.studygoals

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dobi.app.domain.model.McqQuestion
import com.dobi.app.domain.model.ParentTaskType
import com.dobi.app.domain.model.SubjectiveQuestion

private val ScreenBg = Color(0xFFFFF8EE)
private val CardBg = Color.White
private val CardBorder = Color(0xFFEDEAE0)
private val PrimaryBlue = Color(0xFF3B7BFF)
private val PrimaryText = Color(0xFF16213E)
private val SecondaryText = Color(0xFF6B7895)
private val SoftBlueBg = Color(0xFFEEF5FF)
private val AmberSoft = Color(0xFFFFEFD9)
private val AmberDeep = Color(0xFFB45309)
private val SuccessGreen = Color(0xFF16A34A)
private val SuccessSoft = Color(0xFFE6F7EC)

@Composable
fun StudyGoalsScreen(
    onNavigateBack: () -> Unit,
    viewModel: StudyGoalsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as android.app.Activity).window
            window.statusBarColor = ScreenBg.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    BackHandler {
        when {
            state.step == StudyGoalsStep.SUBJECT || state.blockingTask != null -> onNavigateBack()
            state.step == StudyGoalsStep.SUBMITTING -> { /* swallow during submit */ }
            state.step == StudyGoalsStep.DONE -> onNavigateBack()
            else -> viewModel.back()
        }
    }

    LaunchedEffect(state.step) {
        if (state.step == StudyGoalsStep.DONE) {
            // Auto-pop after a beat so the parent sees the success message.
            kotlinx.coroutines.delay(900)
            onNavigateBack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
            .statusBarsPadding(),
    ) {
        TopBar(
            title = topBarTitle(state),
            onBack = {
                if (state.step == StudyGoalsStep.SUBJECT || state.blockingTask != null) onNavigateBack()
                else viewModel.back()
            },
        )

        when {
            state.isLoading -> LoadingBlock()
            state.blockingTask != null -> BlockingBlock(
                childName = state.childName,
                task = state.blockingTask!!,
                onSendReminder = viewModel::sendReminder,
                onClose = onNavigateBack,
            )
            state.step == StudyGoalsStep.SUBJECT -> SubjectStep(state, viewModel::selectSubject)
            state.step == StudyGoalsStep.TOPIC -> TopicStep(state, viewModel::selectTopic)
            state.step == StudyGoalsStep.TASK_TYPE -> TaskTypeStep(state, viewModel::selectType)
            state.step == StudyGoalsStep.PREVIEW -> PreviewStep(
                state = state,
                onRegenerate = {
                    if (state.selectedType == ParentTaskType.OUR_QUESTIONS) viewModel.regenerateMcqs()
                    else if (state.selectedType == ParentTaskType.WRITE_QA) viewModel.regenerateSubjective()
                },
                onCustomModeSelect = viewModel::selectCustomMode,
                onCustomNoteChange = viewModel::setCustomNote,
                onAssign = viewModel::assign,
            )
            state.step == StudyGoalsStep.SUBMITTING -> LoadingBlock(message = "Assigning…")
            state.step == StudyGoalsStep.DONE -> DoneBlock(state.childName)
        }
    }
}

private fun topBarTitle(state: StudyGoalsUiState): String = when {
    state.blockingTask != null -> "Study Goals"
    state.step == StudyGoalsStep.SUBJECT -> "Pick a subject"
    state.step == StudyGoalsStep.TOPIC -> state.selectedSubject?.name ?: "Pick a topic"
    state.step == StudyGoalsStep.TASK_TYPE -> "How to practise?"
    state.step == StudyGoalsStep.PREVIEW -> when (state.selectedType) {
        ParentTaskType.OUR_QUESTIONS -> "Review questions"
        ParentTaskType.WRITE_QA -> "Review questions"
        ParentTaskType.CUSTOM, ParentTaskType.REVISE_CHAPTER -> "Custom task"
        null -> "Review"
    }
    else -> "Study Goals"
}

@Composable
private fun TopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = PrimaryText,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(4.dp))
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryText,
            maxLines = 1,
        )
    }
}

@Composable
private fun LoadingBlock(message: String = "Loading…") {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = PrimaryBlue, strokeWidth = 3.dp)
            Spacer(Modifier.height(12.dp))
            Text(message, fontSize = 13.sp, color = SecondaryText)
        }
    }
}

@Composable
private fun BlockingBlock(
    childName: String,
    task: com.dobi.app.domain.model.ParentTask,
    onSendReminder: () -> Unit,
    onClose: () -> Unit,
) {
    val isAssigned = task.status == com.dobi.app.domain.model.ParentTaskStatus.ASSIGNED
    val twentyFourH = 24L * 60L * 60L * 1000L
    val now = System.currentTimeMillis()
    val canRemind = isAssigned &&
        task.assignedAtMillis > 0 &&
        now - task.assignedAtMillis >= twentyFourH &&
        (task.reminderSentAtMillis == 0L || now - task.reminderSentAtMillis >= twentyFourH)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = AmberSoft,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "$childName already has a task to finish",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = AmberDeep,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "You can assign a new task once the current one is completed and accepted.",
                    fontSize = 13.sp,
                    color = PrimaryText,
                    lineHeight = 18.sp,
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CardBg,
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = task.displayName.ifBlank { task.subject.ifBlank { "Current task" } },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Status: ${statusLabel(task.status)}",
                    fontSize = 12.sp,
                    color = SecondaryText,
                )
            }
        }

        if (canRemind) {
            Button(
                onClick = onSendReminder,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
            ) {
                Text("Send reminder", fontWeight = FontWeight.Bold)
            }
        } else if (isAssigned) {
            Text(
                text = "Reminder will unlock 24 hours after you assigned the task.",
                fontSize = 12.sp,
                color = SecondaryText,
            )
        }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth().height(46.dp),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text("Close", color = PrimaryText)
        }
    }
}

private fun statusLabel(s: com.dobi.app.domain.model.ParentTaskStatus): String = when (s) {
    com.dobi.app.domain.model.ParentTaskStatus.REQUESTED -> "Waiting for you to assign"
    com.dobi.app.domain.model.ParentTaskStatus.ASSIGNED -> "Working on it"
    com.dobi.app.domain.model.ParentTaskStatus.PENDING_REVIEW -> "Submitted — needs your review"
    com.dobi.app.domain.model.ParentTaskStatus.ACCEPTED -> "Completed"
    com.dobi.app.domain.model.ParentTaskStatus.REJECTED -> "Redo in progress"
}

@Composable
private fun SubjectStep(state: StudyGoalsUiState, onPick: (SubjectChoice) -> Unit) {
    if (state.subjects.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
            Text(
                "No subjects available for ${state.childName}'s class yet.",
                color = SecondaryText,
                fontSize = 14.sp,
            )
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(state.subjects, key = { it.name }) { subject ->
            PickerRow(
                icon = subject.icon.ifBlank { "📚" },
                title = subject.name,
                subtitle = "Pick a chapter inside",
                onClick = { onPick(subject) },
            )
        }
    }
}

@Composable
private fun TopicStep(state: StudyGoalsUiState, onPick: (TopicChoice) -> Unit) {
    val subjectName = state.selectedSubject?.name.orEmpty()
    val topics = state.topicsBySubject[subjectName].orEmpty()
    if (topics.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
            Text("No chapters listed for $subjectName.", color = SecondaryText, fontSize = 14.sp)
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(topics, key = { it.unitId }) { topic ->
            PickerRow(
                icon = topic.subjectIcon.ifBlank { "📖" },
                title = topic.unitName,
                subtitle = topic.subject,
                onClick = { onPick(topic) },
            )
        }
    }
}

@Composable
private fun TaskTypeStep(state: StudyGoalsUiState, onPick: (ParentTaskType) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = state.selectedTopic?.unitName.orEmpty(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryText,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        item {
            TypeCard(
                emoji = "✏️",
                title = "Quick quiz",
                subtitle = "5 AI-generated MCQs. You'll review them before sending.",
                onClick = { onPick(ParentTaskType.OUR_QUESTIONS) },
            )
        }
        item {
            TypeCard(
                emoji = "📝",
                title = "Subjective questions",
                subtitle = "5 short-answer questions to write & upload.",
                onClick = { onPick(ParentTaskType.WRITE_QA) },
            )
        }
        item {
            TypeCard(
                emoji = "📚",
                title = "Custom",
                subtitle = "Assign the whole chapter, or type your own instruction.",
                onClick = { onPick(ParentTaskType.CUSTOM) },
            )
        }
    }
}

@Composable
private fun PreviewStep(
    state: StudyGoalsUiState,
    onRegenerate: () -> Unit,
    onCustomModeSelect: (CustomMode) -> Unit,
    onCustomNoteChange: (String) -> Unit,
    onAssign: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            when (state.selectedType) {
                ParentTaskType.OUR_QUESTIONS -> McqPreviewList(
                    questions = state.previewMcqs,
                    loading = state.generatingPreview,
                    error = state.previewError,
                )
                ParentTaskType.WRITE_QA -> SubjectivePreviewList(
                    questions = state.previewSubjectiveQs,
                    loading = state.generatingPreview,
                    error = state.previewError,
                )
                ParentTaskType.CUSTOM, ParentTaskType.REVISE_CHAPTER -> CustomPreview(
                    selected = state.customMode,
                    note = state.customNote,
                    onModeSelect = onCustomModeSelect,
                    onNoteChange = onCustomNoteChange,
                )
                null -> {}
            }
        }
        BottomBar(
            state = state,
            onRegenerate = onRegenerate,
            onAssign = onAssign,
        )
    }
}

@Composable
private fun McqPreviewList(questions: List<McqQuestion>, loading: Boolean, error: String) {
    if (loading) { LoadingBlock(message = "Generating questions…"); return }
    if (error.isNotBlank() && questions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
            Text(error, color = SecondaryText, fontSize = 14.sp)
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(questions.size) { index ->
            val q = questions[index]
            QuestionCard(
                number = index + 1,
                title = q.text,
                body = {
                    q.options.forEachIndexed { i, opt ->
                        val isCorrect = i == q.correctIndex
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 3.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(if (isCorrect) SuccessSoft else Color(0xFFF3F4F6)),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isCorrect) Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = opt,
                                fontSize = 13.sp,
                                color = if (isCorrect) SuccessGreen else PrimaryText,
                                fontWeight = if (isCorrect) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun SubjectivePreviewList(questions: List<SubjectiveQuestion>, loading: Boolean, error: String) {
    if (loading) { LoadingBlock(message = "Generating questions…"); return }
    if (error.isNotBlank() && questions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
            Text(error, color = SecondaryText, fontSize = 14.sp)
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(questions.size) { index ->
            val q = questions[index]
            QuestionCard(
                number = index + 1,
                title = q.text,
                body = {
                    if (q.expectedKeywords.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Look for: ${q.expectedKeywords.joinToString(", ")}",
                            fontSize = 11.sp,
                            color = SecondaryText,
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun CustomPreview(
    selected: CustomMode,
    note: String,
    onModeSelect: (CustomMode) -> Unit,
    onNoteChange: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            CustomChoiceCard(
                selected = selected == CustomMode.WHOLE_CHAPTER,
                emoji = "📖",
                title = "Assign the whole chapter",
                subtitle = "Child revises the chapter and uploads photos of their work.",
                onClick = { onModeSelect(CustomMode.WHOLE_CHAPTER) },
            )
        }
        item {
            CustomChoiceCard(
                selected = selected == CustomMode.TYPED,
                emoji = "✍️",
                title = "Type my own task",
                subtitle = "Write a short instruction for your child.",
                onClick = { onModeSelect(CustomMode.TYPED) },
            )
        }
        if (selected == CustomMode.TYPED) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CardBg,
                    border = BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    BasicTextField(
                        value = note,
                        onValueChange = onNoteChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 96.dp)
                            .padding(14.dp),
                        textStyle = TextStyle(color = PrimaryText, fontSize = 14.sp),
                        decorationBox = { inner ->
                            if (note.isEmpty()) {
                                Text(
                                    "e.g. Read the lesson and tell me 3 examples in your own words.",
                                    color = SecondaryText,
                                    fontSize = 13.sp,
                                )
                            }
                            inner()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomBar(
    state: StudyGoalsUiState,
    onRegenerate: () -> Unit,
    onAssign: () -> Unit,
) {
    val type = state.selectedType
    val canAssign = when (type) {
        ParentTaskType.OUR_QUESTIONS -> state.previewMcqs.isNotEmpty() && !state.generatingPreview
        ParentTaskType.WRITE_QA -> state.previewSubjectiveQs.isNotEmpty() && !state.generatingPreview
        ParentTaskType.CUSTOM -> state.customNote.trim().isNotEmpty()
        ParentTaskType.REVISE_CHAPTER -> true
        null -> false
    }
    val showRegenerate = type == ParentTaskType.OUR_QUESTIONS || type == ParentTaskType.WRITE_QA

    Surface(
        color = ScreenBg,
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (state.errorMessage.isNotBlank()) {
                Text(state.errorMessage, color = Color(0xFFDC2626), fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (showRegenerate) {
                    OutlinedButton(
                        onClick = onRegenerate,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        enabled = !state.generatingPreview,
                    ) {
                        Text("Regenerate", color = PrimaryText, fontWeight = FontWeight.SemiBold)
                    }
                }
                Button(
                    onClick = onAssign,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    enabled = canAssign,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                ) {
                    Text("Assign to ${state.childName}", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DoneBlock(childName: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(SuccessSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = SuccessGreen,
                    modifier = Modifier.size(32.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Task sent to $childName",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryText,
            )
        }
    }
}

@Composable
private fun PickerRow(
    icon: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = CardBg,
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SoftBlueBg),
                contentAlignment = Alignment.Center,
            ) {
                Text(icon, fontSize = 20.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PrimaryText)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, fontSize = 12.sp, color = SecondaryText, maxLines = 1)
            }
        }
    }
}

@Composable
private fun TypeCard(emoji: String, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CardBg,
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 28.sp)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PrimaryText)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, fontSize = 12.sp, color = SecondaryText, lineHeight = 16.sp)
            }
        }
    }
}

@Composable
private fun CustomChoiceCard(
    selected: Boolean,
    emoji: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) SoftBlueBg else CardBg,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) PrimaryBlue else CardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 24.sp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PrimaryText)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, fontSize = 12.sp, color = SecondaryText, lineHeight = 16.sp)
            }
            if (selected) Icon(
                Icons.Filled.Check, contentDescription = null,
                tint = PrimaryBlue, modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun QuestionCard(number: Int, title: String, body: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = CardBg,
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(SoftBlueBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("$number", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryText,
                    lineHeight = 18.sp,
                )
            }
            Spacer(Modifier.height(8.dp))
            body()
        }
    }
}
