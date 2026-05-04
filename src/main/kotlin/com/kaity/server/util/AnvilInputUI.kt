package com.kaity.server.util

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.inventory.AnvilInventory
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

/**
 * Custom Anvil Input UI - Library-free and Folia compatible.
 * Uses player.openAnvil() to ensure a real anvil container is used.
 */
object AnvilInputUI : Listener {

    private val callbacks = mutableMapOf<UUID, (String) -> Unit>()

    /**
     * Opens a real Anvil GUI to prompt the user for text input.
     */
    fun open(player: Player, title: String, defaultText: String, onComplete: (String) -> Unit) {
        val view = player.openAnvil(player.location, true) ?: return
        
        val component = Messenger.parse(title)
        val legacyTitle = LegacyComponentSerializer.legacySection().serialize(component)
        view.setTitle(legacyTitle)
        
        val inventory = view.topInventory as? AnvilInventory ?: return
        
        val item = ItemStack(Material.PAPER).apply {
            editMeta { meta ->
                meta.displayName(Messenger.parse(defaultText))
            }
        }
        
        inventory.setItem(0, item)
        inventory.repairCost = 0
        
        callbacks[player.uniqueId] = onComplete
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val onComplete = callbacks[player.uniqueId] ?: return
        
        val topInventory = event.view.topInventory as? AnvilInventory ?: return

        event.isCancelled = true

        if (event.clickedInventory == topInventory && event.slot == 2) {
            
            val resultText = topInventory.renameText ?: topInventory.getItem(0)?.itemMeta?.displayName()?.let { 
                PlainTextComponentSerializer.plainText().serialize(it)
            } ?: ""
            
            topInventory.clear() 
            callbacks.remove(player.uniqueId)
            
            val plugin = JavaPlugin.getProvidingPlugin(AnvilInputUI::class.java)
            player.scheduler.runDelayed(plugin, { _ ->
                player.closeInventory()
                onComplete(resultText)
            }, null, 1L)
        }
    }

    @EventHandler
    fun onPrepareAnvil(event: PrepareAnvilEvent) {
        if (callbacks.containsKey(event.view.player.uniqueId)) {
            event.inventory.repairCost = 0
            event.inventory.maximumRepairCost = 0
        }
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return
        
        if (callbacks.containsKey(player.uniqueId)) {
            event.inventory.clear() 
            callbacks.remove(player.uniqueId)
            
            // 1-tick delay is still required so the server actually puts the item in their inventory first
            val plugin = JavaPlugin.getProvidingPlugin(AnvilInputUI::class.java)
            player.scheduler.runDelayed(plugin, { _ ->
                val inventory = player.inventory
                
                // Directly loop through the player's inventory and delete the specific paper
                for (i in 0 until inventory.size) {
                    val item = inventory.getItem(i) ?: continue
                    
                    if (item.type == Material.PAPER && item.hasItemMeta()) {
                        val meta = item.itemMeta
                        if (meta != null && meta.hasDisplayName()) {
                            val name = PlainTextComponentSerializer.plainText().serialize(meta.displayName()!!)
                            if (name == "Enter Name...") {
                                inventory.setItem(i, null)
                            }
                        }
                    }
                }
            }, null, 1L)
        }
    }
}
