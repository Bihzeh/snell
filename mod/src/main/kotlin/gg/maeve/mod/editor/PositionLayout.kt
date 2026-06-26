package gg.maeve.mod.editor

/**
 * Layout for the editor's first tier — the position screen reached by Right-Shift. Shows the
 * Maeve wordmark, a centered "Mods" button beneath it (deep-dive into per-module customization),
 * and a bottom-right "Done". Drag happens directly on the HUD elements; no styling here. Pure.
 */
object PositionLayout {
    const val LOGO_SCALE = 2.5f
    private const val LOGO_W = 140
    private const val LOGO_H = 28
    private const val BAR_H = 16
    private const val MODS_W = 96
    private const val DONE_W = 60

    fun logoRect(screenW: Int, screenH: Int): Rect =
        Rect((screenW - LOGO_W) / 2, screenH / 6, LOGO_W, LOGO_H)

    fun modsButton(screenW: Int, screenH: Int): Rect {
        val logo = logoRect(screenW, screenH)
        return Rect((screenW - MODS_W) / 2, logo.bottom + 10, MODS_W, BAR_H)
    }

    fun doneButton(screenW: Int, screenH: Int): Rect =
        Rect(screenW - DONE_W - 6, screenH - BAR_H - 6, DONE_W, BAR_H)
}
