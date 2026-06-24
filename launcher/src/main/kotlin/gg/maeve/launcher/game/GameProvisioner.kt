package gg.maeve.launcher.game

import java.nio.file.Files
import java.nio.file.Path

/** Orchestrates all downloads needed to launch MC + Fabric + mods, returning the
 *  data the LaunchBuilder needs. Idempotent: re-runs skip already-present files. */
class GameProvisioner(private val net: Net, private val paths: MaevePaths) {

    data class Provisioned(
        val mcVersion: String,
        val assetsIndexId: String,
        val vanillaArgs: Arguments,
        val fabricArgs: Arguments,
        val fabricMainClass: String,
        val classpath: List<Path>,
        val instanceDir: Path,
        val nativesDir: Path,
    )

    suspend fun provision(
        mcVersion: String,
        loader: String,
        instanceId: String,
        localMaeveMod: Path?,
        onStatus: (String) -> Unit,
    ): Provisioned {
        paths.ensureBase()
        val mojang = MojangMeta(net)
        val fabric = FabricMeta(net)

        onStatus("Fetching version metadata…")
        val vj = mojang.version(mojang.manifest().url(mcVersion))

        onStatus("Downloading client…")
        net.download(vj.downloads.client.url, paths.clientJar(mcVersion), vj.downloads.client.sha1, vj.downloads.client.size)

        val plat = Platform.current()
        val resolved = LibraryResolver.resolve(vj.libraries, plat)
        onStatus("Downloading ${resolved.size} libraries…")
        resolved.forEach { net.download(it.url, paths.libraries.resolve(it.path), it.sha1, it.size) }

        onStatus("Downloading assets…")
        val assetIndex = mojang.assetIndex(vj.assetIndex)
        net.download(vj.assetIndex.url, paths.assetIndexes.resolve("${vj.assetIndex.id}.json"), vj.assetIndex.sha1, vj.assetIndex.size)
        AssetProvisioner(net, paths).provision(assetIndex, onStatus)

        onStatus("Installing Fabric $loader…")
        val fp = fabric.profile(mcVersion, loader)
        val fabricCp = fp.libraries.map { lib ->
            val (path, url) = fabric.resolve(lib)
            val dest = paths.libraries.resolve(path)
            net.download(url, dest)
            dest
        }

        onStatus("Downloading mods…")
        ModProvisioner(net, paths).provision(instanceId, mcVersion, localMaeveMod, onStatus)

        val instanceDir = paths.instance(instanceId).also { Files.createDirectories(it) }
        val nativesDir = paths.natives(instanceId).also { Files.createDirectories(it) }

        val classpath = buildList {
            add(paths.clientJar(mcVersion))
            resolved.forEach { add(paths.libraries.resolve(it.path)) }
            addAll(fabricCp)
        }
        return Provisioned(mcVersion, vj.assetIndex.id, vj.arguments, fp.arguments, fp.mainClass, classpath, instanceDir, nativesDir)
    }
}
