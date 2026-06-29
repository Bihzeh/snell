package gg.snell.mod.ui

import gg.snell.mod.editor.Rect
import gg.snell.mod.platform.EditorCanvas
import gg.snell.shared.SnellPalette
import kotlin.math.abs

/** Button visual styles, mirroring the launcher's SnellButton variants. */
enum class SnellBtn { Primary, Secondary, Ghost, Danger }

/** Status-pill colour roles, mirroring the launcher's PillKind. */
enum class PillRole { Online, Offline, Info, Neutral, Warning }

/**
 * The in-game menu design system ("Snell In-Game Menus"), drawn only from the canvas primitives
 * Minecraft gives us (fill / outline / vertical gradient / text / scale) so it stays version-
 * independent and unit-renderable off a real game. Twilight-purple translucent cards over a (would-be
 * blurred) world, a cyan accent, gold trim, soft shadows, knocked corners and real toggles/sliders.
 *
 * All methods are pure draws (side effects on [EditorCanvas]); layout/hit-testing lives in the
 * per-screen `*Layout` so it can be tested without a canvas.
 */
object SnellUi {
    const val WHITE: Int = 0xFFFFFFFF.toInt()
    private const val BLACK: Int = 0xFF000000.toInt()

    // The design paints panel/row chrome with rgba(255,255,255,a). Derive those white-alpha tints once.
    val rowFill: Int = SnellPalette.withAlpha(WHITE, 0x07)    // row / control field fill (~3%)
    val rowFillHi: Int = SnellPalette.withAlpha(WHITE, 0x0E)  // hovered row fill
    val rowBorder: Int = SnellPalette.withAlpha(WHITE, 0x12)  // row / field border (~6%)
    val panelBorder: Int = SnellPalette.withAlpha(WHITE, 0x16) // card border (~8%)
    private val shadow: Int = 0x70000000

    // ---- colour maths -------------------------------------------------------------------------
    fun blend(a: Int, b: Int, f: Float): Int {
        val ar = (a ushr 16) and 0xFF; val ag = (a ushr 8) and 0xFF; val ab = a and 0xFF
        val br = (b ushr 16) and 0xFF; val bg = (b ushr 8) and 0xFF; val bb = b and 0xFF
        val rr = (ar + (br - ar) * f).toInt().coerceIn(0, 255)
        val gg = (ag + (bg - ag) * f).toInt().coerceIn(0, 255)
        val bl = (ab + (bb - ab) * f).toInt().coerceIn(0, 255)
        return (a.toLong() and 0xFF000000L).toInt() or (rr shl 16) or (gg shl 8) or bl
    }
    fun lighten(c: Int, f: Float) = blend(c, WHITE, f)
    fun darken(c: Int, f: Float) = blend(c, BLACK, f)

    fun ellipsize(canvas: EditorCanvas, text: String, maxW: Int): String {
        if (canvas.textWidth(text) <= maxW) return text
        var t = text
        while (t.isNotEmpty() && canvas.textWidth("$t…") > maxW) t = t.dropLast(1)
        return t.trimEnd() + "…"
    }

    /** Knock ~2px corners to [bg] so a panel/row/button reads as rounded over that surface. */
    fun round(canvas: EditorCanvas, r: Rect, bg: Int) {
        canvas.fill(r.left, r.top, 2, 1, bg); canvas.fill(r.left, r.top, 1, 2, bg)
        canvas.fill(r.right - 2, r.top, 2, 1, bg); canvas.fill(r.right - 1, r.top, 1, 2, bg)
        canvas.fill(r.left, r.bottom - 1, 2, 1, bg); canvas.fill(r.left, r.bottom - 2, 1, 2, bg)
        canvas.fill(r.right - 2, r.bottom - 1, 2, 1, bg); canvas.fill(r.right - 1, r.bottom - 2, 1, 2, bg)
    }

    // ---- tiny vector glyphs (the fixed game font has no icon set) ------------------------------

    /** Filled right-pointing triangle centred at ([cx],[cy]); [s] is the full height. */
    fun playGlyph(canvas: EditorCanvas, cx: Int, cy: Int, s: Int, color: Int) {
        val half = s / 2
        for (dy in -half..half) {
            val w = (half - abs(dy)).coerceAtLeast(0) + 1
            canvas.fill(cx - half / 2, cy + dy, w, 1, color)
        }
    }

