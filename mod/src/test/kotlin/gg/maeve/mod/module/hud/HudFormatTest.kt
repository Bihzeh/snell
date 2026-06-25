package gg.maeve.mod.module.hud

import kotlin.test.Test
import kotlin.test.assertEquals

class HudFormatTest {
    @Test fun `cardinal maps yaw to compass + axis`() {
        assertEquals("S (+Z)", HudFormat.cardinal(0f))
        assertEquals("W (-X)", HudFormat.cardinal(90f))
        assertEquals("N (-Z)", HudFormat.cardinal(180f))
        assertEquals("E (+X)", HudFormat.cardinal(-90f))
        assertEquals("SW", HudFormat.cardinal(45f))
        assertEquals("S (+Z)", HudFormat.cardinal(360f))
    }

    @Test fun `day counts 24000-tick days`() {
        assertEquals(0L, HudFormat.day(0L))
        assertEquals(0L, HudFormat.day(23999L))
        assertEquals(1L, HudFormat.day(24000L))
        assertEquals(2L, HudFormat.day(49000L))
    }

    @Test fun `clock anchors dayTime 0 to sunrise`() {
        assertEquals("06:00", HudFormat.clock(0L))
        assertEquals("12:00", HudFormat.clock(6000L))
        assertEquals("00:00", HudFormat.clock(18000L))
        assertEquals("06:00", HudFormat.clock(24000L))
    }

    @Test fun `speed formats two decimals`() {
        assertEquals("3.46 b/s", HudFormat.speed(3.456))
        assertEquals("0.00 b/s", HudFormat.speed(0.0))
    }
}
