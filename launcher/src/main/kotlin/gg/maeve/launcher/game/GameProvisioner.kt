package gg.maeve.launcher.game

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.nio.file.Files
import java.nio.file.Path

/** Orchestrates all downloads needed to launch MC + Fabric + mods. Idempotent:
 *  re-runs skip already-present files. Library jars download in parallel. */
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
        enabledMods: Set<String>? = null,
        onStatus: (String) -> Unit,
    ): Provisioned {
        paths.ensureBase()
        val mojang = MojangMeta(net)
        val fabric = FabricMeta(net)

        onStatus("Fetching version metadata…")
        val vj = mojang.version(mojang.manifest().url(mcVersion))

        onStatus("Downloading client…")
        net.download(vj.downloads.client.url, paths.clientJar(mcVersion), sha1 = vj.downloads.client.sha1, size = vj.downloads.client.size)

        val plat = Platform.current()
        val resolved = LibraryResolver.resolve(vj.libraries, plat)
        onStatus("Downloading ${resolved.size} libraries…")
        parallel(resolved) { net.download(it.url, paths.safeLibrary(it.path), sha1 = it.sha1, size = it.size) }

        onStatus("Downloading assets…")
        val assetIndex = mojang.assetIndex(vj.assetIndex)
        net.download(vj.assetIndex.url, paths.assetIndexes.resolve("${vj.assetIndex.id}.json"), sha1 = vj.assetIndex.sha1, size = vj.assetIndex.size)
        AssetProvisioner(net, paths).provision(assetIndex, onStatus)

        onStatus("Installing Fabric $loader…")
        val fp = fabric.profile(mcVersion, loader)
        val fabricCp = fp.libraries.map { paths.safeLibrary(fabric.resolve(it).first) }
        // Fabric profile JSON carries no hashes; verify each jar against its Maven .sha1
        // sidecar when available (closes the classpath supply-chain gap).
        parallel(fp.libraries) { lib ->
            val (path, url) = fabric.resolve(lib)
            val sha1 = runCatching { net.text("$url.sha1").trim().substringBefore(' ').lowercase() }
                .getOrNull()?.takeIf { it.matches(Regex("[0-9a-f]{40}")) }
            net.download(url, paths.safeLibrary(path), sha1 = sha1)
        }

        onStatus("Downloading mods…")
        ModProvisioner(net, paths).provision(instanceId, mcVersion, localMaeveMod, enabledMods, onStatus)

        val instanceDir = paths.instance(instanceId).also { Files.createDirectories(it) }
        val nativesDir = paths.natives(instanceId).also { Files.createDirectories(it) }

        val classpath = buildList {
            add(paths.clientJar(mcVersion))
            resolved.forEach { add(paths.safeLibrary(it.path)) }
            addAll(fabricCp)
        }
        return Provisioned(mcVersion, vj.assetIndex.id, vj.arguments, fp.arguments, fp.mainClass, classpath, instanceDir, nativesDir)
    }

    private suspend fun <T> parallel(items: List<T>, concurrency: Int = 16, action: suspend (T) -> Unit) = coroutineScope {
        val sem = Semaphore(concurrency)
        items.map { item -> async(Dispatchers.IO) { sem.withPermit { action(item) } } }.awaitAll()
    }
}
