package io.wesner.robert.cb1060.clamplace.listener

import io.wesner.robert.cb1060.clamplace.asRevertible
import io.wesner.robert.cb1060.clamplace.contextOrNull
import io.wesner.robert.cb1060.clamplace.isOccupied
import io.wesner.robert.cb1060.clamplace.isPlacementSuccessful
import io.wesner.robert.cb1060.clamplace.relativeBlockFace
import io.wesner.robert.cb1060.clamplace.rotate
import org.bukkit.Bukkit
import org.bukkit.Effect
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.material.Directional

// this honestly took a lot of manual testing, but I am confident in my solutions

// TODO: buckets!

class InteractablePlaceListener : Listener {
    val allowedClicked = setOf(
        Material.DISPENSER,
        Material.NOTE_BLOCK,
        Material.BED_BLOCK,
        Material.CHEST,
        Material.WORKBENCH,
        Material.FURNACE,
        Material.BURNING_FURNACE,
        Material.WOODEN_DOOR,
        Material.LEVER,
        Material.STONE_BUTTON,
        Material.TRAP_DOOR,
    )
    val alwaysAllowed = setOf(
        Material.PUMPKIN,
        Material.JACK_O_LANTERN,
    )

    val playerAngledBlocks = setOf(
        Material.DISPENSER,
        Material.PISTON_STICKY_BASE,
        Material.PISTON_BASE,
        Material.FURNACE,
        Material.PUMPKIN,
        Material.JACK_O_LANTERN,
    )

    @EventHandler(priority = Event.Priority.High, ignoreCancelled = true)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val (player, clicked, direction, target, item) = event.contextOrNull()
            ?: return

        if (!player.isSneaking) return
        if (item.type === Material.AIR) return
        if (clicked.type !in allowedClicked && item.type !in alwaysAllowed) return
        if (target.type != Material.AIR) return
        if (!materialAllowed(item.type, direction)) return
        if (target.isOccupied) return

        // placery
        if (!attemptPlace(event)) {
            event.isCancelled = true

            return
        }

        // sounds and stuff
        // TODO: maybe not as annoying?
        event.player.playEffect(target.location, Effect.STEP_SOUND, target.type.id) // that makes particles... :(

        // -1 item credit
        player.inventory.removeItem(item.clone().apply { amount = 1 })

        event.isCancelled = true
    }

    private fun materialAllowed(material: Material, face: BlockFace): Boolean = when {
        // safety filter
        material == Material.AIR -> false

        // water and lava, we want!
        material in setOf(Material.WATER_BUCKET, Material.LAVA_BUCKET) -> true

        // signs are the only other non-block item we tolerate
        material == Material.SIGN -> true

        // all others non-blocks get rejected, even: cane, cake, bed (complain enough and I might add)
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

        // things that can be problematic with /cprivate and such
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
        ) && face !in setOf(
            BlockFace.NORTH,
            BlockFace.WEST,
            BlockFace.SOUTH,
            BlockFace.EAST,
        ) -> false
        material in setOf(
            Material.WOOD_PLATE,
            Material.STONE_PLATE,
            Material.SNOW,
        ) && face != BlockFace.UP -> false
        material in setOf(
            Material.TORCH,
            Material.LEVER,
            Material.REDSTONE_TORCH_OFF,
            Material.REDSTONE_TORCH_ON,
        ) && face !in setOf(
            BlockFace.NORTH,
            BlockFace.WEST,
            BlockFace.SOUTH,
            BlockFace.EAST,
            BlockFace.UP,
        ) -> false

        // blocks -> yes!
        else -> true
    }

    private fun attemptPlace(event: PlayerInteractEvent): Boolean {
        val (player, clicked, direction, target, item) = event.contextOrNull()!!
        val revert = target.asRevertible()

        // change the block
        target.type = item.type

        // flip and twist
        val state = target.state
        val stateData = target.state.data
        if (stateData is Directional) {
            when (target.type) {
                Material.LEVER if direction.modY != 0 -> {
                    // taken straight out the Lever.java, very ugly, but its necessary
                    when (player.relativeBlockFace(target)) {
                        BlockFace.WEST, BlockFace.EAST -> stateData.data = (stateData.data.toInt() or 5).toByte()
                        BlockFace.SOUTH, BlockFace.NORTH -> stateData.data = (stateData.data.toInt() or 6).toByte()
                        else -> {
                            Bukkit.getLogger().info { direction.toString() }
                        }
                    }
                }
                in playerAngledBlocks -> {
                    // all blocks "fronts" are actually to their east, so I rotate them.
                    // and pumpkin stuffs wants to be opposite, apparently.
                    stateData.setFacingDirection(
                        player.relativeBlockFace(target).rotate(1).let {
                            when (target.type) {
                                Material.PUMPKIN, Material.JACK_O_LANTERN -> it.oppositeFace
                                else -> it
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

            state.data = stateData
            state.update(true)
            target.setData(stateData.data, true)
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
}
