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

/** Bienvenida (S15-UI06): where a signed-out user starts. */
@Serializable
data object WelcomeRoute

@Serializable
data object LoginRoute

@Serializable
data object RegisterRoute

/** REQ-AUT-09: Recuperar contraseña, from Iniciar sesión. */
@Serializable
data object ForgotPasswordRoute

/** REQ-COR-01: Ajustes → Conectar correo. */
@Serializable
data object ConnectEmailRoute

@Serializable
data object NewAccountRoute

/** Gestión de cuentas → Préstamos · Agregar (S12-UI05). */
@Serializable
data object NewLoanRoute

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
    /** REQ-COR-05: Revisar transacción, filled from this draft. */
    val draftId: String? = null,
)

@Serializable
data class TransactionDetailRoute(
    val id: String,
)

@Serializable
data object RecurringRoute

/** New recurring expense, or the edit of [id]. */
@Serializable
data class RecurringFormRoute(
    val id: String? = null,
)

@Serializable
data object ImportExportRoute

/** REQ-PRE-09: "Pasar a detallado" of a simple loan. */
@Serializable
data class ConvertLoanRoute(
    val loanId: String,
)

@Serializable
data object TagsRoute

@Serializable
data class TagDetailRoute(
    val id: String,
)

@Serializable
data object ChangeHistoryRoute

@Serializable
data object CustomizeHomeRoute
