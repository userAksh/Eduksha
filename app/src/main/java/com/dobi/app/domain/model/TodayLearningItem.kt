package com.dobi.app.domain.model

data class TodayLearningItem(
    val conceptId: String = "",
    val conceptName: String,
    val subject: String,
    val unit: String,
    val status: LearningStatus
)
