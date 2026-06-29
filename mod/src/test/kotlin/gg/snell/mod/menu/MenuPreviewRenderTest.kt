package gg.snell.mod.menu

import gg.snell.mod.platform.EditorCanvas
import gg.snell.mod.platform.TextRun
import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Font
import java.awt.GradientPaint
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Visual smoke render (not a behavioural assertion): rasterizes each bespoke menu via a Java2D
 * [EditorCanvas] (headless, no GL) so the design-matched chrome can be eyeballed without launching
 * the game. Writes PNGs under build/menu-preview/.
 */
class MenuPreviewRenderTest {
    private class AwtCanvas(val img: BufferedImage, override val screenWidth: Int, override val screenHeight: Int) : EditorCanvas {
        val g = img.createGraphics().apply {
            setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            font = Font("SansSerif", Font.PLAIN, 9)
        }
        private fun col(argb: Int) = Color(argb, true)
        override fun drawText(x: Int, y: Int, text: String, color: Int) {
            val fm = g.fontMetrics
            g.color = Color(0, 0, 0, 140); g.drawString(text, x + 1f, y + fm.ascent + 1f)
            g.color = col(color); g.drawString(text, x.toFloat(), y + fm.ascent.toFloat())
        }
        override fun drawStyledText(x: Int, y: Int, text: String, run: TextRun) = drawText(x, y, text, run.color)
        override fun fill(x: Int, y: Int, w: Int, h: Int, color: Int) {
            g.composite = AlphaComposite.SrcOver; g.color = col(color); g.fillRect(x, y, w, h)
        }
        override fun border(x: Int, y: Int, w: Int, h: Int, color: Int) { g.color = col(color); g.drawRect(x, y, w - 1, h - 1) }
        override fun gradientV(x: Int, y: Int, w: Int, h: Int, top: Int, bottom: Int) {
            g.paint = GradientPaint(x.toFloat(), y.toFloat(), col(top), x.toFloat(), (y + h).toFloat(), col(bottom))
            g.fillRect(x, y, w, h); g.paint = null
        }
        override fun withScale(scale: Float, pivotX: Int, pivotY: Int, body: () -> Unit) {
            val saved = g.transform
            val t = AffineTransform(saved); t.translate(pivotX.toDouble(), pivotY.toDouble()); t.scale(scale.toDouble(), scale.toDouble())
            g.transform = t; try { body() } finally { g.transform = saved }
        }
        override fun textWidth(text: String) = g.fontMetrics.stringWidth(text)
        override val lineHeight: Int get() = g.fontMetrics.height - 2
        override fun overlayStratum() {}

        private val iconFont: Font? = javaClass.classLoader.getResourceAsStream("assets/snell/font/tabler-snell.ttf")
            ?.use { Font.createFont(Font.TRUETYPE_FONT, it).deriveFont(10f) }

        override fun drawIcon(glyph: Char, x: Int, y: Int, color: Int) {
            val f = iconFont ?: return
            g.color = col(color)
            // Top-anchor the glyph ink at (x,y) to approximate MC's top-anchored text (not the AWT baseline).
            val gv = f.createGlyphVector(g.fontRenderContext, glyph.toString())
            val b = gv.visualBounds
            g.drawGlyphVector(gv, x.toFloat() - b.x.toFloat(), y.toFloat() - b.y.toFloat())
        }

        override fun iconWidth(glyph: Char) = iconFont?.let { g.getFontMetrics(it).charWidth(glyph) } ?: 8

        override fun drawTexture(id: String, x: Int, y: Int, w: Int, h: Int) {
            val res = "assets/" + id.replace(":", "/")
            val img = javaClass.classLoader.getResourceAsStream(res)?.use { ImageIO.read(it) } ?: return
            g.drawImage(img, x, y, w, h, null)
        }

        private val monoFont: Font? = javaClass.classLoader.getResourceAsStream("assets/snell/font/geist-mono.ttf")
            ?.use { Font.createFont(Font.TRUETYPE_FONT, it).deriveFont(9f) }

        override fun drawMono(x: Int, y: Int, text: String, color: Int) {
            val saved = g.font; g.font = monoFont ?: saved
            val fm = g.fontMetrics
            g.color = Color(0, 0, 0, 140); g.drawString(text, x + 1f, y + fm.ascent + 1f)
            g.color = col(color); g.drawString(text, x.toFloat(), y + fm.ascent.toFloat())
            g.font = saved
        }

        override fun monoWidth(text: String) = monoFont?.let { g.getFontMetrics(it).stringWidth(text) } ?: textWidth(text)
    }

    private fun frame(w: Int, h: Int): Pair<BufferedImage, AwtCanvas> {
        val img = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        return img to AwtCanvas(img, w, h)
    }

