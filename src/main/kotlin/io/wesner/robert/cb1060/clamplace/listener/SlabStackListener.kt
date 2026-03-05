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

class SlabStackListener : Listener {
    @EventHandler(priority = Event.Priority.High, ignoreCancelled = true)
    fun onBlockPlace(event: PlayerInteractEvent) {
        val (player, clicked, _, _, item) = event.contextOrNull()
            ?: return

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
