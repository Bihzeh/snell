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

    /** Knock ~2px corners to [bg] so a tiny element (slider/switch knob) reads as rounded. Surfaces use [surface]. */
    fun round(canvas: EditorCanvas, r: Rect, bg: Int) {
        canvas.fill(r.left, r.top, 2, 1, bg); canvas.fill(r.left, r.top, 1, 2, bg)
        canvas.fill(r.right - 2, r.top, 2, 1, bg); canvas.fill(r.right - 1, r.top, 1, 2, bg)
        canvas.fill(r.left, r.bottom - 1, 2, 1, bg); canvas.fill(r.left, r.bottom - 2, 1, 2, bg)
        canvas.fill(r.right - 2, r.bottom - 1, 2, 1, bg); canvas.fill(r.right - 1, r.bottom - 2, 1, 2, bg)
    }

    // ---- rounded surfaces (9-slice white-master sprites, tinted) -------------------------------
    // Replace the old fill+border+round(knock) triple with real rounded corners. radius ~6 (rrect),
    // ~3 (sm: chips/keycaps/fields), or full capsule (pill: wallet/status/switch track).

    /** A rounded-rect surface: [fill] tint + optional [border] ring (radius ~6). */
    fun surface(canvas: EditorCanvas, r: Rect, fill: Int, border: Int? = null) {
        canvas.sprite("snell:shape/rrect", r.left, r.top, r.width, r.height, fill)
        if (border != null) canvas.sprite("snell:shape/rrect_outline", r.left, r.top, r.width, r.height, border)
    }

    /** A small rounded-rect surface (radius ~3) for chips / keycaps / fields. */
    fun surfaceSm(canvas: EditorCanvas, r: Rect, fill: Int, border: Int? = null) {
        canvas.sprite("snell:shape/rrect_sm", r.left, r.top, r.width, r.height, fill)
        if (border != null) canvas.sprite("snell:shape/rrect_sm_outline", r.left, r.top, r.width, r.height, border)
    }

    /** A full-capsule surface (wallet pill, status pills, switch track): [fill] + optional 1px [border] ring. */
    fun capsule(canvas: EditorCanvas, r: Rect, fill: Int, border: Int? = null) {
        if (border != null) {
            canvas.sprite("snell:shape/pill", r.left, r.top, r.width, r.height, border)
            canvas.sprite("snell:shape/pill", r.left + 1, r.top + 1, r.width - 2, r.height - 2, fill)
        } else {
            canvas.sprite("snell:shape/pill", r.left, r.top, r.width, r.height, fill)
        }
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

    // ---- Tabler icons (bundled subset, drawn from the always-on `snell:icons` font) ------------
    private const val ICON_BASE = 10f // matches the icons.json font size
    // Vertical centring of the glyph in its tile. The icon ink sits in the UPPER part of its text box,
    // so a small DOWNWARD nudge (positive) is needed to optically centre it; the old -0.14 raised it to
    // the top edge (the "floating at the top of the box" bug). Tune in ~0.02 steps if it reads off.
    private const val ICON_V_NUDGE = 0.10f
    private val ICONS: Map<String, Char> = mapOf(
        "discord" to '', "singleplayer" to '', "multiplayer" to '', "options" to '',
        "quit" to '', "wallet" to '', "cosmetics" to '', "friends" to '',
        "back" to '', "chevron" to '', "cycle" to '', "search" to '',
        "play" to '', "create" to '', "add" to '', "edit" to '',
        "delete" to '', "refresh" to '', "done" to '', "check" to '',
        "controls" to '', "audio" to '', "mods" to '', "video" to '',
        "quickswitch" to '', "advancements" to '', "statistics" to '', "lan" to '裡',
        "link" to '', "rewards" to '', "people" to '',
    )

    /** Draw a Tabler [name] icon centred at ([cx],[cy]), [size] px tall, tinted [color]. Unknown name = nothing. */
    fun icon(canvas: EditorCanvas, name: String, cx: Int, cy: Int, size: Int, color: Int) {
        // White-master sprite drawn as a centred square -> exact vertical centring, no font-glyph
        // baseline nudge (that nudge was the "icon floating at the top of the tile" bug). Tinted [color]
        // by the GUI shader; an unknown name simply blits nothing.
        canvas.sprite("snell:icon/$name", cx - size / 2, cy - size / 2, size, size, color)
    }

    private const val SLIPSTREAM_TEX = "snell:textures/gui/snell_mark.png"

    /**
     * The Snell slipstream brand mark — blitted from the bundled cyan texture (both the runtime
     * extractor and the headless menu preview blit; the only no-blit canvas, the HUD-editor preview,
     * never draws this mark). The PNG carries transparent margins (the bars occupy the middle ~40%),
     * so it's drawn into a slightly larger box centred on the (x,y,size) cell to match the wordmark's
     * optical weight. [color] is unused (the texture carries its own colour); kept for call-site symmetry.
     */
    fun slipstream(canvas: EditorCanvas, x: Int, y: Int, size: Int, color: Int = SnellPalette.accent) {
        val box = (size * 1.35f).toInt()
        val off = (box - size) / 2
        canvas.drawTexture(SLIPSTREAM_TEX, x - off, y - off, box, box)
    }

    /** Presence colour: green online / gold away / red offline. */
    fun statusColor(status: String): Int = when (status.lowercase()) {
        "online" -> SnellPalette.success
        "away", "idle" -> SnellPalette.gold
        "offline", "dnd", "busy" -> SnellPalette.danger
        else -> SnellPalette.text3
    }

    // ---- surfaces -----------------------------------------------------------------------------

    /**
     * Full-screen menu backdrop: a dusk atmosphere. The design sits the menu over a blurred dusk
     * world; we can't blur the live panorama from the 2D extractor, so we evoke it — indigo sky →
     * mauve horizon glow → dark base, with a cyan top sheen and a bottom vignette so the cards read.
     */
    fun backdrop(canvas: EditorCanvas, w: Int, h: Int) {
        val horizon = (h * 0.56f).toInt()
        canvas.gradientV(0, 0, w, horizon, 0xFF161229.toInt(), 0xFF3B2A47.toInt())
        canvas.gradientV(0, horizon, w, h - horizon, 0xFF3B2A47.toInt(), SnellPalette.menuBase)
        canvas.gradientV(0, (h * 0.40f).toInt(), w, (h * 0.22f).toInt(), SnellPalette.withAlpha(0xFFE7A6C8.toInt(), 0), SnellPalette.withAlpha(0xFFE7A6C8.toInt(), 0x30))
        canvas.gradientV(0, 0, w, h / 3, SnellPalette.withAlpha(SnellPalette.accent, 0x14), SnellPalette.withAlpha(SnellPalette.accent, 0))
        canvas.gradientV(0, (h * 0.60f).toInt(), w, h - (h * 0.60f).toInt(), SnellPalette.withAlpha(SnellPalette.menuBase, 0), SnellPalette.withAlpha(SnellPalette.menuBase, 0xCC))
    }

    /** Translucent dim over the live world/HUD, for menus opened in-game (pause, popups). */
    fun scrim(canvas: EditorCanvas, w: Int, h: Int) =
        canvas.fill(0, 0, w, h, SnellPalette.withAlpha(SnellPalette.menuBase, 0xCC))

    /**
     * Flat scrim over the live world (pause / options-from-pause). The mockup blurs the world too, but
     * blur can no-op on some GPU paths, so the alpha is kept heavy enough (~0x9E) to keep the world
     * obscured on its own — heavier than the mockup's 0.5 but well short of an opaque dim.
     */
    fun pauseScrim(canvas: EditorCanvas, w: Int, h: Int) =
        canvas.fill(0, 0, w, h, SnellPalette.withAlpha(SnellPalette.menuBase, 0x9E))

    /** Card/panel surface: soft drop shadow, translucent-purple fill, white-alpha border, round corners. */
    fun panel(canvas: EditorCanvas, r: Rect, under: Int = SnellPalette.menuBase) {
        canvas.fill(r.left + 4, r.bottom, r.width, 4, shadow)
        canvas.fill(r.right, r.top + 4, 4, r.height, shadow)
        surface(canvas, r, SnellPalette.menuPanel, panelBorder)
    }

    /** A 1px hairline divider, in the design's white-alpha. */
    fun divider(canvas: EditorCanvas, x: Int, y: Int, w: Int, color: Int = rowBorder) =
        canvas.fill(x, y, w, 1, color)

    /** Muted uppercase section label (no rule), in proportional Geist — section metadata. */
    fun sectionLabel(canvas: EditorCanvas, x: Int, y: Int, text: String) =
        canvas.drawText(x, y, text.uppercase(), SnellPalette.menuText3)

    // ---- buttons / controls -------------------------------------------------------------------

    /** A button in one of four [SnellBtn] styles, with hover + disabled states + an optional leading icon. */
    fun button(canvas: EditorCanvas, r: Rect, text: String, style: SnellBtn = SnellBtn.Secondary, hover: Boolean = false, enabled: Boolean = true, iconName: String? = null) {
        val fg: Int
        when (style) {
            SnellBtn.Primary -> {
                if (enabled) canvas.fill(r.left + 2, r.bottom, r.width - 4, 2, SnellPalette.withAlpha(SnellPalette.accent, 0x55)) // glow
                val face = if (!enabled) darken(SnellPalette.accent, 0.5f) else if (hover) lighten(SnellPalette.accent, 0.10f) else SnellPalette.accent
                surface(canvas, r, face, lighten(SnellPalette.accent, 0.2f))
                fg = if (enabled) SnellPalette.onAccent else SnellPalette.textDisabled
            }
            SnellBtn.Secondary -> {
                surface(canvas, r, if (enabled && hover) rowFillHi else rowFill, rowBorder)
                fg = if (enabled) SnellPalette.text else SnellPalette.textDisabled
            }
            SnellBtn.Ghost -> {
                if (enabled && hover) surface(canvas, r, rowFill, null)
                fg = if (enabled) SnellPalette.menuText3 else SnellPalette.textDisabled
            }
            SnellBtn.Danger -> {
                surface(canvas, r, SnellPalette.withAlpha(SnellPalette.danger, if (enabled && hover) 0x26 else 0x14), SnellPalette.withAlpha(SnellPalette.danger, 0x40))
                fg = if (enabled) SnellPalette.dangerSoft else SnellPalette.textDisabled
            }
        }
        val ty = r.top + (r.height - canvas.lineHeight) / 2 + 1
        if (iconName != null) {
            val isz = (r.height - 10).coerceIn(9, 14)
            val total = isz + 6 + canvas.textWidth(text)
            val sx = r.left + (r.width - total) / 2
            icon(canvas, iconName, sx + isz / 2, r.top + r.height / 2, isz, fg)
            canvas.drawText(sx + isz + 6, ty, text, fg)
        } else {
            canvas.drawText(r.left + (r.width - canvas.textWidth(text)) / 2, ty, text, fg)
        }
    }

    /** Square icon button chrome (back / refresh / close); caller draws the glyph centred. */
    fun squareButton(canvas: EditorCanvas, r: Rect, hover: Boolean = false, danger: Boolean = false) {
        val fill = when {
            danger && hover -> SnellPalette.withAlpha(SnellPalette.danger, 0x22)
            hover -> rowFillHi
            else -> rowFill
        }
        surface(canvas, r, fill, if (danger && hover) SnellPalette.withAlpha(SnellPalette.danger, 0x55) else rowBorder)
    }

    /** Rounded icon tile (world/server/nav glyph holder); caller draws the glyph/initial centred. */
    fun iconTile(canvas: EditorCanvas, r: Rect, bg: Int, border: Int? = null) {
        surface(canvas, r, bg, border)
    }

    /**
     * A large nav row (the title's Singleplayer/Multiplayer buttons): a coloured icon tile on the
     * left, a title over a muted subtitle, and a chevron. Caller draws the tile glyph in the returned
     * icon-tile rect.
     */
    fun navButton(canvas: EditorCanvas, r: Rect, tileColor: Int, title: String, subtitle: String, hover: Boolean, accent: Boolean = true): Rect {
        surface(canvas, r, if (hover) (if (accent) SnellPalette.accentSubtle else rowFillHi) else rowFill, if (hover && accent) SnellPalette.withAlpha(SnellPalette.accent, 0x66) else rowBorder)
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

    /** Full-ARGB lerp (alpha included), unlike [blend] which keeps a's alpha. */
    private fun blendArgb(a: Int, b: Int, f: Float): Int {
        fun ch(s: Int, sh: Int) = (s ushr sh) and 0xFF
        val na = (ch(a, 24) + (ch(b, 24) - ch(a, 24)) * f).toInt().coerceIn(0, 255)
        val nr = (ch(a, 16) + (ch(b, 16) - ch(a, 16)) * f).toInt().coerceIn(0, 255)
        val ng = (ch(a, 8) + (ch(b, 8) - ch(a, 8)) * f).toInt().coerceIn(0, 255)
        val nb = (ch(a, 0) + (ch(b, 0) - ch(a, 0)) * f).toInt().coerceIn(0, 255)
        return (na shl 24) or (nr shl 16) or (ng shl 8) or nb
    }

    /** Approximate a horizontal gradient (MC has only vertical) with [steps] vertical strips, [left]→[right] (alpha included). */
    fun gradientHApprox(canvas: EditorCanvas, r: Rect, left: Int, right: Int, steps: Int = 12) {
        val n = steps.coerceAtLeast(1)
        for (i in 0 until n) {
            val f = if (n == 1) 0f else i / (n - 1f)
            val x0 = r.left + (r.width * i) / n
            val x1 = r.left + (r.width * (i + 1)) / n
            canvas.fill(x0, r.top, (x1 - x0).coerceAtLeast(1), r.height, blendArgb(left, right, f))
        }
    }

    /** A solid filled badge (e.g. "REWARDS") — caller passes already-cased text. Returns its width. */
    fun badge(canvas: EditorCanvas, x: Int, y: Int, text: String, bg: Int, fg: Int = WHITE): Int {
        val w = canvas.textWidth(text) + 10
        val h = canvas.lineHeight + 3
        surfaceSm(canvas, Rect(x, y, w, h), bg, null)
        canvas.drawText(x + 5, y + (h - canvas.lineHeight) / 2, text, fg)
        return w
    }

    /** A solid brand-coloured button (the Discord "Link →" CTA): solid [bg], centred [fg] text + optional trailing icon. */
    fun solidButton(canvas: EditorCanvas, r: Rect, text: String, bg: Int, fg: Int = WHITE, hover: Boolean = false, iconName: String? = null) {
        canvas.fill(r.left, r.top, r.width, r.height, if (hover) lighten(bg, 0.12f) else bg)
        round(canvas, r, SnellPalette.menuPanel)
        val ty = r.top + (r.height - canvas.lineHeight) / 2 + 1
        val tw = canvas.textWidth(text)
        if (iconName != null) {
            val isz = (r.height - 8).coerceIn(7, 11)
            val sx = r.left + (r.width - (tw + 4 + isz)) / 2
            canvas.drawText(sx, ty, text, fg)
            icon(canvas, iconName, sx + tw + 4 + isz / 2, r.top + r.height / 2, isz, fg)
        } else {
            canvas.drawText(r.left + (r.width - tw) / 2, ty, text, fg)
        }
    }

    /** The gold wallet pill (top-right crown balance): gold-tinted chrome, coin glyph + mono [value]. */
    fun walletPill(canvas: EditorCanvas, r: Rect, value: String, hover: Boolean = false) {
        capsule(canvas, r, SnellPalette.withAlpha(SnellPalette.gold, if (hover) 0x26 else 0x1A), SnellPalette.withAlpha(SnellPalette.gold, if (hover) 0x73 else 0x52))
        icon(canvas, "wallet", r.left + 10, r.top + r.height / 2, 11, SnellPalette.gold)
        canvas.drawMono(r.left + 18, r.top + (r.height - canvas.lineHeight) / 2, value, SnellPalette.gold)
    }

    /**
     * The featured Discord nav card: a Discord-tinted gradient fill + glow, a solid brand icon tile, a
     * "REWARDS" [badgeText], a muted [subtitle], and a solid [linkLabel] CTA drawn into [linkRect].
     * Caller draws the tile glyph into the returned icon-tile rect. Visually distinct from [navButton].
     */
    fun featuredNavButton(
        canvas: EditorCanvas, r: Rect, title: String, subtitle: String, badgeText: String,
        linkRect: Rect, linkLabel: String, hover: Boolean, hoverLink: Boolean,
    ): Rect {
        val brand = SnellPalette.discord
        // glow (fake box-shadow): offset translucent fills beneath the card
        canvas.fill(r.left + 4, r.bottom, r.width - 8, 4, SnellPalette.withAlpha(brand, 0x33))
        canvas.fill(r.left + 8, r.bottom + 2, r.width - 16, 3, SnellPalette.withAlpha(brand, 0x1E))
        // rounded discord-tinted card (flat fill; a 9-slice sprite can't carry the 100° gradient). Brighter on hover.
        surface(canvas, r, SnellPalette.withAlpha(brand, if (hover) 0x3A else 0x2A), SnellPalette.withAlpha(brand, if (hover) 0x8C else 0x73))
        // solid brand icon tile
        val tile = r.height - 14
        val tileRect = Rect(r.left + 8, r.top + (r.height - tile) / 2, tile, tile)
        iconTile(canvas, tileRect, brand, lighten(brand, 0.2f))
        // title + REWARDS badge over a muted subtitle
        val tx = tileRect.right + 10
        val block = canvas.lineHeight * 2 + 3
        val ty = r.top + (r.height - block) / 2
        val badgeW = if (badgeText.isEmpty()) 0 else canvas.textWidth(badgeText) + 16
        val titleMax = (linkRect.left - 10 - tx - badgeW).coerceAtLeast(20)
        val shownTitle = ellipsize(canvas, title, titleMax)
        canvas.drawText(tx, ty, shownTitle, SnellPalette.text)
        if (badgeText.isNotEmpty()) badge(canvas, tx + canvas.textWidth(shownTitle) + 6, ty - 1, badgeText, brand)
        canvas.drawText(tx, ty + canvas.lineHeight + 3, ellipsize(canvas, subtitle, linkRect.left - 10 - tx), SnellPalette.text2)
        solidButton(canvas, linkRect, linkLabel, brand, WHITE, hoverLink, "chevron")
        return tileRect
    }

    /** A 2-state pill toggle: cyan track + white knob when on, neutral inset when off. */
    fun switch(canvas: EditorCanvas, r: Rect, on: Boolean) {
        val track = if (on) SnellPalette.accent else SnellPalette.menuInset
        capsule(canvas, r, track, if (on) lighten(SnellPalette.accent, 0.2f) else rowBorder)
        val k = r.height - 4
        val kx = if (on) r.right - k - 2 else r.left + 2
        capsule(canvas, Rect(kx, r.top + 2, k, k), if (on) WHITE else SnellPalette.text3)
    }

    /** A thin track + cyan fill to [fraction] + white knob, with [valueText] right-aligned in accent. */
    fun slider(canvas: EditorCanvas, r: Rect, fraction: Float, valueText: String, valueW: Int = 0) {
        val f = fraction.coerceIn(0f, 1f)
        val vw = if (valueText.isEmpty()) 0 else (if (valueW > 0) valueW else canvas.monoWidth(valueText) + 6)
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
        if (vw > 0) canvas.drawMono(r.right - canvas.monoWidth(valueText), r.top + (r.height - canvas.lineHeight) / 2 + 1, valueText, SnellPalette.accent)
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
        surface(canvas, r, bg, if (selected || hover) SnellPalette.withAlpha(SnellPalette.accent, if (selected) 0x66 else 0x40) else rowBorder)
        if (selected) canvas.fill(r.left, r.top + 2, 3, r.height - 4, SnellPalette.accent)
    }

    /** A category-nav item (Options left rail): active gets a cyan left bar + fill. Caller draws icon+label. */
    fun categoryItem(canvas: EditorCanvas, r: Rect, active: Boolean, hover: Boolean) {
        if (active || hover) surface(canvas, r, if (active) rowFillHi else rowFill, null)
        if (active) canvas.fill(r.left, r.top + (r.height - 14) / 2, 3, 14, SnellPalette.accent)
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
        capsule(canvas, r, SnellPalette.withAlpha(c, 0x22), SnellPalette.withAlpha(c, 0x55))
        dot(canvas, r.left + 8, r.top + h / 2, 4, c)
        canvas.drawText(r.left + 13, r.top + (h - canvas.lineHeight) / 2, text, c)
    }

    /** A compact tinted chip (mode pill, "Rewards" badge). Returns its width for layout. */
    fun chip(canvas: EditorCanvas, x: Int, y: Int, text: String, color: Int): Int {
        val w = canvas.textWidth(text) + 10
        val h = canvas.lineHeight + 4
        surfaceSm(canvas, Rect(x, y, w, h), SnellPalette.withAlpha(color, 0x22), SnellPalette.withAlpha(color, 0x55))
        canvas.drawText(x + 5, y + (h - canvas.lineHeight) / 2, text, color)
        return w
    }

    /** A keybinding cap (mono-ish boxed key). */
    fun keyCap(canvas: EditorCanvas, r: Rect, text: String) {
        surfaceSm(canvas, r, rowFillHi, rowBorder)
        canvas.drawMono(r.left + (r.width - canvas.monoWidth(text)) / 2, r.top + (r.height - canvas.lineHeight) / 2, text, SnellPalette.text2)
    }

    /** Text input chrome: inset field, accent border when focused, placeholder/caret. */
    fun textField(canvas: EditorCanvas, r: Rect, text: String, focused: Boolean, placeholder: String = "") {
        surface(canvas, r, SnellPalette.menuInset, if (focused) SnellPalette.accent else rowBorder)
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

    private const val DISPLAY_SIZE = 62f // must equal the `size` in assets/snell/font/display.json

    /**
     * A crisp display heading (the SNELL wordmark). Renders the large native Geist display font scaled
     * DOWN to [pixelHeight] (downscaling stays sharp, unlike upscaling the 11px atlas) and inserts
     * manual letter-spacing per glyph (MC has no per-glyph tracking). ([x],[y]) is the top-left.
     */
    fun heading(canvas: EditorCanvas, x: Int, y: Int, text: String, pixelHeight: Int = 24, color: Int = SnellPalette.text, letterSpacingEm: Float = 0.16f) {
        val scale = pixelHeight / DISPLAY_SIZE
        val gap = (letterSpacingEm * DISPLAY_SIZE).toInt() // native-px spacing, scaled down with the glyphs
        canvas.withScale(scale, x, y) {
            var cx = 0
            for (ch in text) {
                val s = ch.toString()
                canvas.drawDisplay(cx, 0, s, color)
                cx += canvas.displayWidth(s) + gap
            }
        }
    }

}
