package com.dev.julian09.financeapp.ui.feature.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.julian09.financeapp.BuildConfig
import com.dev.julian09.financeapp.domain.model.AuthFailure
import com.dev.julian09.financeapp.domain.usecase.LoginWithGoogle
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GoogleSignInState(
    val loading: Boolean = false,
    val error: String? = null,
)

/** "Continuar con Google" of Iniciar sesión and Registro (REQ-AUT-03): the session change opens the app by itself. */
@HiltViewModel
class GoogleSignInViewModel
    @Inject
    constructor(
        private val loginWithGoogle: LoginWithGoogle,
    ) : ViewModel() {
        private val _state = MutableStateFlow(GoogleSignInState())
        val state: StateFlow<GoogleSignInState> = _state.asStateFlow()

        fun started() = _state.update { GoogleSignInState(loading = true) }

        /** [message] null: the user closed the account picker, which is not an error. */
        fun failed(message: String?) = _state.update { GoogleSignInState(error = message) }

        fun signIn(idToken: String) {
            viewModelScope.launch {
                val failure = loginWithGoogle(idToken).exceptionOrNull() as? AuthFailure
                _state.update {
                    GoogleSignInState(
                        error =
                            when (failure) {
                                null -> null
                                // On this route a 401 means Google's token was not accepted, not a wrong password
                                AuthFailure.WrongCredentials -> "No pudimos validar tu cuenta de Google. Inténtalo de nuevo."
                                else -> failure.formMessage()
                            },
                    )
                }
            }
        }
    }

/**
 * SEQ-01 §1.3 with Credential Manager: the "Sign in with Google" sheet returns an ID token for our web client id.
 * Null when the build has no client id (the button stays disabled).
 */
@Composable
fun rememberGoogleSignIn(viewModel: GoogleSignInViewModel): (() -> Unit)? {
    val clientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    if (clientId.isBlank()) return null
    return remember(viewModel) {
        {
            viewModel.started()
            scope.launch {
                try {
                    val request =
                        GetCredentialRequest
                            .Builder()
                            .addCredentialOption(GetSignInWithGoogleOption.Builder(clientId).build())
                            .build()
                    val credential = CredentialManager.create(context).getCredential(context, request).credential
                    if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        viewModel.signIn(GoogleIdTokenCredential.createFrom(credential.data).idToken)
                    } else {
                        viewModel.failed("No se pudo iniciar con Google. Inténtalo de nuevo.")
                    }
                } catch (_: GetCredentialCancellationException) {
                    viewModel.failed(null)
                } catch (_: NoCredentialException) {
                    viewModel.failed("Agrega una cuenta de Google en el teléfono para continuar.")
                } catch (_: GetCredentialException) {
                    viewModel.failed("No se pudo iniciar con Google. Inténtalo de nuevo.")
                }
            }
        }
    }
}
