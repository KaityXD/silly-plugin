package com.kaity.server.command

import com.kaity.server.SillyPlugin
import com.kaity.server.util.command
import com.kaity.server.util.msg


object GeneralCommands : CommandModule {
    override fun SillyPlugin.register() {
        command("profile", "Open your profile", permission = "sillyplugin.profile") {
            val p = player ?: return@command
            val user = userManager.getUser(p) ?: return@command
            profileGUI.open(p, user)
        }

        command("afk", "Toggle your AFK status") {
            val p = player ?: return@command
            afkManager.setAFK(p, !afkManager.isAFK(p))
        }

        command("heal", "heal yourself", permission = "sillyplugin.heal") {
          val target = player ?: return@command
          target.health = target.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH)?.value ?: 20.0
          target.msg("<green>Healed ${target.name}.")
        }.suggest { com.kaity.server.util.Suggest.players(it) }

        command("feed", "feed yourself", permission = "sillyplugin.feed") {
          val target = player ?: return@command
          target.foodLevel = 20
          target.msg("<green>Fed ${target.name}.")
        }.suggest { com.kaity.server.util.Suggest.players(it) }

        if (config.getBoolean("enderchest.enabled", true)) {
            val ecPerm = config.getString("enderchest.permission", "sillyplugin.enderchest")
            command("enderchest", "Open your enderchest", aliases = listOf("ec"), permission = ecPerm) {
                val p = player ?: return@command
                p.openInventory(p.enderChest)
            }
        }
        
    }
}
