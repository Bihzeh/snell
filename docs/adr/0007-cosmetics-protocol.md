# ADR-0007: Cosmetics sync protocol

Status: Accepted

## Context
Cosmetics must be visible between Snell users in-game while non-users see vanilla.
Large servers (Hypixel) do not relay custom plugin-channel packets, so in-game
packet sync is unreliable and can be flagged.

## Decision
Sync **out-of-band through the Snell backend**, keyed by player UUID:
1. Client proves control of its MC UUID to the backend -> Snell session token.
2. On entering a world/server, client batch-requests cosmetics for visible players
   (`GET /v1/cosmetics?uuids=...`). Unknown UUIDs return nothing -> vanilla.
3. A player-renderer Mixin draws equipped cosmetics via **Blaze3D**.
4. Live equip changes via WebSocket push (Phase 4); Phase 3 uses cache + short TTL.

Wire format DTOs live in `:shared` so the mod and backend cannot drift.

## Consequences
- No dependency on game-server cooperation; safe on anticheat servers.
- Cosmetics are visible only among Snell users (acceptable; matches the market).
- Backend availability affects cosmetic visibility (degrade gracefully to vanilla).
