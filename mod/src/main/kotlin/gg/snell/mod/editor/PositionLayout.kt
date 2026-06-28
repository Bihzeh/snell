package gg.snell.mod.editor

/**
 * Layout for the editor's first tier — the position screen reached by Right-Shift. Shows the
 * Snell wordmark, a centered "Mods" button beneath it (deep-dive into per-module customization),
 * and a bottom-right "Done". Drag happens directly on the HUD elements; no styling here. Pure.
 */
object PositionLayout {
    const val LOGO_SCALE = 2.5f
    private const val LOGO_W = 84
    private const val LOGO_H = 76
    private const val BAR_H = 16
    private const val MODS_W = 96
    private const val DONE_W = 60
    private const val SNAP_W = 84
    const val HANDLE = 10

    fun logoRect(screenW: Int, screenH: Int): Rect =
        Rect((screenW - LOGO_W) / 2, screenH / 6, LOGO_W, LOGO_H)

    fun modsButton(screenW: Int, screenH: Int): Rect {
        val logo = logoRect(screenW, screenH)
        return Rect((screenW - MODS_W) / 2, logo.bottom + 10, MODS_W, BAR_H)
    }

    fun doneButton(screenW: Int, screenH: Int): Rect =
        Rect(screenW - DONE_W - 6, screenH - BAR_H - 6, DONE_W, BAR_H)

    /** Bottom-left snap-mode toggle (mirrors [doneButton] on the right). */
    fun snapButton(screenW: Int, screenH: Int): Rect = Rect(6, screenH - BAR_H - 6, SNAP_W, BAR_H)

    /** Resize grip at a selected element's bottom-right corner. */
    fun resizeHandle(box: Rect): Rect = Rect(box.right - HANDLE, box.bottom - HANDLE, HANDLE, HANDLE)
}
