package io.wesner.robert.cb1060.clamplace.listener

import io.wesner.robert.cb1060.clamplace.asRevertible
import io.wesner.robert.cb1060.clamplace.contextOrNull
import io.wesner.robert.cb1060.clamplace.isOccupied
import io.wesner.robert.cb1060.clamplace.isPlacementSuccessful
import io.wesner.robert.cb1060.clamplace.withSlabPreservation
import net.minecraft.server.AxisAlignedBB
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent

class SlabStackListener : Listener {
    @EventHandler(priority = Event.Priority.High, ignoreCancelled = true)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return

        val (player, clicked, direction, target, item) = event.contextOrNull()
            ?: return

        // must be targeting block directly above
        if (
            target.x != clicked.x
            || target.y !in setOf(clicked.y - 1, clicked.y + 1)
            || target.z != clicked.z
        ) {
            return
        }

        if (target.type == Material.AIR && !target.isOccupied) return

        if (
            item.type != Material.STEP
            || item.type != when (direction) {
                BlockFace.DOWN -> target.type
                else -> clicked.type
            }
            || item.data.data != clicked.data // if data.data != data, makes total sense!
        ) return

        val realTarget = when (direction) {
            BlockFace.DOWN -> target
            else -> clicked
        }

        // prevent colliding with player
        // TODO: probably also worth checking other entities
        if (
            (player as CraftPlayer).handle.boundingBox.a(AxisAlignedBB.a(
                realTarget.x.toDouble(),
                realTarget.y.toDouble(),
                realTarget.z.toDouble(),
                realTarget.x + 1.0,
                realTarget.y + 1.0,
                realTarget.z + 1.0,
            ))
        ) return

        val (clear, restore) = realTarget.withSlabPreservation()
        val revert = realTarget.asRevertible()

        val oldState = target.state
        clear()
        realTarget.setTypeIdAndData(Material.DOUBLE_STEP.id, item.data.data, true)
        restore()

        if (
            isPlacementSuccessful(
                realTarget,
                oldState,
                realTarget,
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
