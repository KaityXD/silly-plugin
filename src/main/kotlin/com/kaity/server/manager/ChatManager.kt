package com.kaity.server.manager

import com.kaity.server.SillyPlugin
import com.kaity.server.util.Messenger
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Formats the chat and handles ignores.
 */
class ChatManager(val plugin: SillyPlugin) : Listener {

    private val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    /**
     * Custom chat formatter.
     * Grabs the player's top group and parses their prefix/suffix with MiniMessage.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onChat(event: AsyncChatEvent) {
        val player = event.player
        val message = event.message()
        val senderUser = plugin.userManager.getUser(player)
        
        // We cancel the event to use our own broadcasting logic
        event.isCancelled = true

        val userGroups = plugin.userManager.database.getUserGroups(player.uniqueId)
        val groupsList = mutableListOf<com.kaity.server.manager.GroupData>()
        for (groupId in userGroups) {
            val group = plugin.userManager.database.getGroup(groupId)
            if (group != null) {
                groupsList.add(group)
            }
        }
        groupsList.sortByDescending { it.weight }
        
        val primaryGroup = groupsList.firstOrNull()
        val prefixText = primaryGroup?.prefix ?: ""
        val suffixText = primaryGroup?.suffix ?: ""
        
        val prefixStr = if (prefixText.isNotEmpty()) "$prefixText " else ""
        val suffixStr = if (suffixText.isNotEmpty()) " $suffixText" else ""
        
        // Parsing the entire name string at once allows prefixes to style the username
        val displayNameStr = if (prefixText.isEmpty() && suffixText.isEmpty()) {
            "<gray>${player.name}"
        } else {
            "$prefixStr${player.name}$suffixStr"
        }

        val nameComponent = Messenger.parse(displayNameStr)
            .hoverEvent(HoverEvent.showText(Messenger.parse("<gray>Click to view <gradient:#8E2DE2:#F093FB>${player.name}'s</gradient> profile.")))
            .clickEvent(ClickEvent.runCommand("/profile ${player.name}"))

        // Add timestamp and extra info to the message hover
        val zoneId = try {
            ZoneId.of(senderUser?.timezone ?: "UTC")
        } catch (e: Exception) {
            ZoneId.of("UTC")
        }
        val currentTime = ZonedDateTime.now(zoneId).format(timeFormatter)
        val messageHover = Messenger.parse(
            "<dark_gray>▪</dark_gray> <gray>Sent at:</gray> <white>$currentTime</white> <gray>(${zoneId.id})</gray><newline>" +
            "<dark_gray>▪</dark_gray> <gray>Ping:</gray> <white>${player.ping}ms</white>"
        )
        val messageComponent = message.hoverEvent(HoverEvent.showText(messageHover))

        // Format: [Prefix] Name [Suffix] » Message
        val format = Component.text()
            .append(nameComponent)
            .append(Messenger.parse(" <dark_gray>» <white>"))
            .append(messageComponent)
            .build()

        // Send to everyone who isn't ignoring the sender
        plugin.server.onlinePlayers.forEach { recipient ->
            val user = plugin.userManager.getUser(recipient)
            if (user != null && !user.ignoredUsers.contains(player.uniqueId)) {
                recipient.sendMessage(format)
            }
        }
        
        plugin.server.consoleSender.sendMessage(format)
    }
}

