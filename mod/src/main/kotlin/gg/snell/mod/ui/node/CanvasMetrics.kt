package gg.snell.mod.ui.node

import gg.snell.mod.platform.EditorCanvas

/** Adapt an [EditorCanvas] to the layout [Metrics] interface (it already exposes the same widths). */
fun EditorCanvas.asMetrics(): Metrics = object : Metrics {
    override fun textWidth(s: String) = this@asMetrics.textWidth(s)
    override fun monoWidth(s: String) = this@asMetrics.monoWidth(s)
    override fun displayWidth(s: String) = this@asMetrics.displayWidth(s)
    override val lineHeight get() = this@asMetrics.lineHeight
}
