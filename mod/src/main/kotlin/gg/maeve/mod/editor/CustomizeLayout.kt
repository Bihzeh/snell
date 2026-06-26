package gg.maeve.mod.editor

import gg.maeve.shared.MaevePalette

/**
 * Layout for the editor's third tier — the centered customization popup for one module, reached
 * by clicking a card in the grid. HUD modules get the full style controls (a two-column popup:
 * HSV colour picker on the left; enable + style toggles + scale + reset + swatches on the right).
 * Non-HUD modules (e.g. the font) get a single enable toggle. Control ids match EditorState and
 * EditorRenderer so geometry and behaviour never drift. Pure.
 */
object CustomizeLayout {
    const val W = 300
    const val NONHUD_W = 184
    private const val PAD = 10
    private const val TITLE_H = 20
    private const val SV = 100   // SV square side
    private const val BAR = 10   // hue / alpha bar width
    private const val GAP = 12   // gap between the two columns
    private const val ROW_H = 14
    private const val ROW_GAP = 3

    /** Preset colours: launcher palette plus a few vivid Minecraft-friendly tones. */
    val SWATCHES = intArrayOf(
        MaevePalette.text, MaevePalette.gold, MaevePalette.primary, MaevePalette.success, MaevePalette.error,
        0xFFFFFFFF.toInt(), 0xFFFF5555.toInt(), 0xFF55FF55.toInt(), 0xFF55FFFF.toInt(), 0xFFFFFF55.toInt(),
    )

    /** Style toggles shown beneath the enable row (the enable row uses the "visible" control id). */
    val TOGGLES = listOf("bold", "italic", "underline", "strike", "shadow", "background")

    private fun hudHeight(): Int {
        val rightRows = 1 + TOGGLES.size + 1 + 1 // visible + toggles + scale row + reset
        val swatchRows = (SWATCHES.size + 4) / 5
        val rightH = rightRows * (ROW_H + ROW_GAP) + swatchRows * (13 + 4)
        val leftH = 12 + 4 + SV + 6 + 12 // preview + sv + hex
        return TITLE_H + maxOf(rightH, leftH) + PAD
    }

    private fun nonHudHeight(): Int = TITLE_H + ROW_H + PAD * 2

    fun popupRect(screenW: Int, screenH: Int, isHud: Boolean): Rect {
        val w = if (isHud) W else NONHUD_W
        val h = if (isHud) hudHeight() else nonHudHeight()
        return Rect((screenW - w) / 2, ((screenH - h) / 2).coerceAtLeast(0), w, h)
    }

    fun closeButton(popup: Rect): Rect = Rect(popup.right - PAD - 12, popup.top + 4, 12, 12)

    /** Non-HUD modules: a single full-width enable toggle. */
    fun enableToggle(popup: Rect): Rect = Rect(popup.left + PAD, popup.top + TITLE_H, popup.width - 2 * PAD, ROW_H)

    /** HUD style controls (two columns). Same ids the single-panel layout used. */
    fun controls(popup: Rect): List<Control> {
        val out = mutableListOf<Control>()
        val top = popup.top + TITLE_H
        val leftX = popup.left + PAD
        val colW = (popup.width - 2 * PAD - GAP) / 2
        val rightX = leftX + colW + GAP

        // Left column: colour picker.
        out += Control("preview", Rect(leftX + colW - 30, top, 30, 12))
        val svTop = top + 16
        out += Control("sv", Rect(leftX, svTop, SV, SV))
        out += Control("hue", Rect(leftX + SV + 4, svTop, BAR, SV))
        out += Control("alpha", Rect(leftX + SV + 4 + BAR + 2, svTop, BAR, SV))
        out += Control("hex", Rect(leftX, svTop + SV + 6, colW, 12))

        // Right column: enable + style toggles + scale + reset + swatches.
        var y = top
        out += Control("visible", Rect(rightX, y, colW, ROW_H)); y += ROW_H + ROW_GAP
        for (id in TOGGLES) { out += Control(id, Rect(rightX, y, colW, ROW_H)); y += ROW_H + ROW_GAP }
        out += Control("scale-", Rect(rightX, y, 22, ROW_H))
        out += Control("scale+", Rect(rightX + colW - 22, y, 22, ROW_H)); y += ROW_H + ROW_GAP
        out += Control("reset", Rect(rightX, y, colW, ROW_H)); y += ROW_H + ROW_GAP
        val sw = 13; val gap = 4; val perRow = 5
        SWATCHES.forEachIndexed { i, _ ->
            val col = i % perRow; val r = i / perRow
            out += Control("swatch:$i", Rect(rightX + col * (sw + gap), y + r * (sw + gap), sw, sw))
        }
        return out
    }

    fun controlRect(popup: Rect, id: String): Rect? = controls(popup).firstOrNull { it.id == id }?.rect
}
