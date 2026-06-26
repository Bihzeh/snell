package gg.maeve.mod.editor

import gg.maeve.mod.config.HexColor
import gg.maeve.mod.module.HudModule
import gg.maeve.mod.module.HudStyle
import gg.maeve.mod.module.ModuleManager
import gg.maeve.mod.platform.EditorCanvas
import gg.maeve.mod.platform.GameContext
import gg.maeve.mod.render.HudModuleRender
import gg.maeve.shared.MaevePalette

/**
 * Draws the HUD editor across its three tiers with a modern, launcher-matched chrome built only
 * from the canvas primitives the game gives us (fill / outline / vertical gradient / text / scale):
 * soft drop shadows, gradient + bevel surfaces, real toggle switches, ellipsized labels and hover
 * states. POSITION: live preview + drag outlines + the MAEVE wordmark + a "Mods" button. GRID: a
 * card per module. CUSTOMIZE: a centered popup with one module's controls. Pure orchestration.
 */
class EditorRenderer {
    private val white = 0xFFFFFFFF.toInt()
    private val black = 0xFF000000.toInt()
    private val scrim = 0xD00B0A12.toInt()       // background-tinted dim
    private val shadow = 0x66000000

    fun render(
        canvas: EditorCanvas, screenW: Int, screenH: Int, mouseX: Int, mouseY: Int,
        ctx: GameContext, modules: ModuleManager, state: EditorState,
    ) {
        val measurer = object : TextMeasurer {
            override fun width(text: String) = canvas.textWidth(text)
            override val lineHeight get() = canvas.lineHeight
        }
        val all = modules.hudModules()
        for (m in all) HudModuleRender.draw(canvas, m, ctx)
        canvas.overlayStratum()

        val boxes = ElementLayout.boxesFor(all, ctx, measurer, screenW, screenH)
        state.pruneSelection(boxes)

        when (state.view) {
            EditorView.POSITION -> drawPosition(canvas, screenW, screenH, mouseX, mouseY, boxes, modules, state)
            EditorView.GRID -> { dim(canvas, screenW, screenH); drawGrid(canvas, screenW, screenH, mouseX, mouseY, modules) }
            EditorView.CUSTOMIZE -> {
                drawSelectedOutline(canvas, boxes, state)
                dim(canvas, screenW, screenH)
                drawCustomize(canvas, screenW, screenH, mouseX, mouseY, modules, state)
            }
        }
    }

    // ---- POSITION ----------------------------------------------------------
    private fun drawPosition(
        canvas: EditorCanvas, screenW: Int, screenH: Int, mouseX: Int, mouseY: Int,
        boxes: List<ElementBox>, modules: ModuleManager, state: EditorState,
    ) {
        val hover = hitTest(boxes, mouseX, mouseY)
        for (b in boxes) {
            val enabled = modules.hudById(b.id)?.enabled ?: true
            val color = when {
                b.id == state.selectedId -> MaevePalette.gold
                b.id == hover -> lighten(MaevePalette.primary, 0.25f)
                !enabled -> MaevePalette.error
                else -> MaevePalette.outline
            }
            canvas.border(b.rect.left - 2, b.rect.top - 2, b.rect.width + 4, b.rect.height + 4, color)
        }
        for (gx in state.activeGuidesX) canvas.fill(gx, 0, 1, screenH, MaevePalette.gold)       // alignment guides
        for (gy in state.activeGuidesY) canvas.fill(0, gy, screenW, 1, MaevePalette.gold)
        state.selectedId?.let { sid ->                                                            // resize grip
            boxes.firstOrNull { it.id == sid }?.rect?.let { b ->
                val h = PositionLayout.resizeHandle(b)
                canvas.fill(h.left, h.top, h.width, h.height, MaevePalette.gold)
                canvas.border(h.left, h.top, h.width, h.height, white)
            }
        }
        val hint = "Drag to reposition  ·  Mods to customize  ·  Esc / Done to save"
        canvas.drawText((screenW - canvas.textWidth(hint)) / 2, screenH - 16, hint, MaevePalette.text2) // bottom-center, clear of corner HUD
        drawLogo(canvas, screenW, screenH)
        val mods = PositionLayout.modsButton(screenW, screenH)
        val done = PositionLayout.doneButton(screenW, screenH)
        button(canvas, mods, "Mods", primary = true, hover = mods.contains(mouseX, mouseY))
        button(canvas, done, "Done", primary = false, hover = done.contains(mouseX, mouseY))
        val snap = PositionLayout.snapButton(screenW, screenH)
        button(canvas, snap, if (modules.snapEnabled()) "Snap: On" else "Snap: Off", primary = false, hover = snap.contains(mouseX, mouseY))
    }

