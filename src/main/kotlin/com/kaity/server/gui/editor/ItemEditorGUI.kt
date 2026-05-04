package com.kaity.server.gui.editor

import com.kaity.server.SillyPlugin
import com.kaity.server.util.item
import com.kaity.server.util.msg
import com.kaity.server.util.openGui
import org.bukkit.Material
import org.bukkit.entity.Player

class ItemEditorGUI(val plugin: SillyPlugin) {

    fun open(player: Player) {
        val itemInHand = player.inventory.itemInMainHand
        if (itemInHand.type == Material.AIR) {
            player.msg("<red>You must be holding an item to edit it!")
            player.closeInventory()
            return
        }

        player.openGui("<gradient:#8E2DE2:#F093FB><bold>Item Editor", 3) {
            fillBorder(item(Material.BLACK_STAINED_GLASS_PANE) { name(" ") })

            // Display current item
            setItem(1, 4, itemInHand.clone()) { /* No grab */ }

            // Rename Button
            setItem(1, 2, item(Material.NAME_TAG) {
                name("<gradient:#8E2DE2:#F093FB><bold>Rename Item")
                lore(
                    "<gray>Modify the item's display name.",
                    "",
                    " <gray>» <yellow>Click to rename"
                )
            }) { 
                player.msg("<red>Rename feature coming soon!")
            }

            // Lore Editor
            setItem(1, 6, item(Material.BOOK) {
                name("<gradient:#8E2DE2:#F093FB><bold>Edit Lore")
                lore(
                    "<gray>Modify the item's lore description.",
                    "",
                    " <gray>» <yellow>Click to edit"
                )
            }) {
                player.msg("<red>Lore editing coming soon!")
            }

            // Back button
            setItem(2, 4, item(Material.BARRIER) {
                name("<red>Back to Hub")
            }) { plugin.kitHubGUI.open(player) }
        }
    }
}
