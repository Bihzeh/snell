package gg.maeve.launcher.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font

/**
 * Maeve launcher type families ("Obsidian" design system):
 *  - [Display] = **Outfit** (geometric) — wordmark, headings, section labels, numerics.
 *  - [Body]    = **Hanken Grotesk** — body copy and buttons.
 *  - [Mono]    = **Geist Mono** — device codes / numeric readouts.
 *
 * All three are bundled as pre-instanced static weights under `resources/fonts/` (variable
 * sources are instanced at build-prep time; we do NOT rely on runtime `FontVariation`, which is
 * unreliable on Skiko desktop). Outfit/Hanken are OFL. The in-game HUD font (Poppins) lives in
 * the `:mod` module and is unaffected by this launcher retheme.
 */
object MaeveFonts {
    val Display: FontFamily = FontFamily(
        Font("fonts/Outfit-Regular.ttf", FontWeight.Normal),
        Font("fonts/Outfit-Medium.ttf", FontWeight.Medium),
        Font("fonts/Outfit-SemiBold.ttf", FontWeight.SemiBold),
        Font("fonts/Outfit-Bold.ttf", FontWeight.Bold),
    )
    val Body: FontFamily = FontFamily(
        Font("fonts/HankenGrotesk-Regular.ttf", FontWeight.Normal),
        Font("fonts/HankenGrotesk-Medium.ttf", FontWeight.Medium),
        Font("fonts/HankenGrotesk-SemiBold.ttf", FontWeight.SemiBold),
        Font("fonts/HankenGrotesk-Bold.ttf", FontWeight.Bold),
        Font("fonts/HankenGrotesk-ExtraBold.ttf", FontWeight.ExtraBold),
    )
    val Mono: FontFamily = FontFamily(
        Font("fonts/GeistMono.ttf", FontWeight.Normal),
    )

    /** Monocraft (OFL) - Minecraft-style pixel font, used for the in-launcher nametag. */
    val Pixel: FontFamily = FontFamily(Font("fonts/Monocraft.ttf"))
}