    private fun drawLogo(canvas: EditorCanvas, screenW: Int, screenH: Int) {
        for (b in LogoArt.bands(PositionLayout.logoRect(screenW, screenH))) {
            canvas.fill(b.rect.left, b.rect.top, b.rect.width, b.rect.height, b.color)
        }
    }

    // ---- GRID --------------------------------------------------------------
    private fun drawGrid(canvas: EditorCanvas, screenW: Int, screenH: Int, mouseX: Int, mouseY: Int, modules: ModuleManager) {
        val mods = modules.all().toList()
        val p = GridLayout.panelRect(screenW, screenH, mods.size)
        panel(canvas, p)
        header(canvas, p, GridLayout.HEADER, "MODULES")
        val back = GridLayout.backButton(screenW, screenH, mods.size)
        button(canvas, back, "< Back", primary = false, hover = back.contains(mouseX, mouseY))
        val cards = GridLayout.cards(screenW, screenH, mods.size)
        for (i in mods.indices) {
            val hover = cards[i].contains(mouseX, mouseY)
            drawCard(canvas, cards[i], mods[i].displayName, mods[i].enabled, hover)
        }
    }

    private fun drawCard(canvas: EditorCanvas, r: Rect, name: String, enabled: Boolean, hover: Boolean) {
        val borderC = if (hover) lighten(MaevePalette.primary, 0.25f) else MaevePalette.outline
        card(canvas, r, MaevePalette.elevated, borderC)
        canvas.fill(r.left + 1, r.top + 1, 3, r.height - 2, if (enabled) MaevePalette.primary else MaevePalette.outline) // accent strip
        val innerL = r.left + 11
        canvas.drawText(innerL, r.top + 9, ellipsize(canvas, name, r.width - 22), if (enabled) white else MaevePalette.text2)
        val ts = GridLayout.toggleSwitch(r) // shared hit-area: switch widget + its label
        switch(canvas, Rect(ts.left, ts.top, 26, 13), enabled)
        canvas.drawText(ts.left + 32, ts.top + 3, if (enabled) "On" else "Off", MaevePalette.text2)
        canvas.drawText(r.right - 12, r.top + (r.height - canvas.lineHeight) / 2, ">", MaevePalette.text2) // opens affordance
    }

    // ---- CUSTOMIZE ---------------------------------------------------------
    private fun drawSelectedOutline(canvas: EditorCanvas, boxes: List<ElementBox>, state: EditorState) {
        val sel = state.customizing ?: return
        val b = boxes.firstOrNull { it.id == sel }?.rect ?: return
        canvas.border(b.left - 2, b.top - 2, b.width + 4, b.height + 4, MaevePalette.gold)
    }

