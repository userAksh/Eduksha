package com.dobi.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import com.dobi.app.data.local.UserPreferences
import com.dobi.app.domain.repository.InviteRepository
import kotlinx.coroutines.flow.first
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
import kotlinx.serialization.json.Json
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @Inject lateinit var fcmTokenRepository: FcmTokenRepository
    @Inject lateinit var inviteRepository: InviteRepository
    @Inject lateinit var userPreferences: UserPreferences

    private var pendingDeeplink: Pair<String, String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingDeeplink = readDeeplink(intent)
        lifecycleScope.launch { fcmTokenRepository.syncCurrentToken() }
        maybeRunDebugInviteClaim(intent)
        // SplashActivity resolves the start destination during video playback
        // and passes it forward; falls back to MainViewModel when MainActivity
        // is launched directly (e.g. an FCM notification tap).
        val splashStart = intent.getStringExtra(SplashActivity.EXTRA_START_SCREEN)
            ?.let { runCatching { Json.decodeFromString(Screen.serializer(), it) }.getOrNull() }

        setContent {
            DobiTheme {
                val viewModelStart by viewModel.startDestination.collectAsStateWithLifecycle()
                val startDestination = splashStart ?: viewModelStart
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

    /**
     * Debug-only invite-claim shortcut. The Play Install Referrer API only
     * fires for installs that came through the Play Store, so locally-built
     * APKs can't exercise the claim flow. To test against a real backend
     * before publishing, launch with:
     *
     *   adb shell am start -n com.dobi.app/.MainActivity \
     *       --es dobi_test_invite_code <code-from-createInvite>
     *
     * Stripped from release builds via BuildConfig.DEBUG.
     */
    private fun maybeRunDebugInviteClaim(intent: Intent?) {
        if (!BuildConfig.DEBUG) return
        val code = intent?.getStringExtra(EXTRA_TEST_INVITE_CODE)?.trim().orEmpty()
        if (code.isBlank()) return
        lifecycleScope.launch {
            val childId = userPreferences.linkedChildId.first().orEmpty()
            if (childId.isBlank()) {
                Log.w(TAG, "Test claim skipped: no linked child yet")
                return@launch
            }
            val outcome = inviteRepository.claimInvite(code, childId)
            Log.i(TAG, "Test claim outcome=$outcome (code=$code)")
        }
    }

    companion object {
        private const val TAG = "MainActivity"
        const val EXTRA_TEST_INVITE_CODE = "dobi_test_invite_code"
    }
}
