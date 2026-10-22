package com.dev.julian09.financeapp.ui.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.julian09.financeapp.domain.model.AuthFailure
import com.dev.julian09.financeapp.domain.model.AuthField
import com.dev.julian09.financeapp.domain.model.RegisterInput
import com.dev.julian09.financeapp.domain.usecase.AcceptLegalTerms
import com.dev.julian09.financeapp.domain.usecase.RegisterUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegisterUiState(
    val displayName: String = "",
    val email: String = "",
    val username: String = "",
    val password: String = "",
    val acceptedLegal: Boolean = false,
    val errors: Map<AuthField, String> = emptyMap(),
    val formError: String? = null,
    val loading: Boolean = false,
)

@HiltViewModel
class RegisterViewModel
    @Inject
    constructor(
        private val register: RegisterUser,
        private val acceptLegalTerms: AcceptLegalTerms,
    ) : ViewModel() {
        private val _state = MutableStateFlow(RegisterUiState())
        val state: StateFlow<RegisterUiState> = _state.asStateFlow()

        fun onChange(
            field: AuthField,
            value: String,
        ) = _state.update {
            val edited =
                when (field) {
                    AuthField.DisplayName -> it.copy(displayName = value)
                    AuthField.Email -> it.copy(email = value)
                    // Usernames are lowercase by contract; typing "Julian" just works
                    AuthField.Username -> it.copy(username = value.lowercase().trim())
                    AuthField.Password -> it.copy(password = value)
                    AuthField.Identifier -> it
                }
            edited.copy(errors = it.errors - field, formError = null)
        }

        fun onAcceptedLegal(accepted: Boolean) = _state.update { it.copy(acceptedLegal = accepted, formError = null) }

        fun submit() {
            if (_state.value.loading) return
            if (!_state.value.acceptedLegal) {
                _state.update { it.copy(formError = "Acepta los términos y la política de privacidad para crear tu cuenta.") }
                return
            }
            _state.update { it.copy(loading = true, formError = null) }
            viewModelScope.launch {
                val input = _state.value.let { RegisterInput(it.displayName, it.email, it.username, it.password) }
                val result = register(input)
                // Remembered before the session reaches the root, which would otherwise ask again
                result.getOrNull()?.let { acceptLegalTerms(it.id) }
                val failure = result.exceptionOrNull() as? AuthFailure
                _state.update {
                    it.copy(
                        loading = false,
                        errors =
                            when (failure) {
                                is AuthFailure.Invalid -> failure.fields
                                is AuthFailure.Taken -> mapOf(failure.field to failure.message)
                                else -> emptyMap()
                            },
                        formError = failure?.formMessage(),
                    )
                }
            }
        }
    }