    private fun drawCustomize(canvas: EditorCanvas, screenW: Int, screenH: Int, mouseX: Int, mouseY: Int, modules: ModuleManager, state: EditorState) {
        val sel = state.customizing ?: return
        val module = modules.byId(sel) ?: return
        val hud = modules.hudById(sel)
        val popup = CustomizeLayout.popupRect(screenW, screenH, hud != null, hud?.colorTargets()?.size ?: 0, hud?.toggles?.size ?: 0)
        panel(canvas, popup)
        round(canvas, popup, scrim)
        val hh = CustomizeLayout.TITLE_H
        canvas.gradientV(popup.left + 1, popup.top + 1, popup.width - 2, hh - 2, lighten(MaevePalette.elevated, 0.12f), MaevePalette.elevated)
        canvas.fill(popup.left + 1, popup.top + hh - 1, popup.width - 2, 1, MaevePalette.outline)
        canvas.fill(popup.left + CustomizeLayout.PAD, popup.top + (hh - 7) / 2, 4, 7, MaevePalette.gold) // accent tab
        canvas.drawText(popup.left + CustomizeLayout.PAD + 9, popup.top + (hh - canvas.lineHeight) / 2, ellipsize(canvas, module.displayName, popup.width - 50), MaevePalette.text)
        val cb = CustomizeLayout.closeButton(popup)
        button(canvas, cb, "x", primary = false, hover = cb.contains(mouseX, mouseY)); round(canvas, cb, MaevePalette.elevated)

        if (hud != null) {
            drawStyleControls(canvas, hud, popup, state, mouseX, mouseY)
        } else {
            val en = CustomizeLayout.enableToggle(popup)
            canvas.drawText(en.left, en.top + 3, if (module.enabled) "Enabled" else "Disabled", if (module.enabled) white else MaevePalette.text2)
            switch(canvas, Rect(en.right - 28, en.top + 1, 26, en.height - 2), module.enabled)
        }
    }

    private fun drawStyleControls(canvas: EditorCanvas, module: HudModule, popup: Rect, state: EditorState, mouseX: Int, mouseY: Int) {
        val st = module.style
        val targets = module.colorTargets()
        val tc = targets.size
        val oc = module.toggles.size
        val active = module.targetColor(state.selectedTargetKey) // the colour the picker is editing
        val c = CustomizeLayout.controls(popup, tc, oc).associateBy { it.id }

        c["preview"]?.rect?.let { r ->
            checker(canvas, r.left, r.top, r.width, r.height)
            canvas.fill(r.left, r.top, r.width, r.height, active)
            canvas.border(r.left, r.top, r.width, r.height, MaevePalette.outline)
        }
        c["sv"]?.rect?.let { r -> drawSvSquare(canvas, r, state) }
        c["hue"]?.rect?.let { r -> drawHueBar(canvas, r, state) }
        c["alpha"]?.rect?.let { r -> drawAlphaBar(canvas, r, state) }
        c["hex"]?.rect?.let { r ->
            canvas.fill(r.left, r.top, r.width, r.height, darken(MaevePalette.elevated, 0.12f))
            canvas.border(r.left, r.top, r.width, r.height, if (state.isHexFocused) MaevePalette.gold else MaevePalette.outline)
            round(canvas, r, MaevePalette.surface)
            val text = if (state.isHexFocused) "#" + state.hexText + "_" else HexColor.encode(active)
            canvas.drawText(r.left + 5, r.top + (r.height - canvas.lineHeight) / 2 + 1, text, MaevePalette.text)
        }

        val (colCap, optCap, styCap) = CustomizeLayout.captions(popup, tc, oc)
        sectionLabel(canvas, colCap, "COLOUR")
        val chips = CustomizeLayout.targetChips(popup, tc)
        targets.forEachIndexed { i, t ->
            val r = chips[i]; val on = t.key == state.selectedTargetKey; val hov = r.contains(mouseX, mouseY)
            val bg = when { on -> blend(MaevePalette.elevated, MaevePalette.gold, 0.16f); hov -> lighten(MaevePalette.elevated, 0.12f); else -> MaevePalette.elevated }
            canvas.fill(r.left, r.top, r.width, r.height, bg)
            canvas.border(r.left, r.top, r.width, r.height, if (on) MaevePalette.gold else MaevePalette.outline)
            round(canvas, r, MaevePalette.surface)
            canvas.drawText(r.left + 7, r.top + (r.height - canvas.lineHeight) / 2 + 1, t.label, if (on) white else MaevePalette.text2)
            val sw = 14; val sx = r.right - sw - 6; val sy = r.top + (r.height - sw) / 2
            checker(canvas, sx, sy, sw, sw)
            canvas.fill(sx, sy, sw, sw, module.targetColor(t.key)) // full ARGB so translucency shows
            canvas.border(sx, sy, sw, sw, MaevePalette.outline)
        }

        if (oc > 0) {
            sectionLabel(canvas, optCap, "OPTIONS")
            val rows = CustomizeLayout.optionRows(popup, tc, oc)
            module.toggles.forEachIndexed { i, t ->
                val r = rows[i]; rowHover(canvas, r, mouseX, mouseY); val on = module.option(t.key)
                canvas.drawText(r.left + 6, r.top + (r.height - canvas.lineHeight) / 2 + 1, t.label, if (on) white else MaevePalette.text2)
                switch(canvas, Rect(r.right - 26, r.top + 2, 24, r.height - 4), on)
            }
        }

        sectionLabel(canvas, styCap, "STYLE")
        for (id in TOGGLE_ROW) {
            val r = c[id]?.rect ?: continue
            rowHover(canvas, r, mouseX, mouseY); val on = toggleState(id, module, st)
            canvas.drawText(r.left + 6, r.top + (r.height - canvas.lineHeight) / 2 + 1, label(id), if (on) white else MaevePalette.text2)
            switch(canvas, Rect(r.right - 26, r.top + 2, 24, r.height - 4), on)
        }
        val sm = c["scale-"]?.rect; val sp = c["scale+"]?.rect
        if (sm != null && sp != null) {
            button(canvas, sm, "-", hover = sm.contains(mouseX, mouseY)); round(canvas, sm, MaevePalette.surface)
            button(canvas, sp, "+", hover = sp.contains(mouseX, mouseY)); round(canvas, sp, MaevePalette.surface)
            val v = "x%.2f".format(st.scale)
            canvas.drawText((sm.right + sp.left) / 2 - canvas.textWidth(v) / 2, sm.top + (sm.height - canvas.lineHeight) / 2 + 1, v, MaevePalette.text2)
        }
        c["reset"]?.rect?.let { button(canvas, it, "Reset", hover = it.contains(mouseX, mouseY)); round(canvas, it, MaevePalette.surface) }

        CustomizeLayout.SWATCHES.forEachIndexed { i, col ->
            c["swatch:$i"]?.rect?.let { r ->
                canvas.fill(r.left, r.top, r.width, r.height, black or MaeveColor.rgbOf(col))
                val selected = MaeveColor.rgbOf(active) == MaeveColor.rgbOf(col)
                if (selected) canvas.border(r.left - 1, r.top - 1, r.width + 2, r.height + 2, MaevePalette.gold)
                else canvas.border(r.left, r.top, r.width, r.height, MaevePalette.outline)
            }
        }
    }

