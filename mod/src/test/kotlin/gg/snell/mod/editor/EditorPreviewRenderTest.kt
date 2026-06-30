package gg.snell.mod.editor

import gg.snell.mod.config.Config
import gg.snell.mod.module.FontModule
import gg.snell.mod.module.ModuleManager
import gg.snell.mod.module.hud.ClockModule
import gg.snell.mod.module.hud.CompassModule
import gg.snell.mod.module.hud.CoordsModule
import gg.snell.mod.module.hud.CpsModule
import gg.snell.mod.module.hud.DayModule
import gg.snell.mod.module.hud.DirectionModule
import gg.snell.mod.module.hud.FpsModule
import gg.snell.mod.module.hud.KeystrokesModule
import gg.snell.mod.module.hud.SpeedModule
import gg.snell.mod.platform.EditorCanvas
import gg.snell.mod.platform.GameContext
import gg.snell.mod.platform.TextRun
import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Font
import java.awt.GradientPaint
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Not a behavioural assertion — a visual smoke render. Drives EditorState through its three tiers
 * and rasterizes each via a Java2D-backed [EditorCanvas] (headless-safe; no GL) so the editor
 * chrome can be eyeballed off a real Minecraft launch. Writes PNGs under build/editor-preview/.
 */
class EditorPreviewRenderTest {
    private class AwtCanvas(val img: BufferedImage, override val screenWidth: Int, override val screenHeight: Int) : EditorCanvas {
        val g = img.createGraphics().apply {
            setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            font = Font("SansSerif", Font.PLAIN, 9)
        }
        private fun col(argb: Int) = Color(argb, true)
        override fun drawText(x: Int, y: Int, text: String, color: Int) {
            val fm = g.fontMetrics
            g.color = Color(0, 0, 0, 140); g.drawString(text, x + 1f, y + fm.ascent + 1f) // shadow
            g.color = col(color); g.drawString(text, x.toFloat(), y + fm.ascent.toFloat())
        }
        override fun drawStyledText(x: Int, y: Int, text: String, run: TextRun) = drawText(x, y, text, run.color)
        override fun fill(x: Int, y: Int, w: Int, h: Int, color: Int) {
            val c = col(color)
            if (c.alpha < 255) { g.composite = AlphaComposite.SrcOver; }
            g.color = c; g.fillRect(x, y, w, h)
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
        override fun drawIcon(glyph: Char, x: Int, y: Int, color: Int) {} // HUD editor preview uses no icons
        override fun iconWidth(glyph: Char) = 8
        override fun drawTexture(id: String, x: Int, y: Int, w: Int, h: Int) {}
        override fun sprite(id: String, x: Int, y: Int, w: Int, h: Int, tint: Int) {} // HUD editor preview draws no menu sprites
        override fun drawMono(x: Int, y: Int, text: String, color: Int) = drawText(x, y, text, color)
        override fun monoWidth(text: String) = textWidth(text)
        override fun drawDisplay(x: Int, y: Int, text: String, color: Int) = drawText(x, y, text, color) // HUD editor preview draws no wordmark
        override fun displayWidth(text: String) = textWidth(text)
    }

    private fun ctx() = GameContext(
        fps = 641, inWorld = true, playerX = -21.1, playerY = 73.0, playerZ = 6.6,
        keyForward = true, keyBack = false, keyLeft = false, keyRight = false,
        yaw = 175f, dayTime = 3641L, speed = 0.0, leftCps = 0, rightCps = 0,
    )

    private fun fullManager(): ModuleManager =
        ModuleManager(Config(Files.createTempDirectory("preview"))).apply {
            register(FontModule()); register(FpsModule()); register(CoordsModule()); register(CompassModule()); register(KeystrokesModule())
            register(DirectionModule()); register(DayModule()); register(ClockModule()); register(SpeedModule()); register(CpsModule())
        }

