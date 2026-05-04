package com.kaity.server.manager

import com.kaity.server.SillyPlugin
import com.kaity.server.util.msg
import org.bukkit.entity.Player

class HomeManager(val plugin: SillyPlugin) {
    fun setHome(player: Player, name: String) {
        val user = plugin.userManager.getUser(player) ?: return
        plugin.userManager.setHome(user, name, player.location)
        player.msg("<gray>Home <gold>$name <gray>has been set.")
    }

    fun deleteHome(player: Player, name: String) {
        val user = plugin.userManager.getUser(player) ?: return
        if (user.homes.containsKey(name)) {
            plugin.userManager.removeHome(user, name)
            player.msg("<gray>Home <gold>$name <gray>has been deleted.")
        } else {
            player.msg("<red>Home <gold>$name <red>does not exist.")
        }
    }

    fun teleportHome(player: Player, name: String) {
        val user = plugin.userManager.getUser(player) ?: return
        val home = user.homes[name]
        if (home != null) {
            player.teleportAsync(home.location).thenAccept {
                player.msg("<gray>Teleported to home <gold>$name<gray>.")
            }
        } else {
            player.msg("<red>Home <gold>$name <red>does not exist.")
        }
    }
}
