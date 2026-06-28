package gg.snell.mod.module.hud

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

private const val SEC = 1_000_000_000L

class ClickTrackerTest {
    @BeforeTest fun clear() = ClickTracker.reset()

    @Test fun `counts clicks within the last second`() {
        ClickTracker.onLeft(SEC)
        ClickTracker.onLeft(SEC + SEC / 10)
        ClickTracker.onLeft(SEC + SEC / 5)
        assertEquals(3, ClickTracker.leftCps(SEC + SEC / 3))
    }

    @Test fun `drops clicks older than one second`() {
        ClickTracker.onLeft(0)
        ClickTracker.onLeft(SEC / 2)
        assertEquals(0, ClickTracker.leftCps(SEC + 6 * SEC / 10)) // both now >1s old
        ClickTracker.onLeft(SEC + 6 * SEC / 10)
        assertEquals(1, ClickTracker.leftCps(SEC + 65 * SEC / 100))
    }

    @Test fun `left and right counters are independent`() {
        ClickTracker.onLeft(0)
        ClickTracker.onRight(0)
        ClickTracker.onRight(SEC / 10)
        assertEquals(1, ClickTracker.leftCps(SEC / 5))
        assertEquals(2, ClickTracker.rightCps(SEC / 5))
    }
}
