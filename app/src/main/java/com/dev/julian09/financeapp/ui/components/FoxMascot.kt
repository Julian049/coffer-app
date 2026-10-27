package com.dev.julian09.financeapp.ui.components

import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val CANVAS = 1024f
private val ENTER = CubicBezierEasing(0.3f, 1.35f, 0.6f, 1f)
private val POP = CubicBezierEasing(0.3f, 1.6f, 0.6f, 1f)
private val EASE_OUT = CubicBezierEasing(0f, 0f, 0.58f, 1f)
private val EASE_IN = CubicBezierEasing(0.42f, 0f, 1f, 1f)

/** «Zorro de Coffer: saludando» for TalkBack; the layers have none. */
private fun foxDescription(state: String) =
    "Zorro de Coffer: " +
        when (state) {
            "welcome" -> "saludando"
            "resting" -> "durmiendo"
            "good" -> "brindando con un grano de café"
            "goal" -> "celebrando"
            "attentive" -> "atento a un pago"
            "worried" -> "preocupado"
            "debt" -> "agitado"
            else -> "tranquilo"
        }

/** True when the system has animations off: the fox then stands still and whole (README §4.5). */
@Composable
private fun reducedMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember { Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
}

/**
 * The animated fox (docs/08-ui/icons/mascot-animated, FEATURE-09): [state] is a manifest id (`welcome`, `resting`,
 * `good`, `normal`, `goal`, `attentive`, `worried`, `debt`). It bounces in, cross-fades between states and loops its
 * layers; with animations off it shows the still pose.
 */
@Composable
fun FoxMascot(
    state: String,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
) {
    val still = reducedMotion()
    val entered = remember { Animatable(if (still) 1f else 0f) }
    LaunchedEffect(Unit) { if (!still) entered.animateTo(1f, tween(520, easing = ENTER)) }
    Box(
        modifier
            .size(size)
            .graphicsLayer {
                val p = entered.value
                alpha = p.coerceIn(0f, 1f)
                val s = 0.88f + 0.12f * p
                scaleX = s
                scaleY = s
                translationY = (1f - p) * 0.04f * this.size.height
            }.semantics { contentDescription = foxDescription(state) },
    ) {
        AnimatedContent(
            targetState = state,
            transitionSpec = {
                (
                    fadeIn(
                        tween(420, easing = FastOutSlowInEasing),
                    ) + scaleIn(tween(420, easing = FastOutSlowInEasing), initialScale = 0.96f)
                ) togetherWith
                    fadeOut(tween(250))
            },
            label = "fox-state",
        ) { s -> FoxLayers(s, size, still) }
    }
}

/** One value looping over keyframes (fraction of the cycle to value), ease-in-out between them unless told otherwise. */
@Composable
private fun InfiniteTransition.track(
    duration: Int,
    frames: List<Pair<Float, Float>>,
    delay: Int = 0,
    easing: Easing = FastOutSlowInEasing,
    fastForward: Boolean = false,
): State<Float> =
    animateFloat(
        frames.first().second,
        frames.last().second,
        infiniteRepeatable(
            keyframes {
                durationMillis = duration
                frames.forEach { (at, value) -> value at (at * duration).toInt() using easing }
            },
            initialStartOffset = StartOffset(delay, if (fastForward) StartOffsetType.FastForward else StartOffsetType.Delay),
        ),
        label = "fox",
    )

@Composable
private fun FoxLayers(
    state: String,
    size: Dp,
    still: Boolean,
) {
    val layers = FOX_LAYERS[state] ?: FOX_LAYERS.getValue("normal")
    val canvasPx = with(LocalDensity.current) { size.toPx() }
    val t = rememberInfiniteTransition(label = "fox")
    val breathMs =
        when (state) {
            "debt" -> 2300
            "resting" -> 4800
            else -> 3600
        }
    val (bx, by) = if (state == "resting") 1.012f to 1.026f else 1.006f to 1.014f
    val breath = t.track(breathMs, listOf(0f to 0f, 0.5f to 1f, 1f to 0f))
    val swing =
        t.track(
            4500,
            listOf(0f to 0f, 0.72f to 0f, 0.76f to -4f, 0.8f to 3.5f, 0.84f to -2.5f, 0.88f to 1.5f, 0.92f to 0f, 1f to 0f),
            delay = 1600,
        )
    // Groups are drawn where their first layer is, each with its layers in the manifest's order (README §3)
    layers.map { it.group }.distinct().forEach { group ->
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                if (still) return@graphicsLayer
                when (group) {
                    "body" -> {
                        transformOrigin = TransformOrigin(512f / CANVAS, 922f / CANVAS)
                        scaleX = 1f + (bx - 1f) * breath.value
                        scaleY = 1f + (by - 1f) * breath.value
                    }
                    "calendar" -> {
                        transformOrigin = TransformOrigin(835f / CANVAS, 198f / CANVAS)
                        rotationZ = swing.value
                    }
                }
            },
        ) {
            layers.filter { it.group == group }.forEach { LayerImage(it, state, size, canvasPx, t, still) }
        }
    }
}

/** Alpha, scale, rotation and translation of one layer, already in pixels. */
private class Pose(
    val alpha: () -> Float = { 1f },
    val scaleX: () -> Float = { 1f },
    val scaleY: () -> Float = scaleX,
    val rotation: () -> Float = { 0f },
    val dx: () -> Float = { 0f },
    val dy: () -> Float = { 0f },
)

