package com.kaity.server.command

import com.kaity.server.SillyPlugin
import com.kaity.server.util.command

object PermissionCommands : CommandModule {
    override fun SillyPlugin.register() {
        command("perms", "Open the Permission Manager Player Selector", permission = "sillyplugin.admin") {
            val p = player ?: return@command
            playerSelectorGUI.open(p) { target ->
                permissionGUI.open(p, target)
            }
        }
        
        command("groups", "Open the Group Management GUI", permission = "sillyplugin.admin") {
            val p = player ?: return@command
            groupManagementGUI.open(p)
        }
    }
}

