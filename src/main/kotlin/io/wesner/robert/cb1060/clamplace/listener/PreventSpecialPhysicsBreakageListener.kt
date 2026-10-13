package io.wesner.robert.cb1060.clamplace.listener

import io.wesner.robert.cb1060.clamplace.BlockGroup
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.*
import org.bukkit.material.TrapDoor

class PreventSpecialPhysicsBreakageListener : Listener {
    @EventHandler(priority = Event.Priority.High, ignoreCancelled = true)
    fun onBlockPhysics(event: BlockPhysicsEvent) {
        val below = event.block.getRelative(BlockFace.DOWN)
        if (
            (
                event.block.type in setOf(Material.STONE_PLATE, Material.WOOD_PLATE)
                && below.type == Material.FENCE
            ) || (
                event.block.type == Material.TRAP_DOOR
                && (
                    event.block.getRelative((event.block.state.data as TrapDoor).attachedFace).type in BlockGroup.trapdoorAllowAttachTo
                    || below.type in BlockGroup.solid
                    || below.type in BlockGroup.trapdoorAllowAttachTo
                )
            )
        ) {
            event.isCancelled = true
        }
    }
}
