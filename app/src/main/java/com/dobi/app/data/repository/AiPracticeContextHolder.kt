package com.dobi.app.data.repository

import javax.inject.Inject
import javax.inject.Singleton

data class AiPracticeContext(
    val selectionKey: String,
    val displayName: String,
    val subject: String,
    val grade: Int,
    val scopeText: String,
    val teacherHonorific: String = "",
    val teacherName: String = "",
    val dateMillis: Long = System.currentTimeMillis(),
    /**
     * Optional hint for the WriteAnswer flow to skip its mode picker and
     * jump straight into "notebook" (snap pre-written answers) or
     * "generated" (Dobi makes questions, student answers each). Test
     * Yourself uses this; Class Test Prep leaves it null so its picker
     * still shows.
     */
    val preselectedWriteMode: PreselectedWriteMode? = null,
    /**
     * When the practice session was scoped to a single curated lesson or
     * concept, these point back to it so the resulting subjective record
     * can surface in the Self Revision → Revisit Past Topics flow.
     */
    val unitId: String = "",
    val unitName: String = "",
    val conceptId: String = "",
    val conceptName: String = ""
)

enum class PreselectedWriteMode { NOTEBOOK, GENERATED, AI_TEXTBOOK }

@Singleton
class AiPracticeContextHolder @Inject constructor() {
    @Volatile private var current: AiPracticeContext? = null

    fun put(context: AiPracticeContext) { current = context }
    fun peek(): AiPracticeContext? = current
    fun clear() { current = null }
}
