package gg.snell.mod.editor

import gg.snell.mod.config.Config
import gg.snell.mod.module.FontModule
import gg.snell.mod.module.HudAnchor
import gg.snell.mod.module.ModuleManager
import gg.snell.mod.module.hud.CoordsModule
import gg.snell.mod.module.hud.CpsModule
import gg.snell.mod.module.hud.FpsModule
import gg.snell.mod.module.hud.KeystrokesModule
import gg.snell.mod.platform.GameContext
import gg.snell.shared.SnellPalette
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private object Measure : TextMeasurer {
    override fun width(text: String) = text.length * 6
    override val lineHeight = 10
}

private fun ctx() = GameContext(60, true, 1.0, 64.0, -2.0, false, false, false, false)

private fun Rect.cx() = left + width / 2
private fun Rect.cy() = top + height / 2

class EditorStateTest {
    private fun setup(screenW: Int = 800, screenH: Int = 600, withFont: Boolean = false, withCoords: Boolean = false, withCps: Boolean = false, withKeystrokes: Boolean = false):
        Triple<ModuleManager, List<ElementBox>, EditorState> {
        val mgr = ModuleManager(Config(Files.createTempDirectory("editor"))).apply {
            register(FpsModule()); if (withCoords) register(CoordsModule()); if (withCps) register(CpsModule()); if (withKeystrokes) register(KeystrokesModule()); if (withFont) register(FontModule())
        }
        val boxes = ElementLayout.boxesFor(mgr.hudModules(), ctx(), Measure, screenW, screenH)
        return Triple(mgr, boxes, EditorState())
    }

    // --- navigation helpers -------------------------------------------------
    private fun openGrid(s: EditorState, boxes: List<ElementBox>, mgr: ModuleManager) {
        val b = PositionLayout.modsButton(800, 600)
        s.onPress(b.cx(), b.cy(), 800, 600, boxes, mgr)
    }

    private fun openCustomize(s: EditorState, boxes: List<ElementBox>, mgr: ModuleManager, id: String) {
        openGrid(s, boxes, mgr)
        val idx = mgr.all().indexOfFirst { it.id == id }
        val card = GridLayout.cards(800, 600, mgr.all().size)[idx]
        s.onPress(card.left + 20, card.top + 10, 800, 600, boxes, mgr) // title area, not the switch
    }

    private fun control(id: String): Rect =
        CustomizeLayout.controlRect(CustomizeLayout.popupRect(800, 600, true, 1, 0), id, 1, 0)!!

    private fun selectFps(s: EditorState, boxes: List<ElementBox>, mgr: ModuleManager) =
        openCustomize(s, boxes, mgr, "fps")

    private fun fpsColor(mgr: ModuleManager) = mgr.hudById("fps")!!.style.color

    // --- POSITION tier ------------------------------------------------------
    @Test fun `press on element selects it for dragging`() {
        val (mgr, boxes, s) = setup()
        val b = boxes.first { it.id == "fps" }.rect
        s.onPress(b.cx(), b.cy(), 800, 600, boxes, mgr)
        assertEquals("fps", s.selectedId)
        assertEquals(EditorView.POSITION, s.view)
    }

    @Test fun `press on empty space deselects`() {
        val (mgr, boxes, s) = setup()
        assertFalse(s.onPress(400, 320, 800, 600, boxes, mgr)); assertNull(s.selectedId)
    }

    @Test fun `drag re-anchors the element so it stays put`() {
        val (mgr, boxes, s) = setup()
        val b = boxes.first { it.id == "fps" }.rect
        s.onPress(b.cx(), b.cy(), 800, 600, boxes, mgr)
        s.onDrag(b.cx() + 700, b.cy() + 500, 800, 600, mgr)
        s.onRelease()
        assertEquals(HudAnchor.BOTTOM_RIGHT, mgr.hudById("fps")!!.anchor); assertTrue(s.dirty)
    }

    @Test fun `drag does not crash when the element is larger than the screen`() {
        val (mgr, boxes, s) = setup(screenW = 20, screenH = 600)
        val b = boxes.first { it.id == "fps" }.rect
        s.onPress(b.left + 1, b.top + 1, 20, 600, boxes, mgr)
        s.onDrag(b.left + 5, b.top + 5, 20, 600, mgr); s.onRelease(); assertTrue(s.dirty)
    }

    @Test fun `prune deselects when the selected element has no box`() {
        val (mgr, boxes, s) = setup()
        val b = boxes.first { it.id == "fps" }.rect
        s.onPress(b.cx(), b.cy(), 800, 600, boxes, mgr)
        s.pruneSelection(emptyList()); assertNull(s.selectedId)
    }

