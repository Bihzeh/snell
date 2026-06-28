package gg.snell.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gg.snell.launcher.ui.theme.SnellFonts

/** Minecraft-style floating nametag: low-opacity black plate + pixel font. */
@Composable
fun NameTag(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier.clip(RoundedCornerShape(3.dp)).background(Color.Black.copy(alpha = 0.28f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(text, fontFamily = SnellFonts.Pixel, color = Color.White, fontSize = 14.sp)
    }
}
