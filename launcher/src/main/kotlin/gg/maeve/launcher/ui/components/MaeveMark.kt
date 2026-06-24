package gg.maeve.launcher.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The Maeve crown mark — translated from the design's 48x48 SVG. */
@Composable
fun MaeveMark(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    color: Color = Color(0xFF8B6DFF),
    gem: Color = Color(0xFFE2B45C),
) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.minDimension / 48f
        fun pt(x: Float, y: Float) = Offset(x * s, y * s)
        val crown = Path().apply {
            moveTo(6 * s, 35 * s); lineTo(8.5f * s, 15 * s); lineTo(17 * s, 25 * s)
            lineTo(24 * s, 9 * s); lineTo(31 * s, 25 * s); lineTo(39.5f * s, 15 * s)
            lineTo(42 * s, 35 * s); close()
        }
        drawPath(crown, color)
        drawRoundRect(color, topLeft = pt(6f, 36.5f), size = Size(36 * s, 5 * s), cornerRadius = CornerRadius(2 * s, 2 * s))
        val gemPath = Path().apply {
            moveTo(24 * s, 17.5f * s); lineTo(27.2f * s, 21 * s); lineTo(24 * s, 24.5f * s); lineTo(20.8f * s, 21 * s); close()
        }
        drawPath(gemPath, gem)
        drawCircle(gem, radius = 2 * s, center = pt(8.5f, 14f))
        drawCircle(gem, radius = 2 * s, center = pt(39.5f, 14f))
    }
}
