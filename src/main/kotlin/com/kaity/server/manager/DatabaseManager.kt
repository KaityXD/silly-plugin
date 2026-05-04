package com.kaity.server.manager

import com.kaity.server.SillyPlugin
import com.kaity.server.user.Home
import org.bukkit.Location
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Handles all SQLite persistence.
 * Uses a single connection with a ReentrantLock for thread safety.
 */
class DatabaseManager(private val plugin: SillyPlugin) {

    private val dbFile = plugin.dataFolder.resolve("database.db")
    private val url get() = "jdbc:sqlite:${dbFile.absolutePath}"
    private val lock = ReentrantLock()

    // Lazy reconnect — always returns a live connection
    private var _connection: Connection? = null
    private val connection: Connection
        get() = lock.withLock {
            val conn = _connection
            return if (conn != null && !conn.isClosed) {
                conn
            } else {
                plugin.logger.info("[DB] (Re)connecting to database...")
                DriverManager.getConnection(url).also { newConn ->
                    newConn.createStatement().use { it.execute("PRAGMA journal_mode=WAL") }
                    newConn.createStatement().use { it.execute("PRAGMA synchronous=NORMAL") }
                    newConn.createStatement().use { it.execute("PRAGMA foreign_keys=ON") }
                    _connection = newConn
                }
            }
        }

    init {
        plugin.dataFolder.mkdirs()
        initSchema()
    }

