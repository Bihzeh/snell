package gg.maeve.launcher.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Twilight Royal palette (from the Claude Design tokens). */
private val MaeveColorScheme = darkColorScheme(
    primary = Color(0xFF8B6DFF),
    onPrimary = Color(0xFF0B0A12),
    background = Color(0xFF0B0A12),
    onBackground = Color(0xFFECEAF5),
    surface = Color(0xFF14121E),
    onSurface = Color(0xFFECEAF5),
    surfaceVariant = Color(0xFF1C1A2A),
    onSurfaceVariant = Color(0xFFA7A3C0),
    outline = Color(0xFF2A2740),
    error = Color(0xFFFF6B7A),
    onError = Color(0xFF0B0A12),
)

/** Tokens Material 3's ColorScheme does not model. */
@Immutable
data class MaeveColors(
    val elevated: Color = Color(0xFF1C1A2A),
    val elevated2: Color = Color(0xFF23202F),
    val border: Color = Color(0xFF2A2740),
    val borderSoft: Color = Color(0xFF211E30),
    val text2: Color = Color(0xFFA7A3C0),
    val text3: Color = Color(0xFF6E6A88),
    val textDisabled: Color = Color(0xFF514E68),
    val accentHover: Color = Color(0xFF9D82FF),
    val accentPressed: Color = Color(0xFF7857F0),
    val accentSubtle: Color = Color(0x248B6DFF),
    val gold: Color = Color(0xFFE2B45C),
    val goldSubtle: Color = Color(0x21E2B45C),
    val success: Color = Color(0xFF48D597),
    val warning: Color = Color(0xFFF2B84B),
    val info: Color = Color(0xFF69B6FF),
)

val LocalMaeveColors = staticCompositionLocalOf { MaeveColors() }

/** Motion tokens; honor [reduceMotion] by snapping. */
object MaeveMotion {
    const val quick = 120
    const val standard = 160
    const val emphasized = 200
    val easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    var reduceMotion: Boolean = false
}

private val MaeveTypography = Typography().run {
    val g = MaeveFonts.Geist
    copy(
        displayLarge = TextStyle(fontFamily = MaeveFonts.Marcellus, fontWeight = FontWeight.Normal, fontSize = 40.sp, lineHeight = 46.sp),
        headlineMedium = TextStyle(fontFamily = g, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 34.sp),
        titleLarge = TextStyle(fontFamily = g, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
        titleMedium = TextStyle(fontFamily = g, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
        bodyLarge = TextStyle(fontFamily = g, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = TextStyle(fontFamily = g, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
        bodySmall = TextStyle(fontFamily = g, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
        labelLarge = TextStyle(fontFamily = g, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 18.sp),
        labelMedium = TextStyle(fontFamily = g, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
        labelSmall = TextStyle(fontFamily = g, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp),
    )
}

private val MaeveShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
)

@Composable
fun MaeveTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalMaeveColors provides MaeveColors()) {
        MaterialTheme(
            colorScheme = MaeveColorScheme,
            typography = MaeveTypography,
            shapes = MaeveShapes,
            content = content,
        )
    }
}

/** Shorthand for the extended palette: `Maeve.gold` etc. */
val Maeve: MaeveColors
    @Composable @ReadOnlyComposable get() = LocalMaeveColors.current
