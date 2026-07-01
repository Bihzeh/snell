package gg.snell.mod.ui.node

import gg.snell.mod.editor.Rect
import gg.snell.mod.editor.Size
import gg.snell.mod.platform.EditorCanvas

private fun Rect.inset(e: Edge) =
    Rect(left + e.l, top + e.t, (width - e.l - e.r).coerceAtLeast(0), (height - e.t - e.b).coerceAtLeast(0))

/**
 * A small flex/stack layout engine: [measure] computes intrinsic sizes bottom-up (only where `Auto` is
 * used); [arrange] resolves `Flex`/`Frac` against the parent and assigns every `rect` top-down. One pass,
 * no constraint solver. `Row`/`Column` flow children; `Stack` overlaps them by `Anchor`.
 */
object Layout {

    /** Lay [root] into the [w]×[h] screen using text [m]etrics. */
    fun layout(root: Node, w: Int, h: Int, m: Metrics) {
        measure(root, m)
        arrange(root, Rect(0, 0, w, h), m)
    }

    // ---- intrinsic sizes (bottom-up) ----------------------------------------------------------
    private fun measure(n: Node, m: Metrics): Size {
        n.children.forEach { measure(it, m) }
        val self = n.measure?.invoke(m) ?: Size(0, 0)
        val cw: Int
        val ch: Int
        when (n.dir) {
            Dir.Row -> { cw = n.children.sumOf { it.iw } + gaps(n); ch = n.children.maxOfOrNull { it.ih } ?: 0 }
            Dir.Column -> { cw = n.children.maxOfOrNull { it.iw } ?: 0; ch = n.children.sumOf { it.ih } + gaps(n) }
            Dir.Stack -> { cw = n.children.maxOfOrNull { it.iw } ?: 0; ch = n.children.maxOfOrNull { it.ih } ?: 0 }
        }
        n.iw = intrinsic(n.width, maxOf(cw, self.w) + n.padding.l + n.padding.r)
        n.ih = intrinsic(n.height, maxOf(ch, self.h) + n.padding.t + n.padding.b)
        return Size(n.iw, n.ih)
    }

    private fun intrinsic(len: Len, content: Int) = if (len is Len.Fixed) len.px else content
    private fun gaps(n: Node) = (n.children.size - 1).coerceAtLeast(0) * n.gap

    // ---- placement (top-down) -----------------------------------------------------------------
    private fun arrange(n: Node, rect: Rect, m: Metrics) {
        n.rect = rect
        val inner = rect.inset(n.padding)
        if (n.lazy != null) { arrangeLazy(n, inner, m); return }
        if (n.children.isEmpty()) return
        when (n.dir) {
            Dir.Stack -> n.children.forEach { c ->
                val cw = resolve(c.width, inner.width, c.iw)
                val cht = resolve(c.height, inner.height, c.ih)
                arrange(c, place(inner, cw, cht, c.anchor, c.offset.x, c.offset.y), m)
            }
            else -> flow(n, inner, m)
        }
    }

    private fun resolve(len: Len, avail: Int, intrinsic: Int): Int = when (len) {
        is Len.Fixed -> len.px
        is Len.Auto -> intrinsic
        is Len.Frac -> (avail * len.f).toInt().coerceIn(len.min, len.max)
        is Len.Flex -> avail.coerceIn(len.min, len.max) // fill (Stack child / cross axis)
    }

    private fun place(inner: Rect, w: Int, h: Int, a: Anchor, ox: Int, oy: Int): Rect {
        val x = when (a) {
            Anchor.TopLeft, Anchor.CenterLeft, Anchor.BottomLeft -> inner.left
            Anchor.TopCenter, Anchor.Center, Anchor.BottomCenter -> inner.left + (inner.width - w) / 2
            else -> inner.right - w
        }
        val y = when (a) {
            Anchor.TopLeft, Anchor.TopCenter, Anchor.TopRight -> inner.top
            Anchor.CenterLeft, Anchor.Center, Anchor.CenterRight -> inner.top + (inner.height - h) / 2
            else -> inner.bottom - h
        }
        return Rect(x + ox, y + oy, w, h)
    }

