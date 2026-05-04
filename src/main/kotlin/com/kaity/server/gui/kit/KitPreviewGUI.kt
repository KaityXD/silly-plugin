package com.kaity.server.gui.kit

import com.kaity.server.SillyPlugin
import com.kaity.server.util.item
import com.kaity.server.util.msg
import com.kaity.server.util.openGui
import org.bukkit.Material
import org.bukkit.entity.Player

class KitPreviewGUI(val plugin: SillyPlugin) {

    fun open(player: Player, kitName: String, parentGui: KitLoaderGUI) {
        val kitItems = plugin.kitManager.getKitItems(kitName)
        if (kitItems == null) {
            player.msg("<red>Kit not found.")
            return
        }

        player.openGui("<gradient:#8E2DE2:#F093FB><bold>Preview: ${kitName.replaceFirstChar { it.uppercase() }}", 6) {
            
            for (i in kitItems.indices) {
                if (i < 45) {
                    val item = kitItems[i]
                    if (item != null && item.type != Material.AIR) {
                        setItem(i, item) { /* Prevent grabbing */ }
                    }
                }
            }

            for (i in 45..53) {
                setItem(i, item(Material.BLACK_STAINED_GLASS_PANE) { name(" ") }) {}
            }

            setItem(49, item(Material.ARROW) {
                name("<red>Back to Kit Loader")
            }) { parentGui.open(player) }
        }
    }
}
