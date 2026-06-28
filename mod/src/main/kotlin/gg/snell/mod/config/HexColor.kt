package gg.snell.mod.config

/**
 * Encodes/decodes ARGB Ints as `"#AARRGGBB"` so colors in config.json stay readable and
 * hand-editable (raw ARGB Ints serialize as opaque large negative numbers).
 */
object HexColor {
    fun encode(argb: Int): String = "#%08X".format(argb)

    /**
     * Parses `"#AARRGGBB"` or `"#RRGGBB"` (assumes full alpha); the leading `#` is optional.
     * Returns null on malformed input so the caller can fall back to a default.
     */
    fun decode(text: String): Int? {
        val hex = text.trim().removePrefix("#")
        val normalized = when (hex.length) {
            6 -> "FF$hex"
            8 -> hex
            else -> return null
        }
        return normalized.toLongOrNull(16)?.toInt()
    }
}
