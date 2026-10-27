package com.dev.julian09.financeapp.ui.components

import com.dev.julian09.financeapp.R

// Generated from docs/08-ui/icons/mascot-animated/manifest.json (1024 px canvas); regenerate it if the art changes.

/** One layer of the animated fox: its box and pivot on the 1024 px canvas, its group and its own animation. */
data class FoxLayer(
    val layer: String,
    val drawable: Int,
    val group: String,
    val x: Int,
    val y: Int,
    val w: Int,
    val h: Int,
    val pivotX: Float,
    val pivotY: Float,
    val anim: String?,
)

/** Layers per state in drawing order, back to front (mascot-animated README §3). */
val FOX_LAYERS: Map<String, List<FoxLayer>> =
    mapOf(
        "attentive" to
            listOf(
                FoxLayer("fox", R.drawable.fox_attentive_fox, "body", 56, 138, 660, 773, 0.5f, 0.5f, null),
                FoxLayer("calendar", R.drawable.fox_attentive_calendar, "calendar", 702, 198, 266, 291, 0.5f, 0.5f, null),
                FoxLayer("lines", R.drawable.fox_attentive_lines, "fx", 522, 113, 109, 97, 0.5f, 0.5f, "pulse"),
                FoxLayer("day1", R.drawable.fox_attentive_day1, "calendar", 744, 319, 51, 54, 0.5f, 0.5f, "daySweep"),
                FoxLayer("day2", R.drawable.fox_attentive_day2, "calendar", 809, 319, 51, 54, 0.5f, 0.5f, "daySweep"),
                FoxLayer("day3", R.drawable.fox_attentive_day3, "calendar", 874, 319, 51, 54, 0.5f, 0.5f, "dueDay"),
                FoxLayer("day4", R.drawable.fox_attentive_day4, "calendar", 744, 386, 51, 54, 0.5f, 0.5f, "daySweep"),
                FoxLayer("day5", R.drawable.fox_attentive_day5, "calendar", 809, 386, 51, 54, 0.5f, 0.5f, "daySweep"),
                FoxLayer("day6", R.drawable.fox_attentive_day6, "calendar", 874, 386, 51, 54, 0.5f, 0.5f, "daySweep"),
            ),
        "debt" to
            listOf(
                FoxLayer("fox", R.drawable.fox_debt_fox, "body", 93, 56, 838, 912, 0.5f, 0.5f, null),
            ),
        "goal" to
            listOf(
                FoxLayer("fox", R.drawable.fox_goal_fox, "body", 103, 115, 756, 816, 0.5f, 0.5f, null),
                FoxLayer("sparkle1", R.drawable.fox_goal_sparkle1, "fx", 662, 92, 119, 128, 0.5f, 0.5f, "twinkle"),
                FoxLayer("sparkle2", R.drawable.fox_goal_sparkle2, "fx", 766, 438, 107, 121, 0.5f, 0.5f, "twinkle"),
                FoxLayer("lines", R.drawable.fox_goal_lines, "fx", 121, 153, 800, 219, 0.5f, 0.5f, "pulse"),
            ),
        "good" to
            listOf(
                FoxLayer("arm", R.drawable.fox_good_arm, "body", 644, 331, 278, 312, 0.0863f, 0.734f, "toast"),
                FoxLayer("fox", R.drawable.fox_good_fox, "body", 102, 115, 820, 794, 0.5f, 0.5f, null),
                FoxLayer("lines", R.drawable.fox_good_lines, "fx", 763, 137, 112, 101, 0.5f, 0.5f, "pulse"),
            ),
        "normal" to
            listOf(
                FoxLayer("fox", R.drawable.fox_normal_fox, "body", 121, 91, 782, 841, 0.5f, 0.5f, null),
            ),
        "resting" to
            listOf(
                FoxLayer("fox", R.drawable.fox_resting_fox, "body", 81, 323, 862, 506, 0.5f, 0.5f, null),
                FoxLayer("z1", R.drawable.fox_resting_z1, "fx", 670, 328, 39, 43, 0.5f, 0.5f, "floatZ"),
                FoxLayer("z2", R.drawable.fox_resting_z2, "fx", 724, 261, 52, 60, 0.5f, 0.5f, "floatZ"),
                FoxLayer("z3", R.drawable.fox_resting_z3, "fx", 793, 194, 56, 61, 0.5f, 0.5f, "floatZ"),
            ),
        "welcome" to
            listOf(
                FoxLayer("arm", R.drawable.fox_welcome_arm, "body", 622, 352, 230, 229, 0.1435f, 0.7118f, "wave"),
                FoxLayer("fox", R.drawable.fox_welcome_fox, "body", 104, 95, 750, 833, 0.5f, 0.5f, null),
                FoxLayer("lines", R.drawable.fox_welcome_lines, "fx", 834, 261, 85, 82, 0.5f, 0.5f, "pulse"),
                FoxLayer("eye_l", R.drawable.fox_welcome_eye_l, "body", 476, 326, 45, 55, 0.5f, 0.5f, "blink"),
                FoxLayer("eye_r", R.drawable.fox_welcome_eye_r, "body", 596, 302, 45, 54, 0.5f, 0.5f, "blink"),
            ),
        "worried" to
            listOf(
                FoxLayer("fox", R.drawable.fox_worried_fox, "body", 103, 56, 818, 912, 0.5f, 0.5f, null),
                FoxLayer("drop", R.drawable.fox_worried_drop, "fx", 406, 302, 34, 59, 0.5f, 0.5f, "drip"),
            ),
    )