    /** A plus sign centred at ([cx],[cy]) spanning [s] px. */
    fun plusGlyph(canvas: EditorCanvas, cx: Int, cy: Int, s: Int, color: Int) {
        val h = s / 2
        canvas.fill(cx - h, cy, s, 2, color)
        canvas.fill(cx, cy - h, 2, s, color)
    }

    /** A right chevron (›) centred at ([cx],[cy]); [s] is its half-extent. */
    fun chevronRight(canvas: EditorCanvas, cx: Int, cy: Int, s: Int, color: Int) {
        for (i in 0..s) {
            canvas.fill(cx - s / 2 + i, cy - s + i, 2, 2, color)
            canvas.fill(cx - s / 2 + i, cy + s - i, 2, 2, color)
        }
    }

    /** A left chevron (‹) centred at ([cx],[cy]); [s] is its half-extent (back buttons). */
    fun chevronLeft(canvas: EditorCanvas, cx: Int, cy: Int, s: Int, color: Int) {
        for (i in 0..s) {
            canvas.fill(cx + s / 2 - i, cy - s + i, 2, 2, color)
            canvas.fill(cx + s / 2 - i, cy + s - i, 2, 2, color)
        }
    }

    /** A down chevron (⌄) centred at ([cx],[cy]); [s] is its half-extent (cycle/expand controls). */
    fun chevronDown(canvas: EditorCanvas, cx: Int, cy: Int, s: Int, color: Int) {
        for (i in 0..s) {
            canvas.fill(cx - s + i, cy - s / 2 + i, 2, 2, color)
            canvas.fill(cx + s - i, cy - s / 2 + i, 2, 2, color)
        }
    }

    /** A check mark (✓) centred near ([cx],[cy]); [s] scales it. */
    fun checkGlyph(canvas: EditorCanvas, cx: Int, cy: Int, s: Int, color: Int) {
        val a = s / 2
        for (i in 0..a) canvas.fill(cx - a + i, cy + i, 2, 2, color)        // down-right stroke
        for (i in 0..s) canvas.fill(cx + i, cy + a - i, 2, 2, color)        // up-right stroke
    }

    /** A small filled dot (status indicator). */
    fun dot(canvas: EditorCanvas, cx: Int, cy: Int, d: Int, color: Int) =
        canvas.fill(cx - d / 2, cy - d / 2, d, d, color)

    // ---- surfaces -----------------------------------------------------------------------------

    /** Opaque full-screen menu backdrop (dark base + a faint top sheen). */
    fun backdrop(canvas: EditorCanvas, w: Int, h: Int) {
        canvas.fill(0, 0, w, h, SnellPalette.menuBase)
        canvas.gradientV(0, 0, w, h * 2 / 3, SnellPalette.withAlpha(SnellPalette.accent, 0x12), SnellPalette.withAlpha(SnellPalette.accent, 0))
    }

    /** Translucent dim over the live world/HUD, for menus opened in-game (pause, popups). */
    fun scrim(canvas: EditorCanvas, w: Int, h: Int) =
        canvas.fill(0, 0, w, h, SnellPalette.withAlpha(SnellPalette.menuBase, 0xCC))

    /** Card/panel surface: soft drop shadow, translucent-purple fill, white-alpha border, round corners. */
    fun panel(canvas: EditorCanvas, r: Rect, under: Int = SnellPalette.menuBase) {
        canvas.fill(r.left + 4, r.bottom, r.width, 4, shadow)
        canvas.fill(r.right, r.top + 4, 4, r.height, shadow)
        canvas.fill(r.left, r.top, r.width, r.height, SnellPalette.menuPanel)
        canvas.border(r.left, r.top, r.width, r.height, panelBorder)
        round(canvas, r, under)
    }

    /** A 1px hairline divider, in the design's white-alpha. */
    fun divider(canvas: EditorCanvas, x: Int, y: Int, w: Int, color: Int = rowBorder) =
        canvas.fill(x, y, w, 1, color)

    /** Muted uppercase section label (no rule), like the design's section headers. */
    fun sectionLabel(canvas: EditorCanvas, x: Int, y: Int, text: String) =
        canvas.drawText(x, y, text.uppercase(), SnellPalette.menuText3)

    // ---- buttons / controls -------------------------------------------------------------------

