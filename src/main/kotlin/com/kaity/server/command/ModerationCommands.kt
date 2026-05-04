package com.kaity.server.command

import com.kaity.server.SillyPlugin
import com.kaity.server.util.command
import com.kaity.server.util.msg
import org.bukkit.Bukkit

object ModerationCommands : CommandModule {
    override fun SillyPlugin.register() {
        command("mod", "Open the moderation menu", aliases = listOf("moderation")) {
            val p = player ?: return@command
            if (!p.hasPermission("server.moderation")) {
                p.msg("<red>You do not have permission to use this command.")
                return@command
            }

            if (args.isEmpty()) {
                playerSelectorGUI.open(p, guiContext = "moderation", onSelect = { target ->
                    moderationGUI.open(p, target)
                })
            } else {
                val target = Bukkit.getPlayer(args[0])
                if (target == null) {
                    p.msg("<red>Player not found.")
                    return@command
                }
                moderationGUI.open(p, target)
            }
        }.suggest { com.kaity.server.util.Suggest.players(it) }

        command("invsee", "View and edit a player's inventory", permission = "sillyplugin.invsee") {
            val p = player ?: return@command
            if (!p.hasPermission("server.moderation")) {
                p.msg("<red>You do not have permission to use this command.")
                return@command
            }

            if (args.isEmpty()) {
                p.msg("<red>Usage: /invsee <player>")
                return@command
            }

            val target = Bukkit.getPlayer(args[0])
            if (target == null) {
                p.msg("<red>Player not found.")
                return@command
            }
            com.kaity.server.gui.admin.PlayerInventoryGUI(this@register, target).open(p)
        }.suggest { com.kaity.server.util.Suggest.players(it) }

        command("unban", "Unban a player", aliases = listOf("pardon")) {
            if (!sender.hasPermission("server.moderation")) {
                sender.msg("<red>You do not have permission to use this command.")
                return@command
            }

            if (args.isEmpty()) {
                sender.msg("<red>Usage: /unban <player>")
                return@command
            }

            val targetName = args[0]
            val success = moderationManager.unbanPlayer(targetName)
            if (success) {
                sender.msg("<green>Unbanned $targetName.")
            } else {
                sender.msg("<red>Player $targetName is not banned.")
            }
        }.suggest { com.kaity.server.util.Suggest.players(it) }
    }
}