    private fun frame(w: Int, h: Int): Pair<BufferedImage, AwtCanvas> {
        val img = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        val canvas = AwtCanvas(img, w, h)
        // fake world backdrop so scrim/contrast reads like in-game
        canvas.gradientV(0, 0, w, h, 0xFF3A4A2A.toInt(), 0xFF12180C.toInt())
        return img to canvas
    }

    private fun write(img: BufferedImage, name: String): File {
        val out = File("build/editor-preview").apply { mkdirs() }
        val up = BufferedImage(img.width * 2, img.height * 2, BufferedImage.TYPE_INT_ARGB)
        up.createGraphics().apply { drawImage(img, AffineTransform.getScaleInstance(2.0, 2.0), null); dispose() }
        val f = File(out, name); ImageIO.write(up, "png", f); return f
    }

    private fun cx(r: Rect) = r.left + r.width / 2
    private fun cy(r: Rect) = r.top + r.height / 2

    @Test fun `render all three editor tiers to PNG`() {
        val w = 520; val h = 360
        val renderer = EditorRenderer()

        // POSITION (with a selected element -> resize handle, and an active alignment guide)
        run {
            val (img, canvas) = frame(w, h); val mgr = fullManager(); val st = EditorState()
            val measurer = object : TextMeasurer {
                override fun width(text: String) = canvas.textWidth(text)
                override val lineHeight get() = canvas.lineHeight
            }
            val boxes = ElementLayout.boxesFor(mgr.hudModules(), ctx(), measurer, w, h)
            val fps = boxes.first { it.id == "fps" }.rect
            st.onPress(fps.left + fps.width / 2, fps.top + fps.height / 2, w, h, boxes, mgr) // select fps
            st.onDrag(w / 2 + 2, fps.top + fps.height / 2, w, h, mgr)                        // snap centre -> guide
            renderer.render(canvas, w, h, -1, -1, ctx(), mgr, st)
            assertTrue(write(img, "1-position.png").length() > 0)
        }
        // GRID
        run {
            val (img, canvas) = frame(w, h); val mgr = fullManager(); val st = EditorState()
            val mods = PositionLayout.modsButton(w, h); st.onPress(cx(mods), cy(mods), w, h, emptyList(), mgr)
            val cards = GridLayout.cards(w, h, mgr.all().size)
            renderer.render(canvas, w, h, cx(cards[1]), cy(cards[1]), ctx(), mgr, st) // hover the FPS card
            assertTrue(write(img, "2-grid.png").length() > 0)
        }
        // CUSTOMIZE (HUD module: FPS)
        run {
            val (img, canvas) = frame(w, h); val mgr = fullManager(); val st = EditorState()
            val mods = PositionLayout.modsButton(w, h); st.onPress(cx(mods), cy(mods), w, h, emptyList(), mgr)
            val idx = mgr.all().indexOfFirst { it.id == "keystrokes" }
            val card = GridLayout.cards(w, h, mgr.all().size)[idx]; st.onPress(cx(card), cy(card), w, h, emptyList(), mgr)
            renderer.render(canvas, w, h, -1, -1, ctx(), mgr, st)
            assertTrue(write(img, "3-customize-hud.png").length() > 0)
        }
        // CUSTOMIZE (non-HUD module: Font)
        run {
            val (img, canvas) = frame(w, h); val mgr = fullManager(); val st = EditorState()
            val mods = PositionLayout.modsButton(w, h); st.onPress(cx(mods), cy(mods), w, h, emptyList(), mgr)
            val idx = mgr.all().indexOfFirst { it.id == "font" }
            val card = GridLayout.cards(w, h, mgr.all().size)[idx]; st.onPress(cx(card), cy(card), w, h, emptyList(), mgr)
            renderer.render(canvas, w, h, -1, -1, ctx(), mgr, st)
            assertTrue(write(img, "4-customize-font.png").length() > 0)
        }
    }
}
