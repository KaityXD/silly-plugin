package com.kaity.server.gui.admin

import com.kaity.server.SillyPlugin
import com.kaity.server.util.AnvilInputUI
import com.kaity.server.util.item
import com.kaity.server.util.msg
import com.kaity.server.util.openGui
import org.bukkit.Material
import org.bukkit.entity.Player

class GroupDetailGUI(val plugin: SillyPlugin) {

    fun open(player: Player, groupName: String, page: Int = 0) {
        val permissions = plugin.userManager.database.getGroupPermissions(groupName).sorted()
        val totalPages = if (permissions.isEmpty()) 1 else (permissions.size - 1) / 28 + 1
        val groupData = plugin.userManager.database.getGroup(groupName) ?: return

        player.openGui("<gradient:#8E2DE2:#F093FB><bold>Group: $groupName <gray>(${page + 1}/$totalPages)", 6) {
            fillBorder(item(Material.BLACK_STAINED_GLASS_PANE) { name(" ") }, top = false, sides = false)

            val start = page * 28
            val end = (start + 28).coerceAtMost(permissions.size)
            val slots = (0 until 28).toList()

            for (i in start until end) {
                val perm = permissions[i]
                
                setItem(slots[i - start], item(Material.PAPER) {
                    name("<green>$perm")
                    lore(
                        " <gray>Status: <green>Granted to Group",
                        "",
                        " <gray>» <red>Shift-Click <gray>to remove",
                        ""
                    )
                }) { event ->
                    if (event.isShiftClick) {
                        plugin.userManager.database.removeGroupPermission(groupName, perm)
                        open(player, groupName, page)
                    }
                }
            }

            // Edit Weight
            setItem(54, item(Material.COMPARATOR) {
                name("<yellow><bold>Edit Weight")
                lore(" <gray>Current Weight: <white>${groupData.weight}", "", " <gray>» Click to edit")
            }) {
                AnvilInputUI.open(player, "Edit Weight", groupData.weight.toString()) { input ->
                    val weight = input.toIntOrNull()
                    if (weight != null) {
                        plugin.userManager.database.updateGroup(groupName, weight, groupData.prefix, groupData.suffix)
                        player.msg("<green>Updated weight for $groupName to $weight.")
                    }
                    open(player, groupName, page)
                }
            }

            // Edit Prefix
            setItem(45, item(Material.NAME_TAG) {
                name("<yellow><bold>Edit Prefix")
                lore(" <gray>Current Prefix: <white>${groupData.prefix.ifEmpty { "None" }}", "", " <gray>» Click to edit")
            }) {
                AnvilInputUI.open(player, "Edit Prefix", groupData.prefix.ifEmpty { "Enter prefix..." }) { input ->
                    val newPrefix = if (input.equals("cancel", true) || input == "Enter prefix...") "" else input
                    plugin.userManager.database.updateGroup(groupName, groupData.weight, newPrefix, groupData.suffix)
                    player.msg("<green>Updated prefix for $groupName.")
                    open(player, groupName, page)
                }
            }

            // Edit Suffix
            setItem(46, item(Material.NAME_TAG) {
                name("<yellow><bold>Edit Suffix")
                lore(" <gray>Current Suffix: <white>${groupData.suffix.ifEmpty { "None" }}", "", " <gray>» Click to edit")
            }) {
                AnvilInputUI.open(player, "Edit Suffix", groupData.suffix.ifEmpty { "Enter suffix..." }) { input ->
                    val newSuffix = if (input.equals("cancel", true) || input == "Enter suffix...") "" else input
                    plugin.userManager.database.updateGroup(groupName, groupData.weight, groupData.prefix, newSuffix)
                    player.msg("<green>Updated suffix for $groupName.")
                    open(player, groupName, page)
                }
            }

            // Add Permission
            setItem(47, item(Material.ANVIL) {
                name("<green><bold>Add Permission")
                lore(" <gray>» Click to add permission to group")
            }) {
                plugin.addGroupPermissionGUI.open(player, groupName)
            }

            if (page > 0) {
                setItem(48, item(Material.ARROW) {
                    name("<light_purple>Previous Page")
                }) { open(player, groupName, page - 1) }
            }

            setItem(49, item(Material.BARRIER) {
                name("<red>Back")
            }) { plugin.groupManagementGUI.open(player) }

            if (page < totalPages - 1) {
                setItem(50, item(Material.ARROW) {
                    name("<light_purple>Next Page")
                }) { open(player, groupName, page + 1) }
            }
        }
    }
}

