package io.wesner.robert.cb1060.clamplace.listener

import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.*

class KeepPlateOnFenceListener : Listener {
    @EventHandler(priority = Event.Priority.High, ignoreCancelled = true)
    fun onBlockPhysics(event: BlockPhysicsEvent) {
        if (event.block.type !in setOf(Material.STONE_PLATE, Material.WOOD_PLATE)) return
        if (event.block.getRelative(BlockFace.DOWN).type != Material.FENCE) return

        event.isCancelled = true
    }
}
