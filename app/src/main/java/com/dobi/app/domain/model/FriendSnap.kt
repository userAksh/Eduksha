package com.dobi.app.domain.model

/**
 * A streak snap a friend has shared with the current child.
 * Surfaces on the Streak Home dashboard once today's task is complete.
 */
data class FriendSnap(
    val id: String = "",
    val friendName: String = "",
    val friendAvatarEmoji: String = "🧑",
    val lessonName: String = "",
    val subject: String = "",
    val sharedAtMillis: Long = 0L
)
