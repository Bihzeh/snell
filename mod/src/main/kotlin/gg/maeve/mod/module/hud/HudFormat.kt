package gg.maeve.mod.module.hud

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
}
