package com.kaity.server.manager

import com.kaity.server.SillyPlugin
import com.kaity.server.util.msg
import org.bukkit.entity.Player
import java.util.*

enum class TPARequestType {
    TPA, TPAHERE
}

data class TPARequest(
    val sender: UUID,
    val target: UUID,
    val type: TPARequestType,
    val timestamp: Long = System.currentTimeMillis()
)

class TPAManager(val plugin: SillyPlugin) {
    private val requests = mutableMapOf<UUID, TPARequest>()
    private val outgoing = mutableMapOf<UUID, TPARequest>()
    private val TIMEOUT = 60 * 1000L

    fun sendRequest(sender: Player, target: Player, type: TPARequestType) {
        val request = TPARequest(sender.uniqueId, target.uniqueId, type)
        requests[target.uniqueId] = request
        outgoing[sender.uniqueId] = request
        
        val typeStr = if (type == TPARequestType.TPA) "to teleport to you" else "for you to teleport to them"
        target.msg("<gold>${sender.name} <gray>has requested $typeStr.")
        target.msg("<gray>Click to: <green><bold><click:run_command:/tpaccept>[ACCEPT]</click> <red><bold><click:run_command:/tpdeny>[DENY]</click>")
        sender.msg("<gray>TPA request sent to <gold>${target.name}<gray>. Type <gold>/tpcancel <gray>to cancel.")
    }

    fun getRequest(target: Player): TPARequest? {
        val request = requests[target.uniqueId] ?: return null
        if (System.currentTimeMillis() - request.timestamp > TIMEOUT) {
            requests.remove(target.uniqueId)
            outgoing.remove(request.sender)
            return null
        }
        return request
    }

    fun acceptRequest(target: Player) {
        val request = getRequest(target) ?: run {
            target.msg("<red>You have no pending TPA requests.")
            return
        }
        val sender = plugin.server.getPlayer(request.sender)
        
        if (sender == null) {
            target.msg("<red>The requester is no longer online.")
        } else {
            when (request.type) {
                TPARequestType.TPA -> sender.teleportAsync(target.location).thenAccept {
                    target.msg("<gray>Teleporting...")
                    sender.msg("<gray>Teleporting...")
                }
                TPARequestType.TPAHERE -> target.teleportAsync(sender.location).thenAccept {
                    target.msg("<gray>Teleporting...")
                    sender.msg("<gray>Teleporting...")
                }
            }
        }
        
        requests.remove(target.uniqueId)
        outgoing.remove(request.sender)
    }

    fun denyRequest(target: Player) {
        val request = getRequest(target) ?: run {
            target.msg("<red>You have no pending TPA requests.")
            return
        }
        val sender = plugin.server.getPlayer(request.sender)
        sender?.msg("<red>${target.name} denied your TPA request.")
        target.msg("<gray>Denied TPA request from <gold>${sender?.name ?: "Unknown"}<gray>.")
        
        requests.remove(target.uniqueId)
        outgoing.remove(request.sender)
    }
    
    fun cancelRequest(sender: Player) {
        val request = outgoing[sender.uniqueId] ?: run {
            sender.msg("<red>You have no outgoing TPA requests.")
            return
        }
        requests.remove(request.target)
        outgoing.remove(sender.uniqueId)
        sender.msg("<gray>Cancelled TPA request.")
    }
}
