package gg.maeve.mod.editor

/**
 * Layout for the editor's second tier — the card grid reached from the "Mods" button. One card
 * per registered module (HUD and non-HUD), in a centered panel; clicking a card opens that
 * module's customization popup. A "Back" button returns to the position screen. Pure.
 *
 * Callers: EditorState (hit-test) and EditorRenderer (draw).
 */
object GridLayout {
    private const val COLS = 3
    private const val CARD_W = 92
    private const val CARD_H = 44
    private const val GAP = 8
    private const val TOP_PAD = 26 // room for the title + Back button

    fun panelRect(screenW: Int, screenH: Int, count: Int): Rect {
        val rows = ((count + COLS - 1) / COLS).coerceAtLeast(1)
        val w = COLS * CARD_W + (COLS + 1) * GAP
        val h = TOP_PAD + rows * (CARD_H + GAP) + GAP
        return Rect((screenW - w) / 2, ((screenH - h) / 2).coerceAtLeast(0), w, h)
    }

    /** One card rect per module index, left-to-right then top-to-bottom inside the panel. */
    fun cards(screenW: Int, screenH: Int, count: Int): List<Rect> {
        val panel = panelRect(screenW, screenH, count)
        return (0 until count).map { i ->
            val col = i % COLS
            val row = i / COLS
            Rect(
                panel.left + GAP + col * (CARD_W + GAP),
                panel.top + TOP_PAD + row * (CARD_H + GAP),
                CARD_W,
                CARD_H,
            )
        }
    }

    fun backButton(screenW: Int, screenH: Int, count: Int): Rect {
        val panel = panelRect(screenW, screenH, count)
        return Rect(panel.left + 6, panel.top + 4, 44, 14)
    }
}
