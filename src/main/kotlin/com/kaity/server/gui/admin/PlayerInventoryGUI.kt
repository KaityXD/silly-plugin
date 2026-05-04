package com.kaity.server.gui.admin

import com.kaity.server.SillyPlugin
import com.kaity.server.util.Messenger
import com.kaity.server.util.item
import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder

class PlayerInventoryGUI(val plugin: SillyPlugin, val target: Player) : InventoryHolder, Listener {
    private val inventory = Bukkit.createInventory(this, 54, Messenger.parse("<red>${target.name}'s Inventory"))
    private var task: ScheduledTask? = null
    
    private var isSyncingToPlayer = false

    init {
        updateFromPlayer()
        Bukkit.getPluginManager().registerEvents(this, plugin)
        task = target.scheduler.runAtFixedRate(plugin, { t ->
            if (inventory.viewers.isEmpty()) {
                t.cancel()
                HandlerList.unregisterAll(this)
            } else {
                updateFromPlayer()
            }
        }, null, 10L, 10L)
    }

    override fun getInventory(): Inventory = inventory

    fun open(player: Player) {
        player.openInventory(inventory)
    }

    private fun updateFromPlayer() {
        if (isSyncingToPlayer) return
        val pInv = target.inventory
        
        inventory.setItem(0, pInv.helmet)
        inventory.setItem(1, pInv.chestplate)
        inventory.setItem(2, pInv.leggings)
        inventory.setItem(3, pInv.boots)
        
        val spacer = item(Material.BLACK_STAINED_GLASS_PANE) { name(" ") }
        inventory.setItem(4, spacer)
        inventory.setItem(5, pInv.itemInOffHand)
        inventory.setItem(6, spacer)
        inventory.setItem(7, spacer)
        inventory.setItem(8, spacer)
        
        for (i in 9..35) inventory.setItem(i, pInv.getItem(i))
        for (i in 0..8) inventory.setItem(i + 36, pInv.getItem(i))
        for (i in 45..53) inventory.setItem(i, spacer)
    }

    private fun syncToPlayer() {
        isSyncingToPlayer = true
        val pInv = target.inventory
        
        pInv.helmet = inventory.getItem(0)
        pInv.chestplate = inventory.getItem(1)
        pInv.leggings = inventory.getItem(2)
        pInv.boots = inventory.getItem(3)
        pInv.setItemInOffHand(inventory.getItem(5))
        
        for (i in 9..35) pInv.setItem(i, inventory.getItem(i))
        for (i in 0..8) pInv.setItem(i, inventory.getItem(i + 36))
        
        isSyncingToPlayer = false
    }

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        if (event.inventory.holder != this) return
        
        if (event.rawSlot in listOf(4, 6, 7, 8) || event.rawSlot in 45..53) {
            event.isCancelled = true
            return
        }
        
        target.scheduler.run(plugin, { _ ->
            syncToPlayer()
        }, null)
    }

    @EventHandler
    fun onDrag(event: InventoryDragEvent) {
        if (event.inventory.holder != this) return
        
        if (event.rawSlots.any { it in listOf(4, 6, 7, 8) || it in 45..53 }) {
            event.isCancelled = true
            return
        }
        
        target.scheduler.run(plugin, { _ ->
            syncToPlayer()
        }, null)
    }
    
    @EventHandler
    fun onClose(event: InventoryCloseEvent) {
        if (event.inventory.holder == this && inventory.viewers.size <= 1) {
            HandlerList.unregisterAll(this)
            task?.cancel()
        }
    }
}