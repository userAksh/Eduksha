package com.dobi.app.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import javax.inject.Inject
import javax.inject.Singleton

object Events {
    // ACQUISITION
    const val ROLE_SELECTED = "role_selected"
    const val PHONE_AUTH_INITIATED = "phone_auth_initiated"
    const val OTP_VERIFIED = "otp_verified"
    const val REFERRAL_CODE_APPLIED = "referral_code_applied"

    // ACTIVATION
    const val ADD_CHILD_STARTED = "add_child_started"
    const val CHILD_PROFILE_CREATED = "child_profile_created"
    const val PARENT_HOME_REACHED = "parent_home_reached"
    const val CHILD_HOME_REACHED = "child_home_reached"
    const val PAIRING_CODE_GENERATED = "pairing_code_generated"
    const val PAIRING_SUCCESS = "pairing_success"

    // PARENT ENGAGEMENT
    const val TASK_ASSIGNED = "task_assigned"
    const val TASK_REVIEWED = "task_reviewed"
    const val STUDY_GOALS_OPENED = "study_goals_opened"
    const val INVITE_SHARED = "invite_shared"

    // REFERRAL
    const val REFERRAL_CLAIMED = "referral_claimed"
    const val REWARD_SCRATCHED = "reward_scratched"
    const val REWARD_UNLOCKED = "reward_unlocked"

    // CHILD ENGAGEMENT
    const val UPLOAD_INITIATED = "upload_initiated"
    const val UPLOAD_COMPLETED = "upload_completed"
    const val QUIZ_STARTED = "quiz_started"
    const val QUIZ_COMPLETED = "quiz_completed"
    const val HOMEWORK_LOGGED = "homework_logged"
    const val TASK_SUBMITTED = "task_submitted"

    // REVENUE
    const val PAYWALL_SHOWN = "paywall_shown"
    const val CHECKOUT_STARTED = "checkout_started"
    const val SUBSCRIPTION_STARTED = "subscription_started"
}

@Singleton
class AnalyticsTracker @Inject constructor(
    private val analytics: FirebaseAnalytics,
) {
    fun log(event: String, params: Bundle.() -> Unit = {}) {
        analytics.logEvent(event, Bundle().apply(params))
    }
}
