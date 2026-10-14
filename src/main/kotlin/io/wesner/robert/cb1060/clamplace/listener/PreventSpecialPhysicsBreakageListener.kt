package io.wesner.robert.cb1060.clamplace.listener

import io.wesner.robert.cb1060.clamplace.BlockGroup
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.*
import org.bukkit.material.Attachable

class PreventSpecialPhysicsBreakageListener : Listener {
    @EventHandler(priority = Event.Priority.High, ignoreCancelled = true)
    fun onBlockPhysics(event: BlockPhysicsEvent) {
        val cancel = { event.isCancelled = true }

        val below = event.block.getRelative(BlockFace.DOWN)
        if (
            event.block.type in setOf(Material.STONE_PLATE, Material.WOOD_PLATE)
            && below.type == Material.FENCE
        ) return cancel()

        if (
            event.block.type in BlockGroup.requireBottomSupport
            && below.type in BlockGroup.solid
        ) return cancel()

        if (event.block.state.data is Attachable) {
            val attachedTo = event.block.getRelative((event.block.state.data as Attachable).attachedFace)

            if (
                event.block.type == Material.TRAP_DOOR
                && (
                    attachedTo.type in BlockGroup.trapdoorAllowAttachTo
                    || below.type in BlockGroup.solid + BlockGroup.trapdoorAllowAttachTo
                )
            ) return cancel()

            if (
                event.block.type in BlockGroup.requireSolidToAttachTo
                && attachedTo.type in BlockGroup.solid
            ) return cancel()
        }
    }

    @EventHandler(priority = Event.Priority.High, ignoreCancelled = true)
    fun test(event: BlockBreakEvent) {
        System.out.println(event.toString())
    }
}
