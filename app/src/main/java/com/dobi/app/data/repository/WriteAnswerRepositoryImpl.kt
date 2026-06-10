package com.dobi.app.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.dobi.app.domain.model.AnswerGrade
import com.dobi.app.domain.model.GradingResult
import com.dobi.app.domain.model.SubjectiveQuestion
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.HarmBlockThreshold
import com.google.firebase.ai.type.HarmCategory
import com.google.firebase.ai.type.SafetySetting
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
class WriteAnswerRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : WriteAnswerRepository {

    private val json = Json { ignoreUnknownKeys = true }

    private val questionGenModel: GenerativeModel by lazy {
        buildModel("gemini-2.5-flash", QUESTION_GEN_SCHEMA, QUESTION_GEN_SYSTEM)
    }
    private val gradingModel: GenerativeModel by lazy {
        buildModel("gemini-2.5-flash", GRADING_SCHEMA, GRADING_SYSTEM)
    }
    private val gradingFallback: GenerativeModel by lazy {
        buildModel("gemini-2.5-flash-lite", GRADING_SCHEMA, GRADING_SYSTEM)
    }

    private fun buildModel(name: String, schema: Schema, sysPrompt: String): GenerativeModel =
        Firebase.ai(backend = GenerativeBackend.vertexAI()).generativeModel(
            modelName = name,
            generationConfig = generationConfig {
                temperature = 0.15f
                responseMimeType = "application/json"
                responseSchema = schema
            },
            safetySettings = SAFETY_SETTINGS,
            systemInstruction = content { text(sysPrompt) }
        )

    override suspend fun generateQuestions(
        scopeText: String,
        subject: String,
        grade: Int,
        targetCount: Int
    ): Result<List<SubjectiveQuestion>> = withContext(Dispatchers.IO) {
        runCatching {
            val prompt = """
                $scopeText
                Subject: $subject
                Grade: $grade

                Generate exactly $targetCount short subjective questions covering the
                topic(s) above. Mix difficulty: roughly half short-answer (1–2 marks)
                and half slightly deeper (3 marks). Avoid pure recall — prefer
                "explain", "why", "how", "give an example".
            """.trimIndent()
            val raw = questionGenModel.generateContent(content { text(prompt) }).text
                ?: error("Empty response from Gemini")
            val parsed = json.decodeFromString<QuestionGenResponse>(raw)
            if (parsed.error != null) error(parsed.error)
            parsed.questions.orEmpty().map {
                SubjectiveQuestion(
                    id = UUID.randomUUID().toString(),
                    text = it.text,
                    expectedKeywords = it.expectedKeywords,
                    maxMarks = it.maxMarks
                )
            }.also { require(it.isNotEmpty()) { "Model returned no questions" } }
        }
    }

    private val imageQuestionGenModel: GenerativeModel by lazy {
        buildModel("gemini-2.5-flash", QUESTION_GEN_SCHEMA, IMAGE_QUESTION_GEN_SYSTEM)
    }

    override suspend fun generateQuestionsFromImages(
        scopeText: String,
        subject: String,
        grade: Int,
        photoUris: List<String>,
        targetCount: Int
    ): Result<List<SubjectiveQuestion>> = withContext(Dispatchers.IO) {
        runCatching {
            require(photoUris.isNotEmpty()) { "No photos provided" }
            val bitmaps = photoUris.mapNotNull { decodeBitmap(it) }
            require(bitmaps.isNotEmpty()) { "Couldn't read uploaded photos" }
            val prompt = """
                $scopeText
                Subject: $subject
                Grade: $grade

                The student has uploaded photos of their textbook / question pages
                for the lesson above. Apply the photo-classification rule from the
                system prompt:
                  - If every photo is on-topic for the named lesson, generate
                    exactly $targetCount short subjective questions ANCHORED in
                    what's actually visible on those pages.
                  - Otherwise refuse with {"error": "<kid-friendly sentence
                    naming the topic and what you saw>"}.

                Mix difficulty: roughly half short-answer (1–2 marks) and half
                slightly deeper (3 marks). Prefer "explain", "why", "how",
                "give an example".
            """.trimIndent()
            val input = content {
                text(prompt)
                bitmaps.forEach { image(it) }
            }
            val raw = imageQuestionGenModel.generateContent(input).text
                ?: error("Empty response from Gemini")
            val parsed = json.decodeFromString<QuestionGenResponse>(raw)
            if (parsed.error != null) error(parsed.error)
            parsed.questions.orEmpty().map {
                SubjectiveQuestion(
                    id = UUID.randomUUID().toString(),
                    text = it.text,
                    expectedKeywords = it.expectedKeywords,
                    maxMarks = it.maxMarks
                )
            }.also { require(it.isNotEmpty()) { "Model returned no questions" } }
        }
    }

