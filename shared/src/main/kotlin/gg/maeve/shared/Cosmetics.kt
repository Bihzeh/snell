package gg.maeve.shared

import kotlinx.serialization.Serializable

/**
 * Cosmetics protocol DTOs shared by the mod (consumer), launcher, and backend
 * (producer). The wire format lives here so it cannot drift between components.
 *
 * Sync model (see docs/adr/ADR-0007): cosmetics are resolved out-of-band through
 * the Maeve backend by player UUID, never via in-game packets. Players not known
 * to the backend resolve to an empty result and render vanilla.
 */
@Serializable
enum class CosmeticType { CLOAK, WINGS, HAT, PET }

/** A cosmetic the user can own/equip. Assets are referenced, not embedded. */
@Serializable
data class Cosmetic(
    val id: String,
    val type: CosmeticType,
    val name: String,
    /** Relative path/key of the model descriptor in the cosmetics asset store. */
    val modelKey: String,
    /** Relative path/key(s) of the texture(s). */
    val textureKeys: List<String>,
)

/** What a given player currently has equipped. Returned by the backend lookup. */
@Serializable
data class EquippedCosmetics(
    val uuid: String,
    val equipped: List<Cosmetic>,
)

/** Batch lookup request: GET /v1/cosmetics?uuids=a,b,c (modeled as a body for clarity). */
@Serializable
data class CosmeticsLookupRequest(val uuids: List<String>)

/** Batch lookup response. Missing UUIDs are simply absent -> vanilla rendering. */
@Serializable
data class CosmeticsLookupResponse(val players: List<EquippedCosmetics>)
