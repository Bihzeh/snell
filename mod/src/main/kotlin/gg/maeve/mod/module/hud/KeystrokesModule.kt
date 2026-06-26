package gg.maeve.mod.module.hud

import gg.maeve.mod.module.ColorOption
import gg.maeve.mod.module.ColorTarget
import gg.maeve.mod.module.HudAnchor
import gg.maeve.mod.module.HudLine
import gg.maeve.mod.module.HudModule
import gg.maeve.mod.module.HudSize
import gg.maeve.mod.module.HudStyle
import gg.maeve.mod.module.ModuleOptions
import gg.maeve.mod.module.ToggleOption
import gg.maeve.mod.platform.GameContext
import gg.maeve.mod.platform.HudCanvas
import gg.maeve.mod.platform.TextRun

/**
 * Fully-customizable boxed WASD keystroke display (custom-drawn). Independent colours for the key
 * box, the held (pressed) box, the letters and the box/letter outlines; toggles for auto letter
 * colour, box/letter outlines and the spacebar dash. W sits exactly over S; gaps are uniform.
 */
class KeystrokesModule : HudModule {
    override val id = "keystrokes"
    override val displayName = "Keystrokes"
    override var enabled = false
    override var anchor = HudAnchor.TOP_LEFT
    override var offsetX = 4
    override var offsetY = 40
    override val defaultStyle = HudStyle()
    override var style = HudStyle()

    private val opts = ModuleOptions(
        listOf(
            ColorOption("box", "Box", 0xD03B3658.toInt()),
            ColorOption("pressed", "Pressed", 0xE01D1B2C.toInt()),
            ColorOption("letter", "Letter", 0xFFFFFFFF.toInt()),
            ColorOption("boxOutline", "Box edge", 0xFF8B6DFF.toInt()),
            ColorOption("letterOutline", "Letter edge", 0xFF101018.toInt()),
            ToggleOption("space", "Spacebar", true),
            ToggleOption("autoLetter", "Auto letter colour", true),
            ToggleOption("boxOutlineOn", "Box outline", false),
            ToggleOption("letterOutlineOn", "Letter outline", false),
            ToggleOption("spaceDash", "Spacebar dash", true),
        ),
    )
    override val options get() = opts.options
    override fun option(key: String) = opts.bool(key)
    override fun setOption(key: String, value: Boolean) = opts.setBool(key, value)
    override fun colorOption(key: String) = opts.color(key)
    override fun setColorOption(key: String, value: Int) = opts.setColor(key, value)

    override fun colorTargets() = listOf(
        ColorTarget("box", "Box"), ColorTarget("letter", "Letter"), ColorTarget("pressed", "Pressed"),
        ColorTarget("boxOutline", "Box edge"), ColorTarget("letterOutline", "Letter edge"),
    )
    override fun targetColor(key: String) = opts.color(key)
    override fun setTargetColor(key: String, value: Int) {
        opts.setColor(key, value)
        when (key) { // picking a colour engages its effect so the change is visible immediately
            "letter" -> opts.setBool("autoLetter", false)
            "boxOutline" -> opts.setBool("boxOutlineOn", true)
            "letterOutline" -> opts.setBool("letterOutlineOn", true)
        }
    }

    override fun render(ctx: GameContext): List<HudLine> = emptyList() // custom-drawn

    override fun footprint(ctx: GameContext): HudSize {
        val w = 3 * KEY + 2 * GAP
        var h = KEY + GAP + KEY
        if (opts.bool("space")) h += GAP + SPACE_H
        return HudSize(w, h)
    }

    override fun drawCustom(canvas: HudCanvas, ctx: GameContext) {
        val rowW = 3 * KEY + 2 * GAP
        key(canvas, KEY + GAP, 0, KEY, KEY, ctx.keyForward, "W") // W column == S column
        val y2 = KEY + GAP
        key(canvas, 0, y2, KEY, KEY, ctx.keyLeft, "A")
        key(canvas, KEY + GAP, y2, KEY, KEY, ctx.keyBack, "S")
        key(canvas, 2 * (KEY + GAP), y2, KEY, KEY, ctx.keyRight, "D")
        if (opts.bool("space")) {
            val sy = y2 + KEY + GAP
            key(canvas, 0, sy, rowW, SPACE_H, ctx.keyJump, "")
            if (opts.bool("spaceDash")) {
                val fill = if (ctx.keyJump) opts.color("pressed") else opts.color("box")
                val dashW = rowW / 3
                canvas.fill((rowW - dashW) / 2, sy + SPACE_H / 2 - 1, dashW, 2, letter(fill))
            }
        }
    }

    private fun key(canvas: HudCanvas, x: Int, y: Int, w: Int, h: Int, pressed: Boolean, label: String) {
        val fill = if (pressed) opts.color("pressed") else opts.color("box")
        canvas.fill(x, y, w, h, fill)
        if (opts.bool("boxOutlineOn")) border(canvas, x, y, w, h, opts.color("boxOutline"))
        if (label.isEmpty()) return
        val lc = letter(fill)
        val tw = canvas.textWidth(label)
        val lx = x + (w - tw + 1) / 2 // +1 offsets the font's trailing advance so the glyph centres
        val ly = y + (h - canvas.lineHeight) / 2 + 1
        if (opts.bool("letterOutlineOn")) {
            val oc = opts.color("letterOutline")
            for (dx in -1..1) for (dy in -1..1) {
                if (dx != 0 || dy != 0) canvas.drawStyledText(lx + dx, ly + dy, label, TextRun(oc, shadow = false))
            }
        }
        canvas.drawStyledText(lx, ly, label, TextRun(lc, bold = style.bold, italic = style.italic, shadow = false))
    }

    /** The effective letter colour for a given fill — manual, or auto-contrast when enabled. */
    private fun letter(fill: Int): Int = if (opts.bool("autoLetter")) autoContrast(fill) else opts.color("letter")

    private fun border(canvas: HudCanvas, x: Int, y: Int, w: Int, h: Int, c: Int) {
        canvas.fill(x, y, w, 1, c); canvas.fill(x, y + h - 1, w, 1, c)
        canvas.fill(x, y, 1, h, c); canvas.fill(x + w - 1, y, 1, h, c)
    }

    private fun autoContrast(bg: Int): Int {
        val r = (bg ushr 16) and 0xFF; val g = (bg ushr 8) and 0xFF; val b = bg and 0xFF
        val lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
        return if (lum > 0.6) 0xFF101018.toInt() else 0xFFFFFFFF.toInt()
    }

    private companion object {
        const val KEY = 18
        const val GAP = 4
        const val SPACE_H = 11
    }
}
