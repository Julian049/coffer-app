package com.dev.julian09.financeapp.domain.model

data class RegisterInput(
    val displayName: String,
    val email: String,
    val username: String,
    val password: String,
)

data class LoginInput(
    val identifier: String,
    val password: String,
)

/** Same rules as the server contract (openapi.yml RegisterRequest/LoginRequest), checked before any network call. */
object AuthValidation {
    private val USERNAME = Regex("^[a-z0-9._]{3,40}$")
    private val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    fun register(input: RegisterInput): Map<AuthField, String> =
        buildMap {
            if (input.displayName.isBlank()) {
                put(AuthField.DisplayName, "Escribe tu nombre")
            } else if (input.displayName.trim().length > 100) {
                put(AuthField.DisplayName, "Máximo 100 caracteres")
            }
            if (!EMAIL.matches(input.email.trim()) || input.email.trim().length > 255) put(AuthField.Email, "Correo no válido")
            if (!USERNAME.matches(input.username)) {
                put(AuthField.Username, "Entre 3 y 40 caracteres: minúsculas, números, punto o guion bajo")
            }
            if (input.password.length !in 8..128) put(AuthField.Password, "Entre 8 y 128 caracteres")
        }

    fun login(input: LoginInput): Map<AuthField, String> =
        buildMap {
            if (input.identifier.trim().length !in 3..255) put(AuthField.Identifier, "Escribe tu usuario o correo")
            if (input.password.isEmpty()) put(AuthField.Password, "Escribe tu contraseña")
        }
}
