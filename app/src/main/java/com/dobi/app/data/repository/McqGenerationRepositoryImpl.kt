package com.dobi.app.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.dobi.app.domain.model.McqQuestion
import com.dobi.app.domain.usecase.GenerateMcqRequest
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class McqGenerationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : McqGenerationRepository {

    private val json = Json { ignoreUnknownKeys = true }

    private val primaryModel: GenerativeModel by lazy { buildModel("gemini-2.5-flash") }
    private val fallbackModel: GenerativeModel by lazy { buildModel("gemini-2.5-flash-lite") }

    private fun buildModel(name: String): GenerativeModel =
        Firebase.ai(backend = GenerativeBackend.vertexAI()).generativeModel(
            modelName = name,
            generationConfig = generationConfig {
                temperature = 0.15f
                responseMimeType = "application/json"
                responseSchema = MCQ_SCHEMA
            },
            systemInstruction = content { text(SYSTEM_PROMPT) }
        )

    override suspend fun generate(request: GenerateMcqRequest): Result<List<McqQuestion>> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(request.photoUris.isNotEmpty()) { "No photos provided" }

                val bitmaps = request.photoUris.mapNotNull { decodeBitmap(it) }
                require(bitmaps.isNotEmpty()) { "Couldn't read uploaded photos" }

                val userPrompt = buildUserPrompt(request)
                val input = content {
                    text(userPrompt)
                    bitmaps.forEach { image(it) }
                }

                val raw = generateWithResilience(input)
                val parsed = json.decodeFromString<McqResponse>(raw)
                if (parsed.error != null) error(parsed.error)
                parsed.questions.orEmpty().map { it.toDomain() }
                    .also { require(it.isNotEmpty()) { "Model returned no questions" } }
            }
        }

    private suspend fun generateWithResilience(input: Content): String {
        val backoffsMs = longArrayOf(0, 1500, 4000)
        var lastError: Throwable? = null

        for (attempt in backoffsMs.indices) {
            if (backoffsMs[attempt] > 0) delay(backoffsMs[attempt])
            try {
                val text = primaryModel.generateContent(input).text
                if (!text.isNullOrBlank()) return text
                error("Empty response from Gemini")
            } catch (t: Throwable) {
                lastError = t
                Log.w(TAG, "Primary model attempt ${attempt + 1} failed: ${t.message}")
                if (!isRetryable(t)) throw t
            }
        }

        try {
            Log.w(TAG, "Falling back to gemini-2.5-flash-lite")
            val text = fallbackModel.generateContent(input).text
            if (!text.isNullOrBlank()) return text
        } catch (t: Throwable) {
            Log.w(TAG, "Fallback model failed: ${t.message}")
            lastError = t
        }

        throw lastError ?: IllegalStateException("Generation failed")
    }

    private fun isRetryable(t: Throwable): Boolean {
        val msg = (t.message ?: "").lowercase()
        return "unavailable" in msg ||
            "overloaded" in msg ||
            "503" in msg ||
            "high demand" in msg ||
            "deadline" in msg ||
            "timeout" in msg ||
            "internal" in msg && "500" in msg
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

    private fun buildUserPrompt(r: GenerateMcqRequest): String {
        val scopeLines = r.customScopeText
            ?: if (r.conceptName.isNotBlank()) {
                """
                    Lesson: ${r.unitName}
                    Concept (focus narrowly on this): ${r.conceptName}
                """.trimIndent()
            } else {
                "Lesson: ${r.unitName}"
            }
        return """
            $scopeLines
            Subject: ${r.subject}
            Grade: ${r.grade}

            Apply the 4-step procedure from the system prompt now:
            1. Classify EACH photo independently as (a/b/c/d/e). Number them in
               upload order: photo 1, photo 2, …
            2. Decide for the whole batch. Generate ONLY if EVERY photo is (a).
               If even one photo is (b)/(c)/(d)/(e), refuse the entire batch and
               name the offending photo(s) by index — do not silently proceed using
               only the matching photos.
            3. If refusing, mention the topic(s) above by name, list which photo(s)
               are off-topic by 1-based index, and describe what each off-topic
               photo actually is.
            4. Only when every photo is (a), generate exactly ${r.targetCount}
               questions with Bloom mix recall=${r.bloomMix.recall},
               understand=${r.bloomMix.understand}, apply=${r.bloomMix.apply},
               analyze=${r.bloomMix.analyze}.

            Reminder: a page that mixes Q&A from many chapters is case (c) and must
            be refused, even if one or two of its items happen to relate to the
            topic(s) above. Default to refusing when uncertain.
        """.trimIndent()
    }

    @Serializable
    private data class McqResponse(
        val questions: List<McqDto>? = null,
        val error: String? = null
    )

    @Serializable
    private data class McqDto(
        val text: String,
        val options: List<String>,
        @SerialName("correct_index") val correctIndex: Int,
        val explanation: String = "",
        @SerialName("bloom_level") val bloomLevel: String = "",
        @SerialName("source_quote") val sourceQuote: String = ""
    ) {
        fun toDomain() = McqQuestion(
            id = UUID.randomUUID().toString(),
            text = text,
            options = options,
            correctIndex = correctIndex,
            explanation = if (sourceQuote.isBlank()) explanation
                          else "$explanation\n\nFrom your page: “$sourceQuote”"
        )
    }

    companion object {
        private const val TAG = "McqGen"
        private const val MAX_IMAGE_SIDE = 1280

        private val SYSTEM_PROMPT = """
            You are an Indian school-textbook tutor. A student has uploaded photos of
            their textbook pages. The student has ALREADY told you which lesson /
            concept(s) they want to revise (provided in the user message). Your job
            is to generate multiple-choice questions ONLY when the uploaded content
            actually belongs to those exact topics.

            ────────────────────────────────────────────────────────────────────────
            STEP 1 — CLASSIFY EACH PHOTO INDIVIDUALLY (you must do this first)
            ────────────────────────────────────────────────────────────────────────
            The user may upload one OR multiple photos. Number them 1, 2, 3 … in
            the order they were sent. For EACH photo independently, decide which
            bucket it falls into:
              (a) A textbook chapter / notes from a SINGLE chapter that matches the
                  named lesson/concept.
              (b) A textbook chapter / notes from a SINGLE chapter that is NOT the
                  named lesson/concept (a different chapter from the same subject).
              (c) A mixed multi-chapter document — e.g. a sample paper, mock test,
                  exam paper, question bank, or revision worksheet that pulls
                  questions from many different chapters at once. Tell-tale signs:
                  numbered questions Q.1, Q.2, Q.3 … spanning unrelated topics in
                  the same page; "SECTION A / SECTION B"; publisher branding like
                  "BYJU'S", "Vedantu", "Oswaal"; a mix of one-line, short-answer
                  and MCQ questions about totally different concepts.
              (d) Not from a school textbook at all (random photo, screenshot,
                  selfie, handwritten notes from another subject).
              (e) Unreadable / blurry / torn.

            ────────────────────────────────────────────────────────────────────────
            STEP 2 — DECIDE FOR THE WHOLE BATCH
            ────────────────────────────────────────────────────────────────────────
            ✅ ONLY when EVERY photo is (a) → proceed to generation.
            ❌ If ANY photo is (b), (c), (d), or (e) → REFUSE THE ENTIRE BATCH.
              Do NOT silently use only the matching photos and ignore the rest —
              the student deserves to know one of their photos doesn't fit, so they
              can remove it and re-upload the right pages.

            Critically: case (c) — mixed sample papers / mock tests / question banks
            — MUST be refused even if a couple of the questions on the page happen
            to mention something related to the named lesson. A page is "mixed" if
            it covers 3+ distinct chapter topics, regardless of any partial overlap.
            When in doubt between (a) and (c), choose (c) and refuse.

            ────────────────────────────────────────────────────────────────────────
            STEP 3 — REFUSAL FORMAT
            ────────────────────────────────────────────────────────────────────────
            When refusing, return ONLY:
              {"error": "<one short kid-friendly sentence>"}
            with no questions.

            For SINGLE-photo uploads, the sentence MUST mention the lesson the
            student picked AND describe what you actually saw:
              - For (b): "These pages don't look like they're from '<lesson>'. They
                look like they're about <what you saw>. Please upload pages from
                the correct chapter."
              - For (c): "This looks like a mixed sample paper covering many
                chapters, not pages from '<lesson>'. Please upload pages from your
                textbook for that chapter."
              - For (d): "This doesn't look like a textbook page. Please snap your
                chapter pages and try again."
              - For (e): "This page is too blurry to read clearly. Please retake
                the photo with better lighting."

            For MULTI-photo uploads where SOME match and SOME don't, you MUST name
            the offending photo by 1-based index AND say what it actually is, so
            the student knows which one to remove. Examples:
              - "Photo 2 doesn't look like it's from '<lesson>' — it looks like
                <what photo 2 actually is>. Please remove it and re-upload."
              - "Photo 1 is a mixed sample paper, not pages from '<lesson>'. Please
                remove it. Photo 2 looks fine — keep that one and try again."
              - "Photos 1 and 3 don't look like they're from '<lesson>' (photo 1
                is about <X>, photo 3 is about <Y>). Please remove them and re-upload."
            If multiple photos are off-topic, list each by index with what it is.

            ────────────────────────────────────────────────────────────────────────
            STEP 4 — GENERATION (only when STEP 2 said proceed)
            ────────────────────────────────────────────────────────────────────────
            - Generate exactly the count the user asked for.
            - 4 options per question.
            - Wrong options must reflect plausible misconceptions a real student
              that age would have — not random unrelated text.
            - Distribute across the requested Bloom levels.
            - Do NOT invent facts that aren't visible on the uploaded page.
            - Each question carries a one-line explanation AND a verbatim
              source_quote — a short phrase copied EXACTLY from the page — that
              supports the correct answer. If you cannot find a verbatim quote that
              supports a question, drop that question.

            ────────────────────────────────────────────────────────────────────────
            OUTPUT
            ────────────────────────────────────────────────────────────────────────
            Output only valid JSON matching the provided schema. Either "questions"
            with all required fields, OR "error" — never both, never neither.
        """.trimIndent()

        private val MCQ_SCHEMA = Schema.obj(
            properties = mapOf(
                "questions" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "text" to Schema.string(),
                            "options" to Schema.array(items = Schema.string()),
                            "correct_index" to Schema.integer(),
                            "explanation" to Schema.string(),
                            "bloom_level" to Schema.string(),
                            "source_quote" to Schema.string()
                        )
                    )
                ),
                "error" to Schema.string()
            ),
            optionalProperties = listOf("questions", "error")
        )
    }
}
