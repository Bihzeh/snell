package gg.snell.mod.menu

/**
 * One row in the singleplayer world picker — a plain data model so the picker can be unit-tested and
 * headlessly rendered without Minecraft's `LevelSummary`. The runtime screen maps these from the real
 * save list; the layout/renderer only ever see this shape.
 *
 * @param name   display name of the world
 * @param folder save-directory id (stable key for select / play / edit / delete)
 * @param mode   game mode shown as the row's mode pill (e.g. "Survival", "Creative", "Hardcore")
 * @param meta   primary meta line, e.g. "1.21 · 2 minutes ago"
 * @param detail muted mono detail line, e.g. the folder id or on-disk size
 */
data class WorldRow(
    val name: String,
    val folder: String,
    val mode: String,
    val meta: String,
    val detail: String,
)
