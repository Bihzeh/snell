package gg.snell.mod

import gg.snell.mod.platform.GameContext
import gg.snell.mod.platform.HudCanvas
import gg.snell.mod.platform.TextRun

/** Records draw calls so the pure HUD logic can be tested without Minecraft. */
internal class FakeHudCanvas(
    override val screenWidth: Int = 800,
    override val screenHeight: Int = 600,
) : HudCanvas {
    data class Draw(val x: Int, val y: Int, val text: String, val color: Int, val styled: Boolean)
    data class Fill(val x: Int, val y: Int, val w: Int, val h: Int, val color: Int)
    data class Transform(val scale: Float, val pivotX: Int, val pivotY: Int)

    val draws = mutableListOf<Draw>()
    val fills = mutableListOf<Fill>()
    val transforms = mutableListOf<Transform>()

    override fun drawText(x: Int, y: Int, text: String, color: Int) { draws.add(Draw(x, y, text, color, false)) }
    override fun drawStyledText(x: Int, y: Int, text: String, run: TextRun) { draws.add(Draw(x, y, text, run.color, true)) }
    override fun fill(x: Int, y: Int, w: Int, h: Int, color: Int) { fills.add(Fill(x, y, w, h, color)) }
    override fun withScale(scale: Float, pivotX: Int, pivotY: Int, body: () -> Unit) {
        transforms.add(Transform(scale, pivotX, pivotY)); body()
    }
    override fun textWidth(text: String) = text.length * 6
    override val lineHeight = 10
}

internal fun gameCtx(inWorld: Boolean = true) = GameContext(
    fps = 60, inWorld = inWorld,
    playerX = 1.0, playerY = 64.0, playerZ = -2.0,
    keyForward = true, keyBack = false, keyLeft = false, keyRight = true,
)
