package com.kaity.server.manager

import com.kaity.server.SillyPlugin
import org.bukkit.configuration.ConfigurationSection

/**
 * Manages dynamic text animations for the server.
 * Supports both nested and root-level animation definitions.
 */
class AnimationManager(private val plugin: SillyPlugin) {

    private data class Animation(val frames: List<String>, val interval: Long)
    private val animations = mutableMapOf<String, Animation>()

    init {
        reload()
    }

    /**
     * Reloads animations from the configuration.
     */
    fun reload() {
        animations.clear()
        val section = plugin.config.getConfigurationSection("animations") ?: return
        
        for (key in section.getKeys(false)) {
            // Priority 1: Nested under animations.<key>
            // Example:
            // animations:
            //   logo:
            //     change-interval: 50
            //     texts: [...]
            var animSection = section.getConfigurationSection(key)
            
            // Priority 2: Listed in animations, but defined at root level
            // Example:
            // animations:
            //   logo: {}
            // logo:
            //   change-interval: 50
            //   texts: [...]
            if (animSection == null || !animSection.contains("texts")) {
                animSection = plugin.config.getConfigurationSection(key)
            }

            if (animSection != null && animSection.contains("texts")) {
                val frames = animSection.getStringList("texts")
                val interval = animSection.getLong("change-interval", 100L)
                if (frames.isNotEmpty()) {
                    animations[key] = Animation(frames, interval)
                }
            }
        }
    }

    /**
     * Gets the current frame of an animation by name.
     */
    fun getAnimationFrame(name: String): String {
        val anim = animations[name] ?: return ""
        val index = (System.currentTimeMillis() / anim.interval).toInt() % anim.frames.size
        return anim.frames[index]
    }

    /**
     * Replaces animation placeholders in the given text.
     * Supports modern %animation.name% format and legacy %name% format.
     */
    fun parse(text: String): String {
        var parsed = text

        // Modern format: %animation.<name>%
        val regex = Regex("%animation\\.(\\w+)%")
        parsed = regex.replace(parsed) { matchResult ->
            getAnimationFrame(matchResult.groupValues[1])
        }

        // Legacy format: %<name>%
        animations.keys.forEach { key ->
            parsed = parsed.replace("%$key%", getAnimationFrame(key))
        }

        return parsed
    }
}
