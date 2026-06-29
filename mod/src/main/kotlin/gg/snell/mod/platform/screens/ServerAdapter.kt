package gg.snell.mod.platform.screens

import gg.snell.mod.menu.ServerRow
import gg.snell.mod.menu.ServerStatus
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.ConnectScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.multiplayer.ServerData
import net.minecraft.client.multiplayer.ServerList
import net.minecraft.client.multiplayer.ServerStatusPinger
import net.minecraft.client.multiplayer.resolver.ServerAddress
import net.minecraft.server.network.EventLoopGroupHolder

/**
 * The seam between the bespoke server picker and vanilla's multiplayer model. Custom view, vanilla
 * actions: the list comes from [ServerList], live status from [ServerStatusPinger], and join delegates
 * to [ConnectScreen.startConnecting] — the same call vanilla's JoinMultiplayerScreen makes. Add / Edit
 * / Direct Connect have no standalone screens in 26.1.2 (they live inside JoinMultiplayerScreen), so
 * the screen hands those off to vanilla's list via a one-shot swap bypass.
 */
class ServerAdapter(private val mc: Minecraft) {
    private val servers = ServerList(mc).apply { load() }
    private val pinger = ServerStatusPinger()
    private var pinged = false

    fun count(): Int = servers.size()

    fun rows(): List<ServerRow> = (0 until servers.size()).map { toRow(servers.get(it)) }

    /** Kick off an async ping of every server (status fields mutate in place; [tick] advances them). */
    fun pingAll() {
        for (i in 0 until servers.size()) {
            try {
                pinger.pingServer(servers.get(i), Runnable {}, Runnable {}, EventLoopGroupHolder.remote(false))
            } catch (e: Exception) { /* unreachable host; the row shows offline */ }
        }
        pinged = true
    }

    fun tick() { if (pinged) pinger.tick() }

    fun dispose() { try { pinger.removeAll() } catch (e: Exception) {} }

    /** Connect to server [index], returning to [parent] on disconnect (vanilla's join path). */
    fun join(parent: Screen, index: Int) {
        if (index !in 0 until servers.size()) return
        val data = servers.get(index)
        val address = ServerAddress.parseString(data.ip)
        ConnectScreen.startConnecting(parent, mc, address, data, true, null)
    }

    private fun toRow(d: ServerData): ServerRow {
        val status = when {
            !pinged || d.ping == -1L -> ServerStatus.Pinging
            d.ping < 0L -> ServerStatus.Offline
            else -> ServerStatus.Online
        }
        val players = d.status?.string?.trim().orEmpty()
        val motd = d.motd?.string?.trim().orEmpty()
        return ServerRow(d.name, d.ip, motd, players, d.ping.toInt(), status)
    }
}
