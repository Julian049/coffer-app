package com.dev.julian09.financeapp.ui.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.julian09.financeapp.domain.model.AuthFailure
import com.dev.julian09.financeapp.domain.model.AuthField
import com.dev.julian09.financeapp.domain.usecase.ConfirmPasswordReset
import com.dev.julian09.financeapp.domain.usecase.RequestPasswordReset
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ForgotStep { EMAIL, CODE, DONE }

data class ForgotPasswordState(
    val step: ForgotStep = ForgotStep.EMAIL,
    val email: String = "",
    val code: String = "",
    val password: String = "",
    val errors: Map<AuthField, String> = emptyMap(),
    val formError: String? = null,
    val loading: Boolean = false,
)

/** REQ-AUT-09: correo → código y contraseña nueva → listo, back to Iniciar sesión. */
@HiltViewModel
class ForgotPasswordViewModel
    @Inject
    constructor(
        private val request: RequestPasswordReset,
        private val confirm: ConfirmPasswordReset,
    ) : ViewModel() {
        private val _state = MutableStateFlow(ForgotPasswordState())
        val state: StateFlow<ForgotPasswordState> = _state.asStateFlow()

        fun onEmail(value: String) = _state.update { it.copy(email = value, errors = it.errors - AuthField.Email, formError = null) }

        fun onCode(value: String) =
            _state.update { it.copy(code = value.filter(Char::isDigit).take(6), errors = it.errors - AuthField.Code, formError = null) }

        fun onPassword(value: String) =
            _state.update { it.copy(password = value, errors = it.errors - AuthField.Password, formError = null) }

        /** Also «Reenviar código» from the second step. */
        fun sendCode() =
            run { s ->
                val failure = request(s.email).exceptionOrNull() as? AuthFailure
                if (failure == null) s.copy(step = ForgotStep.CODE, code = "") else s.failed(failure)
            }

        fun changePassword() =
            run { s ->
                val failure = confirm(s.email, s.code, s.password).exceptionOrNull() as? AuthFailure
                if (failure == null) s.copy(step = ForgotStep.DONE, password = "") else s.failed(failure)
            }

        fun backToEmail() = _state.update { it.copy(step = ForgotStep.EMAIL, errors = emptyMap(), formError = null) }

        private fun run(block: suspend (ForgotPasswordState) -> ForgotPasswordState) {
            if (_state.value.loading) return
            _state.update { it.copy(loading = true, formError = null) }
            viewModelScope.launch {
                val next = block(_state.value)
                _state.value = next.copy(loading = false)
            }
        }

        private fun ForgotPasswordState.failed(failure: AuthFailure) =
            copy(errors = (failure as? AuthFailure.Invalid)?.fields.orEmpty(), formError = failure.formMessage())
    }
