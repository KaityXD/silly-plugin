package com.kaity.server.manager

import com.kaity.server.SillyPlugin
import com.kaity.server.util.Messenger
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.scoreboard.Criteria
import org.bukkit.scoreboard.DisplaySlot
import java.util.concurrent.TimeUnit

class DisplayManager(val plugin: SillyPlugin) : Listener {

    private val scoreboardEnabled = plugin.config.getBoolean("scoreboard.enabled", true)
    private val scoreboardInterval = plugin.config.getLong("scoreboard.update_interval", 20L)
    
    private val tablistEnabled = plugin.config.getBoolean("tablist.enabled", true)
    private val tablistInterval = plugin.config.getLong("tablist.update_interval", 20L)

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)

        if (scoreboardEnabled) {
            val periodMs = scoreboardInterval * 50
            Bukkit.getAsyncScheduler().runAtFixedRate(plugin, { _ ->
                Bukkit.getOnlinePlayers().forEach { player ->
                    player.scheduler.run(plugin, { _ ->
                        if (player.isOnline) {
                            updateScoreboard(player)
                        }
                    }, null)
                }
            }, periodMs, periodMs, TimeUnit.MILLISECONDS)
        }

        if (tablistEnabled) {
            val periodMs = tablistInterval * 50
            Bukkit.getAsyncScheduler().runAtFixedRate(plugin, { _ ->
                Bukkit.getOnlinePlayers().forEach { player ->
                    player.scheduler.run(plugin, { _ ->
                        if (player.isOnline) {
                            updateTablist(player)
                        }
                    }, null)
                }
            }, periodMs, periodMs, TimeUnit.MILLISECONDS)
        }
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        if (scoreboardEnabled) {
            setupScoreboard(player)
            updateScoreboard(player)
        }
        if (tablistEnabled) {
            updateTablist(player)
        }
    }

    private fun replacePlaceholders(player: Player, text: String): String {
        return text
            .replace("%player_name%", player.name)
            .replace("%player_ping%", player.ping.toString())
            .replace("%server_online%", Bukkit.getOnlinePlayers().size.toString())
    }

    private fun updateTablist(player: Player) {
        val headerStr = plugin.config.getString("tablist.header", "")!!
        val footerStr = plugin.config.getString("tablist.footer", "")!!
        
        val header = Messenger.parse(replacePlaceholders(player, headerStr))
        val footer = Messenger.parse(replacePlaceholders(player, footerStr))
        
        player.sendPlayerListHeaderAndFooter(header, footer)
    }

    private fun setupScoreboard(player: Player) {
        val manager = Bukkit.getScoreboardManager()
        val scoreboard = manager.newScoreboard
        val titleStr = plugin.config.getString("scoreboard.title", "<gold><bold>Silly Server</bold></gold>")!!
        
        val objective = scoreboard.registerNewObjective("silly_board", Criteria.DUMMY, Messenger.parse(titleStr))
        objective.displaySlot = DisplaySlot.SIDEBAR
        
        player.scoreboard = scoreboard
    }

    private fun updateScoreboard(player: Player) {
        val scoreboard = player.scoreboard
        val objective = scoreboard.getObjective("silly_board") ?: return
        
        val titleStr = plugin.config.getString("scoreboard.title", "<gold><bold>Silly Server</bold></gold>")!!
        objective.displayName(Messenger.parse(replacePlaceholders(player, titleStr)))

        val lines = plugin.config.getStringList("scoreboard.lines")
        
        for (i in lines.indices) {
            val scoreValue = lines.size - i
            val teamName = "line_\$i"
            var team = scoreboard.getTeam(teamName)
            
            // Unique entry using a combination of invisible color codes based on index
            val entryColor = "§" + Integer.toHexString(i) + "§r"

            if (team == null) {
                team = scoreboard.registerNewTeam(teamName)
                team.addEntry(entryColor)
                objective.getScore(entryColor).score = scoreValue
            } else {
                objective.getScore(entryColor).score = scoreValue
            }
            
            val lineStr = replacePlaceholders(player, lines[i])
            team.prefix(Messenger.parse(lineStr))
            team.suffix(Component.empty())
        }
        
        // Clean up excess teams/scores
        for (i in lines.size..15) {
            val teamName = "line_\$i"
            val team = scoreboard.getTeam(teamName)
            if (team != null) {
                val entryColor = "§" + Integer.toHexString(i) + "§r"
                scoreboard.resetScores(entryColor)
                team.unregister()
            }
        }
    }
}
