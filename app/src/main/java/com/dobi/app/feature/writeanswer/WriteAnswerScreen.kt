package com.dobi.app.feature.writeanswer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.dobi.app.core.ui.theme.DobiBackground
import com.dobi.app.core.ui.theme.DobiPrimary
import com.dobi.app.core.ui.theme.DobiPrimaryContainer
import com.dobi.app.core.ui.theme.DobiTextPrimary
import com.dobi.app.core.ui.theme.DobiTextSecondary
import com.dobi.app.core.ui.theme.SFProBold
import com.dobi.app.core.ui.theme.SFProSemiBold
import com.dobi.app.domain.model.AnswerGrade
import com.dobi.app.domain.model.GradingResult
import com.dobi.app.domain.model.SubjectiveQuestion
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriteAnswerScreen(
    onBack: () -> Unit,
    onLaunchAntiCheatQuiz: () -> Unit,
    viewModel: WriteAnswerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navEvents.collect { e ->
            when (e) {
                WriteAnswerNavEvent.LaunchAntiCheatQuiz -> onLaunchAntiCheatQuiz()
                WriteAnswerNavEvent.Back -> onBack()
            }
        }
    }

    Scaffold(
        containerColor = DobiBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Write Answers",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = SFProBold,
                        color = DobiTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = DobiTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DobiBackground)
            )
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            when (uiState.phase) {
                WritePhase.LOADING, WritePhase.GENERATING_QUESTIONS, WritePhase.GRADING ->
                    LoadingView(uiState.phase)
                WritePhase.MODE_PICKER -> ModePicker(
                    displayName = uiState.displayName,
                    onNotebook = viewModel::pickNotebookMode,
                    onGenerated = viewModel::pickGeneratedMode
                )
                WritePhase.COLLECTING_NOTEBOOK -> NotebookCollect(
                    state = uiState,
                    onPhotosAdded = viewModel::onNotebookPhotosAdded,
                    onPhotoRemoved = viewModel::onNotebookPhotoRemoved,
                    onSubmit = viewModel::submitNotebook,
                    onChangeMode = viewModel::goBackToModePicker
                )
                WritePhase.COLLECTING_PER_QUESTION -> PerQuestionCollect(
                    state = uiState,
                    onPhotoAdded = viewModel::onPerQuestionPhotoAdded,
                    onPhotoRemoved = viewModel::onPerQuestionPhotoRemoved,
                    onSubmit = viewModel::submitGenerated,
                    onChangeMode = viewModel::goBackToModePicker
                )
                WritePhase.RESULTS -> ResultsView(
                    state = uiState,
                    onLaunchAntiCheatQuiz = viewModel::launchAntiCheatQuiz,
                    onDone = viewModel::finish
                )
                WritePhase.ERROR -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(uiState.error ?: "Something went wrong.", color = Color(0xFFEF4444))
                }
            }
        }
    }
}

@Composable
private fun LoadingView(phase: WritePhase) {
    val msg = when (phase) {
        WritePhase.GENERATING_QUESTIONS -> "Cooking up some questions for you…"
        WritePhase.GRADING -> "Reading your answers…"
        else -> "Loading…"
    }
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = DobiPrimary)
        Spacer(Modifier.height(16.dp))
        Text(msg, color = DobiTextSecondary)
    }
}

@Composable
private fun ModePicker(
    displayName: String,
    onNotebook: () -> Unit,
    onGenerated: () -> Unit
) {
    Column(Modifier.verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(8.dp))
        TopicChip(displayName)
        Spacer(Modifier.height(20.dp))
        Text(
            "How would you like to do it?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = SFProBold,
            color = DobiTextPrimary
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Pick one — Dobi will read your handwriting and tell you how you did.",
            color = DobiTextSecondary,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(20.dp))
        ModeCardLarge(
            icon = Icons.Default.MenuBook,
            title = "I've practised in my notebook",
            subtitle = "Snap photos of the answers you've already written. Dobi will check them.",
            accent = DobiPrimary,
            onClick = onNotebook
        )
        Spacer(Modifier.height(12.dp))
        ModeCardLarge(
            icon = Icons.Default.AutoAwesome,
            title = "Give me practice questions",
            subtitle = "Dobi makes a few questions; write the answers in your notebook and snap each one.",
            accent = Color(0xFFD946EF),
            onClick = onGenerated
        )
    }
}

