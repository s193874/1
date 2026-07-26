package com.playtranslate.ui

import android.content.Context
import android.graphics.Color
import com.playtranslate.Prefs
import kotlin.math.roundToInt

/**
 * User-selectable background style for the translation overlay boxes.
 *
 * ADAPTIVE keeps upstream behaviour: each box samples the game content
 * behind it so the overlay blends in. DARK / LIGHT force a uniform card
 * colour (with a matching readable text colour) for users who prefer a
 * consistent look over blending.
 */
enum class OverlayBoxColorMode(val storageKey: String) {
    ADAPTIVE("adaptive"),
    DARK("dark"),
    LIGHT("light");

    companion object {
        fun fromKey(key: String?): OverlayBoxColorMode =
            values().firstOrNull { it.storageKey == key } ?: ADAPTIVE
    }
}

/**
 * Applies the user's overlay-box appearance settings (colour mode +
 * background opacity) to a freshly built [TextBox] list, before
 * [TranslationOverlayView.setBoxes] stores and renders it.
 *
 * Mapping the boxes here — rather than at each paint site — keeps every
 * consumer (skeleton bars, bilingual notes, pinhole diffing, the
 * `tag_bg_color` view tags) consistent for free. Pinhole change-detection
 * is invariant under this transform: it compares against what was actually
 * rendered, and the same transform is applied on every cycle.
 */
object OverlayBoxStyle {

    fun applyTo(context: Context, boxes: List<TextBox>): List<TextBox> {
        if (boxes.isEmpty()) return boxes
        val prefs = Prefs(context)
        val mode = prefs.overlayBoxColorMode
        val opacity = prefs.overlayBoxOpacity
        val opacityIsDefault = opacity == Prefs.DEFAULT_OVERLAY_BOX_OPACITY
        if (mode == OverlayBoxColorMode.ADAPTIVE && opacityIsDefault) return boxes

        val alpha = (opacity * 255f).roundToInt().coerceIn(0, 255)
        return boxes.map { box ->
            // Furigana pills carry their own compact styling — leave them be.
            if (box.isFurigana) return@map box
            when (mode) {
                OverlayBoxColorMode.ADAPTIVE -> box.copy(
                    bgColor = withAlpha(box.bgColor, alpha),
                )
                OverlayBoxColorMode.DARK -> box.copy(
                    bgColor = Color.argb(alpha, 0x15, 0x18, 0x1B),
                    textColor = Color.WHITE,
                )
                OverlayBoxColorMode.LIGHT -> box.copy(
                    bgColor = Color.argb(alpha, 0xF2, 0xF3, 0xF5),
                    textColor = Color.BLACK,
                )
            }
        }
    }

    private fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
}
