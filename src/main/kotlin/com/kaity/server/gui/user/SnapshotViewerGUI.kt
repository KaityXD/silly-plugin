package com.kaity.server.gui.user

import com.kaity.server.SillyPlugin
import com.kaity.server.util.item
import com.kaity.server.util.openGui
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class SnapshotViewerGUI(val plugin: SillyPlugin) {

    /**
     * Opens a read-only snapshot of a player's inventory.
     * items array expected to be size 54 to match the GUI layout.
     */
    fun openInventory(player: Player, ownerName: String, items: Array<ItemStack?>) {
        player.openGui("<gradient:#8E2DE2:#F093FB><bold>${ownerName}'s Inventory", 6) {
            val spacer = item(Material.BLACK_STAINED_GLASS_PANE) { name(" ") }
            
            // Set everything from the snapshot
            for (i in 0 until 54) {
                val item = items[i]
                if (item != null && item.type != Material.AIR) {
                    setItem(i, item.clone())
                } else if (i == 4 || i in 6..8 || i in 45..53) {
                    setItem(i, spacer)
                }
            }
        }
    }

    /**
     * Opens a read-only snapshot of a player's ender chest.
     * items array expected to be size 27.
     */
    fun openEnderChest(player: Player, ownerName: String, items: Array<ItemStack?>) {
        player.openGui("<gradient:#8E2DE2:#F093FB><bold>${ownerName}'s Ender Chest", 3) {
            for (i in 0 until 27) {
                val item = items[i]
                if (item != null && item.type != Material.AIR) {
                    setItem(i, item.clone())
                }
            }
        }
    }
}
