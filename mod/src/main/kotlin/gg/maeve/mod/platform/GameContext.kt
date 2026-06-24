package gg.maeve.mod.platform

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
)

/**
 * Minimal text-drawing surface backed by the Minecraft DrawContext at runtime.
 * Modules draw through this so the renderer can be swapped/tested.
 */
interface HudCanvas {
    fun drawText(x: Int, y: Int, text: String, color: Int)
    fun textWidth(text: String): Int
    val lineHeight: Int
}