@Composable
private fun ModeCardLarge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE5EBF5), RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = accent, modifier = Modifier.size(28.dp)) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = SFProBold, color = DobiTextPrimary, fontSize = 15.sp)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = DobiTextSecondary, fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun NotebookCollect(
    state: WriteAnswerUiState,
    onPhotosAdded: (List<String>) -> Unit,
    onPhotoRemoved: (String) -> Unit,
    onSubmit: () -> Unit,
    onChangeMode: () -> Unit
) {
    Column(Modifier.verticalScroll(rememberScrollState())) {
        TopicChip(state.displayName)
        Spacer(Modifier.height(16.dp))
        Text(
            "Snap your notebook pages",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = SFProBold,
            color = DobiTextPrimary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Take clear photos of the page(s) where you wrote answers for this topic.",
            color = DobiTextSecondary,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
        Spacer(Modifier.height(14.dp))
        if (state.notebookPhotoUris.isNotEmpty()) {
            PhotoStrip(state.notebookPhotoUris, onPhotoRemoved)
            Spacer(Modifier.height(12.dp))
        }
        PhotoButtons(
            onPicked = onPhotosAdded,
            onCameraPicked = { onPhotosAdded(listOf(it)) }
        )
        state.error?.let {
            Spacer(Modifier.height(14.dp))
            Text(it, color = Color(0xFFEF4444), fontSize = 13.sp, lineHeight = 18.sp)
        }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onSubmit,
            enabled = state.canSubmitNotebook,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = DobiPrimary,
                disabledContainerColor = Color(0xFFBFD7F7)
            )
        ) {
            Text("Submit Answers", color = Color.White, fontWeight = SFProSemiBold)
        }
        Spacer(Modifier.height(8.dp))
        ChangeModeLink(onChangeMode)
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun PerQuestionCollect(
    state: WriteAnswerUiState,
    onPhotoAdded: (questionId: String, uri: String) -> Unit,
    onPhotoRemoved: (questionId: String, uri: String) -> Unit,
    onSubmit: () -> Unit,
    onChangeMode: () -> Unit
) {
    Column(Modifier.verticalScroll(rememberScrollState())) {
        TopicChip(state.displayName)
        Spacer(Modifier.height(14.dp))
        Text(
            "Write the answers in your notebook, then snap each one",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = SFProBold,
            color = DobiTextPrimary,
            lineHeight = 22.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Tap the camera next to a question to upload that answer.",
            color = DobiTextSecondary,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(14.dp))
        state.questions.forEachIndexed { i, q ->
            QuestionRow(
                index = i + 1,
                question = q,
                photos = state.photosByQuestionId[q.id].orEmpty(),
                onPhotoAdded = { uri -> onPhotoAdded(q.id, uri) },
                onPhotoRemoved = { uri -> onPhotoRemoved(q.id, uri) }
            )
            Spacer(Modifier.height(10.dp))
        }
        state.error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = Color(0xFFEF4444), fontSize = 13.sp, lineHeight = 18.sp)
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onSubmit,
            enabled = state.canSubmitGenerated,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = DobiPrimary,
                disabledContainerColor = Color(0xFFBFD7F7)
            )
        ) {
            Text("Submit Answers", color = Color.White, fontWeight = SFProSemiBold)
        }
        Spacer(Modifier.height(8.dp))
        ChangeModeLink(onChangeMode)
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun QuestionRow(
    index: Int,
    question: SubjectiveQuestion,
    photos: List<String>,
    onPhotoAdded: (String) -> Unit,
    onPhotoRemoved: (String) -> Unit
) {
    val context = LocalContext.current
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) pendingCameraUri?.toString()?.let(onPhotoAdded)
        pendingCameraUri = null
    }
    val cameraPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val uri = createCameraImageUri(context)
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        }
    }
    fun launchCamera() {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            val uri = createCameraImageUri(context)
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        } else cameraPermLauncher.launch(Manifest.permission.CAMERA)
    }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE5EBF5), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(DobiPrimaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "$index", color = DobiPrimary,
                    fontWeight = SFProBold, fontSize = 13.sp
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                question.text,
                modifier = Modifier.weight(1f),
                color = DobiTextPrimary,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(DobiPrimary)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = ::launchCamera
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
        if (photos.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            PhotoStrip(photos, onPhotoRemoved)
        }
    }
}

