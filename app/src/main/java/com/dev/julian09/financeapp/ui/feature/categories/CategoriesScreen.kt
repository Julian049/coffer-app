package com.dev.julian09.financeapp.ui.feature.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextOverflow
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
import com.dev.julian09.financeapp.ui.components.BubbleRow
import com.dev.julian09.financeapp.ui.components.ButtonKind
import com.dev.julian09.financeapp.ui.components.CATEGORY_ICONS
import com.dev.julian09.financeapp.ui.components.CategoryGridSheet
import com.dev.julian09.financeapp.ui.components.IconBubble
import com.dev.julian09.financeapp.ui.components.IconPickerSheet
import com.dev.julian09.financeapp.ui.components.IconRow
import com.dev.julian09.financeapp.ui.components.MoneyText
import com.dev.julian09.financeapp.ui.components.SegmentedSelector
import com.dev.julian09.financeapp.ui.components.categoryIcon
import com.dev.julian09.financeapp.ui.components.message
import com.dev.julian09.financeapp.ui.feature.accounts.OptionRow
import com.dev.julian09.financeapp.ui.theme.CofferShapes
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
    CategoriesContent(
        state,
        onBack,
        viewModel::selectKind,
        { viewModel.startNew() },
        viewModel::openOptions,
        onAddSub = viewModel::startNew,
    )
    when (val s = sheet) {
        null -> Unit
        is CategorySheet.New ->
            NewCategorySheet(
                s,
                mains =
                    state
                        ?.nodes
                        ?.sortedByDescending { it.count }
                        ?.map { it.line.category }
                        .orEmpty(),
                onDismiss = viewModel::dismiss,
                onEdit = viewModel::editNew,
                onSave = viewModel::saveNew,
            )
        is CategorySheet.Options ->
            AppBottomSheet(onDismiss = viewModel::dismiss, title = s.category.name) {
                if (s.category.parentId == null) ExcludeRow(s.category.excludeFromReports, viewModel::setExcluded)
                OptionRow(Icons.Outlined.Edit, "Renombrar", null, danger = false, onClick = viewModel::startRename)
                OptionRow(Icons.Outlined.Palette, "Cambiar ícono", null, danger = false, onClick = viewModel::startIcon)
                // S17-H13 (MEJORA-01): any expense category, main or sub, can receive the installments of loans
                if (s.category.kind == CategoryKind.EXPENSE) {
                    OptionRow(
                        Icons.Outlined.Payments,
                        "Usar para cuotas de préstamos",
                        if (s.usedForLoans) "Es la categoría de las cuotas ahora" else "El capital de tus cuotas contará aquí",
                        danger = false,
                        onClick = { if (!s.usedForLoans) viewModel.useForLoans() },
                    )
                }
                OptionRow(Icons.Outlined.DeleteOutline, "Eliminar", null, danger = true, onClick = viewModel::startDelete)
                AppButton("Cancelar", onClick = viewModel::dismiss, kind = ButtonKind.Secondary, modifier = Modifier.fillMaxWidth())
            }
        is CategorySheet.Rename ->
            AppBottomSheet(
                onDismiss = viewModel::dismiss,
                title = "Renombrar categoría",
                actions = { AppButton("Guardar", onClick = viewModel::saveRename, modifier = Modifier.fillMaxWidth()) },
            ) {
                AppTextField(value = s.name, onValueChange = viewModel::typeName, label = "Nombre", error = s.error?.message())
            }
        is CategorySheet.Exclude ->
            AppBottomSheet(onDismiss = viewModel::dismiss, title = "Excluir «${s.category.name}» de reportes") {
                Text(
                    "Los movimientos de esta categoría no cuentan en ningún reporte, ni en los gastos e ingresos del mes de Inicio " +
                        "ni en el zorro. También aplica a sus subcategorías y a los meses pasados. El saldo de tus cuentas no cambia.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CofferTheme.colors.textSecondary,
                )
                AppButton("Excluir de reportes", onClick = viewModel::confirmExclude, modifier = Modifier.fillMaxWidth())
                AppButton("Cancelar", onClick = viewModel::dismiss, kind = ButtonKind.Secondary, modifier = Modifier.fillMaxWidth())
            }
        is CategorySheet.Icon -> IconPickerSheet(s.category.icon, onPick = viewModel::saveIcon, onDismiss = viewModel::dismiss)
        is CategorySheet.Delete -> DeleteCategorySheet(s, viewModel::dismiss, viewModel::selectTarget, viewModel::confirmDelete)
    }
}

/** Categorías · Versión C, mosaico (Figma `86:1199`): each main category in a card, its subcategories as tiles. */
@Composable
fun CategoriesContent(
    state: CategoriesState?,
    onBack: () -> Unit,
    onSelectKind: (CategoryKind) -> Unit,
    onNew: () -> Unit,
    onOpen: (Category) -> Unit,
    modifier: Modifier = Modifier,
    onAddSub: (UUID) -> Unit = {},
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
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Total del mes", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                MoneyText(state.monthTotal, style = MaterialTheme.typography.displaySmall)
                Text(
                    "${plural(state.nodes.size, "categoría", "categorías")} · " +
                        plural(state.subcategories, "subcategoría", "subcategorías"),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
            state.nodes.forEach { node -> CategoryCard(node, state.monthTotal, onOpen, onAddSub) }
        }
    }
}

private fun plural(
    n: Int,
    one: String,
    many: String,
) = if (n == 1) "1 $one" else "$n $many"

private fun share(
    part: Money,
    whole: Money,
): String? {
    if (whole == Money.ZERO || part == Money.ZERO) return null
    val pct = part.toBigDecimal().multiply(BigDecimal(100)).divide(whole.toBigDecimal(), 1, RoundingMode.HALF_UP)
    return "${pct.toPlainString().replace('.', ',')} % del total"
}

