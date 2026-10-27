package com.dev.julian09.financeapp.ui.feature.transactions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.NorthEast
import androidx.compose.material.icons.outlined.NorthWest
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SouthEast
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.julian09.financeapp.domain.model.AccountBalance
import com.dev.julian09.financeapp.domain.model.CardOverview
import com.dev.julian09.financeapp.domain.model.Category
import com.dev.julian09.financeapp.domain.model.CategoryKind
import com.dev.julian09.financeapp.domain.model.NoteSuggestion
import com.dev.julian09.financeapp.domain.model.TransactionType
import com.dev.julian09.financeapp.domain.usecase.TransactionField
import com.dev.julian09.financeapp.ui.components.AppBottomSheet
import com.dev.julian09.financeapp.ui.components.AppButton
import com.dev.julian09.financeapp.ui.components.AppCard
import com.dev.julian09.financeapp.ui.components.AppChip
import com.dev.julian09.financeapp.ui.components.AppHeader
import com.dev.julian09.financeapp.ui.components.AppTextField
import com.dev.julian09.financeapp.ui.components.BubbleRow
import com.dev.julian09.financeapp.ui.components.CategoryGridSheet
import com.dev.julian09.financeapp.ui.components.ConfirmDialog
import com.dev.julian09.financeapp.ui.components.IconBubble
import com.dev.julian09.financeapp.ui.components.PesosTransformation
import com.dev.julian09.financeapp.ui.components.SegmentedSelector
import com.dev.julian09.financeapp.ui.components.accountIcon
import com.dev.julian09.financeapp.ui.components.accountTypeLabel
import com.dev.julian09.financeapp.ui.components.formatMoney
import com.dev.julian09.financeapp.ui.components.iconOf
import com.dev.julian09.financeapp.ui.components.message
import com.dev.julian09.financeapp.ui.feature.cards.parsePercent
import com.dev.julian09.financeapp.ui.feature.cards.percentText
import com.dev.julian09.financeapp.ui.feature.cards.rateLabel
import com.dev.julian09.financeapp.ui.feature.cards.rateTypeLabel
import com.dev.julian09.financeapp.ui.feature.history.tagColor
import com.dev.julian09.financeapp.ui.theme.CofferShapes
import com.dev.julian09.financeapp.ui.theme.CofferTheme
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

@Composable
fun TransactionFormScreen(
    onBack: () -> Unit,
    viewModel: TransactionFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val choices by viewModel.choices.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) onBack() }
    TransactionFormContent(
        state = state,
        choices = choices,
        actions =
            TransactionFormActions(
                onBack = onBack,
                onType = viewModel::setType,
                onAmount = viewModel::setAmount,
                onCategory = viewModel::setCategory,
                onAccount = viewModel::setAccount,
                onDestination = viewModel::setDestination,
                onSwap = viewModel::swapAccounts,
                onOccurredAt = viewModel::setOccurredAt,
                onTag = viewModel::toggleTag,
                onNewTag = viewModel::newTag,
                onClearTagError = viewModel::clearTagError,
                onNote = viewModel::setNote,
                onConcept = viewModel::setConcept,
                onSuggestion = viewModel::pickSuggestion,
                onSave = viewModel::save,
                onDelete = viewModel::delete,
                onDiscard = viewModel::discard,
                onInstallments = viewModel::setInstallments,
                onRate = viewModel::setRate,
            ),
    )
}

class TransactionFormActions(
    val onBack: () -> Unit = {},
    val onType: (TransactionType) -> Unit = {},
    val onAmount: (String) -> Unit = {},
    val onCategory: (UUID) -> Unit = {},
    val onAccount: (UUID) -> Unit = {},
    val onDestination: (UUID) -> Unit = {},
    val onSwap: () -> Unit = {},
    val onOccurredAt: (Instant) -> Unit = {},
    val onTag: (UUID) -> Unit = {},
    val onNewTag: (String) -> Unit = {},
    val onClearTagError: () -> Unit = {},
    val onNote: (String) -> Unit = {},
    val onConcept: (String) -> Unit = {},
    val onSuggestion: (NoteSuggestion) -> Unit = {},
    val onSave: () -> Unit = {},
    val onDelete: () -> Unit = {},
    val onDiscard: () -> Unit = {},
    val onInstallments: (Int) -> Unit = {},
    val onRate: (String) -> Unit = {},
)

