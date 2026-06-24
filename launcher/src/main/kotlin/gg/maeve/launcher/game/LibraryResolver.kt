package gg.maeve.launcher.game

/** The running OS/arch in Mojang's vocabulary. */
data class Platform(val os: String, val arch: String) {
    /** Expected `:natives-*` classifier for this platform. */
    val nativeSuffix: String = buildString {
        append("natives-")
        append(if (os == "osx") "macos" else os)
        if (arch == "arm64") append("-arm64")
    }

    companion object {
        fun current(): Platform {
            val n = System.getProperty("os.name").lowercase()
            val os = when {
                n.contains("win") -> "windows"
                n.contains("mac") || n.contains("darwin") -> "osx"
                else -> "linux"
            }
            val a = System.getProperty("os.arch").lowercase()
            val arch = when {
                a.contains("aarch64") || a.contains("arm64") -> "arm64"
                a.contains("64") -> "x64"
                else -> "x86"
            }
            return Platform(os, arch)
        }
    }
}

/** Evaluates Mojang/Fabric rule lists (os + features). We enable no game features. */
object RuleEval {
    fun matches(rule: Rule, p: Platform, features: Map<String, Boolean>): Boolean {
        val osOk = rule.os?.let { o ->
            (o.name == null || o.name == p.os) && (o.arch == null || o.arch == p.arch)
        } ?: true
        val featOk = rule.features?.all { (k, v) -> features[k] == v } ?: true
        return osOk && featOk
    }

    fun allowed(rules: List<Rule>?, p: Platform, features: Map<String, Boolean> = emptyMap()): Boolean {
        if (rules.isNullOrEmpty()) return true
        var allow = false
        for (rule in rules) if (matches(rule, p, features)) allow = rule.action == "allow"
        return allow
    }
}

data class ResolvedLib(
    val path: String,   // relative path under libraries/
    val url: String,
    val sha1: String,
    val size: Long,
    val isNative: Boolean,
)

/**
 * Pure resolution of a Mojang library list for a platform: applies rules, filters
 * native classifiers to the running OS/arch, and dedupes by path. (26.2 natives are
 * classpath jars LWJGL self-extracts — no manual extraction needed.)
 */
object LibraryResolver {
    fun resolve(libraries: List<Library>, p: Platform): List<ResolvedLib> {
        val out = LinkedHashMap<String, ResolvedLib>()
        for (lib in libraries) {
            if (!RuleEval.allowed(lib.rules, p)) continue
            val art = lib.downloads?.artifact ?: continue
            val classifier = lib.name.split(":").getOrNull(3)
            val isNative = classifier?.startsWith("natives-") == true
            if (isNative && classifier != p.nativeSuffix) continue
            out[art.path] = ResolvedLib(art.path, art.url, art.sha1, art.size, isNative)
        }
        return out.values.toList()
    }
}
