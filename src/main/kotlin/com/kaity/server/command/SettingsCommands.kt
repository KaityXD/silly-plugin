package com.kaity.server.command

import com.kaity.server.SillyPlugin
import com.kaity.server.util.command

object SettingsCommands : CommandModule {
    override fun SillyPlugin.register() {
        command("settings", "Open settings GUI", permission = "sillyplugin.settings") {
            player?.let { settingsGUI.open(it) }
        }
    }
}
