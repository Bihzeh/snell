package gg.snell.mod.module

import gg.snell.mod.config.Config
import java.util.Collections

/**
 * Owns the registry of modules and bridges them to the config layer.
 * Registration order is the HUD draw order.
 *
 * Threading: all access happens on the Minecraft client main thread (module
 * registration at init, toggles from input/tick, HUD reads from the GUI extract
 * phase — all the same thread), so no synchronization is required.
 */
class ModuleManager(private val config: Config) {
    private val modules = LinkedHashMap<String, Module>()
    private val hudList = ArrayList<HudModule>() // cached so the render hot path allocates nothing
    private val hudView: List<HudModule> = Collections.unmodifiableList(hudList) // read-only view, zero per-call alloc

    /** Fired after any enabled toggle (any path) so the bridge can apply side effects (e.g. font reload). */
    var onEnabledChanged: ((id: String, enabled: Boolean) -> Unit)? = null

    fun register(module: Module) {
        require(!modules.containsKey(module.id)) { "Duplicate module id: ${module.id}" }
        modules[module.id] = module
        if (module is HudModule) hudList.add(module)
        config.applyTo(module)   // restore persisted state
        module.onRegister()
    }

    fun all(): Collection<Module> = modules.values

    /** Returns the cached HUD list directly (read-only use): no per-frame allocation. */
    fun hudModules(): List<HudModule> = hudView

    fun byId(id: String): Module? = modules[id]
    fun hudById(id: String): HudModule? = modules[id] as? HudModule

    fun toggle(id: String) {
        modules[id]?.let {
            it.enabled = !it.enabled
            config.snapshot(modules.values)
            config.save()
            onEnabledChanged?.invoke(id, it.enabled)
        }
    }

    // --- editor write-through setters (mutate-only; the editor persists once via saveAll) ---

    fun setEnabled(id: String, value: Boolean) { modules[id]?.let { it.enabled = value; onEnabledChanged?.invoke(id, value) } }

    fun setAnchorOffset(id: String, anchor: HudAnchor, offsetX: Int, offsetY: Int) {
        (modules[id] as? HudModule)?.let { it.anchor = anchor; it.offsetX = offsetX; it.offsetY = offsetY }
    }

    fun updateStyle(id: String, transform: (HudStyle) -> HudStyle) {
        (modules[id] as? HudModule)?.let { it.style = transform(it.style) }
    }

    fun setOption(id: String, key: String, value: Boolean) {
        (modules[id] as? HudModule)?.setOption(key, value)
    }

    fun setTargetColor(id: String, key: String, value: Int) {
        (modules[id] as? HudModule)?.setTargetColor(key, value)
    }

    fun resetStyle(id: String) {
        (modules[id] as? HudModule)?.let { it.style = it.defaultStyle }
    }

    /** Editor-global snap-to-guides preference (persisted via [config]). */
    fun snapEnabled(): Boolean = config.isSnapEnabled()
    fun setSnapEnabled(value: Boolean) = config.setSnapEnabled(value)

    fun saveAll() {
        config.snapshot(modules.values)
        config.save()
    }
}