    /** A button in one of four [SnellBtn] styles, with hover + disabled states. */
    fun button(canvas: EditorCanvas, r: Rect, text: String, style: SnellBtn = SnellBtn.Secondary, hover: Boolean = false, enabled: Boolean = true) {
        val fg: Int
        when (style) {
            SnellBtn.Primary -> {
                if (enabled) canvas.fill(r.left + 2, r.bottom, r.width - 4, 2, SnellPalette.withAlpha(SnellPalette.accent, 0x55)) // glow
                val top = if (enabled) (if (hover) lighten(SnellPalette.accent, 0.10f) else SnellPalette.accent) else darken(SnellPalette.accent, 0.5f)
                val bot = if (enabled) SnellPalette.accentMid else darken(SnellPalette.accentMid, 0.5f)
                canvas.gradientV(r.left, r.top, r.width, r.height, top, bot)
                canvas.border(r.left, r.top, r.width, r.height, lighten(SnellPalette.accent, 0.2f))
                fg = if (enabled) SnellPalette.onAccent else SnellPalette.textDisabled
            }
            SnellBtn.Secondary -> {
                canvas.fill(r.left, r.top, r.width, r.height, if (enabled && hover) rowFillHi else rowFill)
                canvas.border(r.left, r.top, r.width, r.height, rowBorder)
                fg = if (enabled) SnellPalette.text else SnellPalette.textDisabled
            }
            SnellBtn.Ghost -> {
                if (enabled && hover) canvas.fill(r.left, r.top, r.width, r.height, rowFill)
                fg = if (enabled) SnellPalette.menuText3 else SnellPalette.textDisabled
            }
            SnellBtn.Danger -> {
                canvas.fill(r.left, r.top, r.width, r.height, SnellPalette.withAlpha(SnellPalette.danger, if (enabled && hover) 0x26 else 0x14))
                canvas.border(r.left, r.top, r.width, r.height, SnellPalette.withAlpha(SnellPalette.danger, 0x40))
                fg = if (enabled) SnellPalette.dangerSoft else SnellPalette.textDisabled
            }
        }
        round(canvas, r, SnellPalette.menuPanel)
        val tw = canvas.textWidth(text)
        canvas.drawText(r.left + (r.width - tw) / 2, r.top + (r.height - canvas.lineHeight) / 2 + 1, text, fg)
    }

    /** Square icon button chrome (back / refresh / close); caller draws the glyph centred. */
    fun squareButton(canvas: EditorCanvas, r: Rect, hover: Boolean = false, danger: Boolean = false) {
        val fill = when {
            danger && hover -> SnellPalette.withAlpha(SnellPalette.danger, 0x22)
            hover -> rowFillHi
            else -> rowFill
        }
        canvas.fill(r.left, r.top, r.width, r.height, fill)
        canvas.border(r.left, r.top, r.width, r.height, if (danger && hover) SnellPalette.withAlpha(SnellPalette.danger, 0x55) else rowBorder)
        round(canvas, r, SnellPalette.menuPanel)
    }

    /** Rounded icon tile (world/server/nav glyph holder); caller draws the glyph/initial centred. */
    fun iconTile(canvas: EditorCanvas, r: Rect, bg: Int, border: Int? = null) {
        canvas.fill(r.left, r.top, r.width, r.height, bg)
        if (border != null) canvas.border(r.left, r.top, r.width, r.height, border)
        round(canvas, r, SnellPalette.menuPanel)
    }

    /**
     * A large nav row (the title's Singleplayer/Multiplayer buttons): a coloured icon tile on the
     * left, a title over a muted subtitle, and a chevron. Caller draws the tile glyph in the returned
     * icon-tile rect.
     */
    fun navButton(canvas: EditorCanvas, r: Rect, tileColor: Int, title: String, subtitle: String, hover: Boolean, accent: Boolean = true): Rect {
        canvas.fill(r.left, r.top, r.width, r.height, if (hover) (if (accent) SnellPalette.accentSubtle else rowFillHi) else rowFill)
        canvas.border(r.left, r.top, r.width, r.height, if (hover && accent) SnellPalette.withAlpha(SnellPalette.accent, 0x66) else rowBorder)
        round(canvas, r, SnellPalette.menuPanel)
        val tile = r.height - 12
        val tileRect = Rect(r.left + 7, r.top + (r.height - tile) / 2, tile, tile)
        iconTile(canvas, tileRect, SnellPalette.withAlpha(tileColor, 0x22), SnellPalette.withAlpha(tileColor, 0x55))
        val tx = tileRect.right + 9
        val block = canvas.lineHeight * 2 + 2
        val ty = r.top + (r.height - block) / 2
        canvas.drawText(tx, ty, ellipsize(canvas, title, r.right - 22 - tx), SnellPalette.text)
        canvas.drawText(tx, ty + canvas.lineHeight + 2, ellipsize(canvas, subtitle, r.right - 22 - tx), SnellPalette.text2)
        chevronRight(canvas, r.right - 12, r.top + r.height / 2, 3, SnellPalette.menuText3)
        return tileRect
    }

