package gg.snell.mod.platform

/**
 * A snapshot of per-frame game state consumed by HUD modules. Built once per
 * frame from Minecraft by the platform bridge. Modules depend ONLY on this, not
 * on Minecraft classes, which keeps them unit-testable and version-independent.
 */
data class GameContext(
    val fps: Int,
    val inWorld: Boolean,
    val playerX: Double,
    val playerY: Double,
    val playerZ: Double,
    val keyForward: Boolean,
    val keyBack: Boolean,
    val keyLeft: Boolean,
    val keyRight: Boolean,
    val yaw: Float = 0f,
    val dayTime: Long = 0L,
    val speed: Double = 0.0,
    val leftCps: Int = 0,
    val rightCps: Int = 0,
    val keyJump: Boolean = false,
)

/**
 * Render-time text attributes passed from the controller to the canvas (no Minecraft types).
 * Note: Minecraft's text renderer uses an RGB-only color, so the alpha channel of [color] is
 * ignored on styled text — use a background panel ([gg.snell.mod.module.HudStyle.background])
 * for translucency.
 */
data class TextRun(
    val color: Int,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strikethrough: Boolean = false,
    val shadow: Boolean = true,
)

/**
 * Minimal drawing surface backed by the Minecraft retained-mode extractor at runtime.
 * Modules and the renderer draw through this so the logic can be swapped/tested.
 */
interface HudCanvas {
    fun drawText(x: Int, y: Int, text: String, color: Int)
    fun drawStyledText(x: Int, y: Int, text: String, run: TextRun)
    fun fill(x: Int, y: Int, w: Int, h: Int, color: Int)
    fun withScale(scale: Float, pivotX: Int, pivotY: Int, body: () -> Unit)
    fun textWidth(text: String): Int
    val lineHeight: Int
    val screenWidth: Int
    val screenHeight: Int
}

/** Adds the overlay primitives the HUD editor + bespoke menus need on top of [HudCanvas]. */
interface EditorCanvas : HudCanvas {
    /** Draw a 1px rectangle outline. */
    fun border(x: Int, y: Int, w: Int, h: Int, color: Int)
    /** Vertical gradient fill: [top] color at y, [bottom] color at y+h. */
    fun gradientV(x: Int, y: Int, w: Int, h: Int, top: Int, bottom: Int)
    /** Raise the draw z-layer so editor overlays sit above the HUD preview. */
    fun overlayStratum()
    /** Draw a single icon glyph (Tabler, from the always-on `snell:icons` font) at top-left ([x],[y]), tinted [color]. */
    fun drawIcon(glyph: Char, x: Int, y: Int, color: Int)
    /** Blit a full texture (namespaced id, e.g. "snell:textures/gui/snell_mark.png") into the box ([x],[y],[w],[h]). */
    fun drawTexture(id: String, x: Int, y: Int, w: Int, h: Int)
}
