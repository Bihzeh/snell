# Maeve — Performance + QoL Minecraft Client: Architecture & Roadmap

## Context

Greenfield project (`/home/bihzeh/projects/maeve`, empty, not yet a git repo). Goal: a legitimate, server-legal performance + quality-of-life Minecraft client to compete with Lunar/Feather — FPS boost, bundled QoL mods, in-game HUD/mod menu, cross-user cosmetics, friends/parties, and a custom launcher with Microsoft auth and multi-version support.

This plan reflects a baseline established with the user and **versions verified live (June 2026), not from memory** per the brief's constraint #5. One verified finding changed an early decision: the Minecraft `1.21.x` line is now **legacy**; the current line is the year-based **26.x** series. The user confirmed targeting **26.1** for the MVP.

### Locked decisions

| Area | Decision |
|---|---|
| Client path | **Path A — Fabric + Mixins** (never redistribute Mojang code; inherit Sodium/Lithium) |
| MVP MC version | **26.1** (current; 1.21.x is legacy). Architecture version-abstracted for later backports |
| Mappings | **Mojang official mappings** (Fabric officially dropped Yarn; jars now ship unobfuscated) |
| Java | **JDK 25** (required by 26.1) |
| Audience | General survival / **QoL** (but every module held to the server-legal bar) |
| Resources | **Solo, minimal budget** |
| Launcher OS | **Windows-first** (stack stays cross-platform-capable) |
| Stack | **Kotlin/JVM everywhere** — mod (Java/Kotlin), launcher (Compose Multiplatform), backend (Ktor), one shared module |
| Monetization | **Free now, cosmetics later** (no payment infra in MVP) |
| Brand | **Maeve** |

### Verified tool versions (June 2026 — pin exact patches in `gradle/libs.versions.toml` at scaffold)

| Component | Version | Notes / source |
|---|---|---|
| Minecraft (target) | **26.1.2** ("Chaos Cubed", ~2026-06-16); 26.2 in snapshots | minecraft.net / minecraft.wiki |
| Fabric Loader | **0.18.4** | github.com/FabricMC/fabric-loader/releases |
| Fabric API | 26.1 build (~0.15x) | modrinth.com/mod/fabric-api |
| Fabric Loom | **1.15** — use the no-remap `net.fabricmc.fabric-loom` plugin (jars unobfuscated) | docs.fabricmc.net/develop/porting |
| Gradle | **9.4.0** | gradle.org |
| Fabric Language Kotlin | latest for 26.1 | modrinth.com/mod/fabric-language-kotlin |
| Sodium | **0.8.7** (Fabric/26.1), LGPL-3.0 | modrinth.com/mod/sodium |
| Lithium | **0.22.1** (mc26.1), LGPL-3.0-only | modrinth.com/mod/lithium |
| Compose Multiplatform | **1.11.1** | github.com/JetBrains/compose-multiplatform |
| Kotlin | **2.4.0** | kotlinlang.org/docs/releases |
| Ktor | **3.5.0** (2026-05-15) | ktor.io/docs/releases |

> **26.2/Vulkan watch:** 26.1 is the last OpenGL-only release; 26.2 snapshots add a Vulkan backend and OpenGL is slated for removal. **All custom rendering (cosmetics, HUD) must go through the Blaze3D API, never raw GL calls**, to survive that transition.

---

## Architecture

Four Gradle subprojects under one monorepo, all Kotlin/JVM, sharing one version catalog and a `:shared` DTO module.

```
                    +-------------------------+
                    |  shared (Kotlin)        |  cosmetics protocol DTOs,
                    |  kotlinx.serialization  |  models, version constants
                    +-----------+-------------+
            +-------------------+-------------------+
            v                   v                   v
   +-----------------+ +-----------------+ +-----------------+
   | mod (Fabric)    | | launcher (CMP)  | | backend (Ktor)  |
   | HUD, mod menu,  | | MS auth, JVM dl,| | identity,       |
   | config, cosmetic| | version/profile,| | cosmetics own + |
   | render (mixins) | | mod inject, RPC | | sync, friends   |
   +--------+--------+ +--------+--------+ +--------+--------+
            | HTTPS (cosmetics lookup by UUID)       |
            +--------------------+-------------------+
                                 v
                         Maeve backend (Phase 3+)
```

