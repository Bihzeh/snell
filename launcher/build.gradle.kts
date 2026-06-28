plugins {
    id("snell.kotlin-common")
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)

    // Microsoft auth + Mojang downloads.
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    implementation(project(":shared"))

    // Brand SVG -> PNG/ICO rasterizer (rasterizeBrand task only; pure-Java, headless).
    // compileOnly so Batik never lands in the shipped app.
    compileOnly("org.apache.xmlgraphics:batik-transcoder:1.17")
    compileOnly("org.apache.xmlgraphics:batik-codec:1.17")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
}

tasks.test { useJUnitPlatform() }

val brandTools: Configuration by configurations.creating
dependencies {
    brandTools("org.apache.xmlgraphics:batik-transcoder:1.17")
    brandTools("org.apache.xmlgraphics:batik-codec:1.17")
}

// Bake version + the build-time dev flag into a classpath resource read at runtime.
// dev=true ONLY when built with -Psnell.dev=true (the dev-build workflow). Public
// releases build without it -> dev=false -> the offline bypass is absent, not merely
// hidden. No client-side password/file can enable it in a public binary.
val generatedVersionDir = layout.buildDirectory.dir("generated/version")
val generateBuildInfo = tasks.register("generateBuildInfo") {
    val outFile = generatedVersionDir.get().file("build.properties").asFile
    val v = project.version.toString()
    val dev = providers.gradleProperty("snell.dev").getOrElse("false")
    inputs.property("version", v)
    inputs.property("dev", dev)
    outputs.file(outFile)
    doLast { outFile.parentFile.mkdirs(); outFile.writeText("version=$v\ndev=$dev\n") }
}
sourceSets.named("main") { resources.srcDir(generatedVersionDir) }
tasks.named("processResources") { dependsOn(generateBuildInfo) }

// Bundle the Snell Fabric mod jar into the launcher's own resources so INSTALLED
// launchers ship our mod (not just the dev-only mod/build/libs path). The :mod jar is
// copied under a version-agnostic name; at runtime ModProvisioner extracts it from the
// classpath resource bundled-mods/snell.jar into the game's mods/ folder. It is opaque
// bytes on the resource path — NOT on the launcher's compile/runtime classpath — so the
// mod's Minecraft/Fabric classes never leak into the launcher JVM. (Loom runs in no-remap
// mode here, so `jar` — not `remapJar` — is the loadable artifact; it carries JiJ includes.)
// srcDir is the resource ROOT, so copy into a bundled-mods/ subdir under it to get the
// classpath resource path bundled-mods/snell.jar (not a root-level snell.jar).
val bundledModResDir = layout.buildDirectory.dir("generated/bundled-mod-resources")
val copyBundledModJar = tasks.register<Copy>("copyBundledModJar") {
    // from(archiveFile) carries the implicit task dependency on :mod:jar and tracks the
    // exact jar as an input, so the copy reruns when (and only when) the mod jar changes.
    from(project(":mod").tasks.named<Jar>("jar").flatMap { it.archiveFile })
    into(bundledModResDir.map { it.dir("bundled-mods") })
    rename { "snell.jar" }
}
sourceSets.named("main") { resources.srcDir(bundledModResDir) }
tasks.named("processResources") { dependsOn(copyBundledModJar) }

compose.desktop {
    application {
        mainClass = "gg.snell.launcher.MainKt"

        // ProGuard 7.7 (used by Compose release builds) cannot read Java 25 bytecode
        // (class version 69). Disable until ProGuard supports 25; re-enable for size.
        buildTypes.release.proguard {
            isEnabled.set(false)
        }
        nativeDistributions {
            // Windows-first; macOS/Linux added later (see ADR-0004).
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe,
            )
            packageName = "Snell"
            packageVersion = (project.version as String).substringBefore("-").ifEmpty { "1.0.0" }
            windows {
                // Snell "slipstream" mark — regenerate via `:launcher:rasterizeBrand`.
                iconFile.set(project.file("icons/snell.ico"))
            }
        }
    }
}

// Headless provision + launch verification (downloads MC/Fabric/mods; needs network).
tasks.register<JavaExec>("provisionTest") {
    group = "verification"
    description = "Provision MC 26.2 + Fabric + mods and launch headless to verify the chain."
    mainClass.set("gg.snell.launcher.game.ProvisionMainKt")
    classpath = sourceSets["main"].runtimeClasspath
    workingDir = rootProject.projectDir
}

// Headless render of the UI screens to PNGs (CPU Skia; no display needed).
tasks.register<JavaExec>("uiPreview") {
    group = "verification"
    description = "Render launcher screens to build/ui-preview/*.png"
    mainClass.set("gg.snell.launcher.ui.UiPreviewMainKt")
    classpath = sourceSets["main"].runtimeClasspath
    // CPU Skia on headless/GPU-less CI runners.
    systemProperty("skiko.renderApi", "SOFTWARE")
}

// Rasterize brand SVGs (logo/icon) to PNG via Skia's CPU surface.
tasks.register<JavaExec>("rasterizeBrand") {
    group = "build"
    description = "Rasterize docs/brand/snell-icon.svg to launcher/build/brand/icon-*.png"
    mainClass.set("gg.snell.launcher.brand.BrandRasterMainKt")
    classpath = sourceSets["main"].runtimeClasspath + brandTools
    workingDir = rootProject.projectDir
}
