package com.kaity.server.util

import com.kaity.server.SillyPlugin
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import java.util.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * NightGui - The absolute best reactive GUI framework for Minecraft.
 * Features:
 * - Reactive state management (auto-renders on change)
 * - Component-based DSL
 * - Smart slot management
 * - Automatic event handling
 */

abstract class NightMenu(val player: Player, val title: String, val rows: Int) : InventoryHolder {
    
    private val inventory = Bukkit.createInventory(this, rows * 9, Messenger.parse(title))
    private val components = mutableMapOf<Int, GuiComponent>()
    private val stateListeners = mutableListOf<() -> Unit>()

    override fun getInventory(): Inventory = inventory

    // --- State Management ---

    class StateProperty<T>(private var value: T, private val onUpdate: () -> Unit) : ReadWriteProperty<Any?, T> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): T = value
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            if (this.value != value) {
                this.value = value
                onUpdate()
            }
        }
    }

    fun <T> state(initial: T): ReadWriteProperty<Any?, T> = StateProperty(initial) { render() }

    // --- Core Lifecycle ---

    fun open() {
        render()
        player.openInventory(inventory)
    }

    fun render() {
        // Clear logic (optional, components can overwrite)
        components.clear()
        
        // Call user-defined building logic
        build()
        
        // Refresh inventory contents
        for (i in 0 until rows * 9) {
            val component = components[i]
            if (component != null) {
                inventory.setItem(i, component.icon())
            } else {
                inventory.setItem(i, null)
            }
        }
    }

    abstract fun build()

    // --- DSL Helpers ---

    fun setItem(slot: Int, item: ItemStack, onClick: (InventoryClickEvent) -> Unit = {}) {
        components[slot] = StaticComponent(item, onClick)
    }

    fun setItem(row: Int, col: Int, item: ItemStack, onClick: (InventoryClickEvent) -> Unit = {}) {
        setItem(row * 9 + col, item, onClick)
    }

    fun button(slot: Int, item: ItemStack, onClick: (InventoryClickEvent) -> Unit) {
        setItem(slot, item, onClick)
    }

    fun toggle(slot: Int, label: String, icon: Material, enabled: Boolean, onToggle: (Boolean) -> Unit) {
        val statusLine = if (enabled) "<green>⬤ <bold>ENABLED</bold>" else "<red>⬤ <bold>DISABLED</bold>"
        val stateMat   = if (enabled) Material.LIME_STAINED_GLASS_PANE else Material.RED_STAINED_GLASS_PANE

        setItem(slot - 9, item(stateMat) { name(" ") })
        setItem(slot, item(icon) {
            name("<white><bold>$label")
            lore(
                "",
                "<dark_gray>─────────────────",
                "  $statusLine",
                "",
                "<yellow>▶ Click to toggle"
            )
        }) { onToggle(!enabled) }
    }

    fun fillBorder(item: ItemStack) {
        for (i in 0 until 9) setItem(i, item)
        for (i in 0 until 9) setItem(i + (rows - 1) * 9, item)
        for (i in 1 until rows - 1) {
            setItem(i * 9, item)
            setItem(i * 9 + 8, item)
        }
    }

    fun fillAll(item: ItemStack) {
        for (i in 0 until rows * 9) {
            if (!components.containsKey(i)) {
                setItem(i, item)
            }
        }
    }

    // --- Event Handling ---

    fun handleInteraction(event: InventoryClickEvent) {
        event.isCancelled = true
        components[event.slot]?.onClick(event)
    }
}

abstract class PaginatedMenu<T>(player: Player, title: String, rows: Int, val items: List<T>) : NightMenu(player, title, rows) {
    var page by state(0)
    
    val pageSize = (rows - 2) * 7 // Defaulting to center 7x(rows-2) grid
    val totalPages = if (items.isEmpty()) 1 else (items.size - 1) / pageSize + 1

    override fun build() {
        fillBorder(item(Material.BLACK_STAINED_GLASS_PANE) { name(" ") })
        
        val start = page * pageSize
        val end = (start + pageSize).coerceAtMost(items.size)
        
        val slots = mutableListOf<Int>()
        for (r in 1 until rows - 1) {
            for (c in 1..7) {
                slots.add(r * 9 + c)
            }
        }

        for (i in start until end) {
            val itemData = items[i]
            val slot = slots[i - start]
            renderItem(slot, itemData)
        }

        if (page > 0) {
            setItem((rows - 1) * 9 + 3, item(Material.ARROW) { name("<light_purple>Previous Page") }) { page-- }
        }
        
        if (page < totalPages - 1) {
            setItem((rows - 1) * 9 + 5, item(Material.ARROW) { name("<light_purple>Next Page") }) { page++ }
        }

        setItem((rows - 1) * 9 + 4, item(Material.BARRIER) { name("<red>Close") }) { player.closeInventory() }
        
        buildExtra()
    }

    abstract fun renderItem(slot: Int, data: T)
    open fun buildExtra() {}
}

interface GuiComponent {
    fun icon(): ItemStack
    fun onClick(event: InventoryClickEvent)
}

class StaticComponent(val item: ItemStack, val click: (InventoryClickEvent) -> Unit) : GuiComponent {
    override fun icon(): ItemStack = item
    override fun onClick(event: InventoryClickEvent) { click(event) }
}

class NightGuiListener(private val plugin: SillyPlugin) : Listener {
    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        val holder = event.inventory.holder as? NightMenu ?: return
        if (event.clickedInventory != event.inventory) return
        holder.handleInteraction(event)
    }

    @EventHandler
    fun onDrag(event: InventoryDragEvent) {
        if (event.inventory.holder is NightMenu) {
            event.isCancelled = true
        }
    }
}

/**
 * Global registry for easy access if needed
 */
object NightGuiManager {
    fun register(plugin: SillyPlugin) {
        plugin.server.pluginManager.registerEvents(NightGuiListener(plugin), plugin)
    }
}
