package com.dobi.app.core.navigation

import kotlinx.serialization.Serializable

@Serializable sealed class Screen {
    @Serializable data object RoleSelection : Screen()
    @Serializable data object PhoneAuth : Screen()
    @Serializable data class OtpVerification(
        val verificationId: String,
        val name: String,
        val phone: String
    ) : Screen()
    @Serializable data object AddChild : Screen()
    @Serializable data object SelectChild : Screen()
    @Serializable data object EnterPairingCode : Screen()
    @Serializable data object SetPin : Screen()
    @Serializable data object EnterPin : Screen()
    @Serializable data class ChildHome(val childId: String) : Screen()
    @Serializable data object ParentHome : Screen()
    @Serializable data object ConceptList : Screen()
    @Serializable data class ConceptDetail(val conceptId: String) : Screen()
    @Serializable data object HomeworkLog : Screen()
}
