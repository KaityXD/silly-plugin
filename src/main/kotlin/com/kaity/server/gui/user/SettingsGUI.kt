package com.kaity.server.gui.user

import com.kaity.server.SillyPlugin
import com.kaity.server.util.item
import com.kaity.server.util.openGui
import org.bukkit.Material
import org.bukkit.entity.Player

class SettingsGUI(val plugin: SillyPlugin) {

    fun open(player: Player) {
        val user = plugin.userManager.getUser(player) ?: return

        player.openGui("<gradient:#7F00FF:#E100FF><bold>⚙ Preferences", 4) {

            val glass  = item(Material.GRAY_STAINED_GLASS_PANE)  { name(" ") }
            val purple = item(Material.PURPLE_STAINED_GLASS_PANE) { name(" ") }

            for (i in 0  until 9)  setItem(i, purple)   // header strip
            for (i in 27 until 36) setItem(i, glass)     // footer strip

            // ── Toggles ───────────────────────────────────────────────────────

            toggle(
                slot    = 11,
                label   = "Auto-Accept TPA",
                icon    = Material.ENDER_PEARL,
                desc    = listOf(
                    "<gray>Automatically accepts all incoming",
                    "<gray>teleport requests without a prompt."
                ),
                enabled = user.autoAcceptTPA
            ) {
                user.autoAcceptTPA = !user.autoAcceptTPA
                plugin.userManager.saveUser(user)
                open(player)
            }

            toggle(
                slot    = 13,
                label   = "Join / Quit Alerts",
                icon    = Material.BELL,
                desc    = listOf(
                    "<gray>Shows a notification whenever",
                    "<gray>a player joins or leaves the server."
                ),
                enabled = user.showJoinQuitMessages
            ) {
                user.showJoinQuitMessages = !user.showJoinQuitMessages
                plugin.userManager.saveUser(user)
                open(player)
            }

            toggle(
                slot    = 15,
                label   = "Flight Mode",
                icon    = Material.FEATHER,
                desc    = listOf(
                    "<gray>Allows you to fly freely",
                    "<gray>while in survival mode."
                ),
                enabled = user.flightMode
            ) {
                user.flightMode = !user.flightMode
                if (player.gameMode == org.bukkit.GameMode.SURVIVAL || player.gameMode == org.bukkit.GameMode.ADVENTURE) {
                    player.allowFlight = user.flightMode
                    if (!user.flightMode) player.isFlying = false
                }
                plugin.userManager.saveUser(user)
                open(player)
            }

            toggle(
                slot    = 17,
                label   = "Command Feedback",
                icon    = Material.WRITABLE_BOOK,
                desc    = listOf(
                    "<gray>Receive confirmation messages",
                    "<gray>when executing commands."
                ),
                enabled = user.commandFeedback
            ) {
                user.commandFeedback = !user.commandFeedback
                plugin.userManager.saveUser(user)
                open(player)
            }

            // ── Back ──────────────────────────────────────────────────────────
            setItem(31, item(Material.ARROW) {
                name("<gray>« <white>Back to Menu")
                lore("", "<gray>Return to the main menu.")
            }) { plugin.mainGUI.open(player) }
        }
    }
}
