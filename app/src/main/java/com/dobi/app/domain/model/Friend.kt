package com.dobi.app.domain.model

data class Friend(
    val parentId: String = "",
    val childId: String = "",
    val name: String = "",
    val grade: Int = 0,
    val gender: String = "",
    val addedAtMillis: Long = 0L
)