@Composable
private fun ResultsView(
    state: WriteAnswerUiState,
    onLaunchAntiCheatQuiz: () -> Unit,
    onDone: () -> Unit
) {
    val r = state.result ?: return
    Column(Modifier.verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(16.dp))
        Text("Here's how you did 🌱",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = SFProBold,
            color = DobiTextPrimary)
        Spacer(Modifier.height(6.dp))
        Text(state.displayName, color = DobiTextSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))
        StatRow(r)
        Spacer(Modifier.height(20.dp))
        Text(
            "Question by question",
            fontWeight = SFProBold,
            color = DobiTextPrimary,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(10.dp))
        r.grades.forEachIndexed { i, g ->
            AnswerCard(index = i + 1, grade = g)
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onLaunchAntiCheatQuiz,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DobiPrimary)
        ) {
            Text("Quick check — 5 quiz questions", color = Color.White, fontWeight = SFProSemiBold)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Skip for now",
            color = DobiTextSecondary,
            fontSize = 13.sp,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDone
                ),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StatRow(r: GradingResult) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DobiPrimaryContainer)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Stat("Attempted", "${r.attemptedCount}/${r.grades.size}")
        Stat("Correct", "${r.correctCount}")
        Stat("Accuracy", "${(r.accuracy * 100).toInt()}%")
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = SFProBold, color = DobiPrimary, fontSize = 18.sp)
        Text(label, color = DobiTextSecondary, fontSize = 11.sp)
    }
}

@Composable
private fun AnswerCard(index: Int, grade: AnswerGrade) {
    val accent = when {
        !grade.isAttempted -> Color(0xFF94A3B8)
        grade.isCorrect -> Color(0xFF22C55E)
        else -> Color(0xFFF59E0B)
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE5EBF5), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Q$index",
                fontWeight = SFProBold,
                color = DobiTextPrimary,
                fontSize = 13.sp
            )
            Spacer(Modifier.width(10.dp))
            Text(
                grade.softVerdict,
                color = accent,
                fontWeight = SFProSemiBold,
                fontSize = 12.sp
            )
        }
        if (grade.questionSeen.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(grade.questionSeen, color = DobiTextSecondary, fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun TopicChip(text: String) {
    Row(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(DobiPrimaryContainer)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("📘", fontSize = 14.sp)
        Spacer(Modifier.width(8.dp))
        Text(text, color = DobiPrimary, fontWeight = SFProSemiBold, fontSize = 13.sp)
    }
}

@Composable
private fun ChangeModeLink(onChangeMode: () -> Unit) {
    Text(
        "Change mode",
        color = DobiTextSecondary,
        fontSize = 12.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onChangeMode
            ),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun PhotoButtons(
    onPicked: (List<String>) -> Unit,
    onCameraPicked: (String) -> Unit
) {
    val context = LocalContext.current
    val gallery = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris -> onPicked(uris.map { it.toString() }) }

    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) pendingCameraUri?.toString()?.let(onCameraPicked)
        pendingCameraUri = null
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val uri = createCameraImageUri(context)
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        }
    }
    fun launchCamera() {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            val uri = createCameraImageUri(context)
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        } else permLauncher.launch(Manifest.permission.CAMERA)
    }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlineButton(icon = Icons.Default.CameraAlt, label = "Camera",
            modifier = Modifier.weight(1f), onClick = ::launchCamera)
        OutlineButton(icon = Icons.Default.AddPhotoAlternate, label = "Gallery",
            modifier = Modifier.weight(1f), onClick = { gallery.launch("image/*") })
    }
}

@Composable
private fun OutlineButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE5EBF5), RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = DobiPrimary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = DobiPrimary, fontWeight = SFProSemiBold, fontSize = 13.sp)
    }
}

@Composable
private fun PhotoStrip(uris: List<String>, onRemove: (String) -> Unit) {
    val rows = uris.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { uri ->
                    Box(Modifier.size(86.dp)) {
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                        )
                        Box(
                            Modifier
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
                            Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(11.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun createCameraImageUri(context: Context): Uri {
    val file = File.createTempFile("dobi_${System.currentTimeMillis()}_", ".jpg", context.cacheDir)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
