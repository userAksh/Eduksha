package com.dobi.app.domain.model

data class Completion(
    val childId: String,
    val conceptId: String,
    val effort: Difficulty,
    val mode: WorkMode
)
