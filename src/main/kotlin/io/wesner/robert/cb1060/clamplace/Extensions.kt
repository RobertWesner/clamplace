package io.wesner.robert.cb1060.clamplace

import net.minecraft.server.AxisAlignedBB
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.craftbukkit.entity.CraftEntity
import org.bukkit.entity.Item
import org.bukkit.entity.Player
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
    val item: ItemStack
)

fun PlayerInteractEvent.contextOrNull(): PlaceContext? = PlaceContext(
    player,
    clickedBlock
        ?: return null,
    blockFace,
    clickedBlock.getRelative(blockFace),
    item.takeUnless { it.type === Material.AIR }
        ?: return null,
)

fun Player.facing(): BlockFace {
    val rot = ((location.yaw % 360) + 360) % 360

    return when {
        rot !in 45.0..<315.0 -> BlockFace.WEST
        rot < 135 -> BlockFace.NORTH
        rot < 225 -> BlockFace.EAST
        else -> BlockFace.SOUTH
    }
}

fun Player.relativeBlockFace(block: Block): BlockFace {
    val dx = location.x - (block.x + 0.5)
    val dy = location.y + eyeHeight - (block.y + 0.5)
    val dz = location.z - (block.z + 0.5)

    val ax = kotlin.math.abs(dx)
    val ay = kotlin.math.abs(dy)
    val az = kotlin.math.abs(dz)

    return when {
        ay >= ax && ay >= az ->
            if (dy > 0) BlockFace.UP
            else BlockFace.DOWN
        ax >= az ->
            if (dx > 0) BlockFace.EAST
            else BlockFace.WEST
        else ->
            if (dz > 0) BlockFace.SOUTH
            else BlockFace.NORTH
    }
}

fun BlockFace.rotate(steps: Int): BlockFace {
    val faces = arrayOf(
        BlockFace.NORTH,
        BlockFace.EAST,
        BlockFace.SOUTH,
        BlockFace.WEST
    )

    val i = faces.indexOf(this)
    if (i == -1) return this

    val rot = (i + steps % 4 + 4) % 4
    return faces[rot]
}
