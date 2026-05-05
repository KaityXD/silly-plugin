package com.kaity.server.manager

import com.kaity.server.SillyPlugin
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.permissions.PermissionAttachment
import java.util.UUID

class PermissionManager(private val plugin: SillyPlugin) : Listener {

    private val attachments = mutableMapOf<UUID, PermissionAttachment>()
    private val testAsMap = mutableMapOf<UUID, UUID>()
    private val wasOpMap = mutableMapOf<UUID, Boolean>()

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
        // Setup attachments for currently online players (if reload)
        for (player in plugin.server.onlinePlayers) {
            setupPermissions(player)
        }
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        setupPermissions(event.player)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        removeAttachment(event.player)
        stopTestingAs(event.player)
    }

    fun reloadPermissions(player: Player) {
        removeAttachment(player)
        setupPermissions(player)
    }

    fun getPrefix(uuid: UUID): String {
        return getHighestGroup(uuid)?.prefix ?: ""
    }

    fun getSuffix(uuid: UUID): String {
        return getHighestGroup(uuid)?.suffix ?: ""
    }

    private fun getHighestGroup(uuid: UUID): GroupData? {
        val targetUuid = testAsMap[uuid] ?: uuid
        val directGroups = plugin.userManager.database.getUserGroups(targetUuid)
        
        // Find all groups including inheritance
        val allGroups = mutableSetOf<String>()
        val queue = ArrayDeque<String>(directGroups)
        while (queue.isNotEmpty()) {
            val group = queue.removeFirst()
            if (allGroups.add(group)) {
                queue.addAll(plugin.userManager.database.getGroupInheritance(group))
            }
        }

        return allGroups.mapNotNull { plugin.userManager.database.getGroup(it) }
            .maxByOrNull { it.weight }
    }

    private fun setupPermissions(player: Player) {
        val attachment = player.addAttachment(plugin)
        attachments[player.uniqueId] = attachment

        val targetUuid = testAsMap[player.uniqueId] ?: player.uniqueId

        // Gather all groups recursively
        val directGroups = plugin.userManager.database.getUserGroups(targetUuid)
        val allGroups = mutableSetOf<String>()
        val queue = ArrayDeque<String>(directGroups)
        while (queue.isNotEmpty()) {
            val group = queue.removeFirst()
            if (allGroups.add(group)) {
                queue.addAll(plugin.userManager.database.getGroupInheritance(group))
            }
        }

        // Sort groups by weight ascending, so higher weight overrides lower
        val sortedGroups = allGroups.mapNotNull { plugin.userManager.database.getGroup(it) }
            .sortedBy { it.weight }

        // Apply group permissions
        for (group in sortedGroups) {
            val groupPerms = plugin.userManager.database.getGroupPermissions(group.name)
            for (perm in groupPerms) {
                applyPermissionNode(attachment, perm)
            }
        }

        // Apply user specific permissions (overrides groups)
        val userPerms = plugin.userManager.database.getUserPermissions(targetUuid)
        for (perm in userPerms) {
            applyPermissionNode(attachment, perm)
        }
    }

    private fun applyPermissionNode(attachment: PermissionAttachment, node: String) {
        val isNegative = node.startsWith("-")
        val actualNode = if (isNegative) node.substring(1) else node
        val value = !isNegative

        if (actualNode == "*") {
            // Give all registered permissions explicitly
            plugin.server.pluginManager.permissions.forEach { perm ->
                attachment.setPermission(perm.name, value)
            }
            attachment.setPermission("*", value)
        } else if (actualNode.endsWith(".*")) {
            val prefix = actualNode.removeSuffix(".*").lowercase()
            plugin.server.pluginManager.permissions.forEach { perm ->
                if (perm.name.lowercase().startsWith(prefix)) {
                    attachment.setPermission(perm.name, value)
                }
            }
            attachment.setPermission(actualNode, value)
        } else {
            attachment.setPermission(actualNode, value)
        }
    }

    private fun removeAttachment(player: Player) {
        val attachment = attachments.remove(player.uniqueId)
        if (attachment != null) {
            try {
                player.removeAttachment(attachment)
            } catch (e: Exception) {
                plugin.logger.warning("Failed to remove attachment for ${player.name}: ${e.message}")
            }
        }
    }

    fun hasPermissionStored(uuid: UUID, permission: String): Boolean {
        return plugin.userManager.database.getUserPermissions(uuid).contains(permission)
    }

    fun addPermission(uuid: UUID, permission: String) {
        plugin.userManager.database.addUserPermission(uuid, permission)
        val player = plugin.server.getPlayer(uuid)
        if (player != null) {
            // Update anyone testing as this player too
            plugin.server.onlinePlayers.filter { testAsMap[it.uniqueId] == uuid || it.uniqueId == uuid }.forEach {
                attachments[it.uniqueId]?.setPermission(permission, true)
                it.recalculatePermissions()
            }
        }
    }

    fun removePermission(uuid: UUID, permission: String) {
        plugin.userManager.database.removeUserPermission(uuid, permission)
        val player = plugin.server.getPlayer(uuid)
        if (player != null) {
            // Update anyone testing as this player too
            plugin.server.onlinePlayers.filter { testAsMap[it.uniqueId] == uuid || it.uniqueId == uuid }.forEach {
                attachments[it.uniqueId]?.unsetPermission(permission)
                it.recalculatePermissions()
            }
        }
    }

    fun getPermissions(uuid: UUID): List<String> {
        return plugin.userManager.database.getUserPermissions(uuid)
    }

    fun startTestingAs(tester: Player, target: Player) {
        wasOpMap[tester.uniqueId] = tester.isOp
        if (tester.isOp) tester.isOp = false
        testAsMap[tester.uniqueId] = target.uniqueId
        reloadPermissions(tester)
    }

    fun stopTestingAs(tester: Player) {
        if (testAsMap.containsKey(tester.uniqueId)) {
            testAsMap.remove(tester.uniqueId)
            val wasOp = wasOpMap.remove(tester.uniqueId) ?: false
            if (wasOp) tester.isOp = true
            reloadPermissions(tester)
        }
    }

    fun isTestingAs(tester: Player): Boolean {
        return testAsMap.containsKey(tester.uniqueId)
    }

    fun getTestingAs(tester: Player): UUID? {
        return testAsMap[tester.uniqueId]
    }
}

