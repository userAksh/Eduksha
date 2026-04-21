package com.dobi.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.dobi.app.feature.childhome.ChildHomeScreen
import com.dobi.app.feature.conceptdetail.ConceptDetailScreen
import com.dobi.app.feature.conceptlist.ConceptListScreen
import com.dobi.app.feature.homework.HomeworkLogScreen
import com.dobi.app.feature.parenthome.ParentHomeScreen
import com.dobi.app.feature.roleselection.RoleSelectionScreen

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
                onStudentSelected = {
                    navController.navigate(Screen.ChildHome) {
                        popUpTo<Screen.RoleSelection> { inclusive = true }
                    }
                },
                onParentSelected = {
                    navController.navigate(Screen.ParentHome) {
                        popUpTo<Screen.RoleSelection> { inclusive = true }
                    }
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
            ParentHomeScreen()
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
