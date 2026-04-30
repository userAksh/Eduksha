package com.dobi.app.data.repository

import com.dobi.app.domain.model.McqQuestion
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Non-consuming in-memory cache for AI-generated "curated" quizzes
 * (the Test Yourself → Our Question Set fallback when the hand-written
 * [com.dobi.app.data.CuratedQuestionBank] doesn't cover a lesson).
 *
 * Repeated attempts on the same target should hit the cache so each
 * retake doesn't burn another Gemini call.
 */
@Singleton
class CuratedQuizCache @Inject constructor() {
    private val store = ConcurrentHashMap<String, List<McqQuestion>>()

    fun put(targetId: String, questions: List<McqQuestion>) {
        store[targetId] = questions
    }

    fun get(targetId: String): List<McqQuestion>? = store[targetId]
}
