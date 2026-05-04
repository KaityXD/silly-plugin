package com.kaity.server.gui.admin

import com.kaity.server.SillyPlugin
import com.kaity.server.util.AnvilInputUI
import com.kaity.server.util.item
import com.kaity.server.util.msg
import com.kaity.server.util.openGui
import org.bukkit.Material
import org.bukkit.entity.Player

class PermissionGUI(val plugin: SillyPlugin) {

    fun open(player: Player, target: Player, page: Int = 0, searchQuery: String? = null) {
        val permissions = plugin.permissionManager.getPermissions(target.uniqueId)
        
        val filteredPerms = if (searchQuery != null) {
            permissions.filter { it.contains(searchQuery, ignoreCase = true) }
        } else {
            permissions
        }.sorted()

        val totalPages = if (filteredPerms.isEmpty()) 1 else (filteredPerms.size - 1) / 28 + 1
        
        val title = if (searchQuery != null) {
            "<gradient:#8E2DE2:#F093FB><bold>Perms (Search) <gray>(${page + 1}/$totalPages)"
        } else {
            "<gradient:#8E2DE2:#F093FB><bold>${target.name}'s Perms <gray>(${page + 1}/$totalPages)"
        }

        player.openGui(title, 6) {
            fillBorder(item(Material.BLACK_STAINED_GLASS_PANE) { name(" ") }, top = false, sides = false)

            val start = page * 28
            val end = (start + 28).coerceAtMost(filteredPerms.size)
            val slots = (0 until 28).toList()

            for (i in start until end) {
                val perm = filteredPerms[i]
                
                setItem(slots[i - start], item(Material.PAPER) {
                    name("<green>$perm")
                    lore(
                        " <gray>Status: <green>Granted",
                        "",
                        " <gray>» <red>Shift-Click <gray>to remove",
                        ""
                    )
                }) { event ->
                    if (event.isShiftClick) {
                        plugin.permissionManager.removePermission(target.uniqueId, perm)
                        open(player, target, page, searchQuery)
                    }
                }
            }

            setItem(45, item(Material.COMPASS) {
                name("<yellow><bold>Search Permissions")
                if (searchQuery != null) {
                    lore(" <gray>Current: <white>$searchQuery", "", " <gray>» Click to search again", " <gray>» Right-Click to clear")
                } else {
                    lore(" <gray>» Click to enter a search term")
                }
            }) { event ->
                if (event.isRightClick && searchQuery != null) {
                    open(player, target, 0, null)
                } else {
                    AnvilInputUI.open(player, "Search Permissions", searchQuery ?: "Enter perm...") { input ->
                        if (input.equals("cancel", true)) {
                            open(player, target, 0, null)
                        } else {
                            open(player, target, 0, input)
                        }
                    }
                }
            }

            setItem(46, item(Material.ANVIL) {
                name("<green><bold>Add Permission")
                lore(" <gray>» Click to select from available permissions")
            }) {
                plugin.addPermissionGUI.open(player, target)
            }

            // Test-As Button
            if (player.uniqueId != target.uniqueId) {
                val isTesting = plugin.permissionManager.getTestingAs(player) == target.uniqueId
                setItem(47, item(if (isTesting) Material.ENDER_EYE else Material.ENDER_PEARL) {
                    name(if (isTesting) "<red><bold>Stop Testing" else "<light_purple><bold>Test as ${target.name}")
                    lore(
                        if (isTesting) " <gray>» Click to restore your own permissions"
                        else " <gray>» Click to temporarily experience the server as this player"
                    )
                }) {
                    if (isTesting) {
                        plugin.permissionManager.stopTestingAs(player)
                        player.msg("<green>You are no longer testing as ${target.name}.")
                    } else {
                        plugin.permissionManager.startTestingAs(player, target)
                        player.msg("<green>You are now testing permissions as ${target.name}.")
                    }
                    open(player, target, page, searchQuery)
                }
            }

            if (page > 0) {
                setItem(48, item(Material.ARROW) {
                    name("<light_purple>Previous Page")
                }) { open(player, target, page - 1, searchQuery) }
            }

            setItem(49, item(Material.BARRIER) {
                name("<red>Close")
            }) { player.closeInventory() }

            if (page < totalPages - 1) {
                setItem(50, item(Material.ARROW) {
                    name("<light_purple>Next Page")
                }) { open(player, target, page + 1, searchQuery) }
            }
        }
    }
}
