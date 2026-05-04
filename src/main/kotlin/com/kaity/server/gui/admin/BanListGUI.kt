package com.kaity.server.gui.admin

import com.kaity.server.SillyPlugin
import com.kaity.server.util.AnvilInputUI
import com.kaity.server.util.item
import com.kaity.server.util.msg
import com.kaity.server.util.openGui
import org.bukkit.Material
import org.bukkit.entity.Player

class BanListGUI(val plugin: SillyPlugin) {

    fun open(player: Player, page: Int = 0, searchQuery: String? = null) {
        val bannedPlayers = plugin.moderationManager.getBannedPlayers()
        
        val filteredPlayers = if (searchQuery != null) {
            bannedPlayers.filter { 
                val target: Any = it.target
                val name = if (target is org.bukkit.profile.PlayerProfile) target.name else target.toString()
                name?.contains(searchQuery, ignoreCase = true) == true
            }
        } else {
            bannedPlayers
        }.sortedBy { 
            val target: Any = it.target
            if (target is org.bukkit.profile.PlayerProfile) target.name ?: "" else target.toString()
        }

        val totalPages = if (filteredPlayers.isEmpty()) 1 else (filteredPlayers.size - 1) / 28 + 1

        val title = "<gradient:#ff4b2b:#ff416c><bold>Ban List <gray>(${page + 1}/$totalPages)"

        player.openGui(title, 6) {
            fillBorder(item(Material.BLACK_STAINED_GLASS_PANE) { name(" ") }, top = false, sides = false)

            val start = page * 28
            val end = (start + 28).coerceAtMost(filteredPlayers.size)
            val slots = (0 until 28).toList()

            for (i in start until end) {
                val banEntry = filteredPlayers[i]
                val target: Any = banEntry.target
                val targetName = if (target is org.bukkit.profile.PlayerProfile) target.name ?: "Unknown" else target.toString()
                
                setItem(slots[i - start], item(Material.PLAYER_HEAD) {
                    name("<red><bold>$targetName")
                    lore(
                        "<gray>Reason: <white>${banEntry.reason}",
                        "<gray>By: <white>${banEntry.source}",
                        "<gray>Expires: <white>${banEntry.expiration ?: "Never"}",
                        "",
                        " <gray>» <yellow>Click to Unban"
                    )
                }) {
                    plugin.moderationManager.unbanPlayer(targetName)
                    player.msg("<green>Unbanned $targetName.")
                    open(player, page, searchQuery) // refresh
                }
            }

            setItem(45, item(Material.COMPASS) {
                name("<yellow><bold>Search Banned Players")
                if (searchQuery != null) {
                    lore(" <gray>Current: <white>$searchQuery", "", " <gray>» Click to search again", " <gray>» Right-Click to clear")
                } else {
                    lore(" <gray>» Click to enter a search term")
                }
            }) { event ->
                if (event.isRightClick && searchQuery != null) {
                    open(player, 0, null)
                } else {
                    AnvilInputUI.open(player, "Search Bans", searchQuery ?: "Enter name...") { input ->
                        if (input.equals("cancel", true)) {
                            open(player, 0, null)
                        } else {
                            open(player, 0, input)
                        }
                    }
                }
            }

            if (page > 0) {
                setItem(48, item(Material.ARROW) {
                    name("<light_purple>Previous Page")
                }) { open(player, page - 1, searchQuery) }
            }

            setItem(49, item(Material.BARRIER) {
                name("<red>Back")
            }) { 
                plugin.playerSelectorGUI.open(player, guiContext = "moderation") { target ->
                    plugin.moderationGUI.open(player, target)
                }
            }

            if (page < totalPages - 1) {
                setItem(50, item(Material.ARROW) {
                    name("<light_purple>Next Page")
                }) { open(player, page + 1, searchQuery) }
            }
        }
    }
}
