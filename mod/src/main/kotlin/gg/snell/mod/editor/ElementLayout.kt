package gg.snell.mod.editor

import gg.snell.mod.module.HudModule
import gg.snell.mod.platform.GameContext
import gg.snell.mod.render.HudLayout
import kotlin.math.ceil

/** Text metrics the editor needs to size elements (HudCanvas already satisfies this). */
interface TextMeasurer {
    fun width(text: String): Int
    val lineHeight: Int
}

/** Computes on-screen bounds for HUD elements, matching the renderer's footprint math,
 *  so editor drag handles line up exactly with drawn pixels. Pure. */
object ElementLayout {
    fun boxesFor(
        modules: List<HudModule>, ctx: GameContext, m: TextMeasurer, screenW: Int, screenH: Int,
    ): List<ElementBox> = modules.mapNotNull { module ->
        val st = module.style
        val fp = module.footprint(ctx)
        if (fp != null) {
            val footW = ceil(fp.w * st.scale).toInt()
            val footH = ceil(fp.h * st.scale).toInt()
            val (l0, t0) = HudLayout.resolveTopLeft(module.anchor, module.offsetX, module.offsetY, footW, footH, screenW, screenH)
            return@mapNotNull ElementBox(module.id, Rect(l0, t0, footW, footH))
        }
        val lines = module.render(ctx)
        if (lines.isEmpty()) return@mapNotNull null
        val textW = lines.maxOf { m.width(it.text) }
        val textH = lines.size * m.lineHeight
        val footW = ceil((textW + st.padding * 2) * st.scale).toInt()
        val footH = ceil((textH + st.padding * 2) * st.scale).toInt()
        val (left, top) = HudLayout.resolveTopLeft(module.anchor, module.offsetX, module.offsetY, footW, footH, screenW, screenH)
        ElementBox(module.id, Rect(left, top, footW, footH))
    }
}
