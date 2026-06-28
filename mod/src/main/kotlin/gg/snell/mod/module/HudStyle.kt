package gg.snell.mod.module

import gg.snell.shared.SnellPalette

/**
 * Where a HUD element is pinned. Offsets grow inward from the chosen point, so an element
 * keeps its place across resolution and GUI-scale changes (resolved each frame).
 */
enum class HudAnchor {
    TOP_LEFT, TOP_CENTER, TOP_RIGHT,
    MID_LEFT, CENTER, MID_RIGHT,
    BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT,
}

enum class TextAlign { LEFT, CENTER, RIGHT }

/**
 * All per-element visual customization. Pure data (no Minecraft types) so it is fully
 * unit-testable. Defaults match the launcher palette, so a fresh install looks themed;
 * every field is user-overridable in the HUD editor and persisted by the config layer.
 */
data class HudStyle(
    val color: Int = SnellPalette.text,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strikethrough: Boolean = false,
    val shadow: Boolean = true,
    val scale: Float = 1.0f,
    val align: TextAlign = TextAlign.LEFT,
    val background: Boolean = false,
    val backgroundColor: Int = SnellPalette.surfaceAlpha(0xC0),
    val padding: Int = 2,
)
