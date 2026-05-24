package com.dobi.app.feature.earnreward

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
import com.dobi.app.core.ui.theme.SFProBold
import com.dobi.app.core.ui.theme.SFProSemiBold
import com.dobi.app.domain.model.ParentTask
import com.dobi.app.feature.conceptlist.SubjectGroup
import com.dobi.app.feature.conceptlist.UnitGroup
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun EarnRewardPickerScreen(
    onNavigateBack: () -> Unit = {},
    onRequestSent: () -> Unit = {},
    viewModel: EarnRewardPickerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.sentSuccessfully) {
        if (uiState.sentSuccessfully) {
            snackbarHostState.showSnackbar("Request sent to your parents 🎉")
            onRequestSent()
        }
    }

    Scaffold(
        containerColor = DobiBackground,
        topBar = { PickerTopBar(onNavigateBack = onNavigateBack) },
        bottomBar = {
            if (uiState.pendingRequest == null) {
                SendRequestBar(
                    enabled = uiState.canContinue,
                    isSending = uiState.isSending,
                    onClick = viewModel::sendRequestToParent
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        val pendingRequest = uiState.pendingRequest
        if (pendingRequest != null) {
            RequestSentState(
                task = pendingRequest,
                modifier = Modifier.padding(padding)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                HeaderHelper()
                SearchBar(
                    query = uiState.searchQuery,
                    onQueryChanged = viewModel::onSearchQueryChanged,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                when {
                    uiState.isLoading -> LoadingState()
                    uiState.error != null -> ErrorState(uiState.error!!)
                    uiState.subjectGroups.isEmpty() -> EmptyState(uiState.searchQuery)
                    else -> PickerTree(
                        groups = uiState.subjectGroups,
                        selection = uiState.selection,
                        onSubjectToggled = viewModel::onSubjectToggled,
                        onLessonSelected = viewModel::onLessonSelected,
                        onConceptSelected = viewModel::onConceptSelected
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickerTopBar(onNavigateBack: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = "Pick Your Topic",
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
private fun HeaderHelper() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp)
    ) {
        Text(
            text = "Pick one lesson or one concept",
            color = DobiTextPrimary,
            fontSize = 15.sp,
            fontWeight = SFProBold
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "Your parents will pick the task type and assign it to you.",
            color = DobiTextSecondary,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = query,
        onValueChange = onQueryChanged,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        placeholder = {
            Text(
                "Search concepts, subjects…",
                color = DobiTextSecondary,
                fontSize = 14.sp
            )
        },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = DobiTextSecondary,
                modifier = Modifier.size(20.dp)
            )
        },
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedTextColor = DobiTextPrimary,
            unfocusedTextColor = DobiTextPrimary,
            cursorColor = DobiPrimary
        ),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun PickerTree(
    groups: List<SubjectGroup>,
    selection: EarnRewardSelection?,
    onSubjectToggled: (String) -> Unit,
    onLessonSelected: (String) -> Unit,
    onConceptSelected: (String, String) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        groups.forEach { group ->
            item(key = "subject_${group.subject}") {
                SubjectRow(
                    group = group,
                    onClick = { onSubjectToggled(group.subject) }
                )
            }
            if (group.isExpanded) {
                group.units.forEach { unit ->
                    val lessonSelected = selection is EarnRewardSelection.Lesson &&
                        selection.unitId == unit.unitId
                    item(key = "unit_${unit.unitId}") {
                        Box(modifier = Modifier.background(Color(0xFFF8FAFF))) {
                            UnitRow(
                                unit = unit,
                                isSelected = lessonSelected,
                                onSelectLesson = { onLessonSelected(unit.unitId) }
                            )
                        }
                    }
                    items(
                        items = unit.concepts,
                        key = { concept -> "concept_${concept.id}" }
                    ) { concept ->
                        val conceptSelected = lessonSelected ||
                            (selection is EarnRewardSelection.ConceptPick &&
                                selection.conceptId == concept.id)
                        Box(modifier = Modifier.background(Color(0xFFF8FAFF))) {
                            ConceptRow(
                                name = concept.name,
                                isSelected = conceptSelected,
                                isLockedByLesson = lessonSelected,
                                onClick = {
                                    if (!lessonSelected) {
                                        onConceptSelected(concept.id, unit.unitId)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(120.dp)) }
    }
}

@Composable
private fun SubjectRow(group: SubjectGroup, onClick: () -> Unit) {
    val bgColor = parseHexColor(group.subjectColor)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(bgColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = group.subjectIcon, fontSize = 18.sp)
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = group.subject,
            style = MaterialTheme.typography.titleMedium,
            color = DobiTextPrimary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${group.units.size} Units",
            style = MaterialTheme.typography.bodySmall,
            color = DobiTextSecondary
        )
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = if (group.isExpanded) Icons.Default.KeyboardArrowUp
            else Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = DobiTextSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0xFFF1F5F9))
    )
}

@Composable
private fun UnitRow(
    unit: UnitGroup,
    isSelected: Boolean,
    onSelectLesson: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSelectLesson
            )
            .padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SelectionDot(isSelected = isSelected)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = unit.unitName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = SFProSemiBold,
                color = DobiTextPrimary
            )
            Text(
                text = "${unit.concepts.size} concepts" +
                    if (isSelected) " — all included" else "",
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) DobiPrimary else DobiTextSecondary
            )
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(start = 40.dp)
            .background(Color(0xFFF1F5F9))
    )
}

@Composable
private fun ConceptRow(
    name: String,
    isSelected: Boolean,
    isLockedByLesson: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(start = 48.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.alpha(if (isLockedByLesson) 0.6f else 1f)) {
            SelectionDot(isSelected = isSelected, small = true)
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = DobiTextPrimary,
            modifier = Modifier.weight(1f)
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(start = 60.dp)
            .background(Color(0xFFF1F5F9))
    )
}

@Composable
private fun SelectionDot(isSelected: Boolean, small: Boolean = false) {
    val outerSize = if (small) 18.dp else 22.dp
    val innerSize = if (small) 12.dp else 14.dp
    Box(
        modifier = Modifier
            .size(outerSize)
            .clip(CircleShape)
            .background(if (isSelected) DobiPrimary else Color.White)
            .border(
                width = if (isSelected) 0.dp else 1.5.dp,
                color = if (isSelected) DobiPrimary else Color(0xFFCBD5E1),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(innerSize)
            )
        }
    }
}

@Composable
private fun SendRequestBar(
    enabled: Boolean,
    isSending: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
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
            if (isSending) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Text(
                    text = "Send Request to Parents",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun RequestSentState(task: ParentTask, modifier: Modifier = Modifier) {
    val twelveHoursMs = 12 * 60 * 60 * 1000L
    val elapsedMs = System.currentTimeMillis() - task.requestedAtMillis
    val remainingMs = (twelveHoursMs - elapsedMs).coerceAtLeast(0L)
    val remainingHours = TimeUnit.MILLISECONDS.toHours(remainingMs)
    val remainingMinutes = TimeUnit.MILLISECONDS.toMinutes(remainingMs) % 60
    val timeLeft = when {
        remainingHours > 0 -> "${remainingHours}h ${remainingMinutes}m"
        remainingMinutes > 0 -> "${remainingMinutes}m"
        else -> "a moment"
    }
    val sentAt = SimpleDateFormat("hh:mm a, dd MMM", Locale.getDefault())
        .format(Date(task.requestedAtMillis))

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(DobiPrimaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.HourglassEmpty,
                contentDescription = null,
                tint = DobiPrimary,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Request Sent to Parent",
            fontSize = 20.sp,
            fontWeight = SFProBold,
            color = DobiTextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = task.displayName,
            fontSize = 15.sp,
            fontWeight = SFProSemiBold,
            color = DobiPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Sent at $sentAt",
            fontSize = 13.sp,
            color = DobiTextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFF1F5F9))
                .padding(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "⏳ Waiting for parent",
                    fontSize = 13.sp,
                    fontWeight = SFProSemiBold,
                    color = DobiTextPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "You can send a new request in $timeLeft, or once your parent responds.",
                    fontSize = 12.sp,
                    color = DobiTextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = DobiPrimary)
    }
}

@Composable
private fun ErrorState(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "Error: $message", color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun EmptyState(query: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = if (query.isBlank()) "No concepts found" else "No results for \"$query\"",
            color = DobiTextSecondary
        )
    }
}

private fun parseHexColor(hex: String): Color = runCatching {
    Color(android.graphics.Color.parseColor(hex))
}.getOrDefault(Color(0xFF22C55E))
