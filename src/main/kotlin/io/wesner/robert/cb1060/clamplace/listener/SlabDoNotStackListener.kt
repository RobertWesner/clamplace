package io.wesner.robert.cb1060.clamplace.listener

import io.wesner.robert.cb1060.clamplace.asRevertible
import io.wesner.robert.cb1060.clamplace.isPlacementSuccessful
import io.wesner.robert.cb1060.clamplace.faceLookingAtBlock
import io.wesner.robert.cb1060.clamplace.faceLookingAtBlockHorizontal
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent

// side-clicking a block diagonally to the top of a slab should not just merge the slab

class SlabDoNotStackListener : Listener {
    var ignored: Block? = null

    @EventHandler(priority = Event.Priority.High, ignoreCancelled = true)
    fun onBlockPlace(event: BlockPlaceEvent) {
        val player = event.player
        val block = event.blockPlaced
        val against = event.blockAgainst
        val item = event.itemInHand

        // do not chain trigger this handler
        if (block == ignored) return

        if (item.type != Material.STEP) return
        if (block.y >= against.y) return

        val targetFace = against.faceLookingAtBlockHorizontal(block).let {
            // this fallback is necessary so it explicitly prefers horizontal to vertical
            when {
                it == BlockFace.SELF -> against.faceLookingAtBlock(block)
                else -> it
            }
        }

        val realTarget = against.getRelative(targetFace)
        if (realTarget.type != Material.AIR) return

        val revertBlock = block.asRevertible()
        val revertTarget = block.asRevertible()

        // set them both to slabs undo the merge!
        // event cancelling breaks other things!!
        realTarget.type = item.type
        realTarget.state.data = item.data
        realTarget.state.update(true)
        realTarget.setData(item.data.data, true)
        block.type = item.type

        ignored = realTarget
        if (
            !isPlacementSuccessful(
                realTarget,
                realTarget.state,
                against,
                item,
                player,
            )
        ) {
            revertBlock()
            revertTarget()
        }
        ignored = null
    }
}
