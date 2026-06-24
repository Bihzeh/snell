# ADR-0001: Mod loader — Fabric

Status: Accepted

## Context
Two paths: (A) build on a mod loader; (B) ship a from-scratch deobfuscated client
(decompile + modify Minecraft directly, like early Lunar). Path B means
redistributing modified Mojang code (violates project constraint #2 and the EULA),
a per-version maintenance sinkhole, and reinventing rendering that Sodium already
does better. Among loaders: Fabric (light, client/perf-mod focused), NeoForge
(content-mod focused, heavier), Quilt (Fabric fork).

## Decision
Path A on **Fabric** + Mixins. We never redistribute Mojang code; we inject at
runtime and bundle existing performance mods (Sodium/Lithium).

## Consequences
- Fast path to MVP; large ecosystem; legally clean.
- Tied to Fabric's release cadence and Mixin for injection.
- Rejected: NeoForge (heavier, content-oriented), Quilt (smaller ecosystem), Path B.
