package com.kaity.server.command

import com.kaity.server.SillyPlugin

/**
 * Simple interface for auto-registering commands.
 */
interface CommandModule {
    fun SillyPlugin.register()
}
