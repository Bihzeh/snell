package gg.snell.launcher.game

import kotlinx.coroutines.runBlocking
import java.nio.file.Path
import java.util.Collections
import java.util.concurrent.TimeUnit

/**
 * Headless verification entrypoint (run via `./gradlew :launcher:provisionTest`):
 * provisions MC 26.2 + Fabric + mods into ~/.snell-test, launches with the current
 * JDK (this box has no display, so it crashes at window creation), and reports
 * whether Fabric booted and discovered our mods before failing. Proves the whole
 * provision -> classpath -> Fabric -> mod-placement chain end to end.
 */
fun main() = runBlocking {
    val paths = SnellPaths(Path.of(System.getProperty("user.home"), ".snell-test"))
    // Prefer a freshly built mod jar (version-agnostic) when present; otherwise pass
    // null so the chain exercises the bundled-resource path that shipped launchers use.
    val modJar = findDevModJar(Path.of("mod/build/libs"))
    val systemJava = Path.of(System.getProperty("java.home"), "bin", "java")
    val lines = Collections.synchronizedList(mutableListOf<String>())

    val proc = Launcher(paths).launch(
        session = GameSession.offline("SnellDev"),
        localSnellMod = modJar,
        java = systemJava,
        onStatus = { println("[status] $it") },
        onLog = { lines.add(it); println("[game] $it") },
    )

    proc.waitFor(180, TimeUnit.SECONDS)
    if (proc.isAlive) proc.destroyForcibly()

    val log = lines.joinToString("\n")
    println("\n===== VERIFICATION =====")
    listOf("Fabric", "Loading", "snell", "sodium", "lithium", "Snell initialized").forEach {
        println("marker '$it': ${log.contains(it, ignoreCase = true)}")
    }
}