@Composable
private fun LayerImage(
    l: FoxLayer,
    state: String,
    canvas: Dp,
    canvasPx: Float,
    t: InfiniteTransition,
    still: Boolean,
) {
    val pose = if (still) stillPose(l) else animatedPose(l, state, canvasPx, t)
    Image(
        painter = painterResource(l.drawable),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier =
            Modifier
                .offset(canvas * (l.x / CANVAS), canvas * (l.y / CANVAS))
                .size(canvas * (l.w / CANVAS), canvas * (l.h / CANVAS))
                .graphicsLayer {
                    transformOrigin = TransformOrigin(l.pivotX, l.pivotY)
                    alpha = pose.alpha()
                    scaleX = pose.scaleX()
                    scaleY = pose.scaleY()
                    rotationZ = pose.rotation()
                    translationX = pose.dx()
                    translationY = pose.dy()
                },
    )
}

/** README §4.5: every layer at rest; the sweeping days hidden, the due day shown. */
private fun stillPose(l: FoxLayer) = if (l.anim == "daySweep") Pose(alpha = { 0f }) else Pose()

@Composable
private fun animatedPose(
    l: FoxLayer,
    state: String,
    s: Float,
    t: InfiniteTransition,
): Pose =
    when (l.anim) {
        "wave" -> {
            val r = t.track(3800, listOf(0f to 0f, 0.1f to -9f, 0.21f to 2f, 0.32f to -8f, 0.43f to 1f, 0.52f to 0f, 1f to 0f), delay = 500)
            Pose(rotation = { r.value })
        }
        "toast" -> {
            val r = t.track(3600, listOf(0f to 0f, 0.5f to 0f, 0.62f to -8f, 0.7f to -4f, 0.78f to -7f, 0.9f to 0f, 1f to 0f), delay = 600)
            Pose(rotation = { r.value })
        }
        "blink" -> {
            val y = t.track(4600, listOf(0f to 1f, 0.9f to 1f, 0.92f to 0.1f, 0.94f to 1f, 1f to 1f), delay = 1200, easing = LinearEasing)
            Pose(scaleX = { 1f }, scaleY = { y.value })
        }
        "floatZ" -> {
            val delay =
                when (l.layer) {
                    "z2" -> 600
                    "z3" -> 1200
                    else -> 0
                }
            val a = t.track(3600, listOf(0f to 0f, 0.25f to 1f, 0.8f to 1f, 1f to 0f), delay = delay, easing = EASE_OUT)
            // Offset and scale go straight from start to end
            val p = t.track(3600, listOf(0f to 0f, 1f to 1f), delay = delay, easing = LinearEasing)
            Pose(
                alpha = { a.value },
                scaleX = { 0.8f + 0.25f * p.value },
                dx = { 0.015f * s * p.value },
                dy = { (0.02f - 0.07f * p.value) * s },
            )
        }
        "twinkle" -> {
            val second = l.layer == "sparkle2"
            val p = t.track(2200, listOf(0f to 0f, 0.5f to 1f, 1f to 0f), delay = if (second) 1100 else 0, fastForward = second)
            Pose(
                alpha = { 0.55f + 0.45f * p.value },
                scaleX = { 0.82f + 0.24f * p.value },
                rotation = { -6f + 10f * p.value },
            )
        }
        "pulse" -> {
            val ms =
                when (state) {
                    "goal" -> 1600
                    "welcome" -> 1400
                    else -> 2000
                }
            val p = t.track(ms, listOf(0f to 0f, 0.5f to 1f, 1f to 0f))
            Pose(alpha = { 0.35f + 0.65f * p.value }, scaleX = { 0.94f + 0.1f * p.value })
        }
        "drip" -> {
            val a = t.track(3400, listOf(0f to 0f, 0.15f to 0f, 0.3f to 1f, 0.8f to 1f, 1f to 0f), easing = EASE_IN)
            val y = t.track(3400, listOf(0f to -0.01f, 0.15f to -0.01f, 0.3f to 0f, 0.8f to 0.024f, 1f to 0.034f), easing = EASE_IN)
            Pose(alpha = { a.value }, dy = { y.value * s })
        }
        "daySweep" -> {
            val start =
                when (l.layer) {
                    "day1" -> 350
                    "day2" -> 480
                    "day4" -> 610
                    "day5" -> 740
                    else -> 870
                }
            val a = remember { Animatable(0f) }
            LaunchedEffect(Unit) {
                a.animateTo(
                    0f,
                    keyframes {
                        durationMillis = start + 500
                        0f at start
                        1f at start + 225
                        0f at start + 500
                    },
                )
            }
            Pose(alpha = { a.value })
        }
        "dueDay" -> {
            val p = remember { Animatable(0f) }
            LaunchedEffect(Unit) {
                p.animateTo(
                    1f,
                    keyframes {
                        durationMillis = 1100 + 600
                        0f at 1100
                        0.4f at 1100 + 240 using POP
                        1f at 1100 + 600 using POP
                    },
                )
            }
            Pose(
                alpha = { (p.value / 0.4f).coerceIn(0f, 1f) },
                scaleX = { if (p.value <= 0.4f) 1f + 0.18f * (p.value / 0.4f) else 1.18f - 0.18f * ((p.value - 0.4f) / 0.6f) },
            )
        }
        else -> Pose()
    }
