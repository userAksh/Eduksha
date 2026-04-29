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
    val dateMillis: Long = System.currentTimeMillis()
)

@Singleton
class AiPracticeContextHolder @Inject constructor() {
    @Volatile private var current: AiPracticeContext? = null

    fun put(context: AiPracticeContext) { current = context }
    fun peek(): AiPracticeContext? = current
    fun clear() { current = null }
}
