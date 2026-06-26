package gg.maeve.mod.editor

import gg.maeve.mod.config.HexColor
import gg.maeve.mod.module.HudModule
import gg.maeve.mod.module.ModuleManager
import gg.maeve.mod.platform.EditorCanvas
import gg.maeve.mod.platform.GameContext
import gg.maeve.mod.render.HudModuleRender
import gg.maeve.shared.MaevePalette

/**
 * Draws the HUD editor across its three tiers. POSITION: a live preview with drag outlines, the
 * Maeve wordmark and a "Mods" button. GRID: a card per module. CUSTOMIZE: a centered popup with
 * one module's controls (full HSV style panel for HUD modules; an enable toggle otherwise).
 * Pure orchestration over [EditorCanvas].
 */
class EditorRenderer {
    private val white = 0xFFFFFFFF.toInt()
    private val black = 0xFF000000.toInt()
    private val scrim = 0xC0000000.toInt()

    fun render(
        canvas: EditorCanvas, screenW: Int, screenH: Int, mouseX: Int, mouseY: Int,
        ctx: GameContext, modules: ModuleManager, state: EditorState,
    ) {
        val measurer = object : TextMeasurer {
            override fun width(text: String) = canvas.textWidth(text)
            override val lineHeight get() = canvas.lineHeight
        }
        val all = modules.hudModules()
        for (m in all) HudModuleRender.draw(canvas, m, m.render(ctx))
        canvas.overlayStratum()

        val boxes = ElementLayout.boxesFor(all, ctx, measurer, screenW, screenH)
        state.pruneSelection(boxes)

        when (state.view) {
            EditorView.POSITION -> drawPosition(canvas, screenW, screenH, mouseX, mouseY, boxes, modules, state)
            EditorView.GRID -> { dim(canvas, screenW, screenH); drawGrid(canvas, screenW, screenH, modules) }
            EditorView.CUSTOMIZE -> {
                drawSelectedOutline(canvas, boxes, state)
                dim(canvas, screenW, screenH)
                drawCustomize(canvas, screenW, screenH, modules, state)
            }
        }
    }

    private fun drawPosition(
        canvas: EditorCanvas, screenW: Int, screenH: Int, mouseX: Int, mouseY: Int,
        boxes: List<ElementBox>, modules: ModuleManager, state: EditorState,
    ) {
        val hover = hitTest(boxes, mouseX, mouseY)
        for (b in boxes) {
            val enabled = modules.hudById(b.id)?.enabled ?: true
            val color = when {
                b.id == state.selectedId -> MaevePalette.gold
                b.id == hover -> white
                !enabled -> MaevePalette.error
                else -> MaevePalette.outline
            }
            canvas.border(b.rect.left - 1, b.rect.top - 1, b.rect.width + 2, b.rect.height + 2, color)
        }
        canvas.drawText(6, 6, "Drag to reposition · Mods to customize · Esc/Done to save", MaevePalette.text)
        drawLogo(canvas, screenW, screenH)
        button(canvas, PositionLayout.modsButton(screenW, screenH), "Mods", false)
        button(canvas, PositionLayout.doneButton(screenW, screenH), "Done", false)
    }

    private fun drawLogo(canvas: EditorCanvas, screenW: Int, screenH: Int) {
        val r = PositionLayout.logoRect(screenW, screenH)
        val cx = r.left + r.width / 2
        val cy = r.top + r.height / 2
        val text = "MAEVE"
        val tw = canvas.textWidth(text)
        canvas.withScale(PositionLayout.LOGO_SCALE, cx, cy) {
            canvas.drawText(-tw / 2, -canvas.lineHeight / 2, text, MaevePalette.gold)
        }
        val ruleW = (tw * PositionLayout.LOGO_SCALE).toInt()
        canvas.fill(cx - ruleW / 2, r.bottom, ruleW, 1, MaevePalette.gold)
    }

