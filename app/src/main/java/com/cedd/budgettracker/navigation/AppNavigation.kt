package com.cedd.budgettracker.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cedd.budgettracker.presentation.budget.BudgetScreen
import com.cedd.budgettracker.presentation.history.HistoryScreen
import com.cedd.budgettracker.presentation.settings.SettingsScreen

sealed class Screen(val route: String) {
    data object Budget : Screen("budget")
    data object History : Screen("history")
    data object Settings : Screen("settings")
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Budget.route
    ) {
        composable(Screen.Budget.route) {
            BudgetScreen(
                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.History.route) {
            HistoryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
