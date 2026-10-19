package com.dev.julian09.financeapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.dev.julian09.financeapp.domain.finance.FoxMood
import com.dev.julian09.financeapp.domain.finance.FoxState
import com.dev.julian09.financeapp.domain.finance.RecurrenceCalculator
import com.dev.julian09.financeapp.domain.model.Account
import com.dev.julian09.financeapp.domain.model.AccountBalance
import com.dev.julian09.financeapp.domain.model.AccountFlows
import com.dev.julian09.financeapp.domain.model.AccountType
import com.dev.julian09.financeapp.domain.model.AuthField
import com.dev.julian09.financeapp.domain.model.Category
import com.dev.julian09.financeapp.domain.model.CategoryKind
import com.dev.julian09.financeapp.domain.model.Frequency
import com.dev.julian09.financeapp.domain.model.InitialSyncProgress
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.Recurring
import com.dev.julian09.financeapp.domain.model.RecurringOverview
import com.dev.julian09.financeapp.domain.model.RecurringPayment
import com.dev.julian09.financeapp.domain.model.RecurringSummary
import com.dev.julian09.financeapp.domain.model.SearchTotals
import com.dev.julian09.financeapp.domain.model.SyncStatus
import com.dev.julian09.financeapp.domain.model.Tag
import com.dev.julian09.financeapp.domain.model.Transaction
import com.dev.julian09.financeapp.domain.model.TransactionType
import com.dev.julian09.financeapp.domain.usecase.HomeDashboard
import com.dev.julian09.financeapp.ui.PROFILE
import com.dev.julian09.financeapp.ui.feature.accounts.AccountDetailContent
import com.dev.julian09.financeapp.ui.feature.accounts.AccountDetailState
import com.dev.julian09.financeapp.ui.feature.accounts.AccountFormContent
import com.dev.julian09.financeapp.ui.feature.accounts.AccountFormState
import com.dev.julian09.financeapp.ui.feature.accounts.AccountsContent
import com.dev.julian09.financeapp.ui.feature.auth.LoginContent
import com.dev.julian09.financeapp.ui.feature.auth.LoginUiState
import com.dev.julian09.financeapp.ui.feature.auth.RegisterContent
import com.dev.julian09.financeapp.ui.feature.auth.RegisterUiState
import com.dev.julian09.financeapp.ui.feature.categories.CategoriesContent
import com.dev.julian09.financeapp.ui.feature.categories.CategoriesState
import com.dev.julian09.financeapp.ui.feature.categories.CategoryLine
import com.dev.julian09.financeapp.ui.feature.categories.CategoryNode
import com.dev.julian09.financeapp.ui.feature.history.HistoryActions
import com.dev.julian09.financeapp.ui.feature.history.HistoryChoices
import com.dev.julian09.financeapp.ui.feature.history.HistoryContent
import com.dev.julian09.financeapp.ui.feature.history.HistoryDay
import com.dev.julian09.financeapp.ui.feature.history.HistoryFilters
import com.dev.julian09.financeapp.ui.feature.history.HistoryRow
import com.dev.julian09.financeapp.ui.feature.history.HistoryUiState
import com.dev.julian09.financeapp.ui.feature.home.HomeContent
import com.dev.julian09.financeapp.ui.feature.home.HomeUiState
import com.dev.julian09.financeapp.ui.feature.recurring.RecurringActions
import com.dev.julian09.financeapp.ui.feature.recurring.RecurringChoices
import com.dev.julian09.financeapp.ui.feature.recurring.RecurringContent
import com.dev.julian09.financeapp.ui.feature.recurring.RecurringFormActions
import com.dev.julian09.financeapp.ui.feature.recurring.RecurringFormContent
import com.dev.julian09.financeapp.ui.feature.recurring.RecurringFormState
import com.dev.julian09.financeapp.ui.feature.recurring.RecurringUiState
import com.dev.julian09.financeapp.ui.feature.settings.ServerState
import com.dev.julian09.financeapp.ui.feature.settings.SettingsContent
import com.dev.julian09.financeapp.ui.feature.settings.SettingsUiState
import com.dev.julian09.financeapp.ui.feature.sync.InitialDownloadContent
import com.dev.julian09.financeapp.ui.feature.transactions.TransactionChoices
import com.dev.julian09.financeapp.ui.feature.transactions.TransactionDetail
import com.dev.julian09.financeapp.ui.feature.transactions.TransactionDetailContent
import com.dev.julian09.financeapp.ui.feature.transactions.TransactionFormActions
import com.dev.julian09.financeapp.ui.feature.transactions.TransactionFormContent
import com.dev.julian09.financeapp.ui.feature.transactions.TransactionFormState
import com.dev.julian09.financeapp.ui.navigation.BottomBar
import com.dev.julian09.financeapp.ui.navigation.Tab
import com.dev.julian09.financeapp.ui.theme.CofferTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneOffset
import java.util.UUID

