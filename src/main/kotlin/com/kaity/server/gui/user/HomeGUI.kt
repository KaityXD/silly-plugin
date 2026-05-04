package com.kaity.server.gui.user

import com.kaity.server.SillyPlugin
import com.kaity.server.util.AnvilInputUI
import com.kaity.server.util.item
import com.kaity.server.util.openGui
import org.bukkit.Material
import org.bukkit.entity.Player
import java.time.format.DateTimeFormatter

class HomeGUI(val plugin: SillyPlugin) {

    fun open(player: Player, page: Int = 0, searchQuery: String? = null) {
        val user = plugin.userManager.getUser(player) ?: return
        val homesList = if (searchQuery != null) {
            user.homes.keys.filter { it.contains(searchQuery, ignoreCase = true) }.sorted()
        } else {
            user.homes.keys.toList().sorted()
        }
        
        val totalPages = if (homesList.isEmpty()) 1 else (homesList.size - 1) / 28 + 1
        
        val title = if (searchQuery != null) {
            "<gradient:#8E2DE2:#F093FB><bold>Search Homes <gray>(${page + 1}/$totalPages)"
        } else {
            "<gradient:#8E2DE2:#F093FB><bold>My Homes <gray>(${page + 1}/$totalPages)"
        }

        player.openGui(title, 6) {
            fillBorder(item(Material.BLACK_STAINED_GLASS_PANE) { name(" ") }, top = false, sides = false)

            val start = page * 28
            val end = (start + 28).coerceAtMost(homesList.size)
            
            val slots = (0 until 28).toList()

            for (i in start until end) {
                val name = homesList[i]
                val home = user.homes[name]!!
                val loc = home.location
                val dimension = when (loc.world.name) {
                    "world" -> "Overworld"
                    "world_nether" -> "Nether"
                    "world_the_end" -> "The End"
                    else -> loc.world.name
                }
                val coords = "${loc.blockX}, ${loc.blockY}, ${loc.blockZ}"
                val time = home.createdAt.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"))

                setItem(slots[i - start], item(Material.RED_BED) {
                    name("<gradient:#8E2DE2:#F093FB><bold>$name")
                    lore(
                        " <gray>🌍 <yellow>Dimension: <white>$dimension",
                        " <gray>📍 <yellow>Location: <white>$coords",
                        " <gray>🕒 <yellow>Created: <white>$time",
                        "",
                        " <gray>» <yellow>Left-Click <gray>to teleport",
                        " <gray>» <red>Shift-Click <gray>to delete",
                        ""
                    )
                }) { event ->
                    if (event.isShiftClick) {
                        plugin.homeManager.deleteHome(player, name)
                        open(player, page, searchQuery)
                    } else {
                        plugin.homeManager.teleportHome(player, name)
                        player.closeInventory()
                    }
                }
            }

            // Search Button
            setItem(45, item(Material.COMPASS) {
                name("<yellow><bold>Search Homes")
                if (searchQuery != null) {
                    lore(" <gray>Current: <white>$searchQuery", "", " <gray>» Click to search again", " <gray>» Right-Click to clear")
                } else {
                    lore(" <gray>» Click to enter a search term")
                }
            }) { event ->
                if (event.isRightClick && searchQuery != null) {
                    open(player, 0, null)
                } else {
                    AnvilInputUI.open(player, "Search Homes", searchQuery ?: "Enter name...") { input ->
                        if (input.equals("cancel", true)) {
                            open(player, 0, null)
                        } else {
                            open(player, 0, input)
                        }
                    }
                }
            }

            // Navigation
            if (page > 0) {
                setItem(48, item(Material.ARROW) {
                    name("<light_purple>Previous Page")
                }) { open(player, page - 1, searchQuery) }
            }

            setItem(49, item(Material.BARRIER) {
                name("<red>Back")
            }) { plugin.mainGUI.open(player) }

            if (page < totalPages - 1) {
                setItem(50, item(Material.ARROW) {
                    name("<light_purple>Next Page")
                }) { open(player, page + 1, searchQuery) }
            }
        }
    }
}
