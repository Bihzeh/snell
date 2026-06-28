package gg.snell.mod.editor

import gg.snell.mod.module.HudAnchor

/**
 * Editor-side inverse of HudLayout.resolveTopLeft. When a dragged element is released, pick
 * the nearest of the 9 anchors (by element center within screen thirds) and the offset that
 * keeps the element visually fixed, so it then stays pinned across resolution/GUI-scale changes.
 */
object EditorAnchor {
    private val ANCHORS = arrayOf(
        HudAnchor.TOP_LEFT, HudAnchor.TOP_CENTER, HudAnchor.TOP_RIGHT,
        HudAnchor.MID_LEFT, HudAnchor.CENTER, HudAnchor.MID_RIGHT,
        HudAnchor.BOTTOM_LEFT, HudAnchor.BOTTOM_CENTER, HudAnchor.BOTTOM_RIGHT,
    )

    fun anchorFromPosition(box: Rect, screenW: Int, screenH: Int): HudAnchor {
        val cx = box.left + box.width / 2
        val cy = box.top + box.height / 2
        val col = if (cx < screenW / 3) 0 else if (cx < screenW * 2 / 3) 1 else 2
        val row = if (cy < screenH / 3) 0 else if (cy < screenH * 2 / 3) 1 else 2
        return ANCHORS[row * 3 + col]
    }

    /** The offset (inward gap / nudge) that reproduces [box]'s top-left for [anchor]. */
    fun offsetForAnchor(anchor: HudAnchor, box: Rect, screenW: Int, screenH: Int): Pair<Int, Int> {
        val offX = when (anchor) {
            HudAnchor.TOP_LEFT, HudAnchor.MID_LEFT, HudAnchor.BOTTOM_LEFT -> box.left
            HudAnchor.TOP_CENTER, HudAnchor.CENTER, HudAnchor.BOTTOM_CENTER -> box.left - (screenW - box.width) / 2
            HudAnchor.TOP_RIGHT, HudAnchor.MID_RIGHT, HudAnchor.BOTTOM_RIGHT -> screenW - box.width - box.left
        }
        val offY = when (anchor) {
            HudAnchor.TOP_LEFT, HudAnchor.TOP_CENTER, HudAnchor.TOP_RIGHT -> box.top
            HudAnchor.MID_LEFT, HudAnchor.CENTER, HudAnchor.MID_RIGHT -> box.top - (screenH - box.height) / 2
            HudAnchor.BOTTOM_LEFT, HudAnchor.BOTTOM_CENTER, HudAnchor.BOTTOM_RIGHT -> screenH - box.height - box.top
        }
        return offX to offY
    }
}
