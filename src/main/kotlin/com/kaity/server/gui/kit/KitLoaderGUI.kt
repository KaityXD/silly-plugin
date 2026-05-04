package com.kaity.server.gui.kit

import com.kaity.server.SillyPlugin
import com.kaity.server.util.item
import com.kaity.server.util.openGui
import org.bukkit.Material
import org.bukkit.entity.Player

class KitLoaderGUI(val plugin: SillyPlugin) {

    fun open(player: Player, page: Int = 0) {
        val kits = plugin.userManager.database.getAllKits().sortedBy { it.name }
        val totalPages = if (kits.isEmpty()) 1 else (kits.size - 1) / 28 + 1
        
        player.openGui("<gradient:#8E2DE2:#F093FB><bold>Kit Loader <gray>(${page + 1}/$totalPages)", 6) {
            fillBorder(item(Material.BLACK_STAINED_GLASS_PANE) { name(" ") })

            val start = page * 28
            val end = (start + 28).coerceAtMost(kits.size)
            
            // Define playable slots (10-16, 19-25, 28-34, 37-43)
            val slots = listOf(
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
            )

            for (i in start until end) {
                val kit = kits[i]
                
                val hasPerm = player.hasPermission("kit.${kit.name}") || player.isOp

                val lastClaimed = plugin.userManager.database.getKitCooldown(player.uniqueId, kit.name)
                val now = System.currentTimeMillis() / 1000
                val timeSinceClaim = now - lastClaimed
                val onCooldown = !player.isOp && timeSinceClaim < kit.cooldownSeconds
                
                val statusString = if (!hasPerm) {
                    "<red>Locked (No Permission)"
                } else if (onCooldown) {
                    val remaining = kit.cooldownSeconds - timeSinceClaim
                    "<red>On Cooldown: ${formatTime(remaining)}"
                } else {
                    "<green>Available"
                }

                val mat = if (hasPerm && !onCooldown) Material.CHEST else if (!hasPerm) Material.BARRIER else Material.ENDER_CHEST

                setItem(slots[i - start], item(mat) {
                    name("<gradient:#8E2DE2:#F093FB><bold>${kit.name.replaceFirstChar { it.uppercase() }}")
                    lore(
                        " <gray>Status: $statusString",
                        " <gray>Cooldown: <yellow>${formatTime(kit.cooldownSeconds)}",
                        "",
                        " <gray>» <yellow>Left-Click <gray>to claim",
                        " <gray>» <aqua>Right-Click <gray>to preview",
                        ""
                    )
                }) { event ->
                    if (event.isRightClick) {
                        plugin.kitPreviewGUI.open(player, kit.name, this@KitLoaderGUI)
                    } else {
                        plugin.kitManager.claimKit(player, kit.name)
                        open(player, page)
                    }
                }
            }

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

    private fun formatTime(seconds: Long): String {
        val d = seconds / 86400
        val h = (seconds % 86400) / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60

        val parts = mutableListOf<String>()
        if (d > 0) parts.add("${d}d")
        if (h > 0) parts.add("${h}h")
        if (m > 0) parts.add("${m}m")
        if (s > 0) parts.add("${s}s")
        return if (parts.isEmpty()) "0s" else parts.joinToString(" ")
    }
}
