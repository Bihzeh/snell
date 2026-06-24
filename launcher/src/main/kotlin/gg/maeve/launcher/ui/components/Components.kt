package gg.maeve.launcher.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import gg.maeve.launcher.ui.theme.Maeve

enum class ButtonVariant { Primary, Secondary, Ghost }

@Composable
fun MaeveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Primary,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    fillWidth: Boolean = false,
) {
    val cs = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val bg = when {
        !enabled -> if (variant == ButtonVariant.Primary) cs.primary.copy(alpha = 0.4f) else Color.Transparent
        variant == ButtonVariant.Primary -> if (hovered) Maeve.accentHover else cs.primary
        else -> if (hovered) Maeve.elevated2 else Color.Transparent
    }
    val fg = when (variant) {
        ButtonVariant.Primary -> cs.onPrimary
        else -> if (enabled) cs.onSurface else Maeve.textDisabled
    }
    var m = modifier.then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
        .clip(RoundedCornerShape(10.dp))
        .background(bg)
    if (variant == ButtonVariant.Secondary) m = m.border(1.dp, Maeve.border, RoundedCornerShape(10.dp))
    m = m.clickable(enabled = enabled, interactionSource = interaction, indication = null, onClick = onClick)
        .padding(horizontal = 18.dp, vertical = 11.dp)
    Row(m, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        leading?.invoke()
        Text(text, color = fg, style = MaterialTheme.typography.labelLarge)
    }
}

/** The big primary Play button with an accent glow + play glyph. */
@Composable
fun PlayButton(enabled: Boolean, label: String = "Play", onClick: () -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val bg by animateColorAsState(if (!enabled) cs.primary.copy(alpha = 0.4f) else if (hovered) Maeve.accentHover else cs.primary)
    Row(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(enabled = enabled, interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 30.dp, vertical = 15.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(13.dp)) {
            val p = Path().apply { moveTo(0f, 0f); lineTo(size.width, size.height / 2f); lineTo(0f, size.height); close() }
            drawPath(p, cs.onPrimary)
        }
        Text(label, color = cs.onPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

enum class PillKind { UpToDate, Downloading, UpdateAvailable, Online, Offline, Failed, Neutral }

@Composable
fun StatusPill(label: String, kind: PillKind, modifier: Modifier = Modifier) {
    val color = when (kind) {
        PillKind.UpToDate, PillKind.Online -> Maeve.success
        PillKind.Downloading -> Maeve.info
        PillKind.UpdateAvailable -> Maeve.gold
        PillKind.Failed -> MaterialTheme.colorScheme.error
        PillKind.Offline, PillKind.Neutral -> Maeve.text3
    }
    Row(
        modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(color))
        Text(label, color = color, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun MaeveCard(modifier: Modifier = Modifier, padding: PaddingValues = PaddingValues(20.dp), content: @Composable () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, Maeve.borderSoft, RoundedCornerShape(14.dp))
            .padding(padding),
    ) { content() }
}

@Composable
fun MaeveProgress(fraction: Float?, modifier: Modifier = Modifier) {
    val mod = modifier.fillMaxWidth().height(6.dp).clip(CircleShape)
    if (fraction != null) {
        LinearProgressIndicator(progress = { fraction.coerceIn(0f, 1f) }, modifier = mod, color = MaterialTheme.colorScheme.primary, trackColor = Maeve.elevated2)
    } else {
        LinearProgressIndicator(modifier = mod, color = MaterialTheme.colorScheme.primary, trackColor = Maeve.elevated2)
    }
}

@Composable
fun Spinner(sizeDp: Int = 18, modifier: Modifier = Modifier) {
    CircularProgressIndicator(modifier = modifier.size(sizeDp.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
}

@Composable
fun ModRow(name: String, description: String, enabled: Boolean, onToggle: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface)
            .border(1.dp, Maeve.borderSoft, RoundedCornerShape(12.dp)).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = Maeve.text2)
        }
        StatusPill(if (enabled) "On" else "Off", if (enabled) PillKind.Online else PillKind.Neutral)
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = enabled, onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedTrackColor = Maeve.elevated2,
                uncheckedBorderColor = Maeve.border,
            ),
        )
    }
}