    @Test fun `mods button opens the grid`() {
        val (mgr, boxes, s) = setup(); openGrid(s, boxes, mgr)
        assertEquals(EditorView.GRID, s.view)
    }

    @Test fun `done button requests close`() {
        val (mgr, boxes, s) = setup()
        val d = PositionLayout.doneButton(800, 600)
        s.onPress(d.cx(), d.cy(), 800, 600, boxes, mgr)
        assertTrue(s.closeRequested)
    }

    // --- GRID tier ----------------------------------------------------------
    @Test fun `grid card opens the customize popup for that module`() {
        val (mgr, boxes, s) = setup(); openCustomize(s, boxes, mgr, "fps")
        assertEquals(EditorView.CUSTOMIZE, s.view); assertEquals("fps", s.customizing)
    }

    @Test fun `grid back button returns to position`() {
        val (mgr, boxes, s) = setup(); openGrid(s, boxes, mgr)
        val back = GridLayout.backButton(800, 600, mgr.all().size)
        s.onPress(back.cx(), back.cy(), 800, 600, boxes, mgr)
        assertEquals(EditorView.POSITION, s.view)
    }

    @Test fun `grid click-away returns to position`() {
        val (mgr, boxes, s) = setup(); openGrid(s, boxes, mgr)
        s.onPress(5, 5, 800, 600, boxes, mgr) // outside the centered panel
        assertEquals(EditorView.POSITION, s.view)
    }

    // --- CUSTOMIZE tier -----------------------------------------------------
    @Test fun `customize close returns to the grid`() {
        val (mgr, boxes, s) = setup(); openCustomize(s, boxes, mgr, "fps")
        val close = CustomizeLayout.closeButton(CustomizeLayout.popupRect(800, 600, true, 1, 0))
        s.onPress(close.cx(), close.cy(), 800, 600, boxes, mgr)
        assertEquals(EditorView.GRID, s.view); assertNull(s.customizing)
    }

    @Test fun `customize click-away returns to the grid`() {
        val (mgr, boxes, s) = setup(); openCustomize(s, boxes, mgr, "fps")
        s.onPress(2, 2, 800, 600, boxes, mgr) // outside the centered popup
        assertEquals(EditorView.GRID, s.view)
    }

    @Test fun `goBack steps customize to grid to position then stops`() {
        val (mgr, boxes, s) = setup(); openCustomize(s, boxes, mgr, "fps")
        assertTrue(s.goBack()); assertEquals(EditorView.GRID, s.view)
        assertTrue(s.goBack()); assertEquals(EditorView.POSITION, s.view)
        assertFalse(s.goBack())
    }

    @Test fun `customize enable toggles a non-HUD module and fires the hook`() {
        val (mgr, boxes, s) = setup(withFont = true)
        val seen = mutableListOf<Pair<String, Boolean>>()
        mgr.onEnabledChanged = { id, en -> seen.add(id to en) }
        openCustomize(s, boxes, mgr, "font")
        assertEquals("font", s.customizing)
        val before = mgr.byId("font")!!.enabled
        val en = CustomizeLayout.enableToggle(CustomizeLayout.popupRect(800, 600, false))
        s.onPress(en.cx(), en.cy(), 800, 600, boxes, mgr)
        assertEquals(!before, mgr.byId("font")!!.enabled)
        assertEquals(listOf("font" to !before), seen)
    }

    @Test fun `customize enable toggles a HUD module via the visible control`() {
        val (mgr, boxes, s) = setup(); openCustomize(s, boxes, mgr, "fps")
        assertTrue(mgr.hudById("fps")!!.enabled)
        val v = control("visible"); s.onPress(v.cx(), v.cy(), 800, 600, boxes, mgr)
        assertFalse(mgr.hudById("fps")!!.enabled)
    }

    @Test fun `panel toggle flips bold on the selected element`() {
        val (mgr, boxes, s) = setup(); selectFps(s, boxes, mgr)
        val bold = control("bold"); s.onPress(bold.cx(), bold.cy(), 800, 600, boxes, mgr)
        assertTrue(mgr.hudById("fps")!!.style.bold)
    }

    @Test fun `swatch recolors the selected element keeping its alpha`() {
        val (mgr, boxes, s) = setup(); selectFps(s, boxes, mgr)
        val sw = control("swatch:2"); s.onPress(sw.cx(), sw.cy(), 800, 600, boxes, mgr)
        assertEquals(SnellColor.rgbOf(SnellPalette.primary), SnellColor.rgbOf(fpsColor(mgr)))
    }

