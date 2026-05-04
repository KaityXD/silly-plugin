package com.kaity.server.manager

import com.kaity.server.SillyPlugin
import com.kaity.server.util.ItemSerialization
import com.kaity.server.util.msg
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class KitManager(private val plugin: SillyPlugin) {

    fun createKit(player: Player, name: String, cooldownSeconds: Long) {
        val contents = player.inventory.contents
        val base64 = ItemSerialization.itemStackArrayToBase64(contents)
        plugin.userManager.database.saveKit(name.lowercase(), cooldownSeconds, base64)
        player.msg("<green>Kit '<white>$name</white>' has been created with a ${cooldownSeconds}s cooldown.")
    }

    fun deleteKit(player: Player, name: String) {
        val kitName = name.lowercase()
        val kit = plugin.userManager.database.getKit(kitName)
        if (kit == null) {
            player.msg("<red>Kit '$name' does not exist.")
            return
        }
        plugin.userManager.database.deleteKit(kitName)
        player.msg("<green>Kit '$name' deleted.")
    }

    fun claimKit(player: Player, name: String) {
        val kitName = name.lowercase()
        val kit = plugin.userManager.database.getKit(kitName)
        if (kit == null) {
            player.msg("<red>Kit '$name' does not exist.")
            return
        }

        // Check permission: default to requiring `kit.<name>`
        val permNode = "kit.$kitName"
        if (!player.hasPermission(permNode) && !player.isOp) {
            player.msg("<red>You do not have permission to claim this kit (<gray>$permNode</gray>).")
            return
        }

        // Check cooldown
        val lastClaimed = plugin.userManager.database.getKitCooldown(player.uniqueId, kitName)
        val now = System.currentTimeMillis() / 1000
        val timeSinceClaim = now - lastClaimed
        if (!player.isOp && timeSinceClaim < kit.cooldownSeconds) {
            val remaining = kit.cooldownSeconds - timeSinceClaim
            val timeString = formatTime(remaining)
            player.msg("<red>You must wait <yellow>$timeString</yellow> before claiming this kit again.")
            return
        }

        val items = ItemSerialization.itemStackArrayFromBase64(kit.base64Items)
        val leftover = mutableListOf<ItemStack>()
        for (item in items) {
            if (item != null) {
                val dropped = player.inventory.addItem(item)
                if (dropped.isNotEmpty()) {
                    leftover.addAll(dropped.values)
                }
            }
        }

        for (drop in leftover) {
            player.world.dropItemNaturally(player.location, drop)
        }

        // Load kit-specific enderchest
        loadKitEnderchest(player, kitName)

        plugin.userManager.database.setKitCooldown(player.uniqueId, kitName, now)
        player.msg("<green>You have claimed the '<white>$name</white>' kit!")
        player.msg("<gray>Your kit-specific enderchest has been loaded.")
        if (leftover.isNotEmpty()) {
            player.msg("<yellow>Some items were dropped on the ground because your inventory is full.")
        }
    }

    fun saveKitEnderchest(player: Player, kitName: String) {
        val base64 = ItemSerialization.itemStackArrayToBase64(player.enderChest.contents)
        plugin.userManager.database.saveKitEnderchest(player.uniqueId, kitName, base64)
    }

    fun loadKitEnderchest(player: Player, kitName: String) {
        val base64 = plugin.userManager.database.getKitEnderchest(player.uniqueId, kitName)
        if (base64 != null) {
            val items = ItemSerialization.itemStackArrayFromBase64(base64)
            player.enderChest.contents = items
        } else {
            player.enderChest.clear()
        }
    }

    fun updateKitLayout(player: Player, kitName: String) {
        val contents = player.inventory.contents
        val base64 = ItemSerialization.itemStackArrayToBase64(contents)
        val kit = plugin.userManager.database.getKit(kitName.lowercase()) ?: return
        plugin.userManager.database.saveKit(kitName.lowercase(), kit.cooldownSeconds, base64)
        player.msg("<green>Layout for kit '<white>$kitName</white>' has been updated.")
    }

    fun getKitItems(name: String): Array<ItemStack?>? {
        val kit = plugin.userManager.database.getKit(name.lowercase()) ?: return null
        return ItemSerialization.itemStackArrayFromBase64(kit.base64Items)
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
