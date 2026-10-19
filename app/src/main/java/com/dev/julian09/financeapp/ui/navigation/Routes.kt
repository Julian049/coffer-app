package com.dev.julian09.financeapp.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data object HistoryRoute

@Serializable
data object ReportsRoute

@Serializable
data object AccountsRoute

@Serializable
data object SettingsRoute

@Serializable
data object LoginRoute

@Serializable
data object RegisterRoute

@Serializable
data object NewAccountRoute

/** Ids travel as text: type-safe routes keep them in the SavedStateHandle under the property name. */
@Serializable
data class EditAccountRoute(
    val id: String,
)

@Serializable
data class AccountDetailRoute(
    val id: String,
)

@Serializable
data object DeactivatedAccountsRoute

@Serializable
data object CategoriesRoute

/** New movement of [type] (EXPENSE, INCOME, TRANSFER), or the edit of [id]. */
@Serializable
data class TransactionFormRoute(
    val type: String = "EXPENSE",
    val id: String? = null,
)

@Serializable
data class TransactionDetailRoute(
    val id: String,
)
