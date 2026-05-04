package com.kaity.server.gui.admin

import com.kaity.server.SillyPlugin
import com.kaity.server.util.item
import com.kaity.server.util.msg
import com.kaity.server.util.openGui
import org.bukkit.Material
import org.bukkit.entity.Player

class PlayerGroupsGUI(val plugin: SillyPlugin, val target: Player) {

    fun open(player: Player) {
        val userGroups = plugin.userManager.database.getUserGroups(target.uniqueId)
        val allGroups = plugin.userManager.database.getGroups()

        player.openGui("<gradient:#8E2DE2:#F093FB><bold>${target.name}'s Groups", 6) {
            fillBorder(item(Material.BLACK_STAINED_GLASS_PANE) { name(" ") }, top = false, sides = false)

            allGroups.forEachIndexed { index, groupName ->
                if (index < 45) {
                    val hasGroup = userGroups.contains(groupName)
                    val mat = if (hasGroup) Material.ENCHANTED_BOOK else Material.BOOK
                    
                    setItem(index, item(mat) {
                        name((if (hasGroup) "<green><bold>" else "<gray><bold>") + groupName)
                        if (hasGroup) {
                            lore(
                                " <gray>Status: <green>Assigned",
                                "",
                                " <gray>» <red>Click to remove"
                            )
                        } else {
                            lore(
                                " <gray>Status: <red>Not Assigned",
                                "",
                                " <gray>» <green>Click to assign"
                            )
                        }
                    }) {
                        if (hasGroup) {
                            plugin.userManager.database.removeUserFromGroup(target.uniqueId, groupName)
                            player.msg("<green>Removed ${target.name} from group $groupName.")
                            // Force recalculation of permissions
                            plugin.permissionManager.reloadPermissions(target)
                        } else {
                            plugin.userManager.database.addUserToGroup(target.uniqueId, groupName)
                            player.msg("<green>Added ${target.name} to group $groupName.")
                            plugin.permissionManager.reloadPermissions(target)
                        }
                        open(player)
                    }
                }
            }

            setItem(49, item(Material.BARRIER) {
                name("<red>Back")
            }) { plugin.moderationGUI.open(player, target) }
        }
    }
}
