package com.dev.julian09.financeapp.domain.usecase

import com.dev.julian09.financeapp.domain.model.Account
import com.dev.julian09.financeapp.domain.model.MailProvider
import com.dev.julian09.financeapp.domain.model.MailProviders
import com.dev.julian09.financeapp.domain.model.MailRule
import com.dev.julian09.financeapp.domain.port.AccountRepository
import com.dev.julian09.financeapp.domain.port.MailRuleRepository
import com.dev.julian09.financeapp.domain.port.SyncScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.UUID
import javax.inject.Inject

/** One provider of Proveedores de correo: associated when its rule points to an active account (REQ-COR-11). */
data class MailProviderLine(
    val provider: MailProvider,
    val rule: MailRule?,
    val account: Account?,
) {
    val associated: Boolean get() = account != null
}

/** The catalog with each provider's rule, and the accounts each one may be associated with. */
data class MailProvidersOverview(
    val lines: List<MailProviderLine>,
    val accounts: List<Account>,
) {
    /** A card provider lists only credit cards; a bank account never a card (roadmap-v2.3 2.3-02). */
    fun choicesFor(provider: MailProvider): List<Account> = accounts.filter { it.type.isPickable && provider.accepts(it.type) }

    /** Ajustes → Correos: «3 de 5 asociados · Uber, Bancolombia y más». */
    val summary: String
        get() {
            val names = lines.filter { it.associated }.map { it.provider.name }
            if (names.isEmpty()) return "Ninguno asociado"
            val shown = if (names.size > 2) "${names.take(2).joinToString(", ")} y más" else names.joinToString(" y ")
            return "${names.size} de ${lines.size} ${if (names.size == 1) "asociado" else "asociados"} · $shown"
        }
}

class ObserveMailProviders
    @Inject
    constructor(
        private val rules: MailRuleRepository,
        private val accounts: AccountRepository,
    ) {
        operator fun invoke(): Flow<MailProvidersOverview> =
            combine(rules.observeAll(), accounts.observeActive()) { ruled, active ->
                MailProvidersOverview(
                    MailProviders.all.map { p ->
                        val rule = ruled.firstOrNull { it.source == p.source }
                        MailProviderLine(p, rule, rule?.let { r -> active.firstOrNull { it.id == r.accountId } })
                    },
                    active,
                )
            }
    }

/** Hoja · Asociar proveedor and Hoja · Asociar tarjeta de crédito: a card provider needs its 4 digits. False when invalid. */
class SaveMailRule
    @Inject
    constructor(
        private val rules: MailRuleRepository,
        private val scheduler: SyncScheduler,
    ) {
        suspend operator fun invoke(
            source: String,
            accountId: UUID,
            digits: String?,
        ): Boolean {
            val card = MailProviders.of(source)?.asksCard == true
            if (card && (digits == null || !Regex("^[0-9]{4}$").matches(digits))) return false
            rules.save(source, digits.takeIf { card }, accountId)
            scheduler.requestSync()
            return true
        }
    }

/** «Quitar asociación»: the provider's drafts fall back to the digits. */
class RemoveMailRule
    @Inject
    constructor(
        private val rules: MailRuleRepository,
        private val scheduler: SyncScheduler,
    ) {
        suspend operator fun invoke(source: String) {
            rules.remove(source)
            scheduler.requestSync()
        }
    }
