package gg.snell.mod.ui.node

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Pure-layout regression for the Node/Layout engine (no canvas; fixed metrics). */
class NodeLayoutTest {

    /** Minimal canvas that records [withClip] rects; every other member is a no-op. */
    private class ClipRecordingCanvas : gg.snell.mod.platform.EditorCanvas {
        val clips = mutableListOf<gg.snell.mod.editor.Rect>()
        override fun withClip(x: Int, y: Int, w: Int, h: Int, body: () -> Unit) {
            clips += gg.snell.mod.editor.Rect(x, y, w, h)
            body()
        }

        override fun drawText(x: Int, y: Int, text: String, color: Int) {}
        override fun drawStyledText(x: Int, y: Int, text: String, run: gg.snell.mod.platform.TextRun) {}
        override fun fill(x: Int, y: Int, w: Int, h: Int, color: Int) {}
        override fun withScale(scale: Float, pivotX: Int, pivotY: Int, body: () -> Unit) = body()
        override fun textWidth(text: String) = text.length * 6
        override fun drawMono(x: Int, y: Int, text: String, color: Int) {}
        override fun monoWidth(text: String) = text.length * 6
        override val lineHeight = 9
        override val screenWidth = 200
        override val screenHeight = 100
        override fun border(x: Int, y: Int, w: Int, h: Int, color: Int) {}
        override fun gradientV(x: Int, y: Int, w: Int, h: Int, top: Int, bottom: Int) {}
        override fun overlayStratum() {}
        override fun drawIcon(glyph: Char, x: Int, y: Int, color: Int) {}
        override fun iconWidth(glyph: Char) = 8
        override fun drawTexture(id: String, x: Int, y: Int, w: Int, h: Int) {}
        override fun drawDisplay(x: Int, y: Int, text: String, color: Int) {}
        override fun displayWidth(text: String) = text.length * 40
        override fun sprite(id: String, x: Int, y: Int, w: Int, h: Int, tint: Int) {}
    }

    @Test fun `render wraps a clip node's children in withClip so overflow can't paint outside`() {
        val c = ClipRecordingCanvas()
        val list = Node(
            id = "list", clip = true, width = Len.Fixed(100), height = Len.Fixed(50),
            children = listOf(Node(width = Len.Fixed(10), height = Len.Fixed(10))),
        )
        val root = Node(dir = Dir.Stack, width = Len.Fixed(200), height = Len.Fixed(100), children = listOf(list)).laidOut(200, 100)
        root.render(c, -1, -1)
        assertEquals(listOf(list.rect), c.clips, "clip node scissors exactly its rect around its children")
    }

    private object FixedMetrics : Metrics {
        override fun textWidth(s: String) = s.length * 6
        override fun monoWidth(s: String) = s.length * 6
        override fun displayWidth(s: String) = s.length * 40
        override val lineHeight = 9
    }

    private fun Node.laidOut(w: Int, h: Int) = also { Layout.layout(it, w, h, FixedMetrics) }

    @Test fun `frac width clamps and flex spacer pushes content to the bottom`() {
        val foot = Node(id = "foot", height = Len.Fixed(22))
        val col = Node(
            anchor = Anchor.TopLeft, width = Len.Frac(0.46f, 200, 280), height = Len.Flex(),
            dir = Dir.Column, gap = 0, cross = Cross.Stretch, // rows fill the column width
            children = listOf(Node(id = "logo", height = Len.Fixed(30)), spacer(), foot),
        )
        val root = Node(dir = Dir.Stack, width = Len.Flex(), height = Len.Flex(), padding = Edge.all(22), children = listOf(col))
        root.laidOut(1000, 600)

        // 0.46*956(inner) = 439 -> clamped to max 280
        assertEquals(280, col.find("foot")!!.rect.width)
        // foot pinned to the bottom of the padded column (h=600, pad 22 => inner bottom 578, foot height 22)
        assertEquals(578, foot.rect.bottom)
        // logo at the top
        assertEquals(22, col.find("logo")!!.rect.top)
    }

    @Test fun `row distributes flex by weight with the remainder to the last flex child`() {
        val a = Node(id = "a", width = Len.Flex(1))
        val b = Node(id = "b", width = Len.Flex(1))
        val row = Node(dir = Dir.Row, width = Len.Fixed(101), height = Len.Fixed(10), gap = 0, children = listOf(a, b))
        Node(children = listOf(row)).laidOut(200, 50)
        assertEquals(50, a.rect.width)             // 101/2 = 50
        assertEquals(51, b.rect.width)             // remainder to the last flex child
        assertEquals(a.rect.right, b.rect.left)    // contiguous
    }

    @Test fun `stack anchors place children at the right corners`() {
        val tr = Node(id = "tr", width = Len.Fixed(40), height = Len.Fixed(20), anchor = Anchor.TopRight)
        val br = Node(id = "br", width = Len.Fixed(30), height = Len.Fixed(10), anchor = Anchor.BottomRight)
        val root = Node(dir = Dir.Stack, width = Len.Flex(), height = Len.Flex(), padding = Edge.all(8), children = listOf(tr, br))
        root.laidOut(200, 100)
        assertEquals(192, tr.rect.right)   // 200 - pad 8
        assertEquals(8, tr.rect.top)
        assertEquals(192, br.rect.right)
        assertEquals(92, br.rect.bottom)   // 100 - pad 8
    }

    @Test fun `lazy list virtualizes and rows hit-test correctly`() {
        val list = Node(
            id = "list", clip = true, width = Len.Flex(), height = Len.Flex(),
            lazy = Lazy(count = 100, itemH = 20, gap = 4, scrollY = 0) { i ->
                Node(id = "row:$i", width = Len.Flex(), height = Len.Fixed(20))
            },
        )
        list.laidOut(300, 200)
        assertTrue(list.lazyKids.size < 100, "virtualized: only visible rows built")
        val r0 = list.find("row:0")!!.rect
        val r1 = list.find("row:1")!!.rect
        assertEquals(0, r0.top)
        assertEquals(24, r1.top)                          // itemH 20 + gap 4
        assertEquals("row:0", list.hit(r0.left + 2, r0.top + 2))
        assertNull(list.hit(310, 210))                    // outside the clipped list
    }

    @Test fun `hit returns the topmost child id`() {
        val under = Node(id = "under", width = Len.Fixed(100), height = Len.Fixed(100), anchor = Anchor.TopLeft)
        val over = Node(id = "over", width = Len.Fixed(40), height = Len.Fixed(40), anchor = Anchor.TopLeft)
        val root = Node(dir = Dir.Stack, width = Len.Flex(), height = Len.Flex(), children = listOf(under, over))
        root.laidOut(200, 200)
        assertEquals("over", root.hit(10, 10))            // over is later in the list -> on top
        assertEquals("under", root.hit(60, 60))           // outside over, inside under
    }

    @Test fun `auto text node sizes to its content`() {
        val label = Node(id = "t", width = Len.Auto, height = Len.Auto, measure = { m -> textSize(m, "Hello") })
        Node(children = listOf(label)).laidOut(200, 50)
        assertEquals(30, label.rect.width)   // 5 chars * 6
        assertEquals(9, label.rect.height)   // lineHeight
    }
}
