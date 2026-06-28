// Root build script. Per-module configuration lives in each subproject's
// build.gradle.kts and in the shared convention plugins under build-logic/.

tasks.register("printVersions") {
    group = "help"
    description = "Prints the pinned tool versions from the version catalog."
    doLast {
        val libs = the<org.gradle.api.artifacts.VersionCatalogsExtension>().named("libs")
        println("Snell pinned versions:")
        listOf(
            "minecraft", "fabric-loader", "fabric-api", "loom",
            "kotlin", "compose-multiplatform", "ktor"
        ).forEach { alias ->
            libs.findVersion(alias).ifPresent { println("  $alias = ${it.requiredVersion}") }
        }
    }
}
