package gg.snell.shared

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Locks the cosmetics wire format: the mod (consumer) and backend (producer) must
 * agree on it, so a round-trip test guards against accidental drift.
 */
class CosmeticsSerializationTest {

    private val json = Json { prettyPrint = false }

    @Test
    fun `equipped cosmetics round-trips through json`() {
        val original = EquippedCosmetics(
            uuid = "00000000-0000-0000-0000-000000000001",
            equipped = listOf(
                Cosmetic("aurora_cloak", CosmeticType.CLOAK, "Aurora Cloak", "cloaks/aurora.json", listOf("cloaks/aurora.png")),
                Cosmetic("halo", CosmeticType.HAT, "Halo", "hats/halo.json", listOf("hats/halo.png")),
            ),
        )

        val decoded = json.decodeFromString<EquippedCosmetics>(json.encodeToString(original))

        assertEquals(original, decoded)
    }

    @Test
    fun `empty lookup response is valid`() {
        val resp = CosmeticsLookupResponse(players = emptyList())
        val decoded = json.decodeFromString<CosmeticsLookupResponse>(json.encodeToString(resp))
        assertEquals(0, decoded.players.size)
    }
}
