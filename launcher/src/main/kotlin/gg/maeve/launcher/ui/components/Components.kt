package gg.maeve.launcher.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import gg.maeve.launcher.ui.theme.Maeve
import gg.maeve.launcher.ui.theme.PillShape

/** Outfit, uppercase, letter-spaced muted section label (frames 05/06/09). */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium,
        color = Maeve.text3,
        letterSpacing = androidx.compose.ui.unit.TextUnit(0.10f, androidx.compose.ui.unit.TextUnitType.Em),
    )
}

enum class ButtonVariant { Primary, Secondary, Tertiary }

@Composable
fun MaeveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Primary,
    enabled: Boolean = true,
    leadingIcon: String? = null,
    fillWidth: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val shape = RoundedCornerShape(10.dp)
    val bg = when {
        !enabled -> if (variant == ButtonVariant.Primary) Maeve.accent.copy(alpha = 0.35f) else Color.Transparent
        variant == ButtonVariant.Primary -> if (hovered) Maeve.accentHi else Maeve.accent
        variant == ButtonVariant.Secondary -> if (hovered) Maeve.border else Maeve.s2
        else -> if (hovered) Maeve.accentSubtle else Color.Transparent
    }
    val fg = when {
        !enabled -> Maeve.textDisabled
        variant == ButtonVariant.Primary -> Color.White
        variant == ButtonVariant.Tertiary -> Maeve.accentHi
        else -> Color(0xFFECECEC)
    }
    var m = modifier.then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
    if (variant == ButtonVariant.Primary && enabled) {
        m = m.shadow(10.dp, shape, ambientColor = Maeve.accent, spotColor = Maeve.accent)
    }
    m = m.clip(shape).background(bg)
    if (variant == ButtonVariant.Secondary) m = m.border(1.dp, Maeve.border, shape)
    m = m.clickable(enabled = enabled, interactionSource = interaction, indication = null, onClick = onClick)
        .heightIn(min = 44.dp).padding(horizontal = 18.dp, vertical = 12.dp)
    Row(m, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        leadingIcon?.let { SymIcon(it, 18.dp, fg) }
        Text(text, color = fg, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

/** Square icon button — window controls, small actions. [danger] turns the hover red (close). */
@Composable
fun MaeveIconButton(
    icon: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    boxSize: androidx.compose.ui.unit.Dp = 32.dp,
    iconSize: androidx.compose.ui.unit.Dp = 18.dp,
    tint: Color = Maeve.text2,
    danger: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val hoverBg = if (danger) Maeve.danger.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.06f)
    Box(
        modifier.size(boxSize).clip(RoundedCornerShape(7.dp))
            .background(if (hovered) hoverBg else Color.Transparent)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { SymIcon(icon, iconSize, if (hovered && danger) Maeve.danger else tint) }
}

/** The big primary Play CTA — gradient fill, accent glow, play glyph (frame 03). */
@Composable
fun PlayButton(enabled: Boolean, label: String = "Launch", onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val shape = RoundedCornerShape(14.dp)
    val brush = if (enabled) {
        Brush.linearGradient(listOf(Maeve.accentHi, if (hovered) Maeve.accentHi else Maeve.accent))
    } else {
        Brush.linearGradient(listOf(Maeve.accent.copy(alpha = 0.35f), Maeve.accent.copy(alpha = 0.35f)))
    }
    Row(
        modifier
            .then(if (enabled) Modifier.shadow(18.dp, shape, ambientColor = Maeve.accent, spotColor = Maeve.accent) else Modifier)
            .clip(shape).background(brush)
            .clickable(enabled = enabled, interactionSource = interaction, indication = null, onClick = onClick)
            .heightIn(min = 64.dp).padding(horizontal = 40.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SymIcon("play_arrow", 26.dp, Color.White)
        Text(label, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    }
}

enum class PillKind { UpToDate, Downloading, UpdateAvailable, Online, Offline, Failed, Founder, Neutral }

@Composable
fun StatusPill(label: String, kind: PillKind, modifier: Modifier = Modifier, showDot: Boolean = true) {
    val color = when (kind) {
        PillKind.UpToDate, PillKind.Online -> Maeve.success
        PillKind.Downloading -> Maeve.info
        PillKind.UpdateAvailable, PillKind.Founder -> Maeve.ember
        PillKind.Offline, PillKind.Failed -> Maeve.danger
        PillKind.Neutral -> Maeve.text3
    }
    Row(
        modifier.clip(PillShape).background(color.copy(alpha = 0.12f)).padding(horizontal = 11.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showDot) Box(Modifier.size(7.dp).clip(CircleShape).background(color))
        Text(label, color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun MaeveCard(modifier: Modifier = Modifier, padding: PaddingValues = PaddingValues(20.dp), content: @Composable () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(14.dp)).background(Maeve.s1)
            .border(1.dp, Maeve.border, RoundedCornerShape(14.dp)).padding(padding),
    ) { content() }
}

/** Custom 46x26 pill switch matching the design (M3 Switch metrics differ). */
@Composable
fun MaeveSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val track by animateColorAsState(if (checked) Maeve.accent else Maeve.border)
    val knobOffset by animateDpAsState(if (checked) 23.dp else 3.dp)
    val knob = if (checked) Color.White else Maeve.text3
    Box(
        modifier.size(46.dp, 26.dp).clip(PillShape).background(track)
            .clickable(enabled = enabled, interactionSource = remember { MutableInteractionSource() }, indication = null) {
                onCheckedChange(!checked)
            },
    ) {
        Box(Modifier.offset(x = knobOffset, y = 3.dp).size(20.dp).clip(CircleShape).background(knob))
    }
}

@Composable
fun MaeveProgress(fraction: Float?, modifier: Modifier = Modifier) {
    val mod = modifier.fillMaxWidth().height(6.dp).clip(CircleShape)
    if (fraction != null) {
        LinearProgressIndicator(progress = { fraction.coerceIn(0f, 1f) }, modifier = mod, color = Maeve.accent, trackColor = Maeve.s2)
    } else {
        LinearProgressIndicator(modifier = mod, color = Maeve.accent, trackColor = Maeve.s2)
    }
}

@Composable
fun Spinner(sizeDp: Int = 18, modifier: Modifier = Modifier) {
    CircularProgressIndicator(modifier = modifier.size(sizeDp.dp), color = Maeve.accent, strokeWidth = 2.dp)
}

/** Display-only select control (version/profile/Java) — styled to match, not interactive yet. */
@Composable
fun MaeveSelectDisplay(text: String, modifier: Modifier = Modifier, leadingIcon: String? = null) {
    Row(
        modifier.clip(RoundedCornerShape(9.dp)).background(Maeve.s2).border(1.dp, Maeve.border, RoundedCornerShape(9.dp))
            .padding(horizontal = 14.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leadingIcon?.let { SymIcon(it, 18.dp, Maeve.text2) }
        Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(2.dp))
        SymIcon("expand_more", 18.dp, Maeve.text3)
    }
}

/** A mod/feature row (frame 06): colored glyph tile + name/meta + description + status + switch. */
@Composable
fun ModRow(
    icon: String,
    name: String,
    meta: String,
    description: String,
    statusLabel: String,
    statusKind: PillKind,
    enabled: Boolean,
    onToggle: ((Boolean) -> Unit)?,
    logo: ImageBitmap? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Maeve.s1)
            .border(1.dp, Maeve.border, RoundedCornerShape(12.dp)).padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(if (logo == null) Maeve.accent.copy(alpha = 0.14f) else Maeve.s2),
            contentAlignment = Alignment.Center,
        ) {
            if (logo != null) Image(bitmap = logo, contentDescription = name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else SymIcon(icon, 24.dp, Maeve.accent)
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(meta, style = MaterialTheme.typography.labelSmall, color = Maeve.text3)
            }
            Text(description, style = MaterialTheme.typography.bodySmall, color = Maeve.text2, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        StatusPill(statusLabel, statusKind)
        if (onToggle != null) {
            Spacer(Modifier.width(4.dp))
            MaeveSwitch(enabled, onToggle)
        }
    }
}

/** Dashed-border empty/coming-soon block (frame 09). */
@Composable
fun EmptyState(icon: String, title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SymIcon(icon, 34.dp, Maeve.text3)
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Maeve.text3)
    }
}

/** Placeholder pixel-skin avatar (no real skin pipeline yet) — diagonal weave + status dot. */
@Composable
fun SkinAvatar(modifier: Modifier = Modifier, sizeDp: androidx.compose.ui.unit.Dp = 44.dp, online: Boolean = true) {
    Box(modifier) {
        Box(
            Modifier.size(sizeDp).clip(RoundedCornerShape(10.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF3A3A3A), Color(0xFF2A2A2A)))),
        )
        Box(
            Modifier.align(Alignment.BottomEnd).offset(x = 2.dp, y = 2.dp).size(12.dp).clip(CircleShape)
                .background(if (online) Maeve.accent else Maeve.text3),
        )
    }
}
