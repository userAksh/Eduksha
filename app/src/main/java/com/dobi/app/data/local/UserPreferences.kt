package com.dobi.app.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
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
        private val KEY_IS_CHILD_DEVICE = booleanPreferencesKey("is_child_device")
        private val KEY_PARENT_REFERRAL_CLAIMED = booleanPreferencesKey("parent_referral_claim_done")
        private val KEY_PARENT_REFERRAL_CODE = stringPreferencesKey("parent_referral_code")
        private val KEY_PARENT_FIRST_ACTIVE_AT = longPreferencesKey("parent_first_active_at")
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
    val isChildDevice: Flow<Boolean> = dataStore.data.map { it[KEY_IS_CHILD_DEVICE] ?: false }
    val parentReferralClaimDone: Flow<Boolean> = dataStore.data.map { it[KEY_PARENT_REFERRAL_CLAIMED] ?: false }
    val parentReferralCode: Flow<String?> = dataStore.data.map { it[KEY_PARENT_REFERRAL_CODE]?.takeIf { c -> c.isNotBlank() } }
    val parentFirstActiveAt: Flow<Long> = dataStore.data.map { it[KEY_PARENT_FIRST_ACTIVE_AT] ?: 0L }

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

    suspend fun saveParentReferralCode(code: String) {
        dataStore.edit { it[KEY_PARENT_REFERRAL_CODE] = code }
    }

    suspend fun clearParentReferralCode() {
        dataStore.edit { it.remove(KEY_PARENT_REFERRAL_CODE) }
    }

    suspend fun markParentReferralClaimDone() {
        dataStore.edit { it[KEY_PARENT_REFERRAL_CLAIMED] = true; it.remove(KEY_PARENT_REFERRAL_CODE) }
    }

    suspend fun saveParentFirstActiveAt(millis: Long) {
        dataStore.edit { if ((it[KEY_PARENT_FIRST_ACTIVE_AT] ?: 0L) == 0L) it[KEY_PARENT_FIRST_ACTIVE_AT] = millis }
    }

    suspend fun markAsChildDevice() {
        dataStore.edit { it[KEY_IS_CHILD_DEVICE] = true }
    }

    /**
     * Wipes every preference key. Used after account deletion so the next
     * cold start lands on RoleSelection with a clean slate.
     */
    suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }
}
