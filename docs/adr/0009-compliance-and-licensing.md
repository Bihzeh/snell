# ADR-0009: Server-legal + licensing policy

Status: Accepted

## Context
The product's value depends on being allowed on big servers, and on respecting the
licenses of bundled mods. Sodium/Lithium are LGPL-3.0.

## Decision
- **Server-legal bar:** every module is held to a published allowed-mods policy
  (Hypixel's "Allowed Modifications" as the reference). No reach/killaura/
  autoclicker/X-ray/hitbox/anti-knockback. `reach-text` shows only the player's own
  post-hit reach from vanilla data — never live opponent distance. Toggle/auto-
  sprint is gated per-server.
- **Bundled-mod licensing:** LGPL-3.0 mods are shipped **unmodified, as separate
  jars (mere aggregation)**, with their license texts and source links; never
  statically linked or modified. Tracked in `LICENSES/`.
- **No copied code/assets/branding** from other clients.
- **Project-code license:** proprietary by default (see `LICENSE`); revisit if a
  different model is chosen.

## Consequences
- Users stay welcome on anticheat servers; brand risk minimized.
- LGPL obligations re-verified on every version bump.
