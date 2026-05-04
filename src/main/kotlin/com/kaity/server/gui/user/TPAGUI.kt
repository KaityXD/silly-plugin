package com.kaity.server.gui.user

import com.kaity.server.SillyPlugin
import com.kaity.server.util.item
import com.kaity.server.util.openGui
import org.bukkit.Material
import org.bukkit.entity.Player

class TPAGUI(val plugin: SillyPlugin) {

    fun open(player: Player) {
        val request = plugin.tpaManager.getRequest(player)
        
        player.openGui("<gradient:#667eea:#764ba2><bold>Teleport Requests", 3) {
            fillBorder(item(Material.BLACK_STAINED_GLASS_PANE) { name(" ") })
            
            if (request == null) {
                setItem(13, item(Material.BARRIER) {
                    name("<red><bold>No Requests")
                    lore(
                        "<gray>You don't have any teleport",
                        "<gray>requests at the moment."
                    )
                }) {
                    plugin.mainGUI.open(player)
                }
            } else {
                val senderName = plugin.server.getPlayer(request.sender)?.name ?: "Unknown"
                val typeStr = if (request.type == com.kaity.server.manager.TPARequestType.TPA) "wants to teleport to you" else "wants you to teleport to them"
                
                setItem(13, item(Material.PLAYER_HEAD) {
                    name("<gradient:#8E2DE2:#F093FB><bold>$senderName")
                    lore(
                        "<gray>$typeStr",
                        "",
                        "<gray>Click <green>Accept <gray>or <red>Deny <gray>below."
                    )
                })

                setItem(11, item(Material.LIME_CONCRETE) {
                    name("<green><bold>Accept")
                    lore(
                        "<gray>Accept the request from",
                        "<gold>$senderName<gray>.",
                        "",
                        "<gray>» <green>Click to accept"
                    )
                }) {
                    plugin.tpaManager.acceptRequest(player)
                    player.closeInventory()
                }

                setItem(15, item(Material.RED_CONCRETE) {
                    name("<red><bold>Deny")
                    lore(
                        "<gray>Deny the request from",
                        "<gold>$senderName<gray>.",
                        "",
                        "<gray>» <red>Click to deny"
                    )
                }) {
                    plugin.tpaManager.denyRequest(player)
                    player.closeInventory()
                }
            }

            setItem(22, item(Material.ARROW) {
                name("<gray>Back")
            }) {
                plugin.mainGUI.open(player)
            }
        }
    }
}
