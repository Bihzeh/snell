package gg.maeve.mod.ui

import gg.maeve.mod.module.ModuleManager

/**
 * Drives the in-game mod menu: lists modules and toggles them. Screen rendering
 * (opening an actual MC Screen) is bound through the platform bridge in Phase 1;
 * this controller holds the menu's behavior so it stays testable.
 */
class ModMenuController(private val modules: ModuleManager) {
    data class Row(val id: String, val name: String, val enabled: Boolean)

    fun rows(): List<Row> = modules.all().map { Row(it.id, it.displayName, it.enabled) }

    fun onToggle(id: String) = modules.toggle(id)

    fun open() {
        // Phase 1: bridge opens a Screen that renders rows() and calls onToggle().
    }
}