@Composable
private fun ExcludeRow(
    excluded: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val colors = CofferTheme.colors
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Excluir de reportes", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                Text(
                    "Para ahorros o ajustes de saldo: no cuentan como gasto ni ingreso.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
            Switch(
                checked = excluded,
                onCheckedChange = onChange,
                colors = SwitchDefaults.colors(checkedTrackColor = colors.primary, checkedThumbColor = colors.onPrimary),
            )
        }
    }
}

private fun movements(count: Int) = plural(count, "movimiento", "movimientos")

@Composable
private fun CategoryCard(
    node: CategoryNode,
    monthTotal: Money,
    onOpen: (Category) -> Unit,
    onAddSub: (UUID) -> Unit,
) {
    val colors = CofferTheme.colors
    val main = node.line.category
    AppCard {
        Row(
            Modifier.fillMaxWidth().clickable(onClickLabel = "Opciones de ${main.name}") { onOpen(main) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.size(48.dp).background(colors.primary, CircleShape), contentAlignment = Alignment.Center) {
                Icon(categoryIcon(main.icon), contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(24.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(main.name, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                val excluded = if (main.excludeFromReports) " · Excluida de reportes" else ""
                Text(movements(node.count) + excluded, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            }
            Column(horizontalAlignment = Alignment.End) {
                MoneyText(node.total, style = MaterialTheme.typography.titleLarge)
                share(node.total, monthTotal)?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary) }
            }
        }
        if (monthTotal > Money.ZERO) {
            LinearProgressIndicator(
                progress = { (node.total.toBigDecimal().toFloat() / monthTotal.toBigDecimal().toFloat()).coerceIn(0f, 1f) },
                Modifier.fillMaxWidth().height(6.dp),
                color = colors.primary,
                trackColor = colors.border,
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
        }
        // S15-UI07: the subcategories in tiles of three, «Añadir» being the last one
        (node.children + listOf(null)).chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { child ->
                    if (child == null) {
                        SubTile(Modifier.weight(1f), onClick = { onAddSub(main.id) }) {
                            Box(Modifier.size(32.dp).background(colors.iconContainer, CircleShape), contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Outlined.Add,
                                    contentDescription = null,
                                    tint = colors.iconOnContainer,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Text("Añadir", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                        }
                    } else {
                        SubTile(Modifier.weight(1f), onClick = { onOpen(child.category) }) {
                            Box(
                                Modifier.size(32.dp).background(colors.iconContainer, CofferShapes.card),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    categoryIcon(child.category.icon ?: main.icon),
                                    contentDescription = null,
                                    tint = colors.iconOnContainer,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Text(
                                child.category.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            MoneyText(child.total, style = MaterialTheme.typography.titleSmall)
                            Text(
                                plural(child.count, "mov.", "mov."),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary,
                            )
                        }
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun SubTile(
    modifier: Modifier,
    onClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CofferTheme.colors
    Column(
        modifier
            .clip(CofferShapes.card)
            .background(colors.background)
            .border(1.dp, colors.border, CofferShapes.card)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        content = content,
    )
}

/** Hoja · Nueva categoría (Figma `20:1212`, `115:192`): Ubicación and Ícono as icon rows, and the button fixed at the foot. */
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
    var morePlaces by rememberSaveable { mutableStateOf(false) }
    val taken = sheet.error == LedgerError.NAME_TAKEN
    val ofKind = mains.filter { it.kind == sheet.kind }
    AppBottomSheet(
        onDismiss = onDismiss,
        title = "Nueva categoría",
        actions = { AppButton("Crear categoría", onClick = onSave, enabled = !taken, modifier = Modifier.fillMaxWidth()) },
    ) {
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
            error =
                when {
                    taken -> "Ya existe una categoría llamada «${sheet.name.trim()}». Usa otro nombre."
                    sheet.error == LedgerError.ONE_LEVEL_ONLY || sheet.error == LedgerError.KIND_MISMATCH -> null
                    else -> sheet.error?.message()
                },
        )
        Text("Ubicación · dentro de qué categoría va", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        val chosen = ofKind.firstOrNull { it.id == sheet.parentId }
        val parents = (listOfNotNull(chosen) + ofKind).distinctBy { it.id }.take(3)
        BubbleRow(parents.size + 1, onMore = { morePlaces = true }) { i, cell ->
            if (i == 0) {
                IconBubble("Principal", Icons.Outlined.Category, selected = sheet.parentId == null, modifier = cell) {
                    onEdit { it.copy(parentId = null) }
                }
            } else {
                val parent = parents[i - 1]
                IconBubble(parent.name, categoryIcon(parent.icon), selected = sheet.parentId == parent.id, modifier = cell) {
                    onEdit { it.copy(parentId = parent.id) }
                }
            }
        }
        Text("Ícono", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        IconRow(sheet.icon, CATEGORY_ICONS.map { it.first }, onPick = { key -> onEdit { it.copy(icon = key) } })
        sheet.error?.takeIf { it == LedgerError.ONE_LEVEL_ONLY || it == LedgerError.KIND_MISMATCH }?.let {
            Text(it.message(), style = MaterialTheme.typography.bodySmall, color = colors.expense)
        }
    }
    if (morePlaces) {
        CategoryGridSheet(
            ofKind,
            mains,
            selected = setOfNotNull(sheet.parentId),
            onPick = { id ->
                onEdit { it.copy(parentId = id) }
                morePlaces = false
            },
            onDismiss = { morePlaces = false },
        )
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
    val needsTarget = sheet.movements > 0 || sheet.receivesLoans
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
                if (sheet.receivesLoans) add("Es la categoría de las cuotas de tus préstamos: la que elijas las recibirá desde ahora.")
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
