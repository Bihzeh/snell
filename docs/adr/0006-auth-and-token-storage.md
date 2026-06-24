# ADR-0006: Microsoft auth + client-side token storage

Status: Accepted

## Context
Auth is the official Microsoft -> Xbox Live -> XSTS -> Minecraft flow (verified
June 2026). Newly-registered Azure apps must apply (form) for Minecraft API access
or `api.minecraftservices.com` returns 403. Storing Microsoft refresh tokens on a
central backend creates a high-value breach target and heavier privacy obligations.

## Decision
- Use OAuth2 **device-code** (Phase 1) and **auth-code + PKCE** (Phase 2), public
  client (no secret), **consumers** tenant, scope `XboxLive.signin offline_access`.
- Microsoft tokens are stored **client-side only**, encrypted in the OS credential
  store (Windows Credential Manager/DPAPI first). The backend NEVER receives them.
- The backend keys its own identity to the Minecraft UUID.
- Submit the Azure Minecraft-API-access application in **Phase 0** (long lead time).

## Consequences
- Smaller breach blast radius; lighter compliance load.
- The Azure approval is a hard external dependency gating launcher auth.
- Per-OS keychain implementations are needed as OS targets expand.
