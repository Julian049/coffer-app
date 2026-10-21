package com.dev.julian09.financeapp.ui.feature.tags

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.Tag
import com.dev.julian09.financeapp.ui.components.AppBottomSheet
import com.dev.julian09.financeapp.ui.components.AppButton
import com.dev.julian09.financeapp.ui.components.AppCard
import com.dev.julian09.financeapp.ui.components.AppHeader
import com.dev.julian09.financeapp.ui.components.AppTextField
import com.dev.julian09.financeapp.ui.components.ButtonKind
import com.dev.julian09.financeapp.ui.components.EmptyState
import com.dev.julian09.financeapp.ui.components.MoneyKind
import com.dev.julian09.financeapp.ui.components.MoneyText
import com.dev.julian09.financeapp.ui.components.SegmentedSelector
import com.dev.julian09.financeapp.ui.components.formatMoney
import com.dev.julian09.financeapp.ui.components.message
import com.dev.julian09.financeapp.ui.feature.history.DateField
import com.dev.julian09.financeapp.ui.feature.history.DayGroup
import com.dev.julian09.financeapp.ui.theme.CofferTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

private val MONTH = DateTimeFormatter.ofPattern("MMMM", Locale.forLanguageTag("es-CO"))

internal fun movementsThisMonth(count: Int) = if (count == 1) "1 movimiento este mes" else "$count movimientos este mes"

internal fun netKind(net: Money) =
    when {
        net.isNegative -> MoneyKind.Expense
        net == Money.ZERO -> MoneyKind.Neutral
        else -> MoneyKind.Income
    }

@Composable
fun TagsScreen(
    onBack: () -> Unit,
    onOpenTag: (UUID) -> Unit,
    viewModel: TagsViewModel = hiltViewModel(),
) {
    val lines by viewModel.lines.collectAsStateWithLifecycle()
    val sheet by viewModel.sheet.collectAsStateWithLifecycle()
    TagsContent(lines, onBack, onOpenTag, viewModel::startNew, viewModel::togglePin)
    sheet?.let { TagSheetContent(it, viewModel::dismiss, viewModel::edit, viewModel::save) }
}

