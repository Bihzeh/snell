# Third-party licenses & redistribution status

Every third-party dependency we ship or build against, its license, and whether/how
we may redistribute it. Re-verify on every version bump (ADR-0009).

## Bundled with the client (placed into the game profile by the launcher)

These are shipped as **separate, unmodified jars (mere aggregation)**. We do not
modify or statically link them. Their full license texts must be vendored next to
the bundled jars at packaging time.

| Mod | Version | License | Redistribution | Source |
|---|---|---|---|---|
| Sodium | 0.9.0 | LGPL-3.0 | OK as unmodified separate jar; ship license + offer source | https://modrinth.com/mod/sodium |
| Lithium | 0.25.0 | LGPL-3.0-only | OK as unmodified separate jar; ship license + offer source | https://modrinth.com/mod/lithium |

> CONFIRM Sodium's exact current license text and vendor it here before any public
> release. Add further perf mods (FerriteCore, ImmediatelyFast, EntityCulling, ...)
> only after confirming a 26.1 build exists AND the license permits bundling.

## Build/runtime dependencies (not redistributed as our own code)

| Dependency | Version | License | Notes |
|---|---|---|---|
| Fabric Loader | 0.19.3 | Apache-2.0 | Loader |
| Fabric API | 0.152.2+26.2 | Apache-2.0 | Required by the mod |
| Fabric Language Kotlin | (latest) | Apache-2.0 | Kotlin language adapter |
| Kotlin stdlib | 2.4.0 | Apache-2.0 | |
| kotlinx.serialization | 1.7.x | Apache-2.0 | |
| Compose Multiplatform | 1.11.1 | Apache-2.0 | Launcher UI |
| Ktor | 3.5.0 | Apache-2.0 | Launcher client + backend |
| Logback | 1.5.x | EPL-1.0 / LGPL-2.1 | Backend logging |

## Minecraft itself

Never redistributed. The launcher downloads official game files from Mojang
endpoints; auth uses the official Microsoft OAuth flow (ADR-0006). Mojang mappings
are used under Mojang's license — review its terms before distribution.

## Snell's own code

Proprietary — see top-level `LICENSE` (decision: ADR-0009).

## Bundled fonts (launcher UI)

Shipped in `launcher/src/main/resources/fonts/`. The "Obsidian" redesign (Jun 2026) moved the
launcher off Poppins onto Outfit + Hanken Grotesk; Poppins now ships only with the in-game HUD
(the `:mod` resource pack). Fonts are pre-instanced static weights; the Material Symbols icon
font is subset to the ~60 glyphs the UI uses.

| Font | Role | License | Source |
|---|---|---|---|
| Geist | Launcher UI (all text; Regular/Medium/SemiBold/Bold) | OFL-1.1 | https://github.com/google/fonts/tree/main/ofl/geist |
| Geist Mono | Device codes / numerics | OFL-1.1 | https://github.com/google/fonts/tree/main/ofl/geistmono |
| Monocraft | In-launcher Minecraft-style nametag (pixel) | OFL-1.1 | https://github.com/IdreesInc/Monocraft |
| Material Symbols Outlined (subset) | Launcher icons (wght 400 / opsz 24 / FILL 0) | Apache-2.0 | https://github.com/google/material-design-icons |
| Poppins | In-game HUD font pack only (`:mod`, weighted) | OFL-1.1 | https://github.com/google/fonts/tree/main/ofl/poppins |

OFL-1.1 texts are vendored alongside the fonts (`*-OFL.txt`); the Material Symbols Apache-2.0
text is vendored as `MaterialSymbols-Apache2.0.txt`.
