package com.kaity.server.gui.user

import com.kaity.server.SillyPlugin
import com.kaity.server.user.ServerUser
import com.kaity.server.util.item
import com.kaity.server.util.openGui
import com.kaity.server.util.msg
import org.bukkit.Material
import org.bukkit.entity.Player
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class ProfileGUI(val plugin: SillyPlugin) {

    fun open(viewer: Player, target: ServerUser) {
        val totalPlaytime = target.playtime + (if (plugin.server.getPlayer(target.id) != null) System.currentTimeMillis() - target.lastJoin else 0L)
        val hours = TimeUnit.MILLISECONDS.toHours(totalPlaytime)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(totalPlaytime) % 60
        val playtimeStr = "${hours}h ${minutes}m"
        
        val kdr = if (target.deaths == 0) target.kills.toDouble() else String.format("%.2f", target.kills.toDouble() / target.deaths).toDouble()
        val firstJoinDate = try {
            LocalDateTime.parse(target.firstJoin).format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
        } catch (e: Exception) {
            "Unknown"
        }

        viewer.openGui("<gradient:#8E2DE2:#F093FB><bold>User Profile", 3) {
            fillBorder(item(Material.BLACK_STAINED_GLASS_PANE) { name(" ") })
            
            setItem(13, item(Material.PLAYER_HEAD) {
                name("<gradient:#8E2DE2:#F093FB><bold>${target.name}")
                skullOwner(plugin.server.getOfflinePlayer(target.id))
                lore(
                    "",
                    " <gray>⚔ <yellow>Kills: <white>${target.kills}",
                    " <gray>☠ <yellow>Deaths: <white>${target.deaths}",
                    " <gray>📊 <yellow>Ratio: <white>$kdr",
                    "",
                    " <gray>⌛ <yellow>Playtime: <white>$playtimeStr",
                    " <gray>📅 <yellow>Joined: <white>$firstJoinDate",
                    " <gray>🏠 <yellow>Homes: <white>${target.homes.size}",
                    ""
                )
            })

            // Actions
            if (viewer.uniqueId != target.id) {
                setItem(11, item(Material.PAPER) {
                    name("<gradient:#667eea:#764ba2><bold>Send Message")
                    lore(
                        "<gray>Click to start a private",
                        "<gray>conversation.",
                        "",
                        "<gray>» <light_purple>Click to message"
                    )
                }) {
                    viewer.closeInventory()
                    viewer.msg("<gray>Type <gold>/msg ${target.name} <message> <gray>to chat!")
                }

                setItem(15, item(Material.ENDER_PEARL) {
                    name("<gradient:#2193b0:#6dd5ed><bold>Teleport Request")
                    lore(
                        "<gray>Click to send a TPA request",
                        "<gray>to this player.",
                        "",
                        "<gray>» <aqua>Click to request"
                    )
                }) {
                    viewer.closeInventory()
                    viewer.performCommand("tpa ${target.name}")
                }
            }

            setItem(22, item(Material.BARRIER) {
                name("<red>Back")
            }) { viewer.closeInventory() }
        }
    }
}
