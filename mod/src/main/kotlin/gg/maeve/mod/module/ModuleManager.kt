package gg.maeve.mod.module

import gg.maeve.mod.config.Config

/**
 * Owns the registry of modules and bridges them to the config layer.
 * Registration order is the HUD draw order.
 */
class ModuleManager(private val config: Config) {
    private val modules = LinkedHashMap<String, Module>()

    fun register(module: Module) {
        require(!modules.containsKey(module.id)) { "Duplicate module id: ${module.id}" }
        modules[module.id] = module
        config.applyTo(module)   // restore persisted state
        module.onRegister()
    }

    fun all(): Collection<Module> = modules.values
    fun hudModules(): List<HudModule> = modules.values.filterIsInstance<HudModule>()

    fun toggle(id: String) {
        modules[id]?.let {
            it.enabled = !it.enabled
            config.snapshot(modules.values)
            config.save()
        }
    }

    fun saveAll() {
        config.snapshot(modules.values)
        config.save()
    }
}
