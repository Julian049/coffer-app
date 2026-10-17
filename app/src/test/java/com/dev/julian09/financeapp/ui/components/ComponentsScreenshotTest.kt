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
import com.dev.julian09.financeapp.domain.model.Account
import com.dev.julian09.financeapp.domain.model.AccountType
import com.dev.julian09.financeapp.domain.model.AuthField
import com.dev.julian09.financeapp.domain.model.InitialSyncProgress
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.SyncStatus
import com.dev.julian09.financeapp.domain.model.TransactionType
import com.dev.julian09.financeapp.ui.PROFILE
import com.dev.julian09.financeapp.ui.feature.accounts.AccountsContent
import com.dev.julian09.financeapp.ui.feature.auth.LoginContent
import com.dev.julian09.financeapp.ui.feature.auth.LoginUiState
import com.dev.julian09.financeapp.ui.feature.auth.RegisterContent
import com.dev.julian09.financeapp.ui.feature.auth.RegisterUiState
import com.dev.julian09.financeapp.ui.feature.history.HistoryContent
import com.dev.julian09.financeapp.ui.feature.history.HistoryDay
import com.dev.julian09.financeapp.ui.feature.history.HistoryRow
import com.dev.julian09.financeapp.ui.feature.home.HomeScreen
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
import java.time.LocalDate
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
    private fun Home() {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) { HomeScreen(onOpenSettings = {}, avatarInitial = 'J', syncStatus = SyncStatus.Pending(2)) }
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
        AccountsContent(
            listOf(
                Account(UUID(0, 1), "Efectivo", AccountType.CASH, Money.ofPesos(150_000)),
                Account(UUID(0, 2), "Nu", AccountType.SAVINGS, Money.ofPesos(2_600_000), institution = "Nu Colombia"),
            ),
            onOpenSettings = {},
            avatarInitial = 'J',
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
}
