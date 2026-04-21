package com.dobi.app.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
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
    }

    val userRole: Flow<UserRole?> = dataStore.data.map { prefs ->
        prefs[KEY_USER_ROLE]?.let { runCatching { UserRole.valueOf(it) }.getOrNull() }
    }

    val linkedChildId: Flow<String?> = dataStore.data.map { it[KEY_LINKED_CHILD_ID] }
    val linkedParentId: Flow<String?> = dataStore.data.map { it[KEY_LINKED_PARENT_ID] }

    suspend fun saveRole(role: UserRole) {
        dataStore.edit { it[KEY_USER_ROLE] = role.name }
    }

    suspend fun saveLinkedChild(parentId: String, childId: String) {
        dataStore.edit {
            it[KEY_LINKED_PARENT_ID] = parentId
            it[KEY_LINKED_CHILD_ID] = childId
        }
    }
}
