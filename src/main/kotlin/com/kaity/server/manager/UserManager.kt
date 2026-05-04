package com.kaity.server.manager

import com.kaity.server.SillyPlugin
import com.kaity.server.user.ServerUser
import com.kaity.server.util.Messenger
import com.kaity.server.util.msgRaw
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.*

class UserManager(val plugin: SillyPlugin) : Listener {
    private val users = mutableMapOf<UUID, ServerUser>()
    val database = DatabaseManager(plugin)

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
        Bukkit.getOnlinePlayers().forEach { player ->
            users[player.uniqueId] = loadUser(player)
        }
    }

    fun getUser(uuid: UUID): ServerUser? = users[uuid]
    fun getUser(player: Player): ServerUser? = users[player.uniqueId]
    fun getOnlineUsers(): Collection<ServerUser> = users.values

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        val user = loadUser(player)
        users[player.uniqueId] = user
        
        // Apply flight mode
        if (player.gameMode == org.bukkit.GameMode.SURVIVAL || player.gameMode == org.bukkit.GameMode.ADVENTURE) {
            player.allowFlight = user.flightMode
        }
        
        if (!user.showJoinQuitMessages) {
            event.joinMessage(null)
        }
        

        player.playSound(player.location, org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        val player = event.player
        val user = users[player.uniqueId]
        if (user != null) {
            if (!user.showJoinQuitMessages) {
                event.quitMessage(null)
            }
            // Update playtime
            user.playtime += System.currentTimeMillis() - user.lastJoin
            saveUser(user)
        }
        users.remove(player.uniqueId)
    }

    @EventHandler
    fun onGameModeChange(event: org.bukkit.event.player.PlayerGameModeChangeEvent) {
        val player = event.player
        val user = users[player.uniqueId] ?: return
        val newMode = event.newGameMode
        if (newMode == org.bukkit.GameMode.SURVIVAL || newMode == org.bukkit.GameMode.ADVENTURE) {
            // Apply their flight setting when entering survival/adventure
            player.scheduler.runDelayed(plugin, { _ ->
                player.allowFlight = user.flightMode
            }, null, 1L)
        }
    }

    private fun loadUser(player: Player): ServerUser {
        val data = database.getUser(player.uniqueId)
        return if (data != null) {
            ServerUser(player.uniqueId, data.name).apply {
                autoAcceptTPA = data.autoAcceptTPA
                showJoinQuitMessages = data.showJoinQuit
                flightMode = data.flightMode
                commandFeedback = data.commandFeedback
                kills = data.kills
                deaths = data.deaths
                playtime = data.playtime
                if (data.firstJoin.isNotEmpty()) {
                    firstJoin = data.firstJoin
                }
                homes.putAll(data.homes)
            }
        } else {
            val newUser = ServerUser(player.uniqueId, player.name)
            saveUser(newUser) // Save immediately to initialize firstJoin in DB
            newUser
        }
    }

    fun saveUser(user: ServerUser) {
        database.insertOrUpdateUser(user.id, user.name, user.autoAcceptTPA, user.showJoinQuitMessages, user.flightMode, user.commandFeedback, user.kills, user.deaths, user.playtime, user.firstJoin)
    }

    fun saveAll() {
        users.values.forEach { user ->
            // Update playtime for online users
            user.playtime += System.currentTimeMillis() - user.lastJoin
            user.lastJoin = System.currentTimeMillis()
            saveUser(user)
        }
    }

    fun setHome(user: ServerUser, name: String, location: Location) {
        user.addHome(name, location)
        database.setHome(user.id, name, location)
    }

    fun removeHome(user: ServerUser, name: String) {
        user.removeHome(name)
        database.removeHome(user.id, name)
    }
}
