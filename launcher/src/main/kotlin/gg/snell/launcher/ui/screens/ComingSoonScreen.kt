package gg.snell.launcher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import gg.snell.launcher.ui.components.StatusPill
import gg.snell.launcher.ui.components.PillKind
import gg.snell.launcher.ui.components.SymIcon
import gg.snell.launcher.ui.theme.Snell

/** Reserved-space placeholder for roadmap screens (Cosmetics P3, Friends P4). */
@Composable
fun ComingSoonScreen(icon: String, title: String, blurb: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.width(360.dp),
        ) {
            Box(
                Modifier.size(72.dp).clip(RoundedCornerShape(18.dp)).background(Snell.accentSubtle),
                contentAlignment = Alignment.Center,
            ) { SymIcon(icon, 36.dp, Snell.accentHi) }
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
            StatusPill("Coming soon", PillKind.UpdateAvailable, showDot = false)
            Text(blurb, style = MaterialTheme.typography.bodyMedium, color = Snell.text2, textAlign = TextAlign.Center)
        }
    }
}
