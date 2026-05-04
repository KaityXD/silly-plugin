package com.kaity.server.command

import com.kaity.server.SillyPlugin
import com.kaity.server.manager.TPARequestType
import com.kaity.server.util.Suggest
import com.kaity.server.util.command
import com.kaity.server.util.msg

object TPACommands : CommandModule {
    override fun SillyPlugin.register() {
        command("tpa", "Request to teleport to a player") {
            val p = player ?: return@command
            val target = playerArg(0)
            if (target == null) {
                playerSelectorGUI.open(p) { selected ->
                    p.performCommand("tpa ${selected.name}")
                }
                return@command
            }
            if (p == target) {
                p.msg("<red>You cannot TPA to yourself.")
                return@command
            }
            tpaManager.sendRequest(p, target, TPARequestType.TPA)
        }.suggest { com.kaity.server.util.Suggest.players(it) }

        command("requests", "Manage your teleport requests", aliases = listOf("tpahere")) {
            val p = player ?: return@command
            val target = playerArg(0)
            
            // If it's used as /tpahere with a target
            if (label.equals("tpahere", true) && target != null) {
                if (p == target) {
                    p.msg("<red>You cannot TPAHere to yourself.")
                    return@command
                }
                tpaManager.sendRequest(p, target, TPARequestType.TPAHERE)
                return@command
            }

            // Otherwise open GUI
            tpaGUI.open(p)
        }.suggest { com.kaity.server.util.Suggest.players(it) }

        command("tpaccept", "Accept a teleport request", aliases = listOf("tpyes")) {
            val p = player ?: return@command
            tpaManager.acceptRequest(p)
        }

        command("tpdeny", "Deny a teleport request", aliases = listOf("tpno")) {
            val p = player ?: return@command
            tpaManager.denyRequest(p)
        }

        command("tpcancel", "Cancel your outgoing teleport request", permission = "sillyplugin.tpa.cancel") {
            val p = player ?: return@command
            tpaManager.cancelRequest(p)
        }
    }
}
