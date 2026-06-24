# ADR-0002: Target Minecraft 26.1 + Mojang mappings

Status: Accepted

## Context
Verified June 2026: Minecraft moved to year-based versioning; `1.21.x` is now the
LEGACY line and `26.x` is current (26.1.2 "Chaos Cubed"). 26.1 was a clean break:
jars ship UNOBFUSCATED, Fabric officially DROPPED Yarn in favor of Mojang official
mappings, and no pre-26.1 mod works without recompilation. JDK 25 is required.

## Decision
Target **Minecraft 26.1** for the MVP with **Mojang official mappings**. Isolate
version-sensitive code behind a thin platform/bridge layer so a later 1.21.x
backport or 26.2 forward-port is additive.

## Consequences
- Single, now-standard mapping target; simpler no-remap Loom build.
- A new project avoids starting life on the just-deprecated Yarn/remap toolchain.
- Costs: JDK 25, Loom 1.15, Gradle 9.4.0, IntelliJ 2025.3+.
- 26.2 adds a Vulkan backend and removes OpenGL -> all rendering MUST use Blaze3D.
- Rejected: targeting legacy 1.21.x (mature but dying toolchain; migration debt).
