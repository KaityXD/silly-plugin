package com.kaity.server.gui.admin

import com.kaity.server.SillyPlugin
import com.kaity.server.util.AnvilInputUI
import com.kaity.server.util.item
import com.kaity.server.util.openGui
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player

class PlayerSelectorGUI(val plugin: SillyPlugin) {

    fun open(player: Player, page: Int = 0, searchQuery: String? = null, guiContext: String = "general", onSelect: (Player) -> Unit = { target -> plugin.permissionGUI.open(player, target) }) {
        val players = Bukkit.getOnlinePlayers().toList()
        
        val filteredPlayers = if (searchQuery != null) {
            players.filter { it.name.contains(searchQuery, ignoreCase = true) }
        } else {
            players
        }.sortedBy { it.name }

        val totalPages = if (filteredPlayers.isEmpty()) 1 else (filteredPlayers.size - 1) / 28 + 1

        val title = if (searchQuery != null) {
            "<gradient:#8E2DE2:#F093FB><bold>Select Player <gray>(${page + 1}/$totalPages)"
        } else {
            "<gradient:#8E2DE2:#F093FB><bold>Select Player <gray>(${page + 1}/$totalPages)"
        }

        player.openGui(title, 6) {
            fillBorder(item(Material.BLACK_STAINED_GLASS_PANE) { name(" ") }, top = false, sides = false)

            val start = page * 28
            val end = (start + 28).coerceAtMost(filteredPlayers.size)
            val slots = (0 until 28).toList()

            for (i in start until end) {
                val target = filteredPlayers[i]
                setItem(slots[i - start], item(Material.PLAYER_HEAD) {
                    name("<gradient:#8E2DE2:#F093FB><bold>${target.name}")
                    skullOwner(target)
                    lore(
                        "<gray>Click to select this player.",
                        "",
                        " <gray>» <yellow>Click to select"
                    )
                }) {
                    onSelect(target)
                }
            }

            setItem(45, item(Material.COMPASS) {
                name("<yellow><bold>Search Players")
                if (searchQuery != null) {
                    lore(" <gray>Current: <white>$searchQuery", "", " <gray>» Click to search again", " <gray>» Right-Click to clear")
                } else {
                    lore(" <gray>» Click to enter a search term")
                }
            }) { event ->
                if (event.isRightClick && searchQuery != null) {
                    open(player, 0, null, guiContext, onSelect)
                } else {
                    AnvilInputUI.open(player, "Search Players", searchQuery ?: "Enter name...") { input ->
                        if (input.equals("cancel", true)) {
                            open(player, 0, null, guiContext, onSelect)
                        } else {
                            open(player, 0, input, guiContext, onSelect)
                        }
                    }
                }
            }

            if (page > 0) {
                setItem(48, item(Material.ARROW) {
                    name("<light_purple>Previous Page")
                }) { open(player, page - 1, searchQuery, guiContext, onSelect) }
            }

            setItem(49, item(Material.BARRIER) {
                name("<red>Back")
            }) { plugin.mainGUI.open(player) }

            if (page < totalPages - 1) {
                setItem(50, item(Material.ARROW) {
                    name("<light_purple>Next Page")
                }) { open(player, page + 1, searchQuery, guiContext, onSelect) }
            }

            if (guiContext == "moderation") {
                setItem(53, item(Material.PAPER) {
                    name("<red><bold>Ban List")
                    lore("<gray>View and manage banned players.", "", " <gray>» <yellow>Click to view")
                }) {
                    plugin.banListGUI.open(player)
                }
            }
        }
    }
}