    override suspend fun gradeStructured(
        scopeText: String,
        subject: String,
        grade: Int,
        questions: List<SubjectiveQuestion>,
        photoUrisByQuestionId: Map<String, List<String>>
    ): Result<GradingResult> = withContext(Dispatchers.IO) {
        runCatching {
            val parts = mutableListOf<ContentPart>()
            val sb = StringBuilder()
            sb.appendLine(scopeText)
            sb.appendLine("Subject: $subject  Grade: $grade")
            sb.appendLine()
            sb.appendLine("MODE: structured. Below are ${questions.size} questions.")
            sb.appendLine("For each question I'll show the question, then the student's")
            sb.appendLine("answer photo(s). Grade each one. If a question has no photo, mark")
            sb.appendLine("it isAttempted=false.")
            questions.forEachIndexed { i, q ->
                sb.appendLine()
                sb.appendLine("Q${i + 1} (id=${q.id}, max_marks=${q.maxMarks}):")
                sb.appendLine(q.text)
                val uris = photoUrisByQuestionId[q.id].orEmpty()
                if (uris.isEmpty()) {
                    sb.appendLine("[No photo uploaded — mark isAttempted=false.]")
                } else {
                    sb.appendLine("[Student answer photo(s) for Q${i + 1} follow.]")
                    uris.mapNotNull { decodeBitmap(it) }.forEach {
                        parts += ContentPart.Text(sb.toString())
                        sb.clear()
                        parts += ContentPart.Image(it)
                    }
                }
            }
            if (sb.isNotEmpty()) parts += ContentPart.Text(sb.toString())

            val raw = generateWithResilience(parts)
            parseGrading(raw, questions.map { it.id }, structured = true)
        }
    }

    override suspend fun gradeFreeform(
        scopeText: String,
        subject: String,
        grade: Int,
        photoUris: List<String>
    ): Result<GradingResult> = withContext(Dispatchers.IO) {
        runCatching {
            require(photoUris.isNotEmpty()) { "No photos provided" }
            val bitmaps = photoUris.mapNotNull { decodeBitmap(it) }
            require(bitmaps.isNotEmpty()) { "Couldn't read uploaded photos" }

            val text = """
                $scopeText
                Subject: $subject  Grade: $grade

                MODE: freeform-notebook. The student practised in their own notebook
                and uploaded photo(s) of their answers. Find each answer the student
                wrote, infer what question they were answering (or what concept the
                answer covers), and grade it. Return one entry per detected answer in
                the "grades" array. Use UUID-like strings for question_id.

                If the notebook page is NOT about the topic(s) above (case b/c/d/e
                from the system prompt), set is_on_topic_overall=false, give a short
                off_topic_reason, and return grades=[].
            """.trimIndent()

            val parts = mutableListOf<ContentPart>(ContentPart.Text(text))
            bitmaps.forEach { parts += ContentPart.Image(it) }

            val raw = generateWithResilience(parts)
            parseGrading(raw, expectedIds = emptyList(), structured = false)
        }
    }

    private fun parseGrading(
        raw: String,
        expectedIds: List<String>,
        structured: Boolean
    ): GradingResult {
        val parsed = json.decodeFromString<GradingResponse>(raw)
        if (parsed.error != null) error(parsed.error)
        if (parsed.isOnTopicOverall == false) {
            return GradingResult(
                isOnTopicOverall = false,
                offTopicReason = parsed.offTopicReason.orEmpty(),
                grades = emptyList()
            )
        }
        val grades = parsed.grades.orEmpty().map { it.toDomain() }
        // For structured mode, fill missing question-ids as not attempted.
        val final = if (structured) {
            val byId = grades.associateBy { it.questionId }
            expectedIds.map { qid ->
                byId[qid] ?: AnswerGrade(
                    questionId = qid,
                    isAttempted = false,
                    isOnTopic = true,
                    score = 0f,
                    isCorrect = false,
                    softVerdict = "Not yet answered",
                    strictFeedback = "No answer photo uploaded."
                )
            }
        } else grades
        return GradingResult(
            isOnTopicOverall = true,
            grades = final
        )
    }

