package com.dev.julian09.financeapp.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dev.julian09.financeapp.domain.model.UserProfile
import com.dev.julian09.financeapp.ui.feature.accounts.AccountsScreen
import com.dev.julian09.financeapp.ui.feature.history.HistoryScreen
import com.dev.julian09.financeapp.ui.feature.home.HomeScreen
import com.dev.julian09.financeapp.ui.feature.reports.ReportsScreen
import com.dev.julian09.financeapp.ui.feature.settings.SettingsScreen
import com.dev.julian09.financeapp.ui.feature.sync.SyncStatusViewModel
import com.dev.julian09.financeapp.ui.theme.CofferTheme

@Composable
fun AppNavHost(
    user: UserProfile,
    navController: NavHostController = rememberNavController(),
) {
    val initial = user.displayName.firstOrNull()
    val backStack by navController.currentBackStackEntryAsState()
    val currentTab = Tab.entries.firstOrNull { tab -> backStack?.destination?.hasRoute(tab.route::class) == true }
    val openSettings = { navController.navigate(SettingsRoute) { launchSingleTop = true } }

    Scaffold(
        containerColor = CofferTheme.colors.background,
        bottomBar = { if (currentTab != null) BottomBar(selected = currentTab, onSelect = navController::navigateToTab) },
    ) { padding ->
        NavHost(navController = navController, startDestination = HomeRoute, modifier = Modifier.padding(padding)) {
            composable<HomeRoute> {
                val sync by hiltViewModel<SyncStatusViewModel>().status.collectAsStateWithLifecycle()
                HomeScreen(onOpenSettings = openSettings, avatarInitial = initial, syncStatus = sync)
            }
            composable<HistoryRoute> { HistoryScreen(onOpenSettings = openSettings, avatarInitial = initial) }
            composable<ReportsRoute> { ReportsScreen(onOpenSettings = openSettings, avatarInitial = initial) }
            composable<AccountsRoute> { AccountsScreen(onOpenSettings = openSettings, avatarInitial = initial) }
            composable<SettingsRoute> { SettingsScreen(onBack = navController::popBackStack) }
        }
    }
}

// Standard tab behavior: one copy of each tab, state kept when switching, back goes to Home
private fun NavHostController.navigateToTab(tab: Tab) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
