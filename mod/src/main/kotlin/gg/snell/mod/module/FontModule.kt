package gg.snell.mod.module

/**
 * Toggles Snell's bundled Geist resource pack (a built-in pack overriding the game's default
 * font). Pure state only — the bridge applies the actual pack enable/disable + resource reload
 * when this module is toggled (and on startup). Persisted by the config layer like any module.
 */
class FontModule : Module {
    override val id = "font"
    override val displayName = "Custom Font (Poppins)"
    override var enabled = true // brand font on by default
}
