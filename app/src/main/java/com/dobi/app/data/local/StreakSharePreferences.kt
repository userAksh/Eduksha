package com.dobi.app.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local store for "has this child shared today's streak snap yet?".
 * Key format: `streak_share_<childId>` → epoch day (UTC) of last share.
 *
 * Per-child so a parent device with multiple linked children doesn't leak
 * one child's share state into another's.
 */
@Singleton
class StreakSharePreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    fun lastSharedEpochDay(childId: String): Flow<Int?> {
        val key = intPreferencesKey(keyFor(childId))
        return dataStore.data.map { it[key] }
    }

    suspend fun setLastSharedEpochDay(childId: String, epochDay: Int) {
        dataStore.edit { it[intPreferencesKey(keyFor(childId))] = epochDay }
    }

    private fun keyFor(childId: String) = "streak_share_$childId"
}
