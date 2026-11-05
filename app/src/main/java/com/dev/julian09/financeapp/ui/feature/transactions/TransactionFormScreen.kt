package com.dev.julian09.financeapp.ui.feature.transactions

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.NorthEast
import androidx.compose.material.icons.outlined.NorthWest
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SouthEast
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.julian09.financeapp.domain.model.AccountBalance
import com.dev.julian09.financeapp.domain.model.CardOverview
import com.dev.julian09.financeapp.domain.model.Category
import com.dev.julian09.financeapp.domain.model.CategoryKind
import com.dev.julian09.financeapp.domain.model.Money
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
import com.dev.julian09.financeapp.ui.components.accountIcon
import com.dev.julian09.financeapp.ui.components.accountTypeShortLabel
import com.dev.julian09.financeapp.ui.components.formatMoney
import com.dev.julian09.financeapp.ui.components.iconOf
import com.dev.julian09.financeapp.ui.components.message
import com.dev.julian09.financeapp.ui.feature.cards.parsePercent
import com.dev.julian09.financeapp.ui.feature.cards.percentText
import com.dev.julian09.financeapp.ui.feature.cards.rateLabel
import com.dev.julian09.financeapp.ui.feature.cards.rateTypeLabel
import com.dev.julian09.financeapp.ui.feature.history.UndoNotice
import com.dev.julian09.financeapp.ui.feature.history.tagColor
import com.dev.julian09.financeapp.ui.theme.CofferShapes
import com.dev.julian09.financeapp.ui.theme.CofferTheme
import kotlinx.coroutines.launch
import java.math.BigDecimal
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
    LaunchedEffect(state.saved) {
        if (state.undone) UndoNotice.show = true
        if (state.saved) onBack()
    }
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
                onUndo = viewModel::undo,
                onSplit = viewModel::split,
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
    val onUndo: () -> Unit = {},
    val onSplit: (List<com.dev.julian09.financeapp.domain.usecase.SplitPart>) -> Unit = {},
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

/**
 * Nueva transacción por pasos (changes-ui-2.3 §1, Figma `1:844`) and Nueva transferencia (§2, `1:1807`): the amount loose on the
 * background, the fields in two blocks, and the button fixed above the keyboard. With the keyboard up only the field in use and
 * the next one show; «Siguiente» moves the focus on and scrolls its block under the header.
 */
