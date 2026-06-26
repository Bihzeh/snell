package gg.maeve.mod.editor

import gg.maeve.mod.config.Config
import gg.maeve.mod.module.FontModule
import gg.maeve.mod.module.HudAnchor
import gg.maeve.mod.module.ModuleManager
import gg.maeve.mod.module.hud.FpsModule
import gg.maeve.mod.platform.GameContext
import gg.maeve.shared.MaevePalette
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
    private fun setup(screenW: Int = 800, screenH: Int = 600, withFont: Boolean = false):
        Triple<ModuleManager, List<ElementBox>, EditorState> {
        val mgr = ModuleManager(Config(Files.createTempDirectory("editor"))).apply {
            register(FpsModule()); if (withFont) register(FontModule())
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
        s.onPress(card.cx(), card.cy(), 800, 600, boxes, mgr)
    }

    private fun control(id: String): Rect =
        CustomizeLayout.controlRect(CustomizeLayout.popupRect(800, 600, true), id)!!

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
        val close = CustomizeLayout.closeButton(CustomizeLayout.popupRect(800, 600, true))
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
        assertEquals(MaeveColor.rgbOf(MaevePalette.primary), MaeveColor.rgbOf(fpsColor(mgr)))
    }

    @Test fun `scale buttons clamp to range`() {
        val (mgr, boxes, s) = setup(); selectFps(s, boxes, mgr)
        repeat(20) { val c = control("scale+"); s.onPress(c.left + 1, c.top + 1, 800, 600, boxes, mgr) }
        assertEquals(3.0f, mgr.hudById("fps")!!.style.scale)
    }

    @Test fun `SV square sets full saturation and value at top-right`() {
        val (mgr, boxes, s) = setup(); selectFps(s, boxes, mgr)
        val r = control("sv"); s.onPress(r.right - 1, r.top, 800, 600, boxes, mgr)
        val (_, sat, value) = MaeveColor.rgbToHsv(MaeveColor.rgbOf(fpsColor(mgr)))
        assertTrue(sat > 0.95f, "sat=$sat"); assertTrue(value > 0.95f, "value=$value")
    }

    @Test fun `hue bar sets the hue from vertical position`() {
        val (mgr, boxes, s) = setup(); selectFps(s, boxes, mgr)
        val r = control("hue"); s.onPress(r.left + 1, r.top + r.height / 2, 800, 600, boxes, mgr) // mid -> ~180
        val (hue, _, _) = MaeveColor.rgbToHsv(MaeveColor.rgbOf(fpsColor(mgr)))
        assertTrue(hue in 170f..190f, "hue=$hue")
    }

    @Test fun `alpha bar bottom makes the color transparent`() {
        val (mgr, boxes, s) = setup(); selectFps(s, boxes, mgr)
        val r = control("alpha"); s.onPress(r.left + 1, r.bottom - 1, 800, 600, boxes, mgr)
        assertTrue(MaeveColor.alphaOf(fpsColor(mgr)) < 10, "alpha=${MaeveColor.alphaOf(fpsColor(mgr))}")
    }

    @Test fun `picker drag updates continuously`() {
        val (mgr, boxes, s) = setup(); selectFps(s, boxes, mgr)
        val r = control("sv")
        s.onPress(r.left + 1, r.bottom - 1, 800, 600, boxes, mgr) // s~0, v~0 -> near black
        s.onDrag(r.right - 1, r.top, 800, 600, mgr)               // drag to s~1, v~1
        s.onRelease()
        val (_, sat, value) = MaeveColor.rgbToHsv(MaeveColor.rgbOf(fpsColor(mgr)))
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
        assertTrue(MaeveColor.alphaOf(fpsColor(mgr)) < 10)
        val hex = control("hex"); s.onPress(hex.left + 2, hex.top + 2, 800, 600, boxes, mgr)
        "112233".forEach { s.onCharTyped(it, mgr) }
        assertEquals(0x112233, MaeveColor.rgbOf(fpsColor(mgr)))
        assertTrue(MaeveColor.alphaOf(fpsColor(mgr)) < 10, "alpha preserved on 6-digit hex")
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
}
