package com.kaity.server.util

import org.bukkit.inventory.ItemStack
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Base64

object ItemSerialization {

    fun itemStackArrayToBase64(items: Array<ItemStack?>): String {
        try {
            val outputStream = ByteArrayOutputStream()
            val dataOutput = BukkitObjectOutputStream(outputStream)

            dataOutput.writeInt(items.size)

            for (i in items.indices) {
                dataOutput.writeObject(items[i])
            }

            dataOutput.close()
            return Base64.getEncoder().encodeToString(outputStream.toByteArray())
        } catch (e: Exception) {
            throw IllegalStateException("Unable to save item stacks.", e)
        }
    }

    fun itemStackArrayFromBase64(data: String): Array<ItemStack?> {
        if (data.isEmpty()) return arrayOfNulls(54)
        try {
            val inputStream = ByteArrayInputStream(Base64.getDecoder().decode(data))
            val dataInput = BukkitObjectInputStream(inputStream)
            val items = arrayOfNulls<ItemStack>(dataInput.readInt())

            for (i in items.indices) {
                items[i] = dataInput.readObject() as ItemStack?
            }

            dataInput.close()
            return items
        } catch (e: Exception) {
            throw IllegalStateException("Unable to decode item stacks.", e)
        }
    }
}
