package gg.snell.mod.editor

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Pure color math for the HUD editor: HSV<->RGB and ARGB pack/unpack. No Minecraft types. */
object SnellColor {
    /** [h] in degrees, [s]/[v] in 0..1 -> 0xRRGGBB (no alpha). */
    fun hsvToRgb(h: Float, s: Float, v: Float): Int {
        val hue = (((h % 360f) + 360f) % 360f) / 60f
        val sat = s.coerceIn(0f, 1f)
        val value = v.coerceIn(0f, 1f)
        val c = value * sat
        val x = c * (1f - abs(hue % 2f - 1f))
        val m = value - c
        val (r, g, b) = when (hue.toInt()) {
            0 -> Triple(c, x, 0f)
            1 -> Triple(x, c, 0f)
            2 -> Triple(0f, c, x)
            3 -> Triple(0f, x, c)
            4 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        return (comp(r + m) shl 16) or (comp(g + m) shl 8) or comp(b + m)
    }

    private fun comp(f: Float): Int = (f * 255f).roundToInt().coerceIn(0, 255)

    /** 0xRRGGBB (alpha ignored) -> Triple(h in 0..360, s in 0..1, v in 0..1). */
    fun rgbToHsv(rgb: Int): Triple<Float, Float, Float> {
        val r = ((rgb shr 16) and 0xFF) / 255f
        val g = ((rgb shr 8) and 0xFF) / 255f
        val b = (rgb and 0xFF) / 255f
        val mx = max(r, max(g, b))
        val mn = min(r, min(g, b))
        val d = mx - mn
        val h = when {
            d == 0f -> 0f
            mx == r -> 60f * (((g - b) / d) % 6f)
            mx == g -> 60f * (((b - r) / d) + 2f)
            else -> 60f * (((r - g) / d) + 4f)
        }.let { if (it < 0f) it + 360f else it }
        val s = if (mx == 0f) 0f else d / mx
        return Triple(h, s, mx)
    }

    fun argb(alpha: Int, rgb: Int): Int = ((alpha and 0xFF) shl 24) or (rgb and 0xFFFFFF)
    fun alphaOf(argb: Int): Int = (argb ushr 24) and 0xFF
    fun rgbOf(argb: Int): Int = argb and 0xFFFFFF
}
