package gg.snell.shared

/**
 * The launcher "Twilight Royal" palette as plain ARGB Ints, shared by the launcher
 * (Compose `Color(Int)`) and the mod (GuiGraphics ARGB) so the in-game HUD's base theme
 * matches the launcher from a single source of truth. (`const` is not usable here because
 * the ARGB values exceed Int range as positive literals and need `.toInt()`.)
 */
object SnellPalette {
    val primary: Int = 0xFF8B6DFF.toInt()
    val gold: Int = 0xFFE2B45C.toInt()
    val background: Int = 0xFF0B0A12.toInt()
    val surface: Int = 0xFF14121E.toInt()
    val elevated: Int = 0xFF1C1A2A.toInt()
    val text: Int = 0xFFECEAF5.toInt()
    val text2: Int = 0xFFA7A3C0.toInt()
    val outline: Int = 0xFF2A2740.toInt()
    val success: Int = 0xFF48D597.toInt()
    val error: Int = 0xFFFF6B7A.toInt()

    /** [surface] at the given [alpha] (0..255), for translucent HUD background panels. */
    fun surfaceAlpha(alpha: Int): Int = (surface and 0x00FFFFFF) or ((alpha and 0xFF) shl 24)
}
