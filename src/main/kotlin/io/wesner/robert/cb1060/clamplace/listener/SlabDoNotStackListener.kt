package io.wesner.robert.cb1060.clamplace.listener

import io.wesner.robert.cb1060.clamplace.BlockGroup
import io.wesner.robert.cb1060.clamplace.asRevertible
import io.wesner.robert.cb1060.clamplace.contextOrNull
import io.wesner.robert.cb1060.clamplace.isPlacementSuccessful
import io.wesner.robert.cb1060.clamplace.withSlabPreservation
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
    var preserve = Pair({}, {})

    @EventHandler(priority = Event.Priority.High, ignoreCancelled = true)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return

        val (player, clicked, direction, target, item, below) = event.contextOrNull()
            ?: return

        var willFirePlaceEvent = true
        if (clicked.type in BlockGroup.interactable) {
            willFirePlaceEvent = false
            if (!player.isSneaking) {
                return
            }

            event.isCancelled = true
        }

        // do not override the default slab fixes!
        if (direction == BlockFace.UP) return
        if (target.type !in BlockGroup.replaceable) return

        if (
            item.type !in BlockGroup.step
            || below.type !in BlockGroup.step
            || item.type.data != below.type.data
        ) {
            return
        }

        val oldState = target.state
        ignored = target
        preserve = target.withSlabPreservation()

        if (
            !willFirePlaceEvent
            // this needs to run, interactable will never fire PlaceEvent
            && !isPlacementSuccessful(
                target,
                oldState,
                clicked,
                item,
                player,
            )
        ) {
            ignored = null
            preserve = Pair({}, {})
        }
    }

    @EventHandler(priority = Event.Priority.High, ignoreCancelled = true)
    fun onBlockPlace(event: BlockPlaceEvent) {
        if (event.block.x != ignored?.x || event.block.z != ignored?.z || ignored == null) return

        // has to be player! because event.itemInHand does not have data for some reason
        val item = event.player.itemInHand
        val target = ignored ?: return
        val player = event.player
        val revert = target.asRevertible()

        // this needs to happen so there are no merges while setting them
        val (clear, restore) = preserve

        clear()
        target.setTypeIdAndData(item.type.id, item.data.data, true)
        restore()

        ignored = null
        preserve = Pair({}, {})

        if (
            !isPlacementSuccessful(
                target,
                target.state,
                event.blockAgainst,
                item,
                player,
            )
        ) {
            revert()
            event.isCancelled = true
        }
    }
}
