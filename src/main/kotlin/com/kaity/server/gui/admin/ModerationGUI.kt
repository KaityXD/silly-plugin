package com.kaity.server.gui.admin

import com.kaity.server.SillyPlugin
import com.kaity.server.util.AnvilInputUI
import com.kaity.server.util.item
import com.kaity.server.util.msg
import com.kaity.server.util.openGui
import org.bukkit.Material
import org.bukkit.entity.Player

/**
 * Admin menu for managing a specific player.
 */
class ModerationGUI(val plugin: SillyPlugin) {

    fun open(player: Player, target: Player) {
        player.openGui("<red><bold>Moderating: ${target.name}", 6) {
            fillBorder(item(Material.BLACK_STAINED_GLASS_PANE) { name(" ") })

            // Live stats & info
            setItem(4, item(Material.PLAYER_HEAD) {
                name("<gradient:#ff4b2b:#ff416c><bold>${target.name}")
                skullOwner(target)
                lore(
                    "",
                    " <gray>Health: <red>${String.format("%.1f", target.health)}/${target.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH)?.value ?: 20.0}",
                    " <gray>Food: <gold>${target.foodLevel}",
                    " <gray>Gamemode: <yellow>${target.gameMode.name}",
                    " <gray>World: <green>${target.world.name}",
                    " <gray>Location: <white>${target.location.blockX}, ${target.location.blockY}, ${target.location.blockZ}",
                    ""
                )
            })

            // Punishment actions
            setItem(10, item(Material.IRON_DOOR) {
                name("<red><bold>Kick Player")
                lore("<gray>Forcefully remove the player", "<gray>from the server.", "", " <gray>» <yellow>Click to Kick")
            }) {
                AnvilInputUI.open(player, "Kick Reason", "Inappropriate behavior") { reason ->
                    plugin.moderationManager.kickPlayer(target, reason)
                    player.msg("<green>Kicked ${target.name} for: <white>$reason")
                }
            }

            setItemClick(11, item(Material.BARRIER) {
                name("<dark_red><bold>Ban Player")
                lore(
                    "<gray>Ban the player from the server.",
                    "",
                    " <gray>» <yellow>Left-Click: <white>Custom Ban",
                    " <gray>» <yellow>Right-Click: <white>Permanent (Default)",
                    " <gray>» <yellow>Shift-Click: <white>24h Ban"
                )
            }) {
                if (isLeft) {
                    if (isShift) {
                         plugin.moderationManager.banPlayer(target.name, "Banned by a moderator", 24 * 60 * 60 * 1000L)
                         player.msg("<green>Banned ${target.name} for 24h.")
                         player.closeInventory()
                    } else {
                        AnvilInputUI.open(player, "Ban Reason", "Breaking rules") { reason ->
                            player.scheduler.runDelayed(plugin, { _ ->
                                AnvilInputUI.open(player, "Duration (e.g. 1d, perm)", "perm") { durationStr ->
                                    val duration = if (durationStr.lowercase() == "perm") -1L else parseDuration(durationStr) ?: -1L
                                    plugin.moderationManager.banPlayer(target.name, reason, duration)
                                    player.msg("<green>Banned ${target.name} for ${if (duration == -1L) "Permanent" else durationStr}.")
                                }
                            }, null, 1L)
                        }
                    }
                } else if (isRight) {
                    plugin.moderationManager.banPlayer(target.name)
                    player.msg("<green>Permanently banned ${target.name}.")
                    player.closeInventory()
                }
            }

            setItem(12, item(Material.PAPER) {
                val isMuted = plugin.moderationManager.isMuted(target.uniqueId)
                name("${if (isMuted) "<green>" else "<gold>"}<bold>${if (isMuted) "Unmute" else "Mute"} Player")
                lore("<gray>Prevent the player from", "<gray>talking in chat.", "", " <gray>» <yellow>Click to ${if (isMuted) "Unmute" else "Mute"}")
            }) {
                if (plugin.moderationManager.isMuted(target.uniqueId)) {
                    plugin.moderationManager.unmutePlayer(target.uniqueId)
                    player.msg("<green>Unmuted ${target.name}.")
                } else {
                    AnvilInputUI.open(player, "Mute Duration (e.g. 1h, perm)", "1h") { durationStr ->
                        val duration = if (durationStr.lowercase() == "perm") -1L else parseDuration(durationStr) ?: (60 * 60 * 1000L)
                        plugin.moderationManager.mutePlayer(target.uniqueId, duration)
                        player.msg("<green>Muted ${target.name} for $durationStr.")
                    }
                }
                open(player, target) // Refresh
            }

            setItem(13, item(Material.ICE) {
                val isFrozen = plugin.moderationManager.isFrozen(target.uniqueId)
                name("${if (isFrozen) "<green>" else "<aqua>"}<bold>${if (isFrozen) "Unfreeze" else "Freeze"} Player")
                lore("<gray>Prevent the player from", "<gray>moving or interacting.", "", " <gray>» <yellow>Click to ${if (isFrozen) "Unfreeze" else "Freeze"}")
            }) {
                if (plugin.moderationManager.isFrozen(target.uniqueId)) {
                    plugin.moderationManager.unfreezePlayer(target.uniqueId)
                    player.msg("<green>Unfrozen ${target.name}.")
                } else {
                    plugin.moderationManager.freezePlayer(target.uniqueId)
                    player.msg("<green>Frozen ${target.name}.")
                }
                open(player, target) // Refresh
            }

            // Utility shortcuts
            setItem(14, item(Material.CHEST) {
                name("<yellow><bold>View Inventory")
                lore("<gray>See and edit the player's", "<gray>current inventory.", "", " <gray>» <yellow>Click to View")
            }) {
                PlayerInventoryGUI(plugin, target).open(player)
            }

            setItem(15, item(Material.ENDER_CHEST) {
                name("<light_purple><bold>View Enderchest")
                lore("<gray>See and edit the player's", "<gray>enderchest contents.", "", " <gray>» <yellow>Click to View")
            }) {
                player.openInventory(target.enderChest)
            }

            setItem(16, item(Material.COMPASS) {
                name("<aqua><bold>Teleport to Player")
                lore("<gray>Instant teleport to the", "<gray>player's location.", "", " <gray>» <yellow>Click to Teleport")
            }) {
                player.teleportAsync(target.location)
                player.msg("<green>Teleported to ${target.name}.")
            }

            setItem(19, item(Material.ENDER_PEARL) {
                name("<aqua><bold>Teleport Here")
                lore("<gray>Instant teleport the", "<gray>player to you.", "", " <gray>» <yellow>Click to Teleport")
            }) {
                target.teleportAsync(player.location)
                player.msg("<green>Teleported ${target.name} to you.")
            }

            // Quick state modification
            setItem(20, item(Material.APPLE) {
                name("<green><bold>Heal & Feed")
                lore("<gray>Restore player's health", "<gray>and hunger to full.", "", " <gray>» <yellow>Click to Restore")
            }) {
                target.health = target.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH)?.value ?: 20.0
                target.foodLevel = 20
                player.msg("<green>Healed and fed ${target.name}.")
            }

            setItem(21, item(Material.BLAZE_POWDER) {
                name("<red><bold>Kill Player")
                lore("<gray>Instantly kill the player.", "", " <gray>» <yellow>Click to Kill")
            }) {
                target.health = 0.0
                player.msg("<green>Killed ${target.name}.")
            }

            setItem(22, item(Material.TNT) {
                name("<gold><bold>Clear Inventory")
                lore("<gray>Remove ALL items from the", "<gray>player's inventory.", "", " <gray>» <yellow>Click to Clear")
            }) {
                target.inventory.clear()
                player.msg("<green>Cleared inventory of ${target.name}.")
            }

            // Permissions & Groups
            setItem(23, item(Material.NAME_TAG) {
                name("<gradient:#8E2DE2:#F093FB><bold>Manage Groups")
                lore("<gray>Add or remove the player", "<gray>from permission groups.", "", " <gray>» <yellow>Click to Manage")
            }) {
                com.kaity.server.gui.admin.PlayerGroupsGUI(plugin, target).open(player)
            }

            // Navigation
            setItem(45, item(Material.ARROW) {
                name("<gray>Back to Player Selector")
            }) {
                plugin.playerSelectorGUI.open(player, onSelect = { selected -> open(player, selected) })
            }
        }
    }

    /**
     * Parses simple duration strings like "1d", "12h" into ms.
     */
    private fun parseDuration(input: String): Long? {
        val regex = Regex("(\\d+)([dhms])")
        val matches = regex.findAll(input.lowercase())
        var totalMs = 0L
        var found = false
        
        for (match in matches) {
            found = true
            val value = match.groupValues[1].toLongOrNull() ?: continue
            val unit = match.groupValues[2]
            
            totalMs += when (unit) {
                "d" -> value * 24 * 60 * 60 * 1000
                "h" -> value * 60 * 60 * 1000
                "m" -> value * 60 * 1000
                "s" -> value * 1000
                else -> 0L
            }
        }
        
        return if (found && totalMs > 0) totalMs else null
    }
}
