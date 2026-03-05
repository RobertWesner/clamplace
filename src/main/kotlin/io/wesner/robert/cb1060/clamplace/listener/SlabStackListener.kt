package io.wesner.robert.cb1060.clamplace.listener

import org.bukkit.Bukkit
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent

class SlabStackListener : Listener {
    @EventHandler(priority = Event.Priority.High, ignoreCancelled = true)
    fun onBlockPlace(event: BlockPlaceEvent) {
        Bukkit.getLogger().info { event.toString() }
        // TODO
    }
}
