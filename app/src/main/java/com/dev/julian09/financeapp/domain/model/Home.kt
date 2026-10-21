package com.dev.julian09.financeapp.domain.model

/** The cards of Inicio that Personalizar inicio shows, hides and orders (REQ-INI-04; the contract's homeCards keys). */
enum class HomeSection { BALANCE, QUICK_ACTIONS, ACCOUNTS, PENDING, PINNED_TAGS, RECENT, MONTHLY_TREND }

/**
 * What Home shows, in order: the stored cards it knows, then any it is missing, visible, so an empty value (every
 * user before S11) and a section added later both keep Home as it was.
 */
fun homeLayout(stored: List<HomeCard>): List<HomeCard> {
    val known = stored.filter { card -> HomeSection.entries.any { it.name == card.key } }.distinctBy { it.key }
    val missing = HomeSection.entries.filter { s -> known.none { it.key == s.name } }.map { HomeCard(it.name, true) }
    return known + missing
}
