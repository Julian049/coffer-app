package com.dev.julian09.financeapp.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.dev.julian09.financeapp.domain.model.TransactionType
import com.dev.julian09.financeapp.domain.model.UserProfile
import com.dev.julian09.financeapp.ui.components.AmountFormat
import com.dev.julian09.financeapp.ui.feature.accounts.AccountDetailScreen
import com.dev.julian09.financeapp.ui.feature.accounts.AccountFormScreen
import com.dev.julian09.financeapp.ui.feature.accounts.AccountsScreen
import com.dev.julian09.financeapp.ui.feature.accounts.DeactivatedAccountsScreen
import com.dev.julian09.financeapp.ui.feature.categories.CategoriesScreen
import com.dev.julian09.financeapp.ui.feature.changes.ChangeHistoryScreen
import com.dev.julian09.financeapp.ui.feature.history.HistoryScreen
import com.dev.julian09.financeapp.ui.feature.home.CustomizeHomeScreen
import com.dev.julian09.financeapp.ui.feature.home.HomeScreen
import com.dev.julian09.financeapp.ui.feature.importexport.ImportExportScreen
import com.dev.julian09.financeapp.ui.feature.loans.LoanFormScreen
import com.dev.julian09.financeapp.ui.feature.mail.ConnectEmailScreen
import com.dev.julian09.financeapp.ui.feature.recurring.RecurringFormScreen
import com.dev.julian09.financeapp.ui.feature.recurring.RecurringScreen
import com.dev.julian09.financeapp.ui.feature.reports.ReportsScreen
import com.dev.julian09.financeapp.ui.feature.settings.ReminderSettingsViewModel
import com.dev.julian09.financeapp.ui.feature.settings.SettingsScreen
import com.dev.julian09.financeapp.ui.feature.sync.SyncStatusViewModel
import com.dev.julian09.financeapp.ui.feature.tags.TagDetailScreen
import com.dev.julian09.financeapp.ui.feature.tags.TagsScreen
import com.dev.julian09.financeapp.ui.feature.transactions.TransactionDetailScreen
import com.dev.julian09.financeapp.ui.feature.transactions.TransactionFormScreen
import com.dev.julian09.financeapp.ui.theme.CofferShapes
import com.dev.julian09.financeapp.ui.theme.CofferTheme
import java.util.UUID

