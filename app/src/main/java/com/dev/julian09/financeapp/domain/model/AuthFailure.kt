package com.dev.julian09.financeapp.domain.model

/** Why an auth action failed, already in terms the screens can show. */
sealed class AuthFailure : Exception() {
    /** Form errors by field, from local validation or the server's FIN-VALIDATION. */
    data class Invalid(
        val fields: Map<AuthField, String>,
    ) : AuthFailure()

    /** Username or email already registered (FIN-CONFLICT); [field] tells which one. */
    data class Taken(
        val field: AuthField,
        override val message: String,
    ) : AuthFailure()

    /** Wrong credentials; the server never says which part (RNF-07). */
    data object WrongCredentials : AuthFailure()

    data class TooManyAttempts(
        val retryAfterSeconds: Long,
    ) : AuthFailure()

    /** The password or Google token asked for before a destructive action did not match (FIN-CONFIRMATION); the session is fine. */
    data object ConfirmationFailed : AuthFailure()

    data object SessionExpired : AuthFailure()

    data object NoConnection : AuthFailure()

    /** Anything else; the UI shows a generic message (RNF-07). */
    data object Unexpected : AuthFailure()
}

enum class AuthField { DisplayName, Email, Username, Password, Identifier, Code }
