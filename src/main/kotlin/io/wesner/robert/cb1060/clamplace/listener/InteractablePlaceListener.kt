package io.wesner.robert.cb1060.clamplace.listener

import io.wesner.robert.cb1060.clamplace.BlockGroup
import io.wesner.robert.cb1060.clamplace.asRevertible
import io.wesner.robert.cb1060.clamplace.contextOrNull
import io.wesner.robert.cb1060.clamplace.isOccupied
import io.wesner.robert.cb1060.clamplace.isPlacementSuccessful
import io.wesner.robert.cb1060.clamplace.faceLookingAtBlock
import io.wesner.robert.cb1060.clamplace.faceLookingAtBlockHorizontal
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.material.Directional
import org.bukkit.material.MaterialData
import org.bukkit.material.Rails

// this honestly took a lot of manual testing, but I am confident in my solutions
// TODO: maybe add the chest stuffs, would be nice, but have to be careful
// TODO: nether water :)
// TODO: listener to prevent plate on fence from breaking
// TODO: paintings could be nice

class InteractablePlaceListener : Listener {

    @EventHandler(priority = Event.Priority.High, ignoreCancelled = true)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return

        val (player, clicked, direction, target, item, below) = event.contextOrNull()
            ?: return

        val isPlateOnFence = item.type in setOf(Material.STONE_PLATE, Material.WOOD_PLATE) && below.type == Material.FENCE

        if (item.type in BlockGroup.alwaysAllowed) {
            // if player is not sneaking but clicked is interactable, do the interaction
            if (!player.isSneaking && clicked.type in BlockGroup.interactable) return
        } else if (
            isPlateOnFence
        ) {
            // pass
        } else {
            // only require sneak and stuff on those that are not always allowed
            if (!player.isSneaking) return
            if (clicked.type !in BlockGroup.interactable) return
        }

        if (item.type === Material.AIR) return
        if (target.type !in BlockGroup.replaceable) return

        // make sure directions are safe
        if (!materialAllowed(item.type, direction)) return

        // torches and ladders can ignore occupation
        if (target.isOccupied && item.type !in BlockGroup.bypassOccupied) return

        // buttons and co require a solid block
        if (
            item.type in BlockGroup.requireSolidToAttachTo
            && clicked.type !in BlockGroup.solid
            && (
                !isPlateOnFence
            )
        ) {
            return
        }

        if (
            item.type in BlockGroup.requireBottomSupport
            && below.type !in BlockGroup.solid
            && !isPlateOnFence
        ) {
            return
        }

        if (!attemptPlace(event)) {
            event.isCancelled = true

            return
        }

        if (item.type in setOf(Material.WATER_BUCKET, Material.LAVA_BUCKET)) {
            player.itemInHand.type = Material.BUCKET
        } else {
            player.inventory.removeItem(item.clone().apply { amount = 1 })
        }

        event.isCancelled = true
    }

    private fun materialAllowed(material: Material, face: BlockFace): Boolean = when {
        // safety filter
        material == Material.AIR -> false

        // water and lava, we want!
        material in setOf(Material.WATER_BUCKET, Material.LAVA_BUCKET) -> true

        // all others non-blocks get rejected, even: cane, cake, bed (complain enough and I might add)
        // signs are janky, and we don't want them!
        material.id > Material.TRAP_DOOR.id -> false

        // stuff that does not make sense to place like that
        material in setOf(
            Material.SAPLING,
            Material.LONG_GRASS,
            Material.DEAD_BUSH,
            Material.YELLOW_FLOWER,
            Material.RED_ROSE,
            Material.BROWN_MUSHROOM,
            Material.RED_MUSHROOM,
            Material.FIRE,
            Material.REDSTONE_WIRE,
            Material.CACTUS,
        ) -> false

        // conditionally allowed
        material in setOf(
            Material.STONE_BUTTON,
            Material.LADDER,
            Material.TRAP_DOOR,
        ) && face !in setOf(
            BlockFace.NORTH,
            BlockFace.WEST,
            BlockFace.SOUTH,
            BlockFace.EAST,
        ) -> false

        // blocks -> yes!
        else -> true
    }

    private fun attemptPlace(event: PlayerInteractEvent): Boolean {
        val (player, clicked, direction, target, item) = event.contextOrNull()!!
        val revert = target.asRevertible()

        // change the block
        target.setTypeIdAndData(targetType(item).id, item.data?.data ?: 0.toByte(), true)

        // flip and twist
        val state = target.state
        val updateState = { stateData: MaterialData ->
            state.data = stateData
            state.update(true)
            target.setData(stateData.data, true)
        }
        when (val stateData = target.state.data) {
            is Directional -> {
                when (target.type) {
                    Material.LEVER -> {
                        if (direction.modY != 0) {
                            // taken straight out the Lever.java, very ugly, but its necessary
                            when (player.faceLookingAtBlockHorizontal(target)) {
                                BlockFace.WEST, BlockFace.EAST -> stateData.data =
                                    (stateData.data.toInt() or 5).toByte()

                                BlockFace.SOUTH, BlockFace.NORTH -> stateData.data =
                                    (stateData.data.toInt() or 6).toByte()

                                else -> stateData.data = 2.toByte()
                            }
                        } else {
                            stateData.setFacingDirection(direction)
                        }
                    }

                    Material.PISTON_BASE, Material.PISTON_STICKY_BASE -> {
                        stateData.setFacingDirection(
                            player.faceLookingAtBlock(target).oppositeFace
                        )
                    }

                    in BlockGroup.playerAngledBlocks -> {
                        stateData.setFacingDirection(
                            player.faceLookingAtBlockHorizontal(target).let {
                                when (target.type) {
                                    Material.PUMPKIN, Material.JACK_O_LANTERN -> it
                                    else -> it.oppositeFace
                                }
                            }
                        )
                    }

                    else -> {
                        stateData.setFacingDirection(
                            when (target.type) {
                                Material.LADDER -> direction.oppositeFace
                                else -> direction
                            }
                        )
                    }
                }

                updateState(stateData)
            }
            is Rails -> {
                stateData.setDirection(
                    player.faceLookingAtBlockHorizontal(target).let {
                        when (target.type) {
                            Material.PUMPKIN, Material.JACK_O_LANTERN -> it
                            else -> it.oppositeFace
                        }
                    },
                    false,
                )
                updateState(stateData)
            }
        }

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

    private fun targetType(item: ItemStack): Material = when (item.type) {
        Material.WATER_BUCKET -> Material.WATER
        Material.LAVA_BUCKET -> Material.LAVA
        else -> item.type
    }
}
