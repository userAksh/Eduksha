package com.dobi.app.core.navigation

import kotlinx.serialization.Serializable

@Serializable sealed class Screen {
    @Serializable data object RoleSelection : Screen()
    @Serializable data object ChildHome : Screen()
    @Serializable data object ParentHome : Screen()
    @Serializable data object ConceptList : Screen()
    @Serializable data class ConceptDetail(val conceptId: String) : Screen()
    @Serializable data object HomeworkLog : Screen()
}
