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
import com.dev.julian09.financeapp.domain.model.Account
import com.dev.julian09.financeapp.domain.model.AccountBalance
import com.dev.julian09.financeapp.domain.model.AccountFlows
import com.dev.julian09.financeapp.domain.model.AccountType
import com.dev.julian09.financeapp.domain.model.AuthField
import com.dev.julian09.financeapp.domain.model.Category
import com.dev.julian09.financeapp.domain.model.CategoryKind
import com.dev.julian09.financeapp.domain.model.InitialSyncProgress
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.SyncStatus
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
import com.dev.julian09.financeapp.ui.feature.history.HistoryContent
import com.dev.julian09.financeapp.ui.feature.history.HistoryDay
import com.dev.julian09.financeapp.ui.feature.history.HistoryRow
import com.dev.julian09.financeapp.ui.feature.home.HomeContent
import com.dev.julian09.financeapp.ui.feature.home.HomeUiState
import com.dev.julian09.financeapp.ui.feature.settings.ServerState
import com.dev.julian09.financeapp.ui.feature.settings.SettingsContent
import com.dev.julian09.financeapp.ui.feature.settings.SettingsUiState
import com.dev.julian09.financeapp.ui.feature.sync.InitialDownloadContent
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
import java.time.LocalDate
import java.time.YearMonth
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
    fun homeNewUserLight() = capture("home_new_user_light", dark = false) { Home(newUser = true) }

    @Test
    fun accountNewLight() = capture("account_new_light", dark = false) { AccountNew() }

    @Test
    fun accountDetailLight() = capture("account_detail_light", dark = false) { AccountDetail() }

    @Test
    fun categoriesLight() = capture("categories_light", dark = false) { Categories() }

    @Test
    fun categoriesDark() = capture("categories_dark", dark = true) { Categories() }

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
    private fun Home(newUser: Boolean = false) {
        val dashboard =
            if (newUser) {
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
        val today = LocalDate.of(2026, 10, 17)
        HistoryContent(
            listOf(
                HistoryDay(
                    today,
                    listOf(
                        HistoryRow("Comida", "Efectivo", Money.ofPesos(28_000), TransactionType.EXPENSE),
                        HistoryRow("Transferencia", "Nu → Efectivo", Money.ofPesos(200_000), TransactionType.TRANSFER),
                    ),
                ),
                HistoryDay(today.minusDays(3), listOf(HistoryRow("Salario", "Nu", Money.ofPesos(3_500_000), TransactionType.INCOME))),
            ),
            onOpenSettings = {},
            avatarInitial = 'J',
            today = today,
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
