package io.wesner.robert.cb1060.clamplace.listener

import io.wesner.robert.cb1060.clamplace.BlockGroup
import io.wesner.robert.cb1060.clamplace.asRevertible
import io.wesner.robert.cb1060.clamplace.contextOrNull
import io.wesner.robert.cb1060.clamplace.isOccupied
import io.wesner.robert.cb1060.clamplace.isPlacementSuccessful
import io.wesner.robert.cb1060.clamplace.faceLookingAtBlock
import io.wesner.robert.cb1060.clamplace.faceLookingAtBlockHorizontal
import io.wesner.robert.cb1060.clamplace.horizontalFaces
import net.minecraft.server.EntityPainting
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.craftbukkit.CraftWorld
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

class InteractablePlaceListener : Listener {
    @EventHandler(priority = Event.Priority.High, ignoreCancelled = true)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        val (player, clicked, direction, target, item, below) = event.contextOrNull()
            ?: return
        val original = Pair(target.type, target.data)

        val isPlateOnFence = item.type in setOf(Material.STONE_PLATE, Material.WOOD_PLATE) && below.type == Material.FENCE

        // switch handling to other listener
        if (
            item.type === Material.STEP
            && below.type === Material.STEP
            && item.data.data == below.data
        ) {
            return
        }

        // always cancel on sneak click on interactible to be close to modern and not have random interactions
        if (clicked.type in BlockGroup.interactable && player.isSneaking) event.isCancelled = true

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

        // ensure proper chest state
        when (item.type) {
            Material.CHEST -> {
                val getRelativeChests = { block: Block ->
                    horizontalFaces.map { block.getRelative(it) }.filter{ it.type == Material.CHEST }
                }

                // I love Kotlin <3
                val relatives = getRelativeChests(target)

                if (relatives.count() >= 2 || relatives.any { getRelativeChests(it).count() > 0 }) return
            }
            else -> {}
        }

        if (
            item.type in BlockGroup.requireBottomSupport
            && direction == BlockFace.DOWN
            && below.type !in BlockGroup.solid
            && !isPlateOnFence
        ) {
            return
        }

        if (
            item.type in BlockGroup.requireBottomDirt
            && below.type !in setOf(
                Material.DIRT,
                Material.GRASS,
            )
        ) {
            return
        }

        if (
            item.type == Material.CACTUS
            && below.type !in setOf(
                Material.SAND,
                Material.CACTUS,
            )
        ) {
            return
        }

        if (!attemptPlace(event)) {
            event.isCancelled = true

            return
        }

        when (item.type) {
            Material.WATER_BUCKET, Material.LAVA_BUCKET -> player.itemInHand.type = Material.BUCKET
            Material.BUCKET -> player.itemInHand.type = when (original.first) {
                Material.WATER, Material.STATIONARY_WATER -> Material.WATER_BUCKET
                Material.LAVA, Material.STATIONARY_LAVA -> Material.LAVA_BUCKET
                else -> null
            }
            else -> player.inventory.removeItem(item.clone().apply { amount = 1 })
        }

        event.isCancelled = true
    }

    private fun materialAllowed(material: Material, face: BlockFace): Boolean = when {
        // safety filter
        material == Material.AIR -> false

        // paintings are now supported
        material == Material.PAINTING && face in horizontalFaces -> true

        // simply delicious
        material in setOf(Material.CAKE, Material.CAKE_BLOCK) -> true

        // water and lava, we want!
        material in setOf(Material.WATER_BUCKET, Material.LAVA_BUCKET, Material.BUCKET) -> true

        // all others non-blocks get rejected, even: cane, bed (complain enough and I might add)
        // signs are janky, and we don't want them!
        material.id > Material.TRAP_DOOR.id -> false

        // cactus only from below a ceiling
        material == Material.CACTUS && face == BlockFace.DOWN -> false

        // conditionally allowed
        material in setOf(
            Material.STONE_BUTTON,
            Material.LADDER,
            Material.TRAP_DOOR,
        ) && face !in horizontalFaces -> false

        // blocks -> yes!
        else -> true
    }

    private fun attemptPlace(event: PlayerInteractEvent): Boolean {
        val (player, clicked, direction, target, item) = event.contextOrNull()!!

        val revert = when (item.type) {
            Material.PAINTING -> {
                val handle = (clicked.world as CraftWorld).handle

                val entity = EntityPainting(handle, clicked.x, clicked.y, clicked.z, when (direction.oppositeFace) {
                    BlockFace.NORTH -> 3
                    BlockFace.EAST -> 2
                    BlockFace.SOUTH -> 1
                    BlockFace.WEST -> 0
                    else -> -1
                })
                handle.addEntity(entity)

                ; { handle.removeEntity(entity) }
            }
            else -> {
                val doRevert = target.asRevertible()
                // change the block
                target.setTypeIdAndData(targetType(item).id, item.data?.data ?: 0.toByte(), true)

                doRevert
            }
        }
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

        if (target.type in setOf(Material.WATER, Material.STATIONARY_WATER) && target.world.environment == World.Environment.NETHER) {
            // retroactively turn into a no-op, should be more stable than preventing the placement,
            // as the permission checks still need to pass
            revert()
        }

        return true
    }

    private fun targetType(item: ItemStack): Material = when (item.type) {
        Material.WATER_BUCKET -> Material.WATER
        Material.LAVA_BUCKET -> Material.LAVA
        Material.BUCKET -> Material.AIR
        Material.CAKE -> Material.CAKE_BLOCK
        else -> item.type
    }
}
