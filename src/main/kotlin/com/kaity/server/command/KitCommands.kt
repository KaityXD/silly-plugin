package com.kaity.server.command

import com.kaity.server.SillyPlugin
import com.kaity.server.util.command
import com.kaity.server.util.msg

object KitCommands : CommandModule {
    override fun SillyPlugin.register() {

        command("kit", "Open the Kit Management Hub", permission = "sillyplugin.kit") {
            val p = player ?: return@command
            kitHubGUI.open(p)
        }

        command("kits", "Open the Kit Loader GUI", permission = "sillyplugin.kit") {
            val p = player ?: return@command
            kitGUI.open(p)
        }

        command("createkit", "Create a new kit from your inventory", permission = "sillyplugin.kit.admin") {
            val p = player ?: return@command
            val name = arg(0)
            if (name == null) {
                p.msg("<red>Usage: /createkit <name>")
                return@command
            }
            kitManager.createKit(p, name, 3600) // Default 1h cooldown
        }

        command("deletekit", "Delete a kit", permission = "sillyplugin.kit.admin") {
            val p = player ?: return@command
            val name = arg(0)
            if (name == null) {
                p.msg("<red>Usage: /deletekit <name>")
                return@command
            }
            kitManager.deleteKit(p, name)
        }
    }
}