@OptIn(ExperimentalLayoutApi::class)
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
        var splitting by rememberSaveable { mutableStateOf(false) }
        // 2.2: the main button says «Siguiente» until only saving is left
        var pickingCategory by rememberSaveable { mutableStateOf(false) }
        var titleVisited by rememberSaveable { mutableStateOf(false) }
        val focusManager = LocalFocusManager.current
        val keyboard = LocalSoftwareKeyboardController.current
        val scope = rememberCoroutineScope()
        val scroll = rememberScrollState()
        val titleFocus = remember { FocusRequester() }
        // Where the title's block starts inside the scrolling column, to bring it under the header
        var titleBlockY by remember { mutableIntStateOf(0) }
        val ready = readyToSave(state)
        val toTitle: () -> Unit = {
            titleVisited = true
            titleFocus.requestFocus()
            scope.launch { scroll.animateScrollTo(titleBlockY) }
        }
        // A category, an account or the date are no text: the keyboard goes and the whole form fits
        val closeKeyboard: () -> Unit = {
            keyboard?.hide()
            focusManager.clearFocus()
        }
        AppHeader(
            title =
                when {
                    state.draftId != null -> "Revisar transacción"
                    state.editingId == null -> "Nueva transacción"
                    else -> "Editar transacción"
                },
            onBack = actions.onBack,
            actions = {
                if (state.editingId != null && state.undoable) {
                    // 2.4 §4: only the arrow, 44 dp, no text; it sends the movement back to «Por revisar»
                    IconButton(onClick = actions.onUndo) {
                        Icon(
                            Icons.AutoMirrored.Outlined.Undo,
                            contentDescription = "Deshacer",
                            tint = CofferTheme.colors.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                if (state.editingId != null) {
                    // Figma `70:250`: a red trash, a 44 dp target
                    IconButton(onClick = { confirming = true }) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "Eliminar transacción", tint = CofferTheme.colors.expense)
                    }
                }
            },
        )
        if (state.learnedFrom > 0) {
            // 2.4-03: where the title and the category came from; tags never fill in by themselves
            Text(
                "Según tus ${if (state.learnedFrom == 1) "último movimiento" else "${state.learnedFrom} últimos movimientos"} " +
                    "de este comercio. " +
                    "Las etiquetas no se aplican.",
                Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = CofferTheme.colors.textSecondary,
            )
        }
        if (confirming) {
            ConfirmDialog(
                title = "¿Eliminar esta transacción?",
                // MOV-APP-004 (Figma `20:487`): which movement, as in its detail
                message =
                    "${state.note.ifBlank { "Transacción" }} · " +
                        formatMoney(Money.of(state.amountDigits.toBigDecimalOrNull() ?: BigDecimal.ZERO), kindOf(state.type)) + ".",
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
                .verticalScroll(scroll)
                // Figma: the tabs sit 8 dp under the header; 16 dp between every other piece
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            state.draftBank?.let { DraftNote(it) }
            TypeTabs(state.type, actions.onType)
            AmountBlock(state, actions.onAmount, focusOnOpen = state.editingId == null && state.draftId == null, onNext = toTitle)
            // 2.4 §6.1: only on an expense that is being edited or reviewed, never an installment purchase
            if (state.splittable && state.type == TransactionType.EXPENSE && (state.amountDigits.toLongOrNull() ?: 0) > 0) {
                TextButton(onClick = { splitting = true }, Modifier.align(Alignment.CenterHorizontally).height(44.dp)) {
                    Icon(Icons.AutoMirrored.Outlined.CallSplit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Dividir", style = MaterialTheme.typography.labelLarge, color = CofferTheme.colors.primary)
                }
            }
            if (splitting) {
                SplitSheet(
                    total = Money.ofPesos(state.amountDigits.toLong()),
                    title = state.note.ifBlank { "Transacción" },
                    payerName =
                        choices.accounts
                            .firstOrNull { it.account.id == state.accountId }
                            ?.account
                            ?.name ?: "Cuenta",
                    accounts = choices.accounts,
                    onConfirm = {
                        splitting = false
                        actions.onSplit(it)
                    },
                    onDismiss = { splitting = false },
                )
            }
            val titleBlock = Modifier.onPlaced { titleBlockY = it.positionInParent().y.toInt() }
            if (transfer) {
                FormBlock(titleBlock) { TitleRow(state, choices, actions, titleFocus, closeKeyboard) }
                TransferAccounts(state, choices, actions, closeKeyboard)
                FormBlock {
                    DateTimeRow(state.occurredAt, actions.onOccurredAt, today, zone, closeKeyboard)
                    BlockDivider()
                    ConceptRow(state.concept, actions.onConcept)
                }
            } else {
                FormBlock(titleBlock) {
                    TitleRow(state, choices, actions, titleFocus, closeKeyboard)
                    BlockDivider()
                    CategorySection(state, choices, {
                        closeKeyboard()
                        actions.onCategory(it)
                    }, pickingCategory) {
                        if (it) closeKeyboard()
                        pickingCategory = it
                    }
                }
                FormBlock {
                    DateTimeRow(state.occurredAt, actions.onOccurredAt, today, zone, closeKeyboard)
                    BlockDivider()
                    AccountChips(choices.accounts, state.accountId, state.errors[TransactionField.ACCOUNT]) {
                        closeKeyboard()
                        actions.onAccount(it)
                    }
                    BlockDivider()
                    Column(
                        Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TagPicker(state, choices, actions)
                    }
                }
                choices.cardOf(state)?.let { CreditConditions(state, it, actions) }
            }
        }
        // M4 (#88): the main button stays visible; 16/16/24 at rest, 10 above and below on the keyboard (§1.7)
        val keyboardUp = WindowInsets.isImeVisible
        Column(
            Modifier.fillMaxWidth().padding(
                start = 16.dp,
                end = 16.dp,
                top = if (keyboardUp) 10.dp else 16.dp,
                bottom = if (keyboardUp) 10.dp else 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val label =
                when {
                    state.draftId != null -> "Aplicar"
                    state.editingId != null -> "Guardar cambios"
                    !ready -> "Siguiente"
                    transfer -> "Confirmar transferencia"
                    else -> "Guardar transacción"
                }
            Crossfade(label, animationSpec = tween(150), label = "mainButton") { text ->
                AppButton(
                    text,
                    onClick = {
                        when {
                            ready -> actions.onSave()
                            !titleVisited -> toTitle()
                            // Nothing else to type: the category (or, in a transfer, the validation) is what is left
                            !transfer && state.categoryId == null -> {
                                closeKeyboard()
                                pickingCategory = true
                            }
                            else -> actions.onSave()
                        }
                    },
                    icon = if (text == "Siguiente") null else Icons.Outlined.Check,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (state.draftId != null) {
                // Figma `162:186`: red text without a background
                TextButton(onClick = actions.onDiscard, modifier = Modifier.fillMaxWidth()) {
                    Text("Descartar", style = MaterialTheme.typography.labelLarge, color = CofferTheme.colors.expense)
                }
            }
        }
    }
}

/** Gasto / Ingreso / Transferencia (§1.2): a 40 dp pill strip in `#F2EEEA`, the active tab in the selection color. */
@Composable
private fun TypeTabs(
    type: TransactionType,
    onType: (TransactionType) -> Unit,
) {
    val colors = CofferTheme.colors
    val icons = listOf(Icons.Outlined.NorthEast, Icons.Outlined.ArrowDownward, Icons.Outlined.SwapHoriz)
    Row(
        Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(CofferShapes.pill)
            .background(colors.quickStrip)
            .padding(3.dp)
            .selectableGroup(),
    ) {
        listOf("Gasto", "Ingreso", "Transferencia").forEachIndexed { i, label ->
            val selected = TYPES[i] == type
            Row(
                Modifier
                    // 2.4-06: the longest label gets more room, so «Transferencia» is centred in its tab and never wraps
                    .weight(if (i == 2) 1.25f else 0.9f)
                    .fillMaxHeight()
                    .clip(CofferShapes.pill)
                    .background(if (selected) colors.primaryContainer else Color.Transparent)
                    .selectable(selected, role = Role.RadioButton) { onType(TYPES[i]) },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val tint = if (selected) colors.onPrimaryContainer else colors.textSecondary
                Icon(icons[i], contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
                Text(
                    label,
                    Modifier.padding(start = 4.dp),
                    style =
                        MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                        ),
                    color = tint,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}

/** Monto (§1.3, §2.2): no card; «$» 32 and the figure 48 Medium, centered; «0» as the hint. */
@Composable
private fun AmountBlock(
    state: TransactionFormState,
    onAmount: (String) -> Unit,
    focusOnOpen: Boolean,
    onNext: () -> Unit,
) {
    val colors = CofferTheme.colors
    val error = state.errors[TransactionField.AMOUNT]
    val transfer = state.type == TransactionType.TRANSFER
    // S15-UI02: a new movement opens on the amount with the keyboard up; editing or reviewing one does not
    val focus = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(focusOnOpen) { if (focusOnOpen) focus.requestFocus() }
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        FieldLabel(if (transfer) "Monto a transferir" else "Monto", focused)
        BasicTextField(
            value = state.amountDigits,
            visualTransformation = PesosTransformation(symbol = false),
            onValueChange = onAmount,
            modifier = Modifier.fillMaxWidth().focusRequester(focus).onFocusChanged { focused = it.isFocused },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { onNext() }),
            textStyle = AMOUNT_STYLE.merge(TextStyle(color = colors.textPrimary)),
            cursorBrush = SolidColor(colors.primary),
            decorationBox = { inner ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "$",
                        Modifier.padding(end = 6.dp),
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 32.sp),
                        color = colors.textSecondary,
                    )
                    Box(Modifier.width(IntrinsicSize.Min), contentAlignment = Alignment.CenterStart) {
                        // BUG-02: the «0» is a hint for the resting field; with the focus the cursor stands alone
                        if (state.amountDigits.isEmpty() && !focused) Text("0", style = AMOUNT_STYLE, color = colors.textSecondary)
                        inner()
                    }
                }
            },
        )
        if (transfer) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Outlined.TouchApp, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(12.dp))
                Text(
                    "Toca para editar monto",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = colors.textSecondary,
                )
            }
        }
        error?.let { Text(it.message(), style = MaterialTheme.typography.bodySmall, color = colors.expense) }
    }
}

