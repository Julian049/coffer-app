package com.dev.julian09.financeapp.ui.components

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.dev.julian09.financeapp.ui.theme.CofferTheme

/**
 * A short list the user orders by holding the handle and dragging (Personalizar inicio, S11-UI04); TalkBack gets
 * "Subir" and "Bajar" on each row instead. [row] receives the handle to place in the row.
 */
@Composable
fun <T> ReorderableColumn(
    items: List<T>,
    key: (T) -> Any,
    onMove: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
    row: @Composable (item: T, handle: @Composable () -> Unit) -> Unit,
) {
    val heights = remember { mutableStateMapOf<Any, Int>() }
    var dragged by remember { mutableStateOf<Any?>(null) }
    var offset by remember { mutableFloatStateOf(0f) }
    val current by rememberUpdatedState(items)
    val move by rememberUpdatedState(onMove)
    Column(modifier) {
        items.forEach { item ->
            val id = key(item)
            key(id) {
                val index = current.indexOfFirst { key(it) == id }
                Column(
                    Modifier
                        .onSizeChanged { heights[id] = it.height }
                        .zIndex(if (dragged == id) 1f else 0f)
                        .graphicsLayer { translationY = if (dragged == id) offset else 0f }
                        .semantics {
                            customActions =
                                listOfNotNull(
                                    CustomAccessibilityAction("Subir") {
                                        move(index, index - 1)
                                        true
                                    }.takeIf { index > 0 },
                                    CustomAccessibilityAction("Bajar") {
                                        move(index, index + 1)
                                        true
                                    }.takeIf { index < current.lastIndex },
                                )
                        },
                ) {
                    row(item) {
                        Icon(
                            Icons.Outlined.DragIndicator,
                            contentDescription = null,
                            tint = CofferTheme.colors.textSecondary,
                            modifier =
                                Modifier.size(24.dp).pointerInput(id) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            dragged = id
                                            offset = 0f
                                        },
                                        onDragEnd = { dragged = null },
                                        onDragCancel = { dragged = null },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            offset += amount.y
                                            val at = current.indexOfFirst { key(it) == id }
                                            val next = current.getOrNull(at + 1)?.let(key)
                                            val previous = current.getOrNull(at - 1)?.let(key)
                                            // Swap once the row passes half of its neighbor, and keep it under the finger
                                            if (next != null && offset > (heights[next] ?: 0) / 2f) {
                                                offset -= heights[next] ?: 0
                                                move(at, at + 1)
                                            } else if (previous != null && offset < -(heights[previous] ?: 0) / 2f) {
                                                offset += heights[previous] ?: 0
                                                move(at, at - 1)
                                            }
                                        },
                                    )
                                },
                        )
                    }
                }
            }
        }
    }
}
