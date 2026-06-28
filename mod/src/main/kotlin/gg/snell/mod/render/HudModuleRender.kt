package gg.snell.mod.render

import gg.snell.mod.module.HudModule
import gg.snell.mod.platform.GameContext
import gg.snell.mod.platform.HudCanvas
import gg.snell.mod.platform.TextRun
import kotlin.math.ceil

/** Draws a single HUD module: either its own custom graphics (boxed modules) or its text lines,
 *  with the module's anchor/scale/style/background. Pure; shared by the in-game renderer and the
 *  editor preview so they position elements identically. */
object HudModuleRender {
    fun draw(canvas: HudCanvas, module: HudModule, ctx: GameContext) {
        val style = module.style
        val fp = module.footprint(ctx)
        if (fp != null) {
            val (left, top) = HudLayout.resolveTopLeft(
                module.anchor, module.offsetX, module.offsetY,
                ceil(fp.w * style.scale).toInt(), ceil(fp.h * style.scale).toInt(),
                canvas.screenWidth, canvas.screenHeight,
            )
            canvas.withScale(style.scale, left, top) {
                if (style.background) canvas.fill(0, 0, fp.w, fp.h, style.backgroundColor)
                module.drawCustom(canvas, ctx)
            }
            return
        }

        val lines = module.render(ctx)
        if (lines.isEmpty()) return
        val pad = style.padding
        val textW = lines.maxOf { canvas.textWidth(it.text) }
        val textH = lines.size * canvas.lineHeight
        val localW = textW + pad * 2
        val localH = textH + pad * 2
        val footW = ceil(localW * style.scale).toInt()
        val footH = ceil(localH * style.scale).toInt()
        val (left, top) = HudLayout.resolveTopLeft(
            module.anchor, module.offsetX, module.offsetY, footW, footH, canvas.screenWidth, canvas.screenHeight,
        )
        canvas.withScale(style.scale, left, top) {
            if (style.background) canvas.fill(0, 0, localW, localH, style.backgroundColor)
            var lineY = pad
            for (line in lines) {
                val lineW = canvas.textWidth(line.text)
                val lineX = pad + HudLayout.lineX(0, textW, lineW, style.align)
                val color = line.color ?: style.color
                canvas.drawStyledText(
                    lineX, lineY, line.text,
                    TextRun(color, style.bold, style.italic, style.underline, style.strikethrough, style.shadow),
                )
                lineY += canvas.lineHeight
            }
        }
    }
}
