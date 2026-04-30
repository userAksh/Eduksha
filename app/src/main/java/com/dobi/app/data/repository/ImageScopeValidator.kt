package com.dobi.app.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

data class ImageScopeRequest(
    val subject: String,
    val grade: Int,
    /** What the child says they uploaded — e.g. concept, unit, or topic. */
    val topic: String,
    /** Optional broader lesson context, used when topic is concept-level. */
    val lessonContext: String = "",
    /** What kind of artefact this is — class work, homework, etc. */
    val artefactLabel: String = "class work",
    val photoUris: List<String>
)

/**
 * Single-shot Gemini check that confirms the uploaded photos actually show
 * the topic the child claims. Returns Result.success on OK, or
 * Result.failure with a kid-friendly refusal sentence on mismatch.
 *
 * This is intentionally cheaper than [McqGenerationRepository] — it does not
 * generate MCQs, only a short JSON verdict.
 */
@Singleton
class ImageScopeValidator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val model: GenerativeModel by lazy {
        Firebase.ai(backend = GenerativeBackend.vertexAI()).generativeModel(
            modelName = "gemini-2.5-flash-lite",
            generationConfig = generationConfig {
                temperature = 0.0f
                responseMimeType = "application/json"
                responseSchema = SCHEMA
            },
            systemInstruction = content { text(SYSTEM_PROMPT) }
        )
    }

    suspend fun validate(request: ImageScopeRequest): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            require(request.photoUris.isNotEmpty()) { "No photos provided" }
            val bitmaps = request.photoUris.mapNotNull { decodeBitmap(it) }
            require(bitmaps.isNotEmpty()) { "Couldn't read uploaded photos" }

            val input = content {
                text(buildPrompt(request))
                bitmaps.forEach { image(it) }
            }
            val raw = model.generateContent(input).text
            require(!raw.isNullOrBlank()) { "Empty response from Gemini" }
            val verdict = json.decodeFromString<Verdict>(raw)
            if (!verdict.ok) {
                error(verdict.reason.ifBlank { "These photos don't seem to match the topic." })
            }
        }.also { result ->
            result.onFailure { Log.w(TAG, "Validation failed: ${it.message}") }
        }
    }

    private fun buildPrompt(r: ImageScopeRequest): String {
        val context = if (r.lessonContext.isNotBlank() && r.lessonContext != r.topic) {
            "Lesson: ${r.lessonContext}\nTopic: ${r.topic}"
        } else {
            "Topic: ${r.topic}"
        }
        return """
            A student in India says they uploaded photos of their ${r.artefactLabel}
            for Class ${r.grade} ${r.subject}.
            $context

            Decide whether EVERY photo actually shows ${r.artefactLabel} for
            this topic. ${r.artefactLabel.replaceFirstChar { it.uppercase() }} can
            be handwritten notes, worksheets, or textbook pages — anything
            educational. It is NOT random objects, screenshots, selfies, or
            pages from a totally different chapter.

            Reply with ONLY this JSON:
              { "ok": true } — when every photo fits
              { "ok": false, "reason": "<one short kid-friendly sentence>" }
                — when even one photo doesn't fit

            The reason MUST mention the topic by name and describe what you saw
            in the off-topic photo (e.g. "This looks like a purse, not Science
            class work for '${r.topic}'.").

            Be strict. If you're unsure, refuse.
        """.trimIndent()
    }

    private fun decodeBitmap(uriString: String): Bitmap? = runCatching {
        val opts = BitmapFactory.Options().apply { inSampleSize = computeSampleSize(uriString) }
        context.contentResolver.openInputStream(Uri.parse(uriString)).use { stream ->
            BitmapFactory.decodeStream(stream, null, opts)
        }
    }.getOrNull()

    private fun computeSampleSize(uriString: String): Int = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(Uri.parse(uriString)).use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        }
        val maxSide = maxOf(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
        var sample = 1
        while (maxSide / sample > MAX_IMAGE_SIDE) sample *= 2
        sample
    }.getOrDefault(1)

    @Serializable
    private data class Verdict(
        val ok: Boolean = false,
        @SerialName("reason") val reason: String = ""
    )

    companion object {
        private const val TAG = "ScopeValidator"
        private const val MAX_IMAGE_SIDE = 1024

        private val SYSTEM_PROMPT = """
            You are an Indian school-textbook tutor verifying whether a child's
            uploaded photos belong to a stated topic. You only output JSON.
            Be strict — if even one photo is off-topic, not school work, or
            unreadable, refuse the whole batch with a kid-friendly reason that
            names the topic and says what you saw.
        """.trimIndent()

        private val SCHEMA = Schema.obj(
            properties = mapOf(
                "ok" to Schema.boolean(),
                "reason" to Schema.string()
            ),
            optionalProperties = listOf("reason")
        )
    }
}
