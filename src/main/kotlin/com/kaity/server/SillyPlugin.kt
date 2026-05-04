package com.kaity.server

import com.kaity.server.command.*
import com.kaity.server.gui.hub.*
import com.kaity.server.gui.kit.*
import com.kaity.server.gui.editor.*
import com.kaity.server.gui.admin.*
import com.kaity.server.gui.user.*
import com.kaity.server.manager.*
import com.kaity.server.util.GuiListener
import com.kaity.server.util.command
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

/**
 * Main plugin entry point. 
 * Handles manager/GUI setup and registers commands automatically.
 */
class SillyPlugin : JavaPlugin() {

    // --- System Managers ---
    lateinit var userManager: UserManager
    lateinit var tpaManager: TPAManager
    lateinit var homeManager: HomeManager
    lateinit var statsManager: StatsManager
    lateinit var chatManager: ChatManager
    lateinit var kitManager: KitManager
    lateinit var permissionManager: PermissionManager
    lateinit var afkManager: AFKManager
    lateinit var moderationManager: ModerationManager
    lateinit var displayManager: DisplayManager

    // --- GUI Menus ---
    lateinit var homeGUI: HomeGUI
    lateinit var settingsGUI: SettingsGUI
    lateinit var tpaGUI: TPAGUI
    lateinit var mainGUI: MainGUI
    lateinit var profileGUI: ProfileGUI
    lateinit var kitHubGUI: KitHubGUI
    lateinit var kitGUI: KitLoaderGUI
    lateinit var kitPreviewGUI: KitPreviewGUI
    lateinit var enchantGUI: EnchantGUI
    lateinit var itemEditorGUI: ItemEditorGUI
    lateinit var permissionGUI: PermissionGUI
    lateinit var addPermissionGUI: AddPermissionGUI
    lateinit var playerSelectorGUI: PlayerSelectorGUI
    lateinit var groupManagementGUI: GroupManagementGUI
    lateinit var groupDetailGUI: GroupDetailGUI
    lateinit var addGroupPermissionGUI: AddGroupPermissionGUI
    lateinit var moderationGUI: ModerationGUI
    lateinit var banListGUI: BanListGUI

    override fun onEnable() {
        saveDefaultConfig()
        
        // Initializing managers in specific order to avoid dependency issues
        userManager       = UserManager(this)
        permissionManager = PermissionManager(this)
        kitManager        = KitManager(this)
        tpaManager        = TPAManager(this)
        homeManager       = HomeManager(this)
        statsManager      = StatsManager(this)
        chatManager       = ChatManager(this)
        afkManager        = AFKManager(this)
        moderationManager = ModerationManager(this)

        // Core events & Input handlers
        server.pluginManager.registerEvents(GuiListener(), this)
        server.pluginManager.registerEvents(com.kaity.server.util.AnvilInputUI, this)
        server.pluginManager.registerEvents(com.kaity.server.util.KitEnderchestListener(this, kitManager), this)
        server.pluginManager.registerEvents(com.kaity.server.util.ModerationListener(this), this)
        
        com.kaity.server.util.NightGuiManager.register(this)

        // Pre-instantiating GUIs to keep things responsive
        homeGUI           = HomeGUI(this)
        settingsGUI       = SettingsGUI(this)
        tpaGUI            = TPAGUI(this)
        mainGUI           = MainGUI(this)
        profileGUI        = ProfileGUI(this)
        kitHubGUI         = KitHubGUI(this)
        kitGUI            = KitLoaderGUI(this)
        kitPreviewGUI     = KitPreviewGUI(this)
        enchantGUI        = EnchantGUI(this)
        itemEditorGUI     = ItemEditorGUI(this)
        permissionGUI     = PermissionGUI(this)
        addPermissionGUI  = AddPermissionGUI(this)
        playerSelectorGUI = PlayerSelectorGUI(this)
        groupManagementGUI = GroupManagementGUI(this)
        groupDetailGUI = GroupDetailGUI(this)
        addGroupPermissionGUI = AddGroupPermissionGUI(this)
        moderationGUI = ModerationGUI(this)
        banListGUI = BanListGUI(this)

        // Scan the package for CommandModules so we don't have to register every single command manually
        registerAllCommands()
    }

    override fun onDisable() {
        // Shutdown logic: save all pending user data to DB
        if (::userManager.isInitialized) {
            userManager.saveAll()
            userManager.database.close()
        }
    }

    /**
     * Uses reflection to find and register all objects that implement CommandModule.
     * Works both in JAR and IDE environments.
     */
    private fun registerAllCommands() {
        val packageName = "com.kaity.server.command"
        val path = packageName.replace('.', '/')
        val jarFile = File(this.javaClass.protectionDomain.codeSource.location.toURI())

        if (jarFile.isFile) {
            java.util.jar.JarFile(jarFile).use { jar ->
                val entries = jar.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val name = entry.name
                    if (name.startsWith(path) && name.endsWith(".class") && !name.contains('$')) {
                        val className = name.replace('/', '.').removeSuffix(".class")
                        try {
                            val clazz = Class.forName(className)
                            if (CommandModule::class.java.isAssignableFrom(clazz) && clazz != CommandModule::class.java) {
                                val instance = clazz.getField("INSTANCE").get(null) as? CommandModule
                                instance?.apply { register() }
                            }
                        } catch (e: Exception) {
                            // Skip classes that can't be loaded or aren't objects
                        }
                    }
                }
            }
        } else {
            // Fallback for development environment (IDE)
            val resources = this.javaClass.classLoader.getResources(path)
            while (resources.hasMoreElements()) {
                val resource = resources.nextElement()
                val file = File(resource.toURI())
                file.listFiles()?.forEach {
                    if (it.name.endsWith(".class")) {
                        val className = "$packageName.${it.name.removeSuffix(".class")}"
                        try {
                            val clazz = Class.forName(className)
                            if (CommandModule::class.java.isAssignableFrom(clazz) && clazz != CommandModule::class.java) {
                                val instance = clazz.getField("INSTANCE").get(null) as? CommandModule
                                instance?.apply { register() }
                            }
                        } catch (e: Exception) {}
                    }
                }
            }
        }
    }
}