    @Test fun `scale buttons clamp to range`() {
        val (mgr, boxes, s) = setup(); selectFps(s, boxes, mgr)
        repeat(20) { val c = control("scale+"); s.onPress(c.left + 1, c.top + 1, 800, 600, boxes, mgr) }
        assertEquals(3.0f, mgr.hudById("fps")!!.style.scale)
    }

    @Test fun `SV square sets full saturation and value at top-right`() {
        val (mgr, boxes, s) = setup(); selectFps(s, boxes, mgr)
        val r = control("sv"); s.onPress(r.right - 1, r.top, 800, 600, boxes, mgr)
        val (_, sat, value) = SnellColor.rgbToHsv(SnellColor.rgbOf(fpsColor(mgr)))
        assertTrue(sat > 0.95f, "sat=$sat"); assertTrue(value > 0.95f, "value=$value")
    }

    @Test fun `hue bar sets the hue from vertical position`() {
        val (mgr, boxes, s) = setup(); selectFps(s, boxes, mgr)
        val r = control("hue"); s.onPress(r.left + 1, r.top + r.height / 2, 800, 600, boxes, mgr) // mid -> ~180
        val (hue, _, _) = SnellColor.rgbToHsv(SnellColor.rgbOf(fpsColor(mgr)))
        assertTrue(hue in 170f..190f, "hue=$hue")
    }

    @Test fun `alpha bar bottom makes the color transparent`() {
        val (mgr, boxes, s) = setup(); selectFps(s, boxes, mgr)
        val r = control("alpha"); s.onPress(r.left + 1, r.bottom - 1, 800, 600, boxes, mgr)
        assertTrue(SnellColor.alphaOf(fpsColor(mgr)) < 10, "alpha=${SnellColor.alphaOf(fpsColor(mgr))}")
    }

    @Test fun `picker drag updates continuously`() {
        val (mgr, boxes, s) = setup(); selectFps(s, boxes, mgr)
        val r = control("sv")
        s.onPress(r.left + 1, r.bottom - 1, 800, 600, boxes, mgr) // s~0, v~0 -> near black
        s.onDrag(r.right - 1, r.top, 800, 600, mgr)               // drag to s~1, v~1
        s.onRelease()
        val (_, sat, value) = SnellColor.rgbToHsv(SnellColor.rgbOf(fpsColor(mgr)))
        assertTrue(sat > 0.95f && value > 0.95f, "sat=$sat value=$value")
    }

    @Test fun `hex field applies a full 6-digit code`() {
        val (mgr, boxes, s) = setup(); selectFps(s, boxes, mgr)
        val hex = control("hex"); s.onPress(hex.left + 2, hex.top + 2, 800, 600, boxes, mgr)
        assertTrue(s.isHexFocused)
        "00FF00".forEach { s.onCharTyped(it, mgr) }
        assertEquals(0xFF00FF00.toInt(), fpsColor(mgr))
    }

    @Test fun `6-digit hex keeps the element's current alpha`() {
        val (mgr, boxes, s) = setup(); selectFps(s, boxes, mgr)
        val a = control("alpha"); s.onPress(a.left + 1, a.bottom - 1, 800, 600, boxes, mgr) // alpha ~ 0
        assertTrue(SnellColor.alphaOf(fpsColor(mgr)) < 10)
        val hex = control("hex"); s.onPress(hex.left + 2, hex.top + 2, 800, 600, boxes, mgr)
        "112233".forEach { s.onCharTyped(it, mgr) }
        assertEquals(0x112233, SnellColor.rgbOf(fpsColor(mgr)))
        assertTrue(SnellColor.alphaOf(fpsColor(mgr)) < 10, "alpha preserved on 6-digit hex")
    }

    @Test fun `hex field ignores an incomplete code`() {
        val (mgr, boxes, s) = setup(); selectFps(s, boxes, mgr)
        val before = fpsColor(mgr)
        val hex = control("hex"); s.onPress(hex.left + 2, hex.top + 2, 800, 600, boxes, mgr)
        s.onCharTyped('A', mgr); s.onCharTyped('B', mgr) // only 2 chars -> no apply
        assertEquals(before, fpsColor(mgr))
    }

    @Test fun `loaded color round-trips into the picker on open`() {
        val (mgr, boxes, s) = setup(); selectFps(s, boxes, mgr)
        assertNotNull(s.customizing)
        // gold has a non-zero hue; opening fps should have loaded it
        assertTrue(s.colorA in 1..255)
    }

