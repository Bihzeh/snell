package gg.snell.launcher.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gg.snell.launcher.ui.theme.Snell
import gg.snell.launcher.ui.theme.SnellFonts

/**
 * Featured visual for a Minecraft version. ORIGINAL art only — never Mojang's official key art
 * (copyright). Uses resources/featured/<version>.png if present, else a Compose-drawn render in
 * the Obsidian palette (diagonal weave + emerald glow).
 */
@Composable
fun VersionRender(version: String, modifier: Modifier = Modifier, showOverlay: Boolean = true) {
    val resPath = "featured/$version.png"
    val hasImage = remember(version) { Thread.currentThread().contextClassLoader?.getResource(resPath) != null }

    Box(modifier.clip(RoundedCornerShape(14.dp))) {
        if (hasImage) {
            Image(painterResource(resPath), contentDescription = "Minecraft $version", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Snell.ka1, Snell.ka2)))) {
                Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Snell.accent.copy(alpha = 0.20f), Color.Transparent))))
                if (showOverlay) {
                    Image(
                        painter = painterResource(Brand.WATERMARK),
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.Center).size(132.dp),
                        alpha = 0.55f,
                    )
                    Column(Modifier.align(Alignment.BottomStart).padding(18.dp)) {
                        Text("MINECRAFT", color = Snell.ember, letterSpacing = 2.sp, style = MaterialTheme.typography.labelSmall)
                        Text(version, fontFamily = SnellFonts.Display, fontWeight = FontWeight.Bold, fontSize = 46.sp, color = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }
        }
    }
}