    private suspend fun generateWithResilience(parts: List<ContentPart>): String {
        val backoffsMs = longArrayOf(0, 1500, 4000)
        var lastError: Throwable? = null
        val builtContent = content {
            parts.forEach {
                when (it) {
                    is ContentPart.Text -> text(it.value)
                    is ContentPart.Image -> image(it.bitmap)
                }
            }
        }
        for (attempt in backoffsMs.indices) {
            if (backoffsMs[attempt] > 0) delay(backoffsMs[attempt])
            try {
                val r = gradingModel.generateContent(builtContent).text
                if (!r.isNullOrBlank()) return r
                error("Empty response from Gemini")
            } catch (t: Throwable) {
                lastError = t
                Log.w(TAG, "Primary attempt ${attempt + 1} failed: ${t.message}")
                if (!isRetryable(t)) throw t
            }
        }
        try {
            val r = gradingFallback.generateContent(builtContent).text
            if (!r.isNullOrBlank()) return r
        } catch (t: Throwable) { lastError = t }
        throw lastError ?: IllegalStateException("Generation failed")
    }

    private fun isRetryable(t: Throwable): Boolean {
        val msg = (t.message ?: "").lowercase()
        return "unavailable" in msg || "overloaded" in msg || "503" in msg ||
            "high demand" in msg || "deadline" in msg || ("timeout" in msg && "google" in msg)
    }

    private fun decodeBitmap(uriString: String): Bitmap? = runCatching {
        val opts = BitmapFactory.Options().apply { inSampleSize = computeSampleSize(uriString) }
        context.contentResolver.openInputStream(Uri.parse(uriString)).use {
            BitmapFactory.decodeStream(it, null, opts)
        }
    }.getOrNull()

    private fun computeSampleSize(uriString: String): Int = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(Uri.parse(uriString)).use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        val maxSide = maxOf(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
        var sample = 1
        while (maxSide / sample > 1280) sample *= 2
        sample
    }.getOrDefault(1)

    private sealed class ContentPart {
        data class Text(val value: String) : ContentPart()
        data class Image(val bitmap: Bitmap) : ContentPart()
    }

    @Serializable
    private data class QuestionGenResponse(
        val questions: List<QuestionDto>? = null,
        val error: String? = null
    )

    @Serializable
    private data class QuestionDto(
        val text: String,
        @SerialName("expected_keywords") val expectedKeywords: List<String> = emptyList(),
        @SerialName("max_marks") val maxMarks: Int = 2
    )

    @Serializable
    private data class GradingResponse(
        @SerialName("is_on_topic_overall") val isOnTopicOverall: Boolean? = null,
        @SerialName("off_topic_reason") val offTopicReason: String? = null,
        val grades: List<GradeDto>? = null,
        val error: String? = null
    )

    @Serializable
    private data class GradeDto(
        @SerialName("question_id") val questionId: String,
        @SerialName("question_seen") val questionSeen: String = "",
        @SerialName("answer_seen") val answerSeen: String = "",
        @SerialName("is_attempted") val isAttempted: Boolean,
        @SerialName("is_on_topic") val isOnTopic: Boolean,
        val score: Float,
        @SerialName("is_correct") val isCorrect: Boolean,
        @SerialName("soft_verdict") val softVerdict: String,
        @SerialName("strict_feedback") val strictFeedback: String
    ) {
        fun toDomain() = AnswerGrade(
            questionId = questionId,
            questionSeen = questionSeen,
            answerSeen = answerSeen,
            isAttempted = isAttempted,
            isOnTopic = isOnTopic,
            score = score.coerceIn(0f, 1f),
            isCorrect = isCorrect,
            softVerdict = softVerdict,
            strictFeedback = strictFeedback
        )
    }

    companion object {
        private const val TAG = "WriteAns"

        private val SAFETY_SETTINGS = listOf(
            SafetySetting(HarmCategory.HARASSMENT, HarmBlockThreshold.BLOCK_ONLY_HIGH),
            SafetySetting(HarmCategory.HATE_SPEECH, HarmBlockThreshold.BLOCK_ONLY_HIGH),
            SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, HarmBlockThreshold.BLOCK_ONLY_HIGH),
            SafetySetting(HarmCategory.DANGEROUS_CONTENT, HarmBlockThreshold.BLOCK_ONLY_HIGH),
        )

