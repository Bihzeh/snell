package gg.snell.launcher.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Material Symbols Outlined glyphs, rendered from a **subsetted static** font bundled at
 * `resources/fonts/MaterialSymbolsOutlined-subset.ttf` (wght 400 / opsz 24 / FILL 0 / GRAD 0 —
 * the design's default instance). Only the glyphs the launcher actually uses are kept (~10 KB),
 * far leaner than `material-icons-extended`. Skiko rasterizes these in the headless preview too.
 *
 * Codepoints generated from Google's official `MaterialSymbolsOutlined[...].codepoints`.
 */
object SnellIcons {
    val fontFamily: FontFamily = FontFamily(
        Font("fonts/MaterialSymbolsOutlined-subset.ttf"),
    )

    /** Glyph name -> codepoint. Keep in sync with the subset built in build-prep. */
    val codepoints: Map<String, Char> = mapOf(
        "change_history" to '\uE86B',
        "stadia_controller" to '\uF135',
        "sports_esports" to '\uEA28',
        "extension" to '\uE87B',
        "auto_awesome" to '\uE65F',
        "group" to '\uEA21',
        "settings" to '\uE8B8',
        "remove" to '\uE15B',
        "crop_square" to '\uE3C6',
        "close" to '\uE5CD',
        "fullscreen" to '\uE5D0',
        "fullscreen_exit" to '\uE5D1',
        "minimize" to '\uE931',
        "bolt" to '\uEA0B',
        "verified" to '\uEF76',
        "expand_more" to '\uE5CF',
        "expand_less" to '\uE5CE',
        "chevron_right" to '\uE5CC',
        "chevron_left" to '\uE5CB',
        "folder_open" to '\uE2C8',
        "folder" to '\uE2C7',
        "search" to '\uEF7A',
        "person_add" to '\uEA4D',
        "chat_bubble" to '\uE0CB',
        "forum" to '\uE8AF',
        "send" to '\uE163',
        "check_circle" to '\uF0BE',
        "check" to '\uE668',
        "wifi_off" to '\uE648',
        "workspace_premium" to '\uE7AF',
        "timer_off" to '\uE426',
        "error" to '\uF8B6',
        "warning" to '\uF083',
        "info" to '\uE88E',
        "rotate_right" to '\uE41A',
        "lock" to '\uE899',
        "diversity_3" to '\uF8D9',
        "view_in_ar" to '\uEFC9',
        "speed" to '\uE9E4',
        "dashboard" to '\uE871',
        "memory" to '\uE322',
        "visibility" to '\uE8F4',
        "visibility_off" to '\uE8F5',
        "edit" to '\uF097',
        "tune" to '\uE429',
        "add" to '\uE145',
        "code" to '\uE86F',
        "group_off" to '\uE747',
        "content_copy" to '\uE14D',
        "open_in_new" to '\uE89E',
        "refresh" to '\uE5D5',
        "download" to '\uF090',
        "play_arrow" to '\uE037',
        "rocket_launch" to '\uEB9B',
        "logout" to '\uE9BA',
        "person" to '\uF0D3',
        "notifications" to '\uE7F5',
        "more_horiz" to '\uE5D3',
        "arrow_forward" to '\uE5C8',
        "arrow_back" to '\uE5C4',
        "sync" to '\uE627',
        "terminal" to '\uEB8E',
        "tag" to '\uE9EF',    )
}

/**
 * Render a Material Symbols glyph by [name]. Unknown names render as empty space of [size] (so
 * layout is preserved) — [SnellIcons.codepoints] is the source of truth and is unit-tested.
 */
@Composable
fun SymIcon(
    name: String,
    size: Dp = 20.dp,
    tint: Color = LocalContentColor.current,
) {
    val glyph = SnellIcons.codepoints[name]
    if (glyph == null) {
        Box(Modifier.size(size))
        return
    }
    val fontSize = with(LocalDensity.current) { size.toSp() }
    Text(text = glyph.toString(), fontFamily = SnellIcons.fontFamily, fontSize = fontSize, color = tint)
}
