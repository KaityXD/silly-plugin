package com.kaity.server.gui.hub

import com.kaity.server.SillyPlugin
import com.kaity.server.util.item
import com.kaity.server.util.msg
import com.kaity.server.util.openGui
import org.bukkit.Material
import org.bukkit.entity.Player

class MainGUI(val plugin: SillyPlugin) {

    fun open(player: Player) {
        player.openGui("<gradient:#8E2DE2:#F093FB><bold>Server Menu", 3) {
            fillBorder(item(Material.BLACK_STAINED_GLASS_PANE) { name(" ") })

            setItem(1, 2, item(Material.PLAYER_HEAD) {
                name("<gradient:#8E2DE2:#F093FB><bold>My Profile")
                skullOwner(player)
                lore("<gray>View your stats and level.", "", " <gray>» <yellow>Click to open")
            }) { 
                val user = plugin.userManager.getUser(player.uniqueId)
                if (user != null) {
                    plugin.profileGUI.open(player, user)
                }
            }

            setItem(1, 4, item(Material.NETHER_STAR) {
                name("<gradient:#8E2DE2:#F093FB><bold>Kit Management")
                lore("<gray>Access kits and editors.", "", " <gray>» <yellow>Click to open")
            }) { 
                if (!player.hasPermission("sillyplugin.kit")) {
                    player.msg("<red>You do not have permission to access Kit Management.")
                } else {
                    plugin.kitHubGUI.open(player) 
                }
            }

            setItem(1, 6, item(Material.COMPARATOR) {
                name("<gradient:#8E2DE2:#F093FB><bold>Settings")
                lore("<gray>Configure your preferences.", "", " <gray>» <yellow>Click to open")
            }) { plugin.settingsGUI.open(player) }

            setItem(2, 4, item(Material.BARRIER) {
                name("<red>Close")
            }) { player.closeInventory() }
        }
    }
}