    private fun sectionLabel(canvas: EditorCanvas, cap: Rect, text: String) {
        canvas.drawText(cap.left + 1, cap.top, text, MaevePalette.text2)
        canvas.fill(cap.left, cap.bottom - 2, cap.width, 1, MaevePalette.outline)
    }

    private fun rowHover(canvas: EditorCanvas, r: Rect, mouseX: Int, mouseY: Int) {
        if (r.contains(mouseX, mouseY)) canvas.fill(r.left, r.top, r.width, r.height, lighten(MaevePalette.surface, 0.07f))
    }

    /** Knock ~2px corners to [bg] so a panel/chip/button reads as rounded. */
    private fun round(canvas: EditorCanvas, r: Rect, bg: Int) {
        canvas.fill(r.left, r.top, 2, 1, bg); canvas.fill(r.left, r.top, 1, 2, bg)
        canvas.fill(r.right - 2, r.top, 2, 1, bg); canvas.fill(r.right - 1, r.top, 1, 2, bg)
        canvas.fill(r.left, r.bottom - 1, 2, 1, bg); canvas.fill(r.left, r.bottom - 2, 1, 2, bg)
        canvas.fill(r.right - 2, r.bottom - 1, 2, 1, bg); canvas.fill(r.right - 1, r.bottom - 2, 1, 2, bg)
    }

