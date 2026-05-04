package com.dobi.app.data.repository

import android.util.Log
import com.dobi.app.domain.model.DailyNewsStory
import com.dobi.app.domain.repository.DailyNewsRepository
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class DailyNewsRepositoryImpl @Inject constructor(
    private val functions: FirebaseFunctions
) : DailyNewsRepository {

    override suspend fun fetchDailyNews(): Result<List<DailyNewsStory>> = runCatching {
        val result = await {
            functions.getHttpsCallable("getDailyNews").call(emptyMap<String, Any>())
        }
        @Suppress("UNCHECKED_CAST")
        val payload = result.data as? Map<String, Any?>
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
    }.onFailure { Log.w(TAG, "fetchDailyNews failed", it) }

    private suspend fun <T> await(
        block: () -> com.google.android.gms.tasks.Task<T>
    ): T = suspendCancellableCoroutine { cont ->
        block()
            .addOnSuccessListener { cont.resume(it) }
            .addOnFailureListener { cont.resumeWithException(it) }
    }

    companion object {
        private const val TAG = "DailyNewsRepository"
    }
}
