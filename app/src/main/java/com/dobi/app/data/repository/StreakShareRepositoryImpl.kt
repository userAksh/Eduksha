package com.dobi.app.data.repository

import com.dobi.app.data.local.StreakSharePreferences
import com.dobi.app.domain.repository.StreakShareRepository
import com.dobi.app.domain.repository.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class StreakShareRepositoryImpl @Inject constructor(
    private val userRepository: UserRepository,
    private val prefs: StreakSharePreferences
) : StreakShareRepository {

    override fun hasSharedToday(): Flow<Boolean> =
        userRepository.linkedChildId.flatMapLatest { childId ->
            if (childId.isNullOrBlank()) flowOf(false)
            else prefs.lastSharedEpochDay(childId).map { stored ->
                stored != null && stored == todayEpochDay()
            }
        }

    override suspend fun markSharedToday() {
        val childId = userRepository.linkedChildId.first() ?: return
        if (childId.isBlank()) return
        prefs.setLastSharedEpochDay(childId, todayEpochDay())
    }

    private fun todayEpochDay(): Int =
        LocalDate.now(ZoneId.systemDefault()).toEpochDay().toInt()
}
