package gg.snell.mod.platform

import gg.snell.mod.module.ModuleManager

/**
 * Runtime switches + shared refs for the bespoke in-game menu overhaul, read by the screen-swap
 * (which can't easily reach the [gg.snell.mod.config.Config] / module instances). Set from [SnellMod].
 */
object SnellMenus {
    /** When true, the swap replaces vanilla menus with the Snell screens. Mirrors the config flag. */
    @Volatile
    var enabled: Boolean = true

    /** Skip the very next menu swap once (used when handing a flow back to a vanilla screen, e.g.
     *  the server Add/Edit/Direct dialogs that only live inside vanilla's JoinMultiplayerScreen). */
    @Volatile
    var bypassNext: Boolean = false

    /** Set once at init so bespoke screens (the Options "Mods" tab) can reach the live module list. */
    @Volatile
    var modules: ModuleManager? = null

    /** Client version shown in menu footers. */
    const val VERSION: String = "26.2"
}
