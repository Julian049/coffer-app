package com.dev.julian09.financeapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dev.julian09.financeapp.domain.model.Category
import com.dev.julian09.financeapp.ui.theme.CofferShapes
import com.dev.julian09.financeapp.ui.theme.CofferTheme
import java.util.UUID

/** A category's own icon, or its parent's when it has none (categories are flat outside Categorías and Reportes). */
fun iconOf(
    category: Category,
    all: List<Category>,
) = categoryIcon(category.icon ?: all.firstOrNull { it.id == category.parentId }?.icon)

/**
 * The cell of every icon picker (Figma, S13): a 44 dp circle and its name in two lines at most. Chosen: walnut with a
 * white icon; the rest: clay with a copper icon; «Más»: white with a border. [checked] adds the corner tick of the
 * multiple choice.
 */
@Composable
fun IconBubble(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier.width(72.dp),
    outlined: Boolean = false,
    checked: Boolean = false,
    multiple: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = CofferTheme.colors
    val choose =
        if (multiple) {
            Modifier.toggleable(selected, role = Role.Checkbox, onValueChange = { onClick() })
        } else {
            Modifier.selectable(selected, role = Role.RadioButton, onClick = onClick)
        }
    Column(
        modifier
            .clip(CofferShapes.card)
            .then(choose)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box {
            Box(
                Modifier
                    .size(44.dp)
                    .background(
                        when {
                            selected -> colors.primary
                            outlined -> colors.surface
                            else -> colors.iconContainer
                        },
                        CircleShape,
                    ).then(if (outlined) Modifier.border(1.dp, colors.border, CircleShape) else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (selected) colors.onPrimary else colors.iconOnContainer,
                    modifier = Modifier.size(20.dp),
                )
            }
            if (checked) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-2).dp)
                        .size(18.dp)
                        .background(colors.primary, CircleShape)
                        .border(2.dp, colors.surface, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Check, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(11.dp))
                }
            }
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = if (selected) colors.textPrimary else colors.textSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

/** «Más» of every picker row. */
@Composable
fun MoreBubble(
    modifier: Modifier = Modifier.width(72.dp),
    onClick: () -> Unit,
) = IconBubble("Más", Icons.Outlined.GridView, selected = false, outlined = true, modifier = modifier, onClick = onClick)

/**
 * S15-UI04: the quick row of a picker, five equal cells that share the width (a phone is 360 to 412 dp wide), with «Más»
 * always in the fifth whatever the number of [count] categories before it.
 */
@Composable
fun BubbleRow(
    count: Int,
    onMore: () -> Unit,
    cell: @Composable (index: Int, modifier: Modifier) -> Unit,
) {
    Row(Modifier.fillMaxWidth()) {
        repeat(QUICK_CELLS - 1) { i -> if (i < count) cell(i, Modifier.weight(1f)) else Spacer(Modifier.weight(1f)) }
        MoreBubble(Modifier.weight(1f), onMore)
    }
}

private const val QUICK_CELLS = 5

/** Rows of [columns] cells, the last one padded so every cell keeps its place. */
@Composable
fun <T> BubbleGrid(
    items: List<T>,
    columns: Int = 4,
    cell: @Composable (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.chunked(columns).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                row.forEach { cell(it) }
                repeat(columns - row.size) { Box(Modifier.width(72.dp)) }
            }
        }
    }
}

/**
 * Hoja · Elegir categoría (Figma `111:90`) and Elegir categorías (varias) (`117:245`): a search and a flat grid of four
 * columns, the most used first. With [multiple] a tap marks or unmarks without closing, and Limpiar / Aplicar (n) close
 * it; otherwise a tap picks and closes.
 */
@Composable
fun CategoryGridSheet(
    ranked: List<Category>,
    all: List<Category>,
    selected: Set<UUID>,
    onDismiss: () -> Unit,
    multiple: Boolean = false,
    onPick: (UUID) -> Unit = {},
    onApply: (Set<UUID>) -> Unit = {},
) {
    val colors = CofferTheme.colors
    var query by rememberSaveable { mutableStateOf("") }
    var marked by rememberSaveable { mutableStateOf(selected.map(UUID::toString).toSet()) }
    AppBottomSheet(
        onDismiss = onDismiss,
        title = if (multiple) "Elegir categorías" else "Elegir categoría",
        actions =
            if (multiple) {
                {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AppButton("Limpiar", onClick = { marked = emptySet() }, kind = ButtonKind.Secondary, modifier = Modifier.weight(1f))
                        AppButton(
                            "Aplicar (${marked.size})",
                            onClick = { onApply(marked.map(UUID::fromString).toSet()) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            } else {
                null
            },
    ) {
        AppTextField(value = query, onValueChange = {
            query = it
        }, label = null, placeholder = "Buscar categoría", leadingIcon = Icons.Outlined.Search)
        Text(
            if (multiple) {
                (if (marked.size == 1) "1 seleccionada" else "${marked.size} seleccionadas") + " · más usadas primero"
            } else {
                "Más usadas primero"
            },
            style = MaterialTheme.typography.labelMedium,
            color = colors.textSecondary,
        )
        val text = query.trim()
        BubbleGrid(ranked.filter { text.isEmpty() || it.name.contains(text, ignoreCase = true) }) { c ->
            val on = if (multiple) c.id.toString() in marked else c.id in selected
            IconBubble(c.name, iconOf(c, all), selected = on, checked = multiple && on, multiple = multiple) {
                if (multiple) {
                    val id = c.id.toString()
                    marked = if (id in marked) marked - id else marked + id
                } else {
                    onPick(c.id)
                }
            }
        }
    }
}

/** Hoja · Elegir ícono (Figma `113:90`): every icon by group; a tap picks and closes. */
@Composable
fun IconPickerSheet(
    selected: String?,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = CofferTheme.colors
    AppBottomSheet(onDismiss = onDismiss, title = "Elegir ícono") {
        ICON_GROUPS.forEach { (group, keys) ->
            Text(group, style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
            BubbleGrid(keys, columns = 5) { key ->
                IconCircle(iconByKey(key), selected = key == selected, description = key) { onPick(key) }
            }
        }
    }
}

/** An icon without a label, for the icon rows: the same states as [IconBubble]. */
@Composable
fun IconCircle(
    icon: ImageVector,
    selected: Boolean,
    description: String?,
    outlined: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = CofferTheme.colors
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(
                when {
                    selected -> colors.primary
                    outlined -> colors.surface
                    else -> colors.iconContainer
                },
            ).then(if (outlined) Modifier.border(1.dp, colors.border, CircleShape) else Modifier)
            .selectable(selected, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = if (selected) colors.onPrimary else colors.iconOnContainer,
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * Ícono of Nueva cuenta, Editar cuenta and Nueva categoría (Figma, S13): five icons and «Más», which opens Hoja · Elegir
 * ícono; an icon picked there takes the first place of the row.
 */
@Composable
fun IconRow(
    selected: String,
    defaults: List<String>,
    onPick: (String) -> Unit,
) {
    var picking by rememberSaveable { mutableStateOf(false) }
    val shown = (listOf(selected) + defaults).distinct().take(5)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        shown.forEach { key -> IconCircle(iconByKey(key), selected = key == selected, description = key) { onPick(key) } }
        IconCircle(Icons.Outlined.GridView, selected = false, description = "Más íconos", outlined = true) { picking = true }
    }
    if (picking) {
        IconPickerSheet(
            selected,
            onPick = {
                onPick(it)
                picking = false
            },
            onDismiss = { picking = false },
        )
    }
}
