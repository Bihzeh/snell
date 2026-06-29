package gg.snell.mod.menu

/**
 * Pure data model for the bespoke Options screen. No Minecraft types — the screen maps these to/from
 * the real game options via the adapter.
 */

/** What kind of control an [OptionItem] renders as on the right of its row. */
enum class OptionKind { Toggle, Cycle, Slider }

/**
 * One settings row.
 *
 * @param id          stable identifier the screen maps to a game option
 * @param label       left-hand caption
 * @param kind        which control to draw on the right
 * @param valueText   human-readable current value (shown by Cycle + Slider)
 * @param on          Toggle state (ignored unless [kind] is [OptionKind.Toggle])
 * @param fraction    Slider position in 0..1 (ignored unless [kind] is [OptionKind.Slider])
 * @param description optional muted sub-line under the label
 */
data class OptionItem(
    val id: String,
    val label: String,
    val kind: OptionKind,
    val valueText: String,
    val on: Boolean = false,
    val fraction: Float = 0f,
    val description: String = "",
)

/**
 * A content entry in the Options panel: either a section header or an option row. The screen builds a
 * flat list of these from the live options for the active category, and the renderer/layout treat
 * every entry with the same row cadence (sections just draw a label).
 */
sealed interface OptionEntry {
    data class Section(val label: String) : OptionEntry
    data class Item(val item: OptionItem) : OptionEntry
}
