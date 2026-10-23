package com.dev.julian09.financeapp.ui.feature.legal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.julian09.financeapp.domain.usecase.AcceptLegalTerms
import com.dev.julian09.financeapp.domain.usecase.Logout
import com.dev.julian09.financeapp.ui.components.AppButton
import com.dev.julian09.financeapp.ui.components.ButtonKind
import com.dev.julian09.financeapp.ui.theme.CofferTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LegalConsentViewModel
    @Inject
    constructor(
        private val acceptLegalTerms: AcceptLegalTerms,
        private val logout: Logout,
    ) : ViewModel() {
        /** The root leaves this screen by itself once the accepted version is stored. */
        fun accept(userId: String) {
            viewModelScope.launch { acceptLegalTerms(userId) }
        }

        fun declineAndSignOut() {
            viewModelScope.launch { logout() }
        }
    }

/** S14-H02: shown to a signed-in user who has not accepted the current terms and privacy policy. */
@Composable
fun LegalConsentScreen(
    userId: String,
    viewModel: LegalConsentViewModel = hiltViewModel(),
) {
    LegalConsentContent(onAccept = { viewModel.accept(userId) }, onDecline = viewModel::declineAndSignOut)
}

@Composable
fun LegalConsentContent(
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CofferTheme.colors
    var accepted by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Términos y privacidad", style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
        Text(
            "Coffer guarda tu correo, tu nombre y los datos financieros que registras, cifrados en tu teléfono y respaldados " +
                "en la nube. No los vendemos ni los usamos para publicidad. Puedes eliminar tu cuenta y todos tus datos " +
                "cuando quieras desde Ajustes. Coffer no es asesoría financiera.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
        )
        LegalConsentCheckbox(accepted, { accepted = it })
        AppButton("Continuar", onAccept, enabled = accepted, modifier = Modifier.fillMaxWidth())
        AppButton("Cerrar sesión", onDecline, kind = ButtonKind.Ghost, modifier = Modifier.fillMaxWidth())
    }
}
