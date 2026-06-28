# Snell Launcher — UI & Design System Brief (for Claude Design)

> **Audience:** Claude Design (frontend/UI design agent).
> **Goal:** Design the full UI and a reusable design system for the **Snell** desktop launcher.
> **Status of current UI:** bare stub (a window with a non-working "Sign in with Microsoft" button and a "Launch" button). Treat it as a blank slate — redesign from scratch.

---

## 1. Product context & goals

**Snell** is a legitimate, **server-legal** performance + quality-of-life Minecraft client — an FPS boost, bundled performance mods (Sodium, Lithium), an in-game HUD/mod menu, cosmetics that sync between Snell users, friends/parties, and our own launcher. It competes with **Lunar Client** and **Feather**. It is explicitly **not** a cheat client.

The **launcher** is the front door: it authenticates the player's Microsoft account, downloads/manages Minecraft (currently 26.2) + Fabric + our mods, and launches the game. First impressions of the whole product are formed here.

**Design goals**
- **Fast & lightweight** — the launcher for a *performance* client must itself feel instant and use little RAM. Snappy beats flashy.
- **Clean & uncluttered** — one obvious primary action (Play). No ads, ever. No nagging.
- **Trustworthy** — communicates "official Microsoft login, legit client, safe on big servers."
- **Scales with the roadmap** — must gracefully grow from MVP (sign-in + launch) to cosmetics, friends/parties, multi-version.

---

## 2. Target users & jobs-to-be-done

**Primary:** general survival / quality-of-life Minecraft players who want more FPS and nice HUD/QoL features without fuss.
**Secondary:** Hypixel/PvP-curious players who care about FPS, keystrokes, CPS, and cosmetics — but Snell stays server-legal.

**Jobs-to-be-done**
- "Sign in with my Microsoft account and just play, fast."
- "Get more FPS without configuring mods myself."
- "Pick my Minecraft version / profile without confusion."
- "Tweak RAM/Java if I know what I'm doing — but hide it if I don't."
- *(Later)* "Show off cosmetics" / "play with friends / make a party."

**Skill spread:** assume mostly non-technical players; expose power features (JVM args, game dir) behind an Advanced section.

---

## 3. Competitive analysis

| Launcher | Layout | Visual style | Emulate | Avoid |
|---|---|---|---|---|
| **Lunar Client** (new launcher) | Left icon sidebar (settings bottom-left); **right-side social pane** (friends/DMs); large hero imagery | Vibrant, big images, smooth animations, dark modern | Big obvious Play, integrated social pane, smooth feel, dark theme | Heaviness; storefront prominence; don't let cosmetics/store dominate the home screen |
| **Feather** | Clean minimal launcher; integrated **skin manager** + social | "Sleek, clean, modern," intuitive from first launch | **Minimalism**, low friction, calm first-run | Being *so* minimal it hides useful controls |
| **Modrinth App** | Clean home with tabs (browse / installed / **instances**); instant instance switching; **Activity Monitor** (live RAM/CPU); **Files tab** (configs/logs); profile importer | Modern, polished, fast, **no ads/bloat** | Polish + speed, instant version/profile switching, optional live resource monitor, "no bloat" ethos | Web-app weight (Electron-style RAM) — we're a perf product, stay lean |
| **Prism Launcher** | Native Qt; **instance grid**; per-instance shortcuts; themeable | Utilitarian, lightweight, power-user | Low resource use, fast switching, keyboard-friendliness | Utilitarian/dated look; too much config exposed by default |
| **Badlion** | Heavy launcher | Cluttered | — | **Everything users hate: ads, bloat, slow load, pushy cosmetic monetization, login bugs.** This is the anti-pattern. |

**Net takeaways**
- **Be the Modrinth-App level of polish + the Feather level of calm, with a lightweight footprint like Prism.** Avoid Badlion's bloat/ads entirely.
- One unmissable **Play** button. Version/account/profile switching must be instant and obvious.
- A **social pane** (Lunar-style) is the natural home for Phase-4 friends/parties — design space for it now, ship later.
- An optional **resource/performance readout** (Modrinth-style) reinforces the "performance client" identity and is on-brand.

