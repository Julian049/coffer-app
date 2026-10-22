package com.dev.julian09.financeapp.ui.feature.auth

import com.dev.julian09.financeapp.domain.model.AuthFailure

/** Message above the button for failures that are not about one field (RNF-07: never technical details). */
fun AuthFailure.formMessage(): String? =
    when (this) {
        AuthFailure.WrongCredentials -> "Usuario o contraseña incorrectos."
        is AuthFailure.TooManyAttempts -> "Demasiados intentos. Espera $retryAfterSeconds s y vuelve a intentarlo."
        AuthFailure.ConfirmationFailed -> "No pudimos confirmar tu identidad. Revisa tu contraseña o usa Google."
        AuthFailure.NoConnection -> "Sin conexión. Revisa tu internet y vuelve a intentarlo."
        AuthFailure.SessionExpired, AuthFailure.Unexpected -> "Algo salió mal. Vuelve a intentarlo."
        is AuthFailure.Invalid, is AuthFailure.Taken -> null
    }