private val TYPES = listOf(TransactionType.EXPENSE, TransactionType.INCOME, TransactionType.TRANSFER)
private val ES_CO: Locale = Locale.forLanguageTag("es-CO")
private val DATE = DateTimeFormatter.ofPattern("d 'de' MMM 'de' yyyy", ES_CO)
private val TIME = DateTimeFormatter.ofPattern("h:mm a", ES_CO)

/** "Hoy, 7 de oct de 2026 · 1:10 p. m." */
fun dateTimeLabel(
    instant: Instant,
    today: LocalDate = LocalDate.now(),
    zone: ZoneId = ZoneId.systemDefault(),
): String {
    val at = instant.atZone(zone)
    val prefix =
        when (at.toLocalDate()) {
            today -> "Hoy, "
            today.minusDays(1) -> "Ayer, "
            else -> ""
        }
    return prefix + DATE.format(at).replace(".", "") + " · " + TIME.format(at)
}

/** Nueva transacción (S5-UI01): ≤ 4 interactions for an expense, the transfer tab one tap away (RNF-14, REQ-TRX-04). */
@Composable
fun TransactionFormContent(
    state: TransactionFormState,
    choices: TransactionChoices,
    actions: TransactionFormActions,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now(),
    zone: ZoneId = ZoneId.systemDefault(),
) {
    val transfer = state.type == TransactionType.TRANSFER
    // S12-UI09: the bottom button rides on the keyboard
    Column(modifier.fillMaxSize().imePadding()) {
        var confirming by rememberSaveable { mutableStateOf(false) }
        AppHeader(
            title =
                when {
                    state.draftId != null -> "Revisar transacción"
                    state.editingId == null -> "Nueva transacción"
                    else -> "Editar transacción"
                },
            onBack = actions.onBack,
            actions = {
                if (state.editingId != null) {
                    // Figma `70:250`: a red trash, a 44 dp target
                    IconButton(onClick = { confirming = true }) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "Eliminar transacción", tint = CofferTheme.colors.expense)
                    }
                }
            },
        )
        if (confirming) {
            ConfirmDialog(
                title = "¿Eliminar esta transacción?",
                message = "Se quita del historial y de los saldos.",
                confirmText = "Eliminar",
                onConfirm = {
                    confirming = false
                    actions.onDelete()
                },
                onDismiss = { confirming = false },
                destructive = true,
            )
        }
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            state.draftBank?.let { DraftNote(it) }
            SegmentedSelector(
                listOf("Gasto", "Ingreso", "Transferencia"),
                selectedIndex = TYPES.indexOf(state.type),
                onSelect = { actions.onType(TYPES[it]) },
                modifier = Modifier.fillMaxWidth(),
                // S15-UI03: the same icons as Home's quick actions
                icons = listOf(Icons.Outlined.ArrowDownward, Icons.Outlined.ArrowUpward, Icons.Outlined.SwapHoriz),
            )
            AmountCard(state, actions.onAmount, focusOnOpen = state.editingId == null)
            // Figma «Título» (S12-UI01): the name, with the titles used before (M5)
            TitleCard(state, choices, actions)
            if (transfer) {
                // Figma `1:1807`: full-width accounts, a direction icon by each label and the swap between them
                Box {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TransferAccount(
                            "Desde",
                            Icons.Outlined.NorthEast,
                            choices.accounts,
                            state.accountId,
                            state.errors[TransactionField.ACCOUNT],
                            actions.onAccount,
                        )
                        TransferAccount(
                            "Hacia",
                            Icons.Outlined.SouthEast,
                            choices.accounts,
                            state.destinationId,
                            state.errors[TransactionField.DESTINATION],
                            actions.onDestination,
                        )
                    }
                    IconButton(
                        onClick = actions.onSwap,
                        modifier =
                            Modifier
                                .align(Alignment.Center)
                                .size(40.dp)
                                .background(CofferTheme.colors.surface, CircleShape)
                                .border(1.dp, CofferTheme.colors.border, CircleShape),
                    ) {
                        Icon(
                            Icons.Outlined.SwapVert,
                            contentDescription = "Intercambiar origen y destino",
                            tint = CofferTheme.colors.textPrimary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                DateTimeCard(state.occurredAt, actions.onOccurredAt, today, zone)
                AppCard {
                    AppTextField(
                        value = state.concept,
                        onValueChange = actions.onConcept,
                        label = "Nota o concepto · opcional",
                        placeholder = "Para el efectivo de la semana",
                    )
                }
            } else {
                CategoryCard(state, choices, actions.onCategory)
                DateTimeCard(state.occurredAt, actions.onOccurredAt, today, zone)
                // S12-UI03: the account without its balance, as in Figma
                AccountPicker(
                    "Cuenta de origen",
                    choices.accounts,
                    state.accountId,
                    state.errors[TransactionField.ACCOUNT],
                    actions.onAccount,
                    subtitle = null,
                )
                choices.cardOf(state)?.let { CreditConditions(state, it, actions) }
            }
            if (!transfer) AppCard { TagPicker(state, choices, actions) }
        }
        // M4 (#88): the save button stays visible while the form scrolls
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AppButton(
                when {
                    state.draftId != null -> "Aplicar"
                    state.editingId != null -> "Guardar cambios"
                    transfer -> "Confirmar transferencia"
                    else -> "Guardar transacción"
                },
                onClick = actions.onSave,
                icon = Icons.Outlined.Check,
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.draftId != null) {
                // Figma `162:186`: red text without a background
                TextButton(onClick = actions.onDiscard, modifier = Modifier.fillMaxWidth()) {
                    Text("Descartar", style = MaterialTheme.typography.labelLarge, color = CofferTheme.colors.expense)
                }
            }
        }
    }
}

