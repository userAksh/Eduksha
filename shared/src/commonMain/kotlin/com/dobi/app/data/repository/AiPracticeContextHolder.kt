package com.dobi.app.data.repository

data class AiPracticeContext(
    val selectionKey: String,
    val displayName: String,
    val subject: String,
    val grade: Int,
    val scopeText: String,
    val teacherHonorific: String = "",
    val teacherName: String = "",
    val dateMillis: Long = 0L,
    val preselectedWriteMode: PreselectedWriteMode? = null,
    val unitId: String = "",
    val unitName: String = "",
    val conceptId: String = "",
    val conceptName: String = ""
)

enum class PreselectedWriteMode { NOTEBOOK, GENERATED, AI_TEXTBOOK }

class AiPracticeContextHolder {
    private var current: AiPracticeContext? = null

    fun put(context: AiPracticeContext) { current = context }
    fun peek(): AiPracticeContext? = current
    fun clear() { current = null }
}
