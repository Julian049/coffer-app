package com.dev.julian09.financeapp.ui.feature.mail

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.julian09.financeapp.domain.model.CategoryKind
import com.dev.julian09.financeapp.ui.components.AppButton
import com.dev.julian09.financeapp.ui.components.AppHeader
import com.dev.julian09.financeapp.ui.components.AppTextField
import com.dev.julian09.financeapp.ui.components.BubbleRow
import com.dev.julian09.financeapp.ui.components.CategoryGridSheet
import com.dev.julian09.financeapp.ui.components.ConfirmDialog
import com.dev.julian09.financeapp.ui.components.IconBubble
import com.dev.julian09.financeapp.ui.components.PesosTransformation
import com.dev.julian09.financeapp.ui.components.SegmentedSelector
import com.dev.julian09.financeapp.ui.components.accountIcon
import com.dev.julian09.financeapp.ui.components.iconOf
import com.dev.julian09.financeapp.ui.feature.transactions.mostUsed
import com.dev.julian09.financeapp.ui.theme.CofferShapes
import com.dev.julian09.financeapp.ui.theme.CofferTheme
import java.util.UUID

/** What the rule form does; one object so the stateless content stays readable. */
data class DraftRuleFormActions(
    val onMerchant: (String) -> Unit = {},
    val onExactAmount: (Boolean) -> Unit = {},
    val onAmount: (String) -> Unit = {},
    val onOneAccount: (Boolean) -> Unit = {},
    val onAccount: (UUID) -> Unit = {},
    val onNote: (String) -> Unit = {},
    val onCategory: (UUID) -> Unit = {},
    val onSave: () -> Unit = {},
    val onDelete: () -> Unit = {},
    val onConfirmDelete: () -> Unit = {},
    val onDismissDelete: () -> Unit = {},
    val onOpenRule: (UUID) -> Unit = {},
)

@Composable
fun DraftRuleFormScreen(
    onBack: () -> Unit,
    onOpenRule: (UUID) -> Unit,
    viewModel: DraftRuleFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val choices by viewModel.choices.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) onBack() }
    DraftRuleFormContent(
        state,
        choices,
        onBack,
        DraftRuleFormActions(
            onMerchant = viewModel::merchant,
            onExactAmount = viewModel::exactAmount,
            onAmount = viewModel::amount,
            onOneAccount = viewModel::oneAccount,
            onAccount = viewModel::account,
            onNote = viewModel::note,
            onCategory = viewModel::category,
            onSave = viewModel::saveRule,
            onDelete = viewModel::requestDelete,
            onConfirmDelete = viewModel::confirmDelete,
            onDismissDelete = viewModel::dismissDelete,
            onOpenRule = onOpenRule,
        ),
    )
}

/** Nueva regla (`258:676`), Ya existe (`259:676`) and Editar regla (`259:807`, `261:676`); changes-ui-2.5 §3–5. */
@Composable
fun DraftRuleFormContent(
    state: DraftRuleFormState,
    choices: DraftRuleChoices,
    onBack: () -> Unit,
    actions: DraftRuleFormActions,
    modifier: Modifier = Modifier,
) {
    val colors = CofferTheme.colors
    val editing = state.editing != null
    if (state.confirmingDelete) {
        ConfirmDialog(
            title = "¿Eliminar regla?",
            message = "Desde ahora, los movimientos de ${state.editing?.merchant.orEmpty()} vuelven a quedar en «Por revisar».",
            confirmText = "Eliminar",
            onConfirm = actions.onConfirmDelete,
            onDismiss = actions.onDismissDelete,
            destructive = true,
        )
    }
    Column(modifier.fillMaxSize().imePadding()) {
        AppHeader(title = if (editing) "Editar regla" else "Nueva regla", onBack = onBack) {
            if (editing) {
                IconButton(onClick = actions.onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Eliminar regla", tint = colors.expense)
                }
            }
        }
        if (!state.loaded) return@Column
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.learnedNotice?.let { LearnedNotice(it) }
            SectionLabel("Cuando llegue un movimiento de")
            Block(error = state.duplicate != null) {
                MerchantRow(state, actions)
                Divider()
                AmountRow(state, actions)
                Divider()
                AccountRow(state, choices, actions)
            }
            state.duplicate?.let { other ->
                Text(
                    "Ya tienes una regla igual: «${other.note}».",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = colors.expense,
                )
                Text(
                    "Ver regla",
                    Modifier.heightIn(min = 44.dp).clickable { actions.onOpenRule(other.id) }.padding(vertical = 12.dp),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                    color = colors.primary,
                )
            }
            SectionLabel("Se registra así")
            Block {
                NoteRow(state, actions)
                Divider()
                CategoryRow(state, choices, actions)
            }
            Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Bolt, contentDescription = null, tint = colors.iconOnContainer, modifier = Modifier.size(16.dp))
                Text(
                    // Saving a probable rule makes it sure (PO, 2026-11-23), so the note says what happens after Guardar
                    if (state.turnsSure) {
                        "Al guardar cambios, se registra directo en tu historial, con aviso y Deshacer."
                    } else {
                        "Se registra directo en tu historial, con aviso y Deshacer."
                    },
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = colors.textSecondary,
                )
            }
        }
        Box(Modifier.fillMaxWidth().padding(16.dp)) {
            AppButton(
                if (editing) "Guardar cambios" else "Guardar regla",
                onClick = actions.onSave,
                icon = Icons.Outlined.Check,
                enabled = state.canSave,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun LearnedNotice(text: String) {
    val colors = CofferTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .background(colors.iconContainer, CofferShapes.card)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(18.dp).background(colors.primary, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(10.dp))
        }
        Text(text, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = colors.iconOnContainer)
    }
}

