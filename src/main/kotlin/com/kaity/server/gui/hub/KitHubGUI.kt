package com.kaity.server.gui.hub

import com.kaity.server.SillyPlugin
import com.kaity.server.util.item
import com.kaity.server.util.msg
import com.kaity.server.util.openGui
import org.bukkit.Material
import org.bukkit.entity.Player

class KitHubGUI(val plugin: SillyPlugin) {

    fun open(player: Player) {
        player.openGui("<gradient:#8E2DE2:#F093FB><bold>Kit Management Hub", 3) {
            fillBorder(item(Material.BLACK_STAINED_GLASS_PANE) { name(" ") })

            // Kit Loader
            setItem(1, 1, item(Material.CHEST) {
                name("<gradient:#8E2DE2:#F093FB><bold>Kit Loader")
                lore(
                    "<gray>Equip your saved kits",
                    "",
                    " <gray>» <yellow>Click to open"
                )
            }) { 
                if (!player.hasPermission("sillyplugin.kit")) {
                    player.msg("<red>You do not have permission to use the Kit Loader.")
                } else {
                    plugin.kitGUI.open(player) 
                }
            }

            // Kit Editor (Layout Editor)
            setItem(1, 2, item(Material.ANVIL) {
                name("<gradient:#8E2DE2:#F093FB><bold>Kit Editor")
                lore(
                    "<gray>Modify your inventory layout",
                    "<gray>and kit-specific enderchests.",
                    "",
                    " <red>TEMPORARILY DISABLED"
                )
            }) { 
                if (!player.hasPermission("sillyplugin.kit.admin")) {
                    player.msg("<red>You do not have permission to use the Kit Editor.")
                } else {
                    player.msg("<red>The Kit Editor is being upgraded to a modern reactive system!")
                }
            }

            // Custom Enchanter
            setItem(1, 4, item(Material.ENCHANTED_BOOK) {
                name("<gradient:#8E2DE2:#F093FB><bold>Custom Enchanter")
                lore(
                    "<gray>Add or remove enchantments",
                    "<gray>from the item in your hand.",
                    "",
                    " <gray>» <yellow>Click to open"
                )
            }) { 
                if (!player.hasPermission("sillyplugin.enchant")) {
                    player.msg("<red>You do not have permission to use the Custom Enchanter.")
                } else {
                    plugin.enchantGUI.open(player) 
                }
            }

            // Item Editor
            setItem(1, 6, item(Material.NAME_TAG) {
                name("<gradient:#8E2DE2:#F093FB><bold>Item Editor")
                lore(
                    "<gray>Change item meta, names,",
                    "<gray>and lore descriptions.",
                    "",
                    " <gray>» <yellow>Click to open"
                )
            }) { 
                if (!player.hasPermission("sillyplugin.editor")) {
                    player.msg("<red>You do not have permission to use the Item Editor.")
                } else {
                    player.msg("<red>Item Editor is coming soon!")
                    // plugin.itemEditorGUI.open(player) 
                }
            }

            // Enderchest
            setItem(1, 7, item(Material.ENDER_CHEST) {
                name("<gradient:#8E2DE2:#F093FB><bold>Enderchest Manager")
                lore(
                    "<gray>Access and manage your",
                    "<gray>enderchest storage.",
                    "",
                    " <gray>» <yellow>Click to open"
                )
            }) { 
                player.closeInventory()
                player.openInventory(player.enderChest)
            }

            // Close Button
            setItem(2, 4, item(Material.BARRIER) {
                name("<red>Close")
            }) { player.closeInventory() }
        }
    }
}
