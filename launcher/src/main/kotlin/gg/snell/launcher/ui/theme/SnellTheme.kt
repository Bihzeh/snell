package gg.snell.launcher.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * "Snell" palette (Claude Design import). Near-black surfaces, cyan accent, pink warm highlight. Maps the design's CSS `:root` tokens onto Material 3.
 */
private val SnellColorScheme = darkColorScheme(
    primary = Color(0xFF00D9FF),        // --ac
    onPrimary = Color(0xFF04222B),      // dark teal text on the bright cyan accent
    background = Color(0xFF0A0A0A),      // --bg
    onBackground = Color(0xFFECECEC),
    surface = Color(0xFF141414),        // --s1
    onSurface = Color(0xFFECECEC),
    surfaceVariant = Color(0xFF1E1E1E), // --s2
    onSurfaceVariant = Color(0xFF9C9C9C),
    outline = Color(0xFF262626),        // --bd
    error = Color(0xFFF06B6B),
    onError = Color(0xFFFFFFFF),
)

/** Tokens Material 3's ColorScheme does not model (the design's extended ramp). */
@Immutable
data class SnellColors(
    val accent: Color = Color(0xFF00D9FF),      // --ac
    val accentHi: Color = Color(0xFF5CE6FF),    // --ac-hi
    val accentLo: Color = Color(0xFF00A6C2),    // --ac-lo
    val accentSubtle: Color = Color(0x2900D9FF),// ~16% accent fill (chips, active nav)
    val onAccent: Color = Color(0xFF04222B),    // text/icons on the accent (dark teal)
    val bg2: Color = Color(0xFF0E0E0E),         // --bg2 (title bar, rail)
    val s1: Color = Color(0xFF141414),          // --s1 (cards)
    val s2: Color = Color(0xFF1E1E1E),          // --s2 (inset controls)
    val border: Color = Color(0xFF262626),      // --bd
    val ka1: Color = Color(0xFF151515),         // --ka1 (art stripes)
    val ka2: Color = Color(0xFF1C1C1C),         // --ka2
    val text2: Color = Color(0xFF9C9C9C),       // secondary
    val text3: Color = Color(0xFF6E6E6E),       // muted
    val textDisabled: Color = Color(0xFF4A4A4A),
    val ember: Color = Color(0xFFFF4D9D),       // warning / promoted / founder (pink)
    val warning: Color = Color(0xFFFF4D9D),
    val danger: Color = Color(0xFFF06B6B),      // error / offline
    val info: Color = Color(0xFF6FA8FF),        // downloading
    val success: Color = Color(0xFF00D9FF),     // == accent
)

val LocalSnellColors = staticCompositionLocalOf { SnellColors() }

/** Pill shape used by status badges, chips, switches. */
val PillShape = RoundedCornerShape(percent = 50)

/** Motion tokens; honor [reduceMotion] by snapping. */
object SnellMotion {
    const val quick = 120
    const val standard = 160
    const val emphasized = 200
    val easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
    var reduceMotion by mutableStateOf(false)
}

private val SnellTypography = Typography().run {
    val d = SnellFonts.Display // Outfit — headings, labels, wordmark, numerics
    val b = SnellFonts.Body    // Hanken Grotesk — body + buttons
    copy(
        displayLarge = displayLarge.copy(fontFamily = d, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 46.sp),
        displayMedium = displayMedium.copy(fontFamily = d, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 38.sp),
        displaySmall = displaySmall.copy(fontFamily = d, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 32.sp),
        headlineLarge = headlineLarge.copy(fontFamily = d, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp),
        headlineMedium = headlineMedium.copy(fontFamily = d, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
        headlineSmall = headlineSmall.copy(fontFamily = d, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
        titleLarge = titleLarge.copy(fontFamily = d, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
        titleMedium = titleMedium.copy(fontFamily = d, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
        titleSmall = titleSmall.copy(fontFamily = d, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp),
        bodyLarge = bodyLarge.copy(fontFamily = b, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
        bodyMedium = bodyMedium.copy(fontFamily = b, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
        bodySmall = bodySmall.copy(fontFamily = b, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
        labelLarge = labelLarge.copy(fontFamily = b, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp),
        labelMedium = labelMedium.copy(fontFamily = d, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
        labelSmall = labelSmall.copy(fontFamily = d, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp),
    )
}

private val SnellShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(16.dp),
)

@Composable
fun SnellTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = SnellColorScheme, typography = SnellTypography, shapes = SnellShapes) {
        // Default content color must be light — Compose's default is Color.Black, invisible
        // on near-black surfaces (any Text/icon without an explicit color).
        CompositionLocalProvider(
            LocalSnellColors provides SnellColors(),
            LocalContentColor provides SnellColorScheme.onBackground,
            content = content,
        )
    }
}

/** Shorthand for the extended palette: `Snell.accent` etc. */
val Snell: SnellColors
    @Composable @ReadOnlyComposable get() = LocalSnellColors.current
