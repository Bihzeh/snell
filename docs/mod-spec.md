# Snell Mod — Spec & Handoff

Status of the Fabric client mod (`mod/`) and the plan for the rest. Target: **Minecraft
26.2**, Fabric, Mojang mappings (unobfuscated), JDK 25. Companion to ADR-0001/0002/0007/0009
and the architecture spec.

## What exists (built, compiles vs real 26.2, Fabric loads it)

- **Module system** — `module/Module.kt` (`Module`, `HudModule`, `HudLine`),
  `module/ModuleManager.kt` (registry + config bridge; registration order = HUD draw order).
- **HUD modules** — `module/hud/{Fps,Coords,Keystrokes}Module.kt`. Pure logic; produce
  `List<HudLine>` from a `GameContext`. Keystrokes off by default.
- **Render** — `render/HudRenderController.kt` (pure; draws enabled modules via a `HudCanvas`).
- **Bridge (the ONLY Minecraft-touching file)** — `platform/FabricMinecraftBridge.kt`. Real
  26.2 bindings: HUD via `HudElementRegistry.addLast` + `GuiGraphicsExtractor.text` (retained
  mode, Blaze3D-safe); keybind via `KeyMappingHelper.registerKeyMapping` + `ClientTickEvents`;
  menu via `Minecraft.setScreenAndShow`; state from `getFps()` / `player.position()` /
  `options.keyUp/keyDown/keyLeft/keyRight`.
- **Mod menu** — `platform/SnellMenuScreen.kt` (keyboard: Up/Down/Enter/Esc) + pure
  `ui/ModMenuController.kt`. Opened by Right-Shift.
- **Config** — `config/Config.kt`: per-module JSON (`enabled`, `x`, `y`) under
  `.minecraft/config/snell/`, schema-versioned, kotlinx.serialization.
- **Cosmetics (Phase 3 stubs)** — `cosmetics/CosmeticsClient.kt` (interface +
  `LocalStubCosmeticsClient`), `cosmetics/CosmeticsRenderer.kt` (the mixin integration point).
- **Entry** — `SnellMod.kt` (`ClientModInitializer`): wires config → modules → HUD → menu.
- **Resources** — `fabric.mod.json`, `snell.mixins.json` (empty client list for now),
  `assets/snell/lang/en_us.json`.
- **Tests** — `HudAndConfigTest.kt` (HUD render logic + config round-trip).

**Caveat:** in-game *visual* render is unconfirmed (build box is headless). It compiles
against the real 26.2 jar (symbols javap-verified) and Fabric discovers/loads it in the
launch log — visual confirmation is a desktop step.

## Architecture rules (keep these)
- **Pure logic has NO Minecraft imports** (modules, HudRenderController, Config, menu
  controller) → unit-testable + version-independent.
- **All version-sensitive code lives in `FabricMinecraftBridge.kt`** (the seam). Porting to
  another MC version = implement that one file behind `MinecraftBridge`.
- **Render only through Blaze3D / the retained-mode extractor — never raw GL** (26.2 is the
  OpenGL→Vulkan transition line; raw GL breaks on 26.x).

## Add a HUD module (the pattern)
1. Implement `HudModule` (`id`, `displayName`, `var enabled`, `var x/y`, `render(ctx): List<HudLine>`).
2. If it needs new game data, add a field to `GameContext` and populate it in
   `FabricMinecraftBridge.capture()`.
3. Register it in `SnellMod.onInitializeClient()`. Config persistence + menu listing are automatic.

## Module backlog (Phase 2) — with server-legal compliance
Bar = a published allowed-mods policy (Hypixel "Allowed Modifications"). Forbidden everywhere:
reach, kill aura, auto-clicker, X-ray, hitbox extenders, anti-knockback.

| Module | Legality / note |
|---|---|
| CPS (clicks/sec) | OK — reads your own clicks |
| Armor / potion HUD | OK — your own state |
| Scoreboard | OK |
| Toggle-sprint / auto-sprint | Allowed on Hypixel; **gate per-server** |
| Zoom | OK (OptiFine-style) |
| Custom crosshair | OK |
| Post-hit **reach display** | Show only YOUR post-hit reach from vanilla combat data. **Never** live-measure an opponent's distance (bannable). Constrain or drop. |

## Larger pieces
- **HUD editor (Phase 2):** a draggable `Screen` that moves each `HudModule`'s x/y (persisted
  via Config). New `SnellHudEditorScreen` + drag handling in the bridge.
- **Cosmetics render (Phase 3):** a Mixin into the player renderer calling
  `CosmeticsRenderer.renderFor(uuid, client)`, drawing equipped cosmetics via Blaze3D. Data from
  `HttpCosmeticsClient` (backend-mediated UUID lookup, NOT in-game packets; non-users → vanilla).
  See ADR-0007. Register the mixin in `snell.mixins.json`.

## Build / run
- `./gradlew :mod:build` → `mod/build/libs/mod-<v>.jar`.
- Launcher injects it into the game profile. The launcher distribution **bundles the mod**
  (`launcher/build.gradle.kts` `copyBundledModJar` → classpath resource `bundled-mods/snell.jar`);
  at runtime `ModProvisioner` extracts it into the instance's `mods/`. Both the public and
  dev (`-Psnell.dev=true`) workflows embed and install the mod identically — decoupled from
  `BuildInfo.isDev`. A freshly built `mod/build/libs` jar is only preferred when the launcher
  is run from the repo (`./gradlew :launcher:run`).
- `./gradlew :launcher:provisionTest` launches headless and confirms Fabric loads `snell`.