    private fun drawGrid(canvas: EditorCanvas, screenW: Int, screenH: Int, modules: ModuleManager) {
        val mods = modules.all().toList()
        val panel = GridLayout.panelRect(screenW, screenH, mods.size)
        canvas.fill(panel.left, panel.top, panel.width, panel.height, MaevePalette.surface)
        canvas.border(panel.left, panel.top, panel.width, panel.height, MaevePalette.gold)
        canvas.drawText(panel.left + 56, panel.top + 6, "Modules", MaevePalette.gold)
        button(canvas, GridLayout.backButton(screenW, screenH, mods.size), "Back", false)
        val cards = GridLayout.cards(screenW, screenH, mods.size)
        for (i in mods.indices) drawCard(canvas, cards[i], mods[i].displayName, mods[i].enabled)
    }

    private fun drawCard(canvas: EditorCanvas, r: Rect, name: String, enabled: Boolean) {
        canvas.fill(r.left, r.top, r.width, r.height, MaevePalette.elevated)
        canvas.border(r.left, r.top, r.width, r.height, if (enabled) MaevePalette.primary else MaevePalette.outline)
        canvas.drawText(r.left + 6, r.top + 6, name, white)
        val pillW = 30; val pillH = 12
        val px = r.left + 6; val py = r.bottom - pillH - 5
        canvas.fill(px, py, pillW, pillH, if (enabled) MaevePalette.primary else MaevePalette.surface)
        canvas.border(px, py, pillW, pillH, MaevePalette.outline)
        canvas.drawText(px + 5, py + 2, if (enabled) "ON" else "OFF", white)
    }

    private fun drawSelectedOutline(canvas: EditorCanvas, boxes: List<ElementBox>, state: EditorState) {
        val sel = state.customizing ?: return
        val b = boxes.firstOrNull { it.id == sel }?.rect ?: return
        canvas.border(b.left - 1, b.top - 1, b.width + 2, b.height + 2, MaevePalette.gold)
    }

    private fun drawCustomize(canvas: EditorCanvas, screenW: Int, screenH: Int, modules: ModuleManager, state: EditorState) {
        val sel = state.customizing ?: return
        val module = modules.byId(sel) ?: return
        val hud = modules.hudById(sel)
        val popup = CustomizeLayout.popupRect(screenW, screenH, hud != null)
        canvas.fill(popup.left, popup.top, popup.width, popup.height, MaevePalette.surface)
        canvas.border(popup.left, popup.top, popup.width, popup.height, MaevePalette.gold)
        canvas.drawText(popup.left + 10, popup.top + 6, module.displayName, MaevePalette.gold)
        val cb = CustomizeLayout.closeButton(popup)
        canvas.fill(cb.left, cb.top, cb.width, cb.height, MaevePalette.elevated)
        canvas.border(cb.left, cb.top, cb.width, cb.height, MaevePalette.outline)
        canvas.drawText(cb.left + 3, cb.top + 2, "x", white)

        if (hud != null) {
            drawStyleControls(canvas, hud, popup, state)
        } else {
            val en = CustomizeLayout.enableToggle(popup)
            canvas.fill(en.left, en.top, en.width, en.height, if (module.enabled) MaevePalette.primary else MaevePalette.elevated)
            canvas.border(en.left, en.top, en.width, en.height, MaevePalette.outline)
            canvas.drawText(en.left + 4, en.top + 3, if (module.enabled) "Enabled" else "Disabled", white)
        }
    }