/**
 * Reference images of the base components in light and dark (src/test/screenshots).
 * After an intended visual change: ./gradlew recordRoborazziDebug and commit the new PNGs.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi")
class ComponentsScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun componentsLight() = capture("components_light", dark = false) { Gallery() }

    @Test
    fun componentsDark() = capture("components_dark", dark = true) { Gallery() }

    @Test
    fun homeLight() = capture("home_light", dark = false) { Home() }

    @Test
    fun homeDark() = capture("home_dark", dark = true) { Home() }

    @Test
    fun loginLight() = capture("login_light", dark = false) { Login() }

    @Test
    fun loginDark() = capture("login_dark", dark = true) { Login() }

    @Test
    fun registerErrorsLight() = capture("register_errors_light", dark = false) { RegisterWithErrors() }

    @Test
    fun logoutDialogLight() = capture("logout_dialog_light", dark = false) { Settings(confirmingLogout = true) }

    @Test
    fun settingsLight() = capture("settings_light", dark = false) { Settings() }

    @Test
    fun settingsDark() = capture("settings_dark", dark = true) { Settings() }

    @Test
    fun initialDownloadLight() = capture("initial_download_light", dark = false) { InitialDownload() }

    @Test
    fun initialDownloadDark() = capture("initial_download_dark", dark = true) { InitialDownload() }

    @Test
    fun historyLight() = capture("history_light", dark = false) { History() }

    @Test
    fun accountsLight() = capture("accounts_light", dark = false) { Accounts() }

    @Test
    fun homePendingLight() = capture("home_pending_light", dark = false) { Home(pending = true) }

    @Test
    fun recurringLight() = capture("recurring_light", dark = false) { RecurringList() }

    @Test
    fun recurringDark() = capture("recurring_dark", dark = true) { RecurringList() }

    @Test
    fun homeNewUserLight() = capture("home_new_user_light", dark = false) { Home(newUser = true) }

    @Test
    fun accountNewLight() = capture("account_new_light", dark = false) { AccountNew() }

    @Test
    fun accountDetailLight() = capture("account_detail_light", dark = false) { AccountDetail() }

    @Test
    fun categoriesLight() = capture("categories_light", dark = false) { Categories() }

    @Test
    fun categoriesDark() = capture("categories_dark", dark = true) { Categories() }

    @Test
    fun transactionExpenseLight() = capture("transaction_expense_light", dark = false) { TransactionForm(TransactionType.EXPENSE) }

    @Test
    fun transactionExpenseDark() = capture("transaction_expense_dark", dark = true) { TransactionForm(TransactionType.EXPENSE) }

    @Test
    fun transactionTransferLight() = capture("transaction_transfer_light", dark = false) { TransactionForm(TransactionType.TRANSFER) }

    @Test
    fun recurringFormLight() = capture("recurring_form_light", dark = false) { RecurringForm() }

    @Test
    fun transactionDetailLight() = capture("transaction_detail_light", dark = false) { TransactionDetailPreview() }

    private fun capture(
        name: String,
        dark: Boolean,
        content: @Composable () -> Unit,
    ) {
        compose.setContent {
            CofferTheme(darkTheme = dark) {
                Column(Modifier.background(CofferTheme.colors.background)) { content() }
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    @Composable
    private fun Gallery() {
        AppHeader(title = "Inicio", actions = { Avatar(onClick = {}) })
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AppCard(highlight = true) {
                Text("Saldo disponible", color = CofferTheme.colors.textSecondary)
                MoneyText(Money.ofPesos(2_750_000), style = MaterialTheme.typography.displaySmall)
            }
            AppCard {
                MoneyText(Money.ofPesos(3_500_000), kind = MoneyKind.Income)
                MoneyText(Money.ofPesos(28_000), kind = MoneyKind.Expense)
                MoneyText(Money.ofPesos(500_000), kind = MoneyKind.Transfer)
                MoneyText(Money.ofPesos(-1_200_000), kind = MoneyKind.Debt)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppChip("Este mes", selected = true, onClick = {})
                AppChip("Gastos", selected = false, onClick = {})
                SyncBadge(SyncStatus.Pending(3))
            }
            SegmentedSelector(
                listOf("Gasto", "Ingreso", "Transferencia"),
                selectedIndex = 0,
                onSelect = {},
                modifier = Modifier.fillMaxWidth(),
            )
            AppButton("Guardar", onClick = {}, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppButton("Cancelar", onClick = {}, kind = ButtonKind.Secondary, modifier = Modifier.width(120.dp))
                AppButton("Ver todo", onClick = {}, kind = ButtonKind.Ghost)
                AppButton("Eliminar", onClick = {}, kind = ButtonKind.Danger)
            }
            EmptyState(title = "Aún no hay movimientos", message = "Tus gastos e ingresos aparecerán aquí.")
        }
    }

    // Full screen: header, empty state and the 4-tab bar pinned to the bottom
    @Composable
    private fun Home(
        newUser: Boolean = false,
        pending: Boolean = false,
    ) {
        val dashboard =
            if (pending) {
                HomeDashboard(
                    Money.ofPesos(2_750_000),
                    null,
                    YearMonth.of(2026, 9),
                    BALANCES,
                    emptyList(),
                    FoxMood(FoxState.ATTENTIVE, payment = PENDING[0]),
                    PENDING.take(2),
                )
            } else if (newUser) {
                HomeDashboard(Money.ofPesos(150_000), null, YearMonth.of(2026, 9), BALANCES.take(1), emptyList(), FoxMood(FoxState.RESTING))
            } else {
                HomeDashboard(
                    Money.ofPesos(2_750_000),
                    BigDecimal("8.4"),
                    YearMonth.of(2026, 9),
                    BALANCES,
                    emptyList(),
                    FoxMood(FoxState.GOOD, change = BigDecimal("8.4")),
                )
            }
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                HomeContent(
                    HomeUiState(dashboard, if (newUser) emptyList() else DAYS),
                    onOpenSettings = {},
                    onOpenHistory = {},
                    onOpenAccount = {},
                    userName = "Julian Bocanegra",
                    syncStatus = SyncStatus.Pending(2),
                    today = TODAY,
                )
            }
            BottomBar(selected = Tab.Home, onSelect = {})
        }
    }

    @Composable
    private fun Settings(confirmingLogout: Boolean = false) {
        SettingsContent(
            state = SettingsUiState("0.3.0", ServerState.Available, user = PROFILE, confirmingLogout = confirmingLogout),
            onBack = {},
            onRetry = {},
            syncStatus = SyncStatus.Synced(null),
        )
    }

    @Composable
    private fun InitialDownload() {
        InitialDownloadContent(
            InitialSyncProgress(received = 120, total = 200, byEntity = mapOf("account" to 5L, "category" to 24L, "transaction" to 91L)),
            onRetry = {},
        )
    }

    @Composable
    private fun History() {
        HistoryContent(
            HistoryUiState(
                month = YearMonth.of(2026, 10),
                months = listOf(YearMonth.of(2026, 10), YearMonth.of(2026, 9), YearMonth.of(2026, 8)),
                query = "",
                filters = HistoryFilters(),
                days = DAYS.map { d -> d.copy(rows = d.rows.map { it.copy(time = LocalTime.of(13, 30)) }) },
                totals = SearchTotals(Money.ofPesos(450_000), Money.ofPesos(38_500), 4),
                hasMore = false,
                choices = HistoryChoices(),
            ),
            HistoryActions(),
            avatarInitial = 'J',
            today = TODAY,
        )
    }

    @Composable
    private fun TransactionForm(type: TransactionType) {
        TransactionFormContent(
            TransactionFormState(
                type = type,
                amountDigits = if (type == TransactionType.TRANSFER) "100000" else "28000",
                categoryId = CATEGORY_FOOD.id,
                accountId = BALANCES[1].account.id,
                destinationId = BALANCES[0].account.id,
                occurredAt = Instant.parse("2026-10-17T18:10:00Z"),
                tagId = TAG.id,
                note = "Almuerzo en restaurante",
            ),
            TransactionChoices(BALANCES, listOf(CATEGORY_FOOD, Category(UUID(2, 2), "Transporte", CategoryKind.EXPENSE)), listOf(TAG)),
            TransactionFormActions(),
            today = TODAY,
            zone = ZoneOffset.UTC,
        )
    }

    @Composable
    private fun RecurringList() {
        val paid =
            RecurringPayment(
                Transaction(
                    UUID(5, 9),
                    TransactionType.EXPENSE,
                    Money.ofPesos(85_000),
                    Instant.parse("2026-10-01T15:00:00Z"),
                    BALANCES[1].account.id,
                    categoryId = CATEGORY_FOOD.id,
                ),
                "Gimnasio",
            )
        RecurringContent(
            RecurringUiState(
                RecurringOverview(
                    PENDING,
                    listOf(paid),
                    RecurringSummary(Money.ofPesos(1_088_000), 3, Money.ofPesos(1_003_000), 1, Money.ofPesos(85_000)),
                ),
                listOf(CATEGORY_FOOD),
                YearMonth.of(2026, 10),
            ),
            RecurringActions(),
            zone = ZoneOffset.UTC,
        )
    }

    @Composable
    private fun RecurringForm() {
        val home = Category(UUID(2, 3), "Hogar", CategoryKind.EXPENSE, icon = "home")
        RecurringFormContent(
            RecurringFormState(
                name = "Plan celular",
                amountDigits = "65000",
                accountId = BALANCES[1].account.id,
                categoryId = UUID(2, 4),
                startDate = TODAY,
                frequency = Frequency.CUSTOM,
            ),
            RecurringChoices(BALANCES, listOf(home, Category(UUID(2, 4), "Servicios públicos", CategoryKind.EXPENSE, home.id))),
            RecurringFormActions(),
        )
    }

    @Composable
    private fun TransactionDetailPreview() {
        TransactionDetailContent(
            TransactionDetail(
                Transaction(
                    UUID(3, 1),
                    TransactionType.EXPENSE,
                    Money.ofPesos(24_500),
                    Instant.parse("2026-10-17T13:15:00Z"),
                    BALANCES[0].account.id,
                    categoryId = CATEGORY_FOOD.id,
                    note = "Desayuno con el equipo",
                    createdAt = Instant.parse("2026-10-17T13:16:00Z"),
                ),
                CATEGORY_FOOD,
                "Comida > Cafeterías",
                "Billetera",
                null,
                "Salidas",
            ),
            onBack = {},
            onEdit = {},
            onDelete = {},
            today = TODAY,
            zone = ZoneOffset.UTC,
        )
    }

    @Composable
    private fun Accounts() {
        AccountsContent(BALANCES, onOpenSettings = {
        }, onNewAccount = {}, onOpenAccount = {}, onOpenOptions = {}, onOpenDeactivated = {}, avatarInitial = 'J')
    }

    @Composable
    private fun AccountNew() {
        AccountFormContent(
            AccountFormState(name = "Bancolombia Nómina", balanceDigits = "500000", type = AccountType.DEBIT, icon = "bank"),
            onBack = {},
            onName = {},
            onBalance = {},
            onType = {},
            onIcon = {},
            onColor = {},
            onInclude = {},
            onSave = {},
        )
    }

    @Composable
    private fun AccountDetail() {
        AccountDetailContent(AccountDetailState(BALANCES[1], DAYS), onBack = {}, onEdit = {}, onOpenHistory = {}, today = TODAY)
    }

    @Composable
    private fun Categories() {
        val food = Category(UUID(1, 1), "Comida", CategoryKind.EXPENSE, icon = "restaurant")

        fun sub(
            n: Long,
            name: String,
            pesos: Long,
            count: Int,
        ) = CategoryLine(Category(UUID(1, n), name, CategoryKind.EXPENSE, food.id), Money.ofPesos(pesos), count)
        CategoriesContent(
            CategoriesState(
                CategoryKind.EXPENSE,
                listOf(
                    CategoryNode(
                        CategoryLine(food, Money.ofPesos(0), 0),
                        listOf(sub(2, "Restaurantes", 390_000, 18), sub(3, "Supermercado", 375_000, 16), sub(4, "Cafeterías", 75_000, 8)),
                    ),
                    CategoryNode(
                        CategoryLine(Category(UUID(1, 5), "Educación", CategoryKind.EXPENSE, icon = "school"), Money.ofPesos(150_000), 2),
                        emptyList(),
                    ),
                ),
                mapOf(CategoryKind.EXPENSE to 10, CategoryKind.INCOME to 2),
            ),
            onBack = {},
            onSelectKind = {},
            onNew = {},
            onOpen = {},
        )
    }

    @Composable
    private fun Login() {
        LoginContent(LoginUiState(identifier = "julian@correo.com"), sessionExpired = false, {}, {}, {}, {})
    }

    @Composable
    private fun RegisterWithErrors() {
        RegisterContent(
            RegisterUiState(
                displayName = "Julian",
                email = "julian@correo",
                username = "jb",
                errors =
                    mapOf(
                        AuthField.Email to "Correo no válido",
                        AuthField.Username to "Entre 3 y 40 caracteres: minúsculas, números, punto o guion bajo",
                        AuthField.Password to "Entre 8 y 128 caracteres",
                    ),
            ),
            onChange = { _, _ -> },
            onSubmit = {},
            onBack = {},
        )
    }

    private companion object {
        val TODAY: LocalDate = LocalDate.of(2026, 10, 17)
        val CATEGORY_FOOD = Category(UUID(2, 1), "Comida", CategoryKind.EXPENSE, icon = "restaurant")
        val TAG = Tag(UUID(4, 1), "Salidas")
        val PENDING =
            listOf(
                Triple("Arriendo apartamento", 900_000L, 1L),
                Triple("Plan teléfono móvil", 65_000L, 5L),
                Triple("Netflix & Spotify", 38_000L, 15L),
            ).mapIndexed { i, (name, pesos, days) ->
                RecurrenceCalculator.pending(
                    Recurring(
                        UUID(5, i.toLong()),
                        name,
                        Money.ofPesos(pesos),
                        UUID(0, 2),
                        CATEGORY_FOOD.id,
                        TODAY,
                        Frequency.MONTHLY,
                        TODAY.plusDays(days),
                    ),
                    TODAY,
                    5,
                )
            }
        val BALANCES =
            listOf(
                AccountBalance(Account(UUID(0, 1), "Billetera", AccountType.CASH, Money.ofPesos(150_000), icon = "cash"), AccountFlows()),
                AccountBalance(
                    Account(UUID(0, 2), "Bancolombia", AccountType.DEBIT, Money.ofPesos(500_000), icon = "bank", color = "#2F7A45"),
                    AccountFlows(Money.ofPesos(2_400_000), Money.ofPesos(1_264_000), Money.ofPesos(886_000), Money.ofPesos(100_000)),
                    38,
                ),
                AccountBalance(Account(UUID(0, 3), "Nu Ahorros", AccountType.SAVINGS, Money.ofPesos(1_750_000)), AccountFlows()),
            )
        val DAYS =
            listOf(
                HistoryDay(
                    TODAY,
                    listOf(
                        HistoryRow("Café y almuerzo", "Comida · Billetera", Money.ofPesos(24_500), TransactionType.EXPENSE, "restaurant"),
                        HistoryRow("Taxi", "Transporte · Bancolombia", Money.ofPesos(14_000), TransactionType.EXPENSE, "car"),
                    ),
                ),
                HistoryDay(
                    TODAY.minusDays(1),
                    listOf(HistoryRow("Honorarios", "Nu Ahorros", Money.ofPesos(450_000), TransactionType.INCOME, "work")),
                ),
                HistoryDay(
                    TODAY.minusDays(2),
                    listOf(HistoryRow("Transferencia", "Bancolombia → Billetera", Money.ofPesos(100_000), TransactionType.TRANSFER)),
                ),
            )
    }
}
