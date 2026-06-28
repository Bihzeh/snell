# Minecraft API access request (Azure app review)

Reference for the Minecraft API access application submitted at
<https://aka.ms/mce-reviewappid> (required so `api.minecraftservices.com` stops
returning 403 for our newly-registered Azure app). Keep this in sync with what was
submitted so it can be resubmitted if reviewers ask for changes.

## Form field values

| Field | Value |
|---|---|
| Application (client) ID | _(from Azure → app registration → Overview)_ |
| Tenant ID | _(Azure → Microsoft Entra ID → Overview → Tenant ID; the default directory)_ |
| Associated website / domain | https://github.com/Bihzeh/snell |
| Contact email | _(an address you monitor)_ |
| Auth method | Public client (no secret) — OAuth device-code + authorization-code with PKCE |
| Scope | `XboxLive.signin offline_access` (consumers tenant) |

> Keep these consistent: the website above must match the app registration's
> **Home page URL** (Branding & properties) and the `contact.homepage` in
> `mod/src/main/resources/fabric.mod.json`.

## Justification (submitted text)

Snell is a third-party launcher and client for Minecraft: Java Edition, in the same
category as Lunar Client and Feather. It provides a performance boost by bundling
well-known open-source optimization mods (such as Sodium and Lithium) on the Fabric
mod loader, along with quality-of-life features like an in-game HUD and mod menu. The
application uses the Minecraft API for a single purpose: to authenticate the player's
**own** Microsoft account through the official Microsoft OAuth flow (device-code and
authorization-code with PKCE, consumers tenant, `XboxLive.signin` scope), exchange the
result for a Minecraft access token via `login_with_xbox`, and verify that the signed-in
account owns Minecraft: Java Edition before launching the game. We do not automate
accounts, scrape data, alter entitlements, or interact with multiplayer or commerce
services on the user's behalf beyond what the official launcher does.

Snell is built to be safe and compliant. Microsoft and Minecraft access/refresh tokens
are handled entirely on the user's own machine and stored encrypted in the operating
system's credential store; they are never transmitted to or stored on any Snell server.
The launcher downloads all game files exclusively from official Mojang endpoints and
never redistributes the Minecraft jar. The client is strictly server-legal — it contains
no cheating functionality (no reach, kill aura, auto-clicker, X-ray, hitbox modification,
or anti-knockback) and holds every feature to published server allowed-modification
policies, so users remain in good standing on networks like Hypixel. Any bundled
third-party mods are shipped unmodified under their own licenses. The Application (client)
ID in this request is used only by this launcher, solely to sign each user into their own
account.

## Additional info for reviewers (have ready)

- New, pre-release, solo-developer project; expecting a small initial user base.
- Public client: no client secret; PKCE / device-code only.
- Token handling: client-side only, OS credential store; never sent to our backend.
- Scope limited to `XboxLive.signin` + ownership/profile read — nothing broader.
- The cosmetics/friends backend (Phase 3+) never receives Microsoft tokens; it keys
  its own identity to the Minecraft UUID. See `docs/adr/0006-auth-and-token-storage.md`.

## Notes

- Approval is manual and can take days to weeks. Submitted as a Phase 0 long-lead item.
- Until approved, the device-code flow works up to Xbox/XSTS; `login_with_xbox` ->
  profile is where the 403 appears.
