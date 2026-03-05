package io.wesner.robert.cb1060.clamplace

import io.wesner.robert.cb1060.clamplace.listener.InteractablePlaceListener
import io.wesner.robert.cb1060.clamplace.listener.SlabDoNotStackListener
import io.wesner.robert.cb1060.clamplace.listener.SlabStackListener
import io.wesner.robert.cb1060.clamplace.listener.StairsPlaceListener
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Logger

class ClamPlace : JavaPlugin() {
    val logger: Logger = Bukkit.getLogger()

    override fun onDisable() {
        logger.info("${description.name} was disabled!")
    }

    override fun onEnable() {
        arrayOf(
            InteractablePlaceListener(),
            StairsPlaceListener(),
            SlabStackListener(),
            SlabDoNotStackListener(),
        ).forEach { server.pluginManager.registerEvents(it, this) }

        logger.info("${description.name} was enabled!")
    }
}
