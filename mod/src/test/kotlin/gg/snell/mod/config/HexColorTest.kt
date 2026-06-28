package gg.snell.mod.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HexColorTest {
    @Test fun `encode produces hash AARRGGBB`() {
        assertEquals("#FFECEAF5", HexColor.encode(0xFFECEAF5.toInt()))
        assertEquals("#80112233", HexColor.encode(0x80112233.toInt()))
    }

    @Test fun `decode 8-digit argb`() {
        assertEquals(0xFFECEAF5.toInt(), HexColor.decode("#FFECEAF5"))
        assertEquals(0x80112233.toInt(), HexColor.decode("#80112233"))
    }

    @Test fun `decode 6-digit assumes full alpha`() {
        assertEquals(0xFFECEAF5.toInt(), HexColor.decode("#ECEAF5"))
    }

    @Test fun `decode tolerates missing hash`() {
        assertEquals(0xFFECEAF5.toInt(), HexColor.decode("ECEAF5"))
    }

    @Test fun `decode rejects malformed input`() {
        assertNull(HexColor.decode("nope"))
        assertNull(HexColor.decode("#12345"))
        assertNull(HexColor.decode("#GGGGGG"))
        assertNull(HexColor.decode(""))
    }

    @Test fun `round trip`() {
        val c = 0xCC8B6DFF.toInt()
        assertEquals(c, HexColor.decode(HexColor.encode(c)))
    }
}