/** Revisar transacción (changes-ui-2.0 §1): where it came from and that it counts only once applied. */
@Composable
private fun DraftNote(bank: String) {
    val colors = CofferTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            // Figma's #F2EEEA: the warm grey of the borders, a step off the white card
            .background(colors.border, CofferShapes.card)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Outlined.Email, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
        Text(
            "Llegó en un correo de $bank. Revisa los datos y aplícala para que cuente en tu saldo.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )
    }
}

@Composable
private fun AmountCard(
    state: TransactionFormState,
    onAmount: (String) -> Unit,
    focusOnOpen: Boolean,
) {
    val colors = CofferTheme.colors
    val error = state.errors[TransactionField.AMOUNT]
    // S15-UI02: a new movement opens on the amount with the keyboard up; editing one does not
    val focus = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(focusOnOpen) { if (focusOnOpen) focus.requestFocus() }
    AppCard(Modifier.fillMaxWidth()) {
        Text(
            if (state.type == TransactionType.TRANSFER) "Monto a transferir" else "Monto de la transacción",
            Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("$", style = MaterialTheme.typography.headlineMedium, color = colors.textSecondary)
            BasicTextField(
                value = state.amountDigits,
                visualTransformation = PesosTransformation(symbol = false),
                onValueChange = onAmount,
                modifier = Modifier.weight(1f).focusRequester(focus).onFocusChanged { focused = it.isFocused },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle =
                    MaterialTheme.typography.displaySmall.merge(
                        TextStyle(color = colors.textPrimary, textAlign = TextAlign.Center),
                    ),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.Center) {
                        // BUG-02: the «0» is a hint for the resting field; with the focus the cursor stands alone
                        if (state.amountDigits.isEmpty() && !focused) {
                            Text("0", style = MaterialTheme.typography.displaySmall, color = colors.textSecondary)
                        }
                        inner()
                    }
                },
            )
        }
        error?.let { Text(it.message(), style = MaterialTheme.typography.bodySmall, color = colors.expense) }
    }
}

