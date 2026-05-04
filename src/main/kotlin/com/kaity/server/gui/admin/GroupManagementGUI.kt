package com.kaity.server.gui.admin

import com.kaity.server.SillyPlugin
import com.kaity.server.util.AnvilInputUI
import com.kaity.server.util.item
import com.kaity.server.util.msg
import com.kaity.server.util.openGui
import org.bukkit.Material
import org.bukkit.entity.Player

class GroupManagementGUI(val plugin: SillyPlugin) {

    fun open(player: Player) {
        val groups = plugin.userManager.database.getGroups()

        player.openGui("<gradient:#8E2DE2:#F093FB><bold>Group Management", 6) {
            fillBorder(item(Material.BLACK_STAINED_GLASS_PANE) { name(" ") }, top = false, sides = false)

            groups.forEachIndexed { index, groupName ->
                if (index < 45) {
                    setItem(index, item(Material.CHEST) {
                        name("<gold><bold>$groupName")
                        lore(
                            " <gray>» Click to manage group",
                            " <gray>» Shift-Click to delete"
                        )
                    }) { event ->
                        if (event.isShiftClick) {
                            // TODO: Add deletion logic in DB (Requires updating DatabaseManager)
                            player.msg("<red>Group deletion not yet implemented.")
                        } else {
                            plugin.groupDetailGUI.open(player, groupName)
                        }
                    }
                }
            }

            setItem(45, item(Material.ANVIL) {
                name("<green><bold>Create Group")
                lore(" <gray>» Click to create a new group")
            }) {
                AnvilInputUI.open(player, "Create Group", "Enter name...") { input ->
                    if (!input.equals("cancel", true) && input.isNotBlank()) {
                        plugin.userManager.database.addGroup(input, 0)
                        player.msg("<green>Created group '$input'.")
                        open(player)
                    } else {
                        open(player)
                    }
                }
            }

            setItem(49, item(Material.BARRIER) {
                name("<red>Back")
            }) { plugin.mainGUI.open(player) }
        }
    }
}