@Composable
private fun SectionLabel(text: String) =
    Text(text, Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodySmall, color = CofferTheme.colors.textSecondary)

/** A white card with a thin border, 16 radius; red border when its conditions repeat another rule's. */
@Composable
private fun Block(
    error: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = CofferTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(CofferShapes.highlight)
            .background(colors.surface)
            .border(1.dp, if (error) colors.expense else colors.border, CofferShapes.highlight),
    ) { content() }
}

@Composable
private fun Divider() = HorizontalDivider(thickness = 1.dp, color = CofferTheme.colors.border)

@Composable
private fun FieldLabel(text: String) =
    Text(text, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = CofferTheme.colors.textSecondary)

@Composable
private fun PlainField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
) {
    val colors = CofferTheme.colors
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, color = colors.textPrimary),
        cursorBrush = SolidColor(colors.primary),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) {
                    Text(placeholder, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp), color = colors.textSecondary)
                }
                inner()
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MerchantRow(
    state: DraftRuleFormState,
    actions: DraftRuleFormActions,
) {
    val colors = CofferTheme.colors
    Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FieldLabel("Comercio · obligatorio")
        PlainField(state.merchant, actions.onMerchant, "Uber, Rappi, Éxito")
        aliasesLine(state.merchant, state.aliases)?.let {
            Text(it, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = colors.textSecondary)
        }
        val suggestions = state.suggestions.filter { it != state.merchant.trim() }
        if (state.editing == null && suggestions.isNotEmpty()) {
            Box(Modifier.padding(top = 6.dp)) { FieldLabel("Sugerencias") }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                suggestions.forEach { s ->
                    Text(
                        s,
                        Modifier
                            .clip(CofferShapes.pill)
                            .border(1.dp, colors.border, CofferShapes.pill)
                            .clickable { actions.onMerchant(s) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = colors.textPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun AmountRow(
    state: DraftRuleFormState,
    actions: DraftRuleFormActions,
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Monto")
        SegmentedSelector(
            listOf("Cualquiera", "Exacto"),
            selectedIndex = if (state.exactAmount) 1 else 0,
            onSelect = { actions.onExactAmount(it == 1) },
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.exactAmount) {
            AppTextField(
                value = state.amountDigits,
                onValueChange = actions.onAmount,
                label = null,
                placeholder = "$ 0",
                keyboardType = KeyboardType.Number,
                visualTransformation = PesosTransformation(),
            )
        }
    }
}

@Composable
private fun AccountRow(
    state: DraftRuleFormState,
    choices: DraftRuleChoices,
    actions: DraftRuleFormActions,
) {
    val colors = CofferTheme.colors
    Column(Modifier.padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FieldLabel("Cuenta")
            SegmentedSelector(
                listOf("Cualquiera", "Una cuenta"),
                selectedIndex = if (state.oneAccount) 1 else 0,
                onSelect = { actions.onOneAccount(it == 1) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (state.oneAccount) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
                items(choices.accounts, key = { it.account.id }) { item ->
                    val selected = item.account.id == state.accountId
                    Row(
                        Modifier
                            .height(40.dp)
                            .clip(CofferShapes.pill)
                            .background(if (selected) colors.primaryContainer else colors.surface)
                            .border(1.dp, if (selected) colors.primary else colors.border, CofferShapes.pill)
                            .selectable(selected, role = Role.RadioButton) { actions.onAccount(item.account.id) }
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
        }
    }
}

@Composable
private fun NoteRow(
    state: DraftRuleFormState,
    actions: DraftRuleFormActions,
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FieldLabel("Título")
        PlainField(state.note, actions.onNote, "Almuerzo, taxi, mercado")
    }
}

/** Categoría as in Editar transacción: four frequent ones and «Más», which opens Hoja · Elegir categoría. */
@Composable
private fun CategoryRow(
    state: DraftRuleFormState,
    choices: DraftRuleChoices,
    actions: DraftRuleFormActions,
) {
    var picking by rememberSaveable { mutableStateOf(false) }
    val ranked = mostUsed(choices.categories, CategoryKind.EXPENSE, choices.usage)
    val selected = ranked.firstOrNull { it.id == state.categoryId }
    val quick = (listOfNotNull(selected) + ranked).distinctBy { it.id }.take(4)
    Column(Modifier.padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.padding(horizontal = 8.dp)) { FieldLabel("Categoría") }
        BubbleRow(quick.size, onMore = { picking = true }, large = true) { i, cell ->
            val c = quick[i]
            IconBubble(c.name, iconOf(c, choices.categories), selected = c.id == state.categoryId, modifier = cell, large = true) {
                actions.onCategory(c.id)
            }
        }
    }
    if (picking) {
        CategoryGridSheet(
            ranked,
            choices.categories,
            selected = setOfNotNull(state.categoryId),
            onPick = {
                actions.onCategory(it)
                picking = false
            },
            onDismiss = { picking = false },
        )
    }
}
