package com.dobi.app.data.repository

import com.dobi.app.domain.model.McqQuestion
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeneratedQuizCache @Inject constructor() {
    private val store = ConcurrentHashMap<String, List<McqQuestion>>()

    fun put(unitId: String, questions: List<McqQuestion>) {
        store[unitId] = questions
    }

    fun consume(unitId: String): List<McqQuestion>? = store.remove(unitId)

    fun peek(unitId: String): List<McqQuestion>? = store[unitId]
}