private val AMOUNT_STYLE = TextStyle(fontSize = 48.sp, lineHeight = 56.sp, fontWeight = FontWeight.Medium, letterSpacing = (-1.2).sp)

/** A field's label: walnut and Medium while it is the active one (§1, «Campo activo»). */
@Composable
private fun FieldLabel(
    text: String,
    active: Boolean = false,
    icon: ImageVector? = null,
) {
    val colors = CofferTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        icon?.let { Icon(it, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(12.dp)) }
        Text(
            text,
            style =
                MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                ),
            color = if (active) colors.primary else colors.textSecondary,
        )
    }
}

/** One block of the form: a white card, thin border, 16 radius; its rows are split by thin lines, never by cards. */
@Composable
private fun FormBlock(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = CofferTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .clip(CofferShapes.highlight)
            .background(colors.surface)
            .border(1.dp, colors.border, CofferShapes.highlight),
    ) { content() }
}

@Composable
private fun BlockDivider() = HorizontalDivider(thickness = 1.dp, color = CofferTheme.colors.border)

/** Título (§1.4) with the titles used before (M5): each shows «categoría · cuenta», as in Figma `114:268`. */
@Composable
private fun TitleRow(
    state: TransactionFormState,
    choices: TransactionChoices,
    actions: TransactionFormActions,
    focus: FocusRequester,
    onDone: () -> Unit,
) {
    val colors = CofferTheme.colors
    var focused by remember { mutableStateOf(false) }
    // BUG-03: when a suggestion appears the form scrolls just enough to show it above the keyboard
    val bringIntoView = remember { BringIntoViewRequester() }
    LaunchedEffect(state.suggestions.firstOrNull()?.note) {
        if (state.suggestions.isNotEmpty()) bringIntoView.bringIntoView()
    }
    Column(Modifier.bringIntoViewRequester(bringIntoView)) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FieldLabel("Título", focused)
            BasicTextField(
                value = state.note,
                onValueChange = actions.onNote,
                modifier = Modifier.fillMaxWidth().focusRequester(focus).onFocusChanged { focused = it.isFocused },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, color = colors.textPrimary),
                cursorBrush = SolidColor(colors.primary),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onDone() }),
                decorationBox = { inner ->
                    Box {
                        if (state.note.isEmpty()) {
                            Text(
                                if (state.type == TransactionType.TRANSFER) "Retiro cajero" else "Almuerzo, taxi, mercado",
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                                color = colors.textSecondary,
                            )
                        }
                        inner()
                    }
                },
            )
            state.errors[TransactionField.NOTE]?.let {
                Text(
                    it.message(),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.expense,
                )
            }
        }
        state.suggestions.forEach { s ->
            BlockDivider()
            val category = s.categoryId?.let { id -> choices.categories.firstOrNull { it.id == id } }
            val account = s.accountId?.let { id -> choices.accounts.firstOrNull { it.account.id == id } }
            val detail = listOfNotNull(category?.name, account?.account?.name).joinToString(" · ")
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClickLabel = "Usar") { actions.onSuggestion(s) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Outlined.History, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(20.dp))
                Column(Modifier.weight(1f)) {
                    // What was typed in Medium, as in Figma «**Alm**uerzo en restaurante»
                    Text(
                        boldMatch(s.note, state.note.trim()),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = colors.textPrimary,
                    )
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

/**
 * 2.2: nothing is left to ask for. Editing or reviewing a draft already has everything; a new movement needs the amount, the
 * account and, depending on the type, the category or the destination (a suggestion fills the category).
 */
internal fun readyToSave(state: TransactionFormState): Boolean =
    state.editingId != null ||
        state.draftId != null ||
        (
            state.amountDigits.isNotEmpty() &&
                state.accountId != null &&
                if (state.type == TransactionType.TRANSFER) state.destinationId != null else state.categoryId != null
        )

/** Categoría (§1.4): four frequent ones and «Más», 48 dp circles, one-line names; «Más» opens Hoja · Elegir categoría. */
@Composable
private fun CategorySection(
    state: TransactionFormState,
    choices: TransactionChoices,
    onCategory: (UUID) -> Unit,
    picking: Boolean,
    onPicking: (Boolean) -> Unit,
) {
    val colors = CofferTheme.colors
    val kind = if (state.type == TransactionType.EXPENSE) CategoryKind.EXPENSE else CategoryKind.INCOME
    val ranked = mostUsed(choices.categories, kind, choices.usage)
    val selected = ranked.firstOrNull { it.id == state.categoryId }
    // The chosen one stays in sight even when it is not among the most used
    val quick = (listOfNotNull(selected) + ranked).distinctBy { it.id }.take(QUICK_CATEGORIES)
    Column(Modifier.padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.padding(horizontal = 8.dp)) { FieldLabel("Categoría") }
        BubbleRow(quick.size, onMore = { onPicking(true) }, large = true) { i, cell ->
            val c = quick[i]
            IconBubble(
                c.name,
                iconOf(c, choices.categories),
                selected = c.id == state.categoryId,
                modifier = cell,
                large = true,
            ) { onCategory(c.id) }
        }
        state.errors[TransactionField.CATEGORY]?.let {
            Text(it.message(), Modifier.padding(horizontal = 8.dp), style = MaterialTheme.typography.bodySmall, color = colors.expense)
        }
    }
    if (picking) {
        CategoryGridSheet(
            ranked,
            choices.categories,
            selected = setOfNotNull(state.categoryId),
            onPick = {
                onCategory(it)
                onPicking(false)
            },
            onDismiss = { onPicking(false) },
        )
    }
}

