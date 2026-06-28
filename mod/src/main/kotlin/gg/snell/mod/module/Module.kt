package gg.snell.mod.module

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
    /** Corner/edge/center this element is pinned to. Persisted. */
    var anchor: HudAnchor

    /** Offset from [anchor], in scaled GUI pixels: an inward gap for corner/edge anchors,
     *  and a fine nudge from the centered position for the centered anchors. Persisted. */
    var offsetX: Int
    var offsetY: Int

    /** Current visual style (starts at [defaultStyle], user-overridable). Persisted. */
    var style: HudStyle

    /** The module's base/theme style — the reset target. */
    val defaultStyle: HudStyle get() = HudStyle()

    /** Module-specific typed options surfaced in the editor (beyond generic [style]). */
    val options: List<ModuleOption> get() = emptyList()
    val toggles: List<ToggleOption> get() = options.filterIsInstance<ToggleOption>()
    val colorOptions: List<ColorOption> get() = options.filterIsInstance<ColorOption>()
    fun option(key: String): Boolean = false
    fun setOption(key: String, value: Boolean) {}
    fun colorOption(key: String): Int = 0
    fun setColorOption(key: String, value: Int) {}

    /** Colours the editor's HSV picker can target. Default: the single [style] colour.
     *  A module that overrides this to add targets MUST also override [targetColor]/[setTargetColor]
     *  (the defaults only handle the "style" key and would route every other target to [style].color). */
    fun colorTargets(): List<ColorTarget> = listOf(ColorTarget("style", "Colour"))
    fun targetColor(key: String): Int = style.color
    fun setTargetColor(key: String, value: Int) { style = style.copy(color = value) }

    /** Produce the lines to draw this frame. Empty = nothing to draw. */
    fun render(ctx: gg.snell.mod.platform.GameContext): List<HudLine>

    /** If non-null, the module draws itself (boxes/graphics) via [drawCustom] instead of text
     *  lines; the value is its unscaled footprint. [drawCustom] draws at local (0,0). */
    fun footprint(ctx: gg.snell.mod.platform.GameContext): HudSize? = null
    fun drawCustom(canvas: gg.snell.mod.platform.HudCanvas, ctx: gg.snell.mod.platform.GameContext) {}
}

/**
 * One line of HUD text. A null [color] means "use the module's style color"; a non-null
 * value is an explicit per-line override (e.g. keystroke pressed vs released).
 */
data class HudLine(val text: String, val color: Int? = null)

/** Unscaled pixel footprint of a custom-drawn HUD module. */
data class HudSize(val w: Int, val h: Int)
