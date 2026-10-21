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
import com.dev.julian09.financeapp.domain.finance.LoanScheduler
import com.dev.julian09.financeapp.domain.finance.RecurrenceCalculator
import com.dev.julian09.financeapp.domain.finance.ReportAggregator
import com.dev.julian09.financeapp.domain.finance.ReportSpan
import com.dev.julian09.financeapp.domain.model.Account
import com.dev.julian09.financeapp.domain.model.AccountBalance
import com.dev.julian09.financeapp.domain.model.AccountFlows
import com.dev.julian09.financeapp.domain.model.AccountResolution
import com.dev.julian09.financeapp.domain.model.AccountType
import com.dev.julian09.financeapp.domain.model.Amortization
import com.dev.julian09.financeapp.domain.model.AuthField
import com.dev.julian09.financeapp.domain.model.CardOverview
import com.dev.julian09.financeapp.domain.model.CardPeriod
import com.dev.julian09.financeapp.domain.model.CardStatement
import com.dev.julian09.financeapp.domain.model.Category
import com.dev.julian09.financeapp.domain.model.CategoryKind
import com.dev.julian09.financeapp.domain.model.CategoryRef
import com.dev.julian09.financeapp.domain.model.CategoryResolution
import com.dev.julian09.financeapp.domain.model.ChangeAction
import com.dev.julian09.financeapp.domain.model.CreditCard
import com.dev.julian09.financeapp.domain.model.DeferredPurchase
import com.dev.julian09.financeapp.domain.model.Frequency
import com.dev.julian09.financeapp.domain.model.HomeCard
import com.dev.julian09.financeapp.domain.model.ImportPreview
import com.dev.julian09.financeapp.domain.model.ImportSummary
import com.dev.julian09.financeapp.domain.model.InitialSyncProgress
import com.dev.julian09.financeapp.domain.model.Installment
import com.dev.julian09.financeapp.domain.model.InstallmentStatus
import com.dev.julian09.financeapp.domain.model.Loan
import com.dev.julian09.financeapp.domain.model.LoanInstallment
import com.dev.julian09.financeapp.domain.model.LoanMode
import com.dev.julian09.financeapp.domain.model.LoanOverview
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.RateType
import com.dev.julian09.financeapp.domain.model.Recurring
import com.dev.julian09.financeapp.domain.model.RecurringOverview
import com.dev.julian09.financeapp.domain.model.RecurringPayment
import com.dev.julian09.financeapp.domain.model.RecurringSummary
import com.dev.julian09.financeapp.domain.model.RowError
import com.dev.julian09.financeapp.domain.model.RowProblem
import com.dev.julian09.financeapp.domain.model.SearchTotals
import com.dev.julian09.financeapp.domain.model.SyncStatus
import com.dev.julian09.financeapp.domain.model.Tag
import com.dev.julian09.financeapp.domain.model.TagTotal
import com.dev.julian09.financeapp.domain.model.Transaction
import com.dev.julian09.financeapp.domain.model.TransactionType
import com.dev.julian09.financeapp.domain.model.homeLayout
import com.dev.julian09.financeapp.domain.port.ImportStatus
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
import com.dev.julian09.financeapp.ui.feature.cards.CardDetailSections
import com.dev.julian09.financeapp.ui.feature.cards.CardFormActions
import com.dev.julian09.financeapp.ui.feature.cards.CardFormContent
import com.dev.julian09.financeapp.ui.feature.cards.CardFormState
import com.dev.julian09.financeapp.ui.feature.cards.PayCardActions
import com.dev.julian09.financeapp.ui.feature.cards.PayCardContent
import com.dev.julian09.financeapp.ui.feature.cards.PayCardState
import com.dev.julian09.financeapp.ui.feature.categories.CategoriesContent
import com.dev.julian09.financeapp.ui.feature.categories.CategoriesState
import com.dev.julian09.financeapp.ui.feature.categories.CategoryLine
import com.dev.julian09.financeapp.ui.feature.categories.CategoryNode
import com.dev.julian09.financeapp.ui.feature.changes.ChangeDay
import com.dev.julian09.financeapp.ui.feature.changes.ChangeHistoryContent
import com.dev.julian09.financeapp.ui.feature.changes.ChangeRow
import com.dev.julian09.financeapp.ui.feature.history.HistoryActions
import com.dev.julian09.financeapp.ui.feature.history.HistoryChoices
import com.dev.julian09.financeapp.ui.feature.history.HistoryContent
import com.dev.julian09.financeapp.ui.feature.history.HistoryDay
import com.dev.julian09.financeapp.ui.feature.history.HistoryFilters
import com.dev.julian09.financeapp.ui.feature.history.HistoryRow
import com.dev.julian09.financeapp.ui.feature.history.HistoryUiState
import com.dev.julian09.financeapp.ui.feature.history.buildDays
import com.dev.julian09.financeapp.ui.feature.home.CustomizeHomeContent
import com.dev.julian09.financeapp.ui.feature.home.CustomizeHomeState
import com.dev.julian09.financeapp.ui.feature.home.HomeContent
import com.dev.julian09.financeapp.ui.feature.home.HomeUiState
import com.dev.julian09.financeapp.ui.feature.importexport.ExportChoice
import com.dev.julian09.financeapp.ui.feature.importexport.ImportCatalog
import com.dev.julian09.financeapp.ui.feature.importexport.ImportExportActions
import com.dev.julian09.financeapp.ui.feature.importexport.ImportExportContent
import com.dev.julian09.financeapp.ui.feature.importexport.ImportExportState
import com.dev.julian09.financeapp.ui.feature.importexport.ImportStep
import com.dev.julian09.financeapp.ui.feature.loans.LoanFormActions
import com.dev.julian09.financeapp.ui.feature.loans.LoanFormContent
import com.dev.julian09.financeapp.ui.feature.loans.LoanFormState
import com.dev.julian09.financeapp.ui.feature.loans.LoanPaymentActions
import com.dev.julian09.financeapp.ui.feature.loans.LoanPaymentContent
import com.dev.julian09.financeapp.ui.feature.loans.LoanPaymentState
import com.dev.julian09.financeapp.ui.feature.loans.LoanSheet
import com.dev.julian09.financeapp.ui.feature.recurring.RecurringActions
import com.dev.julian09.financeapp.ui.feature.recurring.RecurringChoices
import com.dev.julian09.financeapp.ui.feature.recurring.RecurringContent
import com.dev.julian09.financeapp.ui.feature.recurring.RecurringFormActions
import com.dev.julian09.financeapp.ui.feature.recurring.RecurringFormContent
import com.dev.julian09.financeapp.ui.feature.recurring.RecurringFormState
import com.dev.julian09.financeapp.ui.feature.recurring.RecurringUiState
import com.dev.julian09.financeapp.ui.feature.reports.ReportsActions
import com.dev.julian09.financeapp.ui.feature.reports.ReportsContent
import com.dev.julian09.financeapp.ui.feature.reports.ReportsState
import com.dev.julian09.financeapp.ui.feature.settings.ServerState
import com.dev.julian09.financeapp.ui.feature.settings.SettingsContent
import com.dev.julian09.financeapp.ui.feature.settings.SettingsUiState
import com.dev.julian09.financeapp.ui.feature.sync.InitialDownloadContent
import com.dev.julian09.financeapp.ui.feature.tags.TagDetailActions
import com.dev.julian09.financeapp.ui.feature.tags.TagDetailContent
import com.dev.julian09.financeapp.ui.feature.tags.TagDetailState
import com.dev.julian09.financeapp.ui.feature.tags.TagLine
import com.dev.julian09.financeapp.ui.feature.tags.TagRange
import com.dev.julian09.financeapp.ui.feature.tags.TagsContent
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
import com.dev.julian09.financeapp.domain.model.Settings as UserSettings

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

    @Test
    fun reportsLight() = capture("reports_light", dark = false) { Reports() }

    @Test
    fun reportsDark() = capture("reports_dark", dark = true) { Reports() }

    @Test
    fun tagsLight() = capture("tags_light", dark = false) { Tags() }

    @Test
    fun tagDetailLight() = capture("tag_detail_light", dark = false) { TagDetail() }

    @Test
    fun changeHistoryLight() = capture("change_history_light", dark = false) { ChangeHistory() }

    @Test
    fun customizeHomeLight() = capture("customize_home_light", dark = false) { CustomizeHome() }

    @Test
    fun loanFormLight() = capture("loan_form_light", dark = false) { LoanForm() }

    @Test
    fun loanDetailLight() = capture("loan_detail_light", dark = false) { LoanDetail() }

    @Test
    fun loanExtraPaymentLight() = capture("loan_extra_payment_light", dark = false) { LoanExtra() }

    @Test
    fun importExportLight() = capture("import_export_light", dark = false) { ImportExport() }

    @Test
    fun importExportDark() = capture("import_export_dark", dark = true) { ImportExport() }

    @Test
    fun importReviewLight() = capture("import_review_light", dark = false) { ImportReview() }

    @Test
    fun cardFormLight() = capture("card_form_light", dark = false) { CardForm() }

    @Test
    fun cardDetailLight() = capture("card_detail_light", dark = false) { CardDetail() }

    @Test
    fun cardDetailDark() = capture("card_detail_dark", dark = true) { CardDetail() }

    @Test
    fun payCardLight() = capture("pay_card_light", dark = false) { PayCard() }

    @Test
    fun transactionCardLight() = capture("transaction_card_light", dark = false) { TransactionWithCard() }

    @Test
    fun cardPaymentDetailLight() = capture("card_payment_detail_light", dark = false) { CardPaymentDetail() }

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
            reminders = UserSettings(),
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
                fox = FoxMood(FoxState.GOOD),
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
    private fun Reports() {
        val home = Category(UUID.randomUUID(), "Hogar", CategoryKind.EXPENSE, icon = "home")
        val food = Category(UUID.randomUUID(), "Comida", CategoryKind.EXPENSE, icon = "restaurant")
        val salary = Category(UUID.randomUUID(), "Salario", CategoryKind.INCOME, icon = "work")
        val card = UUID.randomUUID()
        val month = ReportAggregator.period(ReportSpan.MONTH, LocalDate.of(2026, 10, 20))
        val rows =
            (0L..5L).flatMap { back ->
                val day =
                    LocalDate
                        .of(2026, 10, 10)
                        .minusMonths(back)
                        .atStartOfDay()
                        .toInstant(ZoneOffset.UTC)
                listOf(
                    Transaction(
                        UUID.randomUUID(),
                        TransactionType.INCOME,
                        Money.ofPesos(2_850_000 - back * 90_000),
                        day,
                        card,
                        categoryId = salary.id,
                    ),
                    Transaction(
                        UUID.randomUUID(),
                        TransactionType.EXPENSE,
                        Money.ofPesos(620_000 + back * 40_000),
                        day,
                        card,
                        categoryId = home.id,
                    ),
                    Transaction(UUID.randomUUID(), TransactionType.EXPENSE, Money.ofPesos(412_400), day, card, categoryId = food.id),
                    Transaction(
                        UUID.randomUUID(),
                        TransactionType.TRANSFER,
                        Money.ofPesos(250_000),
                        day,
                        UUID.randomUUID(),
                        destinationAccountId = card,
                        interest = Money.ofPesos(42_000),
                    ),
                )
            }
        val report =
            ReportAggregator.aggregate(
                ReportSpan.MONTH,
                month,
                LocalDate.of(2026, 10, 20),
                ZoneOffset.UTC,
                rows,
                listOf(home, food, salary),
                emptyList(),
                emptyList(),
                emptyList(),
            )
        ReportsContent(
            ReportsState(ReportSpan.MONTH, month, report, mapOf(card to "Tarjeta Visa Platinum")),
            ReportsActions(),
            avatarInitial = 'J',
        )
    }

    @Composable
    private fun ChangeHistory() {
        ChangeHistoryContent(
            listOf(
                ChangeDay(
                    LocalDate.of(2026, 10, 7),
                    listOf(
                        ChangeRow(
                            UUID(5, 1),
                            ChangeAction.DELETE,
                            "Eliminaste «Almuerzo»",
                            "−$28.000 · Efectivo · 10:42\u00A0a.\u00A0m.",
                            canRestore = true,
                        ),
                        ChangeRow(
                            UUID(5, 2),
                            ChangeAction.UPDATE,
                            "Editaste «Taxi»",
                            "Monto: $12.000 → $14.000 · 9:15\u00A0a.\u00A0m.",
                            canRevert = true,
                        ),
                    ),
                ),
                ChangeDay(
                    LocalDate.of(2026, 10, 6),
                    listOf(
                        ChangeRow(
                            UUID(5, 3),
                            ChangeAction.CREATE,
                            "Creaste «Honorarios asesoría»",
                            "+$450.000 · Nu Ahorros · 6:30\u00A0p.\u00A0m.",
                        ),
                    ),
                ),
            ),
            onBack = {},
            onUndo = {},
            today = LocalDate.of(2026, 10, 7),
        )
    }

    @Composable
    private fun CustomizeHome() {
        CustomizeHomeContent(
            CustomizeHomeState(
                cards = homeLayout(listOf(HomeCard("MONTHLY_TREND", false))).let { it.drop(1) + it.first() },
                accounts = BALANCES.map { it.account },
                loaded = true,
            ),
            onBack = {},
            onMoveCard = { _, _ -> },
            onVisible = { _, _ -> },
            onMoveAccount = { _, _ -> },
            onSave = {},
        )
    }

    @Composable
    private fun Tags() {
        fun line(
            name: String,
            pinned: Boolean,
            pesos: Long,
            count: Int,
        ) = Tag(UUID.randomUUID(), name, pinned).let { TagLine(it, TagTotal(it.id, Money.ZERO, Money.ofPesos(pesos), count)) }
        TagsContent(
            listOf(line("Viaje Cartagena", true, 1_240_000, 12), line("Salidas", true, 182_000, 8), line("Trabajo", false, 54_000, 3)),
            onBack = {},
            onOpenTag = {},
            onNew = {},
            onTogglePin = {},
        )
    }

    @Composable
    private fun TagDetail() {
        val tag = Tag(UUID.randomUUID(), "Viaje Cartagena", pinned = true)
        val visa = UUID.randomUUID()
        val lodging = Category(UUID.randomUUID(), "Alojamiento", CategoryKind.EXPENSE, icon = "home")
        val transport = Category(UUID.randomUUID(), "Transporte", CategoryKind.EXPENSE, icon = "directions_bus")
        val food = Category(UUID.randomUUID(), "Comida", CategoryKind.EXPENSE, icon = "restaurant")

        fun tx(
            pesos: Long,
            day: Int,
            category: Category,
            note: String,
        ) = Transaction(
            UUID.randomUUID(),
            TransactionType.EXPENSE,
            Money.ofPesos(pesos),
            LocalDate.of(2026, 10, day).atTime(15, 0).toInstant(ZoneOffset.UTC),
            visa,
            categoryId = category.id,
            tagId = tag.id,
            note = note,
        )
        val rows =
            listOf(
                tx(680_000, 4, lodging, "Hotel Caribe"),
                tx(300_000, 2, transport, "Vuelo Bogotá – Cartagena"),
                tx(96_000, 5, food, "Restaurante La Cevichería"),
            )
        TagDetailContent(
            TagDetailState(
                tag = tag,
                range = TagRange.THIS_MONTH,
                from = LocalDate.of(2026, 10, 1),
                to = LocalDate.of(2026, 10, 31),
                income = Money.ZERO,
                expense = Money.ofPesos(1_076_000),
                count = 3,
                byCategory = listOf(lodging to Money.ofPesos(680_000), transport to Money.ofPesos(300_000), food to Money.ofPesos(96_000)),
                days = buildDays(rows, mapOf(visa to "Tarjeta Visa"), listOf(lodging, transport, food), ZoneOffset.UTC),
            ),
            TagDetailActions(),
            today = LocalDate.of(2026, 10, 16),
        )
    }

    private fun loanOverview(): LoanOverview {
        val id = UUID(9, 9)
        val loan =
            Loan(
                accountId = id,
                mode = LoanMode.DETAILED,
                paymentDay = 5,
                disbursementAccountId = UUID(9, 1),
                principal = Money.ofPesos(5_000_000),
                installmentsCount = 24,
                rate = java.math.BigDecimal("0.015"),
                rateType = RateType.MV,
                firstDueDate = LocalDate.of(2026, 8, 5),
                lifeInsurance = Money.ofPesos(8_000),
                amortization = Amortization.FIXED_PRINCIPAL,
            )
        val plan =
            LoanScheduler
                .plan(
                    Money.ofPesos(5_000_000),
                    24,
                    java.math.BigDecimal("0.015"),
                    LocalDate.of(2026, 8, 5),
                    5,
                    Amortization.FIXED_PRINCIPAL,
                    Money.ofPesos(8_000),
                ).mapIndexed { i, p ->
                    LoanInstallment(
                        UUID(9, 100L + i),
                        id,
                        p.number,
                        p.dueDate,
                        p.principal,
                        p.interest,
                        p.insurance,
                        p.otherCosts,
                        if (i <
                            3
                        ) {
                            InstallmentStatus.PAID
                        } else {
                            InstallmentStatus.SCHEDULED
                        },
                    )
                }
        val account = Account(id, "Préstamo libre inversión", AccountType.LOAN, Money.ZERO)
        val debt = plan.drop(3).fold(Money.ZERO) { s, it -> s + it.principal }
        return LoanOverview(loan, AccountBalance(account, AccountFlows(transfersOut = debt)), plan)
    }

    @Composable
    private fun LoanForm() {
        LoanFormContent(
            LoanFormState(
                name = "Préstamo libre inversión",
                amountDigits = "5000000",
                destinationId = BALANCES[1].account.id,
                countText = "24",
                paymentDay = "5",
                rateText = "1,5",
                amortization = Amortization.FIXED_PAYMENT,
                firstDueDate = LocalDate.of(2026, 11, 5),
                insurance = true,
                insuranceDigits = "8000",
            ),
            BALANCES.map { it.account }.filter { !it.type.isDebt },
            LoanFormActions(),
        )
    }

    @Composable
    private fun LoanDetail() {
        val overview = loanOverview()
        AccountDetailContent(AccountDetailState(overview.account, emptyList(), loan = overview), onBack = {
        }, onEdit = {}, onOpenHistory = {}, today = TODAY)
    }

    @Composable
    private fun LoanExtra() {
        Column(Modifier.padding(16.dp)) {
            LoanPaymentContent(
                LoanPaymentState(loanOverview(), LoanSheet.EXTRA, amountDigits = "1000000", sourceId = BALANCES[1].account.id),
                BALANCES.filter { !it.account.type.isDebt },
                LoanPaymentActions(),
            )
        }
    }

    @Composable
    private fun ImportExport() {
        ImportExportContent(
            ImportExportState(export = ExportChoice.THIS_MONTH),
            ImportCatalog(
                status =
                    ImportStatus(
                        last = ImportSummary("cashew.csv", Instant.parse("2026-10-19T15:00:00Z"), 4_870, 12, 3),
                    ),
            ),
            ImportExportActions(),
            zone = ZoneOffset.UTC,
        )
    }

    @Composable
    private fun ImportReview() {
        val dining = CategoryRef(CategoryKind.EXPENSE, "Dining")
        ImportExportContent(
            ImportExportState(
                step = ImportStep.REVIEW,
                fileName = "cashew.csv",
                preview =
                    ImportPreview(
                        headers = listOf("account", "amount", "date"),
                        mapping = emptyMap(),
                        sample = emptyList(),
                        valid = 33,
                        errors = listOf(RowError(15, RowProblem.DATE)),
                        errorCount = 1,
                        duplicates = 2,
                        unknownAccounts = listOf("RappiCard"),
                        unknownCategories = listOf(dining),
                    ),
                accounts = mapOf("RappiCard" to AccountResolution.Create(AccountType.SAVINGS)),
                categories = mapOf(dining to CategoryResolution.Use(CATEGORY_FOOD.id)),
            ),
            ImportCatalog(accounts = BALANCES.map { it.account }, categories = listOf(CATEGORY_FOOD)),
            ImportExportActions(),
            zone = ZoneOffset.UTC,
        )
    }

    @Composable
    private fun CardForm() {
        CardFormContent(
            CardFormState(
                name = "Visa Platinum",
                limitDigits = "5000000",
                debtDigits = "800000",
                cutOffDay = "20",
                paymentDay = "5",
                rateText = "2,0",
                firstInstallmentInterestFree = true,
            ),
            CardFormActions(),
        )
    }

    @Composable
    private fun CardDetail() {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CardDetailSections(CARD_OVERVIEW, onPay = {})
        }
    }

    @Composable
    private fun PayCard() {
        Column(Modifier.padding(16.dp)) {
            PayCardContent(
                PayCardState(CARD_OVERVIEW, amountDigits = "250000", sourceId = BALANCES[1].account.id),
                BALANCES,
                PayCardActions(),
            )
        }
    }

    @Composable
    private fun TransactionWithCard() {
        TransactionFormContent(
            TransactionFormState(
                amountDigits = "1200000",
                categoryId = CATEGORY_FOOD.id,
                accountId = CARD_ID,
                occurredAt = Instant.parse("2026-10-17T18:10:00Z"),
                installments = 12,
            ),
            TransactionChoices(BALANCES + CARD_OVERVIEW.account, listOf(CATEGORY_FOOD), listOf(TAG), mapOf(CARD_ID to CARD_OVERVIEW)),
            TransactionFormActions(),
            today = TODAY,
            zone = ZoneOffset.UTC,
        )
    }

    @Composable
    private fun CardPaymentDetail() {
        TransactionDetailContent(
            TransactionDetail(
                Transaction(
                    UUID(3, 2),
                    TransactionType.TRANSFER,
                    Money.ofPesos(250_000),
                    Instant.parse("2026-10-17T14:02:00Z"),
                    BALANCES[1].account.id,
                    destinationAccountId = CARD_ID,
                    note = "Pago Visa Platinum",
                    principal = Money.ofPesos(218_000),
                    interest = Money.ofPesos(32_000),
                    fees = Money.ZERO,
                    paymentKind = "CARD_MIN",
                    createdAt = Instant.parse("2026-10-17T14:03:00Z"),
                ),
                null,
                null,
                "Bancolombia",
                "Visa Platinum",
                null,
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
            AccountFormState(name = "Bancolombia Nómina", balanceDigits = "500000", type = AccountType.SAVINGS, icon = "bank"),
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
                    Account(UUID(0, 2), "Bancolombia", AccountType.SAVINGS, Money.ofPesos(500_000), icon = "bank", color = "#2F7A45"),
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

private val CARD_ID = UUID(9, 9)
private val CARD_PURCHASE = UUID(9, 1)

private val CARD_OVERVIEW =
    CardOverview(
        CreditCard(CARD_ID, Money.ofPesos(5_000_000), 20, 5, BigDecimal("0.02"), RateType.MV, true),
        AccountBalance(
            Account(CARD_ID, "Visa Platinum", AccountType.CREDIT_CARD, Money.ZERO),
            AccountFlows(expense = Money.ofPesos(800_000)),
        ),
        CardStatement(
            CardPeriod(LocalDate.parse("2026-09-21"), LocalDate.parse("2026-10-20"), LocalDate.parse("2026-11-05")),
            onePayment = Money.ofPesos(38_000),
            installmentsPrincipal = Money.ofPesos(180_000),
            interest = Money.ofPesos(32_000),
            fees = Money.ZERO,
            paid = Money.ZERO,
            debt = Money.ofPesos(800_000),
            pendingInterest = Money.ofPesos(32_000),
            pendingFees = Money.ZERO,
        ),
        listOf(
            DeferredPurchase(
                CARD_PURCHASE,
                CARD_ID,
                UUID(9, 2),
                Money.ofPesos(1_200_000),
                12,
                BigDecimal("0.02"),
                RateType.MV,
                true,
                LocalDate.parse("2026-09-05"),
            ),
        ),
        (1..12).map {
            Installment(
                UUID(9, 100L + it),
                CARD_PURCHASE,
                it,
                LocalDate.parse("2026-09-05").plusMonths(it - 1L),
                Money.ofPesos(100_000),
                if (it == 1) Money.ZERO else Money.ofPesos((13L - it) * 2_000),
                status = if (it < 3) InstallmentStatus.PAID else InstallmentStatus.SCHEDULED,
            )
        },
    )
