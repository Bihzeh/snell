package gg.maeve.launcher.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gg.maeve.launcher.ui.theme.Maeve
import gg.maeve.launcher.ui.theme.MaeveFonts

/**
 * Featured visual for a Minecraft version. ORIGINAL art only — never Mojang's official
 * key art (copyright; constraint #3). Uses resources/featured/<version>.png if present,
 * else a Compose-drawn stylized render.
 */
@Composable
fun VersionRender(version: String, modifier: Modifier = Modifier) {
    val resPath = "featured/$version.png"
    val hasImage = remember(version) { Thread.currentThread().contextClassLoader?.getResource(resPath) != null }

    Box(modifier.clip(RoundedCornerShape(14.dp))) {
        if (hasImage) {
            Image(painterResource(resPath), contentDescription = "Minecraft $version", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF2A2150), Color(0xFF171327))))) {
                Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0x308B6DFF), Color(0x00000000)))))
                MaeveMark(modifier = Modifier.align(Alignment.Center), size = 140.dp, color = Color(0x408B6DFF), gem = Color(0x40E2B45C))
                Column(Modifier.align(Alignment.BottomStart).padding(18.dp)) {
                    Text("MINECRAFT", color = Maeve.gold, letterSpacing = 2.sp, style = MaterialTheme.typography.labelSmall)
                    Text(version, fontFamily = MaeveFonts.Marcellus, fontSize = 46.sp, color = MaterialTheme.colorScheme.onBackground)
                }
            }
        }
    }
}
