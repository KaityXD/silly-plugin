package com.kaity.server.gui.admin

import com.kaity.server.SillyPlugin
import com.kaity.server.util.AnvilInputUI
import com.kaity.server.util.item
import com.kaity.server.util.msg
import com.kaity.server.util.openGui
import org.bukkit.Material
import org.bukkit.entity.Player

class AddPermissionGUI(val plugin: SillyPlugin) {

    fun open(player: Player, target: Player, page: Int = 0, sortDescending: Boolean = false, searchQuery: String? = null) {
        // Get all registered server permissions
        val allPermissions = plugin.server.pluginManager.permissions
            .map { it.name }
            .distinct()

        val filteredPermissions = if (searchQuery != null) {
            allPermissions.filter { it.contains(searchQuery, ignoreCase = true) }
        } else {
            allPermissions
        }

        val sortedPermissions = if (sortDescending) {
            filteredPermissions.sortedDescending()
        } else {
            filteredPermissions.sorted()
        }

        val totalPages = if (sortedPermissions.isEmpty()) 1 else (sortedPermissions.size - 1) / 28 + 1

        val title = if (searchQuery != null) {
            "<gradient:#8E2DE2:#F093FB><bold>Add Perms (Search) <gray>(${page + 1}/$totalPages)"
        } else {
            "<gradient:#8E2DE2:#F093FB><bold>Add Perms <gray>(${page + 1}/$totalPages)"
        }

        player.openGui(title, 6) {
            fillBorder(item(Material.BLACK_STAINED_GLASS_PANE) { name(" ") }, top = false, sides = false)

            val start = page * 28
            val end = (start + 28).coerceAtMost(sortedPermissions.size)
            val slots = (0 until 28).toList()

            for (i in start until end) {
                val perm = sortedPermissions[i]
                
                setItem(slots[i - start], item(Material.PAPER) {
                    name("<green>$perm")
                    lore(
                        " <gray>Status: <yellow>Unassigned",
                        "",
                        " <gray>» <green>Click <gray>to add",
                        ""
                    )
                }) {
                    plugin.permissionManager.addPermission(target.uniqueId, perm)
                    player.msg("<green>Added permission '$perm' to ${target.name}.")
                    // Go back to the main permission GUI for that player
                    plugin.permissionGUI.open(player, target)
                }
            }

            // Search button
            setItem(44, item(Material.COMPASS) {
                name("<yellow><bold>Search Permissions")
                if (searchQuery != null) {
                    lore(" <gray>Current: <white>$searchQuery", "", " <gray>» Click to search again", " <gray>» Right-Click to clear")
                } else {
                    lore(" <gray>» Click to enter a search term")
                }
            }) { event ->
                if (event.isRightClick && searchQuery != null) {
                    open(player, target, 0, sortDescending, null)
                } else {
                    AnvilInputUI.open(player, "Search Permissions", searchQuery ?: "Enter perm...") { input ->
                        if (input.equals("cancel", true)) {
                            open(player, target, 0, sortDescending, null)
                        } else {
                            open(player, target, 0, sortDescending, input)
                        }
                    }
                }
            }

            // Sorting toggle
            setItem(45, item(Material.COMPARATOR) {
                name("<yellow><bold>Sort Mode")
                lore(
                    " <gray>Current: ${if (sortDescending) "<red>Z-A" else "<green>A-Z"}",
                    "",
                    " <gray>» Click to toggle"
                )
            }) {
                open(player, target, 0, !sortDescending, searchQuery)
            }

            // Back button
            setItem(46, item(Material.DARK_OAK_DOOR) {
                name("<red><bold>Back")
                lore(" <gray>» Return to ${target.name}'s perms")
            }) {
                plugin.permissionGUI.open(player, target)
            }

            // Add Custom Permission button
            setItem(47, item(Material.WRITABLE_BOOK) {
                name("<green><bold>Add Custom Permission")
                lore(" <gray>» Click to type a custom permission (e.g. *)")
            }) {
                AnvilInputUI.open(player, "Custom Permission", "Enter perm...") { input ->
                    if (!input.equals("cancel", true) && input.isNotBlank() && input != "Enter perm...") {
                        plugin.permissionManager.addPermission(target.uniqueId, input)
                        player.msg("<green>Added custom permission '$input' to ${target.name}.")
                        plugin.permissionGUI.open(player, target)
                    } else {
                        open(player, target, page, sortDescending, searchQuery)
                    }
                }
            }

            if (page > 0) {
                setItem(48, item(Material.ARROW) {
                    name("<light_purple>Previous Page")
                }) { open(player, target, page - 1, sortDescending, searchQuery) }
            }

            setItem(49, item(Material.BARRIER) {
                name("<red>Close")
            }) { player.closeInventory() }

            if (page < totalPages - 1) {
                setItem(50, item(Material.ARROW) {
                    name("<light_purple>Next Page")
                }) { open(player, target, page + 1, sortDescending, searchQuery) }
            }
        }
    }
}
