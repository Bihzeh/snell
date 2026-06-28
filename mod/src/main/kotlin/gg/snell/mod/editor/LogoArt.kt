package gg.snell.mod.editor

import gg.snell.shared.SnellPalette

/** A horizontal fill band of the logo mark (rect + ARGB color). */
data class LogoBand(val rect: Rect, val color: Int)

/**
 * Pure geometry for the Snell brand mark — a faceted purple gem under a 3-point gold crown —
 * emitted as horizontal fill bands so it needs no texture/sprite primitive and renders identically
 * in-game (extractor canvas) and in the headless preview (Java2D). Symmetric about the area's
 * vertical centre by construction; always fits inside [area].
 */
object LogoArt {
    private val light = 0xFFB9A6FF.toInt() // top facet
    private val deep = 0xFF5B3FD9.toInt()  // bottom point

    fun bands(area: Rect): List<LogoBand> {
        val out = mutableListOf<LogoBand>()
        val cx = area.left + area.width / 2
        val crownH = (area.height * 0.32f).toInt().coerceAtLeast(4)
        val gemTop = area.top + crownH + 2
        val gemBottom = area.bottom
        val gemH = (gemBottom - gemTop).coerceAtLeast(6)
        val gemHalf = (area.width * 0.34f).toInt().coerceAtLeast(3)
        val girdleY = gemTop + (gemH * 0.42f).toInt()

        // Gem: 1px rows widening to the girdle, then narrowing to the bottom point.
        for (i in 0 until gemH) {
            val y = gemTop + i
            val half = if (y <= girdleY)
                lerp(2, gemHalf, (y - gemTop).toFloat() / (girdleY - gemTop).coerceAtLeast(1))
            else
                lerp(gemHalf, 0, (y - girdleY).toFloat() / (gemBottom - 1 - girdleY).coerceAtLeast(1))
            if (half <= 0) continue
            out += LogoBand(Rect(cx - half, y, half * 2, 1), purple(i.toFloat() / gemH))
        }

        // Crown: a thin gold base band + three narrowing points (mirror-symmetric).
        val bandY = gemTop - 3
        out += LogoBand(Rect(cx - gemHalf, bandY, gemHalf * 2, 2), SnellPalette.gold)
        val spread = (gemHalf * 0.66f).toInt()
        val points = intArrayOf(cx - spread, cx, cx + spread)
        val pointH = (crownH - 2).coerceAtLeast(2)
        for ((idx, px) in points.withIndex()) {
            val h = if (idx == 1) pointH else (pointH * 0.72f).toInt().coerceAtLeast(1)
            for (j in 0 until h) {
                val w = lerp(5, 1, j.toFloat() / h).coerceAtLeast(1)
                out += LogoBand(Rect(px - w / 2, bandY - 1 - j, w, 1), SnellPalette.gold)
            }
        }
        return out
    }

    private fun lerp(a: Int, b: Int, f: Float): Int = (a + (b - a) * f.coerceIn(0f, 1f)).toInt()

    private fun purple(t: Float): Int {
        fun ch(shift: Int): Int {
            val a = (light ushr shift) and 0xFF; val b = (deep ushr shift) and 0xFF
            return (a + ((b - a) * t).toInt()).coerceIn(0, 255)
        }
        return (0xFF shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }
}