    @Test fun `drag is ignored after switching tier without releasing`() {
        val (mgr, boxes, s) = setup()
        val b = boxes.first { it.id == "fps" }.rect
        s.onPress(b.cx(), b.cy(), 800, 600, boxes, mgr)   // POSITION: begin a drag
        openGrid(s, boxes, mgr)                            // switch tier without onRelease()
        assertEquals(EditorView.GRID, s.view)
        val anchorBefore = mgr.hudById("fps")!!.anchor
        assertFalse(s.onDrag(10, 590, 800, 600, mgr))     // stale dragId must not re-anchor
        assertEquals(anchorBefore, mgr.hudById("fps")!!.anchor)
    }

    @Test fun `grid switch toggles the module in place without opening customize`() {
        val (mgr, boxes, s) = setup(); openGrid(s, boxes, mgr)
        val idx = mgr.all().indexOfFirst { it.id == "fps" }
        val card = GridLayout.cards(800, 600, mgr.all().size)[idx]
        val sw = GridLayout.toggleSwitch(card)
        val before = mgr.hudById("fps")!!.enabled
        s.onPress(sw.cx(), sw.cy(), 800, 600, boxes, mgr)
        assertEquals(!before, mgr.hudById("fps")!!.enabled, "switch toggles enabled")
        assertEquals(EditorView.GRID, s.view, "stays on the grid")
    }

    // --- snap + resize (POSITION) -----------------------------------------
    private fun boxesOf(mgr: ModuleManager) = ElementLayout.boxesFor(mgr.hudModules(), ctx(), Measure, 800, 600)
    private fun box(mgr: ModuleManager, id: String) = boxesOf(mgr).first { it.id == id }.rect
    private fun pressDrag(s: EditorState, mgr: ModuleManager, boxes: List<ElementBox>, from: Rect, toLeft: Int, toTop: Int) {
        s.onPress(from.cx(), from.cy(), 800, 600, boxes, mgr)
        s.onDrag(from.cx() + (toLeft - from.left), from.cy() + (toTop - from.top), 800, 600, mgr)
    }

    @Test fun `snap on aligns box centre to the screen centre`() {
        val (mgr, boxes, s) = setup()
        val fps = boxes.first { it.id == "fps" }.rect
        pressDrag(s, mgr, boxes, fps, (800 - fps.width) / 2 + 3, fps.top)
        assertEquals((800 - fps.width) / 2, box(mgr, "fps").left, "snapped to horizontal centre")
        assertTrue(s.activeGuidesX.contains(400), "centre guide recorded")
    }

    @Test fun `snap off leaves free pixel position`() {
        val (mgr, boxes, s) = setup()
        mgr.setSnapEnabled(false)
        val fps = boxes.first { it.id == "fps" }.rect
        val target = (800 - fps.width) / 2 + 3
        pressDrag(s, mgr, boxes, fps, target, fps.top)
        assertEquals(target, box(mgr, "fps").left, "free, not snapped")
        assertTrue(s.activeGuidesX.isEmpty())
    }

    @Test fun `snap aligns to a sibling left edge`() {
        val (mgr, _, s) = setup(withCoords = true)
        mgr.setAnchorOffset("coords", HudAnchor.TOP_LEFT, 300, 200)
        val boxes = boxesOf(mgr)
        val fps = boxes.first { it.id == "fps" }.rect
        pressDrag(s, mgr, boxes, fps, 302, fps.top) // 2px right of the sibling's left
        assertEquals(300, box(mgr, "fps").left, "aligned to sibling left edge")
    }

    private fun selectThenGrabHandle(s: EditorState, mgr: ModuleManager): Rect {
        val fps0 = box(mgr, "fps")
        s.onPress(fps0.cx(), fps0.cy(), 800, 600, boxesOf(mgr), mgr); s.onRelease() // select
        val h = PositionLayout.resizeHandle(fps0)
        s.onPress(h.cx(), h.cy(), 800, 600, boxesOf(mgr), mgr) // grab the handle
        return fps0
    }

    @Test fun `corner handle grows the scale and pins the top-left`() {
        val (mgr, _, s) = setup()
        val fps0 = selectThenGrabHandle(s, mgr)
        s.onDrag(fps0.right + 60, fps0.bottom + 30, 800, 600, mgr); s.onRelease()
        assertTrue(mgr.hudById("fps")!!.style.scale > 1.0f, "scaled up")
        val nb = box(mgr, "fps")
        assertEquals(fps0.left, nb.left, "top-left x pinned"); assertEquals(fps0.top, nb.top, "top-left y pinned")
    }

    @Test fun `corner handle shrinks toward the minimum`() {
        val (mgr, _, s) = setup()
        val fps0 = selectThenGrabHandle(s, mgr)
        s.onDrag(fps0.left + 1, fps0.top + 1, 800, 600, mgr); s.onRelease()
        assertEquals(0.5f, mgr.hudById("fps")!!.style.scale, "clamped to min")
    }

