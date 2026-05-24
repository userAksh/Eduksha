package com.dobi.app.feature.childhome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dobi.app.core.ui.theme.DobiBackground
import com.dobi.app.core.ui.theme.DobiPrimary
import com.dobi.app.core.ui.theme.DobiTextPrimary
import com.dobi.app.domain.model.ParentTaskStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentTasksListScreen(
    onNavigateBack: () -> Unit = {},
    onTaskClick: (taskId: String) -> Unit = {},
    onNavigateToEarnReward: () -> Unit = {},
    viewModel: ParentTasksListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = DobiBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Tasks from Parents",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(DobiBackground)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 24.dp
            )
        ) {
            items(uiState.activeTasks, key = { it.id }) { task ->
                ParentTaskCard(task = task, onClick = { onTaskClick(task.id) })
                Spacer(Modifier.height(12.dp))
            }

            items(uiState.requestedTasks, key = { "req-${it.id}" }) { task ->
                PendingRequestCard(
                    task = task,
                    isSendingReminder = uiState.sendingReminderForTaskId == task.id,
                    reminderSent = uiState.reminderSentForTaskId == task.id,
                    onSendReminder = { viewModel.sendReminderToParent(task.id) },
                )
                Spacer(Modifier.height(12.dp))
            }

            val hasRedoPending = uiState.activeTasks.any { it.status == ParentTaskStatus.REJECTED }
            if (!hasRedoPending && uiState.requestedTasks.isEmpty()) {
                item { RequestNextTaskRow(onClick = onNavigateToEarnReward) }
            }
        }
    }
}
