package gg.snell.launcher.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import gg.snell.launcher.ui.theme.SnellMotion
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.util.Random
import kotlin.math.PI
import kotlin.math.sin

/** Preview/test hook: when non-null, [DynamicSky] is static at this day-fraction (no animation). */
val LocalSkyTimeOverride = staticCompositionLocalOf<Float?> { null }

enum class Body { Sun, Moon }

data class SkyState(
    val skyTop: Color,
    val skyBottom: Color,
    val body: Body,
    val bodyX: Float,     // 0..1 across the width
    val bodyY: Float,     // 0..1 of height (0 = top)
    val nightness: Float, // 0 = day, 1 = deep night
    val moonPhase: Int,   // 0..7
)

private class SkyKey(val f: Float, val top: Long, val bottom: Long)

// Top + bottom sky colours through the day (fraction of 24h). Wraps 1.0 -> 0.0.
private val SKY_KEYS = listOf(
    SkyKey(0.00f, 0xFF05060A, 0xFF0A0C14),  // midnight
    SkyKey(0.20f, 0xFF0A0F1E, 0xFF161B30),  // pre-dawn
    SkyKey(0.27f, 0xFF2A2E55, 0xFFE0794B),  // sunrise (warm horizon)
    SkyKey(0.34f, 0xFF3E6FC2, 0xFFAFC9EE),  // morning
    SkyKey(0.50f, 0xFF3F86DE, 0xFFB3D6F6),  // noon (bright blue)
    SkyKey(0.70f, 0xFF5277BE, 0xFFD6BB8E),  // afternoon
    SkyKey(0.78f, 0xFF35365F, 0xFFE8743C),  // sunset
    SkyKey(0.84f, 0xFF1C1B3A, 0xFF5A2E54),  // dusk (purple)
    SkyKey(0.92f, 0xFF0A0B16, 0xFF12131F),  // night
    SkyKey(1.00f, 0xFF05060A, 0xFF0A0C14),  // wrap -> midnight
)

private fun lerpColor(a: Long, b: Long, t: Float): Color {
    fun ch(v: Long, sh: Int) = ((v shr sh) and 0xFF).toInt()
    fun mix(x: Int, y: Int) = ((x + (y - x) * t)).toInt().coerceIn(0, 255)
    return Color(mix(ch(a, 16), ch(b, 16)), mix(ch(a, 8), ch(b, 8)), mix(ch(a, 0), ch(b, 0)))
}

private fun smoothstep(e0: Float, e1: Float, x: Float): Float {
    val t = ((x - e0) / (e1 - e0)).coerceIn(0f, 1f); return t * t * (3f - 2f * t)
}

private const val SUNRISE = 0.25f
private const val SUNSET = 0.79f

/** Pure: the sky for a given day-fraction (0..1) and epoch day (for moon phase). */
fun skyStateAt(dayFraction: Float, epochDay: Long): SkyState {
    val f = ((dayFraction % 1f) + 1f) % 1f
    var lo = SKY_KEYS[0]; var hi = SKY_KEYS[SKY_KEYS.size - 1]
    for (i in 0 until SKY_KEYS.size - 1) {
        if (f >= SKY_KEYS[i].f && f <= SKY_KEYS[i + 1].f) { lo = SKY_KEYS[i]; hi = SKY_KEYS[i + 1]; break }
    }
    val span = (hi.f - lo.f).coerceAtLeast(1e-4f)
    val t = ((f - lo.f) / span).coerceIn(0f, 1f)
    val top = lerpColor(lo.top, hi.top, t)
    val bottom = lerpColor(lo.bottom, hi.bottom, t)

    val isDay = f in SUNRISE..SUNSET
    val p = if (isDay) {
        (f - SUNRISE) / (SUNSET - SUNRISE)
    } else {
        val nightLen = (1f - SUNSET) + SUNRISE
        val into = if (f > SUNSET) f - SUNSET else f + (1f - SUNSET)
        into / nightLen
    }
    val bodyX = 0.08f + 0.84f * p
    val bodyY = 0.86f - sin(p * PI.toFloat()) * 0.74f

    val daylight = smoothstep(0.24f, 0.31f, f) * (1f - smoothstep(0.75f, 0.82f, f))
    val nightness = 1f - daylight
    val moonPhase = (((epochDay % 8) + 8) % 8).toInt()

    return SkyState(top, bottom, if (isDay) Body.Sun else Body.Moon, bodyX, bodyY, nightness, moonPhase)
}

private data class Star(val x: Float, val y: Float, val r: Float, val phase: Float, val speed: Float)
private data class Cloud(val x: Float, val y: Float, val scale: Float, val speed: Float)

private fun makeStars(n: Int): List<Star> {
    val rnd = Random(7)
    return List(n) { Star(rnd.nextFloat(), rnd.nextFloat() * 0.62f, 1.4f + rnd.nextFloat() * 1.8f, rnd.nextFloat() * 6.28f, 0.6f + rnd.nextFloat()) }
}

private fun makeClouds(n: Int): List<Cloud> {
    val rnd = Random(19)
    return List(n) { Cloud(rnd.nextFloat(), 0.10f + rnd.nextFloat() * 0.30f, 0.7f + rnd.nextFloat() * 0.8f, 0.3f + rnd.nextFloat() * 0.5f) }
}

