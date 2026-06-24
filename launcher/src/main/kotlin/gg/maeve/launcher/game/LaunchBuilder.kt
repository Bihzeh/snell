package gg.maeve.launcher.game

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import java.io.File
import java.nio.file.Path

/** The assembled launch command (java executable supplied separately). */
data class LaunchCommand(
    val jvmArgs: List<String>,
    val mainClass: String,
    val gameArgs: List<String>,
) {
    fun fullCommand(java: String): List<String> = buildList {
        add(java); addAll(jvmArgs); add(mainClass); addAll(gameArgs)
    }
}

/**
 * Pure assembly of the Minecraft + Fabric launch command: merges vanilla and Fabric
 * argument templates, evaluates rules, and substitutes ${...} placeholders. No I/O.
 */
class LaunchBuilder(private val platform: Platform = Platform.current()) {

    fun build(
        mcVersion: String,
        assetsIndexId: String,
        fabricMainClass: String,
        vanillaArgs: Arguments,
        fabricArgs: Arguments,
        classpath: List<Path>,
        session: GameSession,
        gameDir: Path,
        assetsRoot: Path,
        nativesDir: Path,
        librariesDir: Path,
        maxMemoryMb: Int = 2048,
    ): LaunchCommand {
        val cp = classpath.joinToString(File.pathSeparator) { it.toString() }
        val jvmVars = mapOf(
            "natives_directory" to nativesDir.toString(),
            "launcher_name" to "Maeve",
            "launcher_version" to "0.0.1",
            "classpath" to cp,
            "classpath_separator" to File.pathSeparator,
            "library_directory" to librariesDir.toString(),
            "version_name" to mcVersion,
        )
        val gameVars = mapOf(
            "auth_player_name" to session.username,
            "version_name" to mcVersion,
            "game_directory" to gameDir.toString(),
            "assets_root" to assetsRoot.toString(),
            "assets_index_name" to assetsIndexId,
            "auth_uuid" to session.uuid,
            "auth_access_token" to session.accessToken,
            "auth_xuid" to "",
            "clientid" to "",
            "user_type" to session.userType,
            "version_type" to "release",
        )

        val jvm = ArrayList<String>()
        jvm += "-Xmx${maxMemoryMb}M"
        jvm += expandArgs(vanillaArgs.jvm, jvmVars)
        jvm += expandArgs(fabricArgs.jvm, jvmVars)

        val game = ArrayList<String>()
        game += expandArgs(vanillaArgs.game, gameVars)
        game += expandArgs(fabricArgs.game, gameVars)

        return LaunchCommand(jvm, fabricMainClass, game)
    }

    private fun expandArgs(elements: List<JsonElement>, vars: Map<String, String>): List<String> {
        val out = ArrayList<String>()
        for (el in elements) {
            when (el) {
                is JsonPrimitive -> if (el.isString) out += expand(el.content, vars)
                is JsonObject -> {
                    val rules = el["rules"]?.let { LauncherJson.decodeFromJsonElement<List<Rule>>(it) }
                    if (!RuleEval.allowed(rules, platform)) continue // no features enabled -> demo/resolution args drop
                    when (val v = el["value"]) {
                        is JsonPrimitive -> if (v.isString) out += expand(v.content, vars)
                        is JsonArray -> v.forEach { if (it is JsonPrimitive && it.isString) out += expand(it.content, vars) }
                        else -> {}
                    }
                }
                else -> {}
            }
        }
        // Defensive: drop tokens with unresolved placeholders, but make the gap visible
        // (Mojang occasionally adds new ${...} variables between versions).
        val (ok, dropped) = out.partition { !it.contains("\${") }
        if (dropped.isNotEmpty()) System.err.println("Maeve: dropped launch args with unresolved placeholders: $dropped")
        return ok
    }

    private fun expand(s: String, vars: Map<String, String>): String {
        var r = s
        for ((k, v) in vars) r = r.replace("\${$k}", v)
        return r
    }
}
