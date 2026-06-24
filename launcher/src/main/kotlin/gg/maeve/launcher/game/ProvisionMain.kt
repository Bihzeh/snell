package gg.maeve.launcher.game

import kotlinx.coroutines.runBlocking
import java.nio.file.Path
import java.util.Collections
import java.util.concurrent.TimeUnit

/**
 * Headless verification entrypoint (run via `./gradlew :launcher:provisionTest`):
 * provisions MC 26.2 + Fabric + mods into ~/.maeve-test, launches with the current
 * JDK (this box has no display, so it crashes at window creation), and reports
 * whether Fabric booted and discovered our mods before failing. Proves the whole
 * provision -> classpath -> Fabric -> mod-placement chain end to end.
 */
fun main() = runBlocking {
    val paths = MaevePaths(Path.of(System.getProperty("user.home"), ".maeve-test"))
    val modJar = Path.of("mod/build/libs/mod-0.0.1-SNAPSHOT.jar")
    val systemJava = Path.of(System.getProperty("java.home"), "bin", "java")
    val lines = Collections.synchronizedList(mutableListOf<String>())

    val proc = Launcher(paths).launch(
        session = GameSession.offline("MaeveDev"),
        localMaeveMod = modJar,
        java = systemJava,
        onStatus = { println("[status] $it") },
        onLog = { lines.add(it); println("[game] $it") },
    )

    proc.waitFor(180, TimeUnit.SECONDS)
    if (proc.isAlive) proc.destroyForcibly()

    val log = lines.joinToString("\n")
    println("\n===== VERIFICATION =====")
    listOf("Fabric", "Loading", "maeve", "sodium", "lithium", "Maeve initialized").forEach {
        println("marker '$it': ${log.contains(it, ignoreCase = true)}")
    }
}
