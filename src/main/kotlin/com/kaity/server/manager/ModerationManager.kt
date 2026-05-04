package com.kaity.server.manager

import com.kaity.server.SillyPlugin
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.profile.PlayerProfile
import java.util.*

class ModerationManager(val plugin: SillyPlugin) {

    private val frozenPlayers = mutableSetOf<UUID>()
    private val mutedPlayers = mutableMapOf<UUID, Long>() // UUID to expiry timestamp (-1 for permanent)

    fun kickPlayer(target: Player, reason: String = "Kicked by a moderator") {
        target.kick(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize("&c&lKICKED\n\n&7Reason: &f$reason"))
    }

    fun banPlayer(targetName: String, reason: String = "Banned by a moderator", duration: Long = -1) {
        val expiry = if (duration == -1L) null else Date(System.currentTimeMillis() + duration)
        
        val targetPlayer = Bukkit.getPlayer(targetName)
        val profile = targetPlayer?.playerProfile ?: Bukkit.createProfile(targetName)
        
        val banList = Bukkit.getBanList<org.bukkit.BanList<org.bukkit.profile.PlayerProfile>>(org.bukkit.BanList.Type.PROFILE)
        banList.addBan(profile, reason, expiry, "Moderator")
        
        val component = createBanMessage(reason, expiry)
        targetPlayer?.kick(component)
    }

    fun createBanMessage(reason: String?, expiry: Date?): net.kyori.adventure.text.Component {
        val durationStr = if (expiry == null) "Permanent" else formatDuration(expiry.time - System.currentTimeMillis())
        val actualReason = reason ?: "Banned by a moderator"
        
        val banMessage = """
            &c&l&m      &r &c&lPUNISHMENT &c&l&m      &r
            
            &7You have been &c&lBANNED &7from the server.
            
            &e&lReason: &f$actualReason
            &e&lDuration: &f$durationStr
            
            &7If you believe this was a mistake,
            &7please appeal on our discord.
            
            &c&l&m      &r &c&l&m            &r &c&l&m      &r
        """.trimIndent()

        return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(banMessage)
    }

    fun unbanPlayer(targetName: String): Boolean {
        var unbanned = false
        val profileBanList = Bukkit.getBanList<org.bukkit.BanList<org.bukkit.profile.PlayerProfile>>(org.bukkit.BanList.Type.PROFILE)
        
        // 1. Iterate and pardon by name match (most robust for console/name-only input)
        for (entry in profileBanList.banEntries) {
            val targetProfile: Any? = entry.target
            if (targetProfile is org.bukkit.profile.PlayerProfile) {
                if (targetProfile.name?.equals(targetName, ignoreCase = true) == true) {
                    profileBanList.pardon(targetProfile)
                    unbanned = true
                }
            }
        }

        // 2. Try pardon by OfflinePlayer profile (covers UUID matches if player played before)
        val offline = Bukkit.getOfflinePlayer(targetName)
        if (profileBanList.isBanned(offline.playerProfile)) {
            profileBanList.pardon(offline.playerProfile)
            unbanned = true
        }

        // 3. Try pardon by name-only profile (covers cases where UUID wasn't recorded)
        val nameOnlyProfile = Bukkit.createProfile(targetName)
        if (profileBanList.isBanned(nameOnlyProfile)) {
            profileBanList.pardon(nameOnlyProfile)
            unbanned = true
        }

        // 4. Try legacy Name Ban List (just in case)
        try {
            @Suppress("DEPRECATION")
            val nameBanList = Bukkit.getBanList<org.bukkit.BanList<Any>>(org.bukkit.BanList.Type.NAME)
            val entries = nameBanList.banEntries
            for (entry in entries) {
                if (entry.target.toString().equals(targetName, ignoreCase = true)) {
                    nameBanList.pardon(entry.target)
                    unbanned = true
                }
            }
        } catch (e: Exception) {}

        // 5. Try IP Ban List (in case the targetName is an IP or was used as an IP target)
        try {
            val ipBanList = Bukkit.getBanList<org.bukkit.BanList<Any>>(org.bukkit.BanList.Type.IP)
            val entries = ipBanList.banEntries
            for (entry in entries) {
                if (entry.target.toString().equals(targetName, ignoreCase = true)) {
                    ipBanList.pardon(entry.target)
                    unbanned = true
                }
            }
        } catch (e: Exception) {}

        return unbanned
    }

    fun getBannedPlayers(): List<org.bukkit.BanEntry<*>> {
        val banList = Bukkit.getBanList<org.bukkit.BanList<org.bukkit.profile.PlayerProfile>>(org.bukkit.BanList.Type.PROFILE)
        return banList.banEntries.toList()
    }

    private fun formatDuration(ms: Long): String {
        val seconds = ms / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days > 0 -> "${days}d ${hours % 24}h"
            hours > 0 -> "${hours}h ${minutes % 60}m"
            minutes > 0 -> "${minutes}m ${seconds % 60}s"
            else -> "${seconds}s"
        }
    }

    fun mutePlayer(uuid: UUID, duration: Long = -1) {
        val expiry = if (duration == -1L) -1L else System.currentTimeMillis() + duration
        mutedPlayers[uuid] = expiry
    }

    fun unmutePlayer(uuid: UUID) {
        mutedPlayers.remove(uuid)
    }

    fun isMuted(uuid: UUID): Boolean {
        val expiry = mutedPlayers[uuid] ?: return false
        if (expiry != -1L && System.currentTimeMillis() > expiry) {
            mutedPlayers.remove(uuid)
            return false
        }
        return true
    }

    fun freezePlayer(uuid: UUID) {
        frozenPlayers.add(uuid)
    }

    fun unfreezePlayer(uuid: UUID) {
        frozenPlayers.remove(uuid)
    }

    fun isFrozen(uuid: UUID): Boolean = frozenPlayers.contains(uuid)

    // TODO: Add persistence for mutes and frozen status if needed.
    // For now, let's keep it simple as requested.
}
