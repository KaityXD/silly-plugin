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

    private val sidebars = java.util.concurrent.ConcurrentHashMap<java.util.UUID, net.megavex.scoreboardlibrary.api.sidebar.Sidebar>()

    private val scoreboardEnabled = plugin.config.getBoolean("scoreboard.enabled", true)
    private val scoreboardInterval = plugin.config.getLong("scoreboard.update_interval", 20L)
    
    private val tablistEnabled = plugin.config.getBoolean("tablist.enabled", true)
    private val tablistInterval = plugin.config.getLong("tablist.update_interval", 1L)

    @Volatile
    private var cachedTps = "20.00"

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)

        // Cache TPS from global region since getTPS() can't be called async in Folia
        plugin.server.globalRegionScheduler.runAtFixedRate(plugin, { _ ->
            val tps = try { Bukkit.getTPS()[0] } catch (_: Exception) { 20.0 }
            cachedTps = String.format("%.2f", tps)
        }, 20, 20 * 5)

        // Async update tasks for Folia
        if (scoreboardEnabled) {
            val periodMs = scoreboardInterval * 50
            Bukkit.getAsyncScheduler().runAtFixedRate(plugin, { _ ->
                sidebars.forEach { (uuid, sidebar) ->
                    val player = Bukkit.getPlayer(uuid)
                    if (player != null && player.isOnline) {
                        player.scheduler.run(plugin, { _ ->
                            if (player.isOnline) updateBoard(player, sidebar)
                        }, null)
                    }
                }
            }, periodMs, periodMs, TimeUnit.MILLISECONDS)
        }

        if (tablistEnabled) {
            val periodMs = tablistInterval * 50
            Bukkit.getAsyncScheduler().runAtFixedRate(plugin, { _ ->
                Bukkit.getOnlinePlayers().forEach { player ->
                    player.scheduler.run(plugin, { _ ->
                        if (player.isOnline) updateTablist(player)
                    }, null)
                }
            }, periodMs, periodMs, TimeUnit.MILLISECONDS)
        }
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        
        // Handle Join Message
        event.joinMessage(null) // Disable default
        val hasPlayedBefore = player.hasPlayedBefore()
        val path = if (hasPlayedBefore) "messages.join" else "messages.first_join"
        val messageStr = plugin.config.getString(path)
        if (!messageStr.isNullOrEmpty()) {
            val component = Messenger.parse(replacePlaceholders(player, messageStr))
            plugin.server.broadcast(component)
        }
        
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
        // Handle Quit Message
        event.quitMessage(null) // Disable default
        val messageStr = plugin.config.getString("messages.quit")
        if (!messageStr.isNullOrEmpty()) {
            val component = Messenger.parse(replacePlaceholders(event.player, messageStr))
            plugin.server.broadcast(component)
        }
        
        // Essential cleanup to prevent memory leaks
        sidebars.remove(event.player.uniqueId)?.close()
    }

    private fun formatTimeMillis(millis: Long): String {
        val seconds = millis / 1000
        val days = seconds / 86400
        val hours = (seconds % 86400) / 3600
        val minutes = (seconds % 3600) / 60
        val parts = mutableListOf<String>()
        if (days > 0) parts.add("${days}d")
        if (hours > 0) parts.add("${hours}h")
        if (minutes > 0) parts.add("${minutes}m")
        if (parts.isEmpty()) return "${seconds}s"
        return parts.joinToString(" ")
    }

    private fun formatWorldTime(time: Long): String {
        var hours = (time / 1000 + 6) % 24
        val minutes = (time % 1000) * 60 / 1000
        val ampm = if (hours >= 12) "PM" else "AM"
        hours = if (hours % 12 == 0L) 12 else hours % 12
        return String.format("%02d:%02d %s", hours, minutes, ampm)
    }

    private fun replacePlaceholders(player: Player, text: String): String {
        val rawPrefix = plugin.permissionManager.getPrefix(player.uniqueId)
        val rawSuffix = plugin.permissionManager.getSuffix(player.uniqueId)
        
        val prefix = if (rawPrefix.isNotEmpty()) "$rawPrefix " else ""
        val suffix = if (rawSuffix.isNotEmpty()) " $rawSuffix" else ""

        val user = plugin.userManager.getUser(player)
        val playtimeMs = if (user != null) user.playtime + (System.currentTimeMillis() - user.lastJoin) else 0L
        val kills = user?.kills ?: 0
        val deaths = user?.deaths ?: 0
        
        val group = plugin.userManager.database.getUserGroups(player.uniqueId).firstOrNull()?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } ?: "Default"

        val ping = player.ping
        val statusPing = when {
            ping < 50 -> "<green>Excellent</green>"
            ping < 100 -> "<yellow>Good</yellow>"
            ping < 150 -> "<gold>Fair</gold>"
            else -> "<red>Poor</red>"
        }

        val afkStatus = if (plugin.afkManager.isAFK(player)) "<gray>[AFK]</gray> " else ""

        val runtime = Runtime.getRuntime()
        val ramUsed = (runtime.totalMemory() - runtime.freeMemory()) / 1048576L
        val ramMax = runtime.maxMemory() / 1048576L
        val uptimeMs = java.lang.management.ManagementFactory.getRuntimeMXBean().uptime

        var parsed = text
            // Player Core
            .replace("%player_name%", player.name)
            .replace("%player_ping%", ping.toString())
            .replace("%prefix%", prefix)
            .replace("%suffix%", suffix)
            // Stats & User
            .replace("%playtime_formatted%", formatTimeMillis(playtimeMs))
            .replace("%player_group%", group)
            .replace("%player_kills%", kills.toString())
            .replace("%player_deaths%", deaths.toString())
            // World Info
            .replace("%world_name%", player.world.name)
            .replace("%world_time%", formatWorldTime(player.world.time))
            .replace("%player_xyz%", "X: ${player.location.blockX}, Y: ${player.location.blockY}, Z: ${player.location.blockZ}")
            .replace("%player_biome%", player.location.block.biome.name().replace("_", " ").lowercase().replaceFirstChar { it.uppercase() })
            // Server Health
            .replace("%server_online%", Bukkit.getOnlinePlayers().size.toString())
            .replace("%server_tps%", cachedTps)
            .replace("%server_ram_used%", ramUsed.toString())
            .replace("%server_ram_max%", ramMax.toString())
            .replace("%server_uptime%", formatTimeMillis(uptimeMs))
            // Dynamic
            .replace("%status_ping%", statusPing)
            .replace("%if_afk%", afkStatus)
            
        // Custom Config Placeholders
        val customPlaceholders = plugin.config.getConfigurationSection("custom_placeholders")
        if (customPlaceholders != null) {
            for (key in customPlaceholders.getKeys(false)) {
                val value = customPlaceholders.getString(key) ?: continue
                parsed = parsed.replace(key, value)
            }
        }
            
        // Replace animation placeholders (%animation.name% and legacy %name%)
        parsed = plugin.animationManager.parse(parsed)
            
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
        
        // Update player list name (the name in the actual list)
        val format = plugin.config.getString("tablist.player_name_format", "%prefix%%player_name%%suffix%")!!
        player.playerListName(Messenger.parse(replacePlaceholders(player, format)))
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
