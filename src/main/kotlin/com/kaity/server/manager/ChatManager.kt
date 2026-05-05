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

import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.JoinConfiguration
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Formats the chat and handles ignores.
 */
class ChatManager(val plugin: SillyPlugin) : Listener {

    private val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")
    private val sharedItems = ConcurrentHashMap<String, ItemStack>()
    private val sharedInvs = ConcurrentHashMap<String, Array<ItemStack?>>()
    private val sharedEchests = ConcurrentHashMap<String, Array<ItemStack?>>()
    private val sharedItemOwners = ConcurrentHashMap<String, String>()

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
        
        // Cleanup old shared items every 10 minutes to prevent memory leak
        Bukkit.getAsyncScheduler().runAtFixedRate(plugin, { _: ScheduledTask ->
            sharedItems.clear()
            sharedInvs.clear()
            sharedEchests.clear()
            sharedItemOwners.clear()
        }, 10L, 10L, TimeUnit.MINUTES)
    }

    /**
     * Custom chat formatter.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onChat(event: AsyncChatEvent) {
        val player = event.player
        val message = event.message()
        val senderUser = plugin.userManager.getUser(player)
        
        event.isCancelled = true

        val prefixText = plugin.permissionManager.getPrefix(player.uniqueId)
        val suffixText = plugin.permissionManager.getSuffix(player.uniqueId)
        val prefixStr = if (prefixText.isNotEmpty()) "$prefixText " else ""
        val suffixStr = if (suffixText.isNotEmpty()) " $suffixText" else ""
        
        val displayNameStr = if (prefixText.isEmpty() && suffixText.isEmpty()) {
            "<gray>${player.name}"
        } else {
            "$prefixStr${player.name}$suffixStr"
        }

        // --- ENHANCED NAME HOVER (STAT CARD) ---
        val user = plugin.userManager.getUser(player)
        val playtimeMs = if (user != null) user.playtime + (System.currentTimeMillis() - user.lastJoin) else 0L
        val seconds = playtimeMs / 1000
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val playtimeStr = "${hours}h ${minutes}m"
        
        val statCard = Messenger.parse(
            "<gradient:#8E2DE2:#F093FB><bold>${player.name}'s Profile</bold></gradient><newline>" +
            "<dark_gray>▪</dark_gray> <gray>Kills:</gray> <white>${user?.kills ?: 0}</white><newline>" +
            "<dark_gray>▪</dark_gray> <gray>Deaths:</gray> <white>${user?.deaths ?: 0}</white><newline>" +
            "<dark_gray>▪</dark_gray> <gray>Playtime:</gray> <white>$playtimeStr</white><newline><newline>" +
            "<yellow>Click to open full profile!</yellow>"
        )

        val nameComponent = Messenger.parse(displayNameStr)
            .hoverEvent(HoverEvent.showText(statCard))
            .clickEvent(ClickEvent.runCommand("/profile ${player.name}"))

        val messageStrPlain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(message)
        val mentionSound = org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP

        // --- SHARING DETECTION ---
        var formattedMessageStr = messageStrPlain
        
        // [item]
        if (formattedMessageStr.contains("[item]", ignoreCase = true)) {
            val item = player.inventory.itemInMainHand
            if (item.type != org.bukkit.Material.AIR) {
                val shareId = UUID.randomUUID().toString().substring(0, 8)
                sharedItems[shareId] = item.clone()
                sharedItemOwners[shareId] = player.name
                formattedMessageStr = formattedMessageStr.replace("[item]", "ITEM_SHARE_$shareId", ignoreCase = true)
            }
        }

        // [inv]
        if (formattedMessageStr.contains("[inv]", ignoreCase = true)) {
            val shareId = UUID.randomUUID().toString().substring(0, 8)
            val pInv = player.inventory
            val items = arrayOfNulls<ItemStack>(54)
            items[0] = pInv.helmet?.clone()
            items[1] = pInv.chestplate?.clone()
            items[2] = pInv.leggings?.clone()
            items[3] = pInv.boots?.clone()
            items[5] = pInv.itemInOffHand?.clone()
            for (i in 9..35) items[i] = pInv.getItem(i)?.clone()
            for (i in 0..8) items[i + 36] = pInv.getItem(i)?.clone()
            sharedInvs[shareId] = items
            sharedItemOwners[shareId] = player.name
            formattedMessageStr = formattedMessageStr.replace("[inv]", "INV_SHARE_$shareId", ignoreCase = true)
        }

        // [echest]
        if (formattedMessageStr.contains("[echest]", ignoreCase = true)) {
            val shareId = UUID.randomUUID().toString().substring(0, 8)
            val items = arrayOfNulls<ItemStack>(27)
            for (i in 0 until 27) items[i] = player.enderChest.getItem(i)?.clone()
            sharedEchests[shareId] = items
            sharedItemOwners[shareId] = player.name
            formattedMessageStr = formattedMessageStr.replace("[echest]", "ECHEST_SHARE_$shareId", ignoreCase = true)
        }

        // Handle Mentions
        val onlinePlayers = plugin.server.onlinePlayers.map { it.name }
        var mentionedPlayers = mutableListOf<org.bukkit.entity.Player>()
        for (playerName in onlinePlayers) {
            val mentionTag = "@$playerName"
            if (formattedMessageStr.contains(mentionTag, ignoreCase = true)) {
                formattedMessageStr = formattedMessageStr.replace(mentionTag, "<gradient:#9863E7:#E43A96>$mentionTag</gradient>", ignoreCase = true)
                val mentionedPlayer = plugin.server.getPlayer(playerName)
                if (mentionedPlayer != null) mentionedPlayers.add(mentionedPlayer)
            }
        }
        
        // Re-parse the message with MiniMessage
        var finalMessageComponent = Messenger.parse(formattedMessageStr)
        
        // --- POST-PROCESS FOR CLICK EVENTS ---
        
        // Item Replacements
        finalMessageComponent = finalMessageComponent.replaceText(net.kyori.adventure.text.TextReplacementConfig.builder()
            .match("ITEM_SHARE_([a-z0-9]+)")
            .replacement { result, _ ->
                val shareId = result.group(1)
                val item = sharedItems[shareId] ?: return@replacement Component.text("[Expired Item]")
                val owner = sharedItemOwners[shareId] ?: "Unknown"
                val itemName = if (item.hasItemMeta() && item.itemMeta.hasDisplayName()) {
                    item.itemMeta.displayName()!!
                } else {
                    Component.text(item.type.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() })
                }
                
                Component.text().append(Component.text("[").color(net.kyori.adventure.text.format.NamedTextColor.GOLD).decorate(net.kyori.adventure.text.format.TextDecoration.BOLD))
                    .append(itemName)
                    .append(Component.text("]").color(net.kyori.adventure.text.format.NamedTextColor.GOLD).decorate(net.kyori.adventure.text.format.TextDecoration.BOLD))
                    .hoverEvent(item.asHoverEvent())
                    .clickEvent(ClickEvent.callback { viewer: Audience -> if (viewer is Player) plugin.itemViewerGUI.open(viewer, owner, item) })
                    .build()
            }.build())

        // Inventory Replacements
        finalMessageComponent = finalMessageComponent.replaceText(net.kyori.adventure.text.TextReplacementConfig.builder()
            .match("INV_SHARE_([a-z0-9]+)")
            .replacement { result, _ ->
                val shareId = result.group(1)
                val items = sharedInvs[shareId] ?: return@replacement Component.text("[Expired Inv]")
                val owner = sharedItemOwners[shareId] ?: "Unknown"
                Component.text("[$owner's Inventory]").color(net.kyori.adventure.text.format.NamedTextColor.AQUA).decorate(net.kyori.adventure.text.format.TextDecoration.BOLD)
                    .hoverEvent(HoverEvent.showText(Component.text("Click to view $owner's Inventory")))
                    .clickEvent(ClickEvent.callback { viewer: Audience -> if (viewer is Player) plugin.snapshotViewerGUI.openInventory(viewer as Player, owner, items) })
            }.build())

        // E-Chest Replacements
        finalMessageComponent = finalMessageComponent.replaceText(net.kyori.adventure.text.TextReplacementConfig.builder()
            .match("ECHEST_SHARE_([a-z0-9]+)")
            .replacement { result, _ ->
                val shareId = result.group(1)
                val items = sharedEchests[shareId] ?: return@replacement Component.text("[Expired E-Chest]")
                val owner = sharedItemOwners[shareId] ?: "Unknown"
                Component.text("[$owner's Ender Chest]").color(net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE).decorate(net.kyori.adventure.text.format.TextDecoration.BOLD)
                    .hoverEvent(HoverEvent.showText(Component.text("Click to view $owner's Ender Chest")))
                    .clickEvent(ClickEvent.callback { viewer: Audience -> if (viewer is Player) plugin.snapshotViewerGUI.openEnderChest(viewer as Player, owner, items) })
            }.build())

        // Add timestamp and extra info to the message hover
        val zoneId = try { ZoneId.of(senderUser?.timezone ?: "UTC") } catch (e: Exception) { ZoneId.of("UTC") }
        val currentTime = ZonedDateTime.now(zoneId).format(timeFormatter)
        val messageHover = Messenger.parse("<dark_gray>▪</dark_gray> <gray>Sent at:</gray> <white>$currentTime</white> <gray>(${zoneId.id})</gray><newline><dark_gray>▪</dark_gray> <gray>Ping:</gray> <white>${player.ping}ms</white>")
        val messageComponent = finalMessageComponent.hoverEvent(HoverEvent.showText(messageHover))

        val format = Component.text().append(nameComponent).append(Messenger.parse(" <dark_gray>» <white>")).append(messageComponent).build()

        plugin.server.onlinePlayers.forEach { recipient ->
            val recipientUser = plugin.userManager.getUser(recipient)
            if (recipientUser != null && !recipientUser.ignoredUsers.contains(player.uniqueId)) {
                recipient.sendMessage(format)
                if (mentionedPlayers.contains(recipient)) recipient.playSound(recipient.location, mentionSound, 1.0f, 1.0f)
            }
        }
        plugin.server.consoleSender.sendMessage(format)
    }
}
