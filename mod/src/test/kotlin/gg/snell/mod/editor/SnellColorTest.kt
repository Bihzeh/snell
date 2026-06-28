package gg.snell.mod.editor

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SnellColorTest {
    @Test fun `hsv primaries`() {
        assertEquals(0xFF0000, SnellColor.hsvToRgb(0f, 1f, 1f))
        assertEquals(0x00FF00, SnellColor.hsvToRgb(120f, 1f, 1f))
        assertEquals(0x0000FF, SnellColor.hsvToRgb(240f, 1f, 1f))
        assertEquals(0xFFFFFF, SnellColor.hsvToRgb(0f, 0f, 1f))
        assertEquals(0x000000, SnellColor.hsvToRgb(0f, 0f, 0f))
    }

    @Test fun `rgb to hsv round-trips`() {
        for (rgb in intArrayOf(0xFF0000, 0x00FF00, 0x0000FF, 0x8B6DFF, 0xE2B45C, 0x123456)) {
            val (h, s, v) = SnellColor.rgbToHsv(rgb)
            assertEquals(rgb, SnellColor.hsvToRgb(h, s, v), "round-trip $rgb")
        }
    }

    @Test fun `value is linear so the SV square column is exact`() {
        val full = SnellColor.hsvToRgb(200f, 0.7f, 1f)
        val half = SnellColor.hsvToRgb(200f, 0.7f, 0.5f)
        for (shift in intArrayOf(16, 8, 0)) {
            val cf = (full shr shift) and 0xFF
            val ch = (half shr shift) and 0xFF
            assertTrue(abs(ch - cf / 2) <= 1, "channel $shift: $ch vs ${cf / 2}")
        }
    }

    @Test fun `argb pack and unpack`() {
        val c = SnellColor.argb(0x80, 0x112233)
        assertEquals(0x80112233.toInt(), c)
        assertEquals(0x80, SnellColor.alphaOf(c))
        assertEquals(0x112233, SnellColor.rgbOf(c))
    }
}
