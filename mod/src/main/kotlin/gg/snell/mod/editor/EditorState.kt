package gg.snell.mod.editor

import gg.snell.mod.config.HexColor
import gg.snell.mod.module.ModuleManager
import kotlin.math.roundToInt

/** The three editor tiers. POSITION: drag modules + Snell logo + "Mods" button (no styling).
 *  GRID: a card per module reached from "Mods". CUSTOMIZE: a centered popup of one module's
 *  available customizations. Right-Shift opens POSITION; the user drills inward via "Mods". */
enum class EditorView { POSITION, GRID, CUSTOMIZE }

/**
 * Pure, immediate-mode editor interaction state. Drives the editor screen from raw mouse/char
 * events without any Minecraft types, so it is fully unit-testable. A small view machine
 * (POSITION -> GRID -> CUSTOMIZE) gates which interactions are live. In CUSTOMIZE it holds the
 * live HSVA being edited for the selected element's color. Control mutations go through
 * ModuleManager setters (live preview); the screen persists once on close.
 */
class EditorState {
    var view: EditorView = EditorView.POSITION
        private set
    var selectedId: String? = null
        private set
    var dirty: Boolean = false
        private set
    var closeRequested: Boolean = false
        private set

    /** The module whose customization popup is open (null unless in CUSTOMIZE). */
    val customizing: String? get() = if (view == EditorView.CUSTOMIZE) selectedId else null

    private var dragId: String? = null
    private var startMouseX = 0
    private var startMouseY = 0
    private var startLeft = 0
    private var startTop = 0
    private var dragW = 0
    private var dragH = 0
    private var resizeId: String? = null
    private var startScale = 1f
    private var startD = 1.0
    private var startFootW = 0
    private var startFootH = 0
    private var startSnapX = emptyList<Int>()
    private var startSnapY = emptyList<Int>()

    /** Alignment guide lines to draw this frame while snapping (empty unless mid-snap-drag). */
    var activeGuidesX: List<Int> = emptyList()
        private set
    var activeGuidesY: List<Int> = emptyList()
        private set

    private var editH = 0f
    private var editS = 0f
    private var editV = 0f
    private var editA = 255
    private var activeColor: String? = null // "sv" | "hue" | "alpha" while dragging a picker
    private var hexFocused = false
    private var hexBuffer = ""
    var selectedTargetKey: String = "style"
        private set

    val colorH get() = editH
    val colorS get() = editS
    val colorV get() = editV
    val colorA get() = editA
    val isHexFocused get() = hexFocused
    val hexText get() = hexBuffer

    fun onPress(mouseX: Int, mouseY: Int, screenW: Int, screenH: Int, boxes: List<ElementBox>, modules: ModuleManager): Boolean =
        when (view) {
            EditorView.POSITION -> onPositionPress(mouseX, mouseY, screenW, screenH, boxes, modules)
            EditorView.GRID -> onGridPress(mouseX, mouseY, screenW, screenH, modules)
            EditorView.CUSTOMIZE -> onCustomizePress(mouseX, mouseY, screenW, screenH, modules)
        }

    private fun onPositionPress(mouseX: Int, mouseY: Int, screenW: Int, screenH: Int, boxes: List<ElementBox>, modules: ModuleManager): Boolean {
        // Resize: grab the selected element's corner handle FIRST (it can overlap a button zone).
        val selBox = selectedId?.let { sid -> boxes.firstOrNull { it.id == sid }?.rect }
        if (selBox != null && PositionLayout.resizeHandle(selBox).contains(mouseX, mouseY)) {
            resizeId = selectedId
            startScale = modules.hudById(selectedId!!)?.style?.scale ?: 1f
            startLeft = selBox.left; startTop = selBox.top
            startFootW = selBox.width; startFootH = selBox.height
            startMouseX = mouseX; startMouseY = mouseY
            startD = kotlin.math.hypot((mouseX - selBox.left).toDouble(), (mouseY - selBox.top).toDouble()).coerceAtLeast(1.0)
            return true
        }
        if (PositionLayout.doneButton(screenW, screenH).contains(mouseX, mouseY)) { closeRequested = true; return true }
        if (PositionLayout.modsButton(screenW, screenH).contains(mouseX, mouseY)) { view = EditorView.GRID; return true }
        if (PositionLayout.snapButton(screenW, screenH).contains(mouseX, mouseY)) {
            modules.setSnapEnabled(!modules.snapEnabled()); dirty = true; return true
        }
        val id = hitTest(boxes, mouseX, mouseY)
        selectedId = id
        if (id == null) { dragId = null; return false }
        val box = boxes.first { it.id == id }.rect
        dragId = id
        startMouseX = mouseX; startMouseY = mouseY
        startLeft = box.left; startTop = box.top
        dragW = box.width; dragH = box.height
        // Capture sibling edges/centres now (they don't move during this drag) for alignment snapping.
        startSnapX = boxes.filter { it.id != id }.flatMap { listOf(it.rect.left, it.rect.left + it.rect.width / 2, it.rect.right) }
        startSnapY = boxes.filter { it.id != id }.flatMap { listOf(it.rect.top, it.rect.top + it.rect.height / 2, it.rect.bottom) }
        return true
    }