private fun makeHills(cols: Int, seed: Long, base: Float, jitter: Float): FloatArray {
    val rnd = Random(seed); val out = FloatArray(cols); var h = base
    for (i in 0 until cols) { h = (h + (rnd.nextFloat() - 0.5f) * jitter).coerceIn(base - jitter, base + jitter); out[i] = h }
    return out
}

private fun currentDayFraction(): Float = LocalTime.now().toSecondOfDay() / 86400f

@Composable
fun DynamicSky(modifier: Modifier = Modifier) {
    val override = LocalSkyTimeOverride.current
    val epochDay = remember { LocalDate.now().toEpochDay() }
    var frac by remember { mutableStateOf(override ?: currentDayFraction()) }
    var anim by remember { mutableStateOf(0f) }
    if (override == null) {
        LaunchedEffect(SnellMotion.reduceMotion) {
            if (SnellMotion.reduceMotion) {
                // Reduce-motion: track time-of-day only, no ambient animation.
                while (true) { frac = currentDayFraction(); delay(2000) }
            } else {
                // 60fps ambient drift off the frame clock; refresh time-of-day ~every 1.5s.
                var t0 = 0L; var lastClock = 0L
                withInfiniteAnimationFrameNanos { t ->
                    if (t0 == 0L) { t0 = t; lastClock = t }
                    anim = (t - t0) / 1_000_000_000f
                    if (t - lastClock > 1_500_000_000L) { frac = currentDayFraction(); lastClock = t }
                }
            }
        }
    }
    val state = remember(frac, epochDay) { skyStateAt(frac, epochDay) }
    val stars = remember { makeStars(150) }
    val clouds = remember { makeClouds(5) }
    val far = remember { makeHills(40, 11, 0.74f, 0.07f) }
    val near = remember { makeHills(30, 23, 0.82f, 0.09f) }
    Canvas(modifier) { drawSky(state, stars, clouds, far, near, anim) }
}

private fun DrawScope.drawSky(s: SkyState, stars: List<Star>, clouds: List<Cloud>, far: FloatArray, near: FloatArray, anim: Float) {
    val w = size.width; val h = size.height
    drawRect(Brush.verticalGradient(listOf(s.skyTop, s.skyBottom)))

    // stars
    if (s.nightness > 0.02f) {
        for (st in stars) {
            val tw = 0.55f + 0.45f * sin(anim * st.speed + st.phase)
            drawRect(Color.White.copy(alpha = s.nightness * tw), topLeft = Offset(st.x * w, st.y * h), size = Size(st.r, st.r))
        }
    }

    // celestial body
    val bx = s.bodyX * w; val by = s.bodyY * h
    if (s.body == Body.Sun) drawSun(bx, by) else drawMoon(bx, by, s.moonPhase, s.skyBottom)

    // clouds (dim at night)
    val cloudA = (1f - s.nightness) * 0.7f
    if (cloudA > 0.03f) for (c in clouds) {
        val cx = (((c.x + anim * c.speed * 0.0012f) % 1.25f) - 0.12f) * w
        drawCloud(cx, c.y * h, c.scale * (w * 0.05f), Color.White.copy(alpha = cloudA))
    }

    // hills (static blocky silhouette, two layers)
    drawHills(far, Color(0xFF161826))
    drawHills(near, Color(0xFF0A0B12))
}

private fun DrawScope.drawSun(cx: Float, cy: Float) {
    val s = size.minDimension * 0.05f
    drawCircle(Color(0xFFFFE890).copy(alpha = 0.18f), radius = s * 3.0f, center = Offset(cx, cy))
    fun sq(half: Float, c: Color) = drawRect(c, topLeft = Offset(cx - half, cy - half), size = Size(half * 2, half * 2))
    sq(s * 1.15f, Color(0xFFFFC24B))
    sq(s * 0.82f, Color(0xFFFFE890))
    sq(s * 0.46f, Color(0xFFFFF7D0))
}

private fun DrawScope.drawMoon(cx: Float, cy: Float, phase: Int, skyColor: Color) {
    val s = size.minDimension * 0.045f
    val full = s * 2f
    drawRect(Color(0xFFE8ECF2), topLeft = Offset(cx - s, cy - s), size = Size(full, full))
    // approximate 8 phases by covering the unlit fraction with sky colour
    val lit = floatArrayOf(1f, 0.75f, 0.5f, 0.25f, 0f, 0.25f, 0.5f, 0.75f)[phase]
    val cover = 1f - lit
    if (cover > 0f) {
        val cw = full * cover
        val left = if (phase <= 4) cx + s - cw else cx - s    // waning/new cover from right, waxing from left
        drawRect(skyColor, topLeft = Offset(left, cy - s), size = Size(cw, full))
    }
}

private fun DrawScope.drawCloud(cx: Float, cy: Float, u: Float, c: Color) {
    // a few blocky rectangles forming a pixel cloud
    drawRect(c, topLeft = Offset(cx, cy + u * 0.4f), size = Size(u * 3f, u * 0.8f))
    drawRect(c, topLeft = Offset(cx + u * 0.6f, cy), size = Size(u * 1.8f, u * 0.8f))
    drawRect(c, topLeft = Offset(cx + u * 1.2f, cy + u * 0.4f), size = Size(u * 1.4f, u * 0.8f))
}

private fun DrawScope.drawHills(heights: FloatArray, color: Color) {
    val w = size.width; val h = size.height; val step = w / heights.size
    heights.forEachIndexed { i, top -> drawRect(color, topLeft = Offset(i * step, top * h), size = Size(step + 1f, h - top * h)) }
}
