package com.kaity.server.manager

import com.kaity.server.SillyPlugin
import com.kaity.server.util.Messenger
import net.kyori.adventure.text.Component
import net.megavex.scoreboardlibrary.api.sidebar.Sidebar
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Handles Tablist and Scoreboard (via Megavex ScoreboardLibrary for Folia compatibility).
 */
class DisplayManager(val plugin: SillyPlugin) : Listener {

    private val sidebars = mutableMapOf<UUID, Sidebar>()

    private val scoreboardEnabled = plugin.config.getBoolean("scoreboard.enabled", true)
    private val scoreboardInterval = plugin.config.getLong("scoreboard.update_interval", 20L)
    
    private val tablistEnabled = plugin.config.getBoolean("tablist.enabled", true)
    private val tablistInterval = plugin.config.getLong("tablist.update_interval", 20L)

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)

        // Async update tasks for Folia
        if (scoreboardEnabled) {
            val periodMs = scoreboardInterval * 50
            Bukkit.getAsyncScheduler().runAtFixedRate(plugin, { _ ->
                sidebars.forEach { (uuid, sidebar) ->
                    val player = Bukkit.getPlayer(uuid)
                    if (player != null && player.isOnline) {
                        updateBoard(player, sidebar)
                    }
                }
            }, periodMs, periodMs, TimeUnit.MILLISECONDS)
        }

        if (tablistEnabled) {
            val periodMs = tablistInterval * 50
            Bukkit.getAsyncScheduler().runAtFixedRate(plugin, { _ ->
                Bukkit.getOnlinePlayers().forEach { player ->
                    updateTablist(player)
                }
            }, periodMs, periodMs, TimeUnit.MILLISECONDS)
        }
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        if (scoreboardEnabled) {
            val sidebar = plugin.scoreboardLibrary.createSidebar()
            sidebar.addPlayer(player)
            sidebars[player.uniqueId] = sidebar
            updateBoard(player, sidebar)
        }
        if (tablistEnabled) {
            updateTablist(player)
        }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        // Essential cleanup to prevent memory leaks
        sidebars.remove(event.player.uniqueId)?.close()
    }

    private fun replacePlaceholders(player: Player, text: String): String {
        var parsed = text
            .replace("%player_name%", player.name)
            .replace("%player_ping%", player.ping.toString())
            .replace("%server_online%", Bukkit.getOnlinePlayers().size.toString())
            
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            parsed = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, parsed)
        }
        
        return parsed
    }

    private fun updateTablist(player: Player) {
        val headerStr = plugin.config.getString("tablist.header", "")!!
        val footerStr = plugin.config.getString("tablist.footer", "")!!
        
        val header = Messenger.parse(replacePlaceholders(player, headerStr))
        val footer = Messenger.parse(replacePlaceholders(player, footerStr))
        
        player.sendPlayerListHeaderAndFooter(header, footer)
    }

    /**
     * Updates the Scoreboard for a player.
     * Uses Adventure Components for full color/gradient support.
     */
    private fun updateBoard(player: Player, sidebar: Sidebar) {
        val titleStr = plugin.config.getString("scoreboard.title", "<gold><bold>Silly Server</bold></gold>")!!
        sidebar.title(Messenger.parse(replacePlaceholders(player, titleStr)))

        val lines = plugin.config.getStringList("scoreboard.lines")
        val formattedLines = lines.take(15).map { Messenger.parse(replacePlaceholders(player, it)) }
        
        for (i in 0 until 15) {
            if (i < formattedLines.size) {
                sidebar.line(i, formattedLines[i])
            } else {
                sidebar.line(i, null)
            }
        }
    }
}
