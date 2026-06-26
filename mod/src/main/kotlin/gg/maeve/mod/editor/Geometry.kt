package gg.maeve.mod.editor

/** Pure 2D geometry for the HUD editor (scaled-GUI pixel space). No Minecraft types. */
data class Point(val x: Int, val y: Int)
data class Size(val w: Int, val h: Int)

data class Rect(val left: Int, val top: Int, val width: Int, val height: Int) {
    val right: Int get() = left + width
    val bottom: Int get() = top + height
    fun contains(px: Int, py: Int): Boolean = px >= left && px < right && py >= top && py < bottom
}

/** A clickable region in the editor (a control in the customization popup, etc.). */
data class Control(val id: String, val rect: Rect)

/** A HUD element's id and its on-screen bounds, in draw order. */
data class ElementBox(val id: String, val rect: Rect)

/** The id of the topmost (last-drawn) box containing the point, or null if none. */
fun hitTest(boxes: List<ElementBox>, x: Int, y: Int): String? =
    boxes.lastOrNull { it.rect.contains(x, y) }?.id