    /** A frame with a faux green/brown world behind it, so scrim + panel contrast read in-game. */
    private fun worldFrame(w: Int, h: Int): Pair<BufferedImage, AwtCanvas> {
        val (img, canvas) = frame(w, h)
        canvas.gradientV(0, 0, w, h, 0xFF3A4A2A.toInt(), 0xFF12180C.toInt())
        return img to canvas
    }

    private fun write(img: BufferedImage, name: String): File {
        val out = File("build/menu-preview").apply { mkdirs() }
        val up = BufferedImage(img.width * 2, img.height * 2, BufferedImage.TYPE_INT_ARGB)
        up.createGraphics().apply { drawImage(img, AffineTransform.getScaleInstance(2.0, 2.0), null); dispose() }
        val f = File(out, name); ImageIO.write(up, "png", f); return f
    }

    @Test fun `render title screen`() {
        val w = 480; val h = 270
        val (img, canvas) = frame(w, h)
        val sp = TitleLayout.navButtons(w, h).first { it.id == "singleplayer" }.rect
        TitleRenderer.render(canvas, w, h, sp.left + sp.width / 2, sp.top + sp.height / 2, version = "26.2", username = "SnellQueen", statusLabel = "Online", crowns = "2,450")
        assertTrue(write(img, "01-title.png").length() > 0)
    }

    @Test fun `render pause menu`() {
        val w = 480; val h = 270
        val (img, canvas) = worldFrame(w, h)
        val opt = PauseLayout.controls(w, h).first { it.id == "options" }.rect
        PauseRenderer.render(canvas, w, h, opt.left + opt.width / 2, opt.top + opt.height / 2, worldName = "Survival World")
        assertTrue(write(img, "02-pause.png").length() > 0)
    }

    @Test fun `render world picker`() {
        val w = 520; val h = 360
        val (img, canvas) = worldFrame(w, h)
        val rows = listOf(
            WorldRow("New World", "New World", "Survival", "1.21 · 2 minutes ago", "2.1 GB"),
            WorldRow("Hardcore Attempt 4", "Hardcore", "Hardcore", "1.21 · yesterday", "880 MB"),
            WorldRow("Creative Flat", "flat-1", "Creative", "1.21 · 3 days ago", "120 MB"),
            WorldRow("SkyBlock", "sb", "Survival", "1.20.4 · last week", "640 MB"),
            WorldRow("Old Base", "old", "Survival", "1.19 · 2 months ago", "1.4 GB"),
        )
        WorldSelectRenderer.render(canvas, w, h, -1, -1, rows, selected = 0, scrollY = 0, search = "", searchFocused = false)
        assertTrue(write(img, "03-world.png").length() > 0)
    }

    @Test fun `render server picker`() {
        val w = 520; val h = 360
        val (img, canvas) = worldFrame(w, h)
        val rows = listOf(
            ServerRow("Hypixel", "mc.hypixel.net", "Bedwars · SkyBlock · 30+ minigames", "84231", 23, ServerStatus.Online),
            ServerRow("CubeCraft", "play.cubecraft.net", "Lucky Islands · EggWars", "12044", 41, ServerStatus.Online),
            ServerRow("My SMP", "smp.example.net", "Private survival realm", "3", 8, ServerStatus.Online),
            ServerRow("Old Server", "dead.example.net", "Can't connect to server", "", -1, ServerStatus.Offline),
            ServerRow("Resolving", "new.example.net", "", "", -1, ServerStatus.Pinging),
        )
        ServerSelectRenderer.render(canvas, w, h, -1, -1, rows, selected = 0, scrollY = 0)
        assertTrue(write(img, "04-server.png").length() > 0)
    }

    @Test fun `render options screen`() {
        val w = 520; val h = 360
        val (img, canvas) = frame(w, h)
        val entries = listOf(
            OptionEntry.Section("Rendering"),
            OptionEntry.Item(OptionItem("rd", "Render Distance", OptionKind.Slider, "16 chunks", fraction = 0.45f, description = "Chunks loaded around you")),
            OptionEntry.Item(OptionItem("gfx", "Graphics", OptionKind.Cycle, "Fancy", description = "Detail and visual quality")),
            OptionEntry.Item(OptionItem("fps", "Max Framerate", OptionKind.Cycle, "120 fps")),
            OptionEntry.Item(OptionItem("bright", "Brightness", OptionKind.Slider, "50%", fraction = 0.5f)),
            OptionEntry.Section("Display"),
            OptionEntry.Item(OptionItem("fs", "Fullscreen", OptionKind.Toggle, "", on = false, description = "Borderless on this display")),
            OptionEntry.Item(OptionItem("vsync", "VSync", OptionKind.Toggle, "", on = true)),
            OptionEntry.Item(OptionItem("smooth", "Smooth Lighting", OptionKind.Toggle, "", on = true)),
        )
        OptionsRenderer.render(canvas, w, h, -1, -1, entries, activeCategory = "video", scrollY = 0)
        assertTrue(write(img, "05-options.png").length() > 0)
    }
}
