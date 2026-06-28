package gg.snell.mod.editor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SliderMathTest {
    @Test fun `value to coord maps ends and middle`() {
        assertEquals(0, SliderMath.valueToCoord(0.5f, 0.5f, 3.0f, 100))
        assertEquals(100, SliderMath.valueToCoord(3.0f, 0.5f, 3.0f, 100))
        assertEquals(50, SliderMath.valueToCoord(1.75f, 0.5f, 3.0f, 100))
    }

    @Test fun `value to coord clamps out of range`() {
        assertEquals(0, SliderMath.valueToCoord(-1f, 0.5f, 3.0f, 100))
        assertEquals(100, SliderMath.valueToCoord(9f, 0.5f, 3.0f, 100))
    }

    @Test fun `coord to value is the inverse`() {
        assertEquals(0.5f, SliderMath.coordToValue(0, 100, 0.5f, 3.0f))
        assertEquals(3.0f, SliderMath.coordToValue(100, 100, 0.5f, 3.0f))
        assertTrue(kotlin.math.abs(1.75f - SliderMath.coordToValue(50, 100, 0.5f, 3.0f)) < 0.001f)
    }

    @Test fun `inverted coord puts max at the top`() {
        assertEquals(3.0f, SliderMath.coordToValueInverted(0, 100, 0.5f, 3.0f))
        assertEquals(0.5f, SliderMath.coordToValueInverted(100, 100, 0.5f, 3.0f))
    }
}