    private fun flow(n: Node, inner: Rect, m: Metrics) {
        val row = n.dir == Dir.Row
        val mainAvail = if (row) inner.width else inner.height
        val crossAvail = if (row) inner.height else inner.width
        // 1. fixed/auto/frac extents; flex starts at 0
        val ext = n.children.map { c ->
            when (val len = if (row) c.width else c.height) {
                is Len.Fixed -> len.px
                is Len.Auto -> if (row) c.iw else c.ih
                is Len.Frac -> (mainAvail * len.f).toInt().coerceIn(len.min, len.max)
                is Len.Flex -> 0
            }
        }.toIntArray()
        // 2. distribute leftover to flex children by weight (remainder to the last flex child)
        val weights = n.children.map { (if (row) it.width else it.height).let { l -> (l as? Len.Flex)?.weight ?: 0 } }
        val wsum = weights.sum()
        if (wsum > 0) {
            val leftover = (mainAvail - gaps(n) - ext.sum()).coerceAtLeast(0)
            var lastFlex = -1
            var used = 0
            n.children.indices.forEach { i ->
                if (weights[i] > 0) { ext[i] = leftover * weights[i] / wsum; used += ext[i]; lastFlex = i }
            }
            if (lastFlex >= 0) ext[lastFlex] += leftover - used
        }
        // 3. main-axis start (Center/End shift the whole run)
        val run = ext.sum() + gaps(n)
        var cur = (if (row) inner.left else inner.top) + when (n.main) {
            Main.Start -> 0
            Main.Center -> (mainAvail - run) / 2
            Main.End -> mainAvail - run
        }
        // 4. place + cross-align each child, recurse
        n.children.forEachIndexed { i, c ->
            val crossLen = if (row) c.height else c.width
            val crossExt = if (n.cross == Cross.Stretch) crossAvail else resolve(crossLen, crossAvail, if (row) c.ih else c.iw)
            val crossOff = when (n.cross) {
                Cross.Start, Cross.Stretch -> 0
                Cross.Center -> (crossAvail - crossExt) / 2
                Cross.End -> crossAvail - crossExt
            }
            val r = if (row) Rect(cur, inner.top + crossOff, ext[i], crossExt)
            else Rect(inner.left + crossOff, cur, crossExt, ext[i])
            arrange(c, r, m)
            cur += ext[i] + n.gap
        }
    }

    private fun arrangeLazy(n: Node, inner: Rect, m: Metrics) {
        val l = n.lazy!!
        val stride = l.itemH + l.gap
        if (l.count == 0 || stride <= 0) { n.lazyKids = emptyList(); return }
        val first = (l.scrollY / stride).coerceAtLeast(0)
        val last = ((l.scrollY + inner.height) / stride).coerceAtMost(l.count - 1)
        n.lazyKids = if (last < first) emptyList() else (first..last).map { i ->
            val c = l.item(i)
            measure(c, m)
            arrange(c, Rect(inner.left, inner.top + i * stride - l.scrollY, inner.width, l.itemH), m)
            c
        }
    }
}

// ---- tree walks (draw / hit-test / lookup) ------------------------------------------------------

/** Paint this node then its children (children draw above the parent). Cursor is for hover only.
 *  A [Node.clip] node scissors its children to its rect (lists: partial rows can't leak outside). */
fun Node.render(c: EditorCanvas, mx: Int, my: Int) {
    paint?.draw(c, rect, mx, my)
    if (clip) {
        c.withClip(rect.left, rect.top, rect.width, rect.height) { for (k in kids()) k.render(c, mx, my) }
    } else {
        for (k in kids()) k.render(c, mx, my)
    }
}

/** Id of the topmost node under the cursor (children tested first; [clip] nodes reject outside points). */
fun Node.hit(mx: Int, my: Int): String? {
    if (clip && !rect.contains(mx, my)) return null
    for (k in kids().asReversed()) k.hit(mx, my)?.let { return it }
    return if (id != null && rect.contains(mx, my)) id else null
}

/** First node in the tree with [id], or null. */
fun Node.find(id: String): Node? =
    if (this.id == id) this else kids().firstNotNullOfOrNull { it.find(id) }
