package gg.maeve.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import gg.maeve.launcher.ui.theme.Maeve
import gg.maeve.launcher.ui.theme.MaeveMotion
import kotlinx.coroutines.delay

enum class PromoKind(val label: String) { News("News"), PromotedServer("Promoted"), Ad("Sponsored") }

data class PromoSlide(val title: String, val subtitle: String, val kind: PromoKind, val accent: Color)

/** Placeholder promo content (our own). Future: ads / promoted servers / live news. */
@Composable
fun promoSlides(): List<PromoSlide> = listOf(
    PromoSlide("Welcome to Maeve", "A server-legal performance + quality-of-life client.", PromoKind.News, MaterialTheme.colorScheme.primary),
    PromoSlide("Your server, featured here", "Promoted servers are coming soon.", PromoKind.PromotedServer, Maeve.gold),
    PromoSlide("Tuned for 26.2", "Sodium, Lithium, and an in-game HUD — bundled.", PromoKind.News, Maeve.info),
)

/** Full-width paged banner carousel. Auto-advances unless reduce-motion is set. */
@Composable
fun Carousel(slides: List<PromoSlide>, modifier: Modifier = Modifier, height: Int = 132) {
    if (slides.isEmpty()) return
    var index by remember { mutableStateOf(0) }
    val count = slides.size

    LaunchedEffect(count) {
        if (!MaeveMotion.reduceMotion && count > 1) {
            while (true) { delay(6000); index = (index + 1) % count }
        }
    }
    val slide = slides[index.coerceIn(0, slides.lastIndex)]

    Box(modifier.fillMaxWidth().height(height.dp).clip(RoundedCornerShape(14.dp))) {
        Box(
            Modifier.fillMaxSize()
                .background(Brush.horizontalGradient(listOf(slide.accent.copy(alpha = 0.22f), MaterialTheme.colorScheme.surface)))
                .border(1.dp, Maeve.borderSoft, RoundedCornerShape(14.dp))
                .padding(22.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.clip(CircleShape).background(slide.accent.copy(alpha = 0.16f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text(slide.kind.label, color = slide.accent, style = MaterialTheme.typography.labelMedium)
                }
                Text(slide.title, style = MaterialTheme.typography.titleLarge)
                Text(slide.subtitle, color = Maeve.text2, style = MaterialTheme.typography.bodyMedium)
            }

            if (count > 1) {
                Row(
                    Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    repeat(count) { i ->
                        Box(Modifier.size(if (i == index) 8.dp else 6.dp).clip(CircleShape).background(if (i == index) slide.accent else Maeve.text3))
                    }
                }
                Row(Modifier.align(Alignment.CenterEnd), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Arrow("‹") { index = (index - 1 + count) % count }
                    Arrow("›") { index = (index + 1) % count }
                }
            }
        }
    }
}

@Composable
private fun Arrow(glyph: String, onClick: () -> Unit) {
    Box(Modifier.size(28.dp).clip(CircleShape).background(Maeve.elevated2).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(glyph, color = MaterialTheme.colorScheme.onBackground)
    }
}
