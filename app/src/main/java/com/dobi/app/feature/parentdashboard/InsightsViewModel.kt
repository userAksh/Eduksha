package com.dobi.app.feature.parentdashboard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.dobi.app.core.navigation.Screen
import com.dobi.app.data.repository.ActivityLogRepository
import com.dobi.app.data.repository.ActivityLogRepository.ActivityLogItem
import com.dobi.app.data.repository.ActivityLogRepository.Source
import com.dobi.app.domain.model.Concept
import com.dobi.app.domain.model.ParentTaskType
import com.dobi.app.domain.repository.ChildRepository
import com.dobi.app.domain.repository.ConceptRepository
import com.dobi.app.domain.repository.ParentTaskRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

// ── UI state shape ───────────────────────────────────────────────────────────

enum class InsightsRange(val label: String, val days: Int) {
    WEEK("1 Week", 7),
    MONTH("1 Month", 30),
}

enum class HeadlineTone { POSITIVE, INFO }

data class HeadlineInsight(
    val text: String = "",
    val tone: HeadlineTone = HeadlineTone.INFO,
)

data class StatTileData(
    val value: String = "—",
    val delta: String? = null,
    val deltaPositive: Boolean = true,
)

data class ImprovedHighlight(
    val subject: String,
    val deltaPct: Int,
    val prevAccuracy: Int,
    val currAccuracy: Int,
    val sparkline: List<Float>,
)

data class AttentionHighlight(
    val subject: String,
    val accuracyPct: Int?,
    val tagline: String,
    val sparkline: List<Float>,
)

data class SubjectStat(
    val subject: String,
    val attempts: Int,
    val accuracyPct: Int?,
    val deltaPct: Int?,
    val lastRevisedLabel: String,
    val lastRevisedDays: Int?,
)

data class DailyBreakdown(
    val dayLabel: String,
    val isToday: Boolean,
    val revision: Int,
    val homework: Int,
    val uploadOther: Int,
) {
    val total: Int = revision + homework + uploadOther
}

/**
 * A subject + scope (unit or concept) the engine considers worth practicing
 * next. Surfaced to the UI so the parent can one-tap-assign a quiz or
 * subjective task on this topic. Built only when there's enough provenance
 * in the activity log to construct a real ParentTask scope.
 */
data class ActionableTopic(
    val subject: String,
    val topicName: String,
    val unitId: String,
    val unitName: String,
    val conceptId: String,
    val conceptName: String,
    val isUnitScope: Boolean,
    val reason: String,
)

data class AssignTaskEvent(
    val ok: Boolean,
    val message: String,
)

/**
 * Curriculum slice scoped to the child's grade & board. Used both as the
 * fallback when the activity log can't ground a topic, and as the source
 * for the "Change topic" picker in the Assign Practice sheet.
 */
data class CurriculumIndex(
    val isLoaded: Boolean = false,
    val subjects: List<SubjectInfo> = emptyList(),
    val unitsBySubject: Map<String, List<UnitOption>> = emptyMap(),
) {
    fun unitsFor(subject: String): List<UnitOption> =
        unitsBySubject[subject.trim().lowercase()].orEmpty()
}

data class SubjectInfo(
    val name: String,
    val color: String,
    val icon: String,
)

data class UnitOption(
    val unitId: String,
    val unitName: String,
    val subject: String,
    val subjectColor: String,
    val subjectIcon: String,
) {
    fun toActionable(reason: String): ActionableTopic = ActionableTopic(
        subject = subject,
        topicName = unitName,
        unitId = unitId,
        unitName = unitName,
        conceptId = "",
        conceptName = "",
        isUnitScope = true,
        reason = reason,
    )
}

