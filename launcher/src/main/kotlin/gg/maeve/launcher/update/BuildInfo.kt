package gg.maeve.launcher.update

/** The launcher's own version, baked into a classpath resource at build time. */
object BuildInfo {
    val version: String = runCatching {
        BuildInfo::class.java.getResource("/maeve-version.txt")?.readText()?.trim()
    }.getOrNull()?.takeIf { it.isNotEmpty() } ?: "0.0.0"
}
