package gg.snell.mod.editor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GeometryTest {
    @Test fun `rect contains is half-open`() {
        val r = Rect(10, 10, 20, 20)
        assertTrue(r.contains(10, 10))
        assertTrue(r.contains(29, 29))
        assertFalse(r.contains(30, 20))
        assertFalse(r.contains(20, 30))
        assertFalse(r.contains(9, 9))
    }

    @Test fun `hit test returns topmost on overlap`() {
        val boxes = listOf(
            ElementBox("under", Rect(0, 0, 50, 50)),
            ElementBox("over", Rect(20, 20, 50, 50)),
        )
        assertEquals("over", hitTest(boxes, 25, 25))
        assertEquals("under", hitTest(boxes, 5, 5))
        assertNull(hitTest(boxes, 200, 200))
        assertNull(hitTest(emptyList(), 5, 5))
    }
}
