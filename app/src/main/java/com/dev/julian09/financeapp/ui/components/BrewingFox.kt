package com.dev.julian09.financeapp.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dev.julian09.financeapp.R
import kotlin.math.PI
import kotlin.math.sin

private const val CANVAS = 1024f

/** Piecewise-linear value of the keyframes [k] (time to value, ascending) at [t]. */
internal fun keys(
    t: Float,
    vararg k: Pair<Float, Float>,
): Float {
    for (i in 1 until k.size) {
        if (t <= k[i].first) {
            val (t0, v0) = k[i - 1]
            val (t1, v1) = k[i]
            return if (t1 == t0) v1 else v0 + (v1 - v0) * (t - t0) / (t1 - t0)
        }
    }
    return k.last().second
}

/** Top of the coffee on the 1024 canvas for a download [progress] 0–1 (mascot-animated v2-2 §1.1). */
internal fun coffeeLevel(progress: Float): Float = 884f - (884f - 722f) * progress.coerceIn(0f, 1f)

/** The crema narrows with the glass: X scale around x = 842 at the given [level]. */
internal fun cremaScaleX(level: Float): Float = (144f - 20f * (level - 712f) / 150f) / 144f

@Composable
private fun loop(
    ms: Int,
    delayMs: Int = 0,
    still: Boolean,
): State<Float> {
    if (still) return remember { mutableFloatStateOf(0f) }
    return rememberInfiniteTransition(label = "brewing").animateFloat(
        0f,
        1f,
        infiniteRepeatable(tween(ms, easing = LinearEasing), initialStartOffset = StartOffset(delayMs)),
        label = "loop",
    )
}

/**
 * The `brewing` fox of «Descargando tus datos» (2.2): the glass fills with the real [progress] (0–1, null = unknown).
 * Layers and times: docs/08-ui/icons/mascot-animated/v2-2/README.md §1.1. Reduced motion keeps the cup level but nothing moves.
 */
