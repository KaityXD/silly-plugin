package com.kaity.server.util

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin

class GuiHolder(title: String, val rows: Int) : InventoryHolder {
    private val inventory = Bukkit.createInventory(this, rows * 9, Messenger.parse(title))
    private val actions = mutableMapOf<Int, (InventoryClickEvent) -> Unit>()

    override fun getInventory(): Inventory = inventory

    fun setItem(slot: Int, item: ItemStack, action: (InventoryClickEvent) -> Unit = {}) {
        inventory.setItem(slot, item)
        actions[slot] = action
    }

    fun fillBorder(
        item: ItemStack,
        top: Boolean = true,
        bottom: Boolean = true,
        sides: Boolean = true
    ) {
        if (top) {
            for (i in 0 until 9) {
                setItem(i, item)
            }
        }
        if (bottom) {
            for (i in 0 until 9) {
                setItem(i + (rows - 1) * 9, item)
            }
        }
        if (sides) {
            for (i in 1 until rows - 1) {
                setItem(i * 9, item)
                setItem(i * 9 + 8, item)
            }
        }
    }

    fun fillEmpty(item: ItemStack) {
        for (i in 0 until rows * 9) {
            if (inventory.getItem(i) == null) {
                setItem(i, item)
            }
        }
    }

    fun setItem(row: Int, col: Int, item: ItemStack, action: (InventoryClickEvent) -> Unit = {}) {
        setItem(row * 9 + col, item, action)
    }

    fun setItemClick(slot: Int, item: ItemStack, block: ClickDSL.() -> Unit) {
        setItem(slot, item) { event ->
            ClickDSL(event).block()
        }
    }

    fun setItemClick(row: Int, col: Int, item: ItemStack, block: ClickDSL.() -> Unit) {
        setItemClick(row * 9 + col, item, block)
    }

    fun handle(event: InventoryClickEvent) {
        actions[event.slot]?.invoke(event)
    }

    /**
     * Places a toggle button at [slot] with a matching state-glass block one row above it.
     * The caller supplies all plugin-specific logic (save, re-open, etc.) inside [onToggle].
     */
    fun toggle(
        slot: Int,
        label: String,
        icon: Material,
        desc: List<String>,
        enabled: Boolean,
        onToggle: () -> Unit
    ) {
        val statusLine = if (enabled) "<green>⬤ <bold>ENABLED</bold>" else "<red>⬤ <bold>DISABLED</bold>"
        val stateMat   = if (enabled) Material.LIME_STAINED_GLASS_PANE else Material.RED_STAINED_GLASS_PANE

        // Decorative state block one row above the icon
        setItem(slot - 9, item(stateMat) { name(" ") })

        setItem(slot, item(icon) {
            name("<white><bold>$label")
            lore(
                *desc.toTypedArray(),
                "",
                "<dark_gray>─────────────────",
                "  $statusLine",
                "",
                "<yellow>▶ Click to toggle"
            )
        }) { onToggle() }
    }
}

class GuiListener : Listener {
    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        val holder = event.inventory.holder as? GuiHolder ?: return
        event.isCancelled = true
        holder.handle(event)
    }
}

class ClickDSL(val event: InventoryClickEvent) {
    val isLeft get() = event.isLeftClick
    val isRight get() = event.isRightClick
    val isShift get() = event.isShiftClick
    val isSwap get() = event.click == org.bukkit.event.inventory.ClickType.SWAP_OFFHAND

    fun left(block: (InventoryClickEvent) -> Unit) { if (isLeft) block(event) }
    fun right(block: (InventoryClickEvent) -> Unit) { if (isRight) block(event) }
    fun shift(block: (InventoryClickEvent) -> Unit) { if (isShift) block(event) }
    fun swap(block: (InventoryClickEvent) -> Unit) { if (isSwap) block(event) }
}

fun item(material: Material, block: ItemBuilder.() -> Unit = {}): ItemStack {
    val builder = ItemBuilder(material)
    builder.block()
    return builder.build()
}

class ItemBuilder(material: Material) {
    private val item = ItemStack(material)
    private var name: String? = null
    private var lore: List<String> = emptyList()

    fun name(name: String) { this.name = name }
    fun lore(vararg lines: String) { this.lore = lines.toList() }
    fun skullOwner(player: org.bukkit.OfflinePlayer) {
        val meta = item.itemMeta
        if (meta is org.bukkit.inventory.meta.SkullMeta) {
            meta.owningPlayer = player
            item.itemMeta = meta
        }
    }

    fun build(): ItemStack {
        val meta = item.itemMeta
        name?.let { meta.displayName(Messenger.parse(it)) }
        if (lore.isNotEmpty()) {
            meta.lore(Messenger.parseLore(lore))
        }
        item.itemMeta = meta
        return item
    }
}

fun Player.openGui(title: String, rows: Int, block: GuiHolder.() -> Unit) {
    val holder = GuiHolder(title, rows)
    holder.block()
    this.openInventory(holder.inventory)
}
