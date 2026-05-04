package com.kaity.server.manager

import com.kaity.server.SillyPlugin
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent

class StatsManager(val plugin: SillyPlugin) : Listener {

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    @EventHandler
    fun onDeath(event: PlayerDeathEvent) {
        val victim = event.entity
        val killer = victim.killer

        // Victim deaths
        plugin.userManager.getUser(victim)?.let { user ->
            user.deaths++
            plugin.userManager.saveUser(user)
        }

        // Killer kills
        if (killer != null) {
            plugin.userManager.getUser(killer)?.let { user ->
                user.kills++
                plugin.userManager.saveUser(user)
            }
        }
    }
}
