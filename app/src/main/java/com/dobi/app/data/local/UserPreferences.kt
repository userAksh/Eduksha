package com.dobi.app.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.dobi.app.domain.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val KEY_USER_ROLE = stringPreferencesKey("user_role")
        private val KEY_LINKED_CHILD_ID = stringPreferencesKey("linked_child_id")
        private val KEY_LINKED_PARENT_ID = stringPreferencesKey("linked_parent_id")
        private val KEY_LINKED_CHILD_GRADE = intPreferencesKey("linked_child_grade")
        private val KEY_TUTOR_HONORIFIC = stringPreferencesKey("tutor_honorific")
        private val KEY_TUTOR_NAME = stringPreferencesKey("tutor_name")
        private val KEY_INVITE_CLAIMED = booleanPreferencesKey("invite_claim_done")
        const val DEFAULT_GRADE = 6
    }

    val userRole: Flow<UserRole?> = dataStore.data.map { prefs ->
        prefs[KEY_USER_ROLE]?.let { runCatching { UserRole.valueOf(it) }.getOrNull() }
    }

    val linkedChildId: Flow<String?> = dataStore.data.map { it[KEY_LINKED_CHILD_ID] }
    val linkedParentId: Flow<String?> = dataStore.data.map { it[KEY_LINKED_PARENT_ID] }
    val linkedChildGrade: Flow<Int> =
        dataStore.data.map { it[KEY_LINKED_CHILD_GRADE] ?: DEFAULT_GRADE }
    val tutorHonorific: Flow<String> = dataStore.data.map { it[KEY_TUTOR_HONORIFIC].orEmpty() }
    val tutorName: Flow<String> = dataStore.data.map { it[KEY_TUTOR_NAME].orEmpty() }
    val inviteClaimDone: Flow<Boolean> = dataStore.data.map { it[KEY_INVITE_CLAIMED] ?: false }

    suspend fun saveRole(role: UserRole) {
        dataStore.edit { it[KEY_USER_ROLE] = role.name }
    }

    suspend fun saveLinkedChild(parentId: String, childId: String) {
        dataStore.edit {
            it[KEY_LINKED_PARENT_ID] = parentId
            it[KEY_LINKED_CHILD_ID] = childId
        }
    }

    suspend fun saveLinkedChildGrade(grade: Int) {
        dataStore.edit { it[KEY_LINKED_CHILD_GRADE] = grade }
    }

    suspend fun saveTutor(honorific: String, name: String) {
        dataStore.edit {
            it[KEY_TUTOR_HONORIFIC] = honorific
            it[KEY_TUTOR_NAME] = name
        }
    }

    suspend fun markInviteClaimDone() {
        dataStore.edit { it[KEY_INVITE_CLAIMED] = true }
    }
}