    /** A 2-state pill toggle: cyan track + white knob when on, neutral inset when off. */
    fun switch(canvas: EditorCanvas, r: Rect, on: Boolean) {
        val track = if (on) SnellPalette.accent else SnellPalette.menuInset
        canvas.fill(r.left, r.top, r.width, r.height, track)
        canvas.border(r.left, r.top, r.width, r.height, if (on) lighten(SnellPalette.accent, 0.2f) else rowBorder)
        round(canvas, r, SnellPalette.menuPanel)
        val k = r.height - 4
        val kx = if (on) r.right - k - 2 else r.left + 2
        canvas.fill(kx, r.top + 2, k, k, if (on) WHITE else SnellPalette.text3)
        round(canvas, Rect(kx, r.top + 2, k, k), track)
    }

    /** A thin track + cyan fill to [fraction] + white knob, with [valueText] right-aligned in accent. */
    fun slider(canvas: EditorCanvas, r: Rect, fraction: Float, valueText: String, valueW: Int = 0) {
        val f = fraction.coerceIn(0f, 1f)
        val vw = if (valueText.isEmpty()) 0 else (if (valueW > 0) valueW else canvas.textWidth(valueText) + 6)
        val trackLeft = r.left
        val trackW = (r.width - vw - (if (vw > 0) 6 else 0)).coerceAtLeast(8)
        val trackH = 4
        val trackY = r.top + (r.height - trackH) / 2
        canvas.fill(trackLeft, trackY, trackW, trackH, SnellPalette.menuInset)
        canvas.border(trackLeft, trackY, trackW, trackH, rowBorder)
        val fillW = (trackW * f).toInt()
        if (fillW > 0) canvas.fill(trackLeft, trackY, fillW, trackH, SnellPalette.accent)
        val knob = 7
        val kx = (trackLeft + fillW - knob / 2).coerceIn(trackLeft, trackLeft + trackW - knob)
        val ky = r.top + (r.height - knob) / 2
        canvas.fill(kx, ky, knob, knob, WHITE)
        canvas.border(kx, ky, knob, knob, SnellPalette.accentHi)
        if (vw > 0) canvas.drawText(r.right - canvas.textWidth(valueText), r.top + (r.height - canvas.lineHeight) / 2 + 1, valueText, SnellPalette.accent)
    }

    /**
     * A selection-list row: hover lighten, selected gets an accent left-strip + subtle cyan wash and
     * border. Caller draws the row content (icon/title/subtitle) on top.
     */
    fun listRow(canvas: EditorCanvas, r: Rect, selected: Boolean, hover: Boolean, under: Int = SnellPalette.menuPanel) {
        val bg = when {
            selected -> SnellPalette.accentSubtle
            hover -> rowFillHi
            else -> rowFill
        }
        canvas.fill(r.left, r.top, r.width, r.height, bg)
        canvas.border(r.left, r.top, r.width, r.height, if (selected || hover) SnellPalette.withAlpha(SnellPalette.accent, if (selected) 0x66 else 0x40) else rowBorder)
        if (selected) canvas.fill(r.left, r.top, 3, r.height, SnellPalette.accent)
        round(canvas, r, under)
    }

    /** A category-nav item (Options left rail): active gets a cyan left bar + fill. Caller draws icon+label. */
    fun categoryItem(canvas: EditorCanvas, r: Rect, active: Boolean, hover: Boolean) {
        if (active || hover) canvas.fill(r.left, r.top, r.width, r.height, if (active) rowFillHi else rowFill)
        if (active) canvas.fill(r.left, r.top + (r.height - 14) / 2, 3, 14, SnellPalette.accent)
        if (active || hover) round(canvas, r, SnellPalette.menuPanel)
    }

