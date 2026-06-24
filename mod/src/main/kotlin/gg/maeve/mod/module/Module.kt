package gg.maeve.mod.module

/**
 * A self-contained client feature. The mod menu and config layer are generic
 * over Module, so adding a feature never requires touching menu or config code.
 */
interface Module {
    /** Stable identifier used as the config key. Lowercase, no spaces. */
    val id: String

    /** Human-readable name shown in the mod menu. */
    val displayName: String

    /** Whether the module is currently active. Persisted by the config layer. */
    var enabled: Boolean

    /** Called once when the module is registered. */
    fun onRegister() {}
}

/** A module that draws to the in-game HUD. */
interface HudModule : Module {
    /** Top-left anchor of this element, in scaled GUI pixels. Persisted. */
    var x: Int
    var y: Int

    /** Produce the lines to draw this frame. Empty = nothing to draw. */
    fun render(ctx: gg.maeve.mod.platform.GameContext): List<HudLine>
}

/** One line of HUD text with an ARGB color. */
data class HudLine(val text: String, val color: Int = 0xFFFFFFFF.toInt())