*Sources:* [Lunar new launcher](https://www.lunarclient.com/news/the-new-launcher-is-here), [Lunar store redesign](https://www.lunarclient.com/news/inside-the-new-lunar-client-store-fresh-design-better-experience), [Feather](https://feathermc.com/), [Prism vs Modrinth vs CurseForge](https://space-node.net/blog/curseforge-vs-modrinth-vs-prism-launcher-2026), [Prism Launcher](https://prismlauncher.org/), [Badlion criticism (Hypixel forums)](https://hypixel.net/threads/badlion-client-haters.3686923/), [Badlion reviews (Trustpilot)](https://www.trustpilot.com/review/client.badlion.net).

---

## 4. Brand direction (starting points — refine, don't treat as final)

Snell has **no logo, colors, or type yet**. "Snell" is an Irish name (a legendary warrior queen) — elegant, a little regal, memorable. Propose & explore; below are three candidate moods to react to:

1. **"Twilight Royal"** (recommended starting point) — dark-mode-first, deep near-black/indigo base, a refined **violet/indigo accent**, restrained gold/warm highlight for premium moments. Elegant, calm, a touch regal. Differentiates from Lunar's electric blues and Feather's neutral greys.
2. **"Performance Tech"** — charcoal base, **cyan/teal** accent, crisp and fast-feeling, sci-fi-lite. Leans into the FPS/performance identity. Risk: generic "gamer" look.
3. **"Soft Cozy"** — warm dark base, **amber/coral** accent, friendly and survival-player-cozy. Approachable. Risk: less "competitive/fast."

**Constraints either way:** **dark mode is the default** (light mode optional/later). Accent used sparingly for the primary action + active states. Typography should feel **clean, geometric, modern** (Inter/Geist-like for UI); a slightly characterful display face for the wordmark/headings is welcome. Propose the actual palette(s) — these are prompts, not a spec.

---

## 5. Screens & flows to design (mapped to roadmap)

Mark each as **MVP** (Phase 1–2) or **Later** (Phase 3–4).

| Screen / flow | Phase | Notes |
|---|---|---|
| **Sign-in** | MVP | Microsoft **device-code** flow (show user code + "go to microsoft.com/link", poll/progress state) *and* an **auth-code** path. Needs: idle, awaiting-code, polling, success, error/expired states. Reassure "official Microsoft login." |
| **Home / Play** | MVP | The core screen. Big **Play** button; **version/profile selector**; **account switcher**; space for a news/changelog strip. Download/launch progress lives here. |
| **Settings** | MVP | Java/RAM slider, JVM args (Advanced), game directory, launcher behavior. Progressive disclosure — simple by default, Advanced collapsible. |
| **Mods / Performance** | MVP→P2 | Show bundled perf mods (Sodium, Lithium) with on/off + status; "what these do." Keep honest/simple. |
| **Update / changelog** | MVP→P2 | Auto-update state + release notes. |
| **Account management** | P2 | Multiple accounts, add/remove, set default; avatars. |
| **Cosmetics** | P3 | Browse owned/equippable cosmetics (cloaks/wings/hats/pets), preview on player model, equip. **Not** a pushy store — ownership-first. |
| **Friends / Party** | P4 | Lunar-style **social pane** (friends list, status, DMs) + party create/join/invite. Design the pane now even if shipped later. |

**Key states to cover for every screen:** loading, empty, error, offline, in-progress (downloads), success. The download/launch progression (resolving → downloading MC/assets → installing Fabric → launching) deserves a clear, calm progress treatment.

---

## 6. Design system needs

Deliver a coherent, tokenized system (dark-first):

- **Color tokens:** background layers (base / surface / elevated), text (primary/secondary/disabled), **accent** (+ hover/pressed/focus), semantic (success/warning/error/info), border/divider, overlay/scrim. Provide hex + intended usage. Light theme optional but structure tokens so it can be added.
- **Typography scale:** display / headline / title / body / label / caption with size, weight, line-height (Compose `sp`). Pick UI typeface(s) + a wordmark/heading face.
- **Spacing & layout:** a spacing scale (e.g. 4/8-based, in `dp`), grid/gutters, content max-widths, sidebar width, standard paddings.
- **Shape & elevation:** corner-radius scale, elevation/shadow levels (subtle — desktop, not mobile).
- **Components (specs + states):** primary/secondary/tertiary/icon **buttons**; **toggles/switches**; **sliders** (RAM); **dropdowns/selects** (version/account); **cards**; **left nav / sidebar**; **modals/dialogs**; **lists & list rows**; **progress & download states** (determinate + indeterminate); **status pills/badges** (e.g. "Up to date", "Downloading", "Online"); **tooltips**; **inputs**; **empty states**; **toasts/notifications**; **avatars**.
- **Iconography:** consistent line-icon set direction.
- **Motion:** **fast and purposeful** — short durations (~120–200ms), subtle easing, meaningful transitions (nav, state changes, download progress). No heavy parallax or decorative animation; it must feel *lighter* than the game it launches. Define standard durations/easings as tokens.

---

## 7. Technical constraints for the designer

- **Implementation target: Compose Multiplatform desktop (Kotlin/JVM).** Everything must be expressible/implementable in Compose. **Material 3 is available** — design *can* build on M3's `ColorScheme`/`Typography`/`Shapes` or define a custom theme on top; either way map tokens to Compose concepts.
- **Windows-first**, but keep it **cross-platform-friendly** (no Windows-only UI idioms; avoid OS-specific chrome assumptions).
- **Window size:** design for a resizable desktop window, **min ~1000x640**, comfortable at ~1280x800. Define responsive behavior between min and larger.
- **Performance-conscious:** lean visuals, limited simultaneous animations, no giant always-loaded media. This is the *performance* product's launcher.
- **No web stack** — it's native Compose, not HTML/CSS; express specs in terms a Compose dev can apply (dp/sp/hex/shape/weight), not CSS classes.

---

## 8. Accessibility

- **Contrast:** meet WCAG AA for text/icons on the dark theme (and any light theme). Don't rely on the accent color alone to convey state.
- **Keyboard:** full keyboard navigation (the in-game menu is already keyboard-driven; the launcher should be navigable too). Visible **focus states** on all interactive elements.
- **Scalable text:** layouts must tolerate a larger UI scale / longer localized strings without breaking.
- **Motion:** respect a "reduce motion" preference; keep essential info non-animated.

---

## 9. Deliverables requested from Claude Design

1. **Design tokens** — a single source-of-truth tokens spec (colors incl. dark theme, typography scale, spacing, shape, elevation, motion durations/easings), expressed so it maps cleanly to a Compose `MaterialTheme` (ColorScheme + Typography + Shapes) or a custom theme object. Hex/dp/sp values.
2. **Brand proposal** — chosen palette + typography + a direction for a simple wordmark (logo can come later; at minimum a type-based wordmark).
3. **Key screen specs/mockups** — at minimum: Sign-in (with its states), Home/Play, Settings, Mods/Performance. Layout, spacing, component usage, and all states (loading/empty/error/progress). ASCII wireframes or structured layout specs are fine if pixel comps aren't produced.
4. **Component specs** — for each component in section 6: anatomy, variants, states (default/hover/pressed/focus/disabled), sizing.
5. **Later-phase layouts (lighter touch):** cosmetics screen + the friends/party social pane, enough to reserve space and direction.
6. **Implementation notes** — concise guidance on translating the system into Compose Multiplatform (theme setup, where tokens live, naming).

Organize deliverables so a solo dev can implement incrementally (tokens + components first, then screens).

---

## 10. Hard don'ts

- **Do NOT copy** the visual design, layouts-as-pixels, assets, icons, or branding of Lunar, Feather, Badlion, or any other client. Learn from their *patterns*; the execution must be **original**.
- **No ads. No bloat. No pushy cosmetic/store monetization** on the home screen (the explicit Badlion anti-pattern). Cosmetics are ownership-first, tucked away — not shoved in the player's face.
- **Position as a legitimate, server-legal client** — never imply cheats, hacks, or anything that gets users banned.
- **Don't make it heavy** — no design choices that bloat RAM/CPU or slow startup. The launcher must embody "performance."
- Keep brand suggestions as **starting points**; the user expects Design to explore and refine, not rubber-stamp section 4.

---

## 11. Locked decisions (answers to Design's clarifying questions, June 2026)

1. **Brand mood:** Twilight Royal — dark indigo/near-black base, refined violet accent, restrained gold; dark-mode default (refine the exact palette).
2. **Primary deliverable:** Both, weighted to the **system** — tokens + components first, then key screens (solo dev implements incrementally in Compose).
3. **Screens to fully design now (MVP four):** Sign-in (all states), Home/Play (+ download/launch progress), Settings (RAM/Java, Advanced collapsible), Mods/Performance. Cosmetics (P3) + Friends/Party pane (P4): reserve space, lighter touch, later.
4. **Variations:** a couple of palette/accent variations only; no Home-layout variants yet.
5. **Per-screen states:** full state matrix for Sign-in + Home/Play; happy-path for the rest.
6. **Wordmark:** wordmark + a simple abstract mark (crown/diamond motif, kept abstract); type-based wordmark at minimum.
7. **Typography:** regal display + clean UI — refined, slightly high-contrast display for the 'Snell' wordmark + headings; clean geometric sans for body/UI.
