package gg.maeve.mod.module.hud

import gg.maeve.mod.module.HudLine
import gg.maeve.mod.module.HudModule
import gg.maeve.mod.platform.GameContext

/** Simple WASD keystroke display. A graphical key grid lands in Phase 2. */
class KeystrokesModule : HudModule {
    override val id = "keystrokes"
    override val displayName = "Keystrokes"
    override var enabled = false   // off by default; opt-in
    override var x = 4
    override var y = 40

    override fun render(ctx: GameContext): List<HudLine> {
        fun mark(down: Boolean, c: String) = if (down) "[$c]" else " $c "
        val on = 0xFFFFFFFF.toInt()
        return listOf(
            HudLine("  ${mark(ctx.keyForward, "W")}  ", on),
            HudLine("${mark(ctx.keyLeft, "A")}${mark(ctx.keyBack, "S")}${mark(ctx.keyRight, "D")}", on),
        )
    }
}
