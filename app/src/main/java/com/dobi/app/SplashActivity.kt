package com.dobi.app

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import com.dobi.app.data.local.LanguagePreference
import java.util.Locale
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.dobi.app.BuildConfig
import com.dobi.app.core.navigation.Screen
import com.dobi.app.data.local.UserPreferences
import com.dobi.app.domain.model.UserRole
import com.dobi.app.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import javax.inject.Inject
import kotlin.coroutines.resume

/**
 * First activity on launch. Plays the branded intro video and, in parallel,
 * resolves which [Screen] [MainActivity] should open on. Hands off only once
 * BOTH the video has finished (or been skipped/timed out) AND the routing
 * decision is ready, so MainActivity can render the first screen immediately.
 *
 * Tap-to-skip is wired so returning users aren't held hostage.
 *
 * Uses Media3 ExoPlayer rather than VideoView/MediaPlayer because the latter
 * failed at decode-start on MIUI hardware (onPrepared OK, then onError what=1
 * extra=0x80000000) — ExoPlayer falls back to software decode and is more
 * forgiving of codec edge cases.
 */
@AndroidEntryPoint
@UnstableApi
class SplashActivity : ComponentActivity() {

    @Inject lateinit var auth: FirebaseAuth
    @Inject lateinit var userRepository: UserRepository
    @Inject lateinit var userPreferences: UserPreferences
    @Inject lateinit var firestore: FirebaseFirestore

    override fun attachBaseContext(newBase: Context) {
        val lang = LanguagePreference.get(newBase)
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    private var handedOff = false
    @Volatile private var splashHoldReleased = false
    @Volatile private var resolvedScreen: Screen? = null
    @Volatile private var handoffRequested = false

    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // Edge-to-edge MUST be configured before super.onCreate so the very
        // first layout pass uses full-screen bounds. If we wait until after
        // super, the activity is laid out within the bar-excluded area first
        // and the FrameLayout/PlayerView caches the smaller height — the
        // video then renders only inside that smaller box and the activity's
        // white windowBackground shows where the status bar and gesture nav
        // used to be.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
        }

        // installSplashScreen() must be called before super.onCreate().
        val splash = installSplashScreen()
        splash.setKeepOnScreenCondition { !splashHoldReleased }

        super.onCreate(savedInstanceState)

        // Resolve routing in parallel with video playback. Same logic that
        // used to live in MainViewModel; doing it here lets us hand the
        // resolved Screen to MainActivity so it renders without a blank frame.
        // Diagnostic: check currentUser synchronously before coroutine starts
        Log.d(TAG, "onCreate: auth.currentUser sync-check = ${auth.currentUser?.uid ?: "null"}")