    private fun onGridPress(mouseX: Int, mouseY: Int, screenW: Int, screenH: Int, modules: ModuleManager): Boolean {
        val mods = modules.all().toList()
        if (GridLayout.backButton(screenW, screenH, mods.size).contains(mouseX, mouseY)) { view = EditorView.POSITION; return true }
        val cards = GridLayout.cards(screenW, screenH, mods.size)
        val idx = cards.indexOfFirst { it.contains(mouseX, mouseY) }
        if (idx >= 0) {
            val m = mods[idx]
            if (GridLayout.toggleSwitch(cards[idx]).contains(mouseX, mouseY)) {
                modules.setEnabled(m.id, !m.enabled); dirty = true // quick toggle in place; stay on the grid
                return true
            }
            selectedId = m.id
            view = EditorView.CUSTOMIZE
            hexFocused = false; activeColor = null
            modules.hudById(m.id)?.let { selectedTargetKey = it.colorTargets().firstOrNull()?.key ?: "style"; loadColor(modules) }
            return true
        }
        if (GridLayout.panelRect(screenW, screenH, mods.size).contains(mouseX, mouseY)) return true // inside panel, not a card
        view = EditorView.POSITION // click-away closes the grid
        return true
    }

    private fun onCustomizePress(mouseX: Int, mouseY: Int, screenW: Int, screenH: Int, modules: ModuleManager): Boolean {
        val sel = selectedId
        val isHud = sel != null && modules.hudById(sel) != null
        val popup = CustomizeLayout.popupRect(screenW, screenH, isHud, targetCount(modules, sel), optCount(modules, sel))
        if (CustomizeLayout.closeButton(popup).contains(mouseX, mouseY)) { backToGrid(); return true }
        if (sel == null) return true
        if (isHud) {
            val mod = modules.hudById(sel)!!
            val tc = mod.colorTargets().size
            val oc = mod.toggles.size
            // colour-target chip selection (picks which colour the picker edits)
            val ti = CustomizeLayout.targetChips(popup, tc).indexOfFirst { it.contains(mouseX, mouseY) }
            if (ti >= 0) {
                selectedTargetKey = mod.colorTargets()[ti].key; loadColor(modules)
                activeColor = null; hexFocused = false
                return true
            }
            val ctrl = CustomizeLayout.controls(popup, tc, oc).firstOrNull { it.rect.contains(mouseX, mouseY) }
            if (ctrl == null) {
                if (mod.toggles.isNotEmpty()) {
                    val oi = CustomizeLayout.optionRows(popup, tc, oc).indexOfFirst { it.contains(mouseX, mouseY) }
                    if (oi >= 0) {
                        val key = mod.toggles[oi].key
                        modules.setOption(sel, key, !mod.option(key)); dirty = true
                        hexFocused = false; hexBuffer = ""; activeColor = null
                        return true
                    }
                }
                if (popup.contains(mouseX, mouseY)) { hexFocused = false; activeColor = null; return true }
                backToGrid(); return true // click-away returns to the grid
            }
            when {
                ctrl.id in PICKERS -> {
                    activeColor = ctrl.id; hexFocused = false
                    setPickerValue(ctrl.id, ctrl.rect, mouseX, mouseY); applyEditColor(modules)
                }
                ctrl.id == "hex" -> { activeColor = null; hexFocused = true; hexBuffer = "" }
                else -> { activeColor = null; hexFocused = false; applyControl(ctrl.id, modules); loadColor(modules) }
            }
            return true
        }
        // Non-HUD module: a single enable toggle.
        if (CustomizeLayout.enableToggle(popup).contains(mouseX, mouseY)) {
            val m = modules.byId(sel) ?: return true
            modules.setEnabled(sel, !m.enabled); dirty = true
            return true
        }
        if (popup.contains(mouseX, mouseY)) return true
        backToGrid(); return true
    }

