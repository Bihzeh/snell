# ADR-0005: Backend — Ktor

Status: Accepted

## Context
Options: Go (single binary, great perf), Node/TS (huge ecosystem), Ktor
(Kotlin/JVM). The rest of the stack is Kotlin/JVM and shares DTOs via `:shared`.
Needs REST + WebSocket for cosmetics sync and friends/parties.

## Decision
Build the backend in **Ktor** (Kotlin/JVM), packaged as a self-hostable fat jar /
container to fit the minimal-budget constraint.

## Consequences
- Reuses `:shared` DTOs directly — no schema duplication or drift.
- One language/toolchain for the whole project.
- Native WebSocket support for Phase 4 social features.
- Rejected: Go/Node (extra language; lose DTO sharing).
