package com.dev.julian09.financeapp.ui.feature.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.julian09.financeapp.domain.model.Category
import com.dev.julian09.financeapp.domain.model.CategoryKind
import com.dev.julian09.financeapp.domain.model.LedgerError
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.ui.components.AppBottomSheet
import com.dev.julian09.financeapp.ui.components.AppButton
import com.dev.julian09.financeapp.ui.components.AppCard
import com.dev.julian09.financeapp.ui.components.AppChip
import com.dev.julian09.financeapp.ui.components.AppHeader
import com.dev.julian09.financeapp.ui.components.AppTextField
import com.dev.julian09.financeapp.ui.components.ButtonKind
import com.dev.julian09.financeapp.ui.components.CATEGORY_ICONS
import com.dev.julian09.financeapp.ui.components.MoneyKind
import com.dev.julian09.financeapp.ui.components.MoneyText
import com.dev.julian09.financeapp.ui.components.SegmentedSelector
import com.dev.julian09.financeapp.ui.components.categoryIcon
import com.dev.julian09.financeapp.ui.components.formatMoney
import com.dev.julian09.financeapp.ui.components.message
import com.dev.julian09.financeapp.ui.feature.accounts.OptionRow
import com.dev.julian09.financeapp.ui.theme.CofferTheme
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

@Composable
fun CategoriesScreen(
    onBack: () -> Unit,
    viewModel: CategoriesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sheet by viewModel.sheet.collectAsStateWithLifecycle()
    CategoriesContent(state, onBack, viewModel::selectKind, viewModel::startNew, viewModel::openOptions)
    when (val s = sheet) {
        null -> Unit
        is CategorySheet.New ->
            NewCategorySheet(
                s,
                mains = state?.nodes?.map { it.line.category }.orEmpty(),
                onDismiss = viewModel::dismiss,
                onEdit = viewModel::editNew,
                onSave = viewModel::saveNew,
            )
        is CategorySheet.Options ->
            AppBottomSheet(onDismiss = viewModel::dismiss, title = s.category.name) {
                OptionRow(Icons.Outlined.Edit, "Renombrar", null, danger = false, onClick = viewModel::startRename)
                OptionRow(Icons.Outlined.DeleteOutline, "Eliminar", null, danger = true, onClick = viewModel::startDelete)
                AppButton("Cancelar", onClick = viewModel::dismiss, kind = ButtonKind.Secondary, modifier = Modifier.fillMaxWidth())
            }
        is CategorySheet.Rename ->
            AppBottomSheet(onDismiss = viewModel::dismiss, title = "Renombrar categoría") {
                AppTextField(value = s.name, onValueChange = viewModel::typeName, label = "Nombre", error = s.error?.message())
                AppButton("Guardar", onClick = viewModel::saveRename, modifier = Modifier.fillMaxWidth())
            }
        is CategorySheet.Delete -> DeleteCategorySheet(s, viewModel::dismiss, viewModel::selectTarget, viewModel::confirmDelete)
    }
}

