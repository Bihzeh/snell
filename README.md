# Maeve

A legitimate, **server-legal** performance + quality-of-life Minecraft client — FPS boost, bundled QoL mods, an in-game HUD/mod menu, cross-user cosmetics, friends/parties, and a custom launcher with Microsoft auth and multi-version support.

> Not a cheat client. No reach/killaura/autoclicker/X-ray/hitbox/anti-knockback. Every module is held to a published allowed-mods policy (Hypixel's "Allowed Modifications" as the reference bar) so users stay welcome on big servers.

## Status

Phase 0 — foundation scaffold. See [`docs/superpowers/specs/`](docs/superpowers/specs/) for the architecture spec and [`docs/adr/`](docs/adr/) for decision records.

## Stack (verified June 2026)

| Component | Choice |
|---|---|
| Client | Fabric mod + Mixins, **Minecraft 26.1**, **Mojang mappings**, **JDK 25** |
| Launcher | Kotlin **Compose Multiplatform** (Windows-first) |
| Backend | Kotlin **Ktor** (Phase 3) |
| Build | Gradle 9.4.0 multi-project, version catalog, convention plugins |

All third-party dependencies and their licenses are tracked in [`LICENSES/`](LICENSES/).

## Modules

- `mod/` — Fabric mod: HUD module system, in-game mod menu, config, cosmetics rendering.
- `launcher/` — Compose Multiplatform desktop launcher: Microsoft auth, game provisioning, mod injection.
- `backend/` — Ktor service: identity, cosmetics ownership + sync, friends/parties (Phase 3+).
- `shared/` — shared Kotlin DTOs (cosmetics protocol, version constants).
- `cosmetics/` — cosmetic asset format + samples.

## Building

Requires **JDK 25** and Gradle 9.4.0 (use the wrapper once generated: `gradle wrapper --gradle-version 9.4.0`).

```bash
./gradlew build          # build all modules
./gradlew :mod:build     # build the Fabric mod jar
./gradlew :launcher:run  # run the launcher
```

## License

Maeve's own code: see [`LICENSE`](LICENSE). Bundled third-party mods retain their own licenses — see [`LICENSES/`](LICENSES/).
