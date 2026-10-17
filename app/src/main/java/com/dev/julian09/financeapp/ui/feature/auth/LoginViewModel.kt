package com.dev.julian09.financeapp.ui.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.julian09.financeapp.domain.model.AuthFailure
import com.dev.julian09.financeapp.domain.model.AuthField
import com.dev.julian09.financeapp.domain.model.LoginInput
import com.dev.julian09.financeapp.domain.usecase.Login
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val identifier: String = "",
    val password: String = "",
    val errors: Map<AuthField, String> = emptyMap(),
    val formError: String? = null,
    val loading: Boolean = false,
)

/** On success nothing to navigate: the root switches to the app as soon as the session becomes active. */
@HiltViewModel
class LoginViewModel
    @Inject
    constructor(
        private val login: Login,
    ) : ViewModel() {
        private val _state = MutableStateFlow(LoginUiState())
        val state: StateFlow<LoginUiState> = _state.asStateFlow()

        /** An expired session pre-fills who was signed in (SEQ-03). */
        fun prefill(identifier: String) {
            if (_state.value.identifier.isEmpty()) _state.update { it.copy(identifier = identifier) }
        }

        fun onIdentifier(value: String) =
            _state.update { it.copy(identifier = value, errors = it.errors - AuthField.Identifier, formError = null) }

        fun onPassword(value: String) =
            _state.update { it.copy(password = value, errors = it.errors - AuthField.Password, formError = null) }

        fun submit() {
            if (_state.value.loading) return
            _state.update { it.copy(loading = true, formError = null) }
            viewModelScope.launch {
                val input = _state.value.let { LoginInput(it.identifier, it.password) }
                val failure = login(input).exceptionOrNull() as? AuthFailure
                _state.update {
                    it.copy(
                        loading = false,
                        errors = (failure as? AuthFailure.Invalid)?.fields.orEmpty(),
                        formError = failure?.formMessage(),
                        // A wrong password is cleared; the identifier stays
                        password = if (failure == AuthFailure.WrongCredentials) "" else it.password,
                    )
                }
            }
        }
    }
