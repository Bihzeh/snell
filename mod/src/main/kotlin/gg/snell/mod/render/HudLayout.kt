package gg.snell.mod.render

import gg.snell.mod.module.HudAnchor
import gg.snell.mod.module.TextAlign

/** Pure HUD geometry shared by the renderer and the editor (no Minecraft types). */
object HudLayout {
    data class Rect(val left: Int, val top: Int, val width: Int, val height: Int)

    /**
     * Resolves the top-left pixel of a [blockW]x[blockH] element pinned to [anchor] with
     * inward [offsetX]/[offsetY], on a [screenW]x[screenH] scaled-GUI surface. For the
     * centered anchors the offsets act as a fine nudge from the centered position.
     */
    fun resolveTopLeft(
        anchor: HudAnchor, offsetX: Int, offsetY: Int,
        blockW: Int, blockH: Int, screenW: Int, screenH: Int,
    ): Pair<Int, Int> {
        val left = when (anchor) {
            HudAnchor.TOP_LEFT, HudAnchor.MID_LEFT, HudAnchor.BOTTOM_LEFT -> offsetX
            HudAnchor.TOP_CENTER, HudAnchor.CENTER, HudAnchor.BOTTOM_CENTER -> (screenW - blockW) / 2 + offsetX
            HudAnchor.TOP_RIGHT, HudAnchor.MID_RIGHT, HudAnchor.BOTTOM_RIGHT -> screenW - blockW - offsetX
        }
        val top = when (anchor) {
            HudAnchor.TOP_LEFT, HudAnchor.TOP_CENTER, HudAnchor.TOP_RIGHT -> offsetY
            HudAnchor.MID_LEFT, HudAnchor.CENTER, HudAnchor.MID_RIGHT -> (screenH - blockH) / 2 + offsetY
            HudAnchor.BOTTOM_LEFT, HudAnchor.BOTTOM_CENTER, HudAnchor.BOTTOM_RIGHT -> screenH - blockH - offsetY
        }
        return left to top
    }

    /** X of a line of width [lineW] within a [blockW]-wide block starting at [left]. */
    fun lineX(left: Int, blockW: Int, lineW: Int, align: TextAlign): Int = when (align) {
        TextAlign.LEFT -> left
        TextAlign.CENTER -> left + (blockW - lineW) / 2
        TextAlign.RIGHT -> left + (blockW - lineW)
    }

    fun bounds(
        anchor: HudAnchor, offsetX: Int, offsetY: Int,
        blockW: Int, blockH: Int, screenW: Int, screenH: Int,
    ): Rect {
        val (l, t) = resolveTopLeft(anchor, offsetX, offsetY, blockW, blockH, screenW, screenH)
        return Rect(l, t, blockW, blockH)
    }
}
