package gg.maeve.launcher.game

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.atomic.AtomicInteger

/** Downloads the asset objects referenced by an asset index, in parallel. */
class AssetProvisioner(private val net: Net, private val paths: MaevePaths) {

    suspend fun provision(index: AssetIndex, onStatus: (String) -> Unit) = coroutineScope {
        val sem = Semaphore(CONCURRENCY)
        val total = index.objects.size
        val done = AtomicInteger(0)
        index.objects.values.map { obj ->
            async(Dispatchers.IO) {
                sem.withPermit {
                    val sub = obj.hash.substring(0, 2)
                    val dest = paths.assetObjects.resolve(sub).resolve(obj.hash)
                    net.download("${MojangMeta.RESOURCES}/$sub/${obj.hash}", dest, sha1 = obj.hash, size = obj.size)
                    val n = done.incrementAndGet()
                    if (n % 250 == 0) onStatus("Assets $n/$total")
                }
            }
        }.awaitAll()
        onStatus("Assets $total/$total")
    }

    private companion object { const val CONCURRENCY = 24 }
}