    /** A status pill (rounded, tinted background + coloured dot + label), like the launcher's StatusPill. */
    fun pill(canvas: EditorCanvas, x: Int, y: Int, text: String, role: PillRole) {
        val c = when (role) {
            PillRole.Online -> SnellPalette.accent
            PillRole.Offline -> SnellPalette.danger
            PillRole.Info -> SnellPalette.info
            PillRole.Warning -> SnellPalette.ember
            PillRole.Neutral -> SnellPalette.menuText3
        }
        val w = canvas.textWidth(text) + 16
        val h = canvas.lineHeight + 6
        val r = Rect(x, y, w, h)
        canvas.fill(r.left, r.top, r.width, r.height, SnellPalette.withAlpha(c, 0x22))
        canvas.border(r.left, r.top, r.width, r.height, SnellPalette.withAlpha(c, 0x55))
        round(canvas, r, SnellPalette.menuPanel)
        dot(canvas, r.left + 8, r.top + h / 2, 4, c)
        canvas.drawText(r.left + 13, r.top + (h - canvas.lineHeight) / 2, text, c)
    }

    /** A compact tinted chip (mode pill, "Rewards" badge). Returns its width for layout. */
    fun chip(canvas: EditorCanvas, x: Int, y: Int, text: String, color: Int): Int {
        val w = canvas.textWidth(text) + 10
        val h = canvas.lineHeight + 4
        canvas.fill(x, y, w, h, SnellPalette.withAlpha(color, 0x22))
        canvas.border(x, y, w, h, SnellPalette.withAlpha(color, 0x55))
        round(canvas, Rect(x, y, w, h), SnellPalette.menuPanel)
        canvas.drawText(x + 5, y + (h - canvas.lineHeight) / 2, text, color)
        return w
    }

    /** A keybinding cap (mono-ish boxed key). */
    fun keyCap(canvas: EditorCanvas, r: Rect, text: String) {
        canvas.fill(r.left, r.bottom - 1, r.width, 1, shadow) // tactile bottom shadow
        canvas.fill(r.left, r.top, r.width, r.height - 1, rowFillHi)
        canvas.border(r.left, r.top, r.width, r.height, rowBorder)
        round(canvas, r, SnellPalette.menuPanel)
        canvas.drawText(r.left + (r.width - canvas.textWidth(text)) / 2, r.top + (r.height - canvas.lineHeight) / 2, text, SnellPalette.text2)
    }

    /** Text input chrome: inset field, accent border when focused, placeholder/caret. */
    fun textField(canvas: EditorCanvas, r: Rect, text: String, focused: Boolean, placeholder: String = "") {
        canvas.fill(r.left, r.top, r.width, r.height, SnellPalette.menuInset)
        canvas.border(r.left, r.top, r.width, r.height, if (focused) SnellPalette.accent else rowBorder)
        round(canvas, r, SnellPalette.menuPanel)
        val ty = r.top + (r.height - canvas.lineHeight) / 2 + 1
        if (text.isEmpty() && !focused) {
            canvas.drawText(r.left + 7, ty, ellipsize(canvas, placeholder, r.width - 14), SnellPalette.menuText3)
        } else {
            val shown = ellipsize(canvas, text, r.width - 16)
            canvas.drawText(r.left + 7, ty, shown, SnellPalette.text)
            if (focused) canvas.fill(r.left + 7 + canvas.textWidth(shown) + 1, ty - 1, 1, canvas.lineHeight + 1, SnellPalette.accent)
        }
    }

    /** A vertical scrollbar for a list: faint track + accent-tinted thumb sized to the viewport. */
    fun scrollbar(canvas: EditorCanvas, x: Int, top: Int, trackH: Int, contentH: Int, scrollY: Int) {
        if (contentH <= trackH) return
        canvas.fill(x, top, 3, trackH, rowBorder)
        val thumbH = (trackH.toLong() * trackH / contentH).toInt().coerceAtLeast(16)
        val maxScroll = contentH - trackH
        val thumbY = top + if (maxScroll <= 0) 0 else ((trackH - thumbH).toLong() * scrollY / maxScroll).toInt()
        canvas.fill(x, thumbY, 3, thumbH, SnellPalette.withAlpha(SnellPalette.accent, 0xAA))
    }

    /**
     * A larger screen heading, scaled up from the fixed game font. ([x],[y]) is the top-left of
     * the scaled text; `withScale` makes the pivot the local origin, so the body draws at (0,0).
     * The unscaled width is `textWidth(text)`; multiply by [scale] to centre.
     */
    fun heading(canvas: EditorCanvas, x: Int, y: Int, text: String, scale: Float = 1.6f, color: Int = SnellPalette.text) {
        canvas.withScale(scale, x, y) { canvas.drawText(0, 0, text, color) }
    }
}