### Component 1 — Mod (Fabric, 26.1)
- Java 21+/Kotlin via `fabric-language-kotlin`; compiled to **JDK 25**; **Loom 1.15 no-remap** plugin; **Mojang mappings**; Mixins for injection.
- **Module system:** each HUD/feature is a self-contained `Module` (id, enabled flag, config schema, render hook, key bind). A central `ModuleManager` registers them; the mod menu and config layer are generic over `Module` so adding a feature never touches the menu code.
- **MVP modules:** FPS, coordinates, keystrokes. Designed so the Phase-2 set (CPS, armor/potion HUD, scoreboard, toggle-sprint, zoom, custom crosshair, post-hit reach *display*) drops in.
- **HUD render:** Blaze3D draw hooks; draggable HUD editor (Phase 2) operates on `HudElement` positions persisted to config.
- **Config:** per-module JSON on disk (`.minecraft/config/maeve/`), schema-versioned, via kotlinx.serialization.
- **Cosmetics render hook:** mixins into the player renderer; consumes equipped-cosmetic data fetched by the cosmetics client; renders cloak/wings/hat/pet via Blaze3D. Stubbed/local in Phase 1.
- **Version abstraction:** isolate every version-sensitive touch point (mixin targets, mappings-specific names, render entry points) behind a thin `platform`/`compat` layer so a future 1.21.x or 26.2 backport is additive.

### Component 2 — Launcher (Compose Multiplatform 1.11.1 desktop, Windows-first)
- **Microsoft auth (verified flow):** OAuth2 **authorization-code + PKCE** (loopback redirect) with **device-code** as fallback; **consumers** AAD tenant; scope **`XboxLive.signin offline_access`** -> XBL authenticate -> XSTS authorize -> `login_with_xbox` -> ownership/profile. No client secret.
  - **Tokens stay client-side, encrypted in the OS credential store** (Windows Credential Manager via DPAPI; macOS Keychain / libsecret later). Backend never sees Microsoft tokens.
  - **Long-lead blocker:** a newly-registered Azure app must **apply (form) for Minecraft API access** or `api.minecraftservices.com` returns 403. Submit in Phase 0.
- **Game provisioning:** download official files from Mojang endpoints only (never redistribute the jar); download/manage a bundled **Temurin JDK 25**; install Fabric Loader 0.18.4 + Fabric API; place our mod + bundled Sodium/Lithium into an isolated profile; build JVM args; launch.
- **Other:** account switching, version/profile management, settings, Discord Rich Presence, auto-update. Code signing deferred (budget) — see risks.

### Component 3 — Backend (Ktor 3.5.0, Kotlin/JVM) — Phase 3+
- REST + WebSocket. Postgres. Stores **our own identity keyed to MC UUID**, cosmetics catalog, ownership/entitlements, friends, parties. **Never stores Microsoft tokens.**
- Self-hostable (single fat jar / container) to respect the minimal-budget constraint; free-tier-friendly.

### Component 4 — Shared (`:shared`)
- Cosmetics protocol DTOs, model/texture descriptors, version constants — consumed by all three so the wire format can't drift.

---

## Cosmetics sync protocol (backend-mediated, **not** in-game packets)

Big servers (Hypixel) won't relay custom plugin-channel packets, so sync goes **out-of-band through our backend**:

1. **Identity proof:** client proves control of its MC UUID to the Maeve backend (challenge validated against the Mojang session — exact mechanism is an ADR detail), receiving a Maeve session token.
2. **Lookup:** on entering a world/server, client batch-requests cosmetics for visible players: `GET /v1/cosmetics?uuids=...`. Backend returns equipped cosmetics for users that are ours; **unknown UUIDs return nothing -> those players render vanilla.**
3. **Render:** player-renderer mixin draws equipped cosmetics (Blaze3D). Result: Maeve users see each other's cosmetics; non-users see vanilla; the game server is uninvolved.
4. **Real-time equip changes:** WebSocket push (Phase 4); Phase 3 uses cache + short TTL polling.
- **Storage format:** cosmetics = model descriptor (JSON, bone/attach-point + animation refs) + texture(s) (PNG), versioned in `cosmetics/`. Catalog + ownership in Postgres.

---

## Compliance & licensing (from day 0)

- **Server-legal bar:** even with a QoL audience, hold **every** module to a published allowed-mods policy (Hypixel's "Allowed Modifications" as the reference bar). No reach/killaura/autoclicker/X-ray/hitbox/anti-KB.
  - **`reach-text display` caveat:** show only the player's *own* post-hit reach derived from vanilla combat data; **never** live-measure an opponent's distance (bannable). Constrain or drop.
  - Toggle/auto-sprint: allowed on Hypixel, flagged elsewhere -> gate per-server.
- **`LICENSES/` manifest from the first commit:** every third-party dependency, version, license, and redistribution status. Sodium/Lithium are **LGPL-3.0** — we bundle them **unmodified, as separate jars (mere aggregation)**, ship their license texts, and link to their source. We do **not** statically link or modify them. (Verify Sodium's exact current license text into the manifest.)
- **No copied code/assets/branding** from Lunar/Feather/others.

---

## Monorepo layout

```
maeve/
  settings.gradle.kts                 # includes :mod :launcher :backend :shared
  gradle/libs.versions.toml           # single source of truth for versions
  build-logic/                        # convention plugins (shared compiler/JDK/lint config)
  mod/
    src/main/{java,kotlin}/...        # ModuleManager, modules, mixins, cosmetics render
    src/main/resources/fabric.mod.json, maeve.mixins.json
  launcher/
    src/...                           # Compose UI, auth, game provisioning, RPC, updater
  backend/                            # Ktor service (Phase 3)
  shared/                             # cosmetics protocol DTOs, models, version constants
  cosmetics/                          # cosmetic asset format + sample assets
  docs/
    adr/                              # ADR-0001 ... ADR-0009
    superpowers/specs/                # this design, committed
  LICENSES/                           # manifest + third-party license texts
  .github/workflows/                  # CI: build mod jar + launcher; lint; license check
```

## ADRs to write in Phase 0

1. **0001 Mod loader** — Fabric (vs NeoForge/Quilt/from-scratch). 2. **0002 Target version + mappings** — 26.1 + Mojang mappings (Yarn deprecated). 3. **0003 Java 25.** 4. **0004 Launcher stack** — Compose Multiplatform (vs Tauri/Electron). 5. **0005 Backend stack** — Ktor (vs Go/Node). 6. **0006 Auth & token storage** — PKCE, consumers tenant, client-side tokens in OS keychain, Azure Minecraft-API permission application. 7. **0007 Cosmetics protocol** — backend-mediated UUID lookup, Blaze3D rendering. 8. **0008 Build system** — Gradle multi-project + version catalog + convention plugins. 9. **0009 Compliance policy** — server-legal module bar, LGPL mere-aggregation bundling, LICENSES manifest.

---

## Roadmap (phased)

**Phase 0 — Foundation.** Monorepo + Gradle multi-project scaffold, version catalog, convention plugins; CI (build mod jar + launcher, lint, license check); `LICENSES/` manifest; ADRs 0001–0009; this spec committed. **Submit the Azure app + Minecraft-API-access form now** (long lead time, blocks launcher auth).

**Phase 1 — MVP.** Launcher authenticates with Microsoft (**device-code first** — no loopback server needed), downloads Temurin 25 + MC 26.1 + Fabric + Fabric API, injects our mod + bundled Sodium/Lithium, launches. Mod shows working **FPS + coords + keystrokes** HUD, a basic mod menu, and persistent per-module config. Cosmetics stubbed/local. No backend.

**Phase 2 — Client depth.** Draggable HUD editor; full module set; settings UI; Discord RPC; auto-update; authcode+PKCE flow. Account switching + profile management.

**Phase 3 — Backend & cosmetics.** Ktor + Postgres; Maeve identity keyed to MC UUID; cosmetics catalog, ownership/entitlements; **in-game cosmetic sync (backend-mediated)** with the player-renderer mixin live.

**Phase 4 — Social & breadth.** Friends/parties (WebSocket); real-time cosmetic equip push; more cosmetics; multi-version (1.21.x backport and/or 26.2/Vulkan) behind the version-abstraction layer; code signing + polish.

*Change from the brief's roadmap:* the Azure/Minecraft-API permission application is pulled into **Phase 0** (not Phase 1) because a 403 there blocks the entire launcher and approval has lead time. `LICENSES/` and ADRs are also Phase 0 as requested.

---

## Verification

- **Phase 0:** `./gradlew build` green for `:mod` and `:launcher`; CI passes on a clean checkout; `LICENSES/` lists every dependency; ADRs 0001–0009 present.
- **Phase 1 (end-to-end, manual):** launch launcher -> device-code login with a real MS account -> it downloads JDK 25 + MC 26.1 + Fabric + our mod + Sodium/Lithium -> game starts -> in-game: FPS, coords, keystrokes HUD visible; open mod menu, toggle a module, change a setting; **quit and relaunch -> config persisted**; confirm Sodium is active (FPS uplift / video settings show Sodium).
- **Build reproducibility:** all versions pinned in `libs.versions.toml`; CI builds from clean.

## Open risks / things to track

- **Azure Minecraft-API permission** approval latency (Phase 0 blocker).
- **26.2 Vulkan transition** — keep all rendering on Blaze3D; budget rework when 26.2 stabilizes.
- **Java 25** is new; bundle the JRE via the launcher so users aren't forced to install it.
- **Code signing** (Windows Authenticode ~$100–400/yr, +Apple later) deferred for budget — unsigned builds hit SmartScreen; revisit before public release.
- **LGPL obligations** — keep bundled mods unmodified + provide source links; re-verify on every version bump.
- Confirm each additional perf mod (FerriteCore, ImmediatelyFast, EntityCulling, etc.) is **ported to 26.1** and license-compatible before bundling.

---

## Post-approval corrections (verified during Phase 0 scaffold, June 2026)

The approved plan said "26.1" and carried a few versions from an earlier research
pass. Verifying live against Mojang's manifest, the Fabric example-mod (26.2 branch),
maven.fabricmc.net, and Modrinth corrected these. The architecture is unchanged;
only versions/coordinates moved:

| Item | Approved | Verified & used |
|---|---|---|
| Target MC | 26.1 (.2) | **26.2** (current stable release; 26.3 is snapshot) |
| Fabric Loader | 0.18.4 | **0.19.3** |
| Loom plugin | id `net.fabricmc.fabric-loom`, v1.15 | id `net.fabricmc.fabric-loom` (correct), **v1.17.12** |
| Gradle | 9.4.0 | **9.6.0** (Loom 1.17.x requires >= 9.5) |
| Fabric API | ~0.153 (guess) | **0.152.2+26.2** |
| Mappings | "Mojang mappings" | **none** — 26.x jars are unobfuscated; no `mappings`, plain `implementation` |
| Sodium / Lithium | 0.8.7 / 0.22.1 | **0.9.0 / 0.25.0** (mc26.2 builds) |
| Java | 25 | 25 (confirmed: 26.2 requires JDK 25) |

**Verification result:** `./gradlew build` is green across all four modules
(`:shared` incl. a passing serialization test, `:mod` compiled against MC 26.2 via
Loom, `:launcher` Compose, `:backend` Ktor). Toolchain used: Temurin JDK 25.0.3,
Gradle 9.6.0.

Note: 26.2 being the current stable means the OpenGL→Vulkan transition is live —
reinforcing the Blaze3D-only rendering rule for cosmetics/HUD (ADR-0002, ADR-0007).
