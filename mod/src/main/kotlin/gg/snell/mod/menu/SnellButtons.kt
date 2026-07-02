package gg.snell.mod.menu

import gg.snell.mod.editor.Rect

/** What a Snell button does when pressed; the bridge maps these to real actions. */
enum class SnellButtonAction { HudEditor, Discord, Cosmetics }

/** One injected button: its action, visible label, and screen-space bounds. */
data class SnellButtonSpec(val action: SnellButtonAction, val label: String, val rect: Rect)

/**
 * Pure layout for the Snell buttons added to the VANILLA title/pause screens (the bespoke menu
 * overhaul is gone — the client stays authentic Minecraft, Snell just adds a compact row of
 * vanilla-styled buttons in the top-right, echoing where the old custom UI kept its cluster).
 * No Minecraft types, so it unit-tests headlessly; the bridge feeds the real font width.
 */
object SnellButtons {
    private const val MARGIN = 8      // from the screen's top/right edges
    private const val GAP = 4         // between buttons
    private const val HEIGHT = 20     // vanilla button height
    private const val PAD_X = 8       // label side padding inside a button
    private const val MIN_W = 48

    private val entries = listOf(
        SnellButtonAction.HudEditor to "HUD Editor",
        SnellButtonAction.Discord to "Discord",
        SnellButtonAction.Cosmetics to "Cosmetics",
    )

    /** The button row for a [screenW]x[screenH] screen; [widthOf] measures a label in px. */
    fun cluster(screenW: Int, screenH: Int, widthOf: (String) -> Int): List<SnellButtonSpec> {
        val widths = entries.map { (_, label) -> (widthOf(label) + 2 * PAD_X).coerceAtLeast(MIN_W) }
        var x = screenW - MARGIN - widths.sum() - GAP * (entries.size - 1)
        return entries.mapIndexed { i, (action, label) ->
            val spec = SnellButtonSpec(action, label, Rect(x, MARGIN, widths[i], HEIGHT))
            x += widths[i] + GAP
            spec
        }
    }
}
