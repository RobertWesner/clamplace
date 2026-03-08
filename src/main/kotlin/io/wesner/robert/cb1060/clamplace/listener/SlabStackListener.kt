package io.wesner.robert.cb1060.clamplace.listener

import io.wesner.robert.cb1060.clamplace.asRevertible
import io.wesner.robert.cb1060.clamplace.contextOrNull
import io.wesner.robert.cb1060.clamplace.isOccupied
import io.wesner.robert.cb1060.clamplace.isPlacementSuccessful
import io.wesner.robert.cb1060.clamplace.withSlabPreservation
import org.bukkit.Material
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent

// TODO: when stacking from below a ceiling it should also work!

class SlabStackListener : Listener {
    @EventHandler(priority = Event.Priority.High, ignoreCancelled = true)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return

        val (player, clicked, _, target, item) = event.contextOrNull()
            ?: return

        // must be targeting block directly above
        if (
            target.x != clicked.x
            || target.y != clicked.y + 1
            || target.z != clicked.z
        ) {
            return
        }

        if (target.type == Material.AIR && !target.isOccupied) return

        if (
            item.type != Material.STEP
            || item.type != clicked.type
            || item.data.data != clicked.data // if data.data != data, makes total sense!
        ) return

        val (clear, restore) = clicked.withSlabPreservation()
        val revert = clicked.asRevertible()
        clear()
        clicked.setTypeIdAndData(Material.DOUBLE_STEP.id, item.data.data, true)
        restore()

        if (
            isPlacementSuccessful(
                clicked,
                clicked.state,
                clicked,
                item,
                player,
            )
        ) {
            player.inventory.removeItem(item.clone().apply { amount = 1 })
            event.isCancelled = true
        } else {
            revert()
        }
    }
}
