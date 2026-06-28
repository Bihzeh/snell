# Cosmetics asset format

A cosmetic is a **model descriptor** (JSON) plus one or more **textures** (PNG).
The catalog and per-user ownership live in the backend (Phase 3); these files are
the asset payload referenced by `modelKey` / `textureKeys` in the shared DTOs
(`gg.snell.shared.Cosmetic`).

## Model descriptor

```jsonc
{
  "id": "aurora_cloak",
  "type": "CLOAK",          // CLOAK | WINGS | HAT | PET
  "name": "Aurora Cloak",
  "attach": "BODY",         // model attach point / bone
  "texture": "aurora.png",  // relative to this descriptor
  "animation": null          // optional; sway/flap params for cloaks/wings
}
```

## Rules

- Rendering goes through the **Blaze3D** API only (never raw GL): MC 26.2 adds a
  Vulkan backend and removes OpenGL. See `mod/.../cosmetics/CosmeticsRenderer.kt`.
- Textures are power-of-two PNGs. Keep cloak/cape textures to the vanilla cape
  proportions where applicable.
- Every shipped cosmetic must be original art (no copied assets — constraint #3).

See `samples/` for a worked example.