    private fun initSchema() = safe("init schema") {
        connection.createStatement().use { stmt ->
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    uuid TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    auto_accept_tpa INTEGER NOT NULL DEFAULT 0,
                    show_join_quit INTEGER NOT NULL DEFAULT 1,
                    flight_mode INTEGER NOT NULL DEFAULT 0,
                    command_feedback INTEGER NOT NULL DEFAULT 1,
                    kills INTEGER NOT NULL DEFAULT 0,
                    deaths INTEGER NOT NULL DEFAULT 0,
                    playtime INTEGER NOT NULL DEFAULT 0,
                    first_join TEXT NOT NULL DEFAULT ''
                )
            """.trimIndent())

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS homes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_uuid TEXT NOT NULL,
                    name TEXT NOT NULL,
                    world TEXT NOT NULL,
                    x REAL NOT NULL,
                    y REAL NOT NULL,
                    z REAL NOT NULL,
                    yaw REAL NOT NULL,
                    pitch REAL NOT NULL,
                    created_at TEXT NOT NULL DEFAULT '',
                    FOREIGN KEY (user_uuid) REFERENCES users (uuid) ON DELETE CASCADE,
                    UNIQUE (user_uuid, name)
                )
            """.trimIndent())

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS kits (
                    name TEXT PRIMARY KEY,
                    cooldown_seconds INTEGER NOT NULL,
                    base64_items TEXT NOT NULL
                )
            """.trimIndent())

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS player_kit_cooldowns (
                    user_uuid TEXT NOT NULL,
                    kit_name TEXT NOT NULL,
                    claimed_at INTEGER NOT NULL,
                    PRIMARY KEY (user_uuid, kit_name),
                    FOREIGN KEY (user_uuid) REFERENCES users (uuid) ON DELETE CASCADE,
                    FOREIGN KEY (kit_name) REFERENCES kits (name) ON DELETE CASCADE
                )
            """.trimIndent())

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS user_permissions (
                    user_uuid TEXT NOT NULL,
                    permission TEXT NOT NULL,
                    PRIMARY KEY (user_uuid, permission),
                    FOREIGN KEY (user_uuid) REFERENCES users (uuid) ON DELETE CASCADE
                )
            """.trimIndent())

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS groups (
                    name TEXT PRIMARY KEY,
                    weight INTEGER NOT NULL DEFAULT 0,
                    prefix TEXT NOT NULL DEFAULT '',
                    suffix TEXT NOT NULL DEFAULT ''
                )
            """.trimIndent())

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS group_permissions (
                    group_name TEXT NOT NULL,
                    permission TEXT NOT NULL,
                    PRIMARY KEY (group_name, permission),
                    FOREIGN KEY (group_name) REFERENCES groups (name) ON DELETE CASCADE
                )
            """.trimIndent())

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS group_inheritance (
                    parent_group TEXT NOT NULL,
                    child_group TEXT NOT NULL,
                    PRIMARY KEY (parent_group, child_group),
                    FOREIGN KEY (parent_group) REFERENCES groups (name) ON DELETE CASCADE,
                    FOREIGN KEY (child_group) REFERENCES groups (name) ON DELETE CASCADE
                )
            """.trimIndent())

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS user_groups (
                    user_uuid TEXT NOT NULL,
                    group_name TEXT NOT NULL,
                    PRIMARY KEY (user_uuid, group_name),
                    FOREIGN KEY (user_uuid) REFERENCES users (uuid) ON DELETE CASCADE,
                    FOREIGN KEY (group_name) REFERENCES groups (name) ON DELETE CASCADE
                )
            """.trimIndent())

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS kit_enderchests (
                    user_uuid TEXT NOT NULL,
                    kit_name TEXT NOT NULL,
                    base64_items TEXT NOT NULL,
                    PRIMARY KEY (user_uuid, kit_name),
                    FOREIGN KEY (user_uuid) REFERENCES users (uuid) ON DELETE CASCADE,
                    FOREIGN KEY (kit_name) REFERENCES kits (name) ON DELETE CASCADE
                )
            """.trimIndent())

            migrateTable(stmt, "users", mapOf(
                "auto_accept_tpa"  to "INTEGER NOT NULL DEFAULT 0",
                "show_join_quit"   to "INTEGER NOT NULL DEFAULT 1",
                "flight_mode"      to "INTEGER NOT NULL DEFAULT 0",
                "command_feedback" to "INTEGER NOT NULL DEFAULT 1",
                "kills"            to "INTEGER NOT NULL DEFAULT 0",
                "deaths"           to "INTEGER NOT NULL DEFAULT 0",
                "playtime"         to "INTEGER NOT NULL DEFAULT 0",
                "first_join"       to "TEXT NOT NULL DEFAULT ''"
            ))

            migrateTable(stmt, "homes", mapOf(
                "created_at" to "TEXT NOT NULL DEFAULT ''"
            ))
            
            migrateTable(stmt, "groups", mapOf(
                "prefix" to "TEXT NOT NULL DEFAULT ''",
                "suffix" to "TEXT NOT NULL DEFAULT ''"
            ))
        }
    }

    private fun migrateTable(stmt: java.sql.Statement, table: String, columns: Map<String, String>) {
        val existing = mutableSetOf<String>()
        stmt.executeQuery("PRAGMA table_info($table)").use { rs ->
            while (rs.next()) existing.add(rs.getString("name").lowercase())
        }
        for ((col, type) in columns) {
            if (col.lowercase() in existing) continue
            try {
                stmt.execute("ALTER TABLE $table ADD COLUMN $col $type")
                plugin.logger.info("[DB] Added column '$col' to '$table'.")
            } catch (e: SQLException) {
                plugin.logger.warning("[DB] Could not add '$col' to '$table': ${e.message}")
            }
        }
    }

    // ─── Core helpers ─────────────────────────────────────────────────────────

    /**
     * Executes [block] inside the DB lock, logs any exception, and returns null on failure.
     * This prevents any single bad query from blowing up the caller's thread.
     */
    private inline fun <T> safe(operation: String, block: () -> T): T? {
        return try {
            lock.withLock { block() }
        } catch (e: Exception) {
            plugin.logger.severe("[DB] Error during '$operation': ${e.javaClass.simpleName} — ${e.message}")
            null
        }
    }

    fun close() {
        lock.withLock {
            try {
                _connection?.close()
                _connection = null
            } catch (e: Exception) {
                plugin.logger.warning("[DB] Error closing connection: ${e.message}")
            }
        }
    }

    // ─── User operations ──────────────────────────────────────────────────────

    fun insertOrUpdateUser(
        uuid: UUID, name: String,
        autoAcceptTPA: Boolean, showJoinQuit: Boolean,
        flightMode: Boolean, commandFeedback: Boolean,
        kills: Int, deaths: Int, playtime: Long, firstJoin: String
    ) = safe("save user $name") {
        connection.prepareStatement("""
            INSERT INTO users (uuid, name, auto_accept_tpa, show_join_quit, flight_mode, command_feedback, kills, deaths, playtime, first_join)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(uuid) DO UPDATE SET
                name             = excluded.name,
                auto_accept_tpa  = excluded.auto_accept_tpa,
                show_join_quit   = excluded.show_join_quit,
                flight_mode      = excluded.flight_mode,
                command_feedback = excluded.command_feedback,
                kills            = excluded.kills,
                deaths           = excluded.deaths,
                playtime         = excluded.playtime,
                first_join       = CASE WHEN users.first_join = '' OR users.first_join IS NULL THEN excluded.first_join ELSE users.first_join END
        """.trimIndent()).use { ps ->
            ps.setString(1, uuid.toString())
            ps.setString(2, name)
            ps.setInt(3, if (autoAcceptTPA) 1 else 0)
            ps.setInt(4, if (showJoinQuit) 1 else 0)
            ps.setInt(5, if (flightMode) 1 else 0)
            ps.setInt(6, if (commandFeedback) 1 else 0)
            ps.setInt(7, kills)
            ps.setInt(8, deaths)
            ps.setLong(9, playtime)
            ps.setString(10, firstJoin)
            ps.executeUpdate()
        }
    }

    fun getUser(uuid: UUID): UserData? = safe("load user $uuid") {
        connection.prepareStatement(
            "SELECT name, auto_accept_tpa, show_join_quit, flight_mode, command_feedback, kills, deaths, playtime, first_join FROM users WHERE uuid = ?"
        ).use { ps ->
            ps.setString(1, uuid.toString())
            ps.executeQuery().use { rs ->
                if (rs.next()) UserData(
                    uuid            = uuid,
                    name            = rs.getString("name"),
                    autoAcceptTPA   = rs.getInt("auto_accept_tpa") == 1,
                    showJoinQuit    = rs.getInt("show_join_quit") == 1,
                    flightMode      = rs.getInt("flight_mode") == 1,
                    commandFeedback = rs.getInt("command_feedback") == 1,
                    kills           = rs.getInt("kills"),
                    deaths          = rs.getInt("deaths"),
                    playtime        = rs.getLong("playtime"),
                    firstJoin       = rs.getString("first_join"),
                    homes           = getHomesInternal(uuid)
                ) else null
            }
        }
    }

    fun deleteUser(uuid: UUID) = safe("delete user $uuid") {
        connection.prepareStatement("DELETE FROM users WHERE uuid = ?").use { ps ->
            ps.setString(1, uuid.toString())
            ps.executeUpdate()
        }
    }

    // ─── Home operations ──────────────────────────────────────────────────────

    // Internal — called within an already-locked safe block
    private fun getHomesInternal(uuid: UUID): Map<String, Home> {
        val homes = mutableMapOf<String, Home>()
        connection.prepareStatement(
            "SELECT name, world, x, y, z, yaw, pitch, created_at FROM homes WHERE user_uuid = ?"
        ).use { ps ->
            ps.setString(1, uuid.toString())
            ps.executeQuery().use { rs ->
                while (rs.next()) {
                    val world = org.bukkit.Bukkit.getWorld(rs.getString("world")) ?: continue
                    val loc = Location(world, rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"), rs.getFloat("yaw"), rs.getFloat("pitch"))
                    val name = rs.getString("name")
                    val createdAt = runCatching {
                        LocalDateTime.parse(rs.getString("created_at").replace(" ", "T"))
                    }.getOrElse { LocalDateTime.now() }
                    homes[name] = Home(name, loc, createdAt)
                }
            }
        }
        return homes
    }

    fun getHomes(uuid: UUID): Map<String, Home> = safe("load homes $uuid") {
        getHomesInternal(uuid)
    } ?: emptyMap()

    fun setHome(uuid: UUID, name: String, location: Location) = safe("set home '$name' for $uuid") {
        connection.prepareStatement("""
            INSERT INTO homes (user_uuid, name, world, x, y, z, yaw, pitch, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(user_uuid, name) DO UPDATE SET
                world = excluded.world, x = excluded.x, y = excluded.y,
                z = excluded.z, yaw = excluded.yaw, pitch = excluded.pitch
        """.trimIndent()).use { ps ->
            ps.setString(1, uuid.toString())
            ps.setString(2, name)
            ps.setString(3, location.world.name)
            ps.setDouble(4, location.x)
            ps.setDouble(5, location.y)
            ps.setDouble(6, location.z)
            ps.setFloat(7, location.yaw)
            ps.setFloat(8, location.pitch)
            ps.setString(9, LocalDateTime.now().toString())
            ps.executeUpdate()
        }
    }

    fun removeHome(uuid: UUID, name: String) = safe("remove home '$name' for $uuid") {
        connection.prepareStatement("DELETE FROM homes WHERE user_uuid = ? AND name = ?").use { ps ->
            ps.setString(1, uuid.toString())
            ps.setString(2, name)
            ps.executeUpdate()
        }
    }

    // ─── Kit operations ───────────────────────────────────────────────────────

    fun saveKit(name: String, cooldownSeconds: Long, base64Items: String) = safe("save kit $name") {
        connection.prepareStatement("""
            INSERT INTO kits (name, cooldown_seconds, base64_items)
            VALUES (?, ?, ?)
            ON CONFLICT(name) DO UPDATE SET
                cooldown_seconds = excluded.cooldown_seconds,
                base64_items     = excluded.base64_items
        """.trimIndent()).use { ps ->
            ps.setString(1, name)
            ps.setLong(2, cooldownSeconds)
            ps.setString(3, base64Items)
            ps.executeUpdate()
        }
    }

    fun getKit(name: String): KitData? = safe("get kit $name") {
        connection.prepareStatement("SELECT name, cooldown_seconds, base64_items FROM kits WHERE name = ?").use { ps ->
            ps.setString(1, name)
            ps.executeQuery().use { rs ->
                if (rs.next()) KitData(
                    name = rs.getString("name"),
                    cooldownSeconds = rs.getLong("cooldown_seconds"),
                    base64Items = rs.getString("base64_items")
                ) else null
            }
        }
    }

    fun getAllKits(): List<KitData> = safe("get all kits") {
        val kits = mutableListOf<KitData>()
        connection.prepareStatement("SELECT name, cooldown_seconds, base64_items FROM kits").use { ps ->
            ps.executeQuery().use { rs ->
                while (rs.next()) {
                    kits.add(KitData(
                        name = rs.getString("name"),
                        cooldownSeconds = rs.getLong("cooldown_seconds"),
                        base64Items = rs.getString("base64_items")
                    ))
                }
            }
        }
        kits
    } ?: emptyList()

    fun deleteKit(name: String) = safe("delete kit $name") {
        connection.prepareStatement("DELETE FROM kits WHERE name = ?").use { ps ->
            ps.setString(1, name)
            ps.executeUpdate()
        }
    }

    fun getKitCooldown(uuid: UUID, kitName: String): Long = safe("get kit cooldown for $uuid $kitName") {
        connection.prepareStatement("SELECT claimed_at FROM player_kit_cooldowns WHERE user_uuid = ? AND kit_name = ?").use { ps ->
            ps.setString(1, uuid.toString())
            ps.setString(2, kitName)
            ps.executeQuery().use { rs ->
                if (rs.next()) rs.getLong("claimed_at") else 0L
            }
        }
    } ?: 0L

    fun setKitCooldown(uuid: UUID, kitName: String, claimedAt: Long) = safe("set kit cooldown for $uuid $kitName") {
        connection.prepareStatement("""
            INSERT INTO player_kit_cooldowns (user_uuid, kit_name, claimed_at)
            VALUES (?, ?, ?)
            ON CONFLICT(user_uuid, kit_name) DO UPDATE SET
                claimed_at = excluded.claimed_at
        """.trimIndent()).use { ps ->
            ps.setString(1, uuid.toString())
            ps.setString(2, kitName)
            ps.setLong(3, claimedAt)
            ps.executeUpdate()
        }
    }

    // ─── Group operations ─────────────────────────────────────────────────────

    fun getGroups(): List<String> = safe("get all group names") {
        val groups = mutableListOf<String>()
        connection.createStatement().use { stmt ->
            stmt.executeQuery("SELECT name FROM groups ORDER BY weight DESC").use { rs ->
                while (rs.next()) groups.add(rs.getString("name"))
            }
        }
        groups
    } ?: emptyList()

    fun getAllGroupData(): List<GroupData> = safe("get all group data") {
        val groups = mutableListOf<GroupData>()
        connection.createStatement().use { stmt ->
            stmt.executeQuery("SELECT name, weight, prefix, suffix FROM groups ORDER BY weight DESC").use { rs ->
                while (rs.next()) groups.add(GroupData(
                    rs.getString("name"),
                    rs.getInt("weight"),
                    rs.getString("prefix"),
                    rs.getString("suffix")
                ))
            }
        }
        groups
    } ?: emptyList()

    fun getGroup(name: String): GroupData? = safe("get group $name") {
        connection.prepareStatement("SELECT name, weight, prefix, suffix FROM groups WHERE name = ?").use { ps ->
            ps.setString(1, name)
            ps.executeQuery().use { rs ->
                if (rs.next()) GroupData(
                    rs.getString("name"),
                    rs.getInt("weight"),
                    rs.getString("prefix"),
                    rs.getString("suffix")
                ) else null
            }
        }
    }

    fun addGroup(name: String, weight: Int) = safe("add group $name") {
        connection.prepareStatement("INSERT OR IGNORE INTO groups (name, weight, prefix, suffix) VALUES (?, ?, '', '')").use { ps ->
            ps.setString(1, name)
            ps.setInt(2, weight)
            ps.executeUpdate()
        }
    }

    fun updateGroup(name: String, weight: Int, prefix: String, suffix: String) = safe("update group $name") {
        connection.prepareStatement("UPDATE groups SET weight = ?, prefix = ?, suffix = ? WHERE name = ?").use { ps ->
            ps.setInt(1, weight)
            ps.setString(2, prefix)
            ps.setString(3, suffix)
            ps.setString(4, name)
            ps.executeUpdate()
        }
    }

    fun getGroupInheritance(groupName: String): List<String> = safe("get inheritance for group $groupName") {
        val parents = mutableListOf<String>()
        connection.prepareStatement("SELECT parent_group FROM group_inheritance WHERE child_group = ?").use { ps ->
            ps.setString(1, groupName)
            ps.executeQuery().use { rs ->
                while (rs.next()) parents.add(rs.getString("parent_group"))
            }
        }
        parents
    } ?: emptyList()

    fun addGroupInheritance(parentGroup: String, childGroup: String) = safe("add inheritance $parentGroup to $childGroup") {
        connection.prepareStatement("INSERT OR IGNORE INTO group_inheritance (parent_group, child_group) VALUES (?, ?)").use { ps ->
            ps.setString(1, parentGroup)
            ps.setString(2, childGroup)
            ps.executeUpdate()
        }
    }

    fun removeGroupInheritance(parentGroup: String, childGroup: String) = safe("remove inheritance $parentGroup from $childGroup") {
        connection.prepareStatement("DELETE FROM group_inheritance WHERE parent_group = ? AND child_group = ?").use { ps ->
            ps.setString(1, parentGroup)
            ps.setString(2, childGroup)
            ps.executeUpdate()
        }
    }

    fun getGroupPermissions(groupName: String): List<String> = safe("get perms for group $groupName") {
        val perms = mutableListOf<String>()
        connection.prepareStatement("SELECT permission FROM group_permissions WHERE group_name = ?").use { ps ->
            ps.setString(1, groupName)
            ps.executeQuery().use { rs ->
                while (rs.next()) perms.add(rs.getString("permission"))
            }
        }
        perms
    } ?: emptyList()

    fun addGroupPermission(groupName: String, permission: String) = safe("add perm $permission to group $groupName") {
        connection.prepareStatement("INSERT OR IGNORE INTO group_permissions (group_name, permission) VALUES (?, ?)").use { ps ->
            ps.setString(1, groupName)
            ps.setString(2, permission)
            ps.executeUpdate()
        }
    }

    fun removeGroupPermission(groupName: String, permission: String) = safe("remove perm $permission from group $groupName") {
        connection.prepareStatement("DELETE FROM group_permissions WHERE group_name = ? AND permission = ?").use { ps ->
            ps.setString(1, groupName)
            ps.setString(2, permission)
            ps.executeUpdate()
        }
    }

    fun getUserGroups(uuid: UUID): List<String> = safe("get groups for $uuid") {
        val groups = mutableListOf<String>()
        connection.prepareStatement("SELECT group_name FROM user_groups WHERE user_uuid = ?").use { ps ->
            ps.setString(1, uuid.toString())
            ps.executeQuery().use { rs ->
                while (rs.next()) groups.add(rs.getString("group_name"))
            }
        }
        groups
    } ?: emptyList()

    fun addUserToGroup(uuid: UUID, groupName: String) = safe("add $uuid to group $groupName") {
        connection.prepareStatement("INSERT OR IGNORE INTO user_groups (user_uuid, group_name) VALUES (?, ?)").use { ps ->
            ps.setString(1, uuid.toString())
            ps.setString(2, groupName)
            ps.executeUpdate()
        }
    }

    fun removeUserFromGroup(uuid: UUID, groupName: String) = safe("remove $uuid from group $groupName") {
        connection.prepareStatement("DELETE FROM user_groups WHERE user_uuid = ? AND group_name = ?").use { ps ->
            ps.setString(1, uuid.toString())
            ps.setString(2, groupName)
            ps.executeUpdate()
        }
    }

    // ─── Permission operations ────────────────────────────────────────────────

    fun getUserPermissions(uuid: UUID): List<String> = safe("get permissions for $uuid") {
        val perms = mutableListOf<String>()
        connection.prepareStatement("SELECT permission FROM user_permissions WHERE user_uuid = ?").use { ps ->
            ps.setString(1, uuid.toString())
            ps.executeQuery().use { rs ->
                while (rs.next()) perms.add(rs.getString("permission"))
            }
        }
        perms
    } ?: emptyList()

    fun addUserPermission(uuid: UUID, permission: String) = safe("add permission $permission for $uuid") {
        connection.prepareStatement("INSERT OR IGNORE INTO user_permissions (user_uuid, permission) VALUES (?, ?)").use { ps ->
            ps.setString(1, uuid.toString())
            ps.setString(2, permission)
            ps.executeUpdate()
        }
    }

    fun removeUserPermission(uuid: UUID, permission: String) = safe("remove permission $permission for $uuid") {
        connection.prepareStatement("DELETE FROM user_permissions WHERE user_uuid = ? AND permission = ?").use { ps ->
            ps.setString(1, uuid.toString())
            ps.setString(2, permission)
            ps.executeUpdate()
        }
    }

    // ─── Kit Enderchest operations ────────────────────────────────────────────

    fun saveKitEnderchest(uuid: UUID, kitName: String, base64: String) = safe("save kit enderchest $kitName for $uuid") {
        connection.prepareStatement("""
            INSERT INTO kit_enderchests (user_uuid, kit_name, base64_items)
            VALUES (?, ?, ?)
            ON CONFLICT(user_uuid, kit_name) DO UPDATE SET
                base64_items = excluded.base64_items
        """.trimIndent()).use { ps ->
            ps.setString(1, uuid.toString())
            ps.setString(2, kitName.lowercase())
            ps.setString(3, base64)
            ps.executeUpdate()
        }
    }

    fun getKitEnderchest(uuid: UUID, kitName: String): String? = safe("get kit enderchest $kitName for $uuid") {
        connection.prepareStatement("SELECT base64_items FROM kit_enderchests WHERE user_uuid = ? AND kit_name = ?").use { ps ->
            ps.setString(1, uuid.toString())
            ps.setString(2, kitName.lowercase())
            ps.executeQuery().use { rs ->
                if (rs.next()) rs.getString("base64_items") else null
            }
        }
    }
}

data class UserData(
    val uuid: UUID,
    val name: String,
    val autoAcceptTPA: Boolean,
    val showJoinQuit: Boolean,
    val flightMode: Boolean,
    val commandFeedback: Boolean,
    val kills: Int,
    val deaths: Int,
    val playtime: Long,
    val firstJoin: String,
    val homes: Map<String, Home>
)

data class KitData(
    val name: String,
    val cooldownSeconds: Long,
    val base64Items: String
)

data class GroupData(
    val name: String,
    val weight: Int,
    val prefix: String,
    val suffix: String
)
