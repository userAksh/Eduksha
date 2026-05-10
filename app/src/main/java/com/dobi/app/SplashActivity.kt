package com.dobi.app

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
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
import com.dobi.app.core.navigation.Screen
import com.dobi.app.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

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
        lifecycleScope.launch {
            val screen = if (auth.currentUser != null) {
                Screen.ParentHome
            } else {
                val linked = userRepository.linkedChildId.first()
                if (linked != null) Screen.ChildHome(linked) else Screen.RoleSelection
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
