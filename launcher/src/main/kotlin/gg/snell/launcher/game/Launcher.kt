package gg.snell.launcher.game

import java.nio.file.Path

/** Top-level: provision everything, then start the game as a subprocess. */
class Launcher(private val paths: SnellPaths = SnellPaths.default()) {

    /**
     * Provisions and launches the game. [java] overrides the JVM used to run the
     * game (defaults to a downloaded Temurin 25). Returns the live [Process]; logs
     * are streamed to [onLog] on a daemon thread.
     */
    suspend fun launch(
        session: GameSession,
        mcVersion: String = "26.2",
        loader: String = "0.19.3",
        instanceId: String = "default",
        localSnellMod: Path? = null,
        java: Path? = null,
        maxMemoryMb: Int = 2048,
        enabledMods: Set<String>? = null,
        onStatus: (String) -> Unit = {},
        onLog: (String) -> Unit = {},
    ): Process {
        val net = Net()
        val provisioned: GameProvisioner.Provisioned
        val javaPath: Path
        try {
            provisioned = GameProvisioner(net, paths).provision(mcVersion, loader, instanceId, localSnellMod, enabledMods, onStatus)
            javaPath = java ?: JreProvisioner(net, paths).ensure()
        } finally {
            net.close()
        }

        onStatus("Launching…")
        val cmd = LaunchBuilder().build(
            mcVersion = provisioned.mcVersion,
            assetsIndexId = provisioned.assetsIndexId,
            fabricMainClass = provisioned.fabricMainClass,
            vanillaArgs = provisioned.vanillaArgs,
            fabricArgs = provisioned.fabricArgs,
            classpath = provisioned.classpath,
            session = session,
            gameDir = provisioned.instanceDir,
            assetsRoot = paths.assets,
            nativesDir = provisioned.nativesDir,
            librariesDir = paths.libraries,
            maxMemoryMb = maxMemoryMb,
        )
        val process = ProcessBuilder(cmd.fullCommand(javaPath.toString()))
            .directory(provisioned.instanceDir.toFile())
            .redirectErrorStream(true)
            .start()
        Thread {
            process.inputStream.bufferedReader().forEachLine(onLog)
        }.apply { isDaemon = true }.start()
        return process
    }
}