    @Test fun `corner handle clamps at the maximum`() {
        val (mgr, _, s) = setup()
        val fps0 = selectThenGrabHandle(s, mgr)
        s.onDrag(fps0.left + 4000, fps0.top + 4000, 800, 600, mgr); s.onRelease()
        assertEquals(3.0f, mgr.hudById("fps")!!.style.scale, "clamped to max")
    }

    @Test fun `resize does not jump on grab`() {
        val (mgr, _, s) = setup()
        val fps0 = selectThenGrabHandle(s, mgr)
        val h = PositionLayout.resizeHandle(fps0)
        s.onDrag(h.cx(), h.cy(), 800, 600, mgr) // drag exactly at the grab point
        assertEquals(1.0f, mgr.hudById("fps")!!.style.scale, "scale unchanged at the grab point")
    }

    @Test fun `resize keeps a bottom-right element off the top-left anchor`() {
        val (mgr, _, s) = setup()
        mgr.setAnchorOffset("fps", HudAnchor.BOTTOM_RIGHT, 10, 10)
        val fps0 = box(mgr, "fps")
        s.onPress(fps0.cx(), fps0.cy(), 800, 600, boxesOf(mgr), mgr); s.onRelease()
        val h = PositionLayout.resizeHandle(fps0)
        s.onPress(h.cx(), h.cy(), 800, 600, boxesOf(mgr), mgr)
        s.onDrag(fps0.right + 20, fps0.bottom + 20, 800, 600, mgr)
        assertTrue(mgr.hudById("fps")!!.anchor != HudAnchor.TOP_LEFT, "anchor not clobbered to TOP_LEFT")
    }

    @Test fun `customize toggles a module option in place`() {
        val (mgr, boxes, s) = setup(withCps = true)
        openCustomize(s, boxes, mgr, "cps")
        val popup = CustomizeLayout.popupRect(800, 600, true, 1, 1)
        val rows = CustomizeLayout.optionRows(popup, 1, mgr.hudById("cps")!!.toggles.size)
        val before = mgr.hudById("cps")!!.option("right")
        s.onPress(rows[0].cx(), rows[0].cy(), 800, 600, boxes, mgr)
        assertEquals(!before, mgr.hudById("cps")!!.option("right"), "option toggled")
        assertEquals(EditorView.CUSTOMIZE, s.view, "popup stays open")
    }

    @Test fun `toggling an option clears hex focus`() {
        val (mgr, boxes, s) = setup(withCps = true)
        openCustomize(s, boxes, mgr, "cps")
        val popup = CustomizeLayout.popupRect(800, 600, true, 1, mgr.hudById("cps")!!.toggles.size)
        val hex = CustomizeLayout.controlRect(popup, "hex", 1, 1)!!
        s.onPress(hex.left + 2, hex.top + 2, 800, 600, boxes, mgr)
        assertTrue(s.isHexFocused)
        val row = CustomizeLayout.optionRows(popup, 1, 1)[0]
        s.onPress(row.cx(), row.cy(), 800, 600, boxes, mgr)
        assertFalse(s.isHexFocused, "hex focus cleared by option toggle")
    }

    @Test fun `selecting a colour target routes the picker to that colour`() {
        val (mgr, boxes, s) = setup(withKeystrokes = true)
        openCustomize(s, boxes, mgr, "keystrokes")
        val ks = mgr.hudById("keystrokes")!!
        assertEquals("box", s.selectedTargetKey, "first target selected by default")
        val tc = ks.colorTargets().size; val oc = ks.toggles.size
        val popup = CustomizeLayout.popupRect(800, 600, true, tc, oc)
        val li = ks.colorTargets().indexOfFirst { it.key == "letter" }
        val chip = CustomizeLayout.targetChips(popup, tc)[li]
        s.onPress(chip.cx(), chip.cy(), 800, 600, boxes, mgr)
        assertEquals("letter", s.selectedTargetKey, "chip selects the target")
        val boxBefore = ks.colorOption("box")
        val red = CustomizeLayout.controlRect(popup, "swatch:6", tc, oc)!! // 0xFFFF5555
        s.onPress(red.cx(), red.cy(), 800, 600, boxes, mgr)
        assertEquals(boxBefore, ks.colorOption("box"), "box colour untouched")
        assertEquals(SnellColor.rgbOf(0xFFFF5555.toInt()), SnellColor.rgbOf(ks.colorOption("letter")), "letter recoloured")
    }
}
