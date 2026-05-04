package com.kaity.server.user

import org.bukkit.Location
import java.time.LocalDateTime

data class Home(
    val name: String,
    val location: Location,
    val createdAt: LocalDateTime = LocalDateTime.now()
)
