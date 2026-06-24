package gg.maeve.mod.cosmetics

import gg.maeve.shared.EquippedCosmetics

/**
 * Resolves cosmetics for players by UUID. See docs/adr/ADR-0007: resolution is
 * out-of-band via the Maeve backend, never via in-game packets. Players unknown
 * to the backend return null -> they render vanilla.
 *
 * Phase 1: LocalStubCosmeticsClient (no network). Phase 3: HttpCosmeticsClient
 * backed by GET /v1/cosmetics with a short-TTL cache, plus a WebSocket push in
 * Phase 4 for live equip changes.
 */
interface CosmeticsClient {
    fun forPlayer(uuid: String): EquippedCosmetics?
}

/** Phase 1 placeholder: returns nothing, so everyone renders vanilla. */
class LocalStubCosmeticsClient : CosmeticsClient {
    override fun forPlayer(uuid: String): EquippedCosmetics? = null
}
