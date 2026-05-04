package com.kaity.server.command

import com.kaity.server.SillyPlugin
import com.kaity.server.util.command
import com.kaity.server.util.msg
import org.bukkit.Material

object EnchantCommands : CommandModule {
    override fun SillyPlugin.register() {

        command("enchantmenu", "Open the enchantment menu for your held item", permission = "sillyplugin.enchant.admin") {
            val p = player ?: return@command
            if (p.inventory.itemInMainHand.type == Material.AIR) {
                p.msg("<red>You must be holding an item!")
                return@command
            }
            enchantGUI.open(p)
        }

        command("itemeditor", "Open the item editor for your held item", permission = "sillyplugin.editor.admin") {
            val p = player ?: return@command
            if (p.inventory.itemInMainHand.type == Material.AIR) {
                p.msg("<red>You must be holding an item!")
                return@command
            }
            itemEditorGUI.open(p)
        }
    }
}