@Composable
fun BrewingFox(
    progress: Float?,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
) {
    val still = reducedMotion()
    val s = size.value / CANVAS
    val level = coffeeLevel(progress ?: 0f)
    val breath by loop(3600, still = still)
    val head by loop(7000, still = still)
    val ear by loop(5200, 1500, still)
    val leavesL by loop(4200, still = still)
    val leavesR by loop(3800, 900, still)
    val blink by loop(4600, 1200, still)
    val steam = listOf(loop(2800, 0, still), loop(2800, 930, still), loop(2800, 1860, still))

    // Once, at 100 %: the head nods and the little lines beat
    val celebration = remember { Animatable(0f) }
    val done = (progress ?: 0f) >= 1f
    LaunchedEffect(done, still) {
        if (done && !still) celebration.animateTo(1f, tween(1600, easing = LinearEasing))
    }
    val c = celebration.value

    @Composable
    fun Layer(
        res: Int,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        modifier: Modifier = Modifier,
    ) = Image(
        painterResource(res),
        contentDescription = null,
        modifier = Modifier.offset((x * s).dp, (y * s).dp).size((w * s).dp, (h * s).dp).then(modifier),
    )

    Box(modifier.size(size).semantics { contentDescription = "Zorro de Coffer: esperando el café" }.clearAndSetSemantics { }) {
        Layer(
            R.drawable.fox_brewing_leaves_l,
            121,
            466,
            302,
            302,
            Modifier.graphicsLayer {
                rotationZ = keys(leavesL, 0f to -1.5f, .5f to 2f, 1f to -1.5f)
                transformOrigin = TransformOrigin(.560f, .894f)
            },
        )
        Layer(
            R.drawable.fox_brewing_leaves_r,
            720,
            638,
            183,
            236,
            Modifier.graphicsLayer {
                rotationZ = keys(leavesR, 0f to 1.5f, .5f to -2f, 1f to 1.5f)
                transformOrigin = TransformOrigin(.383f, .983f)
            },
        )
        // torso group: only breathes, in Y, pivoting on the floor line so the floor never moves
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                scaleY = keys(breath, 0f to 1f, .5f to 1.014f, 1f to 1f)
                transformOrigin = TransformOrigin(.5f, 900f / CANVAS)
            },
        ) {
            Layer(R.drawable.fox_brewing_torso, 121, 91, 782, 841)
            // head group: turns on the neck (600, 465)
            Box(
                Modifier.fillMaxSize().graphicsLayer {
                    rotationZ =
                        keys(
                            head,
                            0f to 0f,
                            .20f to 0f,
                            .27f to -1.5f,
                            .36f to -1.5f,
                            .44f to 0f,
                            .52f to 0f,
                            .60f to 4f,
                            .74f to 4f,
                            .78f to 2.8f,
                            .82f to 4.2f,
                            .90f to 0f,
                            1f to 0f,
                        ) + keys(c, 0f to 0f, .15f to -4f, .30f to 1f, .45f to -3f, .60f to 0f, 1f to 0f)
                    transformOrigin = TransformOrigin(600f / CANVAS, 465f / CANVAS)
                },
            ) {
                Layer(
                    R.drawable.fox_brewing_ear,
                    462,
                    91,
                    149,
                    186,
                    Modifier.graphicsLayer {
                        rotationZ = keys(ear, 0f to 0f, .70f to 0f, .72f to -9f, .745f to 2f, .77f to -7f, .80f to 0f, 1f to 0f)
                        transformOrigin = TransformOrigin(.523f, .855f)
                    },
                )
                Layer(R.drawable.fox_brewing_head, 373, 91, 480, 411)
                Layer(
                    R.drawable.fox_brewing_lid,
                    636,
                    288,
                    59,
                    61,
                    Modifier.graphicsLayer {
                        scaleY = keys(blink, 0f to 0f, .90f to 0f, .92f to 1f, .94f to 0f, 1f to 0f)
                        transformOrigin = TransformOrigin(.5f, 0f)
                    },
                )
            }
        }
        Layer(R.drawable.fox_brewing_cup_back, 735, 709, 226, 194)
        Layer(
            R.drawable.fox_brewing_coffee,
            767,
            709,
            150,
            178,
            // Only the part below the level shows
            Modifier.drawWithContent {
                val top = (level - 709f) / 178f * this.size.height
                clipRect(top = top) { this@drawWithContent.drawContent() }
            },
        )
        if ((progress ?: 0f) >= 0.03f) {
            Layer(
                R.drawable.fox_brewing_crema,
                767,
                (level - 2f).toInt(),
                150,
                22,
                Modifier.graphicsLayer { scaleX = cremaScaleX(level) },
            )
        }
        Layer(R.drawable.fox_brewing_cup_front, 764, 706, 156, 184)
        // Steam from 25 % on: three threads, 2800 ms each, 930 ms apart
        if ((progress ?: 0f) > 0.25f) {
            val res = listOf(R.drawable.fox_brewing_steam1, R.drawable.fox_brewing_steam2, R.drawable.fox_brewing_steam3)
            val xs = listOf(787, 829, 871)
            val ys = listOf(556, 550, 556)
            res.indices.forEach { i ->
                val t = steam[i].value
                Layer(
                    res[i],
                    xs[i],
                    ys[i],
                    26,
                    142,
                    Modifier.graphicsLayer {
                        alpha = if (still) 0.6f else keys(t, 0f to 0f, .25f to .85f, .75f to .6f, 1f to 0f)
                        translationY = -60f * t * s * density
                        translationX = sin(2.0 * PI * t).toFloat() * 6f * s * density
                    },
                )
            }
        }
        if (c > 0f && c < 1f) {
            Layer(
                R.drawable.fox_good_lines,
                842,
                150,
                112,
                101,
                Modifier.graphicsLayer { alpha = keys(c, 0f to 0f, .25f to 1f, .5f to .4f, .7f to 1f, 1f to 0f) },
            )
        }
    }
}
