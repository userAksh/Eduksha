package com.dobi.app.domain.model

data class DailyNewsStory(
    val id: String,
    val category: String,
    val emoji: String,
    val headline: String,
    val body: String
)