    private fun toggleState(id: String, module: HudModule, st: HudStyle) = when (id) {
        "visible" -> module.enabled; "bold" -> st.bold; "italic" -> st.italic
        "underline" -> st.underline; "strike" -> st.strikethrough; "shadow" -> st.shadow
        "background" -> st.background; else -> false
    }

    private fun drawSvSquare(canvas: EditorCanvas, r: Rect, state: EditorState) {
        val denom = (r.width - 1).coerceAtLeast(1)
        for (x in 0 until r.width) {
            val s = x.toFloat() / denom
            val top = black or MaeveColor.hsvToRgb(state.colorH, s, 1f)
            canvas.gradientV(r.left + x, r.top, 1, r.height, top, black)
        }
        val mx = (r.left + (state.colorS * r.width).toInt()).coerceIn(r.left, r.right - 1)
        val my = (r.top + ((1f - state.colorV) * r.height).toInt()).coerceIn(r.top, r.bottom - 1)
        canvas.border(mx - 2, my - 2, 4, 4, white)
        canvas.border(mx - 3, my - 3, 6, 6, black)
    }

    private fun drawHueBar(canvas: EditorCanvas, r: Rect, state: EditorState) {
        for (i in 0 until 6) {
            val y0 = r.top + i * r.height / 6
            val y1 = r.top + (i + 1) * r.height / 6
            val top = black or MaeveColor.hsvToRgb(i * 60f, 1f, 1f)
            val bottom = black or MaeveColor.hsvToRgb((i + 1) * 60f, 1f, 1f)
            canvas.gradientV(r.left, y0, r.width, y1 - y0, top, bottom)
        }
        val my = (r.top + (state.colorH / 360f * r.height).toInt()).coerceIn(r.top, r.bottom - 1)
        canvas.border(r.left - 1, my - 1, r.width + 2, 3, white)
    }

    private fun drawAlphaBar(canvas: EditorCanvas, r: Rect, state: EditorState) {
        checker(canvas, r.left, r.top, r.width, r.height)
        val rgb = MaeveColor.hsvToRgb(state.colorH, state.colorS, state.colorV)
        canvas.gradientV(r.left, r.top, r.width, r.height, black or rgb, rgb)
        canvas.border(r.left, r.top, r.width, r.height, MaevePalette.outline)
        val my = (r.top + ((1f - state.colorA / 255f) * r.height).toInt()).coerceIn(r.top, r.bottom - 1)
        canvas.border(r.left - 1, my - 1, r.width + 2, 3, white)
    }

    private fun checker(canvas: EditorCanvas, x: Int, y: Int, w: Int, h: Int) {
        canvas.fill(x, y, w, h, 0xFFBBBBBB.toInt())
        val s = 4; var yy = 0
        while (yy < h) {
            var xx = 0
            while (xx < w) {
                if (((xx / s) + (yy / s)) % 2 == 0) canvas.fill(x + xx, y + yy, minOf(s, w - xx), minOf(s, h - yy), 0xFF777777.toInt())
                xx += s
            }
            yy += s
        }
    }

    // ---- UI kit ------------------------------------------------------------
    private fun dim(canvas: EditorCanvas, screenW: Int, screenH: Int) = canvas.fill(0, 0, screenW, screenH, scrim)

    private fun panel(canvas: EditorCanvas, r: Rect) {
        canvas.fill(r.left + 4, r.bottom, r.width, 4, shadow)
        canvas.fill(r.right, r.top + 4, 4, r.height, shadow)
        canvas.gradientV(r.left, r.top, r.width, r.height, lighten(MaevePalette.surface, 0.05f), MaevePalette.surface)
        canvas.border(r.left, r.top, r.width, r.height, MaevePalette.outline)
    }

    private fun header(canvas: EditorCanvas, panel: Rect, headerH: Int, title: String) {
        canvas.gradientV(panel.left + 1, panel.top + 1, panel.width - 2, headerH - 1, lighten(MaevePalette.elevated, 0.10f), MaevePalette.elevated)
        canvas.fill(panel.left + 1, panel.top + headerH, panel.width - 2, 1, MaevePalette.outline)
        val tw = canvas.textWidth(title)
        canvas.drawText(panel.left + (panel.width - tw) / 2, panel.top + (headerH - canvas.lineHeight) / 2, title, MaevePalette.gold)
    }

