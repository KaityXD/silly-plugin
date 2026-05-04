package com.kaity.server.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.command.CommandSender

object Messenger {
    private val mm = MiniMessage.miniMessage()
    
    // Standard Prefix
    private const val PREFIX = "<gray>[<gradient:#9863E7:#E43A96>Silly</gradient>]</gray> "

    // Parses a string into a Component with MiniMessage
    fun parse(text: String): Component = mm.deserialize(text)
    
    // Parses a string with the prefix and sends it
    fun send(sender: CommandSender, text: String) {
        sender.sendMessage(parse("$PREFIX<reset>$text"))
    }
    
    // Raw send without prefix (for multi-line or special messages)
    fun sendRaw(sender: CommandSender, text: String) {
        sender.sendMessage(parse(text))
    }
    
    fun parseLore(lines: List<String>): List<Component> = lines.map { parse(it) }
}

// Global extension functions for easy use
fun CommandSender.msg(text: String) = Messenger.send(this, text)
fun CommandSender.msgRaw(text: String) = Messenger.sendRaw(this, text)
