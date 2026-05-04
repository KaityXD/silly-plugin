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

        command("timezone", "Set your timezone") {
            val p = player ?: return@command
            val user = userManager.getUser(p) ?: return@command
            
            if (args.isEmpty()) {
                p.msg("<yellow>Your current timezone is: <white>${user.timezone}</white>")
                p.msg("<gray>Use <white>/timezone <ZoneID></white> to change it. (Example: <white>America/New_York</white>)")
                return@command
            }

            val zoneId = args[0]
            try {
                java.time.ZoneId.of(zoneId)
                user.timezone = zoneId
                p.msg("<green>Your timezone has been set to: <white>$zoneId</white>")
                userManager.saveUser(user)
            } catch (e: Exception) {
                p.msg("<red>Invalid timezone ID! <gray>Search online for 'Java ZoneId list' to find yours.")
            }
        }.suggest { java.time.ZoneId.getAvailableZoneIds().toList() }

        if (config.getBoolean("enderchest.enabled", true)) {
            val ecPerm = config.getString("enderchest.permission", "sillyplugin.enderchest")
            command("enderchest", "Open your enderchest", aliases = listOf("ec"), permission = ecPerm) {
                val p = player ?: return@command
                p.openInventory(p.enderChest)
            }
        }
        
    }
}
