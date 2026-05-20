package com.dobi.app.feature.parentdashboard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.dobi.app.core.navigation.Screen
import com.dobi.app.data.repository.ActivityLogRepository
import com.dobi.app.data.repository.ActivityLogRepository.ActivityLogItem
import com.dobi.app.data.repository.ActivityLogRepository.Category
import com.dobi.app.domain.model.Child
import com.dobi.app.domain.repository.ChildRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ActivityTimeFilter(val label: String, val days: Int) {
    TODAY("Today", 0),
    WEEK("1 Week", 7),
    MONTH("1 Month", 30),
}

data class ActivityDetailUiState(
    val isLoading: Boolean = true,
    val childName: String = "",
    val category: Category = Category.SCHOOL_WORK,
    val items: List<ActivityLogItem> = emptyList(),
    val filter: ActivityTimeFilter = ActivityTimeFilter.WEEK,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ActivityDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val auth: FirebaseAuth,
    private val childRepository: ChildRepository,
    private val activityLogRepository: ActivityLogRepository,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<Screen.ActivityDetail>()
    private val category: Category = Category.fromKey(route.categoryKey) ?: Category.SCHOOL_WORK
    private val childId: String = route.childId

    private val filter = MutableStateFlow(ActivityTimeFilter.WEEK)
    private val childName = MutableStateFlow("")

    private val itemsFlow = activityLogRepository.observeForChild(childId)

    val uiState = combine(itemsFlow, filter, childName) { items, filter, name ->
        val cutoff = cutoffMs(filter)
        val filtered = items
            .filter { it.category == category }
            .filter { it.timestamp >= cutoff }
        ActivityDetailUiState(
            isLoading = false,
            childName = name,
            category = category,
            items = filtered,
            filter = filter,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ActivityDetailUiState(category = category),
    )

    init {
        loadChildName()
    }

    private fun loadChildName() {
        val uid = auth.currentUser?.uid ?: return
        if (childId.isBlank()) return
        viewModelScope.launch {
            childRepository.getChild(uid, childId)
                .onSuccess { c -> childName.value = c.name.substringBefore(" ") }
        }
    }

    fun selectFilter(f: ActivityTimeFilter) { filter.value = f }

    private fun cutoffMs(f: ActivityTimeFilter): Long {
        val now = System.currentTimeMillis()
        return when (f) {
            ActivityTimeFilter.TODAY -> startOfTodayMs(now)
            ActivityTimeFilter.WEEK -> now - 7L * 24 * 60 * 60 * 1000
            ActivityTimeFilter.MONTH -> now - 30L * 24 * 60 * 60 * 1000
        }
    }

    private fun startOfTodayMs(now: Long): Long {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = now
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}
