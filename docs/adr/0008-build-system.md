# ADR-0008: Gradle multi-project + version catalog

Status: Accepted

## Context
Four Kotlin/JVM modules (mod, launcher, backend, shared) plus convention plugins.
Reproducible builds and a single source of truth for versions are required from
day one. Fabric tooling (Loom) is Gradle-based.

## Decision
A single **Gradle multi-project** build. Versions are centralized in
`gradle/libs.versions.toml`. Shared build config lives in `build-logic/`
convention plugins (e.g. `maeve.kotlin-common`). The mod uses the 26.1 no-remap
`net.fabricmc.fabric-loom` plugin.

## Consequences
- One command builds everything; CI builds from a clean checkout.
- Versions change in exactly one place.
- Convention plugins keep per-module build files small.