/** Fecha y hora (§1.5): a 60 dp row, the value on one line; the whole row opens the picker («Modificar» is gone). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimeRow(
    occurredAt: Instant,
    onChange: (Instant) -> Unit,
    today: LocalDate,
    zone: ZoneId,
    onOpen: () -> Unit,
) {
    val colors = CofferTheme.colors
    var step by remember { mutableStateOf(0) } // 0 closed, 1 date, 2 time
    var date by remember { mutableStateOf(occurredAt.atZone(zone).toLocalDate()) }
    Row(
        Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clickable(onClickLabel = "Cambiar fecha y hora") {
                onOpen()
                step = 1
            }.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Outlined.Schedule, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f)) {
            FieldLabel("Fecha y hora")
            Text(
                shortDateTimeLabel(occurredAt, today, zone),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = colors.textSecondary)
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

private val SHORT_DATE = DateTimeFormatter.ofPattern("d 'de' MMM", ES_CO)
private val SHORT_DATE_YEAR = DateTimeFormatter.ofPattern("d 'de' MMM 'de' yyyy", ES_CO)

/** «Hoy, 7 de oct · 1:10 p. m.»: the year only when it is not this one, so it fits on one line (§1.5). */
internal fun shortDateTimeLabel(
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
    val day = (if (at.year == today.year) SHORT_DATE else SHORT_DATE_YEAR).format(at).replace(".", "")
    return prefix + day + " · " + TIME.format(at)
}