        private val QUESTION_GEN_SYSTEM = """
            You are an Indian school-textbook tutor. Generate short subjective
            questions for the named lesson(s)/concept(s) at the named grade level.
            Avoid pure recall; prefer questions that ask the student to explain,
            compare, give an example, or apply the idea. Output only JSON matching
            the schema. If the topic input doesn't make sense, return
            {"error": "<reason>"}.
        """.trimIndent()

        private val IMAGE_QUESTION_GEN_SYSTEM = """
            You are an Indian school-textbook tutor. The student uploads photos
            of textbook pages or question pages and tells you which lesson they
            cover. Your job is to generate short subjective questions ONLY when
            the uploaded pages actually belong to that lesson.

            STEP 1 — Classify EACH photo independently:
              (a) textbook / notes / question page from the named lesson — proceed.
              (b) different chapter from the same subject — refuse.
              (c) mixed sample paper / mock test / question bank covering many
                  chapters — refuse even if a few items overlap with the lesson.
              (d) not from a textbook at all (random photo, screenshot, object) — refuse.
              (e) unreadable / blurry / torn — refuse.

            STEP 2 — Only when every photo is (a), generate the requested number
            of subjective questions, each grounded in something actually visible
            on the uploaded pages.

            On refusal, return ONLY:
              {"error": "<one short kid-friendly sentence that names the lesson
              AND describes what you saw — e.g. 'These pages don't look like
              they're from <lesson>; they look like a mixed sample paper.'>"}

            Output only JSON matching the schema. Either `questions` with all
            required fields, OR `error` — never both, never neither.
        """.trimIndent()

        private val GRADING_SYSTEM = """
            You are an Indian school-textbook tutor reviewing a child's handwritten
            answer(s) to subjective questions. The lesson/concept(s) being revised
            are given in the user message.

            STEP 1 — Decide if the uploaded photos are about the named topic(s).
            Use the same 5-bucket classification as in past prompts:
              (a) on-topic single chapter — proceed
              (b) different chapter — refuse with is_on_topic_overall=false and a
                  short off_topic_reason that names the topic AND what you saw.
              (c) mixed paper / mock test — refuse same way (mention "mixed paper").
              (d) not a textbook/notebook page — refuse same way (describe what you
                  see, e.g. "this looks like a handbag photo").
              (e) unreadable — refuse same way ("the writing is too faint to read").
            Return grades=[] in any refusal case.

            STEP 2 — When on-topic, GRADE each answer:
              - is_attempted: true if the student wrote anything at all for it.
              - is_on_topic: true if the answer is about the question (not random).
              - score: 0.0–1.0 based on correctness + completeness.
              - is_correct: true iff score >= 0.6.
              - soft_verdict: 5–10 kid-friendly words. Encouraging when right
                ("Spot on!", "Great explanation!"); kind when wrong ("Almost — try
                again", "Good try, missed the main idea"). NEVER "Wrong" or "Bad".
              - strict_feedback: 1 sentence honest assessment for parent dashboard.
              - question_seen / answer_seen: short verbatim snippets for traceability.

            Be strict on correctness, soft on tone. Output only valid JSON.
        """.trimIndent()

        private val QUESTION_GEN_SCHEMA = Schema.obj(
            properties = mapOf(
                "questions" to Schema.array(items = Schema.obj(
                    properties = mapOf(
                        "text" to Schema.string(),
                        "expected_keywords" to Schema.array(items = Schema.string()),
                        "max_marks" to Schema.integer()
                    )
                )),
                "error" to Schema.string()
            ),
            optionalProperties = listOf("questions", "error")
        )

        private val GRADING_SCHEMA = Schema.obj(
            properties = mapOf(
                "is_on_topic_overall" to Schema.boolean(),
                "off_topic_reason" to Schema.string(),
                "grades" to Schema.array(items = Schema.obj(
                    properties = mapOf(
                        "question_id" to Schema.string(),
                        "question_seen" to Schema.string(),
                        "answer_seen" to Schema.string(),
                        "is_attempted" to Schema.boolean(),
                        "is_on_topic" to Schema.boolean(),
                        "score" to Schema.double(),
                        "is_correct" to Schema.boolean(),
                        "soft_verdict" to Schema.string(),
                        "strict_feedback" to Schema.string()
                    )
                )),
                "error" to Schema.string()
            ),
            optionalProperties = listOf("off_topic_reason", "grades", "error", "is_on_topic_overall")
        )
    }
}
