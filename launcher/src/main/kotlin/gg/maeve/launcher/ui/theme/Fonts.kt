package gg.maeve.launcher.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font

/** Bundled fonts (OFL/MIT). Geist + Geist Mono are variable; weights are requested via
 *  FontWeight (the desktop loader selects/synthesizes from the variable file). */
object MaeveFonts {
    val Geist: FontFamily = FontFamily(
        Font("fonts/Geist.ttf", FontWeight.Light),
        Font("fonts/Geist.ttf", FontWeight.Normal),
        Font("fonts/Geist.ttf", FontWeight.Medium),
        Font("fonts/Geist.ttf", FontWeight.SemiBold),
        Font("fonts/Geist.ttf", FontWeight.Bold),
    )
    val Mono: FontFamily = FontFamily(Font("fonts/GeistMono.ttf", FontWeight.Normal), Font("fonts/GeistMono.ttf", FontWeight.Medium))
    val Marcellus: FontFamily = FontFamily(Font("fonts/Marcellus.ttf", FontWeight.Normal))
}
