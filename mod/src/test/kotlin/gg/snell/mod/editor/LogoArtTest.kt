package gg.snell.mod.editor

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.math.abs

class LogoArtTest {
    private val area = PositionLayout.logoRect(520, 360)

    @Test fun `mark is non-empty`() {
        assertTrue(LogoArt.bands(area).isNotEmpty())
    }

    @Test fun `every band stays inside the logo area`() {
        for (b in LogoArt.bands(area)) {
            assertTrue(b.rect.left >= area.left && b.rect.right <= area.right, "x out of bounds: ${b.rect}")
            assertTrue(b.rect.top >= area.top && b.rect.bottom <= area.bottom, "y out of bounds: ${b.rect}")
        }
    }

    @Test fun `silhouette is horizontally symmetric about the centre`() {
        val cx = area.left + area.width / 2
        val bands = LogoArt.bands(area)
        val minL = bands.minOf { it.rect.left }
        val maxR = bands.maxOf { it.rect.right }
        assertTrue(abs((cx - minL) - (maxR - cx)) <= 1, "left=${cx - minL} right=${maxR - cx}")
    }
}
