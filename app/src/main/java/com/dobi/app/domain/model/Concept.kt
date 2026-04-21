package com.dobi.app.domain.model

data class Concept(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val subject: String = "",
    val subjectColor: String = "#22C55E",
    val subjectIcon: String = "🔬",
    val unit: String = "",
    val unitId: String = "",
    val unitOrder: Int = 0,
    val grade: Int = 6,
    val board: String = "CBSE",
    val expectedDifficulty: Difficulty = Difficulty.OK,
    val aliases: List<String> = emptyList(),
    val prerequisites: List<String> = emptyList(),
    val order: Int = 0
)

enum class Difficulty { EASY, OK, HARD }