    private fun drawStyleControls(canvas: EditorCanvas, module: HudModule, popup: Rect, state: EditorState) {
        val st = module.style
        val controls = CustomizeLayout.controls(popup).associateBy { it.id }
        controls["preview"]?.rect?.let { r ->
            checker(canvas, r.left, r.top, r.width, r.height)
            canvas.fill(r.left, r.top, r.width, r.height, st.color)
            canvas.border(r.left, r.top, r.width, r.height, MaevePalette.outline)
        }
        controls["sv"]?.rect?.let { r -> drawSvSquare(canvas, r, state) }
        controls["hue"]?.rect?.let { r -> drawHueBar(canvas, r, state) }
        controls["alpha"]?.rect?.let { r -> drawAlphaBar(canvas, r, state) }
        controls["hex"]?.rect?.let { r ->
            canvas.fill(r.left, r.top, r.width, r.height, MaevePalette.elevated)
            canvas.border(r.left, r.top, r.width, r.height, if (state.isHexFocused) MaevePalette.gold else MaevePalette.outline)
            val text = if (state.isHexFocused) "#" + state.hexText + "_" else HexColor.encode(st.color)
            canvas.drawText(r.left + 3, r.top + 2, text, MaevePalette.text)
        }
        for ((id, c) in controls) {
            val r = c.rect
            when {
                id == "visible" || id in CustomizeLayout.TOGGLES -> {
                    val on = when (id) {
                        "visible" -> module.enabled; "bold" -> st.bold; "italic" -> st.italic
                        "underline" -> st.underline; "strike" -> st.strikethrough; "shadow" -> st.shadow
                        "background" -> st.background; else -> false
                    }
                    canvas.fill(r.left, r.top, r.width, r.height, if (on) MaevePalette.primary else MaevePalette.elevated)
                    canvas.border(r.left, r.top, r.width, r.height, MaevePalette.outline)
                    canvas.drawText(r.left + 4, r.top + 3, label(id), white)
                }
                id == "scale-" || id == "scale+" -> {
                    canvas.fill(r.left, r.top, r.width, r.height, MaevePalette.elevated)
                    canvas.border(r.left, r.top, r.width, r.height, MaevePalette.outline)
                    canvas.drawText(r.left + 7, r.top + 3, if (id == "scale-") "-" else "+", white)
                    if (id == "scale+") canvas.drawText(r.left - 34, r.top + 3, "x%.2f".format(st.scale), MaevePalette.text2)
                }
                id == "reset" -> {
                    canvas.fill(r.left, r.top, r.width, r.height, MaevePalette.elevated)
                    canvas.border(r.left, r.top, r.width, r.height, MaevePalette.outline)
                    canvas.drawText(r.left + 4, r.top + 3, "Reset style", white)
                }
                id.startsWith("swatch:") -> {
                    val idx = id.removePrefix("swatch:").toInt()
                    canvas.fill(r.left, r.top, r.width, r.height, black or MaeveColor.rgbOf(CustomizeLayout.SWATCHES[idx]))
                    canvas.border(r.left, r.top, r.width, r.height, MaevePalette.outline)
                }
            }
        }
    }

    private fun drawSvSquare(canvas: EditorCanvas, r: Rect, state: EditorState) {
        // exact: for fixed (h,s), rgb(h,s,v) = v * rgb(h,s,1), so each column is a gradient to black
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
            val y0 = r.top + i * r.height / 6        // boundary-based so the remainder never leaves a gap
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
        canvas.gradientV(r.left, r.top, r.width, r.height, black or rgb, rgb) // bottom alpha 0
        canvas.border(r.left, r.top, r.width, r.height, MaevePalette.outline)
        val my = (r.top + ((1f - state.colorA / 255f) * r.height).toInt()).coerceIn(r.top, r.bottom - 1)
        canvas.border(r.left - 1, my - 1, r.width + 2, 3, white)
    }

    private fun checker(canvas: EditorCanvas, x: Int, y: Int, w: Int, h: Int) {
        canvas.fill(x, y, w, h, 0xFFBBBBBB.toInt())
        val s = 4
        var yy = 0
        while (yy < h) {
            var xx = 0
            while (xx < w) {
                if (((xx / s) + (yy / s)) % 2 == 0) {
                    canvas.fill(x + xx, y + yy, minOf(s, w - xx), minOf(s, h - yy), 0xFF777777.toInt())
                }
                xx += s
            }
            yy += s
        }
    }

    private fun dim(canvas: EditorCanvas, screenW: Int, screenH: Int) {
        canvas.fill(0, 0, screenW, screenH, scrim)
    }

    private fun button(canvas: EditorCanvas, r: Rect, text: String, active: Boolean) {
        canvas.fill(r.left, r.top, r.width, r.height, if (active) MaevePalette.primary else MaevePalette.elevated)
        canvas.border(r.left, r.top, r.width, r.height, MaevePalette.outline)
        canvas.drawText(r.left + 6, r.top + 4, text, white)
    }

    private fun label(id: String) = when (id) {
        "visible" -> "Enabled"; "bold" -> "Bold"; "italic" -> "Italic"; "underline" -> "Underline"
        "strike" -> "Strikethrough"; "shadow" -> "Shadow"; "background" -> "Background"; else -> id
    }
}
