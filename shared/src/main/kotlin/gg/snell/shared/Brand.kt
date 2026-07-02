package gg.snell.shared

/**
 * Outward-facing Snell links, in one place so the mod / launcher / backend never drift.
 * Cosmetics are equipped on the web (ADR-0007: out-of-band via the backend, no in-game UI),
 * so the in-game Cosmetics button just opens this page.
 */
object Brand {
    // TODO: replace both placeholders with the real invite / page before release.
    const val DISCORD_URL = "https://discord.gg/snell"
    const val COSMETICS_URL = "https://snell.gg/cosmetics"
}
