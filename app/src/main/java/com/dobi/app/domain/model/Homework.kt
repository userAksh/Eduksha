package com.dobi.app.domain.model

data class Homework(
    val childId: String,
    val subject: String,
    val teacherName: String,
    val date: String,           // ISO "yyyy-MM-dd"
    val photoUrls: List<String> = emptyList()
)
