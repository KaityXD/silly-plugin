package com.kaity.server.util

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.plugin.java.JavaPlugin

import com.kaity.server.util.msg

/**
 * Specialized listener to handle saving kit-specific enderchests when the inventory is closed.
 */
class KitEnderchestListener(private val plugin: JavaPlugin, private val kitManager: com.kaity.server.manager.KitManager) : Listener {
    
    // We'll use a metadata or a map to track which kit enderchest a player is editing
    companion object {
        val editingKitEnderchest = mutableMapOf<java.util.UUID, String>()
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return
        val kitName = editingKitEnderchest.remove(player.uniqueId) ?: return
        
        kitManager.saveKitEnderchest(player, kitName)
        player.msg("<green>Kit enderchest for <white>$kitName</white> has been saved.")
    }
}
