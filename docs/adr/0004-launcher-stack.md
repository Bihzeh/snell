# ADR-0004: Launcher — Compose Multiplatform

Status: Accepted

## Context
Options: Tauri (Rust + webview; tiny, but a second language), Electron (easy, but
bundles Chromium — heavy RAM, ironic for a perf client), Compose Multiplatform
(Kotlin/JVM desktop). The mod and backend are already Kotlin/JVM; the dev is solo.

## Decision
Build the launcher in **Kotlin Compose Multiplatform** (desktop), Windows-first.

## Consequences
- One language across mod + launcher + backend; shared `:shared` module; least
  context-switching for a solo dev.
- Ships a JVM runtime (acceptable: the launcher already manages a bundled JDK).
- Cross-platform later without a rewrite.
- Rejected: Tauri (extra language/toolchain), Electron (heavy runtime).