data class InsightsUiState(
    val isLoading: Boolean = true,
    val childFirstName: String = "",
    val childGrade: Int = 0,
    val childBoard: String = "",
    val childGender: String = "boy",
    val range: InsightsRange = InsightsRange.WEEK,
    val dateRangeLabel: String = "",
    val headline: HeadlineInsight = HeadlineInsight(),
    val daysActive: StatTileData = StatTileData(),
    val totalAttempts: StatTileData = StatTileData(),
    val avgAccuracy: StatTileData = StatTileData(),
    val revisionSessions: StatTileData = StatTileData(),
    val mostImproved: ImprovedHighlight? = null,
    val needsAttention: AttentionHighlight? = null,
    val subjectBreakdown: List<SubjectStat> = emptyList(),
    val timeline: List<DailyBreakdown> = emptyList(),
    val recommendation: String = "",
    val actionableTopic: ActionableTopic? = null,
    val curriculum: CurriculumIndex = CurriculumIndex(),
    val isAssigning: Boolean = false,
)

// ── ViewModel ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InsightsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val auth: FirebaseAuth,
    private val childRepository: ChildRepository,
    private val parentTaskRepository: ParentTaskRepository,
    private val conceptRepository: ConceptRepository,
    activityLogRepository: ActivityLogRepository,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<Screen.Insights>()
    private val childId: String = route.childId

    private val childProfile = MutableStateFlow(ChildProfile())
    private val range = MutableStateFlow(InsightsRange.WEEK)
    private val isAssigning = MutableStateFlow(false)
    private val itemsFlow = activityLogRepository.observeForChild(childId)

    // Curriculum for the child's grade/board. Re-fetched only when the
    // child's profile (specifically grade or board) changes.
    private val curriculumFlow = childProfile
        .map { it.grade to it.board }
        .distinctUntilChanged()
        .flatMapLatest { (grade, board) ->
            if (grade <= 0 || board.isBlank()) flowOf(CurriculumIndex())
            else conceptRepository.getConcepts(grade, board)
                .map(::buildCurriculumIndex)
        }

    private val _assignEvents = MutableSharedFlow<AssignTaskEvent>(extraBufferCapacity = 1)
    val assignEvents = _assignEvents.asSharedFlow()

    val uiState = combine(
        itemsFlow,
        childProfile,
        range,
        isAssigning,
        curriculumFlow,
    ) { items, profile, range, assigning, curriculum ->
        build(items, profile, range, curriculum).copy(isAssigning = assigning)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = InsightsUiState(),
    )

    init { loadChildProfile() }

    fun selectRange(r: InsightsRange) { range.value = r }

    /**
     * Create a parent-assigned task on [topic] of [type]. The task lands in
     * the child's task list as ASSIGNED (skips the REQUESTED stage that's
     * used when the flow starts from the child). [customNote] is required
     * for [ParentTaskType.CUSTOM] and ignored for the other three types.
     */
    fun assignTask(
        topic: ActionableTopic,
        type: ParentTaskType,
        customNote: String = "",
    ) {
        if (isAssigning.value) return
        val parentId = auth.currentUser?.uid.orEmpty()
        if (parentId.isBlank() || childId.isBlank()) {
            _assignEvents.tryEmit(AssignTaskEvent(false, "Couldn't identify the parent or child."))
            return
        }
        if (type == ParentTaskType.CUSTOM && customNote.trim().isEmpty()) {
            _assignEvents.tryEmit(AssignTaskEvent(false, "Add a short instruction first."))
            return
        }
        isAssigning.value = true
        viewModelScope.launch {
            val firstName = childProfile.value.firstName.ifBlank { "your child" }
            val result = parentTaskRepository.assignDirectlyByParent(
                parentId = parentId,
                childId = childId,
                unitId = topic.unitId,
                unitName = topic.unitName,
                conceptId = topic.conceptId,
                conceptName = topic.conceptName,
                isUnitScope = topic.isUnitScope,
                subject = topic.subject,
                subjectColor = "",
                subjectIcon = "",
                type = type,
                customNote = if (type == ParentTaskType.CUSTOM) customNote.trim() else "",
                mcqCount = if (topic.isUnitScope) 10 else 5,
            )
            isAssigning.value = false
            _assignEvents.tryEmit(
                if (result.isSuccess) AssignTaskEvent(
                    ok = true,
                    message = "Task assigned to $firstName.",
                ) else AssignTaskEvent(
                    ok = false,
                    message = "Couldn't assign the task. Try again.",
                )
            )
        }
    }

    private fun loadChildProfile() {
        val uid = auth.currentUser?.uid ?: return
        if (childId.isBlank()) return
        viewModelScope.launch {
            childRepository.getChild(uid, childId).onSuccess { c ->
                childProfile.value = ChildProfile(
                    firstName = c.name.substringBefore(" ").ifBlank { c.name },
                    grade = c.grade,
                    board = c.board,
                    gender = c.gender,
                )
            }
        }
    }

    private fun build(
        items: List<ActivityLogItem>,
        profile: ChildProfile,
        range: InsightsRange,
        curriculum: CurriculumIndex,
    ): InsightsUiState {
        val now = System.currentTimeMillis()
        val windowMs = range.days * DAY_MS
        val currentStart = now - windowMs
        val priorStart = now - 2L * windowMs

        val thisItems = items.filter { it.timestamp in currentStart..now }
        val priorItems = items.filter { it.timestamp in priorStart..currentStart }

        val perSubjectThis = aggregateBySubject(thisItems)
        val perSubjectPrior = aggregateBySubject(priorItems)

        val subjectBreakdown = perSubjectThis.values
            .map { agg ->
                val priorAcc = perSubjectPrior[normalize(agg.subject)]?.accuracyPctOrNull()
                val curAcc = agg.accuracyPctOrNull()
                val delta = if (priorAcc != null && curAcc != null) curAcc - priorAcc else null
                val lastRev = lastRevisionForSubject(items, agg.subject, now)
                val lastRevDays = lastRev?.let { ((now - it) / DAY_MS).toInt() }
                SubjectStat(
                    subject = agg.subject.ifBlank { "Other" },
                    attempts = agg.attempts,
                    accuracyPct = curAcc,
                    deltaPct = delta,
                    lastRevisedLabel = lastRevisedLabel(lastRev, now),
                    lastRevisedDays = lastRevDays,
                )
            }
            .sortedWith(
                compareByDescending<SubjectStat> { it.attempts }
                    .thenBy { it.accuracyPct ?: Int.MAX_VALUE }
            )

        val daysActiveCount = thisItems
            .map { dayKey(it.timestamp) }.toSet().size
            .coerceIn(0, range.days)
        val priorDaysActive = priorItems
            .map { dayKey(it.timestamp) }.toSet().size

        val totalAttempts = thisItems.size
        val priorTotal = priorItems.size

        val totalCorrect = thisItems.sumOf { it.score?.correct ?: 0 }
        val totalScored = thisItems.sumOf { it.score?.total ?: 0 }
        val curAccuracy = if (totalScored > 0)
            ((totalCorrect.toFloat() / totalScored) * 100f).toInt() else null
        val priorCorrect = priorItems.sumOf { it.score?.correct ?: 0 }
        val priorScored = priorItems.sumOf { it.score?.total ?: 0 }
        val priorAccuracy = if (priorScored > 0)
            ((priorCorrect.toFloat() / priorScored) * 100f).toInt() else null
        val accDelta = if (curAccuracy != null && priorAccuracy != null)
            curAccuracy - priorAccuracy else null

        val curRev = thisItems.count(::isRevisionSession)
        val priorRev = priorItems.count(::isRevisionSession)

        val rangeWord = if (range == InsightsRange.WEEK) "week" else "month"
        val priorWord = "last $rangeWord"

        val daysActive = StatTileData(
            value = "$daysActiveCount/${range.days}",
            delta = signedDelta(daysActiveCount - priorDaysActive, priorWord),
            deltaPositive = daysActiveCount >= priorDaysActive,
        )
        val totalAttemptsStat = StatTileData(
            value = "$totalAttempts",
            delta = signedDelta(totalAttempts - priorTotal, priorWord),
            deltaPositive = totalAttempts >= priorTotal,
        )
        val avgAccuracyStat = StatTileData(
            value = curAccuracy?.let { "$it%" } ?: "—",
            delta = accDelta?.let { signedPercent(it, priorWord) },
            deltaPositive = (accDelta ?: 0) >= 0,
        )
        val revisionSessionsStat = StatTileData(
            value = "$curRev",
            delta = signedDelta(curRev - priorRev, priorWord),
            deltaPositive = curRev >= priorRev,
        )

        val mostImproved = subjectBreakdown
            .filter { (it.deltaPct ?: 0) >= 5 && it.accuracyPct != null }
            .maxByOrNull { it.deltaPct!! }
            ?.let { stat ->
                val priorAcc = (stat.accuracyPct!! - stat.deltaPct!!).coerceIn(0, 100)
                ImprovedHighlight(
                    subject = stat.subject,
                    deltaPct = stat.deltaPct,
                    prevAccuracy = priorAcc,
                    currAccuracy = stat.accuracyPct,
                    sparkline = sparklineForSubject(items, stat.subject, currentStart, now, ascending = true),
                )
            }

        val needsAttention = run {
            val lowAcc = subjectBreakdown
                .filter { (it.accuracyPct ?: 100) < 65 && it.attempts >= 3 }
                .minByOrNull { it.accuracyPct!! }
            val staleSubject = subjectStaleness(items, now)
                .maxByOrNull { it.second }
                ?.takeIf { it.second > 7 }
            when {
                lowAcc != null -> AttentionHighlight(
                    subject = lowAcc.subject,
                    accuracyPct = lowAcc.accuracyPct,
                    tagline = "Lowest accuracy this $rangeWord",
                    sparkline = sparklineForSubject(items, lowAcc.subject, currentStart, now, ascending = false),
                )
                staleSubject != null -> AttentionHighlight(
                    subject = staleSubject.first,
                    accuracyPct = null,
                    tagline = "Not revised in ${staleSubject.second} days",
                    sparkline = sparklineForSubject(items, staleSubject.first, currentStart, now, ascending = false),
                )
                else -> null
            }
        }

        val timeline = buildDailyTimeline(items, now, range.days)

        return InsightsUiState(
            isLoading = false,
            childFirstName = profile.firstName,
            childGrade = profile.grade,
            childBoard = profile.board,
            childGender = profile.gender,
            range = range,
            dateRangeLabel = formatDateRange(now, range.days),
            headline = computeHeadline(
                range = range,
                profile = profile,
                totalAttempts = totalAttempts,
                daysActive = daysActiveCount,
                overallDelta = accDelta,
                mostImproved = mostImproved,
                weakSubject = subjectBreakdown.firstOrNull {
                    (it.accuracyPct ?: 100) < 60 && it.attempts >= 3
                },
            ),
            daysActive = daysActive,
            totalAttempts = totalAttemptsStat,
            avgAccuracy = avgAccuracyStat,
            revisionSessions = revisionSessionsStat,
            mostImproved = mostImproved,
            needsAttention = needsAttention,
            subjectBreakdown = subjectBreakdown,
            timeline = timeline,
            recommendation = computeRecommendation(
                firstName = profile.firstName.ifBlank { "your child" },
                rangeWord = rangeWord,
                weakSubject = subjectBreakdown.firstOrNull {
                    (it.accuracyPct ?: 100) < 60 && it.attempts >= 3
                },
                revisionSessions = curRev,
                daysActive = daysActiveCount,
            ),
            actionableTopic = pickActionableTopic(items, subjectBreakdown, now, curriculum),
            curriculum = curriculum,
        )
    }

    private fun buildCurriculumIndex(concepts: List<Concept>): CurriculumIndex {
        if (concepts.isEmpty()) return CurriculumIndex(isLoaded = true)

        val bySubject = concepts.groupBy { it.subject.trim() }
            .filterKeys { it.isNotBlank() }

        val subjects = bySubject.entries
            .map { (subject, list) ->
                val first = list.first()
                SubjectInfo(name = subject, color = first.subjectColor, icon = first.subjectIcon)
            }
            .sortedBy { it.name.lowercase() }

        val unitsBySubject = bySubject.mapValues { (subject, list) ->
            list.groupBy { it.unitId }
                .filterKeys { it.isNotBlank() }
                .map { (unitId, conceptsInUnit) ->
                    val anchor = conceptsInUnit.minByOrNull { it.order } ?: conceptsInUnit.first()
                    UnitOption(
                        unitId = unitId,
                        unitName = anchor.unit.ifBlank { anchor.name },
                        subject = subject,
                        subjectColor = anchor.subjectColor,
                        subjectIcon = anchor.subjectIcon,
                    )
                }
                .sortedBy { it.unitName.lowercase() }
        }.mapKeys { (k, _) -> k.lowercase() }

        return CurriculumIndex(
            isLoaded = true,
            subjects = subjects,
            unitsBySubject = unitsBySubject,
        )
    }

    /**
     * Find a subject + scope the parent can act on. Walks a four-tier
     * fallback so the Assign button always has a sensible default to show:
     *
     *   1. Weak subject (accuracy < 65%, >= 3 scored attempts) grounded
     *      in a recent quiz / scope-bearing upload.
     *   2. Stale subject (not revised in 5+ days) grounded in a recent item.
     *   3. Same priority order as 1 & 2, but if the recent item lacks an
     *      extractable scope, anchor the topic in the curriculum's first
     *      chapter for that subject.
     *   4. Pure curriculum fallback — first subject the child has touched,
     *      first chapter from the curriculum; or if no activity at all,
     *      the first subject in the curriculum index.
     *
     * Returns null only when curriculum hasn't loaded yet AND there's no
     * extractable scope in activity — a transient state.
     */
    private fun pickActionableTopic(
        items: List<ActivityLogItem>,
        subjects: List<SubjectStat>,
        now: Long,
        curriculum: CurriculumIndex,
    ): ActionableTopic? {
        val weak = subjects.firstOrNull {
            (it.accuracyPct ?: 100) < 65 && it.attempts >= 3
        }
        val byLastRevised = subjects
            .filter { it.lastRevisedDays != null && it.lastRevisedDays >= 5 }
            .sortedByDescending { it.lastRevisedDays }
        val candidates = listOfNotNull(weak) + byLastRevised

        // Tier 1 & 2 — try to ground the candidate in real activity scope.
        for (stat in candidates) {
            val anchor = items
                .asSequence()
                .filter { normalize(it.subject) == normalize(stat.subject) && isRevisionSession(it) }
                .sortedByDescending { it.timestamp }
                .mapNotNull { item -> extractScope(item)?.let { it to item } }
                .firstOrNull()
                ?: items
                    .asSequence()
                    .filter { normalize(it.subject) == normalize(stat.subject) }
                    .sortedByDescending { it.timestamp }
                    .mapNotNull { item -> extractScope(item)?.let { it to item } }
                    .firstOrNull()

            val reason = reasonFor(stat)
            if (anchor != null) {
                val (scope, anchorItem) = anchor
                return ActionableTopic(
                    subject = stat.subject,
                    topicName = anchorItem.title.ifBlank { stat.subject },
                    unitId = if (scope.isUnitScope) scope.id else "",
                    unitName = if (scope.isUnitScope) anchorItem.title.ifBlank { stat.subject } else "",
                    conceptId = if (scope.isUnitScope) "" else scope.id,
                    conceptName = if (scope.isUnitScope) "" else anchorItem.title.ifBlank { stat.subject },
                    isUnitScope = scope.isUnitScope,
                    reason = reason,
                )
            }
            // Tier 3 — fall back to curriculum's first chapter for this subject.
            curriculum.unitsFor(stat.subject).firstOrNull()?.let {
                return it.toActionable(reason)
            }
        }

        // Tier 4 — pure curriculum default. Prefer a subject the child has
        // touched this window, else the first subject the curriculum offers.
        val touchedSubject = subjects.firstOrNull()?.subject
        val pick = touchedSubject?.let { curriculum.unitsFor(it).firstOrNull() }
            ?: curriculum.subjects.firstOrNull()
                ?.let { curriculum.unitsFor(it.name).firstOrNull() }
        if (pick != null) {
            return pick.toActionable("Recommended from the ${pick.subject} chapters.")
        }

        // Tier 5 — curriculum is empty for the child's grade. Fall back to a
        // synthetic topic so the Assign sub-card is still actionable; only
        // CUSTOM task type will work without curriculum scope, but at least
        // the parent isn't stuck on "Finding a topic to assign…".
        val syntheticSubject = touchedSubject?.takeIf { it.isNotBlank() } ?: "General"
        return ActionableTopic(
            subject = syntheticSubject,
            topicName = "Daily practice",
            unitId = "",
            unitName = "Daily practice",
            conceptId = "",
            conceptName = "",
            isUnitScope = true,
            reason = "Send a quick custom practice prompt for today.",
        )
    }

    private fun reasonFor(stat: SubjectStat): String = when {
        (stat.accuracyPct ?: 100) < 65 && stat.attempts >= 3 ->
            "Lowest accuracy (${stat.accuracyPct}%) — a quick revision will help."
        stat.lastRevisedDays != null && stat.lastRevisedDays >= 5 ->
            "Last revised ${stat.lastRevisedDays} days ago."
        else -> "Worth a quick refresher."
    }

    private data class Scope(val id: String, val isUnitScope: Boolean)

    private fun extractScope(item: ActivityLogItem): Scope? = when (val d = item.detail) {
        is ActivityLogRepository.DetailRoute.QuizMcq -> when {
            d.unitId.isNotBlank() -> Scope(d.unitId, isUnitScope = true)
            d.conceptId.isNotBlank() -> Scope(d.conceptId, isUnitScope = false)
            else -> null
        }
        is ActivityLogRepository.DetailRoute.Upload -> when {
            d.groupKey.startsWith("unit:") ->
                Scope(d.groupKey.removePrefix("unit:"), isUnitScope = true)
            d.groupKey.startsWith("concept:") ->
                Scope(d.groupKey.removePrefix("concept:"), isUnitScope = false)
            else -> null
        }
        else -> null
    }

    // ── Subject aggregation ──────────────────────────────────────────────────

    private fun aggregateBySubject(items: List<ActivityLogItem>): Map<String, Aggregator> {
        val map = HashMap<String, Aggregator>()
        items.forEach { item ->
            val key = normalize(item.subject)
            val agg = map.getOrPut(key) { Aggregator(subject = item.subject.trim()) }
            agg.attempts += 1
            item.score?.let { s ->
                agg.correct += s.correct
                agg.total += s.total
            }
        }
        return map
    }

    private fun lastRevisionForSubject(
        items: List<ActivityLogItem>,
        subject: String,
        now: Long,
    ): Long? = items
        .asSequence()
        .filter { normalize(it.subject) == normalize(subject) && isRevisionSession(it) }
        .maxOfOrNull { it.timestamp }
        ?.takeIf { it in (now - 60L * DAY_MS)..now }

    private fun lastRevisedLabel(ts: Long?, now: Long): String {
        if (ts == null) return "No revision"
        val days = ((now - ts) / DAY_MS).toInt()
        return when {
            days <= 0 -> "Today"
            days == 1 -> "Yesterday"
            else -> "$days days ago"
        }
    }

    private fun subjectStaleness(items: List<ActivityLogItem>, now: Long): List<Pair<String, Int>> {
        val lookback60d = now - 60L * DAY_MS
        return items
            .filter { it.subject.isNotBlank() && it.timestamp >= lookback60d }
            .groupBy { it.subject.trim() }
            .map { (subject, entries) ->
                subject to ((now - entries.maxOf { it.timestamp }) / DAY_MS).toInt()
            }
    }

    /**
     * Builds an N-point sparkline (0..1 normalised) for a subject's accuracy
     * across the window. Falls back to a stylised line when there isn't enough
     * scored data to draw something real.
     */
    private fun sparklineForSubject(
        items: List<ActivityLogItem>,
        subject: String,
        startMs: Long,
        endMs: Long,
        ascending: Boolean,
    ): List<Float> {
        val subjectKey = normalize(subject)
        val scored = items
            .asSequence()
            .filter { normalize(it.subject) == subjectKey }
            .filter { it.timestamp in startMs..endMs }
            .filter { (it.score?.total ?: 0) > 0 }
            .sortedBy { it.timestamp }
            .toList()
        if (scored.size < 2) {
            // Decorative line — leftmost low, rightmost high (or vice versa).
            val n = 14
            return if (ascending) {
                (0 until n).map { 0.25f + 0.55f * (it / (n - 1f)) }
            } else {
                (0 until n).map { 0.65f - 0.35f * (it / (n - 1f)) }
            }
        }
        // Rolling accuracy: each point uses cumulative correct/total up to and
        // including that item — smoother than per-item accuracy.
        var c = 0; var t = 0
        return scored.map { item ->
            c += item.score?.correct ?: 0
            t += item.score?.total ?: 0
            if (t > 0) c.toFloat() / t else 0f
        }.toMutableList().also { list ->
            // Pad to at least 8 points so the line has visual length even
            // when there are only 2–3 data points.
            while (list.size < 8) list.add(0, list.first())
        }
    }

    // ── Timeline ─────────────────────────────────────────────────────────────

    private fun buildDailyTimeline(
        items: List<ActivityLogItem>,
        now: Long,
        days: Int,
    ): List<DailyBreakdown> {
        val todayKey = dayKey(now)
        return (0 until days).map { offset ->
            val ts = now - offset * DAY_MS
            DayBucket(key = dayKey(ts), label = shortDateLabel(ts), isToday = dayKey(ts) == todayKey)
        }.reversed().map { bucket ->
            val dayItems = items.filter { dayKey(it.timestamp) == bucket.key }
            DailyBreakdown(
                dayLabel = bucket.label,
                isToday = bucket.isToday,
                revision = dayItems.count(::isRevisionSession),
                homework = dayItems.count(::isHomework),
                uploadOther = dayItems.count(::isUploadOther),
            )
        }
    }

    // ── Headline + recommendation ────────────────────────────────────────────

    private fun computeHeadline(
        range: InsightsRange,
        profile: ChildProfile,
        totalAttempts: Int,
        daysActive: Int,
        overallDelta: Int?,
        mostImproved: ImprovedHighlight?,
        weakSubject: SubjectStat?,
    ): HeadlineInsight {
        val periodWord = if (range == InsightsRange.WEEK) "this week" else "this month"
        val priorWord = if (range == InsightsRange.WEEK) "last week" else "the previous 30 days"
        val streakThreshold = if (range == InsightsRange.WEEK) 5 else 18

        if (mostImproved != null && mostImproved.deltaPct >= 8) {
            return HeadlineInsight(
                text = "${mostImproved.subject} accuracy improved +${mostImproved.deltaPct}% $periodWord — great learning momentum!",
                tone = HeadlineTone.POSITIVE,
            )
        }
        if (overallDelta != null && overallDelta >= 8) {
            return HeadlineInsight(
                text = "Overall accuracy improved +$overallDelta% vs $priorWord — keep it going.",
                tone = HeadlineTone.POSITIVE,
            )
        }
        if (daysActive >= streakThreshold) {
            return HeadlineInsight(
                text = "$daysActive active days $periodWord — consistency is paying off.",
                tone = HeadlineTone.POSITIVE,
            )
        }
        if (weakSubject != null) {
            return HeadlineInsight(
                text = "${weakSubject.subject} could use a closer look — accuracy is ${weakSubject.accuracyPct}%.",
                tone = HeadlineTone.INFO,
            )
        }
        if (totalAttempts == 0) {
            return HeadlineInsight(
                text = "No activity $periodWord yet — encourage a short revision session.",
                tone = HeadlineTone.INFO,
            )
        }
        val nameDisplay = profile.firstName.ifBlank { "Your child" }
        return HeadlineInsight(
            text = "Here's $nameDisplay's learning summary for this period.",
            tone = HeadlineTone.INFO,
        )
    }

    private fun computeRecommendation(
        firstName: String,
        rangeWord: String,
        weakSubject: SubjectStat?,
        revisionSessions: Int,
        daysActive: Int,
    ): String {
        if (weakSubject != null) {
            return "Encourage $firstName to revise ${weakSubject.subject} twice this $rangeWord and revisit key concepts."
        }
        if (revisionSessions < 2) {
            return "Add 2 short revision sessions this $rangeWord to improve retention."
        }
        if (daysActive < 3) {
            return "Encourage short 10–15 minute daily study check-ins."
        }
        return "Keep the current rhythm. Regular revision every 2–3 days will help long-term retention."
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private data class ChildProfile(
        val firstName: String = "",
        val grade: Int = 0,
        val board: String = "",
        val gender: String = "boy",
    )

    private data class Aggregator(
        val subject: String,
        var attempts: Int = 0,
        var correct: Int = 0,
        var total: Int = 0,
    ) {
        fun accuracyPctOrNull(): Int? =
            if (total > 0) ((correct.toFloat() / total) * 100f).toInt() else null
    }

    private data class DayBucket(val key: Long, val label: String, val isToday: Boolean)

    private fun normalize(s: String) = s.trim().lowercase()

    private fun dayKey(ts: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = ts }
        return cal.get(Calendar.YEAR) * 1000L + cal.get(Calendar.DAY_OF_YEAR)
    }

    private fun shortDateLabel(ts: Long): String {
        val sdf = SimpleDateFormat("d MMM", Locale.getDefault())
        return sdf.format(java.util.Date(ts))
    }

    private fun formatDateRange(now: Long, days: Int): String {
        val sdf = SimpleDateFormat("d MMM", Locale.getDefault())
        val end = java.util.Date(now)
        val start = java.util.Date(now - (days - 1).toLong() * DAY_MS)
        return "${sdf.format(start)} – ${sdf.format(end)}"
    }

    private fun signedDelta(diff: Int, period: String): String? {
        if (diff == 0) return null
        val sign = if (diff > 0) "↑" else "↓"
        return "$sign ${kotlin.math.abs(diff)} vs $period"
    }

    private fun signedPercent(diff: Int, period: String): String? {
        if (diff == 0) return null
        val sign = if (diff > 0) "↑" else "↓"
        return "$sign ${kotlin.math.abs(diff)}% vs $period"
    }

    // Revision = scored practice attempts (quiz MCQ + write-answer). This
    // narrower definition makes the Revision Sessions tile and the bar chart's
    // blue category line up — they both mean "deliberate practice."
    private fun isRevisionSession(item: ActivityLogItem): Boolean = when (item.source) {
        Source.QUIZ_MCQ, Source.QUIZ_SUBJECTIVE -> true
        else -> false
    }

    private fun isHomework(item: ActivityLogItem): Boolean = when (item.source) {
        Source.HOMEWORK -> true
        Source.UPLOAD -> item.category == ActivityLogRepository.Category.HOME_WORK
        else -> false
    }

    // Class work / tuition work / self-learning / AI test-prep uploads —
    // material the child captured but isn't a scored practice or homework.
    private fun isUploadOther(item: ActivityLogItem): Boolean =
        item.source == Source.UPLOAD && item.category != ActivityLogRepository.Category.HOME_WORK

    companion object {
        private const val DAY_MS = 24L * 60L * 60L * 1000L
    }
}
