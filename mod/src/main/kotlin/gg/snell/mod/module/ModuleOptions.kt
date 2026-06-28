package gg.snell.mod.module

/** A user-customizable, module-specific option surfaced in the editor (beyond generic style). */
sealed interface ModuleOption {
    val key: String
    val label: String
}

/** A boolean option (rendered as a switch). */
data class ToggleOption(override val key: String, override val label: String, val default: Boolean) : ModuleOption

/** An ARGB colour option (editable via the shared picker as a colour target). */
data class ColorOption(override val key: String, override val label: String, val default: Int) : ModuleOption

/** A selectable colour the editor's HSV picker can edit (key + display label). */
data class ColorTarget(val key: String, val label: String)

/** Reusable typed backing store for a module's [ModuleOption]s. Pure (no Minecraft types). */
class ModuleOptions(val options: List<ModuleOption>) {
    private val bools = mutableMapOf<String, Boolean>()
    private val colors = mutableMapOf<String, Int>()

    val toggles: List<ToggleOption> get() = options.filterIsInstance<ToggleOption>()
    val colorOptions: List<ColorOption> get() = options.filterIsInstance<ColorOption>()

    fun bool(key: String): Boolean = bools[key] ?: (options.firstOrNull { it.key == key } as? ToggleOption)?.default ?: false
    fun setBool(key: String, value: Boolean) { bools[key] = value }

    fun color(key: String): Int = colors[key] ?: (options.firstOrNull { it.key == key } as? ColorOption)?.default ?: 0
    fun setColor(key: String, value: Int) { colors[key] = value }
}
