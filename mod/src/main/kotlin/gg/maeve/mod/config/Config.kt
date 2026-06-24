package gg.maeve.mod.config

import gg.maeve.mod.module.HudModule
import gg.maeve.mod.module.Module
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

@Serializable
private data class ModuleState(
    val enabled: Boolean,
    val x: Int = 0,
    val y: Int = 0,
)

@Serializable
private data class ConfigData(
    val schema: Int = 1,
    val modules: MutableMap<String, ModuleState> = mutableMapOf(),
)

/**
 * Schema-versioned per-module config persisted as JSON under the given dir
 * (at runtime: .minecraft/config/maeve/config.json).
 */
class Config(private val dir: Path) {
    private val file: Path = dir.resolve("config.json")
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private var data: ConfigData = ConfigData()

    fun load() {
        if (file.exists()) {
            runCatching { data = json.decodeFromString<ConfigData>(file.readText()) }
        }
    }

    /** Restore persisted state onto a freshly registered module. */
    fun applyTo(module: Module) {
        val s = data.modules[module.id] ?: return
        module.enabled = s.enabled
        if (module is HudModule) { module.x = s.x; module.y = s.y }
    }

    /** Capture the current state of all modules into memory. */
    fun snapshot(modules: Collection<Module>) {
        modules.forEach { m ->
            data.modules[m.id] = if (m is HudModule) {
                ModuleState(m.enabled, m.x, m.y)
            } else {
                ModuleState(m.enabled)
            }
        }
    }

    fun save() {
        Files.createDirectories(dir)
        file.writeText(json.encodeToString(data))
    }
}
