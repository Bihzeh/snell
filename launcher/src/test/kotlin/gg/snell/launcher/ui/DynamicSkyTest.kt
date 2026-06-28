package gg.snell.launcher.ui

import gg.snell.launcher.ui.components.Body
import gg.snell.launcher.ui.components.skyStateAt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DynamicSkyTest {
    @Test fun noonIsSunHighInSky() {
        val s = skyStateAt(0.5f, 20000L)
        assertEquals(Body.Sun, s.body)
        assertTrue(s.bodyY < 0.3f, "noon sun should be high, bodyY=${s.bodyY}")
        assertTrue(s.nightness < 0.05f, "noon should not be night")
    }

    @Test fun midnightIsMoonAndDark() {
        val s = skyStateAt(0.0f, 20000L)
        assertEquals(Body.Moon, s.body)
        assertTrue(s.nightness > 0.9f, "midnight should be deep night")
    }

    @Test fun sunRisesEastSetsWest() {
        assertTrue(skyStateAt(0.30f, 0L).bodyX < 0.5f, "morning sun on the left")
        assertTrue(skyStateAt(0.74f, 0L).bodyX > 0.5f, "evening sun on the right")
    }

    @Test fun moonPhaseAlwaysInRange() {
        for (d in 0L..20L) assertTrue(skyStateAt(0.0f, d).moonPhase in 0..7)
    }
}
