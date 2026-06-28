package gg.snell.mod.module.hud

/** Pure formatting helpers for HUD modules (no Minecraft types) so they stay unit-testable. */
object HudFormat {
    /**
     * Minecraft yaw (0 = south/+Z, 90 = west/-X, 180 = north/-Z, 270 = east/+X) -> a compass
     * label with the facing axis for the four cardinal directions.
     */
    fun cardinal(yaw: Float): String {
        val y = ((yaw % 360f) + 360f) % 360f
        return when {
            y < 22.5f || y >= 337.5f -> "S (+Z)"
            y < 67.5f -> "SW"
            y < 112.5f -> "W (-X)"
            y < 157.5f -> "NW"
            y < 202.5f -> "N (-Z)"
            y < 247.5f -> "NE"
            y < 292.5f -> "E (+X)"
            else -> "SE"
        }
    }

    /** World day number (dayTime ticks / 24000). */
    fun day(dayTime: Long): Long = Math.floorDiv(dayTime, 24000L)

    /** In-game wall clock as HH:MM (dayTime 0 = 06:00 sunrise; 24000 ticks = 24h). */
    fun clock(dayTime: Long): String {
        val minutesOfDay = (Math.floorMod(dayTime + 6000L, 24000L) * 1440L / 24000L).toInt()
        return "%02d:%02d".format(minutesOfDay / 60, minutesOfDay % 60)
    }

    fun speed(blocksPerSecond: Double): String = "%.2f b/s".format(blocksPerSecond)

    /**
     * A horizontal direction tape centred on the player's facing. MC yaw -> facing bearing
     * (N=0, E=90, S=180, W=270); the 8 compass points are placed by their angular offset within
     * +/-[halfSpan] degrees. Returns two equal-width lines: the tape and a centred caret marking
     * the current heading.
     */
    fun compass(yaw: Float, width: Int = 41, halfSpan: Float = 100f): List<String> {
        val bearing = ((yaw + 180f) % 360f + 360f) % 360f
        val center = width / 2
        val row = CharArray(width) { '\u2500' } // continuous box-draw line (full-width -> low jitter)
        val points = listOf(0 to "N", 45 to "ne", 90 to "E", 135 to "se", 180 to "S", 225 to "sw", 270 to "W", 315 to "nw") // cardinals upper, inter lower (smaller)
        for ((deg, label) in points) {
            val delta = angleDiff(deg.toFloat(), bearing)
            if (kotlin.math.abs(delta) > halfSpan) continue
            val col = center + Math.round(delta / halfSpan * center)
            val start = col - (label.length - 1) / 2
            for (j in label.indices) (start + j).let { if (it in 0 until width) row[it] = label[j] }
        }
        val caret = CharArray(width) { ' ' }.also { it[center] = '^' }.concatToString()
        return listOf(String(row), caret)
    }

    /** Shortest signed difference a-b in (-180, 180]. */
    private fun angleDiff(a: Float, b: Float): Float = (a - b + 540f) % 360f - 180f
}
