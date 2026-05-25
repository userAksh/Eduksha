package com.dobi.app.data.repository

import com.dobi.app.domain.model.DailyNewsStory
import com.dobi.app.domain.repository.DailyNewsRepository
import dev.gitlive.firebase.functions.FirebaseFunctions

class DailyNewsRepositoryImpl(
    private val functions: FirebaseFunctions
) : DailyNewsRepository {

    private var sessionCache: List<DailyNewsStory>? = null

    override suspend fun fetchDailyNews(): Result<List<DailyNewsStory>> {
        sessionCache?.let { return Result.success(it) }
        return fetchFromNetwork()
    }

    private suspend fun fetchFromNetwork(): Result<List<DailyNewsStory>> = runCatching {
        val result = functions.httpsCallable("getDailyNews").invoke(emptyMap<String, Any>())
        @Suppress("UNCHECKED_CAST")
        val payload = result.data<Any>() as? Map<String, Any?>
            ?: error("getDailyNews returned no payload")
        @Suppress("UNCHECKED_CAST")
        val rawStories = payload["stories"] as? List<Map<String, Any?>>
            ?: error("getDailyNews payload missing stories")
        rawStories.mapIndexedNotNull { index, item ->
            val headline = (item["headline"] as? String).orEmpty().trim()
            val body = (item["body"] as? String).orEmpty().trim()
            if (headline.isBlank() || body.isBlank()) return@mapIndexedNotNull null
            DailyNewsStory(
                id = (item["id"] as? String).orEmpty().ifBlank { "story-${index + 1}" },
                category = (item["category"] as? String).orEmpty().ifBlank { "Fun fact" },
                emoji = (item["emoji"] as? String).orEmpty().ifBlank { "💡" },
                headline = headline,
                body = body
            )
        }
    }.onSuccess { sessionCache = it }
}
