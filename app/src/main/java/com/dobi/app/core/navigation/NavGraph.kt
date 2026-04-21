package com.dobi.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.dobi.app.feature.addchild.AddChildScreen
import com.dobi.app.feature.auth.OtpVerificationScreen
import com.dobi.app.feature.auth.PhoneAuthScreen
import com.dobi.app.feature.childhome.ChildHomeScreen
import com.dobi.app.feature.conceptdetail.ConceptDetailScreen
import com.dobi.app.feature.conceptlist.ConceptListScreen
import com.dobi.app.feature.homework.HomeworkLogScreen
import com.dobi.app.feature.parenthome.ParentHomeScreen
import com.dobi.app.feature.roleselection.RoleSelectionScreen
import com.dobi.app.feature.selectchild.SelectChildScreen

@Composable
fun DobiNavGraph(
    navController: NavHostController,
    startDestination: Screen
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable<Screen.RoleSelection> {
            RoleSelectionScreen(
                onNavigateToPhoneAuth = { navController.navigate(Screen.PhoneAuth) },
                onNavigateToParentHome = {
                    navController.navigate(Screen.ParentHome) {
                        popUpTo<Screen.RoleSelection> { inclusive = true }
                    }
                },
                onNavigateToSelectChild = { navController.navigate(Screen.SelectChild) }
            )
        }

        composable<Screen.PhoneAuth> {
            PhoneAuthScreen(
                onNavigateToOtp = { otpScreen -> navController.navigate(otpScreen) },
                onNavigateToParentHome = {
                    navController.navigate(Screen.ParentHome) {
                        popUpTo<Screen.RoleSelection> { inclusive = true }
                    }
                }
            )
        }

        composable<Screen.OtpVerification> {
            OtpVerificationScreen(
                onNavigateToAddChild = {
                    navController.navigate(Screen.AddChild) {
                        popUpTo<Screen.RoleSelection> { inclusive = true }
                    }
                },
                onNavigateToParentHome = {
                    navController.navigate(Screen.ParentHome) {
                        popUpTo<Screen.RoleSelection> { inclusive = true }
                    }
                }
            )
        }

        composable<Screen.AddChild> {
            AddChildScreen(
                onNavigateToParentHome = {
                    navController.navigate(Screen.ParentHome) {
                        popUpTo<Screen.AddChild> { inclusive = true }
                    }
                }
            )
        }

        composable<Screen.SelectChild> {
            SelectChildScreen(
                onChildSelected = { childId ->
                    navController.navigate(Screen.ChildHome(childId))
                }
            )
        }

        composable<Screen.ChildHome> {
            ChildHomeScreen(
                onNavigateToConceptList = { navController.navigate(Screen.ConceptList) },
                onNavigateToHomeworkLog = { navController.navigate(Screen.HomeworkLog) }
            )
        }

        composable<Screen.ParentHome> {
            ParentHomeScreen(
                onNavigateToChildHome = { childId ->
                    navController.navigate(Screen.ChildHome(childId))
                },
                onNavigateToAddChild = { navController.navigate(Screen.AddChild) },
                onNavigateToRoleSelection = {
                    navController.navigate(Screen.RoleSelection) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable<Screen.ConceptList> {
            ConceptListScreen(
                onNavigateBack = { navController.popBackStack() },
                onConceptClick = { conceptId ->
                    navController.navigate(Screen.ConceptDetail(conceptId))
                }
            )
        }

        composable<Screen.ConceptDetail> {
            ConceptDetailScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<Screen.HomeworkLog> {
            HomeworkLogScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
