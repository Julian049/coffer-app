package com.dev.julian09.financeapp.ui.feature.mail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.julian09.financeapp.domain.model.MailProvider
import com.dev.julian09.financeapp.domain.usecase.MailProvidersOverview
import com.dev.julian09.financeapp.domain.usecase.ObserveMailProviders
import com.dev.julian09.financeapp.domain.usecase.RemoveMailRule
import com.dev.julian09.financeapp.domain.usecase.SaveMailRule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/** Hoja · Asociar proveedor (or tarjeta de crédito, when [MailProvider.asksCard]) while it is open. */
data class MailProviderSheet(
    val provider: MailProvider,
    val accountId: UUID? = null,
    /** Digits only, at most 4. */
    val digits: String = "",
    /** Shows «Quitar asociación». */
    val associated: Boolean = false,
) {
    val canSave: Boolean get() = accountId != null && (!provider.asksCard || digits.length == 4)
}

@HiltViewModel
class MailProvidersViewModel
    @Inject
    constructor(
        observe: ObserveMailProviders,
        private val save: SaveMailRule,
        private val remove: RemoveMailRule,
    ) : ViewModel() {
        val overview: StateFlow<MailProvidersOverview?> = observe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

        private val _sheet = MutableStateFlow<MailProviderSheet?>(null)
        val sheet: StateFlow<MailProviderSheet?> = _sheet.asStateFlow()

        fun open(provider: MailProvider) {
            val line = overview.value?.lines?.firstOrNull { it.provider == provider } ?: return
            _sheet.value =
                MailProviderSheet(
                    provider,
                    accountId = line.account?.id,
                    digits = line.rule?.lastDigits ?: line.account?.lastDigits.orEmpty(),
                    associated = line.associated,
                )
        }

        /** The digits come from the chosen card, and can still be changed (changes-ui-2.3 §3.4). */
        fun choose(accountId: UUID) =
            _sheet.update { s ->
                val digits =
                    overview.value
                        ?.accounts
                        ?.firstOrNull { it.id == accountId }
                        ?.lastDigits
                s?.copy(accountId = accountId, digits = digits ?: s.digits)
            }

        fun digits(value: String) = _sheet.update { it?.copy(digits = value.filter(Char::isDigit).take(4)) }

        fun dismiss() {
            _sheet.value = null
        }

        fun confirm() {
            val s = _sheet.value ?: return
            val account = s.accountId ?: return
            viewModelScope.launch { if (save(s.provider.source, account, s.digits)) dismiss() }
        }

        fun unlink() {
            val s = _sheet.value ?: return
            viewModelScope.launch {
                remove(s.provider.source)
                dismiss()
            }
        }
    }
