# Third-party licenses & redistribution status

Every third-party dependency we ship or build against, its license, and whether/how
we may redistribute it. Re-verify on every version bump (ADR-0009).

## Bundled with the client (placed into the game profile by the launcher)

These are shipped as **separate, unmodified jars (mere aggregation)**. We do not
modify or statically link them. Their full license texts must be vendored next to
the bundled jars at packaging time.

| Mod | Version | License | Redistribution | Source |
|---|---|---|---|---|
| Sodium | 0.8.7 | LGPL-3.0 | OK as unmodified separate jar; ship license + offer source | https://modrinth.com/mod/sodium |
| Lithium | 0.22.1 | LGPL-3.0-only | OK as unmodified separate jar; ship license + offer source | https://modrinth.com/mod/lithium |

> CONFIRM Sodium's exact current license text and vendor it here before any public
> release. Add further perf mods (FerriteCore, ImmediatelyFast, EntityCulling, ...)
> only after confirming a 26.1 build exists AND the license permits bundling.

## Build/runtime dependencies (not redistributed as our own code)

| Dependency | Version | License | Notes |
|---|---|---|---|
| Fabric Loader | 0.18.4 | Apache-2.0 | Loader |
| Fabric API | (26.1 build) | Apache-2.0 | Required by the mod |
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

## Maeve's own code

Proprietary — see top-level `LICENSE` (decision: ADR-0009).
