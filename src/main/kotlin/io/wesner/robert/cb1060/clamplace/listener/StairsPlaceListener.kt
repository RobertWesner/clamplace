package io.wesner.robert.cb1060.clamplace.listener

import io.wesner.robert.cb1060.clamplace.asRevertible
import io.wesner.robert.cb1060.clamplace.contextOrNull
import io.wesner.robert.cb1060.clamplace.facing
import io.wesner.robert.cb1060.clamplace.isPlacementSuccessful
import org.bukkit.Material
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.material.Stairs

class StairsPlaceListener : Listener {
    @EventHandler(priority = Event.Priority.High, ignoreCancelled = true)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        val item = event.item

        if (item.type !in setOf(Material.WOOD_STAIRS, Material.COBBLESTONE_STAIRS)) return
        if (player.location.y.let { it - it.toInt() } != 0.5) return

        if (!attemptPlace(event)) {
            return
        }

        player.inventory.removeItem(item.clone().apply { amount = 1 })
        event.isCancelled = true
    }

    private fun attemptPlace(event: PlayerInteractEvent): Boolean {
        val (player, clicked, _, target, item) = event.contextOrNull()!!
        val revert = target.asRevertible()

        target.type = item.type

        val state = target.state
        val stateData = target.state.data
        (stateData as Stairs).setFacingDirection(player.facing().oppositeFace)
        state.data = stateData
        state.update(true)
        target.setData(stateData.data, true)

        if (
            !isPlacementSuccessful(
                target,
                state,
                clicked,
                item,
                player,
            )
        ) {
            return false.also { revert() }
        }

        return true
    }
}
