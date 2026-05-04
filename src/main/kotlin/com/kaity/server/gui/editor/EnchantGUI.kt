package com.kaity.server.gui.editor

import com.kaity.server.SillyPlugin
import com.kaity.server.util.item
import com.kaity.server.util.msg
import com.kaity.server.util.openGui
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player

class EnchantGUI(val plugin: SillyPlugin) {

    @Suppress("DEPRECATION")
    fun open(player: Player, page: Int = 0) {
        val itemInHand = player.inventory.itemInMainHand
        if (itemInHand.type == Material.AIR) {
            player.msg("<red>You must be holding an item to enchant it!")
            player.closeInventory()
            return
        }

        // All applicable enchantments for the item
        val enchantments = Enchantment.values()
            .filter { it.canEnchantItem(itemInHand) || player.isOp }
            .sortedBy { it.key.key }
        
        val totalPages = if (enchantments.isEmpty()) 1 else (enchantments.size - 1) / 28 + 1
        
        player.openGui("<gradient:#8E2DE2:#F093FB><bold>Custom Enchanter <gray>(${page + 1}/$totalPages)", 6) {
            fillBorder(item(Material.BLACK_STAINED_GLASS_PANE) { name(" ") })

            val start = page * 28
            val end = (start + 28).coerceAtMost(enchantments.size)
            
            // Define playable slots (10-16, 19-25, 28-34, 37-43)
            val slots = listOf(
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
            )

            for (i in start until end) {
                val enchant = enchantments[i]
                val currentLevel = itemInHand.getEnchantmentLevel(enchant)
                val maxLevel = if (player.isOp) 255 else enchant.maxLevel

                val isMaxed = currentLevel >= maxLevel

                setItem(slots[i - start], item(Material.ENCHANTED_BOOK) {
                    name("<gradient:#8E2DE2:#F093FB><bold>${enchant.key.key.replace('_', ' ').replaceFirstChar { it.uppercase() }}")
                    val statusColor = if (currentLevel > 0) "<green>" else "<gray>"
                    lore(
                        " <gray>Current Level: $statusColor$currentLevel",
                        " <gray>Max Level: <yellow>$maxLevel",
                        "",
                        if (isMaxed) " <gray>» <red>Max Level Reached" else " <gray>» <yellow>Left-Click <gray>to upgrade",
                        if (currentLevel > 0) " <gray>» <aqua>Right-Click <gray>to downgrade" else " <gray>» <dark_gray>Right-Click to downgrade",
                        ""
                    )
                }) { event ->
                    var newLevel = currentLevel
                    if (event.isLeftClick) {
                        if (newLevel < maxLevel) newLevel++
                    } else if (event.isRightClick) {
                        if (newLevel > 0) newLevel--
                    }

                    if (newLevel != currentLevel) {
                        if (newLevel == 0) {
                            itemInHand.removeEnchantment(enchant)
                        } else {
                            // Using addUnsafeEnchantment to allow levels above natural limits
                            itemInHand.addUnsafeEnchantment(enchant, newLevel)
                        }
                        // Refresh GUI to show updated levels
                        open(player, page)
                    }
                }
            }

            // Put the held item in the top middle for visual flair
            setItem(4, itemInHand.clone()) { /* Prevent grab */ }

            if (page > 0) {
                setItem(48, item(Material.ARROW) {
                    name("<light_purple>Previous Page")
                }) { open(player, page - 1) }
            }

            // Back button to Kit Hub
            setItem(49, item(Material.BARRIER) {
                name("<red>Back to Hub")
            }) { plugin.kitHubGUI.open(player) }

            if (page < totalPages - 1) {
                setItem(50, item(Material.ARROW) {
                    name("<light_purple>Next Page")
                }) { open(player, page + 1) }
            }
        }
    }
}
