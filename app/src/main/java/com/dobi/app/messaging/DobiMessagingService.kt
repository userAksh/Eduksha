package com.dobi.app.messaging

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.dobi.app.MainActivity
import com.dobi.app.R
import com.dobi.app.domain.repository.FcmTokenRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Receives FCM messages.
 *
 * - On token rotation: persists the new token to Firestore via [fcmTokenRepository].
 * - On data message in the foreground: posts a local notification so the user
 *   isn't silently missing the buzz. (Background data messages are handled
 *   automatically by Android with the manifest meta-data tags.)
 */
@AndroidEntryPoint
class DobiMessagingService : FirebaseMessagingService() {

    @Inject lateinit var fcmTokenRepository: FcmTokenRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        scope.launch { fcmTokenRepository.onTokenRotated(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        ensureChannel()
        val title = message.notification?.title
            ?: message.data["title"]
            ?: "Dobi"
        val body = message.notification?.body
            ?: message.data["body"]
            ?: ""
        val taskId = message.data["taskId"].orEmpty()
        val role = message.data["role"].orEmpty() // "parent" or "child"

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_ROLE, role)
        }
        val pi = PendingIntent.getActivity(
            this,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(taskId.hashCode().takeIf { it != 0 } ?: 1, notification)
    }

    private fun ensureChannel() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Dobi alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Task requests, assignments, and reviews"
                }
            )
        }
    }

    companion object {
        const val CHANNEL_ID = "dobi_default"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_ROLE = "extra_role"
    }
}
