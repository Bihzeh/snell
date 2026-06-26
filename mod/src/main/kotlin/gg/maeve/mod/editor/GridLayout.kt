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
    private const val CARD_W = 120
    private const val CARD_H = 58
    private const val GAP = 10
    private const val PAD = 12
    private const val HEADER_H = 30 // title bar + Back button

    fun panelRect(screenW: Int, screenH: Int, count: Int): Rect {
        val rows = ((count + COLS - 1) / COLS).coerceAtLeast(1)
        val w = COLS * CARD_W + (COLS - 1) * GAP + PAD * 2
        val h = HEADER_H + rows * (CARD_H + GAP) - GAP + PAD
        return Rect((screenW - w) / 2, ((screenH - h) / 2).coerceAtLeast(0), w, h)
    }

    /** One card rect per module index, left-to-right then top-to-bottom inside the panel. */
    fun cards(screenW: Int, screenH: Int, count: Int): List<Rect> {
        val panel = panelRect(screenW, screenH, count)
        return (0 until count).map { i ->
            val col = i % COLS
            val row = i / COLS
            Rect(
                panel.left + PAD + col * (CARD_W + GAP),
                panel.top + HEADER_H + row * (CARD_H + GAP),
                CARD_W,
                CARD_H,
            )
        }
    }

    fun backButton(screenW: Int, screenH: Int, count: Int): Rect {
        val panel = panelRect(screenW, screenH, count)
        return Rect(panel.left + PAD, panel.top + 7, 46, 16)
    }

    /** Header strip height (for the renderer's title bar). */
    const val HEADER = HEADER_H
}