    fun onDrag(mouseX: Int, mouseY: Int, screenW: Int, screenH: Int, modules: ModuleManager): Boolean {
        activeColor?.let { ac ->
            val tc = targetCount(modules, selectedId); val oc = optCount(modules, selectedId)
            val popup = CustomizeLayout.popupRect(screenW, screenH, true, tc, oc)
            val rect = CustomizeLayout.controlRect(popup, ac, tc, oc) ?: return true
            setPickerValue(ac, rect, mouseX, mouseY); applyEditColor(modules)
            return true
        }
        if (view != EditorView.POSITION) { dragId = null; resizeId = null; return false } // tier contract: drag is POSITION-only
        resizeId?.let { rid ->
            val d1 = kotlin.math.hypot((mouseX - startLeft).toDouble(), (mouseY - startTop).toDouble())
            val newScale = (startScale * (d1 / startD)).toFloat().coerceIn(0.5f, 3.0f) // 1.0 at the grab point
            val ratio = newScale / startScale
            val newFootW = (startFootW * ratio).roundToInt().coerceAtLeast(1)
            val newFootH = (startFootH * ratio).roundToInt().coerceAtLeast(1)
            val box = Rect(startLeft, startTop, newFootW, newFootH) // top-left pinned; size grows from it
            val anchor = EditorAnchor.anchorFromPosition(box, screenW, screenH) // derive anchor (keeps resolution-independence)
            val (ox, oy) = EditorAnchor.offsetForAnchor(anchor, box, screenW, screenH)
            modules.updateStyle(rid) { it.copy(scale = newScale) }
            modules.setAnchorOffset(rid, anchor, ox, oy)
            dirty = true
            return true
        }
        val id = dragId ?: return false
        val maxLeft = (screenW - dragW).coerceAtLeast(0)
        val maxTop = (screenH - dragH).coerceAtLeast(0)
        var left = (startLeft + (mouseX - startMouseX)).coerceIn(0, maxLeft)
        var top = (startTop + (mouseY - startMouseY)).coerceIn(0, maxTop)
        var gx: Int? = null; var gy: Int? = null
        if (modules.snapEnabled()) {
            val (nl, g1) = snapAxis(left, dragW, startSnapX + listOf(0, screenW / 2, screenW)); left = nl; gx = g1
            val (nt, g2) = snapAxis(top, dragH, startSnapY + listOf(0, screenH / 2, screenH)); top = nt; gy = g2
        }
        activeGuidesX = listOfNotNull(gx); activeGuidesY = listOfNotNull(gy)
        val moved = Rect(left, top, dragW, dragH)
        val anchor = EditorAnchor.anchorFromPosition(moved, screenW, screenH)
        val (ox, oy) = EditorAnchor.offsetForAnchor(anchor, moved, screenW, screenH)
        modules.setAnchorOffset(id, anchor, ox, oy)
        dirty = true
        return true
    }

    fun onRelease(): Boolean {
        val was = dragId != null || activeColor != null || resizeId != null
        dragId = null
        activeColor = null
        resizeId = null
        activeGuidesX = emptyList(); activeGuidesY = emptyList()
        return was
    }

    /** Pop one tier: CUSTOMIZE -> GRID -> POSITION. Returns false at POSITION (caller closes). */
    fun goBack(): Boolean = when (view) {
        EditorView.CUSTOMIZE -> { backToGrid(); true }
        EditorView.GRID -> { view = EditorView.POSITION; true }
        EditorView.POSITION -> false
    }

    fun onCharTyped(ch: Char, modules: ModuleManager): Boolean {
        if (!hexFocused) return false
        if (hexBuffer.length < 8 && (ch in '0'..'9' || ch in 'a'..'f' || ch in 'A'..'F')) {
            hexBuffer += ch.uppercaseChar(); tryApplyHex(modules)
        }
        return true
    }

    fun onBackspace(modules: ModuleManager): Boolean {
        if (!hexFocused) return false
        if (hexBuffer.isNotEmpty()) { hexBuffer = hexBuffer.dropLast(1); tryApplyHex(modules) }
        return true
    }

    fun pruneSelection(boxes: List<ElementBox>) {
        if (view == EditorView.CUSTOMIZE) return // keep the popup's target stable
        val sel = selectedId ?: return
        if (boxes.none { it.id == sel }) { selectedId = null; dragId = null; activeColor = null; resizeId = null }
    }

