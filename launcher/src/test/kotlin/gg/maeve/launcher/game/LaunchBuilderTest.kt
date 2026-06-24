package gg.maeve.launcher.game

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LaunchBuilderTest {
    private val builder = LaunchBuilder(Platform("linux", "x64"))

    @Test fun `substitutes vars and drops feature-gated args`() {
        // "\${x}" is a literal placeholder string (regular Kotlin escape, not a template).
        val vanilla = Arguments(
            jvm = listOf(
                JsonPrimitive("-cp"),
                JsonPrimitive("\${classpath}"),
                JsonPrimitive("-Djava.library.path=\${natives_directory}"),
            ),
            game = listOf(
                JsonPrimitive("--username"), JsonPrimitive("\${auth_player_name}"),
                JsonPrimitive("--uuid"), JsonPrimitive("\${auth_uuid}"),
                buildJsonObject {
                    putJsonArray("rules") {
                        add(buildJsonObject {
                            put("action", "allow")
                            putJsonObject("features") { put("is_demo_user", true) }
                        })
                    }
                    put("value", "--demo")
                },
            ),
        )
        val fabric = Arguments(jvm = buildJsonArray { }.toList(), game = buildJsonArray { }.toList())

        val cmd = builder.build(
            mcVersion = "26.2",
            assetsIndexId = "32",
            fabricMainClass = "net.fabricmc.loader.impl.launch.knot.KnotClient",
            vanillaArgs = vanilla,
            fabricArgs = fabric,
            classpath = listOf(Path.of("/libs/a.jar"), Path.of("/libs/b.jar")),
            session = GameSession.offline("Tester"),
            gameDir = Path.of("/inst"),
            assetsRoot = Path.of("/assets"),
            nativesDir = Path.of("/inst/natives"),
            librariesDir = Path.of("/libraries"),
        )

        assertEquals("net.fabricmc.loader.impl.launch.knot.KnotClient", cmd.mainClass)
        assertTrue(cmd.jvmArgs.any { it == "/libs/a.jar:/libs/b.jar" }, "classpath substituted: ${cmd.jvmArgs}")
        assertTrue(cmd.jvmArgs.any { it == "-Djava.library.path=/inst/natives" })
        assertTrue(cmd.gameArgs.containsAll(listOf("--username", "Tester", "--uuid")))
        assertFalse(cmd.gameArgs.contains("--demo"), "demo (feature-gated) must be dropped")
        assertTrue(cmd.jvmArgs.none { it.contains("\${") }, "no unresolved placeholders")
    }
}
