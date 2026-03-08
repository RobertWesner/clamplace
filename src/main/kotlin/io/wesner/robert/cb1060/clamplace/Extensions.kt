package io.wesner.robert.cb1060.clamplace

import net.minecraft.server.AxisAlignedBB
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.BlockState
import org.bukkit.craftbukkit.entity.CraftEntity
import org.bukkit.entity.Item
import org.bukkit.entity.Painting
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.painting.PaintingPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack

private fun blockAabb(b: Block): AxisAlignedBB =
    AxisAlignedBB.b(
        b.x.toDouble(), b.y.toDouble(), b.z.toDouble(),
        (b.x + 1).toDouble(), (b.y + 1).toDouble(), (b.z + 1).toDouble()
    )

val Block.isOccupied: Boolean
    get() {
        val world = world
        val cx = x shr 4
        val cz = z shr 4
        val target = blockAabb(this)

        for (dx in -1..1) for (dz in -1..1) {
            val chunk = world.getChunkAt(cx + dx, cz + dz)
            for (entity in chunk.entities) {
                if (entity is Item) continue

                val nms = (entity as CraftEntity).handle
                val bb = nms.boundingBox ?: continue
                if (bb.a(target)) return true
            }
        }
        return false
    }

data class PlaceContext(
    val player: Player,
    val clicked: Block,
    val face: BlockFace,
    val target: Block,
    val item: ItemStack,
    val below: Block,
)

fun PlayerInteractEvent.contextOrNull(): PlaceContext? {
    val target = clickedBlock?.getRelative(blockFace)

    return PlaceContext(
        player,
        clickedBlock
            ?: return null,
        blockFace,
        target ?: return null,
        item ?: ItemStack(Material.AIR),
        target.getRelative(BlockFace.DOWN),
    )
}

// the fact that BETA north != modern north is tripping me up constantly
fun Player.facing(): BlockFace {
    val rot = ((location.yaw % 360) + 360) % 360

    return when {
        rot !in 45.0..<315.0 -> BlockFace.WEST
        rot < 135 -> BlockFace.NORTH
        rot < 225 -> BlockFace.EAST
        else -> BlockFace.SOUTH
    }
}

private fun faceLookingAtBlock(dx: Double, dy: Double, dz: Double): BlockFace {
    if (dx == 0.0 && dy == 0.0 && dz == 0.0) {
        return BlockFace.SELF
    }

    val ax = kotlin.math.abs(dx)
    val ay = kotlin.math.abs(dy)
    val az = kotlin.math.abs(dz)

    return when {
        ay >= ax && ay >= az ->
            if (dy > 0) BlockFace.DOWN
            else BlockFace.UP
        ax >= az ->
            if (dx > 0) BlockFace.NORTH
            else BlockFace.SOUTH
        else ->
            if (dz > 0) BlockFace.EAST
            else BlockFace.WEST
    }
}

fun Player.faceLookingAtBlock(block: Block): BlockFace =
    faceLookingAtBlock(
        location.x - (block.x + 0.5),
        location.y + eyeHeight - (block.y + 0.5),
        location.z - (block.z + 0.5),
    ).let {
        // ensure player is never BlockFace.SELF in absolute edge-cases
        when {
            it == BlockFace.SELF -> BlockFace.UP
            else -> it
        }
    }

fun Player.faceLookingAtBlockHorizontal(block: Block): BlockFace =
    faceLookingAtBlock(
        location.x - (block.x + 0.5),
        0.0,
        location.z - (block.z + 0.5),
    )

fun Block.faceLookingAtBlock(block: Block): BlockFace =
    faceLookingAtBlock(
        (x - block.x).toDouble(),
        (y - block.y).toDouble(),
        (z - block.z).toDouble(),
    )

fun Block.faceLookingAtBlockHorizontal(block: Block): BlockFace =
    faceLookingAtBlock(
        (x - block.x).toDouble(),
        0.0,
        (z - block.z).toDouble(),
    )

fun Block.asRevertible(): () -> Unit {
    val originalType = type
    val originalData = data

    return {
        setTypeIdAndData(originalType.id, originalData, true)
    }
}

fun isPlacementSuccessful(
    // wonderful naming from the real bukkit API, lovely!
    placedBlock: Block,
    replacedBlockState: BlockState,
    placedAgainst: Block,
    itemInHand: ItemStack,
    thePlayer: Player,
): Boolean =
    BlockPlaceEvent(
        placedBlock,
        replacedBlockState,
        placedAgainst,
        itemInHand,
        thePlayer,
        true,
    ).let {
        Bukkit.getPluginManager().callEvent(it)
        !it.isCancelled
    }

fun isPaintingSuccessful(
    painting: Painting,
    player: Player,
    block: Block,
    blockFace: BlockFace,
): Boolean =
    PaintingPlaceEvent(
        painting,
        player,
        block,
        blockFace,
    ).let {
        Bukkit.getPluginManager().callEvent(it)
        !it.isCancelled
    }

val horizontalFaces = setOf(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST)

fun Block.withSlabPreservation(): Pair<() -> Unit, () -> Unit> {
    val toPreserve = this.chunk.let { chunk ->
        // it HAS TO BE reversed, otherwise jankbukkit will merge them all again
        (0..<this.y).reversed().map { y ->
            val block = chunk.getBlock(this.x, y, this.z)

            Triple(block, block.type, block.data)
        }.filter {
            it.first.type in BlockGroup.step
            // DO NOT CHECK FOR DATA HERE (trust)
        }
    }

    return Pair(
        { toPreserve.forEach { (block) -> block.type = Material.AIR } },
        { toPreserve.forEach { (block, type, data) -> block.setTypeIdAndData(type.id, data, true) } }
    )
}
