package io.wesner.robert.cb1060.clamplace.listener

import io.wesner.robert.cb1060.clamplace.asRevertible
import io.wesner.robert.cb1060.clamplace.contextOrNull
import io.wesner.robert.cb1060.clamplace.isPlacementSuccessful
import org.bukkit.Effect
import org.bukkit.Material
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent

// TODO: when stacking from below a ceiling it should also work!

class SlabStackListener : Listener {
    @EventHandler(priority = Event.Priority.High, ignoreCancelled = true)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val (player, clicked, _, target, item) = event.contextOrNull()
            ?: return

        // must be targeting block directly above
        if (
            target.location.blockX != clicked.location.blockX
            || target.location.blockY != clicked.location.blockY + 1
            || target.location.blockZ != clicked.location.blockZ
        ) {
            return
        }

        // can place normally, no issues
        if (target.type == Material.AIR) return

        if (
            item.type != Material.STEP
            || item.type != clicked.type
            || item.data.data != clicked.data // if data.data != data, makes total sense!
        ) return

        val revert = clicked.asRevertible()
        clicked.type = Material.DOUBLE_STEP

        if (
            isPlacementSuccessful(
                clicked,
                clicked.state,
                clicked,
                item,
                player,
            )
        ) {
            // TODO: maybe without particles... somehow?
            event.player.playEffect(clicked.location, Effect.STEP_SOUND, clicked.type.id)
            event.isCancelled = true
        } else {
            revert()
        }
    }
}
