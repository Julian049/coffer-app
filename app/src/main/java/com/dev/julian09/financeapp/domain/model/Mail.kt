package com.dev.julian09.financeapp.domain.model

import java.time.Duration
import java.util.UUID

/** What a provider of receipts is, for its label in Proveedores de correo. */
enum class MailProviderKind { BANK, MERCHANT }

/**
 * Who sends the emails a [TransactionDraft] comes from (2.3-02). [source] is the server's reader; [asksCard] providers are
 * credit cards, associated with a card and its last 4 digits.
 */
data class MailProvider(
    val source: String,
    val name: String,
    val kind: MailProviderKind,
    val asksCard: Boolean,
    /** Which emails it covers, under its name in Hoja · Asociar proveedor. */
    val covers: String,
) {
    /** A card's digits only match cards; a bank account's never match a card ending the same (roadmap-v2.3 2.3-02). */
    fun accepts(type: AccountType): Boolean =
        when {
            asksCard -> type == AccountType.CREDIT_CARD
            kind == MailProviderKind.BANK -> type != AccountType.CREDIT_CARD
            else -> true
        }
}

/**
 * The providers the server reads (finance-server `ReceiptReader`s), in the order Proveedores de correo lists them. A new
 * provider is one line here and its logo in `mailProviderLogo`; its drafts arrive before that with [name] as fallback.
 */
object MailProviders {
    val all =
        listOf(
            MailProvider("UBER", "Uber", MailProviderKind.MERCHANT, asksCard = false, covers = "Recibos de viajes y de Uber Eats"),
            MailProvider(
                "BANCOLOMBIA",
                "Bancolombia",
                MailProviderKind.BANK,
                asksCard = false,
                covers = "Pagos, compras y transferencias de tu cuenta",
            ),
            MailProvider(
                "BANCOLOMBIA_TC",
                "Bancolombia · Tarjeta de crédito",
                MailProviderKind.BANK,
                asksCard = true,
                covers = "Alertas de compras con tus tarjetas de crédito Bancolombia",
            ),
            MailProvider(
                "RAPPICUENTA",
                "RappiCuenta",
                MailProviderKind.BANK,
                asksCard = false,
                covers = "Transferencias que recibes en RappiCuenta",
            ),
            MailProvider("RAPPICARD", "RappiCard", MailProviderKind.BANK, asksCard = true, covers = "Compras con tu tarjeta RappiCard"),
        )

    fun of(source: String): MailProvider? = all.firstOrNull { it.source == source }

    /** An unknown source shows as the server wrote it, «NUEVO_BANCO» → «Nuevo banco», instead of failing (2.3-01). */
    fun name(source: String): String = of(source)?.name ?: source.lowercase().replace('_', ' ').replaceFirstChar(Char::titlecase)
}

/** REQ-COR-11: the drafts of [source], or only those with [lastDigits], go to [accountId]. */
data class MailRule(
    val id: UUID,
    val source: String,
    val lastDigits: String?,
    val accountId: UUID,
)

/**
 * The account a receipt of [source] with [digits] goes to: the user's rule for both, else the rule for the provider, else
 * the account with those digits whose type the provider accepts (REQ-COR-06, REQ-COR-11). Only active, pickable accounts.
 */
fun accountFor(
    source: String,
    digits: String?,
    accounts: List<Account>,
    rules: List<MailRule>,
): UUID? {
    val usable = accounts.filter { it.active && it.type.isPickable }
    val ruled =
        rules.firstOrNull { it.source == source && digits != null && it.lastDigits == digits && usable.any { a -> a.id == it.accountId } }
            ?: rules.firstOrNull { it.source == source && it.lastDigits == null && usable.any { a -> a.id == it.accountId } }
    if (ruled != null) return ruled.accountId
    digits ?: return null
    val provider = MailProviders.of(source)
    return usable.firstOrNull { it.lastDigits == digits && provider?.accepts(it.type) != false }?.id
}

/** REQ-COR-12: how close in time the two halves of a transfer between own accounts arrive. */
val TRANSFER_WINDOW: Duration = Duration.ofSeconds(60)

/**
 * REQ-COR-12: an expense and an income of the same amount, at most [TRANSFER_WINDOW] apart, whose accounts are both known
 * and different, become one TRANSFER draft (the expense's id, its partner in [TransactionDraft.pairedId]). The rest stay as
 * they are, in their order; two payments of the same amount to other people never pair, as neither is an income.
 */
fun pairTransfers(
    drafts: List<TransactionDraft>,
    accountOf: (TransactionDraft) -> UUID?,
): List<TransactionDraft> {
    val partners = mutableMapOf<UUID, TransactionDraft>()
    for (out in drafts.filter { it.type == TransactionType.EXPENSE }) {
        val from = accountOf(out) ?: continue
        partners[out.id] =
            drafts.firstOrNull { inc ->
                inc.type == TransactionType.INCOME &&
                    inc.amount == out.amount &&
                    inc !in partners.values &&
                    Duration.between(out.occurredAt, inc.occurredAt).abs() <= TRANSFER_WINDOW &&
                    accountOf(inc).let { it != null && it != from }
            } ?: continue
    }
    val paired = partners.values.map { it.id }.toSet()
    return drafts.filter { it.id !in paired }.map { d ->
        partners[d.id]?.let { inc ->
            d.copy(
                type = TransactionType.TRANSFER,
                merchant = null,
                pairedId = inc.id,
                destinationSource = inc.source,
                destinationDigits = inc.lastDigits,
            )
        } ?: d
    }
}