/** Etiquetas (S9-UI02, REQ-ETQ-01, 04). */
@Composable
fun TagsContent(
    lines: List<TagLine>?,
    onBack: () -> Unit,
    onOpenTag: (UUID) -> Unit,
    onNew: () -> Unit,
    onTogglePin: (Tag) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CofferTheme.colors
    Column(modifier.fillMaxSize()) {
        AppHeader(title = "Etiquetas", onBack = onBack, actions = {
            IconButton(onClick = onNew) { Icon(Icons.Outlined.Add, contentDescription = "Nueva etiqueta", tint = colors.textPrimary) }
        })
        if (lines == null) return@Column
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Agrupan movimientos de distintas categorías bajo un mismo motivo. Cada transacción tiene como máximo una etiqueta.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
            if (lines.isEmpty()) {
                EmptyState(title = "Sin etiquetas", message = "Crea una para seguir un viaje, las salidas o cualquier otro motivo.")
            } else {
                AppCard {
                    lines.forEachIndexed { i, line ->
                        if (i > 0) HorizontalDivider(thickness = 1.dp, color = colors.border)
                        TagRow(line, onOpen = { onOpenTag(line.tag.id) }, onTogglePin = { onTogglePin(line.tag) })
                    }
                }
                Text(
                    "Las etiquetas con el alfiler en verde se muestran en inicio con su total del mes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
            AppButton(
                "Nueva etiqueta",
                onClick = onNew,
                kind = ButtonKind.Secondary,
                icon = Icons.Outlined.Add,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
internal fun TagRow(
    line: TagLine,
    onOpen: () -> Unit,
    onTogglePin: (() -> Unit)? = null,
) {
    val colors = CofferTheme.colors
    Row(
        Modifier.fillMaxWidth().clickable(onClickLabel = "Ver ${line.tag.name}", onClick = onOpen).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(40.dp).background(colors.iconContainer, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Sell, contentDescription = null, tint = colors.iconOnContainer, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(line.tag.name, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
            Text(movementsThisMonth(line.count), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        }
        MoneyText(line.net, kind = netKind(line.net), style = MaterialTheme.typography.titleSmall)
        onTogglePin?.let {
            IconButton(onClick = it) {
                Icon(
                    Icons.Outlined.PushPin,
                    contentDescription = if (line.tag.pinned) "Quitar de inicio" else "Fijar en inicio",
                    tint = if (line.tag.pinned) colors.income else colors.textSecondary,
                )
            }
        }
    }
}

/** Hoja · Nueva etiqueta, and its edit with "Eliminar" (S9-UI02). */
@Composable
internal fun TagSheetContent(
    sheet: TagSheet,
    onDismiss: () -> Unit,
    onEdit: ((TagSheet) -> TagSheet) -> Unit,
    onSave: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    val colors = CofferTheme.colors
    val editing = sheet.editing != null
    AppBottomSheet(onDismiss = onDismiss, title = if (editing) "Editar etiqueta" else "Nueva etiqueta") {
        AppTextField(value = sheet.name, onValueChange = { name ->
            onEdit { it.copy(name = name) }
        }, label = "Nombre", error = sheet.error?.message())
        if (!editing) PinCard(sheet.pinned) { pinned -> onEdit { it.copy(pinned = pinned) } }
        AppButton(if (editing) "Guardar cambios" else "Crear etiqueta", onClick = onSave, modifier = Modifier.fillMaxWidth())
        onDelete?.let {
            Text(
                "Al eliminarla, sus movimientos se quedan sin etiqueta.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
            AppButton("Eliminar etiqueta", onClick = it, kind = ButtonKind.Danger, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun PinCard(
    pinned: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val colors = CofferTheme.colors
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Fijar en inicio", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                Text(
                    "Ver su total del mes en la pantalla de inicio",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
            Switch(
                checked = pinned,
                onCheckedChange = onChange,
                colors = SwitchDefaults.colors(checkedTrackColor = colors.primary, checkedThumbColor = colors.onPrimary),
            )
        }
    }
}

@Composable
fun TagDetailScreen(
    onBack: () -> Unit,
    onOpenTransaction: (UUID) -> Unit,
    viewModel: TagDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sheet by viewModel.sheet.collectAsStateWithLifecycle()
    TagDetailContent(
        state,
        TagDetailActions(
            onBack = onBack,
            onEdit = viewModel::startEdit,
            onRange = viewModel::selectRange,
            onFrom = viewModel::setFrom,
            onTo = viewModel::setTo,
            onTogglePin = viewModel::togglePin,
            onOpenTransaction = onOpenTransaction,
        ),
    )
    sheet?.let { TagSheetContent(it, viewModel::dismiss, viewModel::edit, viewModel::save, onDelete = { viewModel.delete(onBack) }) }
}

data class TagDetailActions(
    val onBack: () -> Unit = {},
    val onEdit: () -> Unit = {},
    val onRange: (TagRange) -> Unit = {},
    val onFrom: (LocalDate) -> Unit = {},
    val onTo: (LocalDate) -> Unit = {},
    val onTogglePin: () -> Unit = {},
    val onOpenTransaction: (UUID) -> Unit = {},
)

/** Detalle de etiqueta (S9-UI02, REQ-ETQ-03). */
@Composable
fun TagDetailContent(
    state: TagDetailState?,
    actions: TagDetailActions,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now(),
) {
    val colors = CofferTheme.colors
    Column(modifier.fillMaxSize()) {
        AppHeader(title = state?.tag?.name.orEmpty(), onBack = actions.onBack, actions = {
            IconButton(
                onClick = actions.onEdit,
            ) { Icon(Icons.Outlined.Edit, contentDescription = "Editar etiqueta", tint = colors.textPrimary) }
        })
        if (state == null) return@Column
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val ranges = TagRange.entries
            SegmentedSelector(
                listOf("Mes actual", "Mes anterior", "Rango"),
                selectedIndex = ranges.indexOf(state.range),
                onSelect = { actions.onRange(ranges[it]) },
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.range == TagRange.RANGE) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DateField("Desde", state.from, Modifier.weight(1f), actions.onFrom)
                    DateField("Hasta", state.to, Modifier.weight(1f), actions.onTo)
                }
            }
            AppCard(Modifier.fillMaxWidth(), highlight = true) {
                val title = if (state.range == TagRange.RANGE) "Total del rango" else "Total en ${MONTH.format(state.from)}"
                Text(title, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                MoneyText(state.net, kind = netKind(state.net), style = MaterialTheme.typography.displaySmall)
                val movements = if (state.count == 1) "1 movimiento" else "${state.count} movimientos"
                val cats = state.byCategory.size.let { if (it == 1) "1 categoría" else "$it categorías" }
                Text("$movements en $cats", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            }
            if (state.byCategory.isNotEmpty()) {
                AppCard {
                    Text("Por categoría", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                    val max =
                        state.byCategory
                            .first()
                            .second.scaled
                            .coerceAtLeast(1)
                    state.byCategory.forEach { (category, total) ->
                        Row {
                            Text(
                                category.name,
                                Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary,
                            )
                            Text(formatMoney(total), style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                        }
                        Box(Modifier.fillMaxWidth().height(6.dp).background(colors.border, RoundedCornerShape(3.dp))) {
                            Box(
                                Modifier
                                    .fillMaxWidth(total.scaled.toFloat() / max)
                                    .fillMaxHeight()
                                    .background(colors.primary, RoundedCornerShape(3.dp)),
                            )
                        }
                    }
                }
            }
            PinCard(state.tag.pinned) { actions.onTogglePin() }
            Text("Movimientos", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
            if (state.days.isEmpty()) {
                Text("Sin movimientos en este periodo.", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
            } else {
                state.days.forEach { DayGroup(it, today, onOpen = actions.onOpenTransaction) }
            }
        }
    }
}
