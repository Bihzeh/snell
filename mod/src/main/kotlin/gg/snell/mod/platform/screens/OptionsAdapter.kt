package gg.snell.mod.platform.screens

import gg.snell.mod.menu.OptionEntry
import gg.snell.mod.menu.OptionItem
import gg.snell.mod.menu.OptionKind
import gg.snell.mod.module.ModuleManager
import net.minecraft.client.GraphicsPreset
import net.minecraft.client.OptionInstance
import net.minecraft.client.Options
import net.minecraft.client.resources.language.I18n
import net.minecraft.sounds.SoundSource
import kotlin.math.roundToInt

/**
 * The seam between the bespoke Options screen and the live game [Options]. Custom view, vanilla model:
 * every value is read from / written to the real [OptionInstance]s (and [ModuleManager] for the Mods
 * tab); nothing is reimplemented. Display strings come from the option's own vanilla widget message
 * (`createButton(...).message`) so they always match the game exactly. Verified against 26.1.2.
 */
object OptionsAdapter {

    /** Slider display ranges (the fill fraction); values still read/write through the real option. */
    private val RANGES = mapOf("rd" to (2.0 to 32.0), "fps" to (10.0 to 260.0), "fov" to (30.0 to 110.0))
    private val AUDIO = listOf(
        Triple("master", "Master Volume", SoundSource.MASTER),
        Triple("music", "Music", SoundSource.MUSIC),
        Triple("blocks", "Blocks", SoundSource.BLOCKS),
        Triple("hostile", "Hostile Creatures", SoundSource.HOSTILE),
        Triple("players", "Players", SoundSource.PLAYERS),
        Triple("ambient", "Ambient / Environment", SoundSource.AMBIENT),
    )

    fun entries(o: Options, modules: ModuleManager?, category: String): List<OptionEntry> = when (category) {
        "video" -> listOf(
            OptionEntry.Section("Rendering"),
            slider("rd", "Render Distance", o, o.renderDistance(), "Chunks loaded around you"),
            cycle("gfx", "Graphics", o, o.graphicsPreset(), "Detail and visual quality"),
            slider("fps", "Max Framerate", o, o.framerateLimit(), "Upper FPS limit"),
            slider("bright", "Brightness", o, o.gamma(), "Gamma adjustment"),
            OptionEntry.Section("Display"),
            toggle("fs", "Fullscreen", o.fullscreen(), "Borderless on this display"),
            toggle("vsync", "VSync", o.enableVsync(), "Cap to refresh rate"),
            toggle("smooth", "Smooth Lighting", o.ambientOcclusion(), "Ambient occlusion"),
        )
        "controls" -> buildList {
            add(OptionEntry.Section("Mouse"))
            add(slider("sens", "Sensitivity", o, o.sensitivity(), ""))
            add(slider("fov", "FOV", o, o.fov(), ""))
            add(toggle("invert", "Invert Y-Axis", o.invertMouseY(), ""))
            add(OptionEntry.Section("Key Bindings"))
            for (km in o.keyMappings) {
                add(OptionEntry.Item(OptionItem("key:${km.name}", I18n.get(km.name), OptionKind.Cycle, km.translatedKeyMessage.string)))
            }
        }
        "audio" -> buildList {
            add(OptionEntry.Section("Volume"))
            for ((id, label, src) in AUDIO) add(slider("vol:$id", label, o, o.getSoundSourceOptionInstance(src), ""))
        }
        "mods" -> buildList {
            val mods = modules?.all()?.toList().orEmpty()
            add(OptionEntry.Section("Installed · ${mods.size}"))
            for (m in mods) add(OptionEntry.Item(OptionItem("mod:${m.id}", m.displayName, OptionKind.Toggle, "", on = m.enabled)))
        }
        else -> emptyList()
    }

    // ---- builders ------------------------------------------------------------------------------

    private fun toggle(id: String, label: String, opt: OptionInstance<Boolean>, desc: String) =
        OptionEntry.Item(OptionItem(id, label, OptionKind.Toggle, "", on = opt.get(), description = desc))

    private fun cycle(id: String, label: String, o: Options, opt: OptionInstance<*>, desc: String) =
        OptionEntry.Item(OptionItem(id, label, OptionKind.Cycle, valueText(o, opt), description = desc))

    private fun slider(id: String, label: String, o: Options, opt: OptionInstance<*>, desc: String): OptionEntry.Item {
        val (min, max) = RANGES[id] ?: (0.0 to 1.0)
        val v = (opt.get() as Number).toDouble()
        val frac = (if (max > min) (v - min) / (max - min) else 0.0).toFloat()
        return OptionEntry.Item(OptionItem(id, label, OptionKind.Slider, valueText(o, opt), fraction = frac, description = desc))
    }

    /** The exact vanilla value text ("16 chunks", "Fancy", "100%") from the option's own widget. */
    private fun valueText(o: Options, opt: OptionInstance<*>): String =
        try {
            opt.createButton(o).message.string.substringAfterLast(": ")
        } catch (e: Exception) {
            opt.get().toString()
        }

    // ---- mutations (write + persist through the live option) ------------------------------------

    fun toggle(o: Options, modules: ModuleManager?, id: String) {
        if (id.startsWith("mod:")) { modules?.toggle(id.removePrefix("mod:")); return }
        val opt = boolOpt(o, id) ?: return
        opt.set(!opt.get())
        o.save()
    }

    private fun boolOpt(o: Options, id: String): OptionInstance<Boolean>? = when (id) {
        "fs" -> o.fullscreen(); "vsync" -> o.enableVsync(); "smooth" -> o.ambientOcclusion(); "invert" -> o.invertMouseY(); else -> null
    }

    fun cycle(o: Options, id: String) {
        if (id == "gfx") {
            val presets = GraphicsPreset.values().filter { it != GraphicsPreset.CUSTOM }
            val cur = o.graphicsPreset().get()
            val next = presets[(presets.indexOf(cur).coerceAtLeast(0) + 1) % presets.size]
            o.graphicsPreset().set(next)
            o.save()
        }
        // "key:*" rebinding is deferred (display-only for now).
    }

    fun setSlider(o: Options, id: String, fraction: Float) {
        val f = fraction.coerceIn(0f, 1f)
        when {
            id == "rd" -> setInt(o.renderDistance(), 2, 32, f)
            id == "fps" -> setInt(o.framerateLimit(), 10, 260, f)
            id == "fov" -> setInt(o.fov(), 30, 110, f)
            id == "bright" -> o.gamma().set(f.toDouble())
            id == "sens" -> o.sensitivity().set(f.toDouble())
            id.startsWith("vol:") -> audioSource(id.removePrefix("vol:"))?.let { o.getSoundSourceOptionInstance(it).set(f.toDouble()) }
        }
        o.save()
    }

    private fun setInt(opt: OptionInstance<Int>, min: Int, max: Int, f: Float) {
        opt.set((min + (f * (max - min)).roundToInt()).coerceIn(min, max))
    }

    private fun audioSource(id: String): SoundSource? = AUDIO.firstOrNull { it.first == id }?.third
}