    private fun card(canvas: EditorCanvas, r: Rect, base: Int, borderC: Int) {
        canvas.fill(r.left + 2, r.bottom, r.width, 2, shadow)
        canvas.fill(r.right, r.top + 2, 2, r.height, shadow)
        canvas.gradientV(r.left, r.top, r.width, r.height, lighten(base, 0.14f), base)
        canvas.fill(r.left + 1, r.top, r.width - 2, 1, lighten(base, 0.30f))      // top bevel
        canvas.fill(r.left + 1, r.bottom - 1, r.width - 2, 1, darken(base, 0.30f)) // bottom shade
        canvas.border(r.left, r.top, r.width, r.height, borderC)
    }

    private fun button(canvas: EditorCanvas, r: Rect, text: String, primary: Boolean = false, hover: Boolean = false) {
        val base0 = if (primary) MaevePalette.primary else MaevePalette.elevated
        val base = if (hover) lighten(base0, 0.14f) else base0
        canvas.fill(r.left + 1, r.bottom, r.width, 2, shadow)
        canvas.gradientV(r.left, r.top, r.width, r.height, lighten(base, 0.16f), base)
        canvas.fill(r.left + 1, r.top, r.width - 2, 1, lighten(base, 0.30f))
        canvas.border(r.left, r.top, r.width, r.height, if (primary) lighten(MaevePalette.primary, 0.15f) else MaevePalette.outline)
        val tw = canvas.textWidth(text)
        canvas.drawText(r.left + (r.width - tw) / 2, r.top + (r.height - canvas.lineHeight) / 2 + 1, text, white)
    }

    private fun switch(canvas: EditorCanvas, r: Rect, on: Boolean) {
        val track = if (on) MaevePalette.primary else darken(MaevePalette.elevated, 0.18f)
        canvas.gradientV(r.left, r.top, r.width, r.height, lighten(track, 0.18f), track)
        canvas.border(r.left, r.top, r.width, r.height, if (on) lighten(MaevePalette.primary, 0.2f) else MaevePalette.outline)
        val k = r.height - 4
        val kx = if (on) r.right - k - 2 else r.left + 2
        canvas.fill(kx, r.top + 2, k, k, white)
    }

    private fun ellipsize(canvas: EditorCanvas, text: String, maxW: Int): String {
        if (canvas.textWidth(text) <= maxW) return text
        var t = text
        while (t.isNotEmpty() && canvas.textWidth("$t…") > maxW) t = t.dropLast(1)
        return t.trimEnd() + "…"
    }

    private fun label(id: String) = when (id) {
        "visible" -> "Enabled"; "bold" -> "Bold"; "italic" -> "Italic"; "underline" -> "Underline"
        "strike" -> "Strikethrough"; "shadow" -> "Shadow"; "background" -> "Background"; else -> id
    }

    private fun blend(a: Int, b: Int, f: Float): Int {
        val ar = (a ushr 16) and 0xFF; val ag = (a ushr 8) and 0xFF; val ab = a and 0xFF
        val br = (b ushr 16) and 0xFF; val bg = (b ushr 8) and 0xFF; val bb = b and 0xFF
        val rr = (ar + (br - ar) * f).toInt().coerceIn(0, 255)
        val gg = (ag + (bg - ag) * f).toInt().coerceIn(0, 255)
        val bl = (ab + (bb - ab) * f).toInt().coerceIn(0, 255)
        return (a.toLong() and 0xFF000000L).toInt() or (rr shl 16) or (gg shl 8) or bl
    }

    private fun lighten(c: Int, f: Float) = blend(c, white, f)
    private fun darken(c: Int, f: Float) = blend(c, black, f)

    private companion object {
        val TOGGLE_ROW = listOf("visible", "bold", "italic")
    }
}
