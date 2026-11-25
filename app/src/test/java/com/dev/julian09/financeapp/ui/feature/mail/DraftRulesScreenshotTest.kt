package com.dev.julian09.financeapp.ui.feature.mail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.dev.julian09.financeapp.domain.model.Account
import com.dev.julian09.financeapp.domain.model.AccountBalance
import com.dev.julian09.financeapp.domain.model.AccountFlows
import com.dev.julian09.financeapp.domain.model.AccountType
import com.dev.julian09.financeapp.domain.model.Category
import com.dev.julian09.financeapp.domain.model.CategoryKind
import com.dev.julian09.financeapp.domain.model.DraftRule
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.RuleLevel
import com.dev.julian09.financeapp.domain.model.RuleOrigin
import com.dev.julian09.financeapp.ui.theme.CofferTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.UUID

/** changes-ui-2.5 §2–5: Registro automático (`257:689`), Nueva regla (`258:676`), Ya existe (`259:676`), Editar regla. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi")
class DraftRulesScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val bank = Account(UUID(1, 1), "Bancolombia", AccountType.SAVINGS, Money.ZERO, icon = "bank")
    private val visa = Account(UUID(1, 2), "Tarjeta Visa", AccountType.CREDIT_CARD, Money.ZERO)
    private val restaurants = Category(UUID(2, 1), "Restaurantes", CategoryKind.EXPENSE, icon = "restaurant")
    private val market = Category(UUID(2, 2), "Supermercado", CategoryKind.EXPENSE, icon = "cart")
    private val taxi = Category(UUID(2, 3), "Taxi / Apps", CategoryKind.EXPENSE, icon = "taxi")
    private val cafe = Category(UUID(2, 4), "Cafeterías", CategoryKind.EXPENSE, icon = "cafe")
    private val delivery = Category(UUID(2, 5), "Domicilios", CategoryKind.EXPENSE, icon = "food")
    private val gas = Category(UUID(2, 6), "Gasolina", CategoryKind.EXPENSE, icon = "gas")
    private val categories = listOf(restaurants, market, taxi, cafe, delivery, gas)
    private val choices =
        DraftRuleChoices(
            listOf(AccountBalance(bank, AccountFlows()), AccountBalance(visa, AccountFlows())),
            categories,
            categories.mapIndexed { i, c -> c.id to 10 - i }.toMap(),
        )

    private fun rule(
        n: Long,
        merchant: String,
        note: String,
        category: Category,
        amount: Long? = null,
        account: Account? = null,
        level: RuleLevel = RuleLevel.SURE,
        origin: RuleOrigin = RuleOrigin.USER,
    ) = DraftRule(UUID(3, n), merchant, merchant.lowercase(), amount?.let(Money::ofPesos), account?.id, note, category.id, level, origin)

    private val corral = rule(1, "El Corral", "Almuerzo del trabajo", restaurants, 28_000, bank)
    private val coffee = rule(3, "Juan Valdez", "Café", cafe, 9_800, origin = RuleOrigin.LEARNED)
    private val rappi = rule(5, "Rappi", "Domicilio", delivery, level = RuleLevel.PROBABLE, origin = RuleOrigin.LEARNED)

    private val list =
        DraftRulesState(
            loaded = true,
            sure =
                listOf(
                    DraftRuleRow(corral, restaurants, bank),
                    DraftRuleRow(rule(2, "Uber", "Viaje en Uber", taxi, account = visa), taxi, visa),
                    DraftRuleRow(coffee, cafe, null),
                    DraftRuleRow(rule(4, "Éxito", "Mercado", market, origin = RuleOrigin.LEARNED), market, null),
                ),
            probable =
                listOf(
                    DraftRuleRow(rappi, delivery, null),
                    DraftRuleRow(
                        rule(6, "Terpel", "Gasolina", gas, account = visa, level = RuleLevel.PROBABLE, origin = RuleOrigin.LEARNED),
                        gas,
                        visa,
                    ),
                ),
            categories = categories,
        )

    private val newRule =
        DraftRuleFormState(
            loaded = true,
            merchant = "Uber",
            oneAccount = true,
            accountId = visa.id,
            note = "Viaje en Uber",
            categoryId = taxi.id,
            aliases = listOf("UBER *TRIP", "Uber Eats"),
            suggestions = listOf("Rappi", "Éxito", "Juan Valdez", "Netflix"),
        )

    @Test
    fun listLight() = capture("draft_rules_light", false) { DraftRulesContent(list, {}, {}, {}, { _, _ -> }) }

    @Test
    fun listDark() = capture("draft_rules_dark", true) { DraftRulesContent(list, {}, {}, {}, { _, _ -> }) }

    @Test
    fun emptyLight() =
        capture("draft_rules_empty_light", false) { DraftRulesContent(DraftRulesState(loaded = true), {}, {}, {}, { _, _ -> }) }

    @Test
    fun newRuleLight() = capture("draft_rule_new_light", false) { DraftRuleFormContent(newRule, choices, {}, DraftRuleFormActions()) }

    @Test
    fun newRuleDark() = capture("draft_rule_new_dark", true) { DraftRuleFormContent(newRule, choices, {}, DraftRuleFormActions()) }

    @Test
    fun duplicateLight() =
        capture("draft_rule_duplicate_light", false) {
            DraftRuleFormContent(
                DraftRuleFormState(
                    loaded = true,
                    merchant = "El Corral",
                    exactAmount = true,
                    amountDigits = "28000",
                    oneAccount = true,
                    accountId = bank.id,
                    note = "Almuerzo del trabajo",
                    categoryId = restaurants.id,
                    aliases = listOf("EL CORRAL CC ANDINO"),
                    suggestions = listOf("Rappi", "Éxito", "Juan Valdez", "Netflix"),
                    duplicate = corral,
                ),
                choices,
                {},
                DraftRuleFormActions(),
            )
        }

    @Test
    fun editCofferLight() =
        capture("draft_rule_edit_coffer_light", false) {
            DraftRuleFormContent(editing(coffee, listOf("JUAN VALDEZ CAFE")), choices, {}, DraftRuleFormActions())
        }

    @Test
    fun editProbableDark() =
        capture("draft_rule_edit_probable_dark", true) {
            DraftRuleFormContent(editing(rappi, listOf("RAPPI COLOMBIA")), choices, {}, DraftRuleFormActions())
        }

    private fun editing(
        r: DraftRule,
        aliases: List<String>,
    ) = DraftRuleFormState(
        loaded = true,
        editing = r,
        merchant = r.merchant,
        exactAmount = r.amount != null,
        amountDigits = r.amount?.let { (it.toBigDecimal().toLong()).toString() }.orEmpty(),
        oneAccount = r.accountId != null,
        accountId = r.accountId,
        note = r.note,
        categoryId = r.categoryId,
        aliases = aliases,
    )

    private fun capture(
        name: String,
        dark: Boolean,
        content: @Composable () -> Unit,
    ) {
        compose.setContent {
            CofferTheme(darkTheme = dark) {
                Column(Modifier.fillMaxSize().background(CofferTheme.colors.background)) { content() }
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }
}