/** Título (S12-UI01) with the titles used before (M5): each shows «categoría · cuenta», as in Figma `114:268`. */
@Composable
private fun TitleCard(
    state: TransactionFormState,
    choices: TransactionChoices,
    actions: TransactionFormActions,
) {
    val colors = CofferTheme.colors
    // BUG-03: when a suggestion appears the form scrolls just enough to show it above the keyboard
    val bringIntoView = remember { BringIntoViewRequester() }
    LaunchedEffect(state.suggestions.firstOrNull()?.note) {
        if (state.suggestions.isNotEmpty()) bringIntoView.bringIntoView()
    }
    AppCard(Modifier.bringIntoViewRequester(bringIntoView)) {
        AppTextField(
            value = state.note,
            onValueChange = actions.onNote,
            label = "Título",
            placeholder = if (state.type == TransactionType.TRANSFER) "Retiro cajero" else "Ej. Almuerzo, taxi, mercado",
            leadingIcon = Icons.Outlined.Edit,
            error = state.errors[TransactionField.NOTE]?.message(),
        )
        state.suggestions.forEach { s ->
            val category = s.categoryId?.let { id -> choices.categories.firstOrNull { it.id == id } }
            val account = s.accountId?.let { id -> choices.accounts.firstOrNull { it.account.id == id } }
            val detail = listOfNotNull(category?.name, account?.account?.name).joinToString(" · ")
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClickLabel = "Usar") { actions.onSuggestion(s) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Outlined.History, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(20.dp))
                Column(Modifier.weight(1f)) {
                    // What was typed in bold, as in Figma «**Alm**uerzo en restaurante»
                    Text(boldMatch(s.note, state.note.trim()), style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                    if (detail.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            category?.let {
                                Icon(
                                    iconOf(it, choices.categories),
                                    contentDescription = null,
                                    tint = colors.iconOnContainer,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                            Text(detail, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                        }
                    }
                }
                Icon(Icons.Outlined.NorthWest, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

internal fun boldMatch(
    text: String,
    typed: String,
) = buildAnnotatedString {
    val at = text.indexOf(typed, ignoreCase = true)
    if (typed.isEmpty() || at < 0) {
        append(text)
    } else {
        append(text.substring(0, at))
        withStyle(SpanStyle(fontWeight = FontWeight.Medium)) { append(text.substring(at, at + typed.length)) }
        append(text.substring(at + typed.length))
    }
}

internal fun categoryLabel(
    category: Category,
    all: List<Category>,
) = category.name // Flat outside Categorías and Reportes (Figma, S13): never «Comida > Restaurantes»

/** Categories of one kind, the most used first (S12-UI02); a subcategory without its own icon takes its parent's. */
internal fun mostUsed(
    categories: List<Category>,
    kind: CategoryKind,
    usage: Map<UUID, Int>,
): List<Category> =
    categories
        .filter { it.kind == kind }
        .sortedWith(compareByDescending<Category> { usage[it.id] ?: 0 }.thenBy { it.parentId != null }.thenBy { it.sortOrder })

private const val QUICK_CATEGORIES = 4

/** Categoría (Figma `1:844`): the four most used and «Más», which opens Hoja · Elegir categoría. */
@Composable
private fun CategoryCard(
    state: TransactionFormState,
    choices: TransactionChoices,
    onCategory: (UUID) -> Unit,
) {
    val colors = CofferTheme.colors
    var picking by rememberSaveable { mutableStateOf(false) }
    val kind = if (state.type == TransactionType.EXPENSE) CategoryKind.EXPENSE else CategoryKind.INCOME
    val ranked = mostUsed(choices.categories, kind, choices.usage)
    val selected = ranked.firstOrNull { it.id == state.categoryId }
    // The chosen one stays in sight even when it is not among the most used
    val quick = (listOfNotNull(selected) + ranked).distinctBy { it.id }.take(QUICK_CATEGORIES)
    AppCard {
        Text("Categoría", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        BubbleRow(quick.size, onMore = { picking = true }) { i, cell ->
            val c = quick[i]
            IconBubble(c.name, iconOf(c, choices.categories), selected = c.id == state.categoryId, modifier = cell) { onCategory(c.id) }
        }
        state.errors[TransactionField.CATEGORY]?.let {
            Text(it.message(), style = MaterialTheme.typography.bodySmall, color = colors.expense)
        }
    }
    if (picking) {
        CategoryGridSheet(
            ranked,
            choices.categories,
            selected = setOfNotNull(state.categoryId),
            onPick = {
                onCategory(it)
                picking = false
            },
            onDismiss = { picking = false },
        )
    }
}

/** One side of a transfer: the chosen account at full width; a tap opens the list to change it. */
@Composable
private fun TransferAccount(
    label: String,
    direction: androidx.compose.ui.graphics.vector.ImageVector,
    accounts: List<AccountBalance>,
    selectedId: UUID?,
    error: com.dev.julian09.financeapp.domain.model.LedgerError?,
    onSelect: (UUID) -> Unit,
) {
    val colors = CofferTheme.colors
    var choosing by rememberSaveable { mutableStateOf(false) }
    val chosen = accounts.firstOrNull { it.account.id == selectedId }
    AppCard(onClick = { choosing = true }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(direction, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(14.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CircleIcon(chosen?.let { accountIcon(it.account.icon, it.account.type) } ?: Icons.Outlined.AccountBalanceWallet)
            Column(Modifier.weight(1f)) {
                Text(
                    chosen?.account?.name ?: "Elige una cuenta",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (chosen == null) colors.textSecondary else colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                chosen?.let {
                    Text(
                        listOfNotNull(accountTypeLabel(it.account.type), it.account.institution).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                    )
                }
            }
            Icon(Icons.Outlined.UnfoldMore, contentDescription = "Cambiar cuenta", tint = colors.textSecondary)
        }
        error?.let { Text(it.message(), style = MaterialTheme.typography.bodySmall, color = colors.expense) }
    }
    if (choosing) {
        AppBottomSheet(onDismiss = { choosing = false }, title = label) {
            accounts.forEach { a ->
                val on = a.account.id == selectedId
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(CofferShapes.card)
                        .background(if (on) colors.primaryContainer else colors.surface)
                        .selectable(on, role = Role.RadioButton) {
                            onSelect(a.account.id)
                            choosing = false
                        }.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircleIcon(accountIcon(a.account.icon, a.account.type))
                    Text(a.account.name, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
                }
            }
        }
    }
}

@Composable
internal fun CircleIcon(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    val colors = CofferTheme.colors
    Box(Modifier.size(40.dp).background(colors.iconContainer, CircleShape), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = colors.iconOnContainer, modifier = Modifier.size(20.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateTimeCard(
    occurredAt: Instant,
    onChange: (Instant) -> Unit,
    today: LocalDate,
    zone: ZoneId,
) {
    val colors = CofferTheme.colors
    var step by remember { mutableStateOf(0) } // 0 closed, 1 date, 2 time
    var date by remember { mutableStateOf(occurredAt.atZone(zone).toLocalDate()) }
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CircleIcon(Icons.Outlined.Schedule)
            Column(Modifier.weight(1f)) {
                Text("Fecha y hora", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                Text(dateTimeLabel(occurredAt, today, zone), style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
            }
            TextButton(onClick = { step = 1 }) { Text("Modificar", color = colors.primary) }
        }
    }
    if (step == 1) {
        // The date picker works in UTC midnights
        val picker = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { step = 0 },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let { date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    step = 2
                }) { Text("Siguiente") }
            },
            dismissButton = { TextButton(onClick = { step = 0 }) { Text("Cancelar") } },
        ) { DatePicker(state = picker) }
    }
    if (step == 2) {
        val time = occurredAt.atZone(zone).toLocalTime()
        val picker = rememberTimePickerState(initialHour = time.hour, initialMinute = time.minute)
        AlertDialog(
            onDismissRequest = { step = 0 },
            confirmButton = {
                TextButton(onClick = {
                    onChange(LocalDateTime.of(date, LocalTime.of(picker.hour, picker.minute)).atZone(zone).toInstant())
                    step = 0
                }) { Text("Listo") }
            },
            dismissButton = { TextButton(onClick = { step = 0 }) { Text("Cancelar") } },
            text = { TimePicker(state = picker) },
        )
    }
}

@Composable
internal fun AccountPicker(
    title: String,
    accounts: List<AccountBalance>,
    selectedId: UUID?,
    error: com.dev.julian09.financeapp.domain.model.LedgerError?,
    onSelect: (UUID) -> Unit,
    subtitle: ((AccountBalance) -> String)? = { formatMoney(it.balance) },
) {
    val colors = CofferTheme.colors
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            // Figma `1:844`: the hint that the accounts slide sideways
            if (accounts.size > 1) Text("Deslizar", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        }
        if (accounts.isEmpty()) {
            Text(
                "Crea una cuenta en Cuentas para registrar movimientos.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(end = 8.dp)) {
            items(accounts, key = { it.account.id }) { item ->
                val selected = item.account.id == selectedId
                Surface(
                    modifier =
                        Modifier
                            .width(
                                180.dp,
                            ).selectable(selected, role = Role.RadioButton, onClick = { onSelect(item.account.id) }),
                    shape = CofferShapes.card,
                    color = if (selected) colors.primaryContainer else colors.surface,
                    border = BorderStroke(1.dp, if (selected) colors.onPrimaryContainer else colors.border),
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CircleIcon(accountIcon(item.account.icon, item.account.type))
                        Column(Modifier.weight(1f, fill = false)) {
                            Text(item.account.name, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary, maxLines = 1)
                            subtitle?.let { Text(it(item), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary) }
                        }
                        // Figma `1:844`: the chosen account carries a dot
                        if (selected) Box(Modifier.size(10.dp).background(colors.onPrimaryContainer, CircleShape))
                    }
                }
            }
        }
        error?.let { Text(it.message(), style = MaterialTheme.typography.bodySmall, color = colors.expense) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagPicker(
    state: TransactionFormState,
    choices: TransactionChoices,
    actions: TransactionFormActions,
) {
    val colors = CofferTheme.colors
    var creating by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    Text("Etiqueta", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        choices.tags.forEach { tag ->
            AppChip(tag.name, selected = tag.id in state.tagIds, onClick = { actions.onTag(tag.id) }, dot = tagColor(tag.shownColor))
        }
        Surface(
            modifier = Modifier.clip(CofferShapes.pill).selectable(false, role = Role.Button, onClick = { creating = true }),
            shape = CofferShapes.pill,
            color = colors.surface,
            border = BorderStroke(1.dp, colors.border),
        ) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Add, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(16.dp))
                Text("Nueva etiqueta", style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
            }
        }
    }
    // A created tag gets selected and closes the dialog; an error keeps it open
    LaunchedEffect(state.createdTags) {
        creating = false
        name = ""
    }
    if (creating) {
        AlertDialog(
            onDismissRequest = {
                creating = false
                actions.onClearTagError()
            },
            title = { Text("Nueva etiqueta") },
            text = {
                AppTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        actions.onClearTagError()
                    },
                    label = "Nombre",
                    placeholder = "Viaje Cartagena",
                    error = state.tagError?.message(),
                )
            },
            confirmButton = { TextButton(onClick = { actions.onNewTag(name) }) { Text("Crear") } },
            dismissButton = {
                TextButton(onClick = {
                    creating = false
                    actions.onClearTagError()
                }) { Text("Cancelar") }
            },
        )
    }
}

private val QUICK_INSTALLMENTS = listOf(1, 6, 12, 24)

/** Condiciones del crédito (S7-UI02): installments, and the card's rate, editable, from 2 on (REQ-TC-05, 06, RNF-15). */
@Composable
private fun CreditConditions(
    state: TransactionFormState,
    card: CardOverview,
    actions: TransactionFormActions,
) {
    val colors = CofferTheme.colors
    val count = state.installments
    val rateType = state.rateType ?: card.card.rateType
    val rateText = state.rateText ?: percentText(card.card.rate)
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Condiciones del crédito", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
            if (count > 1) Text("Diferido", style = MaterialTheme.typography.labelMedium, color = colors.warning)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Número de cuotas", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            IconButton(onClick = { actions.onInstallments(count - 1) }, enabled = count > 1) {
                Icon(Icons.Outlined.Remove, contentDescription = "Menos cuotas", tint = colors.textPrimary)
            }
            Text(
                if (count == 1) "1 cuota" else "$count cuotas",
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
            )
            IconButton(onClick = { actions.onInstallments(count + 1) }, enabled = count < MAX_INSTALLMENTS) {
                Icon(Icons.Outlined.Add, contentDescription = "Más cuotas", tint = colors.textPrimary)
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(QUICK_INSTALLMENTS) { n ->
                AppChip(if (n == 1) "1 cuota" else "$n cuotas", selected = count == n, onClick = { actions.onInstallments(n) })
            }
        }
        state.errors[TransactionField.INSTALLMENTS]?.let {
            Text(it.message(), style = MaterialTheme.typography.bodySmall, color = colors.expense)
        }
        if (count > 1) {
            AppTextField(
                value = rateText,
                onValueChange = actions.onRate,
                label = "Tasa de interés pactada (% ${rateTypeLabel(rateType)})",
                keyboardType = KeyboardType.Decimal,
                error = state.errors[TransactionField.RATE]?.message(),
                help =
                    parsePercent(rateText)?.let { "Editable para convenios con tasa 0 o preferencial · " + rateLabel(it, rateType) }
                        ?: "Editable para convenios con tasa 0 o preferencial",
            )
        }
    }
}
