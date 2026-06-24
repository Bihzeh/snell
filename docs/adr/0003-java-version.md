# ADR-0003: Java 25

Status: Accepted

## Context
Minecraft 26.1 requires Java 25 (OpenJDK 25); Java 21/17 will not run it. The
launcher, mod, and backend are all Kotlin/JVM and benefit from a single toolchain.

## Decision
Standardize on **JDK 25** across all modules (Kotlin `jvmToolchain(25)`). The
launcher bundles a Temurin 25 runtime so end users need not install Java.

## Consequences
- One toolchain for the whole monorepo.
- Java 25 is relatively new; pin the Temurin build and test the bundled runtime.
- CI must provision JDK 25.
