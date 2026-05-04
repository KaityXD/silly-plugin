package com.kaity.server.command

import com.kaity.server.SillyPlugin
import com.kaity.server.util.command
import com.kaity.server.util.msg
import org.bukkit.entity.Player

object HomeCommands : CommandModule {
    override fun SillyPlugin.register() {
        command("sethome", "Set a home") {
            val p = player ?: return@command
            val name = arg(0) ?: "home"
            homeManager.setHome(p, name)
        }

        command("home", "Teleport to a home") {
            val p = player ?: return@command
            val name = arg(0)
            if (name == null) {
                homeGUI.open(p)
                return@command
            }
            homeManager.teleportHome(p, name)
        }

        command("delhome", "Delete a home", permission = "sillyplugin.home.delete") {
            val p = player ?: return@command
            val name = arg(0) ?: "home"
            homeManager.deleteHome(p, name)
        }

        command("homes", "List your homes") {
            val p = player ?: return@command
            homeGUI.open(p)
        }
    }
}
