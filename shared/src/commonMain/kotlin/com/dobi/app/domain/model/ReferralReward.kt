package com.dobi.app.domain.model

data class ReferralReward(
    val id: String = "",
    val stage: Int = 0,
    val referredParentId: String = "",
    val referredParentName: String = "",
    val unlockedAtMillis: Long = 0L,
    val scratchedAtMillis: Long = 0L,
    val rewardLabel: String = "",
    val rewardValue: String = "",
) {
    val isScratched: Boolean get() = scratchedAtMillis > 0L
}
