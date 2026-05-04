package com.dobi.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.dobi.app.core.navigation.DobiNavGraph
import com.dobi.app.core.navigation.Screen
import com.dobi.app.core.ui.theme.DobiBackground
import com.dobi.app.core.ui.theme.DobiTheme
import com.dobi.app.domain.repository.FcmTokenRepository
import com.dobi.app.messaging.DobiMessagingService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @Inject lateinit var fcmTokenRepository: FcmTokenRepository

    private var pendingDeeplink: Pair<String, String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingDeeplink = readDeeplink(intent)
        lifecycleScope.launch { fcmTokenRepository.syncCurrentToken() }
        setContent {
            DobiTheme {
                val startDestination by viewModel.startDestination.collectAsStateWithLifecycle()
                val navController = rememberNavController()
                var deeplink by remember { mutableStateOf(pendingDeeplink) }

                // Permission request for Android 13+ POST_NOTIFICATIONS.
                // Triggered once after the nav graph is composed.
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { /* user choice — non-blocking */ }
                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                startDestination?.let { dest ->
                    DobiNavGraph(
                        navController = navController,
                        startDestination = dest
                    )
                    LaunchedEffect(deeplink) {
                        val (taskId, role) = deeplink ?: return@LaunchedEffect
                        if (taskId.isBlank()) return@LaunchedEffect
                        when (role) {
                            "parent" -> navController.navigate(Screen.ParentTaskDetail(taskId))
                            else -> navController.navigate(Screen.DoParentTask(taskId))
                        }
                        deeplink = null
                    }
                } ?: Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DobiBackground)
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // App was already running and got tapped from a notification.
        // Stash the new deeplink; the LaunchedEffect-keyed-on-deeplink
        // picks it up when the recomposition triggers.
        pendingDeeplink = readDeeplink(intent)
        setIntent(intent)
        recreate()
    }

    private fun readDeeplink(intent: Intent?): Pair<String, String>? {
        intent ?: return null
        val taskId = intent.getStringExtra(DobiMessagingService.EXTRA_TASK_ID).orEmpty()
        val role = intent.getStringExtra(DobiMessagingService.EXTRA_ROLE).orEmpty()
        if (taskId.isBlank()) return null
        return taskId to role
    }
}
