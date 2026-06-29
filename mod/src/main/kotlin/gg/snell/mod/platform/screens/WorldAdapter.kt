package gg.snell.mod.platform.screens

import gg.snell.mod.menu.WorldRow
import it.unimi.dsi.fastutil.booleans.BooleanConsumer
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.ConfirmScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen
import net.minecraft.client.gui.screens.worldselection.EditWorldScreen
import net.minecraft.network.chat.Component
import net.minecraft.world.level.storage.LevelSummary

/**
 * The seam between the bespoke world picker and vanilla's singleplayer model/flows. Custom view,
 * vanilla actions: enumeration goes through [LevelSummary], and play/create/edit/delete delegate to
 * the exact calls vanilla's SelectWorldScreen makes (no save logic reimplemented). All names verified
 * against the 26.1.2 named jar.
 */
object WorldAdapter {

    /** Load the save list off-thread and deliver mapped rows on the main thread. */
    fun load(mc: Minecraft, onRows: (List<WorldRow>) -> Unit) {
        try {
            val src = mc.levelSource
            val candidates = src.findLevelCandidates()
            src.loadLevelSummaries(candidates).thenAccept { summaries ->
                val rows = summaries.filter { !it.isDisabled }.map { toRow(it) }
                mc.execute { onRows(rows) }
            }
        } catch (e: Exception) {
            mc.execute { onRows(emptyList()) }
        }
    }

    private fun toRow(s: LevelSummary): WorldRow {
        val mode = if (s.isHardcore) "Hardcore" else s.gameMode.shortDisplayName.string
        val meta = "${s.worldVersionName.string} · ${relative(s.lastPlayed)}"
        return WorldRow(s.levelName, s.levelId, mode, meta, s.levelId)
    }

    private fun relative(t: Long): String {
        if (t <= 0L) return "never played"
        val d = System.currentTimeMillis() - t
        return when {
            d < 60_000L -> "just now"
            d < 3_600_000L -> "${d / 60_000L} min ago"
            d < 86_400_000L -> "${d / 3_600_000L} hr ago"
            else -> "${d / 86_400_000L} days ago"
        }
    }

    /** Load + enter the world (the same call vanilla's "Play" makes). */
    fun play(mc: Minecraft, folder: String, onFail: () -> Unit) {
        try {
            mc.createWorldOpenFlows().openWorld(folder) { onFail() }
        } catch (e: Exception) {
            onFail()
        }
    }

    /** Open vanilla's create-world flow; [returnTo] is re-shown on cancel. */
    fun create(mc: Minecraft, returnTo: Screen) {
        CreateWorldScreen.openFresh(mc) { mc.setScreenAndShow(returnTo) }
    }

    /** Open vanilla's edit-world screen for [folder]; [returnTo] is re-shown when it closes. */
    fun edit(mc: Minecraft, folder: String, returnTo: Screen) {
        try {
            val access = mc.levelSource.validateAndCreateAccess(folder)
            mc.setScreenAndShow(EditWorldScreen.create(mc, access, BooleanConsumer { mc.setScreenAndShow(returnTo) }))
        } catch (e: Exception) {
            mc.setScreenAndShow(returnTo)
        }
    }

    /** Confirm + delete [folder] (vanilla's deleteLevel), then re-show [returnTo]. */
    fun delete(mc: Minecraft, folder: String, displayName: String, returnTo: Screen) {
        val confirm = ConfirmScreen(
            BooleanConsumer { yes ->
                if (yes) {
                    try {
                        val access = mc.levelSource.createAccess(folder)
                        access.use { it.deleteLevel() }
                    } catch (e: Exception) { /* vanilla would surface a toast; ignore here */ }
                }
                mc.setScreenAndShow(returnTo)
            },
            Component.literal("Delete \"$displayName\"?"),
            Component.literal("This world will be lost forever! (A long time!)"),
        )
        mc.setScreenAndShow(confirm)
    }
}
