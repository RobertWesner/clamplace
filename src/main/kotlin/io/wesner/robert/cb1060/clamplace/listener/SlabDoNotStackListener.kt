package io.wesner.robert.cb1060.clamplace.listener

import io.wesner.robert.cb1060.clamplace.BlockGroup
import io.wesner.robert.cb1060.clamplace.ClamPlace
import io.wesner.robert.cb1060.clamplace.asRevertible
import io.wesner.robert.cb1060.clamplace.contextOrNull
import io.wesner.robert.cb1060.clamplace.faceLookingAtBlock
import io.wesner.robert.cb1060.clamplace.faceLookingAtBlockHorizontal
import io.wesner.robert.cb1060.clamplace.isPlacementSuccessful
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent

// side-clicking a block diagonally to the top of a slab should not just merge the slab

class SlabDoNotStackListener : Listener {
    var ignored: Block? = null
    var toPreserve: List<Triple<Block, Material, Byte>> = listOf()

    @EventHandler(priority = Event.Priority.High, ignoreCancelled = true)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        Bukkit.getLogger().info { event.toString() }

        if (event.action != Action.RIGHT_CLICK_BLOCK) return

        val (player, clicked, direction, target, item, below) = event.contextOrNull()
            ?: return

        if (target.type !in BlockGroup.replaceable) return

        if (
            item.type != Material.STEP
            || below.type != Material.STEP
            || item.type.data != below.type.data
        ) {
            return
        }

        ignored = target
        toPreserve = target.chunk.let { chunk ->
            // it HAS TO BE reversed, otherwise jankbukkit will merge them all again
            (0..<target.y).reversed().map { y ->
                val block = chunk.getBlock(target.x, y, target.z)

                Triple(block, block.type, block.data)
            }.filter {
                it.first.type in setOf(Material.STEP, Material.DOUBLE_STEP)
                // DO NOT CHECK FOR DATA HERE (trust)
            }
        }
    }

    @EventHandler(priority = Event.Priority.High, ignoreCancelled = true)
    fun onBlockPlace(event: BlockPlaceEvent) {
        Bukkit.getLogger().info { event.toString() }
        if (event.block.x != ignored?.x || event.block.z != ignored?.z || ignored == null) return

        // has to be player! because event.itemInHand does not have data for some reason
        val item = event.player.itemInHand

        // TODO: permission check via event

        // this needs to happen so there are no merges while setting them
        toPreserve.forEach { (block) -> block.type = Material.AIR }

        ignored!!.setTypeIdAndData(item.type.id, item.data.data, true)
        toPreserve.forEach { (block, type, data) -> block.setTypeIdAndData(type.id, data, true) }

        ignored = null
        toPreserve = listOf()
    }
}
