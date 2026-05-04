package com.kaity.server.user

import org.bukkit.Location
import java.util.UUID

class ServerUser(val id: UUID, var name: String) {
    val homes: MutableMap<String, Home> = mutableMapOf()
    var autoAcceptTPA: Boolean = false
    var showJoinQuitMessages: Boolean = true
    var flightMode: Boolean = false
    var commandFeedback: Boolean = true
    
    // Stats
    var kills: Int = 0
    var deaths: Int = 0
    var playtime: Long = 0
    var firstJoin: String = java.time.LocalDateTime.now().toString()
    
    // Social
    val ignoredUsers: MutableSet<UUID> = mutableSetOf()
    var lastMessaged: UUID? = null
    var lastJoin: Long = System.currentTimeMillis()

    fun addHome(name: String, location: Location) {
        homes[name] = Home(name, location)
    }

    fun removeHome(name: String) {
        homes.remove(name)
    }

    fun setAutoAccept(value: Boolean) {
        autoAcceptTPA = value
    }
}
