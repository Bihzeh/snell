package gg.snell.launcher.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font

/**
 * Snell launcher type families ("Snell" design system):
 *  - [Display] = [Body] = **Geist** — one geometric sans for all UI (weight-differentiated).
 *  - [Mono]    = **Geist Mono** — codes / numeric readouts.
 *  - [Pixel]   = **Monocraft** — the in-game-style nametag.
 *
 * All bundled as pre-instanced static weights under `resources/fonts/` (Geist OFL). The in-game
 * HUD font (Poppins) lives in the `:mod` module and is unaffected by this launcher theme.
 */
private val Geist = FontFamily(
    Font("fonts/Geist-Regular.ttf", FontWeight.Normal),
    Font("fonts/Geist-Medium.ttf", FontWeight.Medium),
    Font("fonts/Geist-SemiBold.ttf", FontWeight.SemiBold),
    Font("fonts/Geist-Bold.ttf", FontWeight.Bold),
)

object SnellFonts {
    val Display: FontFamily = Geist
    val Body: FontFamily = Geist
    val Mono: FontFamily = FontFamily(
        Font("fonts/GeistMono.ttf", FontWeight.Normal),
        Font("fonts/GeistMono.ttf", FontWeight.Medium),
    )
    val Pixel: FontFamily = FontFamily(Font("fonts/Monocraft.ttf"))
}
