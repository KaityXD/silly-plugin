package com.kaity.server.manager

import com.kaity.server.SillyPlugin
import com.kaity.server.util.Messenger
import org.bukkit.Bukkit
import org.bukkit.Particle
import org.bukkit.entity.Display
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerJoinEvent
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class AFKManager(val plugin: SillyPlugin) : Listener {
    private val afkPlayers = Collections.newSetFromMap(ConcurrentHashMap<UUID, Boolean>())
    private val holograms = ConcurrentHashMap<UUID, TextDisplay>()
    private val lastActivity = ConcurrentHashMap<UUID, Long>()

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
        
        // Timer for auto-AFK check (Global)
        Bukkit.getAsyncScheduler().runAtFixedRate(plugin, { _ ->
            Bukkit.getOnlinePlayers().forEach { player ->
                val last = lastActivity[player.uniqueId] ?: System.currentTimeMillis()
                if (System.currentTimeMillis() - last > 300_000 && !afkPlayers.contains(player.uniqueId)) {
                    // Set AFK on the player's region thread
                    player.scheduler.run(plugin, { _ -> setAFK(player, true) }, null)
                }
            }
        }, 1, 10, java.util.concurrent.TimeUnit.SECONDS)

        // Particles for AFK players (Region-based)
        Bukkit.getAsyncScheduler().runAtFixedRate(plugin, { _ ->
            afkPlayers.forEach { uuid ->
                val player = Bukkit.getPlayer(uuid) ?: return@forEach
                player.scheduler.run(plugin, { _ ->
                    player.world.spawnParticle(Particle.ENCHANT, player.location.add(0.0, 2.0, 0.0), 5, 0.3, 0.3, 0.3, 0.1)
                }, null)
            }
        }, 1, 2, java.util.concurrent.TimeUnit.SECONDS)
    }

    fun setAFK(player: Player, state: Boolean) {
        if (state) {
            if (afkPlayers.add(player.uniqueId)) {
                plugin.server.broadcast(Messenger.parse("<gray><italic>${player.name} is now AFK."))
                spawnHologram(player)
            }
        } else {
            if (afkPlayers.remove(player.uniqueId)) {
                plugin.server.broadcast(Messenger.parse("<gray><italic>${player.name} is no longer AFK."))
                removeHologram(player)
            }
        }
        lastActivity[player.uniqueId] = System.currentTimeMillis()
    }

    fun isAFK(player: Player): Boolean = afkPlayers.contains(player.uniqueId)

    private fun spawnHologram(player: Player) {
        val display = player.world.spawn(player.location.add(0.0, 2.5, 0.0), TextDisplay::class.java) {
            it.text(Messenger.parse("<gradient:#8E2DE2:#F093FB><bold>AFK</bold></gradient>\n<gray><i>Zzz...</i>"))
            it.billboard = Display.Billboard.CENTER
            it.isPersistent = false
        }
        holograms[player.uniqueId] = display
        
        // Update position periodically on the player's thread
        player.scheduler.runAtFixedRate(plugin, { task ->
            if (!afkPlayers.contains(player.uniqueId) || !player.isOnline) {
                display.remove()
                task.cancel()
                return@runAtFixedRate
            }
            display.teleportAsync(player.location.add(0.0, 2.5, 0.0))
        }, null, 1, 1)
    }

    private fun removeHologram(player: Player) {
        holograms.remove(player.uniqueId)?.remove()
    }

    @EventHandler
    fun onMove(event: PlayerMoveEvent) {
        if (event.hasChangedPosition()) {
            lastActivity[event.player.uniqueId] = System.currentTimeMillis()
            if (afkPlayers.contains(event.player.uniqueId)) {
                setAFK(event.player, false)
            }
        }
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        lastActivity[event.player.uniqueId] = System.currentTimeMillis()
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        removeHologram(event.player)
        afkPlayers.remove(event.player.uniqueId)
        lastActivity.remove(event.player.uniqueId)
    }
}