/** Cuenta (§1.5): 40 dp chips that slide sideways up to the block's right edge; no «Deslizar», the cut chip says it. */
@Composable
private fun AccountChips(
    accounts: List<AccountBalance>,
    selectedId: UUID?,
    error: com.dev.julian09.financeapp.domain.model.LedgerError?,
    onSelect: (UUID) -> Unit,
) {
    val colors = CofferTheme.colors
    Column(Modifier.padding(top = 12.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.padding(horizontal = 16.dp)) { FieldLabel("Cuenta") }
        if (accounts.isEmpty()) {
            Text(
                "Crea una cuenta en Cuentas para registrar movimientos.",
                Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(start = 16.dp)) {
            items(accounts, key = { it.account.id }) { item ->
                val selected = item.account.id == selectedId
                Row(
                    Modifier
                        .height(40.dp)
                        .clip(CofferShapes.pill)
                        .background(if (selected) colors.primaryContainer else colors.surface)
                        .border(1.dp, if (selected) colors.primary else colors.border, CofferShapes.pill)
                        .selectable(selected, role = Role.RadioButton) { onSelect(item.account.id) }
                        .padding(start = 4.dp, end = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        Modifier.size(32.dp).background(if (selected) colors.surface else colors.iconContainer, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            accountIcon(item.account.icon, item.account.type),
                            contentDescription = null,
                            tint = colors.iconOnContainer,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Text(
                        item.account.name,
                        style =
                            MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                            ),
                        color = if (selected) colors.onPrimaryContainer else colors.textPrimary,
                        maxLines = 1,
                    )
                }
            }
        }
        error?.let {
            Text(
                it.message(),
                Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = colors.expense,
            )
        }
    }
}

/** Cuentas of a transfer (§2.4): Desde and Hacia in one block, no balances, and ⇅ centered on the line between them. */
@Composable
private fun TransferAccounts(
    state: TransactionFormState,
    choices: TransactionChoices,
    actions: TransactionFormActions,
    onOpen: () -> Unit,
) {
    val colors = CofferTheme.colors
    Box {
        FormBlock {
            TransferAccountRow(
                "Desde",
                Icons.Outlined.NorthEast,
                choices.accounts,
                state.accountId,
                state.errors[TransactionField.ACCOUNT],
                onOpen,
                actions.onAccount,
            )
            BlockDivider()
            TransferAccountRow(
                "Hacia",
                Icons.Outlined.SouthEast,
                choices.accounts,
                state.destinationId,
                state.errors[TransactionField.DESTINATION],
                onOpen,
                actions.onDestination,
            )
        }
        IconButton(
            onClick = actions.onSwap,
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .size(40.dp)
                    .background(colors.surface, CircleShape)
                    .border(1.dp, colors.border, CircleShape),
        ) {
            Icon(
                Icons.Outlined.SwapVert,
                contentDescription = "Intercambiar origen y destino",
                tint = colors.textPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** One side of a transfer: the whole row opens the list to change the account. */
@Composable
private fun TransferAccountRow(
    label: String,
    direction: ImageVector,
    accounts: List<AccountBalance>,
    selectedId: UUID?,
    error: com.dev.julian09.financeapp.domain.model.LedgerError?,
    onOpen: () -> Unit,
    onSelect: (UUID) -> Unit,
) {
    val colors = CofferTheme.colors
    var choosing by rememberSaveable { mutableStateOf(false) }
    val chosen = accounts.firstOrNull { it.account.id == selectedId }
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Cambiar cuenta") {
                onOpen()
                choosing = true
            }.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FieldLabel(label, icon = direction)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CircleIcon(chosen?.let { accountIcon(it.account.icon, it.account.type) } ?: Icons.Outlined.AccountBalanceWallet)
            // Long names stop with «…» before the swap button in the middle (§2, «Se acepta si»)
            Column(Modifier.weight(1f).padding(end = 32.dp)) {
                Text(
                    chosen?.account?.name ?: "Elige una cuenta",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
                    color = if (chosen == null) colors.textSecondary else colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                chosen?.let {
                    Text(
                        listOfNotNull(
                            accountTypeShortLabel(it.account.type),
                            it.account.lastDigits?.let { d -> "*$d" } ?: it.account.institution,
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null, tint = colors.textSecondary)
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

/** Nota o concepto (opcional) of a transfer (§2.5), apart from its title (S12-H01). */
@Composable
private fun ConceptRow(
    concept: String,
    onConcept: (String) -> Unit,
) {
    val colors = CofferTheme.colors
    var focused by remember { mutableStateOf(false) }
    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FieldLabel("Nota o concepto (opcional)", focused, Icons.Outlined.Description)
        BasicTextField(
            value = concept,
            onValueChange = onConcept,
            modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, color = colors.textPrimary),
            cursorBrush = SolidColor(colors.primary),
            decorationBox = { inner ->
                Box {
                    if (concept.isEmpty()) {
                        Text(
                            "Para el efectivo de la semana",
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                            color = colors.textSecondary,
                        )
                    }
                    inner()
                }
            },
        )
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
            // Figma `20:428`: a 32 dp ring with the clock, the date in 14 Medium and a small «Modificar»
            Box(
                Modifier.size(32.dp).background(colors.background, CircleShape).border(1.dp, colors.border, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Schedule, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(15.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "Fecha y hora",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal),
                    color = colors.textSecondary,
                )
                Text(dateTimeLabel(occurredAt, today, zone), style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
            }
            TextButton(onClick = { step = 1 }) { Text("Modificar", style = MaterialTheme.typography.labelMedium, color = colors.primary) }
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
    FieldLabel("Etiquetas")
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
    // MOV-APP-001 (Figma `212:502`): icon and title over a divider, the count in a stepper box, four equal chips and the
    // rate in a box with its «Editable» badge
    AppCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Outlined.Tune, contentDescription = null, tint = colors.primary, modifier = Modifier.size(14.dp))
                Text(
                    "Condiciones del crédito",
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary,
                )
                if (count > 1) Badge("Diferido")
            }
            HorizontalDivider(thickness = 1.dp, color = colors.border)
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Número de cuotas", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                Text(
                    if (count == 1) "1 mes" else "$count meses",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.primary,
                )
            }
            Row(
                Modifier.fillMaxWidth().background(colors.background, CofferShapes.card).padding(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StepButton(Icons.Outlined.Remove, "Menos cuotas", count > 1) { actions.onInstallments(count - 1) }
                Row(
                    Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("$count", style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp), color = colors.textPrimary)
                    Text(if (count == 1) "cuota" else "cuotas", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                }
                StepButton(Icons.Outlined.Add, "Más cuotas", count < MAX_INSTALLMENTS) { actions.onInstallments(count + 1) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                QUICK_INSTALLMENTS.forEach { n ->
                    val selected = count == n
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(CofferShapes.card)
                            .background(if (selected) colors.primaryContainer else colors.background)
                            .border(1.dp, if (selected) colors.primary else colors.border, CofferShapes.card)
                            .clickable(role = Role.Button) { actions.onInstallments(n) }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (n == 1) "1 cuota" else "$n cuotas",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) colors.onPrimaryContainer else colors.textSecondary,
                            maxLines = 1,
                        )
                    }
                }
            }
            state.errors[TransactionField.INSTALLMENTS]?.let {
                Text(it.message(), style = MaterialTheme.typography.bodySmall, color = colors.expense)
            }
        }
        if (count > 1) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Tasa de interés pactada",
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textSecondary,
                    )
                    Text(
                        "Tasa de la tarjeta",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal),
                        color = colors.textSecondary,
                    )
                }
                val error = state.errors[TransactionField.RATE]
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(colors.background, CofferShapes.card)
                        .border(1.dp, if (error != null) colors.expense else colors.border, CofferShapes.card)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("%", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                    BasicTextField(
                        value = rateText,
                        onValueChange = actions.onRate,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        textStyle = MaterialTheme.typography.titleSmall.copy(color = colors.textPrimary),
                        cursorBrush = SolidColor(colors.primary),
                        modifier =
                            Modifier
                                .width(64.dp)
                                .background(colors.surface, RoundedCornerShape(8.dp))
                                .border(1.dp, colors.textSecondary, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                    Text(
                        "% ${rateTypeLabel(rateType)}",
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textSecondary,
                    )
                    Badge("Editable")
                }
                Text(
                    error?.message() ?: parsePercent(rateText)?.let { rateLabel(it, rateType) } ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (error != null) colors.expense else colors.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun Badge(text: String) {
    val colors = CofferTheme.colors
    Text(
        text,
        Modifier.background(colors.primaryContainer, CofferShapes.pill).padding(horizontal = 8.dp, vertical = 2.dp),
        style = MaterialTheme.typography.labelSmall,
        color = colors.onPrimaryContainer,
    )
}

@Composable
private fun StepButton(
    icon: ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = CofferTheme.colors
    Box(
        Modifier
            .size(36.dp)
            .clip(CofferShapes.card)
            .background(colors.surface)
            .border(1.dp, colors.border, CofferShapes.card)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = if (enabled) colors.textPrimary else colors.borderHighlight,
            modifier = Modifier.size(16.dp),
        )
    }
}