    private fun snapAxis(pos: Int, size: Int, cands: List<Int>): Pair<Int, Int?> {
        var best = Int.MAX_VALUE; var guide: Int? = null
        val lines = intArrayOf(pos, pos + size / 2, pos + size)
        for (c in cands) for (l in lines) {
            val d = c - l
            if (kotlin.math.abs(d) <= SNAP && kotlin.math.abs(d) < kotlin.math.abs(best)) { best = d; guide = c }
        }
        return if (guide == null) pos to null else (pos + best) to guide
    }

    private fun targetCount(modules: ModuleManager, sel: String?) = sel?.let { modules.hudById(it)?.colorTargets()?.size } ?: 0
    private fun optCount(modules: ModuleManager, sel: String?) = sel?.let { modules.hudById(it)?.toggles?.size } ?: 0

    private fun backToGrid() { view = EditorView.GRID; selectedId = null; activeColor = null; hexFocused = false }

    private fun setPickerValue(id: String, r: Rect, mx: Int, my: Int) {
        val fx = ((mx - r.left).toFloat() / r.width).coerceIn(0f, 1f)
        val fy = ((my - r.top).toFloat() / r.height).coerceIn(0f, 1f)
        when (id) {
            "sv" -> { editS = fx; editV = 1f - fy }
            "hue" -> editH = fy * 360f
            "alpha" -> editA = ((1f - fy) * 255f).roundToInt()
        }
    }

    private fun applyEditColor(modules: ModuleManager) {
        val sel = selectedId ?: return
        val color = SnellColor.argb(editA, SnellColor.hsvToRgb(editH, editS, editV))
        modules.setTargetColor(sel, selectedTargetKey, color)
        dirty = true
    }

    private fun loadColor(modules: ModuleManager) {
        val c = selectedId?.let { modules.hudById(it)?.targetColor(selectedTargetKey) } ?: return
        val (h, s, v) = SnellColor.rgbToHsv(SnellColor.rgbOf(c))
        editH = h; editS = s; editV = v; editA = SnellColor.alphaOf(c)
        hexFocused = false; hexBuffer = ""
    }

    private fun tryApplyHex(modules: ModuleManager) {
        val parsed = HexColor.decode(hexBuffer) ?: return
        val sel = selectedId ?: return
        // 6-digit codes keep the element's current alpha; 8-digit codes set alpha explicitly.
        val argb = if (hexBuffer.length == 6) SnellColor.argb(editA, SnellColor.rgbOf(parsed)) else parsed
        modules.setTargetColor(sel, selectedTargetKey, argb)
        dirty = true
        val (h, s, v) = SnellColor.rgbToHsv(SnellColor.rgbOf(argb))
        editH = h; editS = s; editV = v; editA = SnellColor.alphaOf(argb)
    }

    private fun applyControl(id: String, modules: ModuleManager): Boolean {
        val sel = selectedId ?: return false
        val module = modules.hudById(sel) ?: return false
        when {
            id == "visible" -> modules.setEnabled(sel, !module.enabled)
            id == "bold" -> modules.updateStyle(sel) { it.copy(bold = !it.bold) }
            id == "italic" -> modules.updateStyle(sel) { it.copy(italic = !it.italic) }
            id == "underline" -> modules.updateStyle(sel) { it.copy(underline = !it.underline) }
            id == "strike" -> modules.updateStyle(sel) { it.copy(strikethrough = !it.strikethrough) }
            id == "shadow" -> modules.updateStyle(sel) { it.copy(shadow = !it.shadow) }
            id == "background" -> modules.updateStyle(sel) { it.copy(background = !it.background) }
            id == "scale-" -> modules.updateStyle(sel) { it.copy(scale = (it.scale - 0.25f).coerceIn(0.5f, 3.0f)) }
            id == "scale+" -> modules.updateStyle(sel) { it.copy(scale = (it.scale + 0.25f).coerceIn(0.5f, 3.0f)) }
            id == "reset" -> modules.resetStyle(sel)
            id.startsWith("swatch:") -> {
                val idx = id.removePrefix("swatch:").toIntOrNull() ?: return false
                val rgb = SnellColor.rgbOf(CustomizeLayout.SWATCHES.getOrNull(idx) ?: return false)
                val cur = module.targetColor(selectedTargetKey)
                modules.setTargetColor(sel, selectedTargetKey, SnellColor.argb(SnellColor.alphaOf(cur), rgb))
            }
            else -> return false
        }
        dirty = true
        return true
    }

    private companion object {
        val PICKERS = setOf("sv", "hue", "alpha")
        const val SNAP = 5
    }
}
