package gg.maeve.mod.cosmetics

/**
 * Integration point for rendering equipped cosmetics onto the player model.
 *
 * Phase 3 wires a Mixin into the player renderer (registered in maeve.mixins.json)
 * that calls into this renderer. RENDERING MUST GO THROUGH THE BLAZE3D API, never
 * raw GL: Minecraft 26.2 adds a Vulkan backend and removes OpenGL (verified June
 * 2026). Raw-GL cosmetics would break on 26.2.
 *
 * Left intentionally minimal in Phase 0 so the mixin target (a version-sensitive
 * binding) is confirmed against Mojang mappings for the exact MC build at the time
 * Phase 3 starts.
 */
object CosmeticsRenderer {
    fun renderFor(uuid: String, client: CosmeticsClient) {
        val equipped = client.forPlayer(uuid) ?: return
        // TODO(Phase 3): draw equipped.equipped via Blaze3D, attached to model bones.
    }
}