@Composable
fun AppNavHost(
    user: UserProfile,
    navController: NavHostController = rememberNavController(),
) {
    val initial = user.displayName.firstOrNull()
    val backStack by navController.currentBackStackEntryAsState()
    val currentTab = Tab.entries.firstOrNull { tab -> backStack?.destination?.hasRoute(tab.route::class) == true }
    val openSettings = { navController.navigate(SettingsRoute) { launchSingleTop = true } }
    val openAccount = { id: UUID -> navController.navigate(AccountDetailRoute(id.toString())) }
    val editAccount = { id: UUID -> navController.navigate(EditAccountRoute(id.toString())) }
    val openTransaction = { id: UUID -> navController.navigate(TransactionDetailRoute(id.toString())) }
    val openTags = { navController.navigate(TagsRoute) { launchSingleTop = true } }
    val openTag = { id: UUID -> navController.navigate(TagDetailRoute(id.toString())) }

    val newTransaction = { type: TransactionType -> navController.navigate(TransactionFormRoute(type.name)) }
    // A tapped notice opens its sheet in Home, from wherever the app was
    val pendingLink by PendingLink.recurringId.collectAsStateWithLifecycle()
    val cardLink by PendingLink.cardId.collectAsStateWithLifecycle()
    val loanLink by PendingLink.loanId.collectAsStateWithLifecycle()
    LaunchedEffect(pendingLink, cardLink, loanLink) {
        if (pendingLink != null || cardLink != null || loanLink != null) navController.navigateToTab(Tab.Home)
    }
    // RNF-13: every amount follows Ajustes → Decimales
    val settings by hiltViewModel<ReminderSettingsViewModel>().settings.collectAsStateWithLifecycle()
    LaunchedEffect(settings?.amountDecimals) { settings?.let { AmountFormat.decimals = it.amountDecimals } }
    Scaffold(
        containerColor = CofferTheme.colors.background,
        bottomBar = { if (currentTab != null) BottomBar(selected = currentTab, onSelect = navController::navigateToTab) },
        floatingActionButton = {
            if (currentTab == Tab.Home || currentTab == Tab.History) NewTransactionButton { newTransaction(TransactionType.EXPENSE) }
        },
    ) { padding ->
        // M10 (#93): short fades with a slight slide; Compose follows the system animation scale ("Quitar animaciones")
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            // Consumed, so a form's imePadding() lifts its button by the keyboard alone (S12-UI09)
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
            enterTransition = { fadeIn(tween(ENTER_MS)) + slideInHorizontally(tween(ENTER_MS)) { it / SLIDE_FRACTION } },
            exitTransition = { fadeOut(tween(EXIT_MS)) },
            popEnterTransition = { fadeIn(tween(ENTER_MS)) },
            popExitTransition = { fadeOut(tween(EXIT_MS)) + slideOutHorizontally(tween(ENTER_MS)) { it / SLIDE_FRACTION } },
        ) {
            composable<HomeRoute> {
                val sync by hiltViewModel<SyncStatusViewModel>().status.collectAsStateWithLifecycle()
                HomeScreen(
                    onOpenSettings = openSettings,
                    onOpenHistory = { navController.navigateToTab(Tab.History) },
                    onOpenAccount = openAccount,
                    onNewTransaction = newTransaction,
                    onOpenTransaction = openTransaction,
                    onOpenRecurring = { navController.navigate(RecurringRoute) },
                    onOpenTags = openTags,
                    onOpenTag = openTag,
                    onCustomize = { navController.navigate(CustomizeHomeRoute) },
                    userName = user.displayName,
                    syncStatus = sync,
                )
            }
            composable<HistoryRoute> {
                HistoryScreen(
                    onOpenSettings = openSettings,
                    onOpenTransaction = openTransaction,
                    avatarInitial = initial,
                    onReviewDraft = { navController.navigate(TransactionFormRoute(draftId = it.toString())) },
                )
            }
            composable<ReportsRoute> {
                ReportsScreen(onOpenSettings = openSettings, onOpenTags = openTags, onOpenTag = openTag, avatarInitial = initial)
            }
            composable<AccountsRoute> {
                AccountsScreen(
                    onOpenSettings = openSettings,
                    onNewAccount = { navController.navigate(NewAccountRoute) },
                    onOpenAccount = openAccount,
                    onEditAccount = editAccount,
                    onOpenDeactivated = { navController.navigate(DeactivatedAccountsRoute) },
                    onNewLoan = { navController.navigate(NewLoanRoute) },
                    avatarInitial = initial,
                )
            }
            composable<SettingsRoute> {
                SettingsScreen(
                    onBack = navController::popBackStack,
                    onOpenCategories = { navController.navigate(CategoriesRoute) },
                    onOpenDeactivated = { navController.navigate(DeactivatedAccountsRoute) },
                    onOpenRecurring = { navController.navigate(RecurringRoute) },
                    onOpenImportExport = { navController.navigate(ImportExportRoute) },
                    onOpenTags = openTags,
                    onOpenChangeHistory = { navController.navigate(ChangeHistoryRoute) },
                    onOpenCustomizeHome = { navController.navigate(CustomizeHomeRoute) },
                    onOpenConnectEmail = { navController.navigate(ConnectEmailRoute) },
                )
            }
            composable<ConnectEmailRoute> { ConnectEmailScreen(onBack = navController::popBackStack) }
            composable<ChangeHistoryRoute> { ChangeHistoryScreen(onBack = navController::popBackStack) }
            composable<CustomizeHomeRoute> { CustomizeHomeScreen(onBack = navController::popBackStack) }
            composable<TagsRoute> { TagsScreen(onBack = navController::popBackStack, onOpenTag = openTag) }
            composable<TagDetailRoute> { TagDetailScreen(onBack = navController::popBackStack, onOpenTransaction = openTransaction) }
            composable<ImportExportRoute> { ImportExportScreen(onBack = navController::popBackStack) }
            composable<RecurringRoute> {
                RecurringScreen(
                    onBack = navController::popBackStack,
                    onNew = { navController.navigate(RecurringFormRoute()) },
                    onEdit = { id -> navController.navigate(RecurringFormRoute(id.toString())) },
                    onOpenTransaction = openTransaction,
                )
            }
            composable<RecurringFormRoute> { RecurringFormScreen(onBack = navController::popBackStack) }
            composable<NewAccountRoute> { AccountFormScreen(onBack = navController::popBackStack) }
            composable<NewLoanRoute> { LoanFormScreen(onBack = navController::popBackStack, name = "") }
            composable<EditAccountRoute> { AccountFormScreen(onBack = navController::popBackStack) }
            composable<AccountDetailRoute> {
                AccountDetailScreen(
                    onBack = navController::popBackStack,
                    onEdit = editAccount,
                    onOpenHistory = { navController.navigateToTab(Tab.History) },
                    onOpenTransaction = openTransaction,
                    onConvertLoan = { id -> navController.navigate(ConvertLoanRoute(id.toString())) },
                )
            }
            composable<ConvertLoanRoute> { LoanFormScreen(onBack = navController::popBackStack) }
            composable<DeactivatedAccountsRoute> { DeactivatedAccountsScreen(onBack = navController::popBackStack) }
            composable<CategoriesRoute> { CategoriesScreen(onBack = navController::popBackStack) }
            composable<TransactionFormRoute> { TransactionFormScreen(onBack = navController::popBackStack) }
            composable<TransactionDetailRoute> {
                TransactionDetailScreen(
                    onBack = navController::popBackStack,
                    onEdit = { d ->
                        navController.navigate(TransactionFormRoute(d.transaction.type.name, d.transaction.id.toString())) {
                            popUpTo<TransactionDetailRoute> { inclusive = true }
                        }
                    },
                )
            }
        }
    }
}

private const val ENTER_MS = 220
private const val EXIT_MS = 150
private const val SLIDE_FRACTION = 8

// Standard tab behavior: one copy of each tab, state kept when switching, back goes to Home
private fun NavHostController.navigateToTab(tab: Tab) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** The + of Home and History (S5-UI02): straight to a new expense. */
@Composable
private fun NewTransactionButton(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        shape = CofferShapes.fab,
        containerColor = CofferTheme.colors.primary,
        contentColor = CofferTheme.colors.onPrimary,
    ) { Icon(Icons.Outlined.Add, contentDescription = "Nueva transacción") }
}
