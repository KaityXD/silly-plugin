package com.kaity.server.gui.user

import com.kaity.server.SillyPlugin
import com.kaity.server.util.item
import com.kaity.server.util.openGui
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class ItemViewerGUI(val plugin: SillyPlugin) {

    fun open(player: Player, ownerName: String, itemStack: ItemStack) {
        player.openGui("<gradient:#8E2DE2:#F093FB><bold>${ownerName}'s Item", 3) {
            fillBorder(item(Material.BLACK_STAINED_GLASS_PANE) { name(" ") })
            
            // The actual item
            setItem(13, itemStack.clone()) {
                // Do nothing on click
            }

            setItem(22, item(Material.BARRIER) {
                name("<red>Close")
            }) { player.closeInventory() }
        }
    }
}