        lifecycleScope.launch {
            // Check minimum required version first. Fail-open if Firestore is
            // unreachable (offline users must not be blocked by a network error).
            val minVersionCode = try {
                val doc = firestore.collection("dobi_meta").document("appConfig").get().await()
                doc.getLong("minVersionCode")?.toInt() ?: 0
            } catch (e: Exception) {
                Log.w(TAG, "version check failed (fail-open): ${e.message}")
                0
            }
            if (BuildConfig.VERSION_CODE < minVersionCode) {
                Log.i(TAG, "force update: versionCode=${BuildConfig.VERSION_CODE} < min=$minVersionCode")
                resolvedScreen = Screen.ForceUpdate
                if (handoffRequested) handOff()
                return@launch
            }

            // Firebase fires the AuthStateListener immediately on registration.
            // On cold start with a Keystore-encrypted refresh token, the first
            // callback can be null before decryption completes; Firebase fires
            // the listener AGAIN once decryption succeeds. Removing it on the
            // first null callback (previous approach) caused us to miss that
            // second fire and incorrectly treat the user as logged-out.
            // Fix: keep the listener alive; only remove when user is non-null.
            // withTimeoutOrNull bounds the wait for genuinely logged-out users.
            // Check synchronously first — Firebase loads the cached user before
            // the first onAuthStateChanged post reaches the Main thread. If the
            // sync check returns a user we can route immediately without any wait.
            val syncUser = auth.currentUser
            Log.d(TAG, "auth resolution start: syncUser=${syncUser?.uid ?: "null"}")
            val currentUser = syncUser ?: withTimeoutOrNull(4_000) {
                suspendCancellableCoroutine { cont ->
                    val listener = object : FirebaseAuth.AuthStateListener {
                        override fun onAuthStateChanged(firebaseAuth: FirebaseAuth) {
                            Log.d(TAG, "AuthStateListener fired: currentUser=${firebaseAuth.currentUser?.uid ?: "null"}")
                            val user = firebaseAuth.currentUser ?: return
                            firebaseAuth.removeAuthStateListener(this)
                            if (cont.isActive) cont.resume(user)
                        }
                    }
                    auth.addAuthStateListener(listener)
                    cont.invokeOnCancellation { auth.removeAuthStateListener(listener) }
                }
            } ?: auth.currentUser.also {
                Log.d(TAG, "auth timeout fired; fallback currentUser=${it?.uid ?: "null"}")
            }
            val screen = if (currentUser != null) {
                Screen.Paywall
            } else {
                val isChildDevice = userPreferences.isChildDevice.first()
                val linked = userRepository.linkedChildId.first()
                when {
                    isChildDevice && linked != null -> Screen.ChildHome(linked)
                    // Firebase session lost (Keystore key invalidated, reinstall, etc.)
                    // but this device was previously used as a parent account — skip
                    // role selection and go straight to phone re-auth.
                    userPreferences.userRole.first() == UserRole.PARENT ->
                        Screen.PhoneAuth
                    else -> Screen.RoleSelection
                }
            }
            Log.d(TAG, "routing resolved: $screen")
            resolvedScreen = screen
            if (handoffRequested) handOff()
        }

        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        val exo = ExoPlayer.Builder(this).build().also { player = it }
        exo.volume = 0f // splash playback shouldn't fight other audio
        exo.repeatMode = Player.REPEAT_MODE_OFF
        exo.setMediaItem(
            MediaItem.fromUri(Uri.parse("android.resource://${packageName}/${R.raw.splash_intro}"))
        )
        exo.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_READY -> {
                        Log.d(TAG, "STATE_READY: duration=${exo.duration}ms")

                        // Surface buffered + decoder ready → release system splash.
                        splashHoldReleased = true
                    }
                    Player.STATE_ENDED -> {
                        Log.d(TAG, "STATE_ENDED: video finished")
                        requestHandoff()
                    }
                    Player.STATE_BUFFERING -> Log.d(TAG, "STATE_BUFFERING")
                    Player.STATE_IDLE -> Log.d(TAG, "STATE_IDLE")
                }
            }

            override fun onRenderedFirstFrame() {
                // Confirms a frame actually painted — useful when debugging
                // the "video is playing but I see white" failure mode.
                Log.d(TAG, "onRenderedFirstFrame")
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "onPlayerError: ${error.errorCodeName} (${error.errorCode})", error)
                splashHoldReleased = true
                requestHandoff()
            }
        })
        exo.prepare()
        exo.playWhenReady = true

        val playerView = PlayerView(this).apply {
            useController = false
            // RESIZE_MODE_ZOOM scales the video to fill the view, preserving
            // its aspect ratio and cropping any excess. The splash is 720×1432
            // (9:18) and the screen is 9:20-ish, so a few % gets cropped off
            // the sides — the icon is centered and that's invisible. FILL was
            // visibly stretching the icon vertically; ZOOM keeps it round.
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            // The "shutter" is what PlayerView shows before the first frame
            // paints. Match the splash background so the handoff from system
            // splash → ExoPlayer surface is seamless instead of flashing black.
            setShutterBackgroundColor(Color.BLACK)
            setBackgroundColor(Color.BLACK)
            this.player = exo
            setOnClickListener {
                Log.d(TAG, "onClick: user tap-skipped")
                requestHandoff()
            }
        }
        setContentView(playerView)

        // Hard ceiling: only fires if ExoPlayer never reaches STATE_READY or
        // STATE_ENDED (rare — codec init hang). Video is ~2.6s; this safety
        // window is comfortably longer so a normal play-through is never cut.
        playerView.postDelayed({
            Log.w(TAG, "MAX_SPLASH_MS reached — forcing handOff (prepare/playback hung)")
            splashHoldReleased = true
            requestHandoff()
        }, MAX_SPLASH_MS)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // MIUI re-shows the system bars on focus events; re-hiding them here
        // keeps the splash truly fullscreen edge-to-edge. Without this the
        // status bar and nav bar peek over the top and bottom of the video
        // even though the PlayerView itself is laid out at full screen size.
        if (hasFocus) {
            WindowInsetsControllerCompat(window, window.decorView).apply {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
    }

    private fun requestHandoff() {
        handoffRequested = true
        if (resolvedScreen != null) handOff()
    }

    private fun handOff() {
        if (handedOff) return
        handedOff = true
        val intent = Intent(this, MainActivity::class.java)
        resolvedScreen?.let { screen ->
            intent.putExtra(
                EXTRA_START_SCREEN,
                Json.encodeToString(Screen.serializer(), screen)
            )
        }
        startActivity(intent)
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    companion object {
        private const val TAG = "CheckVideoIssue"
        private const val MAX_SPLASH_MS = 10_000L
        const val EXTRA_START_SCREEN = "dobi_start_screen"
    }
}