/** Categorías: expense and income tabs, monthly totals and subcategories (S4-UI03). */
@Composable
fun CategoriesContent(
    state: CategoriesState?,
    onBack: () -> Unit,
    onSelectKind: (CategoryKind) -> Unit,
    onNew: () -> Unit,
    onOpen: (Category) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CofferTheme.colors
    Column(modifier.fillMaxSize()) {
        AppHeader(title = "Categorías", onBack = onBack)
        if (state == null) return@Column
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val kinds = listOf(CategoryKind.EXPENSE, CategoryKind.INCOME)
            SegmentedSelector(
                kinds.map { (if (it == CategoryKind.EXPENSE) "Gastos" else "Ingresos") + " · ${state.counts[it] ?: 0}" },
                selectedIndex = kinds.indexOf(state.kind),
                onSelect = { onSelectKind(kinds[it]) },
                modifier = Modifier.fillMaxWidth(),
            )
            AppButton("Nueva categoría", onClick = onNew, icon = Icons.Outlined.AddCircleOutline, modifier = Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(40.dp).background(colors.iconContainer, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.PieChart, contentDescription = null, tint = colors.iconOnContainer, modifier = Modifier.size(20.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text("Total del mes", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    MoneyText(state.monthTotal, style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    if (state.subcategories == 1) "1 subcategoría" else "${state.subcategories} subcategorías",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
            val kind = if (state.kind == CategoryKind.EXPENSE) MoneyKind.Expense else MoneyKind.Income
            state.nodes.forEach { node -> CategoryCard(node, state.monthTotal, kind, onOpen) }
        }
    }
}

private fun share(
    part: Money,
    whole: Money,
): String? {
    if (whole == Money.ZERO || part == Money.ZERO) return null
    val pct = part.toBigDecimal().multiply(BigDecimal(100)).divide(whole.toBigDecimal(), 1, RoundingMode.HALF_UP)
    return "${pct.toPlainString().replace('.', ',')} % del total"
}

private fun movements(count: Int) = if (count == 1) "1 transacción" else "$count transacciones"

@Composable
private fun CategoryCard(
    node: CategoryNode,
    monthTotal: Money,
    kind: MoneyKind,
    onOpen: (Category) -> Unit,
) {
    val colors = CofferTheme.colors
    val main = node.line.category
    AppCard {
        Row(
            Modifier.fillMaxWidth().clickable(onClickLabel = "Opciones de ${main.name}") { onOpen(main) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.size(40.dp).background(colors.iconContainer, CircleShape), contentAlignment = Alignment.Center) {
                Icon(categoryIcon(main.icon), contentDescription = null, tint = colors.iconOnContainer, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(main.name, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                val sub =
                    when (node.children.size) {
                        0 -> "Categoría principal"
                        1 -> "1 subcategoría"
                        else -> "${node.children.size} subcategorías"
                    }
                Text("$sub · ${movements(node.count)}", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            }
            Column(horizontalAlignment = Alignment.End) {
                MoneyText(
                    node.total,
                    kind =
                        if (node.total ==
                            Money.ZERO
                        ) {
                            MoneyKind.Neutral
                        } else {
                            kind
                        },
                    style = MaterialTheme.typography.titleMedium,
                )
                share(node.total, monthTotal)?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary) }
            }
        }
        if (node.children.isNotEmpty()) HorizontalDivider(thickness = 1.dp, color = colors.border)
        node.children.forEach { child ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClickLabel = "Opciones de ${child.category.name}",
                    ) { onOpen(child.category) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "•  ${child.category.name}",
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.textPrimary,
                )
                Text(
                    "${formatMoney(child.total)} (${child.count} tx)",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NewCategorySheet(
    sheet: CategorySheet.New,
    mains: List<Category>,
    onDismiss: () -> Unit,
    onEdit: ((CategorySheet.New) -> CategorySheet.New) -> Unit,
    onSave: () -> Unit,
) {
    val colors = CofferTheme.colors
    val kinds = listOf(CategoryKind.EXPENSE, CategoryKind.INCOME)
    AppBottomSheet(onDismiss = onDismiss, title = "Nueva categoría") {
        SegmentedSelector(
            listOf("Gasto", "Ingreso"),
            selectedIndex = kinds.indexOf(sheet.kind),
            onSelect = { i -> onEdit { it.copy(kind = kinds[i], parentId = null) } },
            modifier = Modifier.fillMaxWidth(),
        )
        AppTextField(
            value = sheet.name,
            onValueChange = { name -> onEdit { it.copy(name = name) } },
            label = "Nombre",
            error = sheet.error?.takeIf { it != LedgerError.ONE_LEVEL_ONLY && it != LedgerError.KIND_MISMATCH }?.message(),
        )
        Text("Ubicación", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AppChip("Categoría principal", selected = sheet.parentId == null, onClick = { onEdit { it.copy(parentId = null) } })
            mains.forEach { parent ->
                AppChip(
                    "Subcategoría de ${parent.name}",
                    selected = sheet.parentId == parent.id,
                    onClick = { onEdit { it.copy(parentId = parent.id) } },
                )
            }
        }
        if (sheet.parentId == null) {
            Text("Ícono", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                CATEGORY_ICONS.forEach { (key, icon) ->
                    val selected = sheet.icon == key
                    Box(
                        Modifier
                            .size(44.dp)
                            .background(if (selected) colors.primaryContainer else colors.background, CircleShape)
                            .selectable(selected, role = Role.RadioButton, onClick = { onEdit { it.copy(icon = key) } }),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(icon, contentDescription = key, tint = if (selected) colors.onPrimaryContainer else colors.textSecondary)
                    }
                }
            }
        }
        sheet.error?.takeIf { it == LedgerError.ONE_LEVEL_ONLY || it == LedgerError.KIND_MISMATCH }?.let {
            Text(it.message(), style = MaterialTheme.typography.bodySmall, color = colors.expense)
        }
        AppButton("Crear categoría", onClick = onSave, modifier = Modifier.fillMaxWidth())
    }
}

/** REQ-CAT-03: subcategories go too, and every movement moves to the chosen category itself (PO, 2026-10-17). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DeleteCategorySheet(
    sheet: CategorySheet.Delete,
    onDismiss: () -> Unit,
    onSelectTarget: (UUID) -> Unit,
    onConfirm: () -> Unit,
) {
    val colors = CofferTheme.colors
    val needsTarget = sheet.movements > 0
    AppBottomSheet(onDismiss = onDismiss, title = "Eliminar «${sheet.category.name}»") {
        val parts =
            buildList {
                if (sheet.subcategories > 0) {
                    add(
                        if (sheet.subcategories == 1) {
                            "También se elimina su subcategoría."
                        } else {
                            "También se eliminan sus ${sheet.subcategories} subcategorías."
                        },
                    )
                }
                if (needsTarget) {
                    add(
                        (if (sheet.movements == 1) "Su movimiento pasa" else "Sus ${sheet.movements} movimientos pasan") +
                            " a la categoría que elijas, directamente a ella y no a sus subcategorías.",
                    )
                } else {
                    add("No tiene movimientos.")
                }
            }
        Text(parts.joinToString(" "), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        if (needsTarget) {
            Text("Mover a", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                sheet.targets.forEach { AppChip(it.name, selected = sheet.targetId == it.id, onClick = { onSelectTarget(it.id) }) }
            }
        }
        sheet.error?.let { Text(it.message(), style = MaterialTheme.typography.bodySmall, color = colors.expense) }
        AppButton(
            "Eliminar categoría",
            onClick = onConfirm,
            kind = ButtonKind.Danger,
            enabled = !needsTarget || sheet.targetId != null,
            modifier = Modifier.fillMaxWidth(),
        )
        AppButton("Cancelar", onClick = onDismiss, kind = ButtonKind.Secondary, modifier = Modifier.fillMaxWidth())
    }
}
