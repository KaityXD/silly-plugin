package com.kaity.server.command

import com.kaity.server.SillyPlugin
import com.kaity.server.util.command
import com.kaity.server.util.msg
import org.bukkit.entity.Player

object SocialCommands : CommandModule {
    override fun SillyPlugin.register() {
        command("msg", "Send a private message", aliases = listOf("w", "tell", "whisper")) {
            val p = player ?: return@command
            val target = playerArg(0)
            if (target == null) {
                p.msg("<red>Player not found.")
                return@command
            }
            val message = args.drop(1).joinToString(" ")
            if (message.isEmpty()) {
                p.msg("<red>Please enter a message.")
                return@command
            }
            // chatManager.sendPrivateMessage(p, target, message) // Need to check if this exists or use a different method
            // For now, let's assume chatManager has it or we just use msg
            p.msg("<gold>[To ${target.name}] <white>$message")
            target.msg("<gold>[From ${p.name}] <white>$message")
        }

        command("reply", "Reply to the last person who messaged you", aliases = listOf("r")) {
            val p = player ?: return@command
            p.msg("<red>Reply is currently disabled.")
        }

        command("ignore", "Ignore or unignore a player", permission = "sillyplugin.ignore") {
            val p = player ?: return@command
            val target = playerArg(0)
            if (target == null) {
                p.msg("<red>Player not found.")
                return@command
            }
            val user = userManager.getUser(p) ?: return@command
            if (user.ignoredUsers.contains(target.uniqueId)) {
                user.ignoredUsers.remove(target.uniqueId)
                p.msg("<green>No longer ignoring <white>${target.name}</white>.")
            } else {
                user.ignoredUsers.add(target.uniqueId)
                p.msg("<green>Ignoring <white>${target.name}</white>.")
            }
        }

        command("socialspy", "Toggle social spy", permission = "sillyplugin.socialspy") {
            val p = player ?: return@command
            p.msg("<red>Social spy is currently disabled.")
        }
    }
}
