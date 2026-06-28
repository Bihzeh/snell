package gg.snell.mod.editor

import gg.snell.shared.SnellPalette

/**
 * Layout for the editor's customization popup. Two balanced columns under a header.
 * LEFT: the HSV picker + hex + swatches, then the COLOUR section (target chips — one per editable
 * colour). RIGHT: the OPTIONS section (the module's toggles) and the STYLE section (enable/bold/
 * italic/scale/reset). The single picker edits whichever colour target is selected. All geometry is
 * shared by EditorState (hit-test) and EditorRenderer (draw) so they never drift. Pure.
 */
object CustomizeLayout {
    const val W = 320
    const val NONHUD_W = 200
    const val PAD = 12
    const val TITLE_H = 24
    private const val SV = 104
    private const val BAR = 11
    private const val GAP = 16
    const val ROW_H = 16
    const val ROW_GAP = 4
    const val SECTION_CAP = 13
    private const val SECTION_GAP = 8
    private const val LEFT_W = SV + 5 + BAR + 3 + BAR // picker / chip column width (134)
    private const val SW = 14 // swatch size

    val SWATCHES = intArrayOf(
        SnellPalette.text, SnellPalette.gold, SnellPalette.primary, SnellPalette.success, SnellPalette.error,
        0xFFFFFFFF.toInt(), 0xFFFF5555.toInt(), 0xFF55FF55.toInt(), 0xFF55FFFF.toInt(), 0xFFFFFF55.toInt(),
    )
    val TOGGLES = listOf("bold", "italic")

    private fun step() = ROW_H + ROW_GAP
    private fun leftX(popup: Rect) = popup.left + PAD
    private fun rightX(popup: Rect) = popup.left + PAD + LEFT_W + GAP
    private fun rightW(popup: Rect) = popup.right - PAD - rightX(popup)
    private fun svTop(popup: Rect) = popup.top + TITLE_H + 4
    private fun swatchTop(popup: Rect) = svTop(popup) + SV + 6 + ROW_H + 6
    private fun swatchBottom(popup: Rect) = swatchTop(popup) + ((SWATCHES.size + 4) / 5) * (SW + 4)

    private fun colourCapY(popup: Rect) = swatchBottom(popup) + SECTION_GAP
    private fun optionsCapY(popup: Rect) = popup.top + TITLE_H + 4
    private fun styleCapY(popup: Rect, oc: Int): Int {
        val o = optionsCapY(popup)
        return if (oc > 0) o + SECTION_CAP + oc * step() + SECTION_GAP else o
    }

    private fun hudHeight(tc: Int, oc: Int): Int {
        val p = Rect(0, 0, W, 0) // origin popup to measure from
        val leftBottom = if (tc > 0) colourCapY(p) + SECTION_CAP + tc * step() else swatchBottom(p)
        val rightBottom = styleCapY(p, oc) + SECTION_CAP + 5 * step() // visible + bold + italic + scale + reset
        return maxOf(leftBottom, rightBottom) + PAD
    }

    private fun nonHudHeight() = TITLE_H + ROW_H + PAD * 2

    fun popupRect(screenW: Int, screenH: Int, isHud: Boolean, targetCount: Int = 0, optionCount: Int = 0): Rect {
        val w = if (isHud) W else NONHUD_W
        val h = if (isHud) hudHeight(targetCount, optionCount) else nonHudHeight()
        return Rect((screenW - w) / 2, ((screenH - h) / 2).coerceAtLeast(0), w, h)
    }

    fun closeButton(popup: Rect): Rect = Rect(popup.right - PAD - 14, popup.top + 5, 14, 14)
    fun enableToggle(popup: Rect): Rect = Rect(popup.left + PAD, popup.top + TITLE_H, popup.width - 2 * PAD, ROW_H)

    /** Section caption rects: COLOUR (left, under the swatches), OPTIONS + STYLE (right). */
    fun captions(popup: Rect, tc: Int, oc: Int): Triple<Rect, Rect, Rect> =
        Triple(
            Rect(leftX(popup), colourCapY(popup), LEFT_W, SECTION_CAP),
            Rect(rightX(popup), optionsCapY(popup), rightW(popup), SECTION_CAP),
            Rect(rightX(popup), styleCapY(popup, oc), rightW(popup), SECTION_CAP),
        )

    /** Colour-target chips, in the LEFT column beneath the swatches. */
    fun targetChips(popup: Rect, count: Int): List<Rect> {
        val top = colourCapY(popup) + SECTION_CAP
        return (0 until count).map { Rect(leftX(popup), top + it * step(), LEFT_W, ROW_H) }
    }

    /** Module option switch rows, in the RIGHT column. */
    fun optionRows(popup: Rect, targetCount: Int, count: Int): List<Rect> {
        val top = optionsCapY(popup) + SECTION_CAP
        return (0 until count).map { Rect(rightX(popup), top + it * step(), rightW(popup), ROW_H) }
    }

    /** Picker + swatches (left) and the STYLE-section controls (right). */
    fun controls(popup: Rect, targetCount: Int, optionCount: Int): List<Control> {
        val out = mutableListOf<Control>()
        val l = leftX(popup); val sv = svTop(popup)
        out += Control("sv", Rect(l, sv, SV, SV))
        out += Control("hue", Rect(l + SV + 5, sv, BAR, SV))
        out += Control("alpha", Rect(l + SV + 5 + BAR + 3, sv, BAR, SV))
        out += Control("hex", Rect(l, sv + SV + 6, LEFT_W, ROW_H))
        val perRow = 5; val swTop = swatchTop(popup)
        SWATCHES.forEachIndexed { i, _ ->
            val col = i % perRow; val r = i / perRow
            out += Control("swatch:$i", Rect(l + col * (SW + 4), swTop + r * (SW + 4), SW, SW))
        }
        val rx = rightX(popup); val rw = rightW(popup)
        var y = styleCapY(popup, optionCount) + SECTION_CAP
        out += Control("visible", Rect(rx, y, rw, ROW_H)); y += step()
        for (id in TOGGLES) { out += Control(id, Rect(rx, y, rw, ROW_H)); y += step() }
        out += Control("scale-", Rect(rx, y, 24, ROW_H))
        out += Control("scale+", Rect(rx + rw - 24, y, 24, ROW_H)); y += step()
        out += Control("reset", Rect(rx, y, rw, ROW_H))
        return out
    }

    fun controlRect(popup: Rect, id: String, targetCount: Int, optionCount: Int): Rect? =
        controls(popup, targetCount, optionCount).firstOrNull { it.id == id }?.rect
}
