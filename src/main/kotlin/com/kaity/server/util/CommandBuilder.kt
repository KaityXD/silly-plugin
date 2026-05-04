package com.kaity.server.util

import org.bukkit.Bukkit
import org.bukkit.command.CommandMap
import org.bukkit.command.CommandSender
import org.bukkit.command.defaults.BukkitCommand
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

@DslMarker
annotation class CommandDsl

class CommandContext(
    val sender: CommandSender,
    val label: String,
    val args: Array<out String>
) {
    val player: Player? = sender as? Player
    
    fun arg(index: Int): String? = args.getOrNull(index)
    fun playerArg(index: Int): Player? = arg(index)?.let { Bukkit.getPlayer(it) }
}

@CommandDsl
class CommandBuilder(val name: String) {
    var description: String = ""
    var usage: String = ""
    var aliases: List<String> = emptyList()
    var permission: String? = null
    
    private var executor: (CommandContext) -> Unit = {}
    private var tabCompleter: (CommandContext) -> List<String> = { emptyList() }

    fun execute(block: (CommandContext) -> Unit) {
        executor = block
    }

    fun suggest(block: (CommandContext) -> List<String>) {
        tabCompleter = block
    }

    fun build(plugin: JavaPlugin): BukkitCommand {
        return object : BukkitCommand(name) {
            init {
                this.description = this@CommandBuilder.description
                this.usageMessage = this@CommandBuilder.usage
                this.aliases = this@CommandBuilder.aliases
                this.permission = this@CommandBuilder.permission
            }

            override fun execute(sender: CommandSender, commandLabel: String, args: Array<out String>): Boolean {
                if (!testPermission(sender)) return true
                executor(CommandContext(sender, commandLabel, args))
                return true
            }

            override fun tabComplete(sender: CommandSender, alias: String, args: Array<out String>): List<String> {
                return tabCompleter(CommandContext(sender, alias, args))
            }
        }
    }
}

private val commandMap: CommandMap by lazy {
    val server = Bukkit.getServer()
    val getCommandMapMethod = server.javaClass.getMethod("getCommandMap")
    getCommandMapMethod.invoke(server) as CommandMap
}

fun JavaPlugin.command(name: String, block: CommandBuilder.() -> Unit) {
    val builder = CommandBuilder(name)
    builder.block()
    val command = builder.build(this)
    commandMap.register(this.name, command)
}

fun JavaPlugin.command(
    name: String,
    description: String = "",
    aliases: List<String> = emptyList(),
    permission: String? = null,
    executor: CommandContext.() -> Unit
): CommandBuilder {
    val builder = CommandBuilder(name)
    builder.description = description
    builder.aliases = aliases
    builder.permission = permission
    builder.execute { ctx -> ctx.executor() }
    val command = builder.build(this)
    commandMap.register(this.name, command)
    return builder
}

object Suggest {
    fun players(context: CommandContext): List<String> {
        val input = context.args.lastOrNull() ?: ""
        return Bukkit.getOnlinePlayers()
            .map { it.name }
            .filter { it.startsWith(input, ignoreCase = true) }
    }
}
