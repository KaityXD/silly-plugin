package com.kaity.server.util

import com.kaity.server.SillyPlugin
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.Bukkit
import org.bukkit.profile.PlayerProfile

class ModerationListener(val plugin: SillyPlugin) : Listener {

    @EventHandler
    fun onPreLogin(event: AsyncPlayerPreLoginEvent) {
        val profile = Bukkit.createProfile(event.uniqueId, event.name)
        
        @Suppress("UNCHECKED_CAST")
        val banList = Bukkit.getBanList(org.bukkit.BanList.Type.PROFILE) as org.bukkit.BanList<PlayerProfile>
        
        val banEntry = banList.getBanEntry(profile)
        if (banEntry != null) {
            val component = plugin.moderationManager.createBanMessage(banEntry.reason, banEntry.expiration)
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, component)
        }
    }

    @EventHandler
    fun onChat(event: AsyncPlayerChatEvent) {
        if (plugin.moderationManager.isMuted(event.player.uniqueId)) {
            event.isCancelled = true
            event.player.msg("<red>You are currently muted and cannot speak.")
        }
    }

    @EventHandler
    fun onMove(event: PlayerMoveEvent) {
        if (plugin.moderationManager.isFrozen(event.player.uniqueId)) {
            val from = event.from
            val to = event.to ?: return
            if (from.x != to.x || from.z != to.z) {
                event.isCancelled = true
                event.player.msg("<red>You are frozen and cannot move!")
            }
        }
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        if (plugin.moderationManager.isFrozen(event.player.uniqueId)) {
            event.isCancelled = true
            event.player.msg("<red>You are frozen and cannot interact!")
        }
    }
}
