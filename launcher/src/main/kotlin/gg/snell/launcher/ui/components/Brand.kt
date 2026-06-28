package gg.snell.launcher.ui.components

/**
 * Classpath paths to the Snell "slipstream" brand logo, the single source of truth for every
 * place the mark is rendered. The full-colour tile is self-contained (dark rounded background +
 * cyan bars), so it drops straight into an icon slot without a wrapper; the cyan-transparent
 * variant has no background and is meant for faint watermarks over the launcher's own surfaces.
 */
object Brand {
    /** Full-colour 512px tile — the OS window / taskbar icon. */
    const val APP_ICON = "brand/snell/snell-slipstream-512.png"

    /** Full-colour 256px tile — inline brand lockups (titlebar, sign-in). */
    const val TILE = "brand/snell/snell-slipstream-256.png"

    /** Cyan bars on a transparent background — faint brand watermark behind other art. */
    const val WATERMARK = "brand/snell/snell-slipstream-cyan-transparent-256.png"
}
