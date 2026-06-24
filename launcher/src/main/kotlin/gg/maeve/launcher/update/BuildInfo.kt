package gg.maeve.launcher.update

/** Version + build-time dev flag, baked into a classpath resource at build time. */
object BuildInfo {
    private val props: Map<String, String> = runCatching {
        BuildInfo::class.java.getResourceAsStream("/build.properties")?.bufferedReader()?.use { r ->
            r.readLines().mapNotNull { line ->
                line.split("=", limit = 2).takeIf { it.size == 2 }?.let { it[0].trim() to it[1].trim() }
            }.toMap()
        }
    }.getOrNull() ?: emptyMap()

    val version: String = props["version"]?.takeIf { it.isNotEmpty() } ?: "0.0.0"

    /** True only in dev builds (-Pmaeve.dev=true). Gates the offline bypass; false in public releases. */
    val isDev: Boolean = props["dev"] == "true"
}
